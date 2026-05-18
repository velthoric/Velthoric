/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 *
 * Author: xI-Mx-Ix
 */
#pragma once
#include <jni.h>

namespace Velthoric {

/**
 * @brief JNI Bridge for providing stateless Jolt Physics narrow-phase collision (GJK/EPA)
 *        between Minecraft Entities and Velthoric Physics Bodies.
 *
 * This class handles JNI initialization and utility functions for character-body
 * interactions.
 */
class EntityBridgeManager {
public:
    /**
     * @brief Initializes JNI environment references if necessary.
     * @param env Pointer to the JNI Environment.
     */
    static void InitJNI(JNIEnv* env);
};

} // namespace Velthoric