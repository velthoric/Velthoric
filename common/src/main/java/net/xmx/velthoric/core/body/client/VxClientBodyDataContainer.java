/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.body.client;

import com.github.stephengold.joltjni.RVec3;
import net.xmx.velthoric.core.body.VxBodyDataContainer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.ByteOrder;

/**
 * Client-specific container for physics body data, including interpolation buffers.
 *
 * @author xI-Mx-Ix
 */
public class VxClientBodyDataContainer extends VxBodyDataContainer {
    /**
     * The timestamp (ns) of the previous interpolation state (state0).
     */
    public final LongBuffer state0_timestamp;
    /**
     * The timestamp (ns) of the current interpolation state (state1).
     */
    public final LongBuffer state1_timestamp;

    /**
     * Previous X, Y, Z coordinates for interpolation (state0).
     */
    public final DoubleBuffer state0_posX, state0_posY, state0_posZ;
    /**
     * Current X, Y, Z coordinates received from the server (state1).
     */
    public final DoubleBuffer state1_posX, state1_posY, state1_posZ;
    /**
     * Last rendered X, Y, Z coordinates for smooth frame transitions.
     */
    public final DoubleBuffer prev_posX, prev_posY, prev_posZ;

    /**
     * Previous rotation quaternion for interpolation (state0).
     */
    public final FloatBuffer state0_rotX, state0_rotY, state0_rotZ, state0_rotW;
    /**
     * Current rotation quaternion received from the server (state1).
     */
    public final FloatBuffer state1_rotX, state1_rotY, state1_rotZ, state1_rotW;
    /**
     * Last rendered rotation quaternion for smooth frame transitions.
     */
    public final FloatBuffer prev_rotX, prev_rotY, prev_rotZ, prev_rotW;

    /**
     * Lineary velocity at state0 used for extrapolation.
     */
    public final FloatBuffer state0_velX, state0_velY, state0_velZ;
    /**
     * Linear velocity at state1 used for extrapolation.
     */
    public final FloatBuffer state1_velX, state1_velY, state1_velZ;

    /**
     * Activation state at state0.
     */
    public final ByteBuffer state0_isActive;
    /**
     * Activation state at state1.
     */
    public final ByteBuffer state1_isActive;

    /**
     * Vertex data at state0.
     */
    public final float[][] state0_vertexData;
    /**
     * Vertex data at state1.
     */
    public final float[][] state1_vertexData;
    /**
     * Last rendered vertex data.
     */
    public final float[][] prev_vertexData;

    /**
     * Whether the renderer has finished initializing resources (e.g. GPU buffers) for this body.
     */
    public final ByteBuffer render_isInitialized;
    /**
     * Arbitrary custom data objects attached to the body for client-side logic.
     */
    public final Object[] customData;
    /**
     * High-precision culling position used for frustum checks.
     */
    public final RVec3[] lastKnownPosition;

    /**
     * Initializes a new client-side container with triple-buffering for interpolation.
     * Pre-allocates all state buffers and culling objects.
     *
     * @param capacity The maximum number of bodies.
     */
    public VxClientBodyDataContainer(int capacity) {
        super(capacity);
        this.state0_timestamp = ByteBuffer.allocateDirect(capacity * Long.BYTES).order(ByteOrder.nativeOrder()).asLongBuffer();
        this.state1_timestamp = ByteBuffer.allocateDirect(capacity * Long.BYTES).order(ByteOrder.nativeOrder()).asLongBuffer();

        this.state0_posX = ByteBuffer.allocateDirect(capacity * Double.BYTES).order(ByteOrder.nativeOrder()).asDoubleBuffer();
        this.state0_posY = ByteBuffer.allocateDirect(capacity * Double.BYTES).order(ByteOrder.nativeOrder()).asDoubleBuffer();
        this.state0_posZ = ByteBuffer.allocateDirect(capacity * Double.BYTES).order(ByteOrder.nativeOrder()).asDoubleBuffer();
        this.state1_posX = ByteBuffer.allocateDirect(capacity * Double.BYTES).order(ByteOrder.nativeOrder()).asDoubleBuffer();
        this.state1_posY = ByteBuffer.allocateDirect(capacity * Double.BYTES).order(ByteOrder.nativeOrder()).asDoubleBuffer();
        this.state1_posZ = ByteBuffer.allocateDirect(capacity * Double.BYTES).order(ByteOrder.nativeOrder()).asDoubleBuffer();
        this.prev_posX = ByteBuffer.allocateDirect(capacity * Double.BYTES).order(ByteOrder.nativeOrder()).asDoubleBuffer();
        this.prev_posY = ByteBuffer.allocateDirect(capacity * Double.BYTES).order(ByteOrder.nativeOrder()).asDoubleBuffer();
        this.prev_posZ = ByteBuffer.allocateDirect(capacity * Double.BYTES).order(ByteOrder.nativeOrder()).asDoubleBuffer();

        this.state0_rotX = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.state0_rotY = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.state0_rotZ = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.state0_rotW = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.state1_rotX = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.state1_rotY = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.state1_rotZ = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.state1_rotW = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.prev_rotX = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.prev_rotY = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.prev_rotZ = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.prev_rotW = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();

        this.state0_velX = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.state0_velY = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.state0_velZ = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.state1_velX = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.state1_velY = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.state1_velZ = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();

        this.state0_isActive = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
        this.state1_isActive = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());

        this.state0_vertexData = new float[capacity][];
        this.state1_vertexData = new float[capacity][];
        this.prev_vertexData = new float[capacity][];

        this.render_isInitialized = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
        this.customData = new Object[capacity];
        this.lastKnownPosition = new RVec3[capacity];
        for (int i = 0; i < capacity; i++) {
            this.lastKnownPosition[i] = new RVec3();
        }
    }

    /**
     * Copies all client-specific physics data and interpolation buffers to another container.
     * <p>
     * Migrates the entire state history (State 0, State 1, Prev) and culling state
     * to the new memory allocation.
     *
     * @param other The destination container (must be an instance of {@link VxClientBodyDataContainer}).
     */
    @Override
    public void copyTo(VxBodyDataContainer other) {
        super.copyTo(other);
        if (other instanceof VxClientBodyDataContainer next) {
            int len = Math.min(this.capacity, next.capacity);
            copyLongBuffer(this.state0_timestamp, next.state0_timestamp, len);
            copyDoubleBuffer(this.state0_posX, next.state0_posX, len);
            copyDoubleBuffer(this.state0_posY, next.state0_posY, len);
            copyDoubleBuffer(this.state0_posZ, next.state0_posZ, len);
            copyFloatBuffer(this.state0_rotX, next.state0_rotX, len);
            copyFloatBuffer(this.state0_rotY, next.state0_rotY, len);
            copyFloatBuffer(this.state0_rotZ, next.state0_rotZ, len);
            copyFloatBuffer(this.state0_rotW, next.state0_rotW, len);
            copyFloatBuffer(this.state0_velX, next.state0_velX, len);
            copyFloatBuffer(this.state0_velY, next.state0_velY, len);
            copyFloatBuffer(this.state0_velZ, next.state0_velZ, len);
            copyByteBuffer(this.state0_isActive, next.state0_isActive, len);
            System.arraycopy(this.state0_vertexData, 0, next.state0_vertexData, 0, len);

            copyLongBuffer(this.state1_timestamp, next.state1_timestamp, len);
            copyDoubleBuffer(this.state1_posX, next.state1_posX, len);
            copyDoubleBuffer(this.state1_posY, next.state1_posY, len);
            copyDoubleBuffer(this.state1_posZ, next.state1_posZ, len);
            copyFloatBuffer(this.state1_rotX, next.state1_rotX, len);
            copyFloatBuffer(this.state1_rotY, next.state1_rotY, len);
            copyFloatBuffer(this.state1_rotZ, next.state1_rotZ, len);
            copyFloatBuffer(this.state1_rotW, next.state1_rotW, len);
            copyFloatBuffer(this.state1_velX, next.state1_velX, len);
            copyFloatBuffer(this.state1_velY, next.state1_velY, len);
            copyFloatBuffer(this.state1_velZ, next.state1_velZ, len);
            copyByteBuffer(this.state1_isActive, next.state1_isActive, len);
            System.arraycopy(this.state1_vertexData, 0, next.state1_vertexData, 0, len);

            copyDoubleBuffer(this.prev_posX, next.prev_posX, len);
            copyDoubleBuffer(this.prev_posY, next.prev_posY, len);
            copyDoubleBuffer(this.prev_posZ, next.prev_posZ, len);
            copyFloatBuffer(this.prev_rotX, next.prev_rotX, len);
            copyFloatBuffer(this.prev_rotY, next.prev_rotY, len);
            copyFloatBuffer(this.prev_rotZ, next.prev_rotZ, len);
            copyFloatBuffer(this.prev_rotW, next.prev_rotW, len);
            System.arraycopy(this.prev_vertexData, 0, next.prev_vertexData, 0, len);

            copyByteBuffer(this.render_isInitialized, next.render_isInitialized, len);
            System.arraycopy(this.customData, 0, next.customData, 0, len);
            for (int i = 0; i < len; i++) {
                next.lastKnownPosition[i].set(this.lastKnownPosition[i]);
            }
        }
    }

    /**
     * Resets all client-specific physics data at the specified index to default values.
     * <p>
     * Clears all interpolation state history, custom data, and resets culling vectors for the slot.
     *
     * @param index The slot index to clear.
     */
    @Override
    public void reset(int index) {
        super.reset(index);
        this.state0_timestamp.put(index, 0L);
        this.state1_timestamp.put(index, 0L);
        this.state0_isActive.put(index, (byte) 0);
        this.state1_isActive.put(index, (byte) 0);
        this.state0_vertexData[index] = null;
        this.state1_vertexData[index] = null;
        this.render_isInitialized.put(index, (byte) 0);
        this.prev_vertexData[index] = null;
        this.customData[index] = null;

        if (this.lastKnownPosition != null && this.lastKnownPosition[index] != null) {
            this.lastKnownPosition[index].loadZero();
        }

        this.state0_velX.put(index, 0f);
        this.state0_velY.put(index, 0f);
        this.state0_velZ.put(index, 0f);
        this.state1_velX.put(index, 0f);
        this.state1_velY.put(index, 0f);
        this.state1_velZ.put(index, 0f);
        this.state0_posX.put(index, 0.0);
        this.state0_posY.put(index, 0.0);
        this.state0_posZ.put(index, 0.0);
        this.state1_posX.put(index, 0.0);
        this.state1_posY.put(index, 0.0);
        this.state1_posZ.put(index, 0.0);
        this.prev_posX.put(index, 0.0);
        this.prev_posY.put(index, 0.0);
        this.prev_posZ.put(index, 0.0);
        this.state0_rotX.put(index, 0f);
        this.state0_rotY.put(index, 0f);
        this.state0_rotZ.put(index, 0f);
        this.state0_rotW.put(index, 1f);
        this.state1_rotX.put(index, 0f);
        this.state1_rotY.put(index, 0f);
        this.state1_rotZ.put(index, 0f);
        this.state1_rotW.put(index, 1f);
        this.prev_rotX.put(index, 0f);
        this.prev_rotY.put(index, 0f);
        this.prev_rotZ.put(index, 0f);
        this.prev_rotW.put(index, 1f);
    }
}