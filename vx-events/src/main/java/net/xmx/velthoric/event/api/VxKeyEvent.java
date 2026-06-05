/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.api;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.architectury.event.EventResult;

/**
 * Fired when a keyboard key is pressed, released, or repeats.
 * This event is fired on the client side.
 * Returning EventResult.interruptFalse() will cancel the original key press action.
 *
 * @author xI-Mx-Ix
 */
public class VxKeyEvent {
    /**
     * The architectural event with result handling.
     */
    public static final Event<Listener> EVENT = EventFactory.createEventResult();

    /**
     * The native GLFW window handle where the key input occurred.
     */
    private final long window;

    /**
     * The GLFW key code (for example, GLFW.GLFW_KEY_E).
     */
    private final int key;

    /**
     * The system-specific scan code of the key.
     */
    private final int scanCode;

    /**
     * The action type (for example, GLFW.GLFW_PRESS, GLFW.GLFW_RELEASE, or GLFW.GLFW_REPEAT).
     */
    private final int action;

    /**
     * A bitfield describing which modifier keys were held down.
     */
    private final int modifiers;

    /**
     * Constructs a new VxKeyEvent instance.
     *
     * @param window    the native window handle
     * @param key       the key code
     * @param scanCode  the key scancode
     * @param action    the action type
     * @param modifiers active key modifiers
     */
    public VxKeyEvent(long window, int key, int scanCode, int action, int modifiers) {
        this.window = window;
        this.key = key;
        this.scanCode = scanCode;
        this.action = action;
        this.modifiers = modifiers;
    }

    /**
     * Gets the GLFW window handle.
     *
     * @return the native window pointer
     */
    public long getWindow() {
        return window;
    }

    /**
     * Gets the key code representation.
     *
     * @return the keyboard key code
     */
    public int getKey() {
        return key;
    }

    /**
     * Gets the scan code representation.
     *
     * @return the physical key scan code
     */
    public int getScanCode() {
        return scanCode;
    }

    /**
     * Gets the action code representation.
     *
     * @return the action code
     */
    public int getAction() {
        return action;
    }

    /**
     * Gets the modifiers representation.
     *
     * @return the bitfield representing modifier keys
     */
    public int getModifiers() {
        return modifiers;
    }

    /**
     * Listener interface for handling keyboard key events.
     */
    @FunctionalInterface
    public interface Listener {
        /**
         * Called when a key event occurs.
         *
         * @param event the key event context
         * @return the outcome result of the key event
         */
        EventResult onKey(VxKeyEvent event);
    }
}