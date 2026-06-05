/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.mixin.impl.server;

import net.minecraft.server.level.ServerLevel;
import net.xmx.velthoric.event.api.VxLevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to handle server level persistence save events.
 *
 * @author xI-Mx-Ix
 */
@Mixin(ServerLevel.class)
public class MixinServerLevel {

    /**
     * Intercepts level saving and triggers the save event.
     *
     * @param ci standard callback info
     */
    @Inject(method = "save", at = @At("RETURN"))
    private void velthoric$onSave(CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        VxLevelEvent.Save.EVENT.invoker().onLevelSave(new VxLevelEvent.Save(level));
    }
}