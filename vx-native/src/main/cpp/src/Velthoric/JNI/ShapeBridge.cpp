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
Java_net_xmx_velthoric_jni_VxShapeBridge_nGetShapeTriangles(JNIEnv* env, jclass clazz, jlong shapeVa) {
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

}