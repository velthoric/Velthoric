/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.api;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.architectury.event.EventResult;

/**
 * A container for client-side mouse input events.
 *
 * @author xI-Mx-Ix
 */
public class VxMouseEvent {

    /**
     * Fired when a mouse button is pressed or released.
     * Returning EventResult.interruptFalse() will cancel the original mouse press action.
     */
    public static class Press {
        /**
         * The architectural event with result handling.
         */
        public static final Event<Listener> EVENT = EventFactory.createEventResult();

        /**
         * The native GLFW window handle where the input occurred.
         */
        private final long window;

        /**
         * The mouse button code that was triggered.
         */
        private final int button;

        /**
         * The action type (for example, GLFW.GLFW_PRESS or GLFW.GLFW_RELEASE).
         */
        private final int action;

        /**
         * Active keyboard modifiers held down during input.
         */
        private final int mods;

        /**
         * Constructs a new mouse press event.
         *
         * @param window the native window handle
         * @param button the mouse button
         * @param action the action type
         * @param mods   active keyboard modifiers
         */
        public Press(long window, int button, int action, int mods) {
            this.window = window;
            this.button = button;
            this.action = action;
            this.mods = mods;
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
         * Gets the mouse button code.
         *
         * @return the button code
         */
        public int getButton() {
            return button;
        }

        /**
         * Gets the action code.
         *
         * @return the action code
         */
        public int getAction() {
            return action;
        }

        /**
         * Gets the active modifiers.
         *
         * @return the keyboard modifier bits
         */
        public int getMods() {
            return mods;
        }

        /**
         * Listener interface for handling mouse press events.
         */
        @FunctionalInterface
        public interface Listener {
            /**
             * Called when a mouse button is pressed or released.
             *
             * @param event the mouse press event
             * @return the result of the event handling
             */
            EventResult onMousePress(Press event);
        }
    }

    /**
     * Fired when the mouse wheel is scrolled.
     * Returning EventResult.interruptFalse() will cancel the original mouse scroll action.
     */
    public static class Scroll {
        /**
         * The architectural event with result handling.
         */
        public static final Event<Listener> EVENT = EventFactory.createEventResult();

        /**
         * The native GLFW window handle where the scroll occurred.
         */
        private final long window;

        /**
         * The horizontal scroll distance offset.
         */
        private final double horizontal;

        /**
         * The vertical scroll distance offset.
         */
        private final double vertical;

        /**
         * Constructs a new mouse scroll event.
         *
         * @param window     the native window handle
         * @param horizontal the horizontal offset
         * @param vertical   the vertical offset
         */
        public Scroll(long window, double horizontal, double vertical) {
            this.window = window;
            this.horizontal = horizontal;
            this.vertical = vertical;
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
         * Gets the horizontal scroll distance.
         *
         * @return the horizontal scroll value
         */
        public double getHorizontal() {
            return horizontal;
        }

        /**
         * Gets the vertical scroll distance.
         *
         * @return the vertical scroll value
         */
        public double getVertical() {
            return vertical;
        }

        /**
         * Listener interface for handling mouse scroll events.
         */
        @FunctionalInterface
        public interface Listener {
            /**
             * Called when the mouse scroll wheel is used.
             *
             * @param event the scroll event
             * @return the result of the event handling
             */
            EventResult onMouseScroll(Scroll event);
        }
    }

    /**
     * Fired when the player view is rotated via the mouse, before the rotation values are updated.
     * Returning EventResult.interruptFalse() will prevent the rotation change.
     */
    public static class Turn {
        /**
         * The architectural event with result handling.
         */
        public static final Event<Listener> EVENT = EventFactory.createEventResult();

        /**
         * The delta movement on the X axis.
         */
        private final double dx;

        /**
         * The delta movement on the Y axis.
         */
        private final double dy;

        /**
         * Constructs a new mouse turn event.
         *
         * @param dx the horizontal change
         * @param dy the vertical change
         */
        public Turn(double dx, double dy) {
            this.dx = dx;
            this.dy = dy;
        }

        /**
         * Gets the delta movement on the horizontal axis.
         *
         * @return the X axis delta
         */
        public double getDx() {
            return dx;
        }

        /**
         * Gets the delta movement on the vertical axis.
         *
         * @return the Y axis delta
         */
        public double getDy() {
            return dy;
        }

        /**
         * Listener interface for handling player mouse turning.
         */
        @FunctionalInterface
        public interface Listener {
            /**
             * Called when the mouse rotates the view.
             *
             * @param event the turn event details
             * @return the result of the turn event
             */
            EventResult onPlayerTurn(Turn event);
        }
    }
}