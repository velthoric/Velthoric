/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.jni;

/**
 * JNI bridge for shape-related helper operations.
 *
 * @author xI-Mx-Ix
 */
public final class VxShapeBridge {

    private VxShapeBridge() {
    }

    /**
     * Retrieves all triangle vertices of a Jolt Shape in local space.
     *
     * @param shapeVa The native virtual address of the JPH::Shape.
     * @return A flat float array containing vertex coordinates (x1, y1, z1, x2, y2, z2, x3, y3, z3) for all triangles.
     */
    public static native float[] nGetShapeTriangles(long shapeVa);
}