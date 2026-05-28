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
import net.xmx.velthoric.core.entity.interaction.VxEntityAttachment;
import net.xmx.velthoric.core.entity.interaction.VxEntityCollisionBufferUtil;
import net.xmx.velthoric.core.entity.interaction.VxEntityCollisionManager;
import net.xmx.velthoric.core.physics.world.VxPhysicsWorld;
import net.xmx.velthoric.jni.ServerEntityCollision;

import java.nio.ByteBuffer;

/**
 * Server-side processor for entity-to-body collision resolution.
 * Retrieves data from the server's Jolt physics representation and forwards it to the JNI backend.
 *
 * @author xI-Mx-Ix
 */
public final class VxServerEntityCollisionManager {

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

        if (c.isActive.get(slotIdx) == 0) {
            return Vec3.ZERO;
        }

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

        if (c.isActive.get(slotIdx) == 0) {
            return 0.0f;
        }

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

        // Calculate dynamic entity mass based on the volume of its AABB.
        // A player with dimensions 0.6 x 1.8 x 0.6 has a volume of 0.648 m³ and a mass of 80 kg.
        // We scale other entities relative to this density (80.0 / 0.648 ≈ 123.45679 kg/m³).
        // A lower bound of 0.1 kg is enforced to avoid degenerate or division-by-zero scenarios.
        double volume = entityBox.getXsize() * entityBox.getYsize() * entityBox.getZsize();
        float entityMass = (float) Math.max(0.1, volume * (80.0 / 0.648));

        ServerEntityCollision.nHandleCollision(
                physicsSystemVa, shapePtrsBuf, c.isActive,
                c.posX, c.posY, c.posZ, c.rotX, c.rotY, c.rotZ, c.rotW,
                c.velX, c.velY, c.velZ, c.angVelX, c.angVelY, c.angVelZ,
                bodyIdsBuf.asIntBuffer(), capacity,
                (float) (entityBox.getXsize() / 2.0), (float) (entityBox.getYsize() / 2.0), (float) (entityBox.getZsize() / 2.0),
                (float) entityBox.getCenter().x, (float) entityBox.getCenter().y, (float) entityBox.getCenter().z,
                (float) movement.x, (float) movement.y, (float) movement.z, entity.maxUpStep(),
                entityMass, (float) velocity.x, (float) velocity.y, (float) velocity.z,
                outResult, ((VxEntityAttachment) entity).velthoric$getServerGroundBody(), 0.05f
        );

        double epsilon = 1.0E-5;
        double finalDx = Math.abs(outResult[0] - movement.x) < epsilon ? movement.x : outResult[0];
        double finalDy = Math.abs(outResult[1] - movement.y) < epsilon ? movement.y : outResult[1];
        double finalDz = Math.abs(outResult[2] - movement.z) < epsilon ? movement.z : outResult[2];

        if (outResult[3] >= 0) {
            int bodyIdx = (int) outResult[3];
            ((VxEntityAttachment) entity).velthoric$setServerGroundBody(bodyIdx < capacity ? (bodyIdx + 1) : 0);
        } else {
            ((VxEntityAttachment) entity).velthoric$setServerGroundBody(0);
        }

        ((VxEntityAttachment) entity).velthoric$setGroundDragScale(outResult[4]);

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