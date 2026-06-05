/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.api;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.minecraft.server.level.ServerLevel;

/**
 * Event wrapper for server-side level life cycle stages.
 *
 * @author xI-Mx-Ix
 */
public class VxLevelEvent {

    /**
     * Fired when a ServerLevel is being saved.
     */
    public static class Save {
        /**
         * The architectural event loop.
         */
        public static final Event<Listener> EVENT = EventFactory.createLoop();

        /**
         * The server-side level currently saving.
         */
        private final ServerLevel level;

        /**
         * Constructs a new ServerLevel save event.
         *
         * @param level the level being saved
         */
        public Save(ServerLevel level) {
            this.level = level;
        }

        /**
         * Gets the server-side level saving.
         *
         * @return the server level
         */
        public ServerLevel getLevel() {
            return level;
        }

        /**
         * Listener interface for handling level save events.
         */
        @FunctionalInterface
        public interface Listener {
            /**
             * Called when the server level is saving.
             *
             * @param event the saving level event
             */
            void onLevelSave(Save event);
        }
    }

    /**
     * Fired when a ServerLevel is loaded and prepared.
     */
    public static class Load {
        /**
         * The architectural event loop.
         */
        public static final Event<Listener> EVENT = EventFactory.createLoop();

        /**
         * The loaded server-side level.
         */
        private final ServerLevel level;

        /**
         * Constructs a new ServerLevel load event.
         *
         * @param level the loaded level
         */
        public Load(ServerLevel level) {
            this.level = level;
        }

        /**
         * Gets the loaded server-side level.
         *
         * @return the server level
         */
        public ServerLevel getLevel() {
            return level;
        }

        /**
         * Listener interface for handling level load events.
         */
        @FunctionalInterface
        public interface Listener {
            /**
             * Called when the server level is loaded.
             *
             * @param event the loaded level event
             */
            void onLevelLoad(Load event);
        }
    }

    /**
     * Fired when a ServerLevel is about to be unloaded.
     */
    public static class Unload {
        /**
         * The architectural event loop.
         */
        public static final Event<Listener> EVENT = EventFactory.createLoop();

        /**
         * The server level being unloaded.
         */
        private final ServerLevel level;

        /**
         * Constructs a new ServerLevel unload event.
         *
         * @param level the level being unloaded
         */
        public Unload(ServerLevel level) {
            this.level = level;
        }

        /**
         * Gets the server-side level being unloaded.
         *
         * @return the server level
         */
        public ServerLevel getLevel() {
            return level;
        }

        /**
         * Listener interface for handling level unload events.
         */
        @FunctionalInterface
        public interface Listener {
            /**
             * Called when the server level is unloading.
             *
             * @param event the unloading level event
             */
            void onLevelUnload(Unload event);
        }
    }
}