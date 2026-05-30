/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.network.synchronization;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.xmx.velthoric.core.body.VxBody;
import net.xmx.velthoric.core.body.server.VxServerBodyDataContainer;
import net.xmx.velthoric.core.body.server.VxServerBodyDataStore;
import net.xmx.velthoric.core.body.server.VxServerBodyManager;
import net.xmx.velthoric.core.network.internal.VxNetworkDispatcher;
import net.xmx.velthoric.core.network.synchronization.packet.S2CSynchronizedDataBatchPacket;
import net.xmx.velthoric.init.VxMainClass;
import net.xmx.velthoric.network.VxByteBuf;
import net.xmx.velthoric.network.VxNetworking;

import java.util.Map;
import java.util.UUID;

/**
 * Manages server-side custom data synchronization tasks.
 * This class processes updates received from clients and broadcasts changes to tracking clients.
 *
 * @author xI-Mx-Ix
 */
public class VxServerSyncDataManager {

    /**
     * The default initial size for the thread-local serialization buffers.
     */
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    /**
     * Reusable thread-local buffer to avoid frequent allocations during bulk synchronization.
     */
    private static final ThreadLocal<VxByteBuf> THREAD_LOCAL_BUF = ThreadLocal.withInitial(() ->
            new VxByteBuf(Unpooled.buffer(DEFAULT_BUFFER_SIZE)));

    /**
     * Default constructor.
     */
    public VxServerSyncDataManager() {
    }

    /**
     * Processes a synchronization request sent by a client.
     *
     * @param bodyManager The server-side body manager.
     * @param networkId   The network ID of the body.
     * @param payload     The serialized sync data.
     * @param sender      The player who sent the update.
     */
    public void handleC2SUpdate(VxServerBodyManager bodyManager, int networkId, byte[] payload, ServerPlayer sender) {
        VxServerBodyDataStore dataStore = bodyManager.getDataStore();
        UUID bodyId = dataStore.getIdForNetworkId(networkId);
        if (bodyId == null) {
            return;
        }

        VxBody body = bodyManager.getVxBody(bodyId);
        if (body == null) {
            return;
        }

        try {
            VxByteBuf buf = new VxByteBuf(Unpooled.wrappedBuffer(payload));
            // Delegate logic to entries, which might perform authority checks
            body.getSynchronizedData().readEntriesC2S(buf, body, sender);
        } catch (Exception e) {
            VxMainClass.LOGGER.error("Failed to process C2S sync for body {} from player {}",
                    bodyId, sender.getName().getString(), e);
        }
    }

    /**
     * Scans for bodies with dirty synchronized data and broadcasts updates to tracking players.
     * This method is designed to be called from the network thread to offload serialization.
     *
     * @param bodyManager The server-side body manager.
     * @param dispatcher  The network dispatcher for tracker resolution.
     */
    public void broadcastS2CUpdates(VxServerBodyManager bodyManager, VxNetworkDispatcher dispatcher) {
        VxServerBodyDataStore dataStore = bodyManager.getDataStore();
        IntArrayList dirtyIndices = new IntArrayList();

        // 1. Collect indices of bodies with server-side dirty flags
        VxServerBodyDataContainer c = dataStore.serverCurrent();
        synchronized (dataStore) {
            for (int i = 0; i < dataStore.getCapacity(); i++) {
                if (c.isCustomDataDirty.get(i) != 0) {
                    dirtyIndices.add(i);
                    c.isCustomDataDirty.put(i, (byte) 0);
                }
            }
        }

        if (dirtyIndices.isEmpty()) {
            return;
        }

        Map<ServerPlayer, Map<Integer, byte[]>> playerUpdateMap = new Object2ObjectOpenHashMap<>();
        VxByteBuf serializationBuffer = THREAD_LOCAL_BUF.get();

        // 2. Serialize updates once per body and identify interested players
        for (int index : dirtyIndices) {
            UUID id = dataStore.getIdForIndex(index);
            if (id == null) {
                continue;
            }

            VxBody body = bodyManager.getVxBody(id);
            if (body == null) {
                continue;
            }

            serializationBuffer.clear();
            if (body.writeDirtySyncData(serializationBuffer)) {
                byte[] payload = new byte[serializationBuffer.readableBytes()];
                serializationBuffer.readBytes(payload);

                int netId = body.getNetworkId();
                dispatcher.forEachTrackerForBody(netId, player ->
                        playerUpdateMap.computeIfAbsent(player, p -> new Object2ObjectArrayMap<>())
                                .put(netId, payload)
                );
                body.getSynchronizedData().clearDirty();
            }
        }

        // 3. Dispatch batch packets to players directly from the network thread.
        if (!playerUpdateMap.isEmpty()) {
            playerUpdateMap.forEach((player, data) -> {
                if (!data.isEmpty()) {
                    VxNetworking.sendToPlayer(player, new S2CSynchronizedDataBatchPacket(data));
                }
            });
        }
    }
}