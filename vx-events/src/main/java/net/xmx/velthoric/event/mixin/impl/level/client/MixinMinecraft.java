/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.mixin.impl.level.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
/*? if >=1.21.1 {*/
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
/*? }*/
import net.minecraft.client.multiplayer.ClientLevel;
import net.xmx.velthoric.event.api.VxClientLevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting the primary Minecraft client class to manage client side levels loading and unloading.
 *
 * @author xI-Mx-Ix
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    /**
     * The active client level instance.
     */
    @Shadow
    public ClientLevel level;

/*? if >=1.21.1 {*/
    /**
     * Triggers before a level is replaced, firing a client level unload event.
     */
    @Inject(method = "setLevel", at = @At("HEAD"))
    private void velthoric$onSetLevelUnload(ClientLevel level, ReceivingLevelScreen.Reason reason, CallbackInfo ci) {
        if (this.level != null) {
            VxClientLevelEvent.Unload.EVENT.invoker().onLevelUnload(new VxClientLevelEvent.Unload(this.level));
        }
    }
/*? } else {*/
    /*/^*
     * Triggers before a level is replaced, firing a client level unload event.
     ^/
    @Inject(method = "setLevel", at = @At("HEAD"))
    private void velthoric$onSetLevelUnload(ClientLevel newClientLevel, CallbackInfo ci) {
        if (this.level != null) {
            VxClientLevelEvent.Unload.EVENT.invoker().onLevelUnload(new VxClientLevelEvent.Unload(this.level));
        }
    }
*//*? }*/

/*? if >=1.21.1 {*/
    /**
     * Triggers after a new level is set, firing a client level load event.
     */
    @Inject(method = "setLevel", at = @At("TAIL"))
    private void velthoric$onSetLevelLoad(ClientLevel level, ReceivingLevelScreen.Reason reason, CallbackInfo ci) {
        if (level != null) {
            VxClientLevelEvent.Load.EVENT.invoker().onLevelLoad(new VxClientLevelEvent.Load(level));
        }
    }
/*? } else {*/
    /*/^*
     * Triggers after a new level is set, firing a client level load event.
     ^/
    @Inject(method = "setLevel", at = @At("TAIL"))
    private void velthoric$onSetLevelLoad(ClientLevel newClientLevel, CallbackInfo ci) {
        if (newClientLevel != null) {
            VxClientLevelEvent.Load.EVENT.invoker().onLevelLoad(new VxClientLevelEvent.Load(newClientLevel));
        }
    }
*//*? }*/

/*? if >=1.21.1 {*/
    /**
     * Triggers before a client level gets cleared (e.g. upon disconnect), firing a client level unload event.
     *
     * @param screen the active screen
     * @param ci     standard callback info
     */
    @Inject(method = "clearClientLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    private void velthoric$onClearLevelUnload(Screen screen, CallbackInfo ci) {
        if (this.level != null) {
            VxClientLevelEvent.Unload.EVENT.invoker().onLevelUnload(new VxClientLevelEvent.Unload(this.level));
        }
    }
/*? } else {*/
    /*/^*
     * Triggers before a client level gets cleared (e.g. upon disconnect), firing a client level unload event.
     *
     * @param screen the active screen
     * @param ci     standard callback info
     ^/
    @Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    private void velthoric$onClearLevelUnload(Screen screen, CallbackInfo ci) {
        if (this.level != null) {
            VxClientLevelEvent.Unload.EVENT.invoker().onLevelUnload(new VxClientLevelEvent.Unload(this.level));
        }
    }
*//*? }*/
}