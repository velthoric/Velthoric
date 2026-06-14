/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.body;

import net.xmx.velthoric.core.body.shape.VxCollisionShape;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.ByteOrder;

/**
 * A container for the base Structure of Arrays (SoA) physics data.
 * This class is designed to be swapped atomically within a {@link VxBodyDataStore}
 * to ensure thread-safety during array resizing.
 *
 * @author xI-Mx-Ix
 */
public class VxBodyDataContainer {
    /**
     * X-coordinate of the body position in world space.
     */
    public final DoubleBuffer posX;
    /**
     * Y-coordinate of the body position in world space.
     */
    public final DoubleBuffer posY;
    /**
     * Z-coordinate of the body position in world space.
     */
    public final DoubleBuffer posZ;

    /**
     * X-component of the body rotation quaternion.
     */
    public final FloatBuffer rotX;
    /**
     * Y-component of the body rotation quaternion.
     */
    public final FloatBuffer rotY;
    /**
     * Z-component of the body rotation quaternion.
     */
    public final FloatBuffer rotZ;
    /**
     * W-component of the body rotation quaternion.
     */
    public final FloatBuffer rotW;

    /**
     * X-component of the linear velocity.
     */
    public final FloatBuffer velX;
    /**
     * Y-component of the linear velocity.
     */
    public final FloatBuffer velY;
    /**
     * Z-component of the linear velocity.
     */
    public final FloatBuffer velZ;

    /**
     * X-component of the angular velocity (radians/sec).
     */
    public final FloatBuffer angVelX;
    /**
     * Y-component of the angular velocity (radians/sec).
     */
    public final FloatBuffer angVelY;
    /**
     * Z-component of the angular velocity (radians/sec).
     */
    public final FloatBuffer angVelZ;

    /**
     * Per-vertex data for complex collision shapes or rendering (e.g. heightmaps, meshes).
     */
    public final float[][] vertexData;

    /**
     * The collision shape of the body, synchronized between server and client.
     */
    public final VxCollisionShape[] shape;

    /**
     * The native virtual addresses of the collision shapes.
     */
    public final LongBuffer shapeAddress;

    /**
     * Minimum X-coordinate of the world-space Axis-Aligned Bounding Box (AABB).
     */
    public final FloatBuffer aabbMinX;
    /**
     * Minimum Y-coordinate of the world-space Axis-Aligned Bounding Box (AABB).
     */
    public final FloatBuffer aabbMinY;
    /**
     * Minimum Z-coordinate of the world-space Axis-Aligned Bounding Box (AABB).
     */
    public final FloatBuffer aabbMinZ;

    /**
     * Maximum X-coordinate of the world-space Axis-Aligned Bounding Box (AABB).
     */
    public final FloatBuffer aabbMaxX;
    /**
     * Maximum Y-coordinate of the world-space Axis-Aligned Bounding Box (AABB).
     */
    public final FloatBuffer aabbMaxY;
    /**
     * Maximum Z-coordinate of the world-space Axis-Aligned Bounding Box (AABB).
     */
    public final FloatBuffer aabbMaxZ;

    /**
     * Whether the body is currently active and ticking in the physics simulation.
     */
    public final ByteBuffer isActive;
    /**
     * Bitmask representing attached behaviors and their network/tick states.
     */
    public final LongBuffer behaviorBits;
    /**
     * Reference to the high-level {@link VxBody} wrapper objects for each slot.
     */
    public final VxBody[] bodies;
    /**
     * The total pre-allocated capacity of the data arrays in this container.
     */
    public final int capacity;

    /**
     * Initializes a new container with the specified capacity.
     * All internal arrays are pre-allocated to this size to avoid runtime allocations.
     *
     * @param capacity The maximum number of bodies this container can manage.
     */
    public VxBodyDataContainer(int capacity) {
        this.capacity = capacity;
        this.posX = ByteBuffer.allocateDirect(capacity * Double.BYTES).order(ByteOrder.nativeOrder()).asDoubleBuffer();
        this.posY = ByteBuffer.allocateDirect(capacity * Double.BYTES).order(ByteOrder.nativeOrder()).asDoubleBuffer();
        this.posZ = ByteBuffer.allocateDirect(capacity * Double.BYTES).order(ByteOrder.nativeOrder()).asDoubleBuffer();
        this.rotX = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.rotY = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.rotZ = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.rotW = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.velX = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.velY = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.velZ = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.angVelX = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.angVelY = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.angVelZ = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.vertexData = new float[capacity][];
        this.shape = new VxCollisionShape[capacity];
        this.shapeAddress = ByteBuffer.allocateDirect(capacity * Long.BYTES).order(ByteOrder.nativeOrder()).asLongBuffer();
        this.aabbMinX = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.aabbMinY = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.aabbMinZ = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.aabbMaxX = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.aabbMaxY = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.aabbMaxZ = ByteBuffer.allocateDirect(capacity * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.isActive = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
        this.behaviorBits = ByteBuffer.allocateDirect(capacity * Long.BYTES).order(ByteOrder.nativeOrder()).asLongBuffer();
        this.bodies = new VxBody[capacity];
    }

    /**
     * Copies all base physics data from this container to another container.
     * <p>
     * This is used during array resizing to migrate existing state to a larger
     * memory allocation while maintaining data integrity.
     *
     * @param other The destination container to copy data into.
     * @throws IllegalArgumentException if the other container's capacity is smaller than this container's.
     */
    public void copyTo(VxBodyDataContainer other) {
        int copyLength = Math.min(this.capacity, other.capacity);
        copyDoubleBuffer(this.posX, other.posX, copyLength);
        copyDoubleBuffer(this.posY, other.posY, copyLength);
        copyDoubleBuffer(this.posZ, other.posZ, copyLength);
        copyFloatBuffer(this.rotX, other.rotX, copyLength);
        copyFloatBuffer(this.rotY, other.rotY, copyLength);
        copyFloatBuffer(this.rotZ, other.rotZ, copyLength);
        copyFloatBuffer(this.rotW, other.rotW, copyLength);
        copyFloatBuffer(this.velX, other.velX, copyLength);
        copyFloatBuffer(this.velY, other.velY, copyLength);
        copyFloatBuffer(this.velZ, other.velZ, copyLength);
        copyFloatBuffer(this.angVelX, other.angVelX, copyLength);
        copyFloatBuffer(this.angVelY, other.angVelY, copyLength);
        copyFloatBuffer(this.angVelZ, other.angVelZ, copyLength);
        System.arraycopy(this.vertexData, 0, other.vertexData, 0, copyLength);
        System.arraycopy(this.shape, 0, other.shape, 0, copyLength);
        copyLongBuffer(this.shapeAddress, other.shapeAddress, copyLength);
        copyFloatBuffer(this.aabbMinX, other.aabbMinX, copyLength);
        copyFloatBuffer(this.aabbMinY, other.aabbMinY, copyLength);
        copyFloatBuffer(this.aabbMinZ, other.aabbMinZ, copyLength);
        copyFloatBuffer(this.aabbMaxX, other.aabbMaxX, copyLength);
        copyFloatBuffer(this.aabbMaxY, other.aabbMaxY, copyLength);
        copyFloatBuffer(this.aabbMaxZ, other.aabbMaxZ, copyLength);
        copyByteBuffer(this.isActive, other.isActive, copyLength);
        copyLongBuffer(this.behaviorBits, other.behaviorBits, copyLength);
        System.arraycopy(this.bodies, 0, other.bodies, 0, copyLength);
    }

    /**
     * Copies a range of elements from one direct {@link DoubleBuffer} to another.
     * <p>
     * Sets positions and limits to transfer precisely the requested amount of elements,
     * resetting buffer states upon completion to avoid side effects.
     *
     * @param src    The source buffer.
     * @param dest   The destination buffer.
     * @param length The number of elements to copy.
     */
    protected void copyDoubleBuffer(DoubleBuffer src, DoubleBuffer dest, int length) {
        src.position(0).limit(length);
        dest.position(0).limit(length);
        dest.put(src);
        src.clear();
        dest.clear();
    }

    /**
     * Copies a range of elements from one direct {@link FloatBuffer} to another.
     * <p>
     * Sets positions and limits to transfer precisely the requested amount of elements,
     * resetting buffer states upon completion to avoid side effects.
     *
     * @param src    The source buffer.
     * @param dest   The destination buffer.
     * @param length The number of elements to copy.
     */
    protected void copyFloatBuffer(FloatBuffer src, FloatBuffer dest, int length) {
        src.position(0).limit(length);
        dest.position(0).limit(length);
        dest.put(src);
        src.clear();
        dest.clear();
    }

    /**
     * Copies a range of elements from one direct {@link ByteBuffer} to another.
     * <p>
     * Sets positions and limits to transfer precisely the requested amount of elements,
     * resetting buffer states upon completion to avoid side effects.
     *
     * @param src    The source buffer.
     * @param dest   The destination buffer.
     * @param length The number of elements to copy.
     */
    protected void copyByteBuffer(ByteBuffer src, ByteBuffer dest, int length) {
        src.position(0).limit(length);
        dest.position(0).limit(length);
        dest.put(src);
        src.clear();
        dest.clear();
    }

    /**
     * Copies a range of elements from one direct {@link LongBuffer} to another.
     * <p>
     * Sets positions and limits to transfer precisely the requested amount of elements,
     * resetting buffer states upon completion to avoid side effects.
     *
     * @param src    The source buffer.
     * @param dest   The destination buffer.
     * @param length The number of elements to copy.
     */
    protected void copyLongBuffer(LongBuffer src, LongBuffer dest, int length) {
        src.position(0).limit(length);
        dest.position(0).limit(length);
        dest.put(src);
        src.clear();
        dest.clear();
    }

    /**
     * Copies a range of elements from one direct {@link IntBuffer} to another.
     * <p>
     * Sets positions and limits to transfer precisely the requested amount of elements,
     * resetting buffer states upon completion to avoid side effects.
     *
     * @param src    The source buffer.
     * @param dest   The destination buffer.
     * @param length The number of elements to copy.
     */
    protected void copyIntBuffer(IntBuffer src, IntBuffer dest, int length) {
        src.position(0).limit(length);
        dest.position(0).limit(length);
        dest.put(src);
        src.clear();
        dest.clear();
    }

    /**
     * Resets all base physics data at the specified index to default values.
     * <p>
     * This clears object references to prevent memory leaks and restores 
     * mathematical identity values (e.g., identity quaternion for rotation).
     *
     * @param index The slot index to clear.
     */
    public void reset(int index) {
        this.bodies[index] = null;
        this.posX.put(index, 0.0);
        this.posY.put(index, 0.0);
        this.posZ.put(index, 0.0);
        this.rotX.put(index, 0f);
        this.rotY.put(index, 0f);
        this.rotZ.put(index, 0f);
        this.rotW.put(index, 1f); // Identity Quaternion
        this.velX.put(index, 0f);
        this.velY.put(index, 0f);
        this.velZ.put(index, 0f);
        this.angVelX.put(index, 0f);
        this.angVelY.put(index, 0f);
        this.angVelZ.put(index, 0f);
        this.vertexData[index] = null;
        this.shape[index] = null;
        this.shapeAddress.put(index, 0L);
        this.aabbMinX.put(index, 0f);
        this.aabbMinY.put(index, 0f);
        this.aabbMinZ.put(index, 0f);
        this.aabbMaxX.put(index, 0f);
        this.aabbMaxY.put(index, 0f);
        this.aabbMaxZ.put(index, 0f);
        this.isActive.put(index, (byte) 0);
        this.behaviorBits.put(index, 0L);
    }

    /**
     * Returns the total pre-allocated capacity of this container.
     *
     * @return The maximum number of body slots.
     */
    public int getCapacity() {
        return capacity;
    }
}