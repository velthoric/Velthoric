/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 *
 * Author: xI-Mx-Ix
 */
#include "net_xmx_velthoric_jni_BatchPhysicsSync.h"
#include <Jolt/Jolt.h>
#include <Jolt/Physics/PhysicsSystem.h>
#include <Jolt/Physics/Body/BodyInterface.h>
#include <Jolt/Physics/Body/BodyLockInterface.h>
#include <Jolt/Physics/Body/Body.h>
#include <Jolt/Physics/SoftBody/SoftBodyMotionProperties.h>
#include <cmath>
#include <algorithm>

using namespace JPH;

namespace Velthoric {

/**
 * Synchronizes the native Jolt simulation results with the Java-side SoA data store.
 * 
 * This function performs a high-performance synchronization of:
 * - World-space positions (double[3])
 * - Rotations (float[4] quaternions)
 * - Linear and angular velocities (float[3])
 * - World-space Axis-Aligned Bounding Boxes (AABB)
 * - Activity states and last update timestamps
 * - Native motion types (Static, Kinematic, Dynamic)
 * - Soft body vertex locations (float[] per body)
 *
 * It utilizes direct Java NIO Buffer addresses via GetDirectBufferAddress to completely
 * avoid JNI Critical pinning overhead for the primary 20+ physics buffers.
 *
 * @param env Pointer to the JNI environment.
 * @param thiz Reference to the Java BatchPhysicsSync instance.
 * @param physicsSystemPtr Native pointer (long) to the Jolt PhysicsSystem.
 * @param count Number of bodies in the current batch.
 * @param indicesArr Java int[] containing the target indices in the SoA store.
 * @param bodyIdsArr Java int[] containing the Jolt BodyIDs.
 * @param behaviorBitsArr Java long[] containing the behavior bitmasks for each body.
 * @param posXBuf Java DoubleBuffer for X-positions.
 * @param posYBuf Java DoubleBuffer for Y-positions.
 * @param posZBuf Java DoubleBuffer for Z-positions.
 * @param rotXBuf Java FloatBuffer for rotation X.
 * @param rotYBuf Java FloatBuffer for rotation Y.
 * @param rotZBuf Java FloatBuffer for rotation Z.
 * @param rotWBuf Java FloatBuffer for rotation W.
 * @param velXBuf Java FloatBuffer for linear velocity X.
 * @param velYBuf Java FloatBuffer for linear velocity Y.
 * @param velZBuf Java FloatBuffer for linear velocity Z.
 * @param angVelXBuf Java FloatBuffer for angular velocity X.
 * @param angVelYBuf Java FloatBuffer for angular velocity Y.
 * @param angVelZBuf Java FloatBuffer for angular velocity Z.
 * @param aabbMinXBuf Java FloatBuffer for AABB minimum X.
 * @param aabbMinYBuf Java FloatBuffer for AABB minimum Y.
 * @param aabbMinZBuf Java FloatBuffer for AABB minimum Z.
 * @param aabbMaxXBuf Java FloatBuffer for AABB maximum X.
 * @param aabbMaxYBuf Java FloatBuffer for AABB maximum Y.
 * @param aabbMaxZBuf Java FloatBuffer for AABB maximum Z.
 * @param isActiveBuf Java ByteBuffer for activity state.
 * @param isTransformDirtyBuf Java ByteBuffer to mark transform updates for networking.
 * @param isVertexDataDirtyBuf Java ByteBuffer to mark vertex updates for networking.
 * @param lastUpdateTimestampBuf Java LongBuffer for simulation timestamps.
 * @param motionTypeOutputArr Java byte[] to store native EMotionType ordinals.
 * @param dirtyIndicesOutputArr Java int[] to collect indices that need network broadcasting.
 * @param vertexDataArr Java float[][] containing vertex data for soft bodies.
 * @param softBodyBehaviorMask Bitmask identifying the SoftPhysics behavior.
 * @param timestampNanos The current simulation timestamp in nanoseconds.
 * @return The number of dirty indices written to dirtyIndicesOutputArr.
 */
extern "C" JNIEXPORT jint JNICALL Java_net_xmx_velthoric_jni_BatchPhysicsSync_syncPhysicsNative(
    JNIEnv* env, jclass clazz,
    jlong physicsSystemPtr,
    jint count,
    jintArray indicesArr,
    jintArray bodyIdsArr,
    jlongArray behaviorBitsArr,
    jobject posXBuf, jobject posYBuf, jobject posZBuf,
    jobject rotXBuf, jobject rotYBuf, jobject rotZBuf, jobject rotWBuf,
    jobject velXBuf, jobject velYBuf, jobject velZBuf,
    jobject angVelXBuf, jobject angVelYBuf, jobject angVelZBuf,
    jobject aabbMinXBuf, jobject aabbMinYBuf, jobject aabbMinZBuf,
    jobject aabbMaxXBuf, jobject aabbMaxYBuf, jobject aabbMaxZBuf,
    jobject isActiveBuf,
    jobject isTransformDirtyBuf,
    jobject isVertexDataDirtyBuf,
    jobject lastUpdateTimestampBuf,
    jbyteArray motionTypeOutputArr,
    jintArray dirtyIndicesOutputArr,
    jobjectArray vertexDataArr,
    jlong softBodyBehaviorMask,
    jlong timestampNanos
) {
    (void)clazz;
    if (physicsSystemPtr == 0) return 0;

    PhysicsSystem* physicsSystem = reinterpret_cast<PhysicsSystem*>(physicsSystemPtr);
    const BodyInterface& bodyInterface = physicsSystem->GetBodyInterfaceNoLock();
    const BodyLockInterface& lockInterface = physicsSystem->GetBodyLockInterfaceNoLock();

    // Batch tracking for soft bodies to handle them in a separate pass.
    int softBodyBatchIndices[512]; 
    int softBodyCount = 0;
    
    // Retrieve direct native addresses for direct buffers
    jdouble* posX = (jdouble*)env->GetDirectBufferAddress(posXBuf);
    jdouble* posY = (jdouble*)env->GetDirectBufferAddress(posYBuf);
    jdouble* posZ = (jdouble*)env->GetDirectBufferAddress(posZBuf);
    
    jfloat* rotX = (jfloat*)env->GetDirectBufferAddress(rotXBuf);
    jfloat* rotY = (jfloat*)env->GetDirectBufferAddress(rotYBuf);
    jfloat* rotZ = (jfloat*)env->GetDirectBufferAddress(rotZBuf);
    jfloat* rotW = (jfloat*)env->GetDirectBufferAddress(rotWBuf);
    
    jfloat* velX = (jfloat*)env->GetDirectBufferAddress(velXBuf);
    jfloat* velY = (jfloat*)env->GetDirectBufferAddress(velYBuf);
    jfloat* velZ = (jfloat*)env->GetDirectBufferAddress(velZBuf);
    
    jfloat* angVelX = (jfloat*)env->GetDirectBufferAddress(angVelXBuf);
    jfloat* angVelY = (jfloat*)env->GetDirectBufferAddress(angVelYBuf);
    jfloat* angVelZ = (jfloat*)env->GetDirectBufferAddress(angVelZBuf);
    
    jfloat* aabbMinX = (jfloat*)env->GetDirectBufferAddress(aabbMinXBuf);
    jfloat* aabbMinY = (jfloat*)env->GetDirectBufferAddress(aabbMinYBuf);
    jfloat* aabbMinZ = (jfloat*)env->GetDirectBufferAddress(aabbMinZBuf);
    
    jfloat* aabbMaxX = (jfloat*)env->GetDirectBufferAddress(aabbMaxXBuf);
    jfloat* aabbMaxY = (jfloat*)env->GetDirectBufferAddress(aabbMaxYBuf);
    jfloat* aabbMaxZ = (jfloat*)env->GetDirectBufferAddress(aabbMaxZBuf);
    
    jboolean* isActive = (jboolean*)env->GetDirectBufferAddress(isActiveBuf);
    jboolean* isTransformDirty = (jboolean*)env->GetDirectBufferAddress(isTransformDirtyBuf);
    jboolean* isVertexDataDirty = (jboolean*)env->GetDirectBufferAddress(isVertexDataDirtyBuf);
    jlong* lastUpdateTimestamp = (jlong*)env->GetDirectBufferAddress(lastUpdateTimestampBuf);

    // Pass 1: Fast synchronization using direct buffer addresses and temporary primitive arrays.
    {
        jint* indices = (jint*)env->GetPrimitiveArrayCritical(indicesArr, nullptr);
        jint* bodyIds = (jint*)env->GetPrimitiveArrayCritical(bodyIdsArr, nullptr);
        jlong* behaviorBits = (jlong*)env->GetPrimitiveArrayCritical(behaviorBitsArr, nullptr);
        jbyte* motionTypes = (jbyte*)env->GetPrimitiveArrayCritical(motionTypeOutputArr, nullptr);

        for (int b = 0; b < count; ++b) {
            BodyID id(bodyIds[b]);
            if (!bodyInterface.IsAdded(id)) continue;

            int i = indices[b];
            
            bool isJoltBodyActive = bodyInterface.IsActive(id);
            bool wasDataStoreBodyActive = (isActive[i] != 0);

            if (isJoltBodyActive || wasDataStoreBodyActive) {
                RVec3 bodyPos = bodyInterface.GetPosition(id);
                Quat rot = bodyInterface.GetRotation(id);
                Vec3 linVel = bodyInterface.GetLinearVelocity(id);
                Vec3 angVel = bodyInterface.GetAngularVelocity(id);

                if (isJoltBodyActive || isJoltBodyActive != wasDataStoreBodyActive) {
                    isTransformDirty[i] = 1;
                }

                posX[i] = bodyPos.GetX();
                posY[i] = bodyPos.GetY();
                posZ[i] = bodyPos.GetZ();

                rotX[i] = rot.GetX();
                rotY[i] = rot.GetY();
                rotZ[i] = rot.GetZ();
                rotW[i] = rot.GetW();

                velX[i] = linVel.GetX();
                velY[i] = linVel.GetY();
                velZ[i] = linVel.GetZ();

                angVelX[i] = angVel.GetX();
                angVelY[i] = angVel.GetY();
                angVelZ[i] = angVel.GetZ();

                isActive[i] = isJoltBodyActive ? 1 : 0;
                lastUpdateTimestamp[i] = timestampNanos;
                motionTypes[b] = (jbyte)bodyInterface.GetMotionType(id);

                {
                    BodyLockRead lock(lockInterface, id);
                    if (lock.Succeeded()) {
                        const Body& body = lock.GetBody();
                        AABox bounds = body.GetWorldSpaceBounds();
                        Vec3 min = bounds.mMin;
                        Vec3 max = bounds.mMax;

                        aabbMinX[i] = (float)min.GetX();
                        aabbMinY[i] = (float)min.GetY();
                        aabbMinZ[i] = (float)min.GetZ();
                        aabbMaxX[i] = (float)max.GetX();
                        aabbMaxY[i] = (float)max.GetY();
                        aabbMaxZ[i] = (float)max.GetZ();

                        // Flag soft bodies for separate vertex pass.
                        if (isJoltBodyActive && (behaviorBits[b] & softBodyBehaviorMask) != 0 && body.GetBodyType() == EBodyType::SoftBody) {
                            softBodyBatchIndices[softBodyCount++] = b;
                        }
                    }
                }
            }
        }

        env->ReleasePrimitiveArrayCritical(motionTypeOutputArr, motionTypes, 0);
        env->ReleasePrimitiveArrayCritical(behaviorBitsArr, behaviorBits, 0);
        env->ReleasePrimitiveArrayCritical(bodyIdsArr, bodyIds, 0);
        env->ReleasePrimitiveArrayCritical(indicesArr, indices, 0);
    }

    int vertexDirtyIndices[512];
    int vertexDirtyCount = 0;

    // Pass 2: Soft Body Vertex Extraction.
    // Uses standard JNI calls to safely access the Java object array (float[][]).
    if (softBodyCount > 0) {
        for (int s = 0; s < softBodyCount; ++s) {
            int b = softBodyBatchIndices[s];
            
            jint i;
            env->GetIntArrayRegion(indicesArr, b, 1, &i);
            jint bodyIdInt;
            env->GetIntArrayRegion(bodyIdsArr, b, 1, &bodyIdInt);
            
            BodyID id(bodyIdInt);
            BodyLockRead lock(lockInterface, id);
            if (lock.Succeeded()) {
                const Body& body = lock.GetBody();
                const SoftBodyMotionProperties* mp = static_cast<const SoftBodyMotionProperties*>(body.GetMotionProperties());
                const Array<SoftBodyVertex>& vertices = mp->GetVertices();
                int numVertices = (int)vertices.size();
                int requiredFloats = numVertices * 3;

                jfloatArray innerArr = (jfloatArray)env->GetObjectArrayElement(vertexDataArr, i);
                bool newlyAllocated = false;
                if (innerArr == nullptr || env->GetArrayLength(innerArr) != requiredFloats) {
                    innerArr = env->NewFloatArray(requiredFloats);
                    env->SetObjectArrayElement(vertexDataArr, i, innerArr);
                    newlyAllocated = true;
                }

                jfloat* javaVertices = env->GetFloatArrayElements(innerArr, nullptr);
                RVec3 bodyPos = body.GetPosition();
                
                bool changed = newlyAllocated;
                for (int v = 0; v < numVertices; ++v) {
                    Vec3 worldPos = Vec3(bodyPos + vertices[v].mPosition);
                    float nx = worldPos.GetX();
                    float ny = worldPos.GetY();
                    float nz = worldPos.GetZ();
                    
                    if (!newlyAllocated && !changed) {
                        if (std::abs(javaVertices[v * 3] - nx) > 1e-4f ||
                            std::abs(javaVertices[v * 3 + 1] - ny) > 1e-4f ||
                            std::abs(javaVertices[v * 3 + 2] - nz) > 1e-4f) {
                            changed = true;
                        }
                    }
                    
                    javaVertices[v * 3] = nx;
                    javaVertices[v * 3 + 1] = ny;
                    javaVertices[v * 3 + 2] = nz;
                }

                env->ReleaseFloatArrayElements(innerArr, javaVertices, 0);
                
                if (changed) {
                    isVertexDataDirty[i] = 1;
                    vertexDirtyIndices[vertexDirtyCount++] = i;
                }
                
                env->DeleteLocalRef(innerArr);
            }
        }
    }

    // Pass 3: Finalize dirty indices list for network broadcasting.
    int totalDirtyCount = 0;
    {
        jint* dirtyIndices = (jint*)env->GetPrimitiveArrayCritical(dirtyIndicesOutputArr, nullptr);
        
        // Collect indices marked in Pass 1.
        for (int b = 0; b < count; ++b) {
            jint i;
            env->GetIntArrayRegion(indicesArr, b, 1, &i);
            if (isTransformDirty[i]) {
                dirtyIndices[totalDirtyCount++] = i;
            }
        }
        
        // Append unique indices marked in Pass 2.
        for (int v = 0; v < vertexDirtyCount; ++v) {
            int i = vertexDirtyIndices[v];
            if (!isTransformDirty[i]) {
                dirtyIndices[totalDirtyCount++] = i;
            }
        }

        env->ReleasePrimitiveArrayCritical(dirtyIndicesOutputArr, dirtyIndices, 0);
    }

    return totalDirtyCount;
}

}