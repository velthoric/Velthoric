/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.mixin.impl.debug;

import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.xmx.velthoric.event.api.VxF3ScreenAdditionEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

/**
 * Mixin to intercept gathering information for the client side debug F3 overlay screen.
 *
 * @author xI-Mx-Ix
 */
@Mixin(DebugScreenOverlay.class)
public class MixinDebugScreenOverlay {

    /**
     * Intercepts the return of getGameInformation to invoke the F3 addition event.
     *
     * @param cir the return callback containing the list of active debug info lines
     */
    @Inject(method = "getGameInformation", at = @At("RETURN"))
    private void velthoric$onGetGameInformation(CallbackInfoReturnable<List<String>> cir) {
        List<String> gameInfo = cir.getReturnValue();
        VxF3ScreenAdditionEvent.AddDebugInfo.EVENT.invoker().onAddDebugInfo(new VxF3ScreenAdditionEvent.AddDebugInfo(gameInfo));
    }
}