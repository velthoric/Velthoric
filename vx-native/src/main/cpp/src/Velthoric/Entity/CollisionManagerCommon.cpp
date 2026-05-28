/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 *
 * Author: xI-Mx-Ix
 */
#include "CollisionManagerCommon.h"
#include <vector>

namespace Velthoric {

/**
 * Computes the volume ratio between a body's collision shape and the entity's bounding box.
 * Uses the shape's local AABB (available on both client and server) as a universal,
 * mass-independent measure of relative physical significance.
 *
 * @param shape The collision shape of the body.
 * @param entityHx Half-extent of the entity's bounding box along X.
 * @param entityHy Half-extent of the entity's bounding box along Y.
 * @param entityHz Half-extent of the entity's bounding box along Z.
 * @return A clamped [0, 1] ratio representing the body's relative volume influence.
 */
float ComputeBodyVolumeRatio(const Shape* shape, float entityHx, float entityHy, float entityHz) {
    // Entity volume from its bounding box half-extents (full extents = 2*h)
    float entityVolume = 8.0f * entityHx * entityHy * entityHz;
    if (entityVolume < 1e-8f) return 1.0f; // Degenerate entity → no filtering

    // Retrieve the shape's local-space AABB (works for ANY shape type on both client and server)
    AABox bounds = shape->GetLocalBounds();
    Vec3 extent = bounds.GetSize();
    float bodyVolume = extent.GetX() * extent.GetY() * extent.GetZ();

    // Raw ratio of body volume relative to entity volume
    float rawRatio = bodyVolume / entityVolume;

    return std::min(1.0f, rawRatio);
}

/**
 * Casts the entity bounding box downward to locate the nearest supporting surface.
 * Filters out bodies whose shape volume is too small relative to the entity to serve as valid ground.
 *
 * @param ctx The active collision context holding shapes, transformations, and physical properties.
 * @param startX Starting world coordinate of the cast along the X-axis.
 * @param startY Starting world coordinate of the cast along the Y-axis.
 * @param startZ Starting world coordinate of the cast along the Z-axis.
 * @param distDown The maximum search distance downward.
 * @param outHitY Output variable populated with the resulting contact elevation on a hit.
 * @param outNormal Output variable containing the normal of the surface hit.
 * @param outHitBody Output variable populated with the index of the body hit, or -1 if no hit.
 * @return True if a valid floor is located, false otherwise.
 */
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
        float currentVolumeRatio = 0.0f;
        const CollisionContext* ctxPtr = nullptr;

        virtual void AddHit(const ShapeCastResult& inResult) override {
            // Server-side additional mass check for dynamic bodies
            uint32_t bodyIdVal = (ctxPtr->bIds && currentBody >= 0 && currentBody < ctxPtr->capacity) ? static_cast<uint32_t>(ctxPtr->bIds[currentBody]) : 0;
            if (ctxPtr->ps && bodyIdVal != 0) {
                BodyID id(bodyIdVal);
                BodyLockRead lock(ctxPtr->ps->GetBodyLockInterface(), id);
                if (lock.Succeeded()) {
                    const Body& body = lock.GetBody();
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
        collector.currentVolumeRatio = ComputeBodyVolumeRatio(shape, ctx.boxHx, ctx.boxHy, ctx.boxHz);

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

/**
 * Resolves overlapping spatial penetrations by translating the entity bounding volume out of intersecting surfaces.
 *
 * @param ctx The active collision context holding shapes, transformations, and physical properties.
 * @param px Reference to the world X coordinate of the box, adjusted if pushed.
 * @param py Reference to the world Y coordinate of the box, adjusted if pushed.
 * @param pz Reference to the world Z coordinate of the box, adjusted if pushed.
 * @param outPushX Output variable accumulating total displacement along the X-axis.
 * @param outPushY Output variable accumulating total displacement along the Y-axis.
 * @param outPushZ Output variable accumulating total displacement along the Z-axis.
 * @param outMaxNormal Output variable identifying the normal of the most significant surface hit.
 * @param bodyIndexOut Output variable populated with the index of the supporting body.
 * @param outSlideFriction Output variable containing the active sliding friction modifier.
 * @return True if any penetration is resolved, false otherwise.
 */
bool ResolvePenetrations(const CollisionContext& ctx, float& px, float& py, float& pz, float& outPushX, float& outPushY, float& outPushZ, Vec3& outMaxNormal, int& bodyIndexOut, float& outSlideFriction) {
    outMaxNormal = Vec3(0.0f, -1.0f, 0.0f);
    int iterations = 8;
    bool hitAny = false;
    bodyIndexOut = -1;
    outSlideFriction = 1.0f;

    // Track the largest body that qualifies as ground to prevent flickering between many small bodies
    float bestGroundVolumeRatio = 0.0f;

    // Constant Settings
    const float characterMass = ctx.entityMass > 0.0f ? ctx.entityMass : 80.0f;
    const float maxStrength = 500.0f; // Limit force capability (Newton)
    const float dt = 0.05f;           // Logical step tick rate
    const float maxImpulse = maxStrength * dt;

    // Accumulators structure to collect physics interactions for each body separately
    struct BodyImpulseAccumulator {
        Vec3 impulse = Vec3::sZero();
        RVec3 contactPoint = RVec3::sZero();
        int hitsCount = 0;
        uint32_t bodyId = 0;
    };
    std::vector<BodyImpulseAccumulator> bodyImpulses(ctx.capacity);

    for (int iter = 0; iter < iterations; iter++) {
        bool resolved = false;

        // Vertical resolution pass
        for (int i = 0; i < ctx.capacity; i++) {
            const Shape* shape = reinterpret_cast<const Shape*>(ctx.shapes[i]);
            if (!shape) continue;

            // Compute shape volume ratio for size-proportional entity interaction
            float volumeRatio = ComputeBodyVolumeRatio(shape, ctx.boxHx, ctx.boxHy, ctx.boxHz);

            float sx = static_cast<float>(ctx.pX[i]);
            float sy = static_cast<float>(ctx.pY[i]);
            float sz = static_cast<float>(ctx.pZ[i]);
            Quat sq(ctx.rX[i], ctx.rY[i], ctx.rZ[i], ctx.rW[i]);

            BoxShape boxShape(Vec3(ctx.boxHx, ctx.boxHy, ctx.boxHz));
            Mat44 shapeMat = Mat44::sRotationTranslation(sq, Vec3(sx, sy, sz));
            Mat44 boxMat = Mat44::sTranslation(Vec3(px, py, pz));

            CollideShapeSettings settings;
            settings.mMaxSeparationDistance = 0.005f;

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
                float ny = normal.GetY();

                if (std::abs(ny) >= 0.65f) {
                    // Vertical correction translation
                    Vec3 push = Vec3(0.0f, depth / ny, 0.0f);

                    uint32_t bodyIdVal = (ctx.bIds && i < ctx.capacity) ? static_cast<uint32_t>(ctx.bIds[i]) : 0;
                    bool isDynamicBody = false;
                    float bodyMass = 0.0f;
                    float invMass = 0.0f;
                    RVec3 bodyPos = RVec3::sZero();
                    Vec3 bodyVel(0.0f, 0.0f, 0.0f);
                    bool bodyIsActive = false;

                    // Rotational and positional metrics for advanced impulse calculation
                    RVec3 bodyCoM = RVec3::sZero();
                    Mat44 invInertia = Mat44::sIdentity();

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
                                bodyCoM = body.GetCenterOfMassPosition();
                                invInertia = body.GetInverseInertia();
                                if (body.GetMotionProperties()) {
                                    invMass = body.GetMotionProperties()->GetInverseMass();
                                    if (invMass > 0.0f) {
                                        bodyMass = 1.0f / invMass;
                                    }
                                }
                            }
                        }
                    }

                    // Vertical collisions are always 100% solid to prevent falling through
                    // or getting stuck inside objects when landing/jumping.
                    float entityPushRatio = 1.0f;

                    px += push.GetX() * entityPushRatio;
                    py += push.GetY() * entityPushRatio;
                    pz += push.GetZ() * entityPushRatio;
                    outPushX += push.GetX() * entityPushRatio;
                    outPushY += push.GetY() * entityPushRatio;
                    outPushZ += push.GetZ() * entityPushRatio;

                    // Dynamically apply impulses back to physics simulations
                    if (isDynamicBody && ctx.ps && bodyIdVal != 0) {
                        // 1. Determine contact points
                        RVec3 contactPoint(collector.result.mContactPointOn2);

                        // r is the lever arm vector from the Body's Center of Mass to the Contact Point
                        Vec3 r = Vec3(contactPoint - bodyCoM);

                        // 2. Compute angular mass factor: ((I^-1 * (r x n)) x r) . n
                        Vec3 rCrossN = r.Cross(normal);
                        Vec3 invInertia_rCrossN = invInertia * rCrossN;
                        float angularFactor = invInertia_rCrossN.Cross(r).Dot(normal);

                        // Combined effective inverse mass at the specific contact point
                        float K = (1.0f / characterMass) + invMass + angularFactor;
                        if (K < 1e-6f) K = 1e-6f;

                        // 3. Approach speed calculation for impact/landing
                        Vec3 entityVel(ctx.entityVelocityX, ctx.entityVelocityY, ctx.entityVelocityZ);
                        Vec3 relVel = entityVel - bodyVel;
                        float approachSpeed = -relVel.Dot(normal);

                        float e = 0.25f; // Restitution
                        float J_vel = 0.0f;
                        if (approachSpeed > 0.05f) {
                            // Impact impulse using localized effective mass K
                            J_vel = (approachSpeed * (1.0f + e)) / K;

                            // Cap velocity impulse based on maximum character strength
                            J_vel = std::min(J_vel, maxImpulse);
                        }

                        // 4. Penetration impulse
                        float J_pen = depth / (dt * K);

                        // Cap penetration impulse based on maximum character strength
                        float appliedJ_pen = std::min(J_pen * 0.2f, maxImpulse);

                        // Accumulate impulse forces along normal
                        float J_total = J_vel + appliedJ_pen;
                        Vec3 impulse = -normal * J_total;

                        // 5. Continuous weight force (gravity) applied straight down at the contact point
                        if (normal.GetY() >= 0.65f) {
                            Vec3 weightImpulse(0.0f, -characterMass * 9.81f * dt, 0.0f);
                            impulse += weightImpulse;
                        }

                        bodyImpulses[i].impulse += impulse;
                        bodyImpulses[i].contactPoint += contactPoint;
                        bodyImpulses[i].hitsCount++;
                        bodyImpulses[i].bodyId = bodyIdVal;
                    }

                    if (normal.GetY() > outMaxNormal.GetY()) {
                        outMaxNormal = normal;
                        if (normal.GetY() >= 0.1f) {
                            // Replicates geometric friction independently of volume ratio
                            outSlideFriction = 1.0f;

                            // Ground body selection: only bodies >= 30% true volume can drag the player
                            if (volumeRatio >= 0.30f && volumeRatio >= bestGroundVolumeRatio) {
                                bestGroundVolumeRatio = volumeRatio;
                                bodyIndexOut = i;
                            }
                        }
                    }
                    resolved = true;
                }
            }
        }

        // Horizontal resolution pass
        for (int i = 0; i < ctx.capacity; i++) {
            const Shape* shape = reinterpret_cast<const Shape*>(ctx.shapes[i]);
            if (!shape) continue;

            // Compute shape volume ratio for size-proportional entity interaction
            float volumeRatio = ComputeBodyVolumeRatio(shape, ctx.boxHx, ctx.boxHy, ctx.boxHz);

            float sx = static_cast<float>(ctx.pX[i]);
            float sy = static_cast<float>(ctx.pY[i]);
            float sz = static_cast<float>(ctx.pZ[i]);
            Quat sq(ctx.rX[i], ctx.rY[i], ctx.rZ[i], ctx.rW[i]);

            BoxShape boxShape(Vec3(ctx.boxHx, ctx.boxHy, ctx.boxHz));
            Mat44 shapeMat = Mat44::sRotationTranslation(sq, Vec3(sx, sy, sz));
            Mat44 boxMat = Mat44::sTranslation(Vec3(px, py, pz));

            CollideShapeSettings settings;
            settings.mMaxSeparationDistance = 0.005f;

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
                float ny = normal.GetY();
                float nHorizSq = normal.GetX() * normal.GetX() + normal.GetZ() * normal.GetZ();

                if (std::abs(ny) < 0.65f && nHorizSq > 0.0001f) {
                    // Horizontal correction translations
                    float k = depth / nHorizSq;
                    if (k > 2.0f / (std::sqrt(nHorizSq) + 0.001f)) k = 2.0f / (std::sqrt(nHorizSq) + 0.001f);
                    Vec3 push = Vec3(normal.GetX() * k, 0.0f, normal.GetZ() * k);

                    uint32_t bodyIdVal = (ctx.bIds && i < ctx.capacity) ? static_cast<uint32_t>(ctx.bIds[i]) : 0;
                    bool isDynamicBody = false;
                    float bodyMass = 0.0f;
                    float invMass = 0.0f;
                    RVec3 bodyPos = RVec3::sZero();
                    Vec3 bodyVel(0.0f, 0.0f, 0.0f);
                    bool bodyIsActive = false;

                    // Rotational and positional metrics for advanced impulse calculation
                    RVec3 bodyCoM = RVec3::sZero();
                    Mat44 invInertia = Mat44::sIdentity();

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
                                bodyCoM = body.GetCenterOfMassPosition();
                                invInertia = body.GetInverseInertia();
                                if (body.GetMotionProperties()) {
                                    invMass = body.GetMotionProperties()->GetInverseMass();
                                    if (invMass > 0.0f) {
                                        bodyMass = 1.0f / invMass;
                                    }
                                }
                            }
                        }
                    }

                    // Replicates Jolt's true mass ratio pushback without artificial limits
                    float entityPushRatio = 1.0f;
                    if (isDynamicBody) {
                        float totalMass = characterMass + bodyMass;
                        entityPushRatio = (totalMass > 0.0f) ? (bodyMass / totalMass) : 0.5f;
                    }

                    px += push.GetX() * entityPushRatio;
                    py += push.GetY() * entityPushRatio;
                    pz += push.GetZ() * entityPushRatio;
                    outPushX += push.GetX() * entityPushRatio;
                    outPushY += push.GetY() * entityPushRatio;
                    outPushZ += push.GetZ() * entityPushRatio;

                    // Dynamically apply impulses back to physics simulations
                    if (isDynamicBody && ctx.ps && bodyIdVal != 0) {
                        // 1. Determine contact points
                        RVec3 contactPoint(collector.result.mContactPointOn2);

                        // r is the lever arm vector from the Body's Center of Mass to the Contact Point
                        Vec3 r = Vec3(contactPoint - bodyCoM);

                        // 2. Compute angular mass factor: ((I^-1 * (r x n)) x r) . n
                        Vec3 rCrossN = r.Cross(normal);
                        Vec3 invInertia_rCrossN = invInertia * rCrossN;
                        float angularFactor = invInertia_rCrossN.Cross(r).Dot(normal);

                        // Combined effective inverse mass at the specific contact point
                        float K = (1.0f / characterMass) + invMass + angularFactor;
                        if (K < 1e-6f) K = 1e-6f;

                        // 3. Approach speed calculation for impact/landing
                        Vec3 entityVel(ctx.entityVelocityX, ctx.entityVelocityY, ctx.entityVelocityZ);
                        Vec3 relVel = entityVel - bodyVel;
                        float approachSpeed = -relVel.Dot(normal);

                        float e = 0.25f; // Restitution
                        float J_vel = 0.0f;
                        if (approachSpeed > 0.05f) {
                            // Impact impulse using localized effective mass K
                            J_vel = (approachSpeed * (1.0f + e)) / K;

                            // Cap velocity impulse based on maximum character strength
                            J_vel = std::min(J_vel, maxImpulse);
                        }

                        float J_push = 0.0f;
                        if (std::abs(ny) < 0.1f && approachSpeed <= 0.05f) {
                            float activeAcceleration = 8.0f;
                            J_push = (activeAcceleration * dt) / K;
                        }

                        // 4. Penetration impulse
                        float J_pen = depth / (dt * K);

                        // Cap penetration impulse based on maximum character strength
                        float appliedJ_pen = std::min(J_pen * 0.2f, maxImpulse);

                        // Accumulate impulse forces along normal
                        float J_total = J_vel + J_push + appliedJ_pen;
                        Vec3 impulse = -normal * J_total;

                        bodyImpulses[i].impulse += impulse;
                        bodyImpulses[i].contactPoint += contactPoint;
                        bodyImpulses[i].hitsCount++;
                        bodyImpulses[i].bodyId = bodyIdVal;
                    }

                    if (normal.GetY() > outMaxNormal.GetY()) {
                        outMaxNormal = normal;
                    }
                    resolved = true;
                }
            }
        }

        if (!resolved) break;
        hitAny = true;
    }

    // Clamp the total accumulated horizontal push magnitude.
    // When many small bodies overlap, their individual pushes compound into large displacements.
    // This cap ensures the entity remains stable even in extreme pile scenarios.
    float horizPushSq = outPushX * outPushX + outPushZ * outPushZ;
    float maxHorizPush = 0.5f; // Maximum horizontal push per tick (blocks)
    if (horizPushSq > maxHorizPush * maxHorizPush) {
        float horizPushLen = std::sqrt(horizPushSq);
        float scale = maxHorizPush / horizPushLen;
        float clampedPushX = outPushX * scale;
        float clampedPushZ = outPushZ * scale;
        // Adjust the position to match the clamped push
        px -= (outPushX - clampedPushX);
        pz -= (outPushZ - clampedPushZ);
        outPushX = clampedPushX;
        outPushZ = clampedPushZ;
    }

    // Deliver accumulated structural reactions to the physics simulator for each body individually
    for (int i = 0; i < ctx.capacity; i++) {
        const auto& bImp = bodyImpulses[i];
        if (bImp.hitsCount > 0 && bImp.bodyId != 0 && ctx.ps) {
            BodyID id(bImp.bodyId);
            RVec3 averageContactPoint = bImp.contactPoint / static_cast<float>(bImp.hitsCount);

            // Clamp total impulse to avoid game physics glitches
            float maxImpulseClamp = 400.0f;
            Vec3 finalImpulse = bImp.impulse;
            float impulseMagnitude = finalImpulse.Length();
            if (impulseMagnitude > maxImpulseClamp && impulseMagnitude > 0.0f) {
                finalImpulse = finalImpulse * (maxImpulseClamp / impulseMagnitude);
            }

            // Apply impulse at the exact contact point to generate translation and rotation
            ctx.ps->GetBodyInterface().AddImpulse(id, finalImpulse, averageContactPoint);
            ctx.ps->GetBodyInterface().ActivateBody(id);
        }
    }

    return hitAny;
}

/**
 * Resolves environmental collisions, including climbing steps and slope constraints.
 *
 * @param ctx The active collision context holding shapes, transformations, and physical properties.
 * @param boxX Current spatial position of the entity box center on the X-axis.
 * @param boxY Current spatial position of the entity box center on the Y-axis.
 * @param boxZ Current spatial position of the entity box center on the Z-axis.
 * @param dx Raw translation intention along the X-axis.
 * @param dy Raw translation intention along the Y-axis.
 * @param dz Raw translation intention along the Z-axis.
 * @param stepHeight Maximum vertical threshold permitted for climbing obstacles.
 * @param outResult Float array containing final resolved positions, platform reference, and friction.
 */
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

/**
 * Bounds checking for sneaking entities to prevent them from slipping off edges.
 *
 * @param ctx The active collision context holding shapes, transformations, and physical properties.
 * @param boxX Central bounding volume translation coordinates on the X-axis.
 * @param boxY Central bounding volume translation coordinates on the Y-axis.
 * @param boxZ Central bounding volume translation coordinates on the Z-axis.
 * @param dx Proposed horizontal step displacement along the X-axis.
 * @param dz Proposed horizontal step displacement along the Z-axis.
 * @param maxDrop Permitted ledge drop vertical threshold.
 * @param outResult Float array containing final bounded translation vectors.
 */
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

/**
 * Checks if the specified bounding volume overlaps with any populated shapes.
 *
 * @param ctx The active collision context holding shapes, transformations, and physical properties.
 * @param boxX Central bounding volume translation coordinates on the X-axis.
 * @param boxY Central bounding volume translation coordinates on the Y-axis.
 * @param boxZ Central bounding volume translation coordinates on the Z-axis.
 * @return True if a spatial intersection is detected, false otherwise.
 */
bool IsCollidingCore(const CollisionContext& ctx, float boxX, float boxY, float boxZ) {
    return GetCollidingBodyIdCore(ctx, boxX, boxY, boxZ) != -1;
}

/**
 * Identifies the exact body index the bounding volume intersects with.
 *
 * @param ctx The active collision context holding shapes, transformations, and physical properties.
 * @param boxX Central bounding volume translation coordinates on the X-axis.
 * @param boxY Central bounding volume translation coordinates on the Y-axis.
 * @param boxZ Central bounding volume translation coordinates on the Z-axis.
 * @return The 0-based index of the intersecting body, or -1 if no intersection is detected.
 */
int GetCollidingBodyIdCore(const CollisionContext& ctx, float boxX, float boxY, float boxZ) {
    const float bHy = ctx.boxHy + 0.05f;

    BoxShape boxShape(Vec3(ctx.boxHx, bHy, ctx.boxHz));
    Mat44 boxMat = Mat44::sTranslation(Vec3(boxX, boxY - 0.05f, boxZ));

    CollideShapeSettings settings;
    settings.mMaxSeparationDistance = 0.0f;

    for (int i = 0; i < ctx.capacity; ++i) {
        const Shape* shape = reinterpret_cast<const Shape*>(ctx.shapes[i]);
        if (!shape) continue;

        struct Collector : public CollideShapeCollector {
            bool hit = false;

            void AddHit(const CollideShapeResult& result) override {
                hit |= result.mPenetrationDepth > 0.0001f;
            }
        } collector;

        float tsx = static_cast<float>(ctx.pX[i]);
        float tsy = static_cast<float>(ctx.pY[i]);
        float tsz = static_cast<float>(ctx.pZ[i]);
        Quat sq(ctx.rX[i], ctx.rY[i], ctx.rZ[i], ctx.rW[i]);

        CollisionDispatch::sCollideShapeVsShape(
            &boxShape,
            shape,
            Vec3::sReplicate(1.0f),
            Vec3::sReplicate(1.0f),
            boxMat,
            Mat44::sRotationTranslation(sq, Vec3(tsx, tsy, tsz)),
            SubShapeIDCreator(),
            SubShapeIDCreator(),
            settings,
            collector
        );

        if (collector.hit) {
            return i;
        }
    }

    return -1;
}

} // namespace Velthoric
