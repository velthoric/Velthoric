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
 * JNI bridge for shape-related helper and batch operations.
 *
 * @author xI-Mx-Ix
 */
public final class ShapeBridge {

    private ShapeBridge() {}

    /**
     * Retrieves all triangle vertices of a Jolt Shape in local space.
     *
     * @param shapeVa The native virtual address of the JPH::Shape.
     * @return A flat float array containing vertex coordinates (x1, y1, z1, x2, y2, z2, x3, y3, z3) for all triangles.
     */
    public static native float[] nGetShapeTriangles(long shapeVa);

    /**
     * Performs a high-performance batch calculation of world-space AABBs for multiple bodies.
     * Extracts bounds directly from Jolt shapes using interpolated positions and rotations.
     *
     * @param capacity             The maximum capacity of the buffers (total number of bodies).
     * @param shapeAddress         Direct LongBuffer containing native virtual addresses of Jolt shapes.
     * @param posX                 Direct DoubleBuffer containing X-coordinates of body positions.
     * @param posY                 Direct DoubleBuffer containing Y-coordinates of body positions.
     * @param posZ                 Direct DoubleBuffer containing Z-coordinates of body positions.
     * @param rotX                 Direct FloatBuffer containing X-components of body rotation quaternions.
     * @param rotY                 Direct FloatBuffer containing Y-components of body rotation quaternions.
     * @param rotZ                 Direct FloatBuffer containing Z-components of body rotation quaternions.
     * @param rotW                 Direct FloatBuffer containing W-components of body rotation quaternions.
     * @param aabbMinX             Direct FloatBuffer to write the calculated minimum X bounds.
     * @param aabbMinY             Direct FloatBuffer to write the calculated minimum Y bounds.
     * @param aabbMinZ             Direct FloatBuffer to write the calculated minimum Z bounds.
     * @param aabbMaxX             Direct FloatBuffer to write the calculated maximum X bounds.
     * @param aabbMaxY             Direct FloatBuffer to write the calculated maximum Y bounds.
     * @param aabbMaxZ             Direct FloatBuffer to write the calculated maximum Z bounds.
     * @param render_isInitialized Direct ByteBuffer indicating whether each body is initialized and active for rendering.
     */
    public static native void nCalculateAABBs(
            int capacity,
            LongBuffer shapeAddress,
            DoubleBuffer posX, DoubleBuffer posY, DoubleBuffer posZ,
            FloatBuffer rotX, FloatBuffer rotY, FloatBuffer rotZ, FloatBuffer rotW,
            FloatBuffer aabbMinX, FloatBuffer aabbMinY, FloatBuffer aabbMinZ,
            FloatBuffer aabbMaxX, FloatBuffer aabbMaxY, FloatBuffer aabbMaxZ,
            ByteBuffer render_isInitialized
    );
}