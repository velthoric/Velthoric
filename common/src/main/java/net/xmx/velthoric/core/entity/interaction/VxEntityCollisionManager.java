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
import net.xmx.velthoric.core.physics.world.VxPhysicsWorld;
import net.xmx.velthoric.core.body.server.VxServerBodyDataContainer;
import net.xmx.velthoric.core.body.client.VxClientBodyManager;
import net.xmx.velthoric.core.body.client.VxClientBodyDataContainer;

/**
 * Universal dispatcher for entity-to-body collisions.
 * Acts as a frontend router for Minecraft Mixins, redirecting physics requests
 * to the appropriate Client or Server collision managers.
 *
 * @author xI-Mx-Ix
 */
public final class VxEntityCollisionManager {

    /**
     * Checks if the entity is currently standing on a Velthoric physics platform.
     * Delegates to the respective client or server ground map.
     *
     * @param entity The entity to check.
     * @return True if standing on a platform, false otherwise.
     */
    public static boolean isStandingOnPlatform(Entity entity) {
        if (entity.level() instanceof ServerLevel) {
            Integer groundIdx = VxServerEntityCollisionManager.SERVER_ENTITY_GROUND_BODY.get(entity);
            return groundIdx != null && groundIdx != 0;
        } else {
            Integer groundIdx = VxClientEntityCollisionManager.CLIENT_ENTITY_GROUND_BODY.get(entity);
            return groundIdx != null && groundIdx != 0;
        }
    }

    /**
     * Retrieves the 0-based index of the platform the entity is standing on, or -1 if none.
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
     * Retrieves the platform's tick velocity displacement vector (vel * dt).
     * Used to prevent acceleration loops during movement redirection.
     *
     * @param entity The entity querying platform velocity.
     * @param slotIdx The index of the platform body.
     * @return The platform's tick velocity vector.
     */
    public static Vec3 getPlatformVelocity(Entity entity, int slotIdx) {
        if (entity.level() instanceof ServerLevel) {
            VxPhysicsWorld world = VxPhysicsWorld.get(entity.level().dimension());
            if (world != null && world.getBodyManager() != null) {
                VxServerBodyDataContainer c = world.getBodyManager().getDataStore().serverCurrent();
                if (slotIdx >= 0 && slotIdx < c.getCapacity()) {
                    return new Vec3(c.velX.get(slotIdx) * 0.05, c.velY.get(slotIdx) * 0.05, c.velZ.get(slotIdx) * 0.05);
                }
            }
        } else {
            VxClientBodyManager manager = VxClientBodyManager.getInstance();
            if (manager.getStore() != null) {
                VxClientBodyDataContainer c = manager.getStore().clientCurrent();
                if (slotIdx >= 0 && slotIdx < c.getCapacity()) {
                    return new Vec3(c.state1_velX.get(slotIdx) * 0.05, c.state1_velY.get(slotIdx) * 0.05, c.state1_velZ.get(slotIdx) * 0.05);
                }
            }
        }
        return Vec3.ZERO;
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