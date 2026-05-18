/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.entity.interaction;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.xmx.velthoric.core.body.VxBody;
import net.xmx.velthoric.math.VxTransform;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Manages the relative movement of an entity standing on a physics body.
 * Ensures the entity rotates and translates correctly along with the body.
 *
 * @author xI-Mx-Ix
 */
public class VxPlatformController {

    /**
     * Map of active controllers per entity to maintain state.
     */
    private static final Map<Entity, VxPlatformController> ACTIVE_CONTROLLERS = new WeakHashMap<>();

    /**
     * Reference to the entity being controlled.
     */
    private final Entity player;

    /**
     * Reference to the physics body being stood upon.
     */
    private final VxBody body;

    /**
     * Transformation state of the body from the previous tick.
     */
    private VxTransform lastBodyTransform;

    /**
     * Counter to detect if an entity has left the body surface.
     */
    private int ticksSinceLanded = 0;

    /**
     * The computed world-space delta movement derived from body movement.
     */
    public Vec3 lastDelta = Vec3.ZERO;

    /**
     * Internal constructor for the controller.
     *
     * @param player The entity.
     * @param body   The physics body.
     */
    public VxPlatformController(Entity player, VxBody body) {
        this.player = player;
        this.body = body;
        this.lastBodyTransform = new VxTransform();
        body.getTransform(this.lastBodyTransform);
    }

    /**
     * Assigns or updates a controller for an entity.
     *
     * @param entity The entity.
     * @param body   The body it is standing on.
     */
    public static void setController(Entity entity, VxBody body) {
        VxPlatformController controller = ACTIVE_CONTROLLERS.get(entity);
        if (controller == null || controller.body != body) {
            ACTIVE_CONTROLLERS.put(entity, new VxPlatformController(entity, body));
        } else {
            controller.ticksSinceLanded = 0;
        }
    }

    /**
     * Removes the controller and transfers remaining momentum to the entity.
     *
     * @param entity The entity to detach.
     */
    public static void removeController(Entity entity) {
        VxPlatformController controller = ACTIVE_CONTROLLERS.remove(entity);
        if (controller != null && entity.isAlive() && !entity.isPassenger() && controller.lastDelta != null) {
            Vec3 currentMovement = entity.getDeltaMovement();
            entity.setDeltaMovement(currentMovement.add(controller.lastDelta));
        }
    }

    /**
     * Ticks the controller and returns the world-space delta.
     *
     * @param entity The entity to tick.
     * @return The movement delta, or null if detached.
     */
    public static Vec3 getDeltaAndTick(Entity entity) {
        VxPlatformController controller = ACTIVE_CONTROLLERS.get(entity);
        if (controller != null) {
            Vec3 delta = controller.tick();
            if (delta == null) {
                removeController(entity);
            } else {
                controller.lastDelta = delta;
            }
            return delta;
        }
        return null;
    }

    /**
     * Gets the active controller for an entity.
     */
    public static VxPlatformController getController(Entity entity) {
        return ACTIVE_CONTROLLERS.get(entity);
    }

    /**
     * Performs the transformation math to calculate world movement based on body movement.
     *
     * @return The required world movement delta to stay "glued" to the moving body.
     */
    public Vec3 tick() {
        if (!player.isAlive() || player.isPassenger() || ticksSinceLanded > 2) {
            return null;
        }
        ticksSinceLanded++;

        VxTransform currentTransform = new VxTransform();
        body.getTransform(currentTransform);

        // No body movement? No delta.
        if (currentTransform.getTranslation().equals(lastBodyTransform.getTranslation()) &&
                currentTransform.getRotation().equals(lastBodyTransform.getRotation())) {
            return Vec3.ZERO;
        }

        // Calculate relative offset in old body space
        Vector3f oldOffset = new Vector3f(
                (float) (player.getX() - lastBodyTransform.getTranslation().x()),
                (float) (player.getY() - lastBodyTransform.getTranslation().y()),
                (float) (player.getZ() - lastBodyTransform.getTranslation().z())
        );

        // Inverse transform to local space
        var oldQ = lastBodyTransform.getRotation();
        Quaternionf invOldRot = new Quaternionf(oldQ.getX(), oldQ.getY(), oldQ.getZ(), oldQ.getW()).invert();
        invOldRot.transform(oldOffset);

        // Transform back to world space using new body rotation/translation
        var newQ = currentTransform.getRotation();
        Quaternionf newRot = new Quaternionf(newQ.getX(), newQ.getY(), newQ.getZ(), newQ.getW());
        newRot.transform(oldOffset);

        Vec3 newWorldPos = new Vec3(
                currentTransform.getTranslation().x() + oldOffset.x,
                currentTransform.getTranslation().y() + oldOffset.y,
                currentTransform.getTranslation().z() + oldOffset.z
        );

        Vec3 delta = newWorldPos.subtract(player.position());
        lastBodyTransform = currentTransform;

        if (delta.lengthSqr() > 0.000001) {
            return delta;
        }
        return Vec3.ZERO;
    }
}