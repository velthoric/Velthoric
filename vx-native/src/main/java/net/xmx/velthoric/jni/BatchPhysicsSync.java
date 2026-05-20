/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.jni;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;

/**
 * JNI bridge for batch-processing physics state synchronization.
 * Designed for Structure of Arrays (SoA) data layouts to minimize native overhead.
 *
 * @author xI-Mx-Ix
 */
public class BatchPhysicsSync {

    /**
     * Synchronizes a batch of physics bodies from native memory to Java buffers.
     *
     * @param physicsSystemPtr     The native memory address (pointer) of the Jolt {@code PhysicsSystem}.
     * @param count                The number of bodies to process in this batch.
     * @param indices              The global indices within the {@code VxServerBodyDataContainer} for each body.
     * @param bodyIds              The native Jolt {@code BodyID}s to be queried.
     * @param behaviorBits         Bitmasks for each body, used by native code to filter logic execution.
     * @param posX                 Output buffer for X-coordinates (Double-precision for large-world support).
     * @param posY                 Output buffer for Y-coordinates.
     * @param posZ                 Output buffer for Z-coordinates.
     * @param rotX                 Output buffer for the X-component of the rotation quaternions.
     * @param rotY                 Output buffer for the Y-component of the rotation quaternions.
     * @param rotZ                 Output buffer for the Z-component of the rotation quaternions.
     * @param rotW                 Output buffer for the W-component of the rotation quaternions.
     * @param velX                 Output buffer for linear velocity X.
     * @param velY                 Output buffer for linear velocity Y.
     * @param velZ                 Output buffer for linear velocity Z.
     * @param angVelX              Output buffer for angular velocity X.
     * @param angVelY              Output buffer for angular velocity Y.
     * @param angVelZ              Output buffer for angular velocity Z.
     * @param aabbMinX             Output buffer for the minimum AABB X-boundary.
     * @param aabbMinY             Output buffer for the minimum AABB Y-boundary.
     * @param aabbMinZ             Output buffer for the minimum AABB Z-boundary.
     * @param aabbMaxX             Output buffer for the maximum AABB X-boundary.
     * @param aabbMaxY             Output buffer for the maximum AABB Y-boundary.
     * @param aabbMaxZ             Output buffer for the maximum AABB Z-boundary.
     * @param isActive             Input/Output buffer indicating if a body is active (awake).
     * @param isTransformDirty     Output flags indicating if the transform (pos/rot) has changed.
     * @param isVertexDataDirty    Output flags indicating if soft-body vertex data has changed.
     * @param lastUpdateTimestamp  Buffer to store the simulation timestamp for each body's last sync.
     * @param motionTypeOutput     Output buffer for the {@code EMotionType} ordinals (Static, Kinematic, Dynamic).
     * @param dirtyIndicesOutput   Buffer to be filled with the {@code indices} of bodies requiring a network sync.
     * @param vertexData           2D-array containing vertex position arrays for soft-body mesh synchronization.
     * @param softBodyBehaviorMask The behavior bitmask used to identify bodies that require vertex processing.
     * @param timestampNanos       The current simulation time in nanoseconds.
     * @return The total number of indices written to {@code dirtyIndicesOutput}.
     */
    public static native int syncPhysicsNative(
            long physicsSystemPtr,
            int count,
            int[] indices,
            int[] bodyIds,
            long[] behaviorBits,
            DoubleBuffer posX, DoubleBuffer posY, DoubleBuffer posZ,
            FloatBuffer rotX, FloatBuffer rotY, FloatBuffer rotZ, FloatBuffer rotW,
            FloatBuffer velX, FloatBuffer velY, FloatBuffer velZ,
            FloatBuffer angVelX, FloatBuffer angVelY, FloatBuffer angVelZ,
            FloatBuffer aabbMinX, FloatBuffer aabbMinY, FloatBuffer aabbMinZ,
            FloatBuffer aabbMaxX, FloatBuffer aabbMaxY, FloatBuffer aabbMaxZ,
            ByteBuffer isActive,
            ByteBuffer isTransformDirty,
            ByteBuffer isVertexDataDirty,
            LongBuffer lastUpdateTimestamp,
            byte[] motionTypeOutput,
            int[] dirtyIndicesOutput,
            float[][] vertexData,
            long softBodyBehaviorMask,
            long timestampNanos
    );
}