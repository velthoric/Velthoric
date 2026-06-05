/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.api;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import org.jetbrains.annotations.Nullable;

/**
 * Container class for client-side player network events.
 * Handles events related to player authentication, logging out, disconnection, and respawn clones.
 *
 * @author xI-Mx-Ix
 */
public class VxClientPlayerNetworkEvent {

    /**
     * Event fired when the client disconnects from a world or server.
     */
    public static class Disconnect {
        /**
         * The loop event instance.
         */
        public static final Event<Listener> EVENT = EventFactory.createLoop();

        /**
         * The client-side level the player was disconnected from.
         */
        private final ClientLevel level;

        /**
         * Constructs a new Disconnect event.
         *
         * @param level the client level being disconnected from
         */
        public Disconnect(ClientLevel level) {
            this.level = level;
        }

        /**
         * Gets the client-side level the player was disconnected from.
         *
         * @return the client level
         */
        public ClientLevel getLevel() {
            return level;
        }

        /**
         * Listener interface for the Disconnect event.
         */
        @FunctionalInterface
        public interface Listener {
            /**
             * Called when the client disconnects.
             *
             * @param event the disconnect event details
             */
            void onClientDisconnect(Disconnect event);
        }
    }

    /**
     * Event fired when the client-side player has successfully completed logging into a server.
     */
    public static class LoggingIn {
        /**
         * The loop event instance.
         */
        public static final Event<Listener> EVENT = EventFactory.createLoop();

        /**
         * The client-side game mode manager.
         */
        private final MultiPlayerGameMode multiPlayerGameMode;

        /**
         * The local player instance.
         */
        private final LocalPlayer player;

        /**
         * The active network connection.
         */
        private final Connection connection;

        /**
         * Constructs a new LoggingIn event.
         *
         * @param multiPlayerGameMode the client multiplayer game mode
         * @param player             the local player instance
         * @param connection         the network connection to the server
         */
        public LoggingIn(final MultiPlayerGameMode multiPlayerGameMode, final LocalPlayer player, final Connection connection) {
            this.multiPlayerGameMode = multiPlayerGameMode;
            this.player = player;
            this.connection = connection;
        }

        /**
         * Gets the multiplayer game mode manager.
         *
         * @return the game mode manager
         */
        public MultiPlayerGameMode getMultiPlayerGameMode() {
            return multiPlayerGameMode;
        }

        /**
         * Gets the client-side player.
         *
         * @return the local player
         */
        public LocalPlayer getPlayer() {
            return player;
        }

        /**
         * Gets the active network connection.
         *
         * @return the network connection
         */
        public Connection getConnection() {
            return connection;
        }

        /**
         * Listener interface for the LoggingIn event.
         */
        @FunctionalInterface
        public interface Listener {
            /**
             * Called when the player has finished logging in.
             *
             * @param event the logging in event details
             */
            void onClientLoggingIn(LoggingIn event);
        }
    }

    /**
     * Event fired immediately before the client-side player starts the logout process.
     */
    public static class LoggingOut {
        /**
         * The loop event instance.
         */
        public static final Event<Listener> EVENT = EventFactory.createLoop();

        /**
         * The client-side game mode manager.
         */
        private final MultiPlayerGameMode multiPlayerGameMode;

        /**
         * The local player instance.
         */
        private final LocalPlayer player;

        /**
         * The active network connection.
         */
        private final Connection connection;

        /**
         * Constructs a new LoggingOut event.
         *
         * @param multiPlayerGameMode the client multiplayer game mode
         * @param player             the local player instance
         * @param connection         the active network connection
         */
        public LoggingOut(@Nullable final MultiPlayerGameMode multiPlayerGameMode, @Nullable final LocalPlayer player, @Nullable final Connection connection) {
            this.multiPlayerGameMode = multiPlayerGameMode;
            this.player = player;
            this.connection = connection;
        }

        /**
         * Gets the multiplayer game mode manager.
         *
         * @return the game mode manager, or null if unavailable
         */
        @Nullable
        public MultiPlayerGameMode getMultiPlayerGameMode() {
            return multiPlayerGameMode;
        }

        /**
         * Gets the client-side player.
         *
         * @return the local player, or null if unavailable
         */
        @Nullable
        public LocalPlayer getPlayer() {
            return player;
        }

        /**
         * Gets the network connection.
         *
         * @return the connection, or null if unavailable
         */
        @Nullable
        public Connection getConnection() {
            return connection;
        }

        /**
         * Listener interface for the LoggingOut event.
         */
        @FunctionalInterface
        public interface Listener {
            /**
             * Called when the player begins logging out.
             *
             * @param event the logging out event details
             */
            void onClientLoggingOut(LoggingOut event);
        }
    }

    /**
     * Event fired when a player respawns or changes dimensions, resulting in a clone being created.
     */
    public static class Clone {
        /**
         * The loop event instance.
         */
        public static final Event<Listener> EVENT = EventFactory.createLoop();

        /**
         * The client-side game mode manager.
         */
        private final MultiPlayerGameMode multiPlayerGameMode;

        /**
         * The previous local player instance.
         */
        private final LocalPlayer oldPlayer;

        /**
         * The new local player instance.
         */
        private final LocalPlayer newPlayer;

        /**
         * The active network connection.
         */
        private final Connection connection;

        /**
         * Constructs a new Clone event.
         *
         * @param multiPlayerGameMode the client multiplayer game mode
         * @param oldPlayer          the previous player instance
         * @param newPlayer          the newly created player instance
         * @param connection         the active network connection
         */
        public Clone(final MultiPlayerGameMode multiPlayerGameMode, final LocalPlayer oldPlayer, final LocalPlayer newPlayer, final Connection connection) {
            this.multiPlayerGameMode = multiPlayerGameMode;
            this.oldPlayer = oldPlayer;
            this.newPlayer = newPlayer;
            this.connection = connection;
        }

        /**
         * Gets the multiplayer game mode manager.
         *
         * @return the game mode manager
         */
        public MultiPlayerGameMode getMultiPlayerGameMode() {
            return multiPlayerGameMode;
        }

        /**
         * Gets the previous player instance.
         *
         * @return the old local player
         */
        public LocalPlayer getOldPlayer() {
            return oldPlayer;
        }

        /**
         * Gets the newly created player instance.
         *
         * @return the new local player
         */
        public LocalPlayer getNewPlayer() {
            return newPlayer;
        }

        /**
         * Gets the network connection.
         *
         * @return the connection
         */
        public Connection getConnection() {
            return connection;
        }

        /**
         * Listener interface for the Clone event.
         */
        @FunctionalInterface
        public interface Listener {
            /**
             * Called when a player clone is created during respawn.
             *
             * @param event the clone event details
             */
            void onClientPlayerClone(Clone event);
        }
    }
}