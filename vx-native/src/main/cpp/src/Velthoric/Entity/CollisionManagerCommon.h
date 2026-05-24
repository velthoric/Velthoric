/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 *
 * Author: xI-Mx-Ix
 */
#pragma once
#include <jni.h>
#include <Jolt/Jolt.h>
#include <Jolt/Physics/PhysicsSystem.h>
#include <Jolt/Physics/Body/BodyInterface.h>
#include <Jolt/Physics/Body/Body.h>
#include <Jolt/Physics/Body/BodyLockInterface.h>
#include <Jolt/Physics/Collision/Shape/Shape.h>
#include <Jolt/Physics/Collision/Shape/BoxShape.h>
#include <Jolt/Physics/Collision/CollisionDispatch.h>
#include <Jolt/Physics/Collision/CollideShape.h>
#include <Jolt/Physics/Collision/CastResult.h>
#include <Jolt/Physics/Collision/ShapeCast.h>
#include <algorithm>
#include <cmath>

using namespace JPH;

namespace Velthoric {

/**
 * Shared context containing all buffer pointers and physics parameters
 * required for entity-to-body collision detection and resolution.
 */
struct CollisionContext {
    /** Pointer to the Jolt physics system. Null on the client. */
    JPH::PhysicsSystem* ps;
    /** Array of 64-bit native memory addresses pointing to collision shapes. */
    const jlong* shapes;
    /** Array of X coordinates for physics bodies. */
    const jdouble* pX;
    /** Array of Y coordinates for physics bodies. */
    const jdouble* pY;
    /** Array of Z coordinates for physics bodies. */
    const jdouble* pZ;
    /** Array of X components of the bodies' quaternions. */
    const jfloat* rX;
    /** Array of Y components of the bodies' quaternions. */
    const jfloat* rY;
    /** Array of Z components of the bodies' quaternions. */
    const jfloat* rZ;
    /** Array of W components of the bodies' quaternions. */
    const jfloat* rW;
    /** Array of native 32-bit integer body identifiers. */
    const jint* bIds;
    /** Maximum capacity / slot count of the physics world. */
    int capacity;
    /** Half-extent of the entity's bounding box along the X axis. */
    float boxHx;
    /** Half-extent of the entity's bounding box along the Y axis. */
    float boxHy;
    /** Half-extent of the entity's bounding box along the Z axis. */
    float boxHz;
    /** Mass of the entity. */
    float entityMass;
    /** Current velocity of the entity along the X axis. */
    float entityVelocityX;
    /** Current velocity of the entity along the Y axis. */
    float entityVelocityY;
    /** Current velocity of the entity along the Z axis. */
    float entityVelocityZ;
    /** Index of the body the entity stood on in the previous tick, or -1. */
    int lastGroundIdx;
    /** True if the entity has a valid ground body. */
    bool hasGround;
    /** Linear displacement of the ground platform during this tick. */
    Vec3 groundDisplacement;
    /** Rotational displacement of the ground platform during this tick. */
    Quat groundRotDisp;
};

/**
 * Casts a shape downwards to find the floor.
 *
 * @param ctx The shared collision context.
 * @param startX Starting X position.
 * @param startY Starting Y position.
 * @param startZ Starting Z position.
 * @param distDown The maximum distance to cast downwards.
 * @param outHitY The resulting Y position if a hit occurs.
 * @param outNormal The normal vector of the surface hit.
 * @param outHitBody The index of the body hit.
 * @return True if a valid floor was found, false otherwise.
 */
bool CastShapeDown(const CollisionContext& ctx, float startX, float startY, float startZ, float distDown, float& outHitY, Vec3& outNormal, int& outHitBody);

/**
 * Resolves penetrations by moving the box out of intersecting geometry.
 *
 * @param ctx The shared collision context.
 * @param px The X position of the box, modified if pushed.
 * @param py The Y position of the box, modified if pushed.
 * @param pz The Z position of the box, modified if pushed.
 * @param outPushX Accumulated push in X axis.
 * @param outPushY Accumulated push in Y axis.
 * @param outPushZ Accumulated push in Z axis.
 * @param outMaxNormal The normal vector of the steepest surface hit.
 * @param bodyIndexOut The index of the body walked on.
 * @param outSlideFriction The slide friction of the surface.
 * @return True if any penetration was resolved.
 */
bool ResolvePenetrations(const CollisionContext& ctx, float& px, float& py, float& pz, float& outPushX, float& outPushY, float& outPushZ, Vec3& outMaxNormal, int& bodyIndexOut, float& outSlideFriction);

/**
 * Handles core collision resolution for an entity.
 *
 * @param ctx The shared collision context.
 * @param boxX Entity Center X.
 * @param boxY Entity Center Y.
 * @param boxZ Entity Center Z.
 * @param dx Entity move intention X.
 * @param dy Entity move intention Y.
 * @param dz Entity move intention Z.
 * @param stepHeight Valid step elevation magnitude.
 * @param outResult Target array to write results back.
 */
void HandleCollisionCore(const CollisionContext& ctx, float boxX, float boxY, float boxZ, float dx, float dy, float dz, float stepHeight, float* outResult);

/**
 * Ensures a sneaking entity does not fall off ledges.
 *
 * @param ctx The shared collision context.
 * @param boxX Entity Center X.
 * @param boxY Entity Center Y.
 * @param boxZ Entity Center Z.
 * @param dx Proposed X move vector.
 * @param dz Proposed Z move vector.
 * @param maxDrop Permitted ledge drop margin.
 * @param outResult Target array to write results back.
 */
void HandleSneakCore(const CollisionContext& ctx, float boxX, float boxY, float boxZ, float dx, float dz, float maxDrop, float* outResult);

/**
 * Performs a fast bounding box intersection check.
 *
 * @param ctx The shared collision context.
 * @param boxX Entity Center X.
 * @param boxY Entity Center Y.
 * @param boxZ Entity Center Z.
 * @return True if colliding.
 */
bool IsCollidingCore(const CollisionContext& ctx, float boxX, float boxY, float boxZ);

/**
 * Performs a fast bounding box intersection check and returns the specific colliding body ID.
 *
 * @param ctx The shared collision context.
 * @param boxX Entity Center X.
 * @param boxY Entity Center Y.
 * @param boxZ Entity Center Z.
 * @return The 0-based index of the colliding body, or -1 if no collision.
 */
int GetCollidingBodyIdCore(const CollisionContext& ctx, float boxX, float boxY, float boxZ);

} // namespace Velthoric
