/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.mixin.impl.event.level.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
/*? if >=1.21.1 {*/
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
/*?}*/
import net.minecraft.client.multiplayer.ClientLevel;
import net.xmx.velthoric.event.api.VxClientLevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to fire client-side level load and unload events.
 * Covers both dimension changes (via setLevel) and disconnecting (via clearLevel).
 *
 * @author xI-Mx-Ix
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraft_VxClientLevelEvent {

    @Shadow public ClientLevel level;

    /**
     * Fires the Unload event for the old level right before it gets replaced.
     * This hook is primarily for dimension changes.
     */
    @Inject(method = "setLevel", at = @At("HEAD"))
    /*? if >=1.21.1 {*/
    private void velthoric$onSetLevelUnload(ClientLevel level, ReceivingLevelScreen.Reason reason, CallbackInfo ci) {
    /*?} else {*/
     /*/^*
      * Legacy hook to fire the Unload event for the old level before it is replaced.
      ^/
     private void velthoric$onSetLevelUnload(ClientLevel newClientLevel, CallbackInfo ci) {
    *//*?}*/
        if (this.level != null) {
            VxClientLevelEvent.Unload.EVENT.invoker().onLevelUnload(new VxClientLevelEvent.Unload(this.level));
        }
    }

    /**
     * Fires the Load event for the new level after it has been set.
     */
    @Inject(method = "setLevel", at = @At("TAIL"))
    /*? if >=1.21.1 {*/
    private void velthoric$onSetLevelLoad(ClientLevel level, ReceivingLevelScreen.Reason reason, CallbackInfo ci) {
        if (level != null) {
            VxClientLevelEvent.Load.EVENT.invoker().onLevelLoad(new VxClientLevelEvent.Load(level));
        }
    /*?} else {*/
     /*/^*
      * Legacy hook to fire the Load event for the new level after it has been set.
      ^/
     private void velthoric$onSetLevelLoad(ClientLevel newClientLevel, CallbackInfo ci) {
         if (newClientLevel != null) {
             VxClientLevelEvent.Load.EVENT.invoker().onLevelLoad(new VxClientLevelEvent.Load(newClientLevel));
         }
    *//*?}*/
    }

    /*? if >=1.21.1 {*/
    @Inject(method = "clearClientLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    /*?} else {*/
    /*@Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    *//*?}*/
    private void velthoric$onClearLevelUnload(Screen screen, CallbackInfo ci) {
        if (this.level != null) {
            VxClientLevelEvent.Unload.EVENT.invoker().onLevelUnload(new VxClientLevelEvent.Unload(this.level));
        }
    }
}