/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.mixin.impl.mounting.input;

//? if >=26.1 {
/*import net.minecraft.world.entity.player.Input;
*///? } else {
import net.minecraft.client.player.Input;
//? }
import net.minecraft.client.player.LocalPlayer;
import net.xmx.velthoric.core.mounting.entity.VxMountingEntity;
import net.xmx.velthoric.core.mounting.input.C2SMountInputPacket;
import net.xmx.velthoric.core.mounting.input.VxMountInput;
import net.xmx.velthoric.network.VxNetworking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into the local player logic to capture and transmit custom mounting inputs
 * when the player is mounting a physics entity.
 *
 * @author xI-Mx-Ix
 */
@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer {

    //? if >=26.1 {
    /*@Shadow
    private Input lastSentInput;
    *///? } else {
    @Shadow
    public Input input;
    //?}
    @Unique
    private VxMountInput velthoric_lastRideInput = null;

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void velthoric_handleRidingInput(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;

        if (player.getVehicle() instanceof VxMountingEntity) {
            // Backward (-1.0), Forward (+1.0)
            float forward = 0.0f;

            // Left (-1.0), Right (+1.0)
            float right = 0.0f;

            // Action Flags Bitmask
            int flags = 0;
            // Handbrake = Jump

            //? if >=26.1 {
            /*if (lastSentInput.backward()) forward -= 1.0f;
            if (lastSentInput.forward()) forward += 1.0f;
            if (lastSentInput.left()) right -= 1.0f;
            if (lastSentInput.right()) right += 1.0f;
            if (lastSentInput.jump()) flags |= VxMountInput.FLAG_HANDBRAKE;
            *///? } else {
            if (input.up) forward += 1.0f;
            if (input.down) forward -= 1.0f;
            if (input.left) right -= 1.0f;
            if (input.right) right += 1.0f;
            if (input.jumping) flags |= VxMountInput.FLAG_HANDBRAKE;
            //?}

            VxMountInput currentInput = new VxMountInput(forward, right, flags);

            // 4. Send packet only if input has changed to save bandwidth
            if (!currentInput.equals(this.velthoric_lastRideInput)) {
                VxNetworking.sendToServer(new C2SMountInputPacket(currentInput));
                this.velthoric_lastRideInput = currentInput;
            }
        } else {
            // Reset state when not riding to ensure server clears inputs
            if (this.velthoric_lastRideInput != null && !this.velthoric_lastRideInput.equals(VxMountInput.NEUTRAL)) {
                VxNetworking.sendToServer(new C2SMountInputPacket(VxMountInput.NEUTRAL));
            }
            this.velthoric_lastRideInput = null;
        }
    }
}