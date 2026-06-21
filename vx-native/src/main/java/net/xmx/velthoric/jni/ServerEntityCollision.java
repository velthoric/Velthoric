/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.jni;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Server-side native interactions for entity-to-body collisions.
 * Performs direct native calculations on the Jolt JPH::PhysicsSystem memory reference.
 *
 * @author xI-Mx-Ix
 */
public class ServerEntityCollision {

    /**
     * Executes narrow-phase server-side collision resolution with active physics bodies.
     * Locks the native systems, updates physics properties, and returns momentum/friction offsets.
     *
     * @param physicsSystemVa  Native address pointer pointing to JPH::PhysicsSystem.
     * @param shapePtrs        Direct buffer containing native 64-bit pointers of active collision shapes.
     * @param isActive         Direct buffer containing platform activation flags.
     * @param posX             Direct buffer storing the absolute X coordinates of active bodies.
     * @param posY             Direct buffer storing the absolute Y coordinates of active bodies.
     * @param posZ             Direct buffer storing the absolute Z coordinates of active bodies.
     * @param rotX             Direct buffer storing the rotation quaternions' X components.
     * @param rotY             Direct buffer storing the rotation quaternions' Y components.
     * @param rotZ             Direct buffer storing the rotation quaternions' Z components.
     * @param rotW             Direct buffer storing the rotation quaternions' W components.
     * @param velX             Direct buffer storing the linear velocities along the X coordinate.
     * @param velY             Direct buffer storing the linear velocities along the Y coordinate.
     * @param velZ             Direct buffer storing the linear velocities along the Z coordinate.
     * @param angVelX          Direct buffer storing the angular velocities on the X axis.
     * @param angVelY          Direct buffer storing the angular velocities on the Y axis.
     * @param angVelZ          Direct buffer storing the angular velocities on the Z axis.
     * @param bodyIds          Direct buffer storing 32-bit native platform identifiers.
     * @param capacity         Maximum slot allocation of the active physics world.
     * @param boxHx            Half-extent of the moving entity's bounding box on the X axis.
     * @param boxHy            Half-extent of the moving entity's bounding box on the Y axis.
     * @param boxHz            Half-extent of the moving entity's bounding box on the Z axis.
     * @param boxX             Central coordinate translation of the entity on the X axis.
     * @param boxY             Central coordinate translation of the entity on the Y axis.
     * @param boxZ             Central coordinate translation of the entity on the Z axis.
     * @param dx               Movement intention translation step on the X axis.
     * @param dy               Movement intention translation step on the Y axis.
     * @param dz               Movement intention translation step on the Z axis.
     * @param stepHeight       Maximum step height allowance for climbing.
     * @param entityMass       Physical entity mass used for calculating collision impulses.
     * @param entityVelocityX  Current entity motion velocity on the X coordinate.
     * @param entityVelocityY  Current entity motion velocity on the Y coordinate.
     * @param entityVelocityZ  Current entity motion velocity on the Z coordinate.
     * @param outResult        Output array containing [dx, dy, dz, groundBodyId, friction, yawError].
     * @param lastGroundBodyId The 1-based index representing the supportive platform in the previous tick.
     * @param dt               Dynamic delta time step.
     */
    public static native void nHandleCollision(
            long physicsSystemVa, ByteBuffer shapePtrs, ByteBuffer isActive,
            DoubleBuffer posX, DoubleBuffer posY, DoubleBuffer posZ,
            FloatBuffer rotX, FloatBuffer rotY, FloatBuffer rotZ, FloatBuffer rotW,
            FloatBuffer velX, FloatBuffer velY, FloatBuffer velZ,
            FloatBuffer angVelX, FloatBuffer angVelY, FloatBuffer angVelZ,
            IntBuffer bodyIds, int capacity,
            float boxHx, float boxHy, float boxHz,
            float boxX, float boxY, float boxZ,
            float dx, float dy, float dz, float stepHeight,
            float entityMass, float entityVelocityX, float entityVelocityY, float entityVelocityZ,
            float[] outResult, int lastGroundBodyId, float dt
    );

    /**
     * Resolves server-side edge limits for sneaking entities on platforms.
     * Ensures movement steps are strictly bounded to supportive surface areas.
     *
     * @param physicsSystemVa  64-bit address pointer of the native Jolt physics system.
     * @param shapePtrs        Contiguous direct buffer of shape pointers.
     * @param isActive         Direct buffer indicating dynamic platform statuses.
     * @param posX             Platform positions on X coordinates.
     * @param posY             Platform positions on Y coordinates.
     * @param posZ             Platform positions on Z coordinates.
     * @param rotX             Platform quaternions on the X axis.
     * @param rotY             Platform quaternions on the Y axis.
     * @param rotZ             Platform quaternions on the Z axis.
     * @param rotW             Platform quaternions on the W axis.
     * @param capacity         Maximum allocated container capacity.
     * @param boxHx            Half-extents of the entity bounding volume on X.
     * @param boxHy            Half-extents of the entity bounding volume on Y.
     * @param boxHz            Half-extents of the entity bounding volume on Z.
     * @param boxX             Sneaking translation coordinates on the X axis.
     * @param boxY             Sneaking translation coordinates on the Y axis.
     * @param boxZ             Sneaking translation coordinates on the Z axis.
     * @param dx               Movement step translation on X.
     * @param dz               Movement step translation on Z.
     * @param maxDrop          Maximum drop margin allowed.
     * @param outResult        Target array returning bounded move step translations [dx, dz].
     */
    public static native void nHandleSneak(
            long physicsSystemVa, ByteBuffer shapePtrs, ByteBuffer isActive,
            DoubleBuffer posX, DoubleBuffer posY, DoubleBuffer posZ,
            FloatBuffer rotX, FloatBuffer rotY, FloatBuffer rotZ, FloatBuffer rotW,
            int capacity,
            float boxHx, float boxHy, float boxHz,
            float boxX, float boxY, float boxZ,
            float dx, float dz, float maxDrop,
            float[] outResult
    );

    /**
     * Statically verifies if an entity's axis-aligned bounding box intersects
     * with any active custom platform on the server.
     *
     * @param physicsSystemVa  64-bit address pointer of the native Jolt physics system.
     * @param shapePtrs        Contiguous direct buffer of shape pointers.
     * @param isActive         Direct buffer indicating platform active statuses.
     * @param posX             Platform positions on X coordinates.
     * @param posY             Platform positions on Y coordinates.
     * @param posZ             Platform positions on Z coordinates.
     * @param rotX             Platform quaternions on the X axis.
     * @param rotY             Platform quaternions on the Y axis.
     * @param rotZ             Platform quaternions on the Z axis.
     * @param rotW             Platform quaternions on the W axis.
     * @param capacity         Maximum allocated container capacity.
     * @param boxHx            Half-extents of the bounding volume on X.
     * @param boxHy            Half-extents of the bounding volume on Y.
     * @param boxHz            Half-extents of the bounding volume on Z.
     * @param boxX             Translation coordinates on the X axis.
     * @param boxY             Translation coordinates on the Y axis.
     * @param boxZ             Translation coordinates on the Z axis.
     * @return True if static collision intersections are detected.
     */
    public static native boolean nIsColliding(
            long physicsSystemVa, ByteBuffer shapePtrs, ByteBuffer isActive,
            DoubleBuffer posX, DoubleBuffer posY, DoubleBuffer posZ,
            FloatBuffer rotX, FloatBuffer rotY, FloatBuffer rotZ, FloatBuffer rotW,
            int capacity,
            float boxHx, float boxHy, float boxHz,
            float boxX, float boxY, float boxZ
    );

    /**
     * Statically retrieves the exact ID of the body the bounding box intersects with.
     *
     * @param physicsSystemVa Pointer address of the Jolt native PhysicsSystem.
     * @param shapePtrs Contiguous direct buffer of shape pointers.
     * @param isActive Direct buffer indicating dynamic platform statuses.
     * @param posX Platform positions on X coordinates.
     * @param posY Platform positions on Y coordinates.
     * @param posZ Platform positions on Z coordinates.
     * @param rotX Platform quaternions on X axis.
     * @param rotY Platform quaternions on Y axis.
     * @param rotZ Platform quaternions on Z axis.
     * @param rotW Platform quaternions on W axis.
     * @param capacity Maximum allocated container capacity.
     * @param boxHx Half-extents of checking bounding volume on X.
     * @param boxHy Half-extents of checking bounding volume on Y.
     * @param boxHz Half-extents of checking bounding volume on Z.
     * @param boxX Central bounding volume translation coordinates on X.
     * @param boxY Central bounding volume translation coordinates on Y.
     * @param boxZ Central bounding volume translation coordinates on Z.
     * @return The 0-based index of the colliding body, or -1 if no intersection is detected.
     */
    public static native int nGetCollidingBodyId(
            long physicsSystemVa, ByteBuffer shapePtrs, ByteBuffer isActive,
            DoubleBuffer posX, DoubleBuffer posY, DoubleBuffer posZ,
            FloatBuffer rotX, FloatBuffer rotY, FloatBuffer rotZ, FloatBuffer rotW,
            int capacity,
            float boxHx, float boxHy, float boxHz,
            float boxX, float boxY, float boxZ
    );
}