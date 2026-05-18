/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 *
 * Author: xI-Mx-Ix
 */
#include "EntityBridgeManager.h"
#include <Jolt/Jolt.h>
#include <Jolt/Physics/Collision/Shape/Shape.h>
#include <Jolt/Physics/Collision/Shape/BoxShape.h>
#include <Jolt/Physics/Collision/CollisionDispatch.h>
#include <Jolt/Physics/Collision/CollideShape.h>
#include <algorithm>
#include <cmath>

using namespace JPH;

namespace Velthoric {
/**
 * Initializes JNI resources for the EntityBridgeManager.
 */
void EntityBridgeManager::InitJNI(JNIEnv* env) {}
}

extern "C" {

/**
 * Native implementation for handling complex character collision with physics bodies.
 * Resolves penetration, sliding on slopes, and step height (stairs).
 */
JNIEXPORT void JNICALL Java_net_xmx_velthoric_jni_EntityBridge_nHandleCollision(
    JNIEnv* env, jclass clazz,
    jlongArray shapePtrs, jfloatArray transforms, jint count,
    jfloat boxHx, jfloat boxHy, jfloat boxHz,
    jfloat boxX, jfloat boxY, jfloat boxZ,
    jfloat dx, jfloat dy, jfloat dz, jfloat stepHeight,
    jfloatArray outResult)
{
    (void)clazz;
    if (count == 0) {
        jfloat res[5] = {dx, dy, dz, -1.0f, 1.0f};
        env->SetFloatArrayRegion(outResult, 0, 5, res);
        return;
    }

    jlong* shapes = env->GetLongArrayElements(shapePtrs, nullptr);
    jfloat* trans = env->GetFloatArrayElements(transforms, nullptr);
    jfloat* res = env->GetFloatArrayElements(outResult, nullptr);

    /**
     * Lambda to cast the entity box downwards to find the ground or step surface.
     */
    auto castShapeDown = [&](float startX, float startY, float startZ, float distDown, float& outHitY, Vec3& outNormal, int& outHitBody) {
        outHitY = startY - distDown;
        outNormal = Vec3(0.0f, 1.0f, 0.0f);
        outHitBody = -1;

        float sHx = std::max(0.001f, boxHx - 0.005f);
        float sHz = std::max(0.001f, boxHz - 0.005f);
        BoxShape boxShape(Vec3(sHx, boxHy, sHz));

        Mat44 boxMat = Mat44::sTranslation(Vec3(startX, startY, startZ));
        Vec3 dir(0.0f, -distDown, 0.0f);

        ShapeCast shapeCast(&boxShape, Vec3::sReplicate(1.0f), boxMat, dir);
        ShapeCastSettings settings;
        settings.mReturnDeepestPoint = true;
        settings.mBackFaceModeTriangles = EBackFaceMode::IgnoreBackFaces;

        struct HitCol : public CastShapeCollector {
            bool hit = false;
            ShapeCastResult result;
            int currentBody = -1;
            int hitBody = -1;
            virtual void AddHit(const ShapeCastResult& inResult) override {
                if (!hit || inResult.mFraction < result.mFraction) {
                    hit = true;
                    result = inResult;
                    hitBody = currentBody;
                }
            }
        } collector;

        for (int i = 0; i < count; i++) {
            const Shape* shape = reinterpret_cast<const Shape*>(shapes[i]);
            if (!shape) continue;

            collector.currentBody = i;
            int tIdx = i * 7;
            float tsx = trans[tIdx], tsy = trans[tIdx+1], tsz = trans[tIdx+2];
            float sqx = trans[tIdx+3], sqy = trans[tIdx+4], sqz = trans[tIdx+5], sqw = trans[tIdx+6];
            Mat44 shapeMat = Mat44::sRotationTranslation(Quat(sqx, sqy, sqz, sqw), Vec3(tsx, tsy, tsz));

            CollisionDispatch::sCastShapeVsShapeWorldSpace(shapeCast, settings, shape, Vec3::sReplicate(1.0f), ShapeFilter(), shapeMat, SubShapeIDCreator(), SubShapeIDCreator(), collector);
        }

        if (collector.hit) {
            outHitY = startY - distDown * collector.result.mFraction;
            outNormal = -collector.result.mPenetrationAxis.Normalized();
            outHitBody = collector.hitBody;
            return true;
        }
        return false;
    };

    /**
     * Iteratively resolves penetration depth between the entity box and multiple shapes.
     */
    auto resolvePenetrations = [&](float& px, float& py, float& pz, float& outPushX, float& outPushY, float& outPushZ, Vec3& outMaxNormal, int& bodyIndexOut, float& outSlideFriction) {
        outMaxNormal = Vec3(0.0f, -1.0f, 0.0f);
        int iterations = 4;
        bool hitAny = false;
        bodyIndexOut = -1;

        for (int iter = 0; iter < iterations; iter++) {
            bool resolved = false;
            for (int i = 0; i < count; i++) {
                const Shape* shape = reinterpret_cast<const Shape*>(shapes[i]);
                if (!shape) continue;

                int tIdx = i * 7;
                float sx = trans[tIdx], sy = trans[tIdx+1], sz = trans[tIdx+2];
                float sqx = trans[tIdx+3], sqy = trans[tIdx+4], sqz = trans[tIdx+5], sqw = trans[tIdx+6];

                BoxShape boxShape(Vec3(boxHx, boxHy, boxHz));
                Mat44 shapeMat = Mat44::sRotationTranslation(Quat(sqx, sqy, sqz, sqw), Vec3(sx, sy, sz));
                Mat44 boxMat = Mat44::sTranslation(Vec3(px, py, pz));

                CollideShapeSettings settings;
                settings.mMaxSeparationDistance = 0.0f;

                struct Collector : public CollideShapeCollector {
                    bool hit = false;
                    CollideShapeResult result;
                    virtual void AddHit(const CollideShapeResult& inResult) override {
                        if (!hit || inResult.mPenetrationDepth > result.mPenetrationDepth) {
                            hit = true;
                            result = inResult;
                        }
                    }
                } collector;

                CollisionDispatch::sCollideShapeVsShape(&boxShape, shape, Vec3::sReplicate(1.0f), Vec3::sReplicate(1.0f), boxMat, shapeMat, SubShapeIDCreator(), SubShapeIDCreator(), settings, collector);

                if (collector.hit && collector.result.mPenetrationDepth > 1e-4f) {
                    Vec3 normal = -collector.result.mPenetrationAxis.Normalized();
                    float depth = collector.result.mPenetrationDepth;
                    Vec3 push(0.0f, 0.0f, 0.0f);

                    float ny = normal.GetY();
                    float nHorizSq = normal.GetX() * normal.GetX() + normal.GetZ() * normal.GetZ();

                    if (ny >= 0.65f) {
                        push = Vec3(0.0f, depth / ny, 0.0f);
                    } else if (nHorizSq > 0.0001f) {
                        float k = depth / nHorizSq;
                        if (k > 2.0f / (std::sqrt(nHorizSq) + 0.001f)) k = 2.0f / (std::sqrt(nHorizSq) + 0.001f);
                        push = Vec3(normal.GetX() * k, 0.0f, normal.GetZ() * k);
                    } else {
                        push = Vec3(0.0f, -depth, 0.0f);
                    }

                    px += push.GetX();
                    py += push.GetY();
                    pz += push.GetZ();
                    outPushX += push.GetX();
                    outPushY += push.GetY();
                    outPushZ += push.GetZ();

                    if (normal.GetY() > outMaxNormal.GetY()) {
                        outMaxNormal = normal;
                        if (normal.GetY() > 0.1f) bodyIndexOut = i;
                    }
                    resolved = true;
                }
            }
            if (!resolved) break;
            hitAny = true;
        }
        return hitAny;
    };

    float basePushX = 0, basePushY = 0, basePushZ = 0;
    float px = boxX + dx;
    float py = boxY + dy;
    float pz = boxZ + dz;

    Vec3 baseMaxNormal(0.0f, -1.0f, 0.0f);
    int baseWalkedOn = -1;
    float slideFriction = 1.0f;
    resolvePenetrations(px, py, pz, basePushX, basePushY, basePushZ, baseMaxNormal, baseWalkedOn, slideFriction);

    bool horizontalRestricted = (std::abs(basePushX) > 1e-4f) || (std::abs(basePushZ) > 1e-4f);

    // Step height logic: Attempt to step over horizontal obstacles
    if (horizontalRestricted && stepHeight > 0.0f && dy <= 0.0f) {
        float stepPushX = 0, stepPushY = 0, stepPushZ = 0;
        float sx = boxX + dx;
        float sy = boxY + dy + stepHeight;
        float sz = boxZ + dz;

        Vec3 dumpNormal;
        int stepBody = -1;
        float dumpFriction = 1.0f;
        resolvePenetrations(sx, sy, sz, stepPushX, stepPushY, stepPushZ, dumpNormal, stepBody, dumpFriction);

        float dropAmount = stepHeight + std::max(0.0f, stepPushY) + 0.05f;
        float landedY;
        Vec3 landedNormal(0.0f, 1.0f, 0.0f);
        int landedBody;
        bool hitDown = castShapeDown(sx, sy, sz, dropAmount, landedY, landedNormal, landedBody);

        if (!hitDown) {
            landedY = sy - stepHeight;
            landedNormal = Vec3(0.0f, 1.0f, 0.0f);
        }

        if (landedNormal.GetY() >= 0.65f) {
            float baseProgress = (dx + basePushX) * dx + (dz + basePushZ) * dz;
            float stepProgress = (dx + stepPushX) * dx + (dz + stepPushZ) * dz;
            float actualStepUp = landedY - (boxY + dy);

            if (stepProgress > baseProgress + 1e-4f && stepProgress > 0.0f && actualStepUp > 0.01f) {
                basePushX = stepPushX;
                basePushZ = stepPushZ;
                basePushY = actualStepUp;
                if (hitDown && landedBody != -1) baseWalkedOn = landedBody;
                slideFriction = 1.0f;
                baseMaxNormal = landedNormal;
            }
        }
    }

    res[0] = dx + basePushX;
    res[1] = dy + basePushY;
    res[2] = dz + basePushZ;

    if (std::abs(res[0]) < 1e-4f) res[0] = 0.0f;
    if (std::abs(res[1]) < 1e-4f) res[1] = 0.0f;
    if (std::abs(res[2]) < 1e-4f) res[2] = 0.0f;

    // Steepness logic: Slide down slopes that are too steep to stand on
    float ny = baseMaxNormal.GetY();
    if (ny >= 0.65f && ny < 0.707f) {
        float steepness = (0.707f - ny) / 0.057f;
        float upX = -baseMaxNormal.GetX();
        float upZ = -baseMaxNormal.GetZ();
        float upLen = std::sqrt(upX * upX + upZ * upZ);

        if (upLen > 0.001f) {
            upX /= upLen;
            upZ /= upLen;
            float dot = res[0] * upX + res[2] * upZ;
            if (dot > 0.0f) {
                float reduction = steepness * steepness;
                if (reduction > 1.0f) reduction = 1.0f;
                float removedAmount = dot * reduction;
                res[0] -= upX * removedAmount;
                res[2] -= upZ * removedAmount;
                float removeY = removedAmount * upLen / ny;
                res[1] -= removeY;
            }
        }
    }

    res[3] = static_cast<float>(baseWalkedOn);
    res[4] = slideFriction;

    env->ReleaseLongArrayElements(shapePtrs, shapes, JNI_ABORT);
    env->ReleaseFloatArrayElements(transforms, trans, JNI_ABORT);
    env->SetFloatArrayRegion(outResult, 0, 5, res);
}

/**
 * Fast intersection check for a bounding box against a list of shapes.
 */
JNIEXPORT jboolean JNICALL Java_net_xmx_velthoric_jni_EntityBridge_nIsColliding(
    JNIEnv* env, jclass clazz,
    jlongArray shapePtrs, jfloatArray transforms, jint count,
    jfloat boxHx, jfloat boxHy, jfloat boxHz,
    jfloat boxX, jfloat boxY, jfloat boxZ)
{
    (void)clazz;
    if (count == 0) return JNI_FALSE;

    jlong* shapes = env->GetLongArrayElements(shapePtrs, nullptr);
    jfloat* trans = env->GetFloatArrayElements(transforms, nullptr);

    boxHy += 0.05f;
    boxY -= 0.05f;

    bool anyHit = false;
    for (int i = 0; i < count; i++) {
        const Shape* shape = reinterpret_cast<const Shape*>(shapes[i]);
        if (!shape) continue;

        int tIdx = i * 7;
        float sx = trans[tIdx], sy = trans[tIdx+1], sz = trans[tIdx+2];
        float sqx = trans[tIdx+3], sqy = trans[tIdx+4], sqz = trans[tIdx+5], sqw = trans[tIdx+6];

        BoxShape boxShape(Vec3(boxHx, boxHy, boxHz));
        Mat44 shapeMat = Mat44::sRotationTranslation(Quat(sqx, sqy, sqz, sqw), Vec3(sx, sy, sz));
        Mat44 boxMat = Mat44::sTranslation(Vec3(boxX, boxY, boxZ));

        CollideShapeSettings settings;
        settings.mMaxSeparationDistance = 0.0f;

        struct Collector : public CollideShapeCollector {
            bool hit = false;
            virtual void AddHit(const CollideShapeResult& inResult) override {
                if (inResult.mPenetrationDepth > 0.0001f) hit = true;
            }
        } collector;

        CollisionDispatch::sCollideShapeVsShape(&boxShape, shape, Vec3::sReplicate(1.0f), Vec3::sReplicate(1.0f), boxMat, shapeMat, SubShapeIDCreator(), SubShapeIDCreator(), settings, collector);

        if (collector.hit) {
            anyHit = true;
            break;
        }
    }

    env->ReleaseLongArrayElements(shapePtrs, shapes, JNI_ABORT);
    env->ReleaseFloatArrayElements(transforms, trans, JNI_ABORT);
    return anyHit ? JNI_TRUE : JNI_FALSE;
}

/**
 * Resolves sneaking movement. Prevents falling off edges by checking safety in multiple directions.
 */
JNIEXPORT void JNICALL Java_net_xmx_velthoric_jni_EntityBridge_nHandleSneak(
    JNIEnv* env, jclass clazz,
    jlongArray shapePtrs, jfloatArray transforms, jint count,
    jfloat boxHx, jfloat boxHy, jfloat boxHz,
    jfloat boxX, jfloat boxY, jfloat boxZ,
    jfloat dx, jfloat dz, jfloat maxDrop,
    jfloatArray outResult)
{
    (void)clazz;
    jfloat res[2] = {dx, dz};
    if (count == 0) {
        env->SetFloatArrayRegion(outResult, 0, 2, res);
        return;
    }

    jlong* shapes = env->GetLongArrayElements(shapePtrs, nullptr);
    jfloat* trans = env->GetFloatArrayElements(transforms, nullptr);

    // Checks if the target position has ground underneath
    auto checkSafe = [&](float testX, float testZ) {
        float sHx = std::max(0.02f, boxHx * 0.4f);
        float sHz = std::max(0.02f, boxHz * 0.4f);
        BoxShape boxShape(Vec3(sHx, boxHy, sHz));

        Mat44 boxMat = Mat44::sTranslation(Vec3(testX, boxY + 0.1f, testZ));
        Vec3 dir(0.0f, -(maxDrop + 0.2f), 0.0f);

        ShapeCast shapeCast(&boxShape, Vec3::sReplicate(1.0f), boxMat, dir);
        ShapeCastSettings settings;
        settings.mReturnDeepestPoint = true;
        settings.mBackFaceModeTriangles = EBackFaceMode::IgnoreBackFaces;

        struct HitCol : public CastShapeCollector {
            bool hit = false;
            ShapeCastResult result;
            virtual void AddHit(const ShapeCastResult& inResult) override {
                Vec3 normal = -inResult.mPenetrationAxis.Normalized();
                if (normal.GetY() < 0.65f) return;
                if (!hit || inResult.mFraction < result.mFraction) {
                    hit = true;
                    result = inResult;
                }
            }
        } collector;

        for (int i = 0; i < count; i++) {
            const Shape* shape = reinterpret_cast<const Shape*>(shapes[i]);
            if (!shape) continue;
            int tIdx = i * 7;
            float sx = trans[tIdx], sy = trans[tIdx+1], sz = trans[tIdx+2];
            float sqx = trans[tIdx+3], sqy = trans[tIdx+4], sqz = trans[tIdx+5], sqw = trans[tIdx+6];
            Mat44 shapeMat = Mat44::sRotationTranslation(Quat(sqx, sqy, sqz, sqw), Vec3(sx, sy, sz));
            CollisionDispatch::sCastShapeVsShapeWorldSpace(shapeCast, settings, shape, Vec3::sReplicate(1.0f), ShapeFilter(), shapeMat, SubShapeIDCreator(), SubShapeIDCreator(), collector);
        }

        if (collector.hit) {
            float dropDist = (maxDrop + 0.2f) * collector.result.mFraction;
            float hitY = (boxY + 0.1f) - dropDist;
            float feetY = boxY - boxHy;
            if (hitY >= feetY - maxDrop - 0.05f) return true;
        }
        return false;
    };

    if (!checkSafe(boxX, boxZ)) {
        env->ReleaseLongArrayElements(shapePtrs, shapes, JNI_ABORT);
        env->ReleaseFloatArrayElements(transforms, trans, JNI_ABORT);
        env->SetFloatArrayRegion(outResult, 0, 2, res);
        return;
    }

    float testX = dx;
    float testZ = dz;

    // If direct move is unsafe, search for safe alternative angles
    if (!checkSafe(boxX + testX, boxZ + testZ)) {
        bool foundSafe = false;
        float lenSq = dx*dx + dz*dz;

        if (lenSq > 0.000001f) {
            for (int i = 1; i <= 18; i++) {
                float rad = (i * 5.0f) * 3.1415926535f / 180.0f;
                float cosA = std::cos(rad);
                float sinA = std::sin(rad);
                float scale = cosA;

                float rx = (dx * cosA - dz * sinA) * scale;
                float rz = (dx * sinA + dz * cosA) * scale;
                if (checkSafe(boxX + rx, boxZ + rz)) {
                    testX = rx; testZ = rz; foundSafe = true; break;
                }

                float lx = (dx * cosA + dz * sinA) * scale;
                float lz = (-dx * sinA + dz * cosA) * scale;
                if (checkSafe(boxX + lx, boxZ + lz)) {
                    testX = lx; testZ = lz; foundSafe = true; break;
                }
            }
        }

        if (!foundSafe) {
            testX = 0.0f;
            testZ = 0.0f;
        }
    }

    res[0] = testX;
    res[1] = testZ;

    env->ReleaseLongArrayElements(shapePtrs, shapes, JNI_ABORT);
    env->ReleaseFloatArrayElements(transforms, trans, JNI_ABORT);
    env->SetFloatArrayRegion(outResult, 0, 2, res);
}

}