/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 *
 * Author: xI-Mx-Ix
 */
#pragma once
#include <jni.h>

extern "C" {

/**
 * Handle native Jolt-based client-side collision resolution for a moving player or entity.
 * Supports platform momentum transfer, step assistance, and friction application.
 *
 * @param env JNI interface pointer.
 * @param clazz Associated JNI class context.
 * @param shapePtrs Contiguous direct buffer of shape pointers.
 * @param isActive Direct buffer indicating dynamic platform statuses.
 * @param posX Platform positions on X coordinates.
 * @param posY Platform positions on Y coordinates.
 * @param posZ Platform positions on Z coordinates.
 * @param rotX Platform quaternions on X axis.
 * @param rotY Platform quaternions on Y axis.
 * @param rotZ Platform quaternions on Z axis.
 * @param rotW Platform quaternions on W axis.
 * @param velX Platform linear velocities on X.
 * @param velY Platform linear velocities on Y.
 * @param velZ Platform linear velocities on Z.
 * @param angVelX Platform angular velocities on X.
 * @param angVelY Platform angular velocities on Y.
 * @param angVelZ Platform angular velocities on Z.
 * @param capacity Maximum allocated container capacity.
 * @param boxHx Half-extents of checking bounding volume on X.
 * @param boxHy Half-extents of checking bounding volume on Y.
 * @param boxHz Half-extents of checking bounding volume on Z.
 * @param boxX Central bounding volume translation coordinates on X.
 * @param boxY Central bounding volume translation coordinates on Y.
 * @param boxZ Central bounding volume translation coordinates on Z.
 * @param dx Intention movement offset on X.
 * @param dy Intention movement offset on Y.
 * @param dz Intention movement offset on Z.
 * @param stepHeight Maximum step climbing offset.
 * @param outResult Target array storing corrected translations and platform data.
 * @param lastGroundBodyId The slot index tracked on the previous tick.
 * @param dt Dynamic frame rate delta step.
 */
JNIEXPORT void JNICALL Java_net_xmx_velthoric_jni_ClientEntityCollision_nHandleCollision(
    JNIEnv* env, jclass clazz,
    jobject shapePtrs, jobject isActive,
    jobject posX, jobject posY, jobject posZ,
    jobject rotX, jobject rotY, jobject rotZ, jobject rotW,
    jobject velX, jobject velY, jobject velZ,
    jobject angVelX, jobject angVelY, jobject angVelZ,
    jint capacity,
    jfloat boxHx, jfloat boxHy, jfloat boxHz,
    jfloat boxX, jfloat boxY, jfloat boxZ,
    jfloat dx, jfloat dy, jfloat dz, jfloat stepHeight,
    jfloatArray outResult, jint lastGroundBodyId, jfloat dt
);

/**
 * Bounds checking for sneaking client-side entities on physics platforms.
 * Prevents the entity from sliding or walking off structural platform edges.
 *
 * @param env JNI interface pointer.
 * @param clazz Associated JNI class context.
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
 * @param dx Proposed movement offset on X.
 * @param dz Proposed movement offset on Z.
 * @param maxDrop Permitted ledge drop offset.
 * @param outResult Target array returning corrected motion boundaries.
 */
JNIEXPORT void JNICALL Java_net_xmx_velthoric_jni_ClientEntityCollision_nHandleSneak(
    JNIEnv* env, jclass clazz,
    jobject shapePtrs, jobject isActive,
    jobject posX, jobject posY, jobject posZ,
    jobject rotX, jobject rotY, jobject rotZ, jobject rotW,
    jint capacity,
    jfloat boxHx, jfloat boxHy, jfloat boxHz,
    jfloat boxX, jfloat boxY, jfloat boxZ,
    jfloat dx, jfloat dz, jfloat maxDrop,
    jfloatArray outResult
);

/**
 * Static client-side intersection check. Verifies if an entity's custom axis-aligned
 * bounding box intersects with any Velthoric physics platforms.
 *
 * @param env JNI interface pointer.
 * @param clazz Associated JNI class context.
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
 * @return True if static intersections are detected.
 */
JNIEXPORT jboolean JNICALL Java_net_xmx_velthoric_jni_ClientEntityCollision_nIsColliding(
    JNIEnv* env, jclass clazz,
    jobject shapePtrs, jobject isActive,
    jobject posX, jobject posY, jobject posZ,
    jobject rotX, jobject rotY, jobject rotZ, jobject rotW,
    jint capacity,
    jfloat boxHx, jfloat boxHy, jfloat boxHz,
    jfloat boxX, jfloat boxY, jfloat boxZ
);

}
