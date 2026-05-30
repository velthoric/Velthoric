/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.network.synchronization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.xmx.velthoric.core.body.VxBody;
import net.xmx.velthoric.core.body.client.VxClientBodyDataStore;
import net.xmx.velthoric.core.body.client.VxClientBodyManager;
import net.xmx.velthoric.core.network.synchronization.packet.C2SSynchronizedDataBatchPacket;
import net.xmx.velthoric.init.VxMainClass;
import net.xmx.velthoric.network.VxByteBuf;
import net.xmx.velthoric.network.VxNetworking;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Manages client-side custom data synchronization tasks.
 * This class tracks client-dirty bodies and serializes their updates to send to the server.
 * It also processes incoming updates from the server.
 *
 * @author xI-Mx-Ix
 */
public class VxClientSyncDataManager {

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
     * Set of network IDs of bodies that have dirty C2S data on the client.
     */
    private final IntSet dirtyBodiesC2S = new IntOpenHashSet();

    /**
     * Default constructor.
     */
    public VxClientSyncDataManager() {
    }

    /**
     * Marks a body as dirty on the client, notifying the system that its custom data
     * should be sent to the server in the next tick.
     *
     * @param body The body whose custom data has changed.
     */
    public synchronized void markDirtyC2S(VxBody body) {
        this.dirtyBodiesC2S.add(body.getNetworkId());
    }

    /**
     * Handles the removal of a body on the client by cleaning up sync trackers.
     *
     * @param body The body instance being removed.
     */
    public synchronized void onBodyRemoved(VxBody body) {
        this.dirtyBodiesC2S.remove(body.getNetworkId());
    }

    /**
     * Clears all client-side synchronization state.
     */
    public synchronized void clear() {
        this.dirtyBodiesC2S.clear();
    }

    /**
     * Logic executed during the client game tick.
     * Scans for dirty bodies, serializes their changes, and dispatches them to the server.
     *
     * @param manager The client body manager.
     * @param store   The client body data store.
     */
    public void tick(VxClientBodyManager manager, VxClientBodyDataStore store) {
        if (dirtyBodiesC2S.isEmpty()) {
            return;
        }

        Map<Integer, byte[]> batchUpdates = new Object2ObjectArrayMap<>();
        VxByteBuf serializationBuffer = THREAD_LOCAL_BUF.get();

        synchronized (this) {
            Iterator<Integer> it = dirtyBodiesC2S.iterator();
            while (it.hasNext()) {
                int netId = it.next();
                Integer index = store.getIndexForNetworkId(netId);

                // Body might have been removed or is no longer tracked
                if (index == null) {
                    it.remove();
                    continue;
                }

                UUID id = store.getIdForIndex(index);
                VxBody body = manager.getVxBody(id);
                if (body == null) {
                    it.remove();
                    continue;
                }

                serializationBuffer.clear();
                // Serialize only dirty entries for this specific body
                if (body.writeDirtySyncData(serializationBuffer)) {
                    byte[] payload = new byte[serializationBuffer.readableBytes()];
                    serializationBuffer.readBytes(payload);
                    batchUpdates.put(netId, payload);
                    body.getSynchronizedData().clearDirty();
                }
                it.remove();
            }
        }

        if (!batchUpdates.isEmpty()) {
            VxNetworking.sendToServer(new C2SSynchronizedDataBatchPacket(batchUpdates));
        }
    }

    /**
     * Processes an incoming custom data update from the server.
     *
     * @param manager   The client-side body manager.
     * @param networkId The network ID of the body being updated.
     * @param payload   The raw data containing the synchronized entries.
     */
    public void applyS2CUpdate(VxClientBodyManager manager, int networkId, ByteBuf payload) {
        VxClientBodyDataStore store = manager.getStore();
        Integer index = store.getIndexForNetworkId(networkId);
        if (index == null) {
            return;
        }

        UUID id = store.getIdForIndex(index);
        if (id == null) {
            return;
        }

        VxBody body = manager.getVxBody(id);
        if (body != null) {
            try {
                // Apply the received entries to the body's synchronized data container
                body.getSynchronizedData().readEntries(new VxByteBuf(payload), body);
            } catch (Exception e) {
                VxMainClass.LOGGER.error("Failed to apply S2C synchronized data for body {}", id, e);
            }
        }
    }
}