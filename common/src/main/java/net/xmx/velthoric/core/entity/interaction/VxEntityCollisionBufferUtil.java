/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.entity.interaction;

import com.github.stephengold.joltjni.ShapeRefC;
import net.xmx.velthoric.core.body.VxBody;
import net.xmx.velthoric.core.body.shape.VxCollisionShape;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Utility class for preparing and managing direct memory buffers 
 * necessary for JNI communication during entity-to-body collision detection.
 * Prevents redundant object instantiation by caching shapes and utilizing thread-local buffers.
 *
 * @author xI-Mx-Ix
 */
public final class VxEntityCollisionBufferUtil {

    /**
     * Cache mapping Velthoric Collision Shapes to native Jolt Shape References to prevent memory leaks.
     */
    public static final Map<VxCollisionShape, ShapeRefC> SHAPE_CACHE = new WeakHashMap<>();

    /**
     * Thread-local buffer for native shape pointers to avoid runtime allocations.
     */
    private static final ThreadLocal<ByteBuffer> SHAPE_PTRS = ThreadLocal.withInitial(() -> ByteBuffer.allocateDirect(0));

    /**
     * Thread-local buffer for native body IDs to avoid runtime allocations.
     */
    private static final ThreadLocal<ByteBuffer> BODY_IDS = ThreadLocal.withInitial(() -> ByteBuffer.allocateDirect(0));

    /**
     * Thread-local buffer for float results returned from JNI.
     */
    public static final ThreadLocal<float[]> OUT_RESULT = ThreadLocal.withInitial(() -> new float[6]);

    /**
     * Prepares and populates a contiguous buffer of shape pointers for the C++ backend.
     * 
     * @param capacity Maximum number of slots in the active physics world.
     * @param bodies The array of physics bodies currently active.
     * @return A ByteBuffer populated with 64-bit memory addresses of the collision shapes.
     */
    public static ByteBuffer prepareShapePtrsBuffer(int capacity, VxBody[] bodies) {
        ByteBuffer bb = SHAPE_PTRS.get();
        if (bb.capacity() < capacity * Long.BYTES) {
            bb = ByteBuffer.allocateDirect(capacity * Long.BYTES).order(ByteOrder.nativeOrder());
            SHAPE_PTRS.set(bb);
        }
        bb.clear();
        for (int i = 0; i < capacity; i++) {
            VxBody body = bodies[i];
            if (body != null && body.getShape() != null) {
                ShapeRefC shapeRef = SHAPE_CACHE.computeIfAbsent(body.getShape(), VxCollisionShape::createShapeRef);
                bb.putLong(i * Long.BYTES, shapeRef.getPtr().targetVa());
            } else {
                bb.putLong(i * Long.BYTES, 0L);
            }
        }
        return bb;
    }

    /**
     * Prepares and populates a contiguous buffer of 32-bit integer body identifiers.
     * Used by the C++ backend to distinguish individual physics objects.
     *
     * @param capacity Maximum number of slots in the active physics world.
     * @param bodies The array of physics bodies currently active.
     * @return A ByteBuffer populated with native body IDs.
     */
    public static ByteBuffer prepareBodyIdsBuffer(int capacity, VxBody[] bodies) {
        ByteBuffer bb = BODY_IDS.get();
        if (bb.capacity() < capacity * Integer.BYTES) {
            bb = ByteBuffer.allocateDirect(capacity * Integer.BYTES).order(ByteOrder.nativeOrder());
            BODY_IDS.set(bb);
        }
        bb.clear();
        for (int i = 0; i < capacity; i++) {
            VxBody body = bodies[i];
            if (body != null) {
                bb.putInt(i * Integer.BYTES, body.getBodyId());
            } else {
                bb.putInt(i * Integer.BYTES, 0);
            }
        }
        return bb;
    }
}