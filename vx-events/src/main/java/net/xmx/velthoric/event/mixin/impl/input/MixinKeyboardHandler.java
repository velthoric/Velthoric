/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.mixin.impl.input;

import dev.architectury.event.EventResult;
import net.minecraft.client.KeyboardHandler;
import net.xmx.velthoric.event.api.VxKeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept native keyboard events within the client keyboard handler.
 *
 * @author xI-Mx-Ix
 */
@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {

    /**
     * Intercepts keyboard input to fire the custom key input event, allowing cancellation.
     *
     * @param window    the native window handle
     * @param key       the physical key code
     * @param scanCode  the key scan code
     * @param action    the action type
     * @param modifiers active key modifiers
     * @param ci        the callback info
     */
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void velthoric$onKeyPress(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        VxKeyEvent event = new VxKeyEvent(window, key, scanCode, action, modifiers);
        EventResult result = VxKeyEvent.EVENT.invoker().onKey(event);
        if (result.isFalse()) {
            ci.cancel();
        }
    }
}