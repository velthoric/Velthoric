/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.body.server;

import com.github.stephengold.joltjni.enumerate.EActivation;
import com.github.stephengold.joltjni.enumerate.EBodyType;
import com.github.stephengold.joltjni.enumerate.EMotionType;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.xmx.velthoric.core.body.VxBodyDataContainer;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.IntBuffer;
import java.nio.ByteOrder;

/**
 * Server-specific container for physics body data.
 *
 * @author xI-Mx-Ix
 */
public class VxServerBodyDataContainer extends VxBodyDataContainer {

    /**
     * The Jolt physics body type (e.g. Rigid, Soft, Fluid).
     */
    public final EBodyType[] bodyType;
    /**
     * The Jolt motion type (Static, Kinematic, Dynamic).
     */
    public final EMotionType[] motionType;
    /**
     * The Jolt activation state (Activate, DontActivate).
     */
    public final EActivation[] activation;
    /**
     * A long representation of the {@link net.minecraft.world.level.ChunkPos} the body resides in.
     */
    public final LongBuffer chunkKey;
    /**
     * The unique network ID assigned for server-client synchronization.
     */
    public final IntBuffer networkId;

    /**
     * Whether the transform (pos/rot) has changed and needs to be broadcast.
     */
    public final ByteBuffer isTransformDirty;
    /**
     * Whether the vertex data has changed and needs to be broadcast.
     */
    public final ByteBuffer isVertexDataDirty;
    /**
     * Whether custom behavior data has changed and needs to be broadcast.
     */
    public final ByteBuffer isCustomDataDirty;
    /**
     * Whether the collision shape has changed and needs to be broadcast.
     */
    public final ByteBuffer isShapeDirty;
    /**
     * A set of all indices currently marked as dirty for the next network tick.
     */
    public final IntSet dirtyIndices;
    /**
     * Last system time (ms) when this body's network state was updated.
     */
    public final LongBuffer lastUpdateTimestamp;

    /**
     * Initializes a new server-side container with specialized tracking arrays.
     * All arrays are pre-allocated and dirty tracking systems are initialized.
     *
     * @param capacity The maximum number of bodies.
     */
    public VxServerBodyDataContainer(int capacity) {
        super(capacity);
        this.bodyType = new EBodyType[capacity];
        this.motionType = new EMotionType[capacity];
        this.activation = new EActivation[capacity];
        this.chunkKey = ByteBuffer.allocateDirect(capacity * Long.BYTES).order(ByteOrder.nativeOrder()).asLongBuffer();
        this.networkId = ByteBuffer.allocateDirect(capacity * Integer.BYTES).order(ByteOrder.nativeOrder()).asIntBuffer();
        this.isTransformDirty = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
        this.isVertexDataDirty = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
        this.isCustomDataDirty = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
        this.isShapeDirty = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
        this.dirtyIndices = new IntOpenHashSet(2048);
        this.lastUpdateTimestamp = ByteBuffer.allocateDirect(capacity * Long.BYTES).order(ByteOrder.nativeOrder()).asLongBuffer();

        for (int i = 0; i < capacity; i++) {
            this.networkId.put(i, -1);
            this.chunkKey.put(i, Long.MAX_VALUE);
            this.motionType[i] = EMotionType.Dynamic;
            this.activation[i] = EActivation.DontActivate;
        }
    }

    /**
     * Copies all server-specific physics data to another container.
     * <p>
     * Extends the base {@link VxBodyDataContainer#copyTo(VxBodyDataContainer)} by
     * migrating angular velocity, AABBs, network state, and dirty flags.
     *
     * @param other The destination container (must be an instance of {@link VxServerBodyDataContainer}).
     */
    @Override
    public void copyTo(VxBodyDataContainer other) {
        super.copyTo(other);
        if (other instanceof VxServerBodyDataContainer next) {
            int len = Math.min(this.capacity, next.capacity);
            System.arraycopy(this.bodyType, 0, next.bodyType, 0, len);
            System.arraycopy(this.motionType, 0, next.motionType, 0, len);
            System.arraycopy(this.activation, 0, next.activation, 0, len);
            copyLongBuffer(this.chunkKey, next.chunkKey, len);
            copyIntBuffer(this.networkId, next.networkId, len);
            copyByteBuffer(this.isTransformDirty, next.isTransformDirty, len);
            copyByteBuffer(this.isVertexDataDirty, next.isVertexDataDirty, len);
            copyByteBuffer(this.isCustomDataDirty, next.isCustomDataDirty, len);
            copyByteBuffer(this.isShapeDirty, next.isShapeDirty, len);
            copyLongBuffer(this.lastUpdateTimestamp, next.lastUpdateTimestamp, len);
            next.dirtyIndices.addAll(this.dirtyIndices);
        }
    }

    /**
     * Resets all server-specific physics data at the specified index to default values.
     * <p>
     * Clears physical bounds, network mapping, and dirty tracking state for the slot.
     *
     * @param index The slot index to clear.
     */
    @Override
    public void reset(int index) {
        super.reset(index);
        this.bodyType[index] = null;
        this.chunkKey.put(index, Long.MAX_VALUE);
        this.networkId.put(index, -1);
        this.motionType[index] = EMotionType.Dynamic;
        this.activation[index] = EActivation.DontActivate;
        this.isTransformDirty.put(index, (byte) 0);
        this.isVertexDataDirty.put(index, (byte) 0);
        this.isCustomDataDirty.put(index, (byte) 0);
        this.isShapeDirty.put(index, (byte) 0);
        this.lastUpdateTimestamp.put(index, 0L);
        this.dirtyIndices.remove(index);
    }
}