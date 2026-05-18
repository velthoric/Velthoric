/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.jni;

/**
 * JNI bindings for entity collision and movement handling.
 *
 * @author xI-Mx-Ix
 */
public class EntityBridge {
    /**
     * Handles the entire collision and sweep logic natively for maximum performance.
     * 
     * @param shapePtrs Array of shape pointers (long).
     * @param transforms Array of floats (x,y,z, qx,qy,qz,qw) for each shape. Length must be shapePtrs.length * 7.
     * @param count Number of bodies.
     * @param boxHx Half-extent X of the AABB.
     * @param boxHy Half-extent Y of the AABB.
     * @param boxHz Half-extent Z of the AABB.
     * @param boxX Center X of the AABB.
     * @param boxY Center Y of the AABB.
     * @param boxZ Center Z of the AABB.
     * @param dx The requested movement X.
     * @param dy The requested movement Y.
     * @param dz The requested movement Z.
     * @param stepHeight The entity's maximum step height.
     * @param outResult A float array of size 4: [newDx, newDy, newDz, walkedOnBodyIndex].
     */
    public static native void nHandleCollision(
            long[] shapePtrs, float[] transforms, int count,
            float boxHx, float boxHy, float boxHz,
            float boxX, float boxY, float boxZ,
            float dx, float dy, float dz, float stepHeight,
            float[] outResult
    );

    /**
     * Checks if the given AABB intersects with any physics body.
     */
    public static native boolean nIsColliding(
            long[] shapePtrs, float[] transforms, int count,
            float boxHx, float boxHy, float boxHz,
            float boxX, float boxY, float boxZ
    );

    public static native void nHandleSneak(
            long[] shapePtrs, float[] transforms, int count,
            float boxHx, float boxHy, float boxHz,
            float boxX, float boxY, float boxZ,
            float dx, float dz, float maxDrop,
            float[] outResult
    );
}
