/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.entity.client;

import com.github.stephengold.joltjni.Quat;
import com.github.stephengold.joltjni.RVec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xmx.velthoric.core.body.client.VxClientBodyDataContainer;
import net.xmx.velthoric.core.body.client.VxClientBodyManager;
import net.xmx.velthoric.core.entity.VxEntityAttachment;
import net.xmx.velthoric.core.entity.VxEntityCollisionBufferUtil;
import net.xmx.velthoric.core.entity.VxEntityCollisionManager;
import net.xmx.velthoric.jni.ClientEntityCollision;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.nio.ByteBuffer;

/**
 * Client-side processor for entity-to-body collision resolution.
 * Retrieves data from the client-specific interpolated render arrays and forwards it to the JNI backend.
 *
 * @author xI-Mx-Ix
 */
public final class VxClientEntityCollisionManager {

    private static final ThreadLocal<Vector3d> TEMP_PREV_POS = ThreadLocal.withInitial(Vector3d::new);
    private static final ThreadLocal<Quaterniond> TEMP_PREV_ROT = ThreadLocal.withInitial(Quaterniond::new);
    private static final ThreadLocal<Vector3d> TEMP_CURR_POS = ThreadLocal.withInitial(Vector3d::new);
    private static final ThreadLocal<Quaterniond> TEMP_CURR_ROT = ThreadLocal.withInitial(Quaterniond::new);
    private static final ThreadLocal<RVec3> TEMP_TEMP_POS = ThreadLocal.withInitial(RVec3::new);
    private static final ThreadLocal<Quat> TEMP_TEMP_ROT = ThreadLocal.withInitial(Quat::new);
    private static final ThreadLocal<Vector3d> TEMP_RENDER_POS = ThreadLocal.withInitial(Vector3d::new);
    private static final ThreadLocal<Quaterniond> TEMP_RENDER_ROT = ThreadLocal.withInitial(Quaterniond::new);
    private static final ThreadLocal<Vector3d> TEMP_START_LOCAL = ThreadLocal.withInitial(Vector3d::new);
    private static final ThreadLocal<Vector3d> TEMP_END_LOCAL = ThreadLocal.withInitial(Vector3d::new);
    private static final ThreadLocal<Quaterniond> TEMP_QUAT_INV = ThreadLocal.withInitial(Quaterniond::new);

    /**
     * Retrieves the body properties and calculates the exact client-side displacement.
     * Uses velocity-based calculation (identical to the server) to prevent drift accumulation
     * at high body speeds that the previous pose-differential approach suffered from.
     *
     * @param entity The entity.
     * @param slotIdx The index of the physics body slot.
     * @return The displacement vector for this logic tick.
     */
    public static Vec3 getBodyDisplacement(Entity entity, int slotIdx) {
        VxClientBodyManager manager = VxClientBodyManager.getInstance();
        if (manager.getStore() == null) return Vec3.ZERO;

        VxClientBodyDataContainer c = manager.getStore().clientCurrent();
        if (slotIdx < 0 || slotIdx >= c.getCapacity()) return Vec3.ZERO;

        if (c.state1_isActive.get(slotIdx) == 0) {
            return Vec3.ZERO;
        }

        return VxEntityCollisionManager.calculateGroundDisplacement(
                entity.getX(), entity.getY(), entity.getZ(),
                c.posX.get(slotIdx), c.posY.get(slotIdx), c.posZ.get(slotIdx),
                c.state1_velX.get(slotIdx), c.state1_velY.get(slotIdx), c.state1_velZ.get(slotIdx),
                c.state1_angVelX.get(slotIdx), c.state1_angVelY.get(slotIdx), c.state1_angVelZ.get(slotIdx),
                0.05f
        );
    }

    /**
     * Retrieves the body properties and calculates the exact client-side yaw rotation delta.
     *
     * @param entity The entity.
     * @param slotIdx The index of the physics body slot.
     * @return The yaw delta in degrees for this logic tick.
     */
    public static float getBodyYawDelta(Entity entity, int slotIdx) {
        VxClientBodyManager manager = VxClientBodyManager.getInstance();
        if (manager.getStore() == null) return 0.0f;

        VxClientBodyDataContainer c = manager.getStore().clientCurrent();
        if (slotIdx < 0 || slotIdx >= c.getCapacity()) return 0.0f;

        if (c.state1_isActive.get(slotIdx) == 0) {
            return 0.0f;
        }

        return VxEntityCollisionManager.calculateYawDelta(
                c.state1_angVelX.get(slotIdx),
                c.state1_angVelY.get(slotIdx),
                c.state1_angVelZ.get(slotIdx),
                0.05f
        );
    }

    /**
     * Calculates the 100% accurate visual interpolated render position using local-space arc-interpolation.
     * Incorporates eye height during local transformations to rotate the height offset correctly along pitch/roll.
     *
     * @param entity The entity to calculate for.
     * @param slotIdx The index of the body slot.
     * @param partialTicks The current frame's delta tracker alpha value.
     * @param eyeHeight The camera/rendering offset height to translate.
     * @return A Vec3 containing the mathematically perfect render coordinates, or null if invalid.
     */
    public static Vec3 getExactInterpolatedRenderPos(Entity entity, int slotIdx, float partialTicks, double eyeHeight) {
        VxClientBodyManager manager = VxClientBodyManager.getInstance();
        if (manager.getStore() == null) return null;

        VxClientBodyDataContainer c = manager.getStore().clientCurrent();
        if (slotIdx < 0 || slotIdx >= c.getCapacity()) return null;

        // Body Prev Pose (Logic Tick State 0)
        Vector3d prevPos = TEMP_PREV_POS.get().set(c.prev_posX.get(slotIdx), c.prev_posY.get(slotIdx), c.prev_posZ.get(slotIdx));
        Quaterniond prevRot = TEMP_PREV_ROT.get().set(c.prev_rotX.get(slotIdx), c.prev_rotY.get(slotIdx), c.prev_rotZ.get(slotIdx), c.prev_rotW.get(slotIdx));

        // Body Current Pose (Logic Tick State 1)
        Vector3d currPos = TEMP_CURR_POS.get().set(c.posX.get(slotIdx), c.posY.get(slotIdx), c.posZ.get(slotIdx));
        Quaterniond currRot = TEMP_CURR_ROT.get().set(c.rotX.get(slotIdx), c.rotY.get(slotIdx), c.rotZ.get(slotIdx), c.rotW.get(slotIdx));

        // Body High-Resolution Interpolated Render Pose for the current frame
        RVec3 tempPos = TEMP_TEMP_POS.get();
        Quat tempRot = TEMP_TEMP_ROT.get();
        manager.getInterpolator().interpolatePosition(manager.getStore(), slotIdx, partialTicks, tempPos);
        manager.getInterpolator().interpolateRotation(manager.getStore(), slotIdx, partialTicks, tempRot);
        Vector3d renderPos = TEMP_RENDER_POS.get().set(tempPos.xx(), tempPos.yy(), tempPos.zz());
        Quaterniond renderRot = TEMP_RENDER_ROT.get().set(tempRot.getX(), tempRot.getY(), tempRot.getZ(), tempRot.getW());

        // Transform the entity's previous tick world position (xo) into the body's previous local space (including height)
        Quaterniond prevRotInv = TEMP_QUAT_INV.get().set(prevRot).invert();
        Vector3d startLocal = TEMP_START_LOCAL.get().set(entity.xo, entity.yo + eyeHeight, entity.zo).sub(prevPos).rotate(prevRotInv);

        // Transform the entity's current tick world position (x) into the body's current local space (including height)
        Quaterniond currRotInv = TEMP_QUAT_INV.get().set(currRot).invert();
        Vector3d endLocal = TEMP_END_LOCAL.get().set(entity.getX(), entity.getY() + eyeHeight, entity.getZ()).sub(currPos).rotate(currRotInv);

        // Linearly interpolate the local walking movement of the player between ticks
        startLocal.lerp(endLocal, partialTicks);

        // Transform the interpolated local position back into world-space using the perfectly smooth body render rotation
        startLocal.rotate(renderRot).add(renderPos);

        return new Vec3(startLocal.x, startLocal.y, startLocal.z);
    }

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
                outResult, ((VxEntityAttachment) entity).velthoric$getClientGroundBody(), 0.05f
        );

        double epsilon = 1.0E-5;
        double finalDx = Math.abs(outResult[0] - movement.x) < epsilon ? movement.x : outResult[0];
        double finalDy = Math.abs(outResult[1] - movement.y) < epsilon ? movement.y : outResult[1];
        double finalDz = Math.abs(outResult[2] - movement.z) < epsilon ? movement.z : outResult[2];

        if (outResult[3] >= 0) {
            int bodyIdx = (int) outResult[3];
            int trackedIdx = bodyIdx < capacity ? (bodyIdx + 1) : 0;
            ((VxEntityAttachment) entity).velthoric$setClientGroundBody(trackedIdx);
        } else {
            ((VxEntityAttachment) entity).velthoric$setClientGroundBody(0);
        }

        ((VxEntityAttachment) entity).velthoric$setGroundDragScale(outResult[4]);

        return new Vec3(finalDx, finalDy, finalDz);
    }

    /**
     * Enforces sneaky constraints for the Client.
     * Extrapolates relative body offsets and enforces bounding constraints.
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

        AABB entityBox = entity.getBoundingBox();
        float[] outResult = VxEntityCollisionBufferUtil.OUT_RESULT.get();

        ClientEntityCollision.nHandleSneak(
                shapePtrsBuf, c.state1_isActive,
                c.prev_posX, c.prev_posY, c.prev_posZ, c.prev_rotX, c.prev_rotY, c.prev_rotZ, c.prev_rotW,
                capacity,
                (float) (entityBox.getXsize() / 2.0), (float) (entityBox.getYsize() / 2.0), (float) (entityBox.getZsize() / 2.0),
                (float) entityBox.getCenter().x, (float) entityBox.getCenter().y, (float) entityBox.getCenter().z,
                (float) totalMovement.x, (float) totalMovement.z, entity.maxUpStep(),
                outResult
        );

        return new Vec3(outResult[0], totalMovement.y, outResult[1]);
    }

    /**
     * Statically checks if the given bounding box intersects with any client-side physical shapes.
     * Bypasses Entity context completely to optimize for direct world spatial checks.
     *
     * @param level     The active client world level.
     * @param entityBox Bounding volume representing the spatial constraints.
     * @return True if static intersections are detected, false otherwise.
     */
    public static boolean isColliding(Level level, AABB entityBox) {
        VxClientBodyManager manager = VxClientBodyManager.getInstance();
        if (manager.getStore() == null) {
            return false;
        }
        VxClientBodyDataContainer c = manager.getStore().clientCurrent();

        int capacity = c.getCapacity();
        ByteBuffer shapePtrsBuf = VxEntityCollisionBufferUtil.prepareShapePtrsBuffer(capacity, c.bodies);
        if (capacity == 0) {
            return false;
        }

        return ClientEntityCollision.nIsColliding(
                shapePtrsBuf, c.state1_isActive,
                c.prev_posX, c.prev_posY, c.prev_posZ, c.prev_rotX, c.prev_rotY, c.prev_rotZ, c.prev_rotW, capacity,
                (float) (entityBox.getXsize() / 2.0), (float) (entityBox.getYsize() / 2.0), (float) (entityBox.getZsize() / 2.0),
                (float) entityBox.getCenter().x, (float) entityBox.getCenter().y, (float) entityBox.getCenter().z
        );
    }

    /**
     * Statically retrieves the exact ID of the body the bounding box intersects with on the client.
     * Bypasses Entity context completely to optimize for direct world spatial checks.
     *
     * @param level     The active client world level.
     * @param entityBox Bounding volume representing the spatial constraints.
     * @return The 0-based index of the colliding body, or -1 if no intersection is detected.
     */
    public static int getCollidingBodyId(Level level, AABB entityBox) {
        VxClientBodyManager manager = VxClientBodyManager.getInstance();
        if (manager.getStore() == null) {
            return -1;
        }
        VxClientBodyDataContainer c = manager.getStore().clientCurrent();

        int capacity = c.getCapacity();
        ByteBuffer shapePtrsBuf = VxEntityCollisionBufferUtil.prepareShapePtrsBuffer(capacity, c.bodies);
        if (capacity == 0) {
            return -1;
        }

        return ClientEntityCollision.nGetCollidingBodyId(
                shapePtrsBuf, c.state1_isActive,
                c.prev_posX, c.prev_posY, c.prev_posZ, c.prev_rotX, c.prev_rotY, c.prev_rotZ, c.prev_rotW, capacity,
                (float) (entityBox.getXsize() / 2.0), (float) (entityBox.getYsize() / 2.0), (float) (entityBox.getZsize() / 2.0),
                (float) entityBox.getCenter().x, (float) entityBox.getCenter().y, (float) entityBox.getCenter().z
        );
    }
}