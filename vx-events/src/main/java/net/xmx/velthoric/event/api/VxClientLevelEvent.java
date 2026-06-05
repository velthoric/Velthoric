/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.api;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Event wrapper for client-side level loading and unloading.
 * These events trigger when world loading occurs, such as during dimension changes or disconnects.
 *
 * @author xI-Mx-Ix
 */
@Environment(EnvType.CLIENT)
public class VxClientLevelEvent {

    /**
     * Fired when a ClientLevel is loaded and set on the Minecraft client.
     * This occurs on world join and dimension changes.
     */
    public static class Load {
        /**
         * The architectural event loop.
         */
        public static final Event<Listener> EVENT = EventFactory.createLoop();

        /**
         * The loaded client level.
         */
        private final ClientLevel level;

        /**
         * Constructs a new level load event.
         *
         * @param level the loaded client level
         */
        public Load(ClientLevel level) {
            this.level = level;
        }

        /**
         * Gets the client level associated with this event.
         *
         * @return the client level
         */
        public ClientLevel getLevel() {
            return level;
        }

        /**
         * Listener interface for handling level load events.
         */
        @FunctionalInterface
        public interface Listener {
            /**
             * Called when a client-side level is loaded.
             *
             * @param event the level load event
             */
            void onLevelLoad(Load event);
        }
    }

    /**
     * Fired when a ClientLevel is about to be unloaded.
     * This occurs just before a new level is set (for example, on disconnect or dimension changes).
     */
    public static class Unload {
        /**
         * The architectural event loop.
         */
        public static final Event<Listener> EVENT = EventFactory.createLoop();

        /**
         * The client level being unloaded.
         */
        private final ClientLevel level;

        /**
         * Constructs a new level unload event.
         *
         * @param level the client level being unloaded
         */
        public Unload(ClientLevel level) {
            this.level = level;
        }

        /**
         * Gets the client level associated with this event.
         *
         * @return the client level
         */
        public ClientLevel getLevel() {
            return level;
        }

        /**
         * Listener interface for handling level unload events.
         */
        @FunctionalInterface
        public interface Listener {
            /**
             * Called when a client-side level is unloaded.
             *
             * @param event the level unload event
             */
            void onLevelUnload(Unload event);
        }
    }
}