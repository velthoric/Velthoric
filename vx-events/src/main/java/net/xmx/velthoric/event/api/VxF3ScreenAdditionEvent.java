/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.api;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import java.util.List;

/**
 * Event wrapper for adding custom debug text to the F3 overlay screen.
 *
 * @author xI-Mx-Ix
 */
public class VxF3ScreenAdditionEvent {

    /**
     * Fired when the debug screen text is being gathered.
     * Allows adding custom lines to the F3 overlay.
     */
    public static class AddDebugInfo {
        /**
         * The architectural event loop.
         */
        public static final Event<Listener> EVENT = EventFactory.createLoop();

        /**
         * The list of text lines that will be displayed in the debug screen.
         */
        private final List<String> infoList;

        /**
         * Constructs a new AddDebugInfo event.
         *
         * @param infoList the list of debug information strings
         */
        public AddDebugInfo(List<String> infoList) {
            this.infoList = infoList;
        }

        /**
         * Gets the list of debug info lines.
         *
         * @return the debug info list
         */
        public List<String> getInfoList() {
            return infoList;
        }

        /**
         * Listener interface for handling debug info addition events.
         */
        @FunctionalInterface
        public interface Listener {
            /**
             * Called when debug information is requested.
             *
             * @param event the debug info event
             */
            void onAddDebugInfo(AddDebugInfo event);
        }
    }
}