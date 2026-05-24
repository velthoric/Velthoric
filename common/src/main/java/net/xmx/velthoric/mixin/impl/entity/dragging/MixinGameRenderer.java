/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.mixin.impl.entity.dragging;

import com.github.stephengold.joltjni.Quat;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.xmx.velthoric.core.body.client.VxClientBodyDataContainer;
import net.xmx.velthoric.core.body.client.VxClientBodyManager;
import net.xmx.velthoric.core.entity.interaction.VxEntityCollisionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting the client's GameRenderer to observe real-time rotational shifts of dynamic physics bodies.
 * By tracking absolute yaw changes between sequential render ticks, this adjusts the local player's camera
 * directly, preventing frame interpolation lag or visual stuttering.
 *
 * @author xI-Mx-Ix
 */
@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    /**
     * Remembers the specific collision-body index occupied by the player in the preceding frame.
     * Forces a reset of the accumulated yaw tracking whenever the active body changes.
     */
    @Unique
    private Integer velthoric$activeBodyIndex = null;

    /**
     * Holds the absolute yaw angle of the tracked body recorded during the last rendering frame.
     */
    @Unique
    private double velthoric$previousBodyYaw = Double.NaN;

    /**
     * Intercepts render ticks to evaluate angular changes of the underlying physics structure.
     * Subtracts the previous absolute orientation from current values to determine the necessary yaw shift.
     *
     * @param deltaTracker Provides active tick interpolation offsets.
     * @param runTasks     Flag indicating active game rendering loop task status.
     * @param ci           Callback pipeline control object.
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void adjustPlayerRotationOnMovingBodies(DeltaTracker deltaTracker, boolean runTasks, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            this.velthoric$activeBodyIndex = null;
            this.velthoric$previousBodyYaw = Double.NaN;
            return;
        }

        LocalPlayer player = mc.player;
        if (player.getVehicle() != null) {
            this.velthoric$activeBodyIndex = null;
            this.velthoric$previousBodyYaw = Double.NaN;
            return;
        }

        int groundIdx = VxEntityCollisionManager.getGroundSlotIdx(player);
        if (groundIdx < 0) {
            this.velthoric$activeBodyIndex = null;
            this.velthoric$previousBodyYaw = Double.NaN;
            return;
        }

        VxClientBodyManager manager = VxClientBodyManager.getInstance();
        if (manager.getStore() == null) {
            return;
        }

        VxClientBodyDataContainer container = manager.getStore().clientCurrent();
        if (groundIdx >= container.getCapacity()) {
            return;
        }

        // Retrieve the interpolated body orientation for the current rendering frame
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(true);
        Quat bodyRot = new Quat();
        manager.getInterpolator().interpolateRotation(manager.getStore(), groundIdx, partialTicks, bodyRot);

        // Rotate the forward vector by the body's full 3D quaternion, then extract the horizontal yaw.
        Quaternionf rotation = new Quaternionf(bodyRot.getX(), bodyRot.getY(), bodyRot.getZ(), bodyRot.getW());
        Vector3f forward = new Vector3f(0.0f, 0.0f, 1.0f);
        forward.rotate(rotation);
        double currentYaw = Math.toDegrees(Math.atan2(forward.x, forward.z));

        // Initialize or reset tracking states if the player has transitioned to a different body
        if (this.velthoric$activeBodyIndex == null || this.velthoric$activeBodyIndex != groundIdx || Double.isNaN(this.velthoric$previousBodyYaw)) {
            this.velthoric$activeBodyIndex = groundIdx;
            this.velthoric$previousBodyYaw = currentYaw;
            return;
        }

        // Calculate the angular delta, safely wrapping it within the -180 to 180 degrees boundary
        double yawDelta = Mth.wrapDegrees(currentYaw - this.velthoric$previousBodyYaw);

        // Apply deadzone filtering to avoid unnecessary updates from floating-point micro-jitter
        if (Math.abs(yawDelta) > 1.0E-4) {
            this.applyYawShift(player, (float) yawDelta);
        }

        this.velthoric$previousBodyYaw = currentYaw;
    }

    /**
     * Mutates the necessary rotation properties on the player to align with the body's yaw change.
     *
     * @param player   The local client-side player entity.
     * @param yawDelta The calculated shift in degrees to be subtracted.
     */
    @Unique
    private void applyYawShift(LocalPlayer player, float yawDelta) {
        // Adjust all yaw-related variables in both the physics and rendering contexts of the local player
        player.setYRot(player.getYRot() - yawDelta);
        player.yRotO -= yawDelta;
        player.yHeadRot -= yawDelta;
        player.yHeadRotO -= yawDelta;
        player.yBodyRot -= yawDelta;
        player.yBodyRotO -= yawDelta;
    }
}