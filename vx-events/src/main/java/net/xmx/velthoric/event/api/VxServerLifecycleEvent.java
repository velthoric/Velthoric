/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.api;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.minecraft.server.MinecraftServer;

/**
 * A container for logical server life cycle events.
 *
 * @author xI-Mx-Ix
 */
public class VxServerLifecycleEvent {

    /**
     * Fired when the logical server is starting.
     */
    public static class Starting {
        /**
         * The architectural event loop.
         */
        public static final Event<Listener> EVENT = EventFactory.createLoop();

        /**
         * The active MinecraftServer instance starting up.
         */
        private final MinecraftServer server;

        /**
         * Constructs a new Starting server event.
         *
         * @param server the server instance starting
         */
        public Starting(MinecraftServer server) {
            this.server = server;
        }

        /**
         * Gets the server instance starting up.
         *
         * @return the server instance
         */
        public MinecraftServer getServer() {
            return server;
        }

        /**
         * Listener interface for handling starting server events.
         */
        @FunctionalInterface
        public interface Listener {
            /**
             * Called when the server is starting up.
             *
             * @param event the starting event details
             */
            void onServerStarting(Starting event);
        }
    }

    /**
     * Fired when the logical server is stopping.
     */
    public static class Stopping {
        /**
         * The architectural event loop.
         */
        public static final Event<Listener> EVENT = EventFactory.createLoop();

        /**
         * The active MinecraftServer instance shutting down.
         */
        private final MinecraftServer server;

        /**
         * Constructs a new Stopping server event.
         *
         * @param server the server instance stopping
         */
        public Stopping(MinecraftServer server) {
            this.server = server;
        }

        /**
         * Gets the server instance shutting down.
         *
         * @return the server instance
         */
        public MinecraftServer getServer() {
            return server;
        }

        /**
         * Listener interface for handling stopping server events.
         */
        @FunctionalInterface
        public interface Listener {
            /**
             * Called when the server is stopping.
             *
             * @param event the stopping event details
             */
            void onServerStopping(Stopping event);
        }
    }
}