/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.mixin.impl.render;

/*? if >=1.21.1 {*/
import net.minecraft.client.DeltaTracker;
/*? }*/
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.xmx.velthoric.event.api.VxClientRenderHudEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to hook into the client HUD rendering and fire the Velthoric render HUD event.
 *
 * @author xI-Mx-Ix
 */
@Mixin(Gui.class)
public class MixinGui {

/*? if >=1.21.1 {*/
    /**
     * Inject at the tail of the render method to invoke the HUD render event.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void velthoric$onRenderHud(
            GuiGraphics guiGraphics,
            DeltaTracker deltaTracker,
            CallbackInfo ci) {
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        VxClientRenderHudEvent.EVENT.invoker().onRenderHud(
                new VxClientRenderHudEvent(guiGraphics, partialTick)
        );
    }
/*? } else {*/
    /*/^*
     * Inject at the tail of the render method to invoke the HUD render event.
     ^/
    @Inject(method = "render", at = @At("TAIL"))
    private void velthoric$onRenderHud(
            GuiGraphics guiGraphics,
            float partialTick,
            CallbackInfo ci) {
        VxClientRenderHudEvent.EVENT.invoker().onRenderHud(
                new VxClientRenderHudEvent(guiGraphics, partialTick)
        );
    }
*//*? }*/
}