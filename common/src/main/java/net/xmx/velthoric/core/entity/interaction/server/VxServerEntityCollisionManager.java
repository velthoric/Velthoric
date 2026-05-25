/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.entity.interaction.server;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xmx.velthoric.core.body.server.VxServerBodyDataContainer;
import net.xmx.velthoric.core.entity.interaction.VxEntityCollisionBufferUtil;
import net.xmx.velthoric.core.entity.interaction.VxEntityCollisionManager;
import net.xmx.velthoric.core.physics.world.VxPhysicsWorld;
import net.xmx.velthoric.jni.ServerEntityCollision;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMaps;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import java.nio.ByteBuffer;

/**
 * Server-side processor for entity-to-body collision resolution.
 * Retrieves data from the server's Jolt physics representation and forwards it to the JNI backend.
 *
 * @author xI-Mx-Ix
 */
public final class VxServerEntityCollisionManager {

    /**
     * Map storing the 1-based slot index of the physics body each server entity was last standing on.
     * Uses FastUtil's Reference2IntMap to avoid unboxing/boxing performance overhead.
     */
    public static final Reference2IntMap<Entity> SERVER_ENTITY_GROUND_BODY = createGroundBodyMap();

    /**
     * Instantiates a thread-safe primitive Reference2IntMap with a default fallback value of 0.
     *
     * @return A synchronized FastUtil Map for thread-safe state tracking.
     */
    private static Reference2IntMap<Entity> createGroundBodyMap() {
        Reference2IntOpenHashMap<Entity> map = new Reference2IntOpenHashMap<>();
        map.defaultReturnValue(0);
        return Reference2IntMaps.synchronize(map);
    }

    /**
     * Retrieves the body properties and calculates the exact server-side displacement
     * representing the conveyor effect of the moving body on the entity.
     *
     * @param entity The entity.
     * @param slotIdx The index of the physics body slot.
     * @return The displacement vector for this logic tick.
     */
    public static Vec3 getBodyDisplacement(Entity entity, int slotIdx) {
        if (!(entity.level() instanceof ServerLevel)) return Vec3.ZERO;

        VxPhysicsWorld world = VxPhysicsWorld.get(entity.level().dimension());
        if (world == null || world.getBodyManager() == null) return Vec3.ZERO;

        VxServerBodyDataContainer c = world.getBodyManager().getDataStore().serverCurrent();
        if (slotIdx < 0 || slotIdx >= c.getCapacity()) return Vec3.ZERO;

        Vec3 pos = entity.position();
        return VxEntityCollisionManager.calculateGroundDisplacement(
                pos.x, pos.y, pos.z,
                c.posX.get(slotIdx), c.posY.get(slotIdx), c.posZ.get(slotIdx),
                c.velX.get(slotIdx), c.velY.get(slotIdx), c.velZ.get(slotIdx),
                c.angVelX.get(slotIdx), c.angVelY.get(slotIdx), c.angVelZ.get(slotIdx),
                0.05f
        );
    }

    /**
     * Retrieves the body properties and calculates the exact server-side yaw rotation delta.
     *
     * @param entity The entity.
     * @param slotIdx The index of the physics body slot.
     * @return The yaw delta in degrees for this logic tick.
     */
    public static float getBodyYawDelta(Entity entity, int slotIdx) {
        if (!(entity.level() instanceof ServerLevel)) return 0.0f;

        VxPhysicsWorld world = VxPhysicsWorld.get(entity.level().dimension());
        if (world == null || world.getBodyManager() == null) return 0.0f;

        VxServerBodyDataContainer c = world.getBodyManager().getDataStore().serverCurrent();
        if (slotIdx < 0 || slotIdx >= c.getCapacity()) return 0.0f;

        return VxEntityCollisionManager.calculateYawDelta(
                c.angVelX.get(slotIdx),
                c.angVelY.get(slotIdx),
                c.angVelZ.get(slotIdx),
                0.05f
        );
    }

    /**
     * Handles collision intersection and resolution strictly for Server entities.
     * Passes the Jolt physics system pointer so the JNI can utilize exact BodyLock operations.
     *
     * @param entity The moving entity.
     * @param movement The initial vector.
     * @return The corrected spatial vector.
     */
    public static Vec3 handleCollision(Entity entity, Vec3 movement) {
        if (!(entity.level() instanceof ServerLevel)) return movement;

        VxPhysicsWorld world = VxPhysicsWorld.get(entity.level().dimension());
        if (world == null || world.getBodyManager() == null) return movement;

        VxServerBodyDataContainer c = world.getBodyManager().getDataStore().serverCurrent();
        long physicsSystemVa = world.getPhysicsSystem().targetVa();
        if (physicsSystemVa == 0L) return movement;

        int capacity = c.getCapacity();
        ByteBuffer shapePtrsBuf = VxEntityCollisionBufferUtil.prepareShapePtrsBuffer(capacity, c.bodies);
        ByteBuffer bodyIdsBuf = VxEntityCollisionBufferUtil.prepareBodyIdsBuffer(capacity, c.bodies);

        AABB entityBox = entity.getBoundingBox();
        float[] outResult = VxEntityCollisionBufferUtil.OUT_RESULT.get();
        outResult[4] = 1.0f; // Default friction
        outResult[5] = 0.0f; // Default deltaYaw

        Vec3 velocity = entity.getDeltaMovement();

        ServerEntityCollision.nHandleCollision(
                physicsSystemVa, shapePtrsBuf, c.isActive,
                c.posX, c.posY, c.posZ, c.rotX, c.rotY, c.rotZ, c.rotW,
                c.velX, c.velY, c.velZ, c.angVelX, c.angVelY, c.angVelZ,
                bodyIdsBuf.asIntBuffer(), capacity,
                (float) (entityBox.getXsize() / 2.0), (float) (entityBox.getYsize() / 2.0), (float) (entityBox.getZsize() / 2.0),
                (float) entityBox.getCenter().x, (float) entityBox.getCenter().y, (float) entityBox.getCenter().z,
                (float) movement.x, (float) movement.y, (float) movement.z, entity.maxUpStep(),
                80.0f, (float) velocity.x, (float) velocity.y, (float) velocity.z,
                outResult, SERVER_ENTITY_GROUND_BODY.getInt(entity), 0.05f
        );

        double epsilon = 1.0E-5;
        double finalDx = Math.abs(outResult[0] - movement.x) < epsilon ? movement.x : outResult[0];
        double finalDy = Math.abs(outResult[1] - movement.y) < epsilon ? movement.y : outResult[1];
        double finalDz = Math.abs(outResult[2] - movement.z) < epsilon ? movement.z : outResult[2];

        if (outResult[3] >= 0) {
            int bodyIdx = (int) outResult[3];
            SERVER_ENTITY_GROUND_BODY.put(entity, bodyIdx < capacity ? (bodyIdx + 1) : 0);
        } else {
            SERVER_ENTITY_GROUND_BODY.put(entity, 0);
        }

        if (outResult[4] < 1.0f) {
            Vec3 currentDelta = entity.getDeltaMovement();
            entity.setDeltaMovement(currentDelta.x * outResult[4], currentDelta.y * outResult[4], currentDelta.z * outResult[4]);
        }

        return new Vec3(finalDx, finalDy, finalDz);
    }

    /**
     * Enforces sneaky constraints for the Server.
     * Extrapolates relative body offsets and enforces bounding constraints.
     *
     * @param entity The sneaking entity.
     * @param totalMovement The proposed move vector including falling.
     * @return The bounded vector keeping the entity on the ledge.
     */
    public static Vec3 handleSneakBackOff(Entity entity, Vec3 totalMovement) {
        if (!(entity.level() instanceof ServerLevel)) return totalMovement;

        VxPhysicsWorld world = VxPhysicsWorld.get(entity.level().dimension());
        if (world == null || world.getBodyManager() == null) return totalMovement;

        VxServerBodyDataContainer c = world.getBodyManager().getDataStore().serverCurrent();
        long physicsSystemVa = world.getPhysicsSystem().targetVa();
        if (physicsSystemVa == 0L) return totalMovement;

        int capacity = c.getCapacity();
        ByteBuffer shapePtrsBuf = VxEntityCollisionBufferUtil.prepareShapePtrsBuffer(capacity, c.bodies);

        AABB entityBox = entity.getBoundingBox();
        float[] outResult = VxEntityCollisionBufferUtil.OUT_RESULT.get();

        ServerEntityCollision.nHandleSneak(
                physicsSystemVa, shapePtrsBuf, c.isActive,
                c.posX, c.posY, c.posZ, c.rotX, c.rotY, c.rotZ, c.rotW,
                capacity,
                (float) (entityBox.getXsize() / 2.0), (float) (entityBox.getYsize() / 2.0), (float) (entityBox.getZsize() / 2.0),
                (float) entityBox.getCenter().x, (float) entityBox.getCenter().y, (float) entityBox.getCenter().z,
                (float) totalMovement.x, (float) totalMovement.z, entity.maxUpStep(),
                outResult
        );

        return new Vec3(outResult[0], totalMovement.y, outResult[1]);
    }

    /**
     * Statically checks if the given bounding box intersects with any server-side physics bodies.
     * Bypasses Entity context completely to optimize for direct world spatial checks.
     *
     * @param level     The active server world level.
     * @param entityBox Bounding volume representing the spatial constraints.
     * @return True if static intersections are detected, false otherwise.
     */
    public static boolean isColliding(Level level, AABB entityBox) {
        if (!(level instanceof ServerLevel)) {
            return false;
        }

        VxPhysicsWorld world = VxPhysicsWorld.get(level.dimension());
        if (world == null || world.getBodyManager() == null) {
            return false;
        }

        VxServerBodyDataContainer c = world.getBodyManager().getDataStore().serverCurrent();
        long physicsSystemVa = world.getPhysicsSystem().targetVa();
        if (physicsSystemVa == 0L) {
            return false;
        }

        int capacity = c.getCapacity();
        ByteBuffer shapePtrsBuf = VxEntityCollisionBufferUtil.prepareShapePtrsBuffer(capacity, c.bodies);

        return ServerEntityCollision.nIsColliding(
                physicsSystemVa, shapePtrsBuf, c.isActive,
                c.posX, c.posY, c.posZ, c.rotX, c.rotY, c.rotZ, c.rotW, capacity,
                (float) (entityBox.getXsize() / 2.0), (float) (entityBox.getYsize() / 2.0), (float) (entityBox.getZsize() / 2.0),
                (float) entityBox.getCenter().x, (float) entityBox.getCenter().y, (float) entityBox.getCenter().z
        );
    }

    /**
     * Statically retrieves the exact ID of the body the bounding box intersects with on the server.
     * Bypasses Entity context completely to optimize for direct world spatial checks.
     *
     * @param level     The active server world level.
     * @param entityBox Bounding volume representing the spatial constraints.
     * @return The 0-based index of the colliding body, or -1 if no intersection is detected.
     */
    public static int getCollidingBodyId(Level level, AABB entityBox) {
        if (!(level instanceof ServerLevel)) {
            return -1;
        }

        VxPhysicsWorld world = VxPhysicsWorld.get(level.dimension());
        if (world == null || world.getBodyManager() == null) {
            return -1;
        }

        VxServerBodyDataContainer c = world.getBodyManager().getDataStore().serverCurrent();
        long physicsSystemVa = world.getPhysicsSystem().targetVa();
        if (physicsSystemVa == 0L) {
            return -1;
        }

        int capacity = c.getCapacity();
        ByteBuffer shapePtrsBuf = VxEntityCollisionBufferUtil.prepareShapePtrsBuffer(capacity, c.bodies);

        return ServerEntityCollision.nGetCollidingBodyId(
                physicsSystemVa, shapePtrsBuf, c.isActive,
                c.posX, c.posY, c.posZ, c.rotX, c.rotY, c.rotZ, c.rotW, capacity,
                (float) (entityBox.getXsize() / 2.0), (float) (entityBox.getYsize() / 2.0), (float) (entityBox.getZsize() / 2.0),
                (float) entityBox.getCenter().x, (float) entityBox.getCenter().y, (float) entityBox.getCenter().z
        );
    }
}