/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.mixin.impl.entity.dragging;

import com.github.stephengold.joltjni.Quat;
import org.joml.Quaternionf;
import org.joml.Vector3f;
/*? if >=1.21.1 {*/
import net.minecraft.client.DeltaTracker;
 /*? }*/
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.xmx.velthoric.core.body.client.VxClientBodyDataContainer;
import net.xmx.velthoric.core.body.client.VxClientBodyManager;
import net.xmx.velthoric.core.entity.VxEntityCollisionManager;
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

    @Unique
    private static final ThreadLocal<Quat> TEMP_JOLT_ROT = ThreadLocal.withInitial(Quat::new);
    @Unique
    private static final ThreadLocal<Quaternionf> TEMP_JOML_ROT = ThreadLocal.withInitial(Quaternionf::new);
    @Unique
    private static final ThreadLocal<Quaternionf> TEMP_JOML_ROT_2 = ThreadLocal.withInitial(Quaternionf::new);
    @Unique
    private static final ThreadLocal<Quaternionf> TEMP_JOML_ROT_3 = ThreadLocal.withInitial(Quaternionf::new);
    @Unique
    private static final ThreadLocal<Vector3f> TEMP_JOML_FWD = ThreadLocal.withInitial(Vector3f::new);

    /**
     * Remembers the specific collision-body index occupied by the player in the preceding frame.
     * Forces a reset of the accumulated yaw tracking whenever the active body changes.
     */
    @Unique
    private Integer velthoric$activeBodyIndex = null;

    /**
     * Holds the absolute rotation of the tracked body recorded during the last rendering frame.
     */
    @Unique
    private final Quaternionf velthoric$previousBodyRot = new Quaternionf();

    /**
     * Flag indicating if the previous body rotation has been initialized.
     */
    @Unique
    private boolean velthoric$hasPreviousBodyRot = false;

    /**
     * Intercepts render ticks to evaluate angular changes of the underlying physics structure.
     */
    @Inject(method = "render", at = @At("HEAD"))
    /*? if >=1.21.1 {*/
    private void adjustPlayerRotationOnMovingBodies(DeltaTracker deltaTracker, boolean runTasks, CallbackInfo ci) {
        this.velthoric$adjustPlayerRotation(deltaTracker.getGameTimeDeltaPartialTick(true));
    }
    /*? } else {*/
    /*private void adjustPlayerRotationOnMovingBodies(float partialTicks, long limitTime, boolean runTasks, CallbackInfo ci) {
        this.velthoric$adjustPlayerRotation(partialTicks);
    }
    *//*? }*/

    /**
     * The core logic, kept free of preprocessor-specific duplicates.
     */
    @Unique
    private void velthoric$adjustPlayerRotation(float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            this.velthoric$activeBodyIndex = null;
            this.velthoric$hasPreviousBodyRot = false;
            return;
        }

        LocalPlayer player = mc.player;
        if (player.getVehicle() != null) {
            this.velthoric$activeBodyIndex = null;
            this.velthoric$hasPreviousBodyRot = false;
            return;
        }

        int groundIdx = VxEntityCollisionManager.getGroundSlotIdx(player);
        if (groundIdx < 0) {
            this.velthoric$activeBodyIndex = null;
            this.velthoric$hasPreviousBodyRot = false;
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
        Quat bodyRot = TEMP_JOLT_ROT.get();
        manager.getInterpolator().interpolateRotation(manager.getStore(), groundIdx, partialTicks, bodyRot);

        Quaternionf rotation = TEMP_JOML_ROT.get().set(bodyRot.getX(), bodyRot.getY(), bodyRot.getZ(), bodyRot.getW());

        // Initialize or reset tracking states if the player has transitioned to a different body
        if (this.velthoric$activeBodyIndex == null || this.velthoric$activeBodyIndex != groundIdx || !this.velthoric$hasPreviousBodyRot) {
            this.velthoric$activeBodyIndex = groundIdx;
            this.velthoric$previousBodyRot.set(rotation);
            this.velthoric$hasPreviousBodyRot = true;
            return;
        }

        // Calculate the relative rotation from the previous frame to the current frame:
        // dq = rotation * previousBodyRot^-1
        Quaternionf qPrevInv = this.velthoric$previousBodyRot.conjugate(TEMP_JOML_ROT_2.get());
        Quaternionf dq = rotation.mul(qPrevInv, TEMP_JOML_ROT_3.get());

        // Rotate the forward vector by the relative rotation, then extract the yaw delta.
        Vector3f forward = TEMP_JOML_FWD.get().set(0.0f, 0.0f, 1.0f);
        forward.rotate(dq);
        double yawDelta = Math.toDegrees(Math.atan2(forward.x, forward.z));

        // Apply deadzone filtering to avoid unnecessary updates from floating-point micro-jitter
        if (Math.abs(yawDelta) > 1.0E-4) {
            this.applyYawShift(player, (float) yawDelta);
        }

        this.velthoric$previousBodyRot.set(rotation);
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