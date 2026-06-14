/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 *
 * Author: xI-Mx-Ix
 */
#include <jni.h>
#include <Jolt/Jolt.h>
#include <Jolt/Physics/Collision/Shape/Shape.h>
#include <vector>

extern "C" {

/**
 * @brief JNI Bridge: Extracts all local-space triangle vertices from a Jolt Shape.
 *
 * @param env JNI Environment pointer.
 * @param clazz Calling Java class object.
 * @param shapeVa The native memory address (pointer) of the JPH::Shape.
 * @return A float array containing flat vertex components (XYZ XYZ XYZ) for each triangle.
 */
JNIEXPORT jfloatArray JNICALL
Java_net_xmx_velthoric_jni_ShapeBridge_nGetShapeTriangles(JNIEnv* env, jclass clazz, jlong shapeVa) {
    (void)clazz;
    auto* shape = reinterpret_cast<const JPH::Shape*>(shapeVa);
    if (!shape) return nullptr;

    std::vector<float> triangles;

    JPH::Shape::GetTrianglesContext context;
    // Query the entire local bounds of the shape to ensure all triangles are returned
    shape->GetTrianglesStart(
        context,
        JPH::AABox(JPH::Vec3::sReplicate(-1e10f), JPH::Vec3::sReplicate(1e10f)),
        JPH::Vec3::sZero(),
        JPH::Quat::sIdentity(),
        JPH::Vec3::sReplicate(1.0f)
    );

    const int maxTriangles = 64;
    JPH::Float3 buffer[maxTriangles * 3];

    while (true) {
        int count = shape->GetTrianglesNext(context, maxTriangles, buffer, nullptr);
        if (count <= 0) break;

        for (int i = 0; i < count; ++i) {
            for (int j = 0; j < 3; ++j) {
                triangles.push_back(buffer[i * 3 + j].x);
                triangles.push_back(buffer[i * 3 + j].y);
                triangles.push_back(buffer[i * 3 + j].z);
            }
        }
    }

    if (triangles.empty()) return nullptr;

    jfloatArray result = env->NewFloatArray(static_cast<jsize>(triangles.size()));
    if (result) {
        env->SetFloatArrayRegion(result, 0, static_cast<jsize>(triangles.size()), triangles.data());
    }
    return result;
}

/**
 * @brief JNI Bridge: Performs batch calculation of world-space AABBs using shape pointer, position, and rotation.
 *
 * @param env JNI Environment pointer.
 * @param clazz Calling Java class object.
 * @param capacity Maximum capacity of the arrays (total number of bodies).
 * @param shapeAddressBuf Direct LongBuffer of shape native virtual addresses.
 * @param posXBuf Direct DoubleBuffer of body X-coordinates.
 * @param posYBuf Direct DoubleBuffer of body Y-coordinates.
 * @param posZBuf Direct DoubleBuffer of body Z-coordinates.
 * @param rotXBuf Direct FloatBuffer of body rotation X-components.
 * @param rotYBuf Direct FloatBuffer of body rotation Y-components.
 * @param rotZBuf Direct FloatBuffer of body rotation Z-components.
 * @param rotWBuf Direct FloatBuffer of body rotation W-components.
 * @param aabbMinXBuf Direct FloatBuffer to write calculated minimum X coordinates.
 * @param aabbMinYBuf Direct FloatBuffer to write calculated minimum Y coordinates.
 * @param aabbMinZBuf Direct FloatBuffer to write calculated minimum Z coordinates.
 * @param aabbMaxXBuf Direct FloatBuffer to write calculated maximum X coordinates.
 * @param aabbMaxYBuf Direct FloatBuffer to write calculated maximum Y coordinates.
 * @param aabbMaxZBuf Direct FloatBuffer to write calculated maximum Z coordinates.
 * @param isInitializedBuf Direct ByteBuffer containing initialization flags for rendering.
 */
JNIEXPORT void JNICALL
Java_net_xmx_velthoric_jni_ShapeBridge_nCalculateAABBs(
    JNIEnv* env, jclass clazz,
    jint capacity,
    jobject shapeAddressBuf,
    jobject posXBuf, jobject posYBuf, jobject posZBuf,
    jobject rotXBuf, jobject rotYBuf, jobject rotZBuf, jobject rotWBuf,
    jobject aabbMinXBuf, jobject aabbMinYBuf, jobject aabbMinZBuf,
    jobject aabbMaxXBuf, jobject aabbMaxYBuf, jobject aabbMaxZBuf,
    jobject isInitializedBuf
) {
    (void)clazz;
    if (capacity <= 0) return;

    jlong* shapeAddresses = (jlong*)env->GetDirectBufferAddress(shapeAddressBuf);
    jdouble* posX = (jdouble*)env->GetDirectBufferAddress(posXBuf);
    jdouble* posY = (jdouble*)env->GetDirectBufferAddress(posYBuf);
    jdouble* posZ = (jdouble*)env->GetDirectBufferAddress(posZBuf);

    jfloat* rotX = (jfloat*)env->GetDirectBufferAddress(rotXBuf);
    jfloat* rotY = (jfloat*)env->GetDirectBufferAddress(rotYBuf);
    jfloat* rotZ = (jfloat*)env->GetDirectBufferAddress(rotZBuf);
    jfloat* rotW = (jfloat*)env->GetDirectBufferAddress(rotWBuf);

    jfloat* aabbMinX = (jfloat*)env->GetDirectBufferAddress(aabbMinXBuf);
    jfloat* aabbMinY = (jfloat*)env->GetDirectBufferAddress(aabbMinYBuf);
    jfloat* aabbMinZ = (jfloat*)env->GetDirectBufferAddress(aabbMinZBuf);

    jfloat* aabbMaxX = (jfloat*)env->GetDirectBufferAddress(aabbMaxXBuf);
    jfloat* aabbMaxY = (jfloat*)env->GetDirectBufferAddress(aabbMaxYBuf);
    jfloat* aabbMaxZ = (jfloat*)env->GetDirectBufferAddress(aabbMaxZBuf);

    jbyte* isInitialized = (jbyte*)env->GetDirectBufferAddress(isInitializedBuf);

    if (!shapeAddresses || !posX || !posY || !posZ || !rotX || !rotY || !rotZ || !rotW ||
        !aabbMinX || !aabbMinY || !aabbMinZ || !aabbMaxX || !aabbMaxY || !aabbMaxZ || !isInitialized) {
        return;
    }

    for (int i = 0; i < capacity; ++i) {
        if (isInitialized[i] == 0) continue;
        jlong shapeVa = shapeAddresses[i];
        if (shapeVa == 0) continue;

        auto* shape = reinterpret_cast<const JPH::Shape*>(shapeVa);
        if (!shape) continue;

        JPH::RVec3 bodyPos(posX[i], posY[i], posZ[i]);
        JPH::Quat bodyRot(rotX[i], rotY[i], rotZ[i], rotW[i]);

        JPH::RMat44 comTransform = JPH::RMat44::sRotationTranslation(bodyRot, bodyPos);

        JPH::AABox bounds = shape->GetWorldSpaceBounds(comTransform, JPH::Vec3::sReplicate(1.0f));

        JPH::Vec3 min = bounds.mMin;
        JPH::Vec3 max = bounds.mMax;

        aabbMinX[i] = min.GetX();
        aabbMinY[i] = min.GetY();
        aabbMinZ[i] = min.GetZ();

        aabbMaxX[i] = max.GetX();
        aabbMaxY[i] = max.GetY();
        aabbMaxZ[i] = max.GetZ();
    }
}

}