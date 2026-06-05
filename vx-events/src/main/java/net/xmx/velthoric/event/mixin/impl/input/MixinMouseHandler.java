/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.mixin.impl.input;

import dev.architectury.event.EventResult;
import net.minecraft.client.MouseHandler;
import net.xmx.velthoric.event.api.VxMouseEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept client side mouse events such as clicks, scrolling, and turning.
 *
 * @author xI-Mx-Ix
 */
@Mixin(MouseHandler.class)
public class MixinMouseHandler {

    /**
     * Shadow reference to the accumulated horizontal mouse movement.
     */
    @Shadow 
    private double accumulatedDX;

    /**
     * Shadow reference to the accumulated vertical mouse movement.
     */
    @Shadow 
    private double accumulatedDY;

    /**
     * Intercepts raw mouse button presses to fire custom press events.
     *
     * @param window the native window pointer
     * @param button the triggered mouse button code
     * @param action the action code
     * @param mods   active modifiers
     * @param ci     the callback info
     */
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void velthoric$onMousePress(long window, int button, int action, int mods, CallbackInfo ci) {
        VxMouseEvent.Press event = new VxMouseEvent.Press(window, button, action, mods);
        EventResult result = VxMouseEvent.Press.EVENT.invoker().onMousePress(event);
        if (result.isFalse()) {
            ci.cancel();
        }
    }

    /**
     * Intercepts mouse scrolling actions to fire custom scroll events.
     *
     * @param window     the native window pointer
     * @param horizontal the horizontal offset
     * @param vertical   the vertical offset
     * @param ci         the callback info
     */
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void velthoric$onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        VxMouseEvent.Scroll event = new VxMouseEvent.Scroll(window, horizontal, vertical);
        EventResult result = VxMouseEvent.Scroll.EVENT.invoker().onMouseScroll(event);
        if (result.isFalse()) {
            ci.cancel();
        }
    }

    /**
     * Intercepts player view rotations to fire custom mouse turning events.
     *
     * @param ci the callback info
     */
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void velthoric$onPlayerTurn(CallbackInfo ci) {
        if (this.accumulatedDX != 0.0D || this.accumulatedDY != 0.0D) {
            VxMouseEvent.Turn event = new VxMouseEvent.Turn(this.accumulatedDX, this.accumulatedDY);
            EventResult result = VxMouseEvent.Turn.EVENT.invoker().onPlayerTurn(event);
            if (result.isFalse()) {
                ci.cancel();
            }
        }
    }
}