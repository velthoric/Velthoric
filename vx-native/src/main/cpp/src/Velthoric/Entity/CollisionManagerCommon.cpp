/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 *
 * Author: xI-Mx-Ix
 */
#include "CollisionManagerCommon.h"

namespace Velthoric {

bool CastShapeDown(const CollisionContext& ctx, float startX, float startY, float startZ, float distDown, float& outHitY, Vec3& outNormal, int& outHitBody) {
    outHitY = startY - distDown;
    outNormal = Vec3(0.0f, 1.0f, 0.0f);
    outHitBody = -1;

    // Shrink the checking footprint slightly to prevent edge snagging
    float sHx = std::max(0.001f, ctx.boxHx - 0.005f);
    float sHz = std::max(0.001f, ctx.boxHz - 0.005f);
    BoxShape boxShape(Vec3(sHx, ctx.boxHy, sHz));

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
        const CollisionContext* ctxPtr = nullptr;

        virtual void AddHit(const ShapeCastResult& inResult) override {
            uint32_t bodyIdVal = (ctxPtr->bIds && currentBody >= 0 && currentBody < ctxPtr->capacity) ? static_cast<uint32_t>(ctxPtr->bIds[currentBody]) : 0;
            if (ctxPtr->ps && bodyIdVal != 0) {
                BodyID id(bodyIdVal);
                BodyLockRead lock(ctxPtr->ps->GetBodyLockInterface(), id);
                if (lock.Succeeded()) {
                    const Body& body = lock.GetBody();
                    // Ignore collision feedback if body is dynamic with very low mass (kicking objects)
                    if (body.GetMotionType() == EMotionType::Dynamic) {
                        float invMass = 0.0f;
                        float bodyMass = 0.0f;
                        if (body.GetMotionProperties()) {
                            invMass = body.GetMotionProperties()->GetInverseMass();
                            if (invMass > 0.0f) {
                                bodyMass = 1.0f / invMass;
                            }
                        }
                        if (bodyMass <= 10.0f) {
                            return;
                        }
                    }
                }
            }

            if (!hit || inResult.mFraction < result.mFraction) {
                hit = true;
                result = inResult;
                hitBody = currentBody;
            }
        }
    } collector;

    collector.ctxPtr = &ctx;

    // Perform continuous sweeping checks across active shape references
    for (int i = 0; i < ctx.capacity; i++) {
        const Shape* shape = reinterpret_cast<const Shape*>(ctx.shapes[i]);
        if (!shape) continue;

        collector.currentBody = i;
        float tsx = static_cast<float>(ctx.pX[i]);
        float tsy = static_cast<float>(ctx.pY[i]);
        float tsz = static_cast<float>(ctx.pZ[i]);
        Quat sq(ctx.rX[i], ctx.rY[i], ctx.rZ[i], ctx.rW[i]);

        Mat44 shapeMat = Mat44::sRotationTranslation(sq, Vec3(tsx, tsy, tsz));
        CollisionDispatch::sCastShapeVsShapeWorldSpace(shapeCast, settings, shape, Vec3::sReplicate(1.0f), ShapeFilter(), shapeMat, SubShapeIDCreator(), SubShapeIDCreator(), collector);
    }

    if (collector.hit) {
        outHitY = startY - distDown * collector.result.mFraction;
        outNormal = -collector.result.mPenetrationAxis.Normalized();
        outHitBody = collector.hitBody;
        return true;
    }
    return false;
}

bool ResolvePenetrations(const CollisionContext& ctx, float& px, float& py, float& pz, float& outPushX, float& outPushY, float& outPushZ, Vec3& outMaxNormal, int& bodyIndexOut, float& outSlideFriction) {
    outMaxNormal = Vec3(0.0f, -1.0f, 0.0f);
    int iterations = 4;
    bool hitAny = false;
    bodyIndexOut = -1;

    for (int iter = 0; iter < iterations; iter++) {
        bool resolved = false;
        for (int i = 0; i < ctx.capacity; i++) {
            const Shape* shape = reinterpret_cast<const Shape*>(ctx.shapes[i]);
            if (!shape) continue;

            float sx = static_cast<float>(ctx.pX[i]);
            float sy = static_cast<float>(ctx.pY[i]);
            float sz = static_cast<float>(ctx.pZ[i]);
            Quat sq(ctx.rX[i], ctx.rY[i], ctx.rZ[i], ctx.rW[i]);

            BoxShape boxShape(Vec3(ctx.boxHx, ctx.boxHy, ctx.boxHz));
            Mat44 shapeMat = Mat44::sRotationTranslation(sq, Vec3(sx, sy, sz));
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

                // Compute exact correction translations
                if (ny >= 0.65f) {
                    push = Vec3(0.0f, depth / ny, 0.0f);
                } else if (nHorizSq > 0.0001f) {
                    float k = depth / nHorizSq;
                    if (k > 2.0f / (std::sqrt(nHorizSq) + 0.001f)) k = 2.0f / (std::sqrt(nHorizSq) + 0.001f);
                    push = Vec3(normal.GetX() * k, 0.0f, normal.GetZ() * k);
                } else {
                    push = Vec3(0.0f, -depth, 0.0f);
                }

                uint32_t bodyIdVal = (ctx.bIds && i < ctx.capacity) ? static_cast<uint32_t>(ctx.bIds[i]) : 0;
                bool isDynamicBody = false;
                float bodyMass = 0.0f;
                float invMass = 0.0f;
                RVec3 bodyPos = RVec3::sZero();
                Vec3 bodyVel(0.0f, 0.0f, 0.0f);
                bool bodyIsActive = false;

                // Handle server dynamics interaction
                if (ctx.ps && bodyIdVal != 0) {
                    BodyID id(bodyIdVal);
                    BodyLockRead lock(ctx.ps->GetBodyLockInterface(), id);
                    if (lock.Succeeded()) {
                        const Body& body = lock.GetBody();
                        if (body.GetMotionType() == EMotionType::Dynamic) {
                            isDynamicBody = true;
                            bodyPos = body.GetPosition();
                            bodyVel = body.GetLinearVelocity();
                            bodyIsActive = body.IsActive();
                            if (body.GetMotionProperties()) {
                                invMass = body.GetMotionProperties()->GetInverseMass();
                                if (invMass > 0.0f) {
                                    bodyMass = 1.0f / invMass;
                                }
                            }
                        }
                    }
                }

                float totalMass = ctx.entityMass + bodyMass;
                float entityPushRatio = 1.0f;
                if (isDynamicBody && totalMass > 0.0f) {
                    entityPushRatio = bodyMass / totalMass;

                    float support = 0.0f;
                    if (bodyMass >= 40.0f) {
                        support = 1.0f;
                    } else if (bodyMass > 10.0f) {
                        support = (bodyMass - 10.0f) / 30.0f;
                    }
                    entityPushRatio *= support;
                }

                px += push.GetX() * entityPushRatio;
                py += push.GetY() * entityPushRatio;
                pz += push.GetZ() * entityPushRatio;
                outPushX += push.GetX() * entityPushRatio;
                outPushY += push.GetY() * entityPushRatio;
                outPushZ += push.GetZ() * entityPushRatio;

                // Dynamically apply impulses back to physics simulations
                if (isDynamicBody && ctx.ps && bodyIdVal != 0) {
                    float bodyPushRatio = 1.0f - entityPushRatio;
                    Vec3 bodyPush = -push * bodyPushRatio;
                    BodyID id(bodyIdVal);

                    Vec3 entityVel(ctx.entityVelocityX, ctx.entityVelocityY, ctx.entityVelocityZ);
                    Vec3 relVel = entityVel - bodyVel;
                    float approachSpeed = -relVel.Dot(normal);

                    float e = 0.25f;
                    float J_vel = 0.0f;
                    if (approachSpeed > 0.0f) {
                        J_vel = (approachSpeed * (1.0f + e)) / ((1.0f / ctx.entityMass) + invMass);

                        if (bodyMass < 10.0f) {
                            float kickMultiplier = 1.0f + (10.0f - bodyMass) * 1.8f;
                            J_vel *= kickMultiplier;
                        }
                    }

                    float dtImpulse = 0.05f;
                    float J_pen = depth / (dtImpulse * ((1.0f / ctx.entityMass) + invMass));

                    float J_total = J_vel + J_pen * 0.5f;

                    if (normal.GetY() >= 0.65f) {
                        float J_weight = ctx.entityMass * 9.81f * dtImpulse * normal.GetY();
                        J_total += J_weight;
                    }

                    float maxVelChange = 15.0f;
                    if (bodyMass < 10.0f) {
                        maxVelChange = 15.0f + (10.0f - bodyMass) * 20.0f;
                    }
                    float maxImpulse = bodyMass * maxVelChange;
                    if (J_total > maxImpulse) {
                        J_total = maxImpulse;
                    }

                    Vec3 impulse = -normal * J_total;
                    ctx.ps->GetBodyInterface().AddImpulse(id, impulse);

                    if (!bodyIsActive) {
                        ctx.ps->GetBodyInterface().ActivateBody(id);
                    }
                }

                if (normal.GetY() > outMaxNormal.GetY()) {
                    outMaxNormal = normal;
                    if (normal.GetY() > 0.1f) {
                        if (!isDynamicBody) {
                            bodyIndexOut = i;
                            outSlideFriction = 1.0f;
                        } else {
                            float support = 0.0f;
                            if (bodyMass >= 40.0f) {
                                support = 1.0f;
                            } else if (bodyMass > 10.0f) {
                                support = (bodyMass - 10.0f) / 30.0f;
                            }

                            if (support > 0.01f) {
                                bodyIndexOut = i;
                                outSlideFriction = support;
                            }
                        }
                    }
                }
                resolved = true;
            }
        }
        if (!resolved) break;
        hitAny = true;
    }
    return hitAny;
}

void HandleCollisionCore(const CollisionContext& ctx, float boxX, float boxY, float boxZ, float dx, float dy, float dz, float stepHeight, float* outResult) {
    float basePushX = 0, basePushY = 0, basePushZ = 0;

    float px = boxX + dx;
    float py = boxY + dy;
    float pz = boxZ + dz;

    Vec3 baseMaxNormal(0.0f, -1.0f, 0.0f);
    int baseWalkedOn = -1;
    float slideFriction = 1.0f;
    ResolvePenetrations(ctx, px, py, pz, basePushX, basePushY, basePushZ, baseMaxNormal, baseWalkedOn, slideFriction);

    bool horizontalRestricted = (std::abs(basePushX) > 1e-4f) || (std::abs(basePushZ) > 1e-4f);

    // Apply elevation step checks
    if (horizontalRestricted && stepHeight > 0.0f && dy <= 0.0f) {
        float stepPushX = 0, stepPushY = 0, stepPushZ = 0;
        float sx = boxX + dx;
        float sy = boxY + dy + stepHeight;
        float sz = boxZ + dz;

        Vec3 dumpNormal;
        int stepBody = -1;
        float dumpFriction = 1.0f;
        ResolvePenetrations(ctx, sx, sy, sz, stepPushX, stepPushY, stepPushZ, dumpNormal, stepBody, dumpFriction);

        float dropAmount = stepHeight + std::max(0.0f, stepPushY) + 0.05f;
        float landedY;
        Vec3 landedNormal(0.0f, 1.0f, 0.0f);
        int landedBody;
        bool hitDown = CastShapeDown(ctx, sx, sy, sz, dropAmount, landedY, landedNormal, landedBody);

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

    outResult[0] = dx + basePushX;
    outResult[1] = dy + basePushY;
    outResult[2] = dz + basePushZ;

    if (std::abs(outResult[0]) < 1e-4f) outResult[0] = 0.0f;
    if (std::abs(outResult[1]) < 1e-4f) outResult[1] = 0.0f;
    if (std::abs(outResult[2]) < 1e-4f) outResult[2] = 0.0f;

    // Apply slope sliding calculations if walking on sloped faces
    float ny = baseMaxNormal.GetY();
    if (ny >= 0.65f && ny < 0.707f) {
        float steepness = (0.707f - ny) / 0.057f;
        float upX = -baseMaxNormal.GetX();
        float upZ = -baseMaxNormal.GetZ();
        float upLen = std::sqrt(upX * upX + upZ * upZ);

        if (upLen > 0.001f) {
            upX /= upLen;
            upZ /= upLen;
            float dot = outResult[0] * upX + outResult[2] * upZ;
            if (dot > 0.0f) {
                float reduction = steepness * steepness;
                if (reduction > 1.0f) reduction = 1.0f;
                float removedAmount = dot * reduction;
                outResult[0] -= upX * removedAmount;
                outResult[2] -= upZ * removedAmount;
                float removeY = removedAmount * upLen / ny;
                outResult[1] -= removeY;
            }
        }
    }

    outResult[3] = static_cast<float>(baseWalkedOn);
    outResult[4] = slideFriction;
}

void HandleSneakCore(const CollisionContext& ctx, float boxX, float boxY, float boxZ, float dx, float dz, float maxDrop, float* outResult) {
    auto checkSafe = [&](float testX, float testZ) {
        float sHx = std::max(0.02f, ctx.boxHx * 0.4f);
        float sHz = std::max(0.02f, ctx.boxHz * 0.4f);
        BoxShape boxShape(Vec3(sHx, ctx.boxHy, sHz));

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

        for (int i = 0; i < ctx.capacity; i++) {
            const Shape* shape = reinterpret_cast<const Shape*>(ctx.shapes[i]);
            if (!shape) continue;
            float sx = static_cast<float>(ctx.pX[i]);
            float sy = static_cast<float>(ctx.pY[i]);
            float sz = static_cast<float>(ctx.pZ[i]);
            Quat sq(ctx.rX[i], ctx.rY[i], ctx.rZ[i], ctx.rW[i]);

            Mat44 shapeMat = Mat44::sRotationTranslation(sq, Vec3(sx, sy, sz));
            CollisionDispatch::sCastShapeVsShapeWorldSpace(shapeCast, settings, shape, Vec3::sReplicate(1.0f), ShapeFilter(), shapeMat, SubShapeIDCreator(), SubShapeIDCreator(), collector);
        }

        if (collector.hit) {
            float dropDist = (maxDrop + 0.2f) * collector.result.mFraction;
            float hitY = (boxY + 0.1f) - dropDist;
            float feetY = boxY - ctx.boxHy;
            if (hitY >= feetY - maxDrop - 0.05f) return true;
        }
        return false;
    };

    if (!checkSafe(boxX, boxZ)) {
        outResult[0] = dx;
        outResult[1] = dz;
        return;
    }

    float testX = dx;
    float testZ = dz;

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

    outResult[0] = testX;
    outResult[1] = testZ;
}

bool IsCollidingCore(const CollisionContext& ctx, float boxX, float boxY, float boxZ) {
    float bHy = ctx.boxHy + 0.05f;
    float bY = boxY - 0.05f;

    for (int i = 0; i < ctx.capacity; i++) {
        const Shape* shape = reinterpret_cast<const Shape*>(ctx.shapes[i]);
        if (!shape) continue;

        float sx = static_cast<float>(ctx.pX[i]);
        float sy = static_cast<float>(ctx.pY[i]);
        float sz = static_cast<float>(ctx.pZ[i]);
        Quat sq(ctx.rX[i], ctx.rY[i], ctx.rZ[i], ctx.rW[i]);

        BoxShape boxShape(Vec3(ctx.boxHx, bHy, ctx.boxHz));
        Mat44 shapeMat = Mat44::sRotationTranslation(sq, Vec3(sx, sy, sz));
        Mat44 boxMat = Mat44::sTranslation(Vec3(boxX, bY, boxZ));

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
            return true;
        }
    }

    return false;
}

} // namespace Velthoric
