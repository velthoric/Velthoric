/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.entity.interaction.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xmx.velthoric.core.body.client.VxClientBodyDataContainer;
import net.xmx.velthoric.core.body.client.VxClientBodyManager;
import net.xmx.velthoric.core.entity.interaction.VxEntityCollisionBufferUtil;
import net.xmx.velthoric.jni.ClientEntityCollision;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMaps;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import java.nio.ByteBuffer;

/**
 * Client-side processor for entity-to-body collision resolution.
 * Retrieves data from the client-specific interpolated render arrays and forwards it to the JNI backend.
 *
 * @author xI-Mx-Ix
 */
public final class VxClientEntityCollisionManager {

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
     * Map storing the 1-based slot index of the physics platform each client entity was last standing on.
     * Uses FastUtil's Reference2IntMap to avoid unboxing/boxing performance overhead.
     */
    public static final Reference2IntMap<Entity> CLIENT_ENTITY_GROUND_BODY = createGroundBodyMap();

    /**
     * Handles collision intersection and resolution strictly for Client entities.
     * Uses interpolated PREV states for perfect visual representation sync without stutter.
     *
     * @param entity The moving entity.
     * @param movement The initial vector.
     * @return The corrected spatial vector.
     */
    public static Vec3 handleCollision(Entity entity, Vec3 movement) {
        VxClientBodyManager manager = VxClientBodyManager.getInstance();
        if (manager.getStore() == null) return movement;
        VxClientBodyDataContainer c = manager.getStore().clientCurrent();

        int capacity = c.getCapacity();
        ByteBuffer shapePtrsBuf = VxEntityCollisionBufferUtil.prepareShapePtrsBuffer(capacity, c.bodies);

        // We only use the first shape pointer to check if the world is completely empty
        if (capacity == 0) return movement;

        AABB entityBox = entity.getBoundingBox();
        float[] outResult = VxEntityCollisionBufferUtil.OUT_RESULT.get();
        outResult[4] = 1.0f; // Default friction
        outResult[5] = 0.0f; // Default deltaYaw

        ClientEntityCollision.nHandleCollision(
                shapePtrsBuf, c.state1_isActive,
                c.prev_posX, c.prev_posY, c.prev_posZ, c.prev_rotX, c.prev_rotY, c.prev_rotZ, c.prev_rotW,
                c.state1_velX, c.state1_velY, c.state1_velZ, c.state1_angVelX, c.state1_angVelY, c.state1_angVelZ,
                capacity,
                (float) (entityBox.getXsize() / 2.0), (float) (entityBox.getYsize() / 2.0), (float) (entityBox.getZsize() / 2.0),
                (float) entityBox.getCenter().x, (float) entityBox.getCenter().y, (float) entityBox.getCenter().z,
                (float) movement.x, (float) movement.y, (float) movement.z, entity.maxUpStep(),
                outResult, CLIENT_ENTITY_GROUND_BODY.getInt(entity), 0.05f
        );

        double epsilon = 1.0E-5;
        double finalDx = Math.abs(outResult[0] - movement.x) < epsilon ? movement.x : outResult[0];
        double finalDy = Math.abs(outResult[1] - movement.y) < epsilon ? movement.y : outResult[1];
        double finalDz = Math.abs(outResult[2] - movement.z) < epsilon ? movement.z : outResult[2];

        // Store the ground body index 
        if (outResult[3] >= 0) {
            int bodyIdx = (int) outResult[3];
            int trackedIdx = bodyIdx < capacity ? (bodyIdx + 1) : 0;
            CLIENT_ENTITY_GROUND_BODY.put(entity, trackedIdx);
        } else {
            CLIENT_ENTITY_GROUND_BODY.put(entity, 0);
        }
        if (outResult[4] < 1.0f) {
            Vec3 currentDelta = entity.getDeltaMovement();
            entity.setDeltaMovement(currentDelta.x * outResult[4], currentDelta.y * outResult[4], currentDelta.z * outResult[4]);
        }

        return new Vec3(finalDx, finalDy, finalDz);
    }

    /**
     * Enforces sneaky constraints for the Client.
     * Extrapolates relative platform offsets and enforces bounding constraints.
     *
     * @param entity The sneaking entity.
     * @param totalMovement The proposed move vector including falling.
     * @return The bounded vector keeping the entity on the ledge.
     */
    public static Vec3 handleSneakBackOff(Entity entity, Vec3 totalMovement) {
        VxClientBodyManager manager = VxClientBodyManager.getInstance();
        if (manager.getStore() == null) return totalMovement;
        VxClientBodyDataContainer c = manager.getStore().clientCurrent();

        int capacity = c.getCapacity();
        ByteBuffer shapePtrsBuf = VxEntityCollisionBufferUtil.prepareShapePtrsBuffer(capacity, c.bodies);
        if (capacity == 0) return totalMovement;

        Vec3 playerDelta = totalMovement;
        AABB entityBox = entity.getBoundingBox();
        float[] outResult = VxEntityCollisionBufferUtil.OUT_RESULT.get();

        ClientEntityCollision.nHandleSneak(
                shapePtrsBuf, c.state1_isActive,
                c.prev_posX, c.prev_posY, c.prev_posZ, c.prev_rotX, c.prev_rotY, c.prev_rotZ, c.prev_rotW,
                capacity,
                (float) (entityBox.getXsize() / 2.0), (float) (entityBox.getYsize() / 2.0), (float) (entityBox.getZsize() / 2.0),
                (float) entityBox.getCenter().x, (float) entityBox.getCenter().y, (float) entityBox.getCenter().z,
                (float) playerDelta.x, (float) playerDelta.z, entity.maxUpStep(),
                outResult
        );

        return new Vec3(outResult[0], totalMovement.y, outResult[1]);
    }

    /**
     * Statically checks if the given bounding box intersects with any populated shapes.
     *
     * @param entity The entity querying intersection.
     * @param entityBox Bounding volume.
     * @return True if colliding.
     */
    public static boolean isColliding(Entity entity, AABB entityBox) {
        if (entity == null) {
            return false;
        }

        VxClientBodyManager manager = VxClientBodyManager.getInstance();
        if (manager.getStore() == null) return false;
        VxClientBodyDataContainer c = manager.getStore().clientCurrent();

        int capacity = c.getCapacity();
        ByteBuffer shapePtrsBuf = VxEntityCollisionBufferUtil.prepareShapePtrsBuffer(capacity, c.bodies);
        if (capacity == 0) return false;

        return ClientEntityCollision.nIsColliding(
                shapePtrsBuf, c.state1_isActive,
                c.prev_posX, c.prev_posY, c.prev_posZ, c.prev_rotX, c.prev_rotY, c.prev_rotZ, c.prev_rotW, capacity,
                (float) (entityBox.getXsize() / 2.0), (float) (entityBox.getYsize() / 2.0), (float) (entityBox.getZsize() / 2.0),
                (float) entityBox.getCenter().x, (float) entityBox.getCenter().y, (float) entityBox.getCenter().z
        );
    }
}