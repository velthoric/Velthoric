/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xmx.velthoric.core.entity.client.VxClientEntityCollisionManager;
import net.xmx.velthoric.core.entity.server.VxServerEntityCollisionManager;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Universal dispatcher for entity-to-body collisions.
 * Acts as a frontend router for Minecraft Mixins, redirecting physics requests
 * to the appropriate Client or Server collision managers.
 *
 * Also contains the core mathematical functions to replicate Jolt's CharacterVirtual
 * momentum and rotation transfer logic.
 *
 * @author xI-Mx-Ix
 */
public final class VxEntityCollisionManager {

    private static final ThreadLocal<Vector3f> TEMP_AXIS = ThreadLocal.withInitial(Vector3f::new);
    private static final ThreadLocal<Quaternionf> TEMP_ROT = ThreadLocal.withInitial(Quaternionf::new);
    private static final ThreadLocal<Vector3f> TEMP_REL_POS = ThreadLocal.withInitial(Vector3f::new);
    private static final ThreadLocal<Vector3f> TEMP_FWD = ThreadLocal.withInitial(Vector3f::new);

    /**
     * Replicates Jolt's CharacterVirtual::CalculateCharacterGroundVelocity exactly.
     * Computes the linear displacement while accounting for the angular velocity
     * acting tangentially on the entity relative to the body's center of mass.
     * Uses primitive double arguments for zero-allocation performance on the hot path.
     *
     * @param entityX  The current X coordinate of the entity.
     * @param entityY  The current Y coordinate of the entity.
     * @param entityZ  The current Z coordinate of the entity.
     * @param comX     The center of mass X coordinate of the physics body.
     * @param comY     The center of mass Y coordinate of the physics body.
     * @param comZ     The center of mass Z coordinate of the physics body.
     * @param linVelX  The linear velocity component on the X-axis of the physics body.
     * @param linVelY  The linear velocity component on the Y-axis of the physics body.
     * @param linVelZ  The linear velocity component on the Z-axis of the physics body.
     * @param angVelX  The angular velocity component on the X-axis of the physics body.
     * @param angVelY  The angular velocity component on the Y-axis of the physics body.
     * @param angVelZ  The angular velocity component on the Z-axis of the physics body.
     * @param dt       The delta time of the simulation tick (typically 0.05 seconds).
     * @return A Vec3 representing the total displacement vector for the entity.
     */
    public static Vec3 calculateGroundDisplacement(
            double entityX, double entityY, double entityZ,
            double comX, double comY, double comZ,
            double linVelX, double linVelY, double linVelZ,
            double angVelX, double angVelY, double angVelZ,
            float dt
    ) {
        double angVelSq = angVelX * angVelX + angVelY * angVelY + angVelZ * angVelZ;

        // If the body has negligible rotation, return simple linear displacement
        if (angVelSq < 1.0E-12) {
            return new Vec3(linVelX * dt, linVelY * dt, linVelZ * dt);
        }

        double angVelLen = Math.sqrt(angVelSq);
        Vector3f axis = TEMP_AXIS.get().set(
                (float) (angVelX / angVelLen),
                (float) (angVelY / angVelLen),
                (float) (angVelZ / angVelLen)
        );
        Quaternionf rotation = TEMP_ROT.get().fromAxisAngleRad(axis, (float) (angVelLen * dt));

        // Calculate entity position relative to the body's center of mass
        Vector3f relativePos = TEMP_REL_POS.get().set(
                (float) (entityX - comX),
                (float) (entityY - comY),
                (float) (entityZ - comZ)
        );

        // Apply the body's rotation delta to the relative position
        relativePos.rotate(rotation);

        // Convert back to world space
        double newPosX = comX + relativePos.x;
        double newPosY = comY + relativePos.y;
        double newPosZ = comZ + relativePos.z;

        // Calculate the difference and add the linear displacement component
        double dx = (newPosX - entityX) + (linVelX * dt);
        double dy = (newPosY - entityY) + (linVelY * dt);
        double dz = (newPosZ - entityZ) + (linVelZ * dt);

        return new Vec3(dx, dy, dz);
    }

    /**
     * Extracts the yaw rotation delta created by the body's angular velocity
     * to rotate the entity identically to the body's Y-axis twist.
     *
     * @param angVelX The angular velocity component on the X-axis.
     * @param angVelY The angular velocity component on the Y-axis.
     * @param angVelZ The angular velocity component on the Z-axis.
     * @param dt      The delta time of the simulation tick (typically 0.05 seconds).
     * @return The yaw delta in degrees.
     */
    public static float calculateYawDelta(double angVelX, double angVelY, double angVelZ, float dt) {
        double angVelSq = angVelX * angVelX + angVelY * angVelY + angVelZ * angVelZ;
        if (angVelSq < 1.0E-12) {
            return 0.0f;
        }

        double angVelLen = Math.sqrt(angVelSq);
        Vector3f axis = TEMP_AXIS.get().set(
                (float) (angVelX / angVelLen),
                (float) (angVelY / angVelLen),
                (float) (angVelZ / angVelLen)
        );
        Quaternionf rotation = TEMP_ROT.get().fromAxisAngleRad(axis, (float) (angVelLen * dt));

        // Rotate the forward vector to extract purely the Y-axis yaw component
        Vector3f forward = TEMP_FWD.get().set(0.0f, 0.0f, 1.0f);
        forward.rotate(rotation);

        return (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
    }

    /**
     * Checks if the entity is currently standing on a Velthoric physics body.
     * Delegates to the respective client or server ground map.
     *
     * @param entity The entity to check.
     * @return True if standing on a body, false otherwise.
     */
    public static boolean isStandingOnBody(Entity entity) {
        if (entity.level() instanceof ServerLevel) {
            int groundIdx = ((VxEntityAttachment) entity).velthoric$getServerGroundBody();
            return groundIdx != 0;
        } else {
            int groundIdx = ((VxEntityAttachment) entity).velthoric$getClientGroundBody();
            return groundIdx != 0;
        }
    }

    /**
     * Retrieves the 0-based index of the body the entity is standing on, or -1 if none.
     *
     * @param entity The entity.
     * @return 0-based slot index or -1.
     */
    public static int getGroundSlotIdx(Entity entity) {
        if (entity.level() instanceof ServerLevel) {
            return ((VxEntityAttachment) entity).velthoric$getServerGroundBody() - 1;
        } else {
            return ((VxEntityAttachment) entity).velthoric$getClientGroundBody() - 1;
        }
    }

    /**
     * Backwards compatibility for horizontal minor-collision mixins.
     * Delegates to the new exact displacement method.
     *
     * @param entity The entity querying body velocity.
     * @param slotIdx The index of the body.
     * @return The body's tick velocity vector.
     */
    public static Vec3 getBodyVelocity(Entity entity, int slotIdx) {
        return getBodyDisplacement(entity, slotIdx);
    }

    /**
     * Calculates the exact local displacement an entity undergoes by standing on the body.
     * Uses the physical velocity state of the specific environment.
     *
     * @param entity The entity.
     * @param slotIdx The physics slot index of the body.
     * @return The displacement vector.
     */
    public static Vec3 getBodyDisplacement(Entity entity, int slotIdx) {
        if (entity.level() instanceof ServerLevel) {
            return VxServerEntityCollisionManager.getBodyDisplacement(entity, slotIdx);
        } else {
            return VxClientEntityCollisionManager.getBodyDisplacement(entity, slotIdx);
        }
    }

    /**
     * Calculates the local yaw rotation transfer from standing on the body.
     *
     * @param entity The entity.
     * @param slotIdx The physics slot index of the body.
     * @return The yaw delta in degrees.
     */
    public static float getBodyYawDelta(Entity entity, int slotIdx) {
        if (entity.level() instanceof ServerLevel) {
            return VxServerEntityCollisionManager.getBodyYawDelta(entity, slotIdx);
        } else {
            return VxClientEntityCollisionManager.getBodyYawDelta(entity, slotIdx);
        }
    }

    /**
     * Intercepts and adjusts the proposed movement vector by routing the entity
     * to the appropriate Server or Client collision manager.
     *
     * @param entity The entity moving.
     * @param movement The initial proposed movement vector.
     * @return The adjusted movement vector after physics interaction.
     */
    public static Vec3 handleCollision(Entity entity, Vec3 movement) {
        if (entity.level() instanceof ServerLevel) {
            return VxServerEntityCollisionManager.handleCollision(entity, movement);
        } else {
            return VxClientEntityCollisionManager.handleCollision(entity, movement);
        }
    }

    /**
     * Routes sneak ledge-bounds calculations to the appropriate environment manager.
     *
     * @param entity The sneaking entity.
     * @param totalMovement Proposed movement vector.
     * @return Bounded movement vector.
     */
    public static Vec3 handleSneakBackOff(Entity entity, Vec3 totalMovement) {
        if (entity.level() instanceof ServerLevel) {
            return VxServerEntityCollisionManager.handleSneakBackOff(entity, totalMovement);
        } else {
            return VxClientEntityCollisionManager.handleSneakBackOff(entity, totalMovement);
        }
    }

    /**
     * Statically checks if the given bounding box intersects with any populated shapes.
     * Delegates to the appropriate client or server collision manager.
     *
     * @param level     The active world level.
     * @param entityBox Bounding volume representing the spatial constraints.
     * @return True if colliding, false otherwise.
     */
    public static boolean isColliding(Level level, AABB entityBox) {
        if (level instanceof ServerLevel) {
            return VxServerEntityCollisionManager.isColliding(level, entityBox);
        } else {
            return VxClientEntityCollisionManager.isColliding(level, entityBox);
        }
    }

    /**
     * Statically retrieves the exact ID of the body the bounding box intersects with.
     * Delegates to the appropriate client or server collision manager.
     *
     * @param level     The active world level.
     * @param entityBox Bounding volume representing the spatial constraints.
     * @return The 0-based index of the colliding body, or -1 if no intersection occurs.
     */
    public static int getCollidingBodyId(Level level, AABB entityBox) {
        if (level instanceof ServerLevel) {
            return VxServerEntityCollisionManager.getCollidingBodyId(level, entityBox);
        } else {
            return VxClientEntityCollisionManager.getCollidingBodyId(level, entityBox);
        }
    }
}