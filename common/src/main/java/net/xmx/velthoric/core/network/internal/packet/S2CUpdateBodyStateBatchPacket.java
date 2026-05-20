/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.network.internal.packet;

import com.github.luben.zstd.Zstd;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.world.level.ChunkPos;
import net.xmx.velthoric.network.IVxNetPacket;
import net.xmx.velthoric.network.VxByteBuf;
import net.xmx.velthoric.core.body.client.VxClientBodyDataContainer;
import net.xmx.velthoric.core.body.client.VxClientBodyDataStore;
import net.xmx.velthoric.core.body.client.VxClientBodyManager;

import java.nio.ByteBuffer;

/**
 * A compressed raw-data packet for synchronizing high-frequency body state updates.
 * <p>
 * <b>Zero-Allocation / Zero-Copy Design:</b>
 * On the server, this packet simply holds a reference to a Pooled Direct ByteBuf containing Zstd-compressed data.
 * On the client, the data is decompressed directly from the network buffer into a reusable thread-local buffer,
 * and then applied directly to the Structure-of-Arrays data store.
 * <p>
 * This eliminates the creation of thousands of temporary objects (float[], Packet objects, wrappers) per tick.
 * <p>
 * <b>Adaptive Delay:</b> The packet handler feeds arrival timestamps to the interpolator
 * to enable dynamic delay calculation based on actual network conditions.
 *
 * @author xI-Mx-Ix
 */
public class S2CUpdateBodyStateBatchPacket implements IVxNetPacket {

    /**
     * ThreadLocal buffer for decompression on the client to avoid repeated allocations.
     * Sized at 512KB to handle dense chunk updates.
     */
    private static final ThreadLocal<ByteBuffer> DECOMPRESSION_BUFFER = ThreadLocal.withInitial(() -> ByteBuffer.allocateDirect(512 * 1024));

    /**
     * The compressed binary payload. On Server, this is a Pooled Direct Buffer. On Client, it's a slice of the network buffer.
     */
    private final ByteBuf data;

    /**
     * Server-side constructor wrapping a pre-built compressed payload.
     * Takes ownership of the passed ByteBuf (it must be released via {@link #release()}).
     *
     * @param data The compressed Zstd data blob.
     */
    public S2CUpdateBodyStateBatchPacket(ByteBuf data) {
        this.data = data;
    }

    /**
     * Encodes the packet into the network buffer.
     * Writes the length prefix followed by the compressed bytes.
     * Releases the buffer after writing.
     *
     * @param buf The output buffer.
     */
    @Override
    public void encode(VxByteBuf buf) {
        // Reset index so multiple encode calls (broadcasting) send the same data
        this.data.readerIndex(0);
        int length = this.data.readableBytes();
        buf.writeVarInt(length);
        buf.writeBytes(this.data);
    }

    /**
     * Decodes the packet from the network buffer.
     * Does NOT allocate a byte array, but returns a retained slice/copy in a ByteBuf.
     *
     * @param buf The input buffer.
     * @return A populated packet instance.
     */
    public static S2CUpdateBodyStateBatchPacket decode(VxByteBuf buf) {
        int length = buf.readVarInt();
        // Read into a new ByteBuf. This makes a copy from the underlying buffer,
        // which is unavoidable with FriendlyByteBuf if we want the data to survive the handler scope.
        ByteBuf copied = buf.readBytes(length);
        return new S2CUpdateBodyStateBatchPacket(copied);
    }

    /**
     * Processes the decoded packet on the client's main thread and updates the data store.
     * Uses Zero-Copy decompression directly into the DataStore arrays.
     * <p>
     * Additionally feeds the packet arrival timestamp to the interpolator's
     * adaptive delay system for dynamic delay calculation.
     *
     * @param context The packet context.
     */
    @Override
    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> {
            try {
                VxClientBodyManager manager = VxClientBodyManager.getInstance();
                VxClientBodyDataStore store = manager.getStore();

                // 1. Prepare Decompression
                // Obtain NIO buffer from Netty ByteBuf without copying
                ByteBuffer compressedNio = this.data.nioBuffer();

                // Determine required size for the output buffer
                long uncompressedSize = Zstd.decompressedSize(compressedNio);

                if (Zstd.isError(uncompressedSize)) {
                    // Corruption or empty payload due to broadcast issue
                    return;
                }

                // Acquire and resize thread-local buffer if necessary
                ByteBuffer targetBuf = DECOMPRESSION_BUFFER.get();
                if (targetBuf.capacity() < uncompressedSize) {
                    targetBuf = ByteBuffer.allocateDirect((int) uncompressedSize);
                    DECOMPRESSION_BUFFER.set(targetBuf);
                }

                // Reset buffer state before writing
                targetBuf.clear();

                // 2. Decompress (Direct Memory -> Direct Memory)
                // Writes the raw physics data directly into the reusable buffer
                Zstd.decompressDirectByteBuffer(targetBuf, 0, (int) uncompressedSize, compressedNio, 0, compressedNio.remaining());

                // Set the limit to the actual data size so the wrapped ByteBuf behaves correctly
                targetBuf.position(0);
                targetBuf.limit((int) uncompressedSize);

                // 3. Read directly from the decompressed buffer
                // Wrapping it in a ByteBuf allows easy reading of primitives without manual offsets
                ByteBuf db = Unpooled.wrappedBuffer(targetBuf);

                int count = db.readInt();
                long timestamp = db.readLong();
                long chunkPosLong = db.readLong();

                ChunkPos cp = new ChunkPos(chunkPosLong);
                double baseX = cp.getMinBlockX();
                double baseY = context.getPlayer().level().getMinBuildHeight();
                double baseZ = cp.getMinBlockZ();

                // Feed clock sync sample
                long clientNow = manager.getClock().getGameTimeNanos();
                manager.addClockSyncSample(timestamp - clientNow);

                // Feed adaptive delay system with packet arrival time
                manager.getInterpolator().onPacketReceived(clientNow);

                // 4. Update Data Store (Zero Object Allocation)
                VxClientBodyDataContainer c = store.clientCurrent();
                for (int i = 0; i < count; i++) {
                    int netId = db.readInt();
                    Integer idx = store.getIndexForNetworkId(netId);

                    // If the body is not tracked locally (e.g., desync or unloaded), skip the data stream
                    // to maintain correct buffer offsets for subsequent bodies.
                    if (idx == null) {
                        db.skipBytes(12); // Position (3 floats * 4 bytes)
                        db.skipBytes(16); // Rotation (4 floats * 4 bytes)

                        boolean isActive = db.readBoolean(); // Must read the activity flag
                        if (isActive) {
                            db.skipBytes(12); // Linear velocity (3 floats * 4 bytes)
                            db.skipBytes(12); // Angular velocity (3 floats * 4 bytes)
                        }
                        continue;
                    }

                    int index = idx; // Unbox index once

                    // Bounds check for race condition during container resize
                    if (index >= c.getCapacity()) {
                        db.skipBytes(12); // Position
                        db.skipBytes(16); // Rotation
                        if (db.readBoolean()) { // isActive
                            db.skipBytes(12); // Linear velocity
                            db.skipBytes(12); // Angular velocity
                        }
                        continue;
                    }

                    // Cycle history states (current -> old)
                    c.state0_timestamp.put(index, c.state1_timestamp.get(index));
                    c.state0_posX.put(index, c.state1_posX.get(index));
                    c.state0_posY.put(index, c.state1_posY.get(index));
                    c.state0_posZ.put(index, c.state1_posZ.get(index));
                    c.state0_rotX.put(index, c.state1_rotX.get(index));
                    c.state0_rotY.put(index, c.state1_rotY.get(index));
                    c.state0_rotZ.put(index, c.state1_rotZ.get(index));
                    c.state0_rotW.put(index, c.state1_rotW.get(index));
                    c.state0_isActive.put(index, c.state1_isActive.get(index));

                    // Read New State into state1
                    c.state1_timestamp.put(index, timestamp);
                    c.state1_posX.put(index, baseX + db.readFloat());
                    c.state1_posY.put(index, baseY + db.readFloat());
                    c.state1_posZ.put(index, baseZ + db.readFloat());
                    c.state1_rotX.put(index, db.readFloat());
                    c.state1_rotY.put(index, db.readFloat());
                    c.state1_rotZ.put(index, db.readFloat());
                    c.state1_rotW.put(index, db.readFloat());

                    boolean active = db.readBoolean();
                    c.state1_isActive.put(index, (byte) (active ? 1 : 0));

                    if (active) {
                        c.state1_velX.put(index, db.readFloat());
                        c.state1_velY.put(index, db.readFloat());
                        c.state1_velZ.put(index, db.readFloat());
                        c.state0_angVelX.put(index, c.state1_angVelX.get(index));
                        c.state0_angVelY.put(index, c.state1_angVelY.get(index));
                        c.state0_angVelZ.put(index, c.state1_angVelZ.get(index));
                        c.state1_angVelX.put(index, db.readFloat());
                        c.state1_angVelY.put(index, db.readFloat());
                        c.state1_angVelZ.put(index, db.readFloat());
                    }

                    // Update culling position for renderer frustum checks
                    c.lastKnownPosition[index].set(c.state1_posX.get(index), c.state1_posY.get(index), c.state1_posZ.get(index));
                }

            } finally {
                // Always release the pooled network buffer on client side
                this.release();
            }
        });
    }

    /**
     * Releases the compressed payload buffer.
     */
    @Override
    public void release() {
        if (this.data.refCnt() > 0) {
            this.data.release();
        }
    }
}