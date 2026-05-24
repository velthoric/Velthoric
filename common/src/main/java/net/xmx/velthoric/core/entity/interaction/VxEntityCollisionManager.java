/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.entity.interaction;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xmx.velthoric.core.entity.interaction.client.VxClientEntityCollisionManager;
import net.xmx.velthoric.core.entity.interaction.server.VxServerEntityCollisionManager;
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

    /**
     * Replicates Jolt's CharacterVirtual::CalculateCharacterGroundVelocity exactly.
     * Computes the linear displacement while accounting for the angular velocity
     * acting tangentially on the entity relative to the body's center of mass.
     *
     * @param entityPos       The current world position of the entity.
     * @param centerOfMass    The center of mass of the physics body.
     * @param linearVelocity  The linear velocity vector of the physics body.
     * @param angularVelocity The angular velocity vector of the physics body.
     * @param dt              The delta time (typically 0.05 for Minecraft's fixed tick).
     * @return A Vec3 representing the total displacement vector for the entity.
     */
    public static Vec3 calculateGroundDisplacement(Vec3 entityPos, Vec3 centerOfMass, Vec3 linearVelocity, Vec3 angularVelocity, float dt) {
        double angVelSq = angularVelocity.lengthSqr();

        // If the body has negligible rotation, return simple linear displacement
        if (angVelSq < 1.0E-12) {
            return linearVelocity.scale(dt);
        }

        double angVelLen = Math.sqrt(angVelSq);
        Vector3f axis = new Vector3f(
                (float) (angularVelocity.x / angVelLen),
                (float) (angularVelocity.y / angVelLen),
                (float) (angularVelocity.z / angVelLen)
        );
        Quaternionf rotation = new Quaternionf().fromAxisAngleRad(axis, (float) (angVelLen * dt));

        // Calculate entity position relative to the body's center of mass
        Vector3f relativePos = new Vector3f(
                (float) (entityPos.x - centerOfMass.x),
                (float) (entityPos.y - centerOfMass.y),
                (float) (entityPos.z - centerOfMass.z)
        );

        // Apply the body's rotation delta to the relative position
        relativePos.rotate(rotation);

        // Convert back to world space
        double newPosX = centerOfMass.x + relativePos.x;
        double newPosY = centerOfMass.y + relativePos.y;
        double newPosZ = centerOfMass.z + relativePos.z;

        // Calculate the difference and add the linear displacement component
        double dx = (newPosX - entityPos.x) + (linearVelocity.x * dt);
        double dy = (newPosY - entityPos.y) + (linearVelocity.y * dt);
        double dz = (newPosZ - entityPos.z) + (linearVelocity.z * dt);

        return new Vec3(dx, dy, dz);
    }

    /**
     * Extracts the yaw rotation delta created by the body's angular velocity 
     * to rotate the entity identically to the body's Y-axis twist.
     *
     * @param angularVelocity The angular velocity vector of the physics body.
     * @param dt              The delta time (typically 0.05 for Minecraft's fixed tick).
     * @return The yaw delta in degrees.
     */
    public static float calculateYawDelta(Vec3 angularVelocity, float dt) {
        double angVelSq = angularVelocity.lengthSqr();
        if (angVelSq < 1.0E-12) {
            return 0.0f;
        }

        double angVelLen = Math.sqrt(angVelSq);
        Vector3f axis = new Vector3f(
                (float) (angularVelocity.x / angVelLen),
                (float) (angularVelocity.y / angVelLen),
                (float) (angularVelocity.z / angVelLen)
        );
        Quaternionf rotation = new Quaternionf().fromAxisAngleRad(axis, (float) (angVelLen * dt));

        // Rotate the forward vector to extract purely the Y-axis yaw component
        Vector3f forward = new Vector3f(0.0f, 0.0f, 1.0f);
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
            Integer groundIdx = VxServerEntityCollisionManager.SERVER_ENTITY_GROUND_BODY.get(entity);
            return groundIdx != null && groundIdx != 0;
        } else {
            Integer groundIdx = VxClientEntityCollisionManager.CLIENT_ENTITY_GROUND_BODY.get(entity);
            return groundIdx != null && groundIdx != 0;
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
            return VxServerEntityCollisionManager.SERVER_ENTITY_GROUND_BODY.getInt(entity) - 1;
        } else {
            return VxClientEntityCollisionManager.CLIENT_ENTITY_GROUND_BODY.getInt(entity) - 1;
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
     * @param movement The proposed movement vector including falling.
     * @return Adjusted movement bounded to valid surfaces.
     */
    public static Vec3 handleSneakBackOff(Entity entity, Vec3 movement) {
        if (entity.level() instanceof ServerLevel) {
            return VxServerEntityCollisionManager.handleSneakBackOff(entity, movement);
        } else {
            return VxClientEntityCollisionManager.handleSneakBackOff(entity, movement);
        }
    }

    /**
     * Routes a static intersection check to the appropriate environment manager.
     *
     * @param entity The entity.
     * @param entityBox Bounding box to query.
     * @return True if colliding.
     */
    public static boolean isColliding(Entity entity, AABB entityBox) {
        if (entity.level() instanceof ServerLevel) {
            return VxServerEntityCollisionManager.isColliding(entity, entityBox);
        } else {
            return VxClientEntityCollisionManager.isColliding(entity, entityBox);
        }
    }
}