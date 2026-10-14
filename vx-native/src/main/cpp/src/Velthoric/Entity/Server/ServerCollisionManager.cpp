/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 *
 * Author: xI-Mx-Ix
 */
#include "Velthoric/Entity/Server/ServerCollisionManager.h"
#include "Velthoric/Entity/CollisionManagerCommon.h"

extern "C" {

JNIEXPORT void JNICALL Java_net_xmx_velthoric_jni_ServerEntityCollision_nHandleCollision(
    JNIEnv* env, jclass clazz, jlong physicsSystemVa,
    jobject shapePtrs, jobject isActive,
    jobject posX, jobject posY, jobject posZ,
    jobject rotX, jobject rotY, jobject rotZ, jobject rotW,
    jobject velX, jobject velY, jobject velZ,
    jobject angVelX, jobject angVelY, jobject angVelZ,
    jobject bodyIds, jint capacity,
    jfloat boxHx, jfloat boxHy, jfloat boxHz,
    jfloat boxX, jfloat boxY, jfloat boxZ,
    jfloat dx, jfloat dy, jfloat dz, jfloat stepHeight,
    jfloat entityMass, jfloat entityVelocityX, jfloat entityVelocityY, jfloat entityVelocityZ,
    jfloatArray outResult, jint lastGroundBodyId, jfloat dt
) {
    (void)clazz;
    (void)isActive;

    JPH::PhysicsSystem* ps = physicsSystemVa != 0 ? reinterpret_cast<JPH::PhysicsSystem*>(physicsSystemVa) : nullptr;
    jfloat res[6] = {0};

    // If the capacity is zero, no collisions can occur, so return input displacement
    if (capacity == 0) {
        res[0] = dx; res[1] = dy; res[2] = dz;
        res[3] = -1.0f; res[4] = 1.0f; res[5] = 0.0f;
        env->SetFloatArrayRegion(outResult, 0, 6, res);
        return;
    }

    // Populate the collision context struct for Jolt narrow phase traversal
    Velthoric::CollisionContext ctx;
    ctx.ps = ps;
    ctx.shapes = (jlong*)env->GetDirectBufferAddress(shapePtrs);
    ctx.pX = (jdouble*)env->GetDirectBufferAddress(posX);
    ctx.pY = (jdouble*)env->GetDirectBufferAddress(posY);
    ctx.pZ = (jdouble*)env->GetDirectBufferAddress(posZ);
    ctx.rX = (jfloat*)env->GetDirectBufferAddress(rotX);
    ctx.rY = (jfloat*)env->GetDirectBufferAddress(rotY);
    ctx.rZ = (jfloat*)env->GetDirectBufferAddress(rotZ);
    ctx.rW = (jfloat*)env->GetDirectBufferAddress(rotW);
    ctx.bIds = (jint*)env->GetDirectBufferAddress(bodyIds);
    ctx.capacity = capacity;
    ctx.boxHx = boxHx; ctx.boxHy = boxHy; ctx.boxHz = boxHz;
    ctx.entityMass = entityMass;
    ctx.entityVelocityX = entityVelocityX;
    ctx.entityVelocityY = entityVelocityY;
    ctx.entityVelocityZ = entityVelocityZ;

    ctx.lastGroundIdx = -1;
    ctx.hasGround = false;
    ctx.groundDisplacement = JPH::Vec3::sZero();
    ctx.groundRotDisp = JPH::Quat::sIdentity();
    res[5] = 0.0f;

    // Execute core Jolt collision resolver
    Velthoric::HandleCollisionCore(ctx, boxX, boxY, boxZ, dx, dy, dz, stepHeight, res);
    env->SetFloatArrayRegion(outResult, 0, 6, res);
}

JNIEXPORT void JNICALL Java_net_xmx_velthoric_jni_ServerEntityCollision_nHandleSneak(
    JNIEnv* env, jclass clazz, jlong physicsSystemVa,
    jobject shapePtrs, jobject isActive,
    jobject posX, jobject posY, jobject posZ,
    jobject rotX, jobject rotY, jobject rotZ, jobject rotW,
    jint capacity,
    jfloat boxHx, jfloat boxHy, jfloat boxHz,
    jfloat boxX, jfloat boxY, jfloat boxZ,
    jfloat dx, jfloat dz, jfloat maxDrop,
    jfloatArray outResult
) {
    (void)clazz; (void)isActive; (void)physicsSystemVa;
    jfloat res[2] = {dx, dz};
    if (capacity == 0) {
        env->SetFloatArrayRegion(outResult, 0, 2, res);
        return;
    }

    // Prepare structure context for edge bounding evaluation
    Velthoric::CollisionContext ctx;
    ctx.ps = nullptr;
    ctx.shapes = (jlong*)env->GetDirectBufferAddress(shapePtrs);
    ctx.pX = (jdouble*)env->GetDirectBufferAddress(posX);
    ctx.pY = (jdouble*)env->GetDirectBufferAddress(posY);
    ctx.pZ = (jdouble*)env->GetDirectBufferAddress(posZ);
    ctx.rX = (jfloat*)env->GetDirectBufferAddress(rotX);
    ctx.rY = (jfloat*)env->GetDirectBufferAddress(rotY);
    ctx.rZ = (jfloat*)env->GetDirectBufferAddress(rotZ);
    ctx.rW = (jfloat*)env->GetDirectBufferAddress(rotW);
    ctx.bIds = nullptr;
    ctx.capacity = capacity;
    ctx.boxHx = boxHx; ctx.boxHy = boxHy; ctx.boxHz = boxHz;
    ctx.lastGroundIdx = -1;
    ctx.hasGround = false;
    ctx.groundDisplacement = JPH::Vec3::sZero();
    ctx.groundRotDisp = JPH::Quat::sIdentity();

    // Call sneaking core utility
    Velthoric::HandleSneakCore(ctx, boxX, boxY, boxZ, dx, dz, maxDrop, res);
    env->SetFloatArrayRegion(outResult, 0, 2, res);
}

JNIEXPORT jboolean JNICALL Java_net_xmx_velthoric_jni_ServerEntityCollision_nIsColliding(
    JNIEnv* env, jclass clazz, jlong physicsSystemVa,
    jobject shapePtrs, jobject isActive,
    jobject posX, jobject posY, jobject posZ,
    jobject rotX, jobject rotY, jobject rotZ, jobject rotW,
    jint capacity,
    jfloat boxHx, jfloat boxHy, jfloat boxHz,
    jfloat boxX, jfloat boxY, jfloat boxZ
) {
    (void)clazz; (void)isActive; (void)physicsSystemVa;
    if (capacity == 0) return JNI_FALSE;

    // Instantiate temporary state context for static bounding box intersection tests
    Velthoric::CollisionContext ctx;
    ctx.ps = nullptr;
    ctx.shapes = (jlong*)env->GetDirectBufferAddress(shapePtrs);
    ctx.pX = (jdouble*)env->GetDirectBufferAddress(posX);
    ctx.pY = (jdouble*)env->GetDirectBufferAddress(posY);
    ctx.pZ = (jdouble*)env->GetDirectBufferAddress(posZ);
    ctx.rX = (jfloat*)env->GetDirectBufferAddress(rotX);
    ctx.rY = (jfloat*)env->GetDirectBufferAddress(rotY);
    ctx.rZ = (jfloat*)env->GetDirectBufferAddress(rotZ);
    ctx.rW = (jfloat*)env->GetDirectBufferAddress(rotW);
    ctx.capacity = capacity;
    ctx.boxHx = boxHx; ctx.boxHy = boxHy; ctx.boxHz = boxHz;

    return Velthoric::IsCollidingCore(ctx, boxX, boxY, boxZ) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_net_xmx_velthoric_jni_ServerEntityCollision_nGetCollidingBodyId(
    JNIEnv* env, jclass clazz, jlong physicsSystemVa,
    jobject shapePtrs, jobject isActive,
    jobject posX, jobject posY, jobject posZ,
    jobject rotX, jobject rotY, jobject rotZ, jobject rotW,
    jint capacity,
    jfloat boxHx, jfloat boxHy, jfloat boxHz,
    jfloat boxX, jfloat boxY, jfloat boxZ
) {
    (void)clazz; (void)isActive; (void)physicsSystemVa;
    if (capacity == 0) return -1;

    Velthoric::CollisionContext ctx;
    ctx.ps = nullptr;
    ctx.shapes = (jlong*)env->GetDirectBufferAddress(shapePtrs);
    ctx.pX = (jdouble*)env->GetDirectBufferAddress(posX);
    ctx.pY = (jdouble*)env->GetDirectBufferAddress(posY);
    ctx.pZ = (jdouble*)env->GetDirectBufferAddress(posZ);
    ctx.rX = (jfloat*)env->GetDirectBufferAddress(rotX);
    ctx.rY = (jfloat*)env->GetDirectBufferAddress(rotY);
    ctx.rZ = (jfloat*)env->GetDirectBufferAddress(rotZ);
    ctx.rW = (jfloat*)env->GetDirectBufferAddress(rotW);
    ctx.capacity = capacity;
    ctx.boxHx = boxHx; ctx.boxHy = boxHy; ctx.boxHz = boxHz;

    return Velthoric::GetCollidingBodyIdCore(ctx, boxX, boxY, boxZ);
}

}
