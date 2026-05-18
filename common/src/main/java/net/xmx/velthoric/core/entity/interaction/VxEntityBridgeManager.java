/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.entity.interaction;

import com.github.stephengold.joltjni.ShapeRefC;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xmx.velthoric.core.body.VxAbstractBodyManager;
import net.xmx.velthoric.core.body.VxBody;
import net.xmx.velthoric.core.body.client.VxClientBodyManager;
import net.xmx.velthoric.core.body.shape.VxCollisionShape;
import net.xmx.velthoric.core.physics.world.VxPhysicsWorld;
import net.xmx.velthoric.jni.EntityBridge;
import net.xmx.velthoric.math.VxTransform;

import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Orchestrates the interaction between Minecraft Entities and Velthoric Physics Bodies.
 * Manages data marshalling for JNI calls to the native Jolt physics engine.
 *
 * @author xI-Mx-Ix
 */
public class VxEntityBridgeManager {

    /**
     * Result buffer for JNI calls: [dx, dy, dz, walkedOnBodyIdx, frictionMultiplier]
     */
    private static final ThreadLocal<float[]> OUT_RESULT = ThreadLocal.withInitial(() -> new float[5]);

    /**
     * Native pointers to Jolt shapes for the current broad-phase result.
     */
    private static final ThreadLocal<long[]> SHAPE_PTRS = ThreadLocal.withInitial(() -> new long[128]);

    /**
     * Packed transforms (3x Translation, 4x Rotation) for the shapes.
     */
    private static final ThreadLocal<float[]> TRANSFORMS = ThreadLocal.withInitial(() -> new float[128 * 7]);

    /**
     * Cache to store and reuse native Jolt shape references for specific collision shapes.
     */
    private static final Map<VxCollisionShape, ShapeRefC> SHAPE_CACHE = new WeakHashMap<>();

    /**
     * Resizes the thread-local buffers if the number of bodies exceeds current capacity.
     *
     * @param count Required capacity.
     */
    private static void ensureCapacity(int count) {
        long[] ptrs = SHAPE_PTRS.get();
        if (ptrs.length < count) {
            int newSize = Math.max(ptrs.length * 2, count);
            SHAPE_PTRS.set(new long[newSize]);
            TRANSFORMS.set(new float[newSize * 7]);
        }
    }

    /**
     * Resolves collision between an entity and all relevant physics bodies.
     * Uses native GJK/EPA via Jolt for high-precision resolution.
     *
     * @param entity   The Minecraft entity.
     * @param movement The proposed movement vector.
     * @return The adjusted movement vector after physical resolution.
     */
    public static Vec3 handleCollision(Entity entity, Vec3 movement) {
        if (entity.level() == null) return movement;

        VxAbstractBodyManager manager = null;
        if (entity.level() instanceof ServerLevel serverLevel) {
            VxPhysicsWorld world = VxPhysicsWorld.get(serverLevel.dimension());
            if (world != null) {
                manager = world.getBodyManager();
            }
        } else {
            manager = VxClientBodyManager.getInstance();
        }

        if (manager == null) return movement;

        Collection<VxBody> bodies = manager.getAllBodies();
        if (bodies == null || bodies.isEmpty()) return movement;

        ensureCapacity(bodies.size());
        long[] ptrs = SHAPE_PTRS.get();
        float[] transforms = TRANSFORMS.get();

        int count = 0;
        VxTransform t = new VxTransform();
        VxBody[] bodyArray = new VxBody[bodies.size()];

        // Marshall body data into arrays for JNI
        for (VxBody body : bodies) {
            VxCollisionShape shape = body.getShape();
            if (shape == null) continue;

            ShapeRefC shapeRef = SHAPE_CACHE.computeIfAbsent(shape, VxCollisionShape::createShapeRef);
            long va = shapeRef.getPtr().targetVa();
            if (va == 0) continue;

            body.getTransform(t);

            ptrs[count] = va;
            int tIdx = count * 7;
            transforms[tIdx] = (float) t.getTranslation().x();
            transforms[tIdx + 1] = (float) t.getTranslation().y();
            transforms[tIdx + 2] = (float) t.getTranslation().z();
            transforms[tIdx + 3] = t.getRotation().getX();
            transforms[tIdx + 4] = t.getRotation().getY();
            transforms[tIdx + 5] = t.getRotation().getZ();
            transforms[tIdx + 6] = t.getRotation().getW();

            bodyArray[count] = body;
            count++;
        }

        if (count == 0) return movement;

        AABB entityBox = entity.getBoundingBox();
        float boxHx = (float) (entityBox.getXsize() / 2.0);
        float boxHy = (float) (entityBox.getYsize() / 2.0);
        float boxHz = (float) (entityBox.getZsize() / 2.0);
        float boxX = (float) entityBox.getCenter().x;
        float boxY = (float) entityBox.getCenter().y;
        float boxZ = (float) entityBox.getCenter().z;

        float[] outResult = OUT_RESULT.get();
        outResult[4] = 1.0f; // Friction
        float stepHeight = entity.maxUpStep();

        // Native call for narrow-phase resolution
        EntityBridge.nHandleCollision(
                ptrs, transforms, count,
                boxHx, boxHy, boxHz,
                boxX, boxY, boxZ,
                (float) movement.x, (float) movement.y, (float) movement.z, stepHeight,
                outResult
        );

        float outDx = outResult[0];
        float outDy = outResult[1];
        float outDz = outResult[2];

        double epsilon = 1.0E-5;
        double finalDx = Math.abs(outDx - movement.x) < epsilon ? movement.x : outDx;
        double finalDy = Math.abs(outDy - movement.y) < epsilon ? movement.y : outDy;
        double finalDz = Math.abs(outDz - movement.z) < epsilon ? movement.z : outDz;

        // If the native code detected a body underneath, update the controller
        if (outResult[3] >= 0) {
            int bodyIdx = (int) outResult[3];
            if (bodyIdx < count) {
                VxPlatformController.setController(entity, bodyArray[bodyIdx]);
            }
        }

        // Apply potential friction slowdown if requested by physics
        if (outResult[4] < 1.0f) {
            Vec3 currentDelta = entity.getDeltaMovement();
            entity.setDeltaMovement(currentDelta.x * outResult[4], currentDelta.y * outResult[4], currentDelta.z * outResult[4]);
        }

        return new Vec3(finalDx, finalDy, finalDz);
    }

    /**
     * Prevents an entity from falling off a physics body while sneaking.
     *
     * @param entity        The entity.
     * @param totalMovement Current movement vector.
     * @return Adjusted movement vector constrained to the body surface.
     */
    public static Vec3 handleSneakBackOff(Entity entity, Vec3 totalMovement) {
        if (entity.level() == null) return totalMovement;

        VxAbstractBodyManager manager = null;
        if (entity.level() instanceof ServerLevel serverLevel) {
            VxPhysicsWorld world = VxPhysicsWorld.get(serverLevel.dimension());
            if (world != null) {
                manager = world.getBodyManager();
            }
        } else {
            manager = VxClientBodyManager.getInstance();
        }

        if (manager == null) return totalMovement;

        Collection<VxBody> bodies = manager.getAllBodies();
        if (bodies == null || bodies.isEmpty()) return totalMovement;

        ensureCapacity(bodies.size());
        long[] ptrs = SHAPE_PTRS.get();
        float[] transforms = TRANSFORMS.get();

        int count = 0;
        VxTransform t = new VxTransform();

        for (VxBody body : bodies) {
            VxCollisionShape shape = body.getShape();
            if (shape == null) continue;

            ShapeRefC shapeRef = SHAPE_CACHE.computeIfAbsent(shape, VxCollisionShape::createShapeRef);
            long va = shapeRef.getPtr().targetVa();
            if (va == 0) continue;

            body.getTransform(t);

            ptrs[count] = va;
            int tIdx = count * 7;
            transforms[tIdx] = (float) t.getTranslation().x();
            transforms[tIdx + 1] = (float) t.getTranslation().y();
            transforms[tIdx + 2] = (float) t.getTranslation().z();
            transforms[tIdx + 3] = t.getRotation().getX();
            transforms[tIdx + 4] = t.getRotation().getY();
            transforms[tIdx + 5] = t.getRotation().getZ();
            transforms[tIdx + 6] = t.getRotation().getW();
            count++;
        }

        if (count == 0) return totalMovement;

        VxPlatformController controller = VxPlatformController.getController(entity);
        Vec3 bodyDelta = (controller != null && controller.lastDelta != null) ? controller.lastDelta : Vec3.ZERO;

        Vec3 playerDelta = totalMovement.subtract(bodyDelta);

        AABB entityBox = entity.getBoundingBox();
        float boxHx = (float) (entityBox.getXsize() / 2.0);
        float boxHy = (float) (entityBox.getYsize() / 2.0);
        float boxHz = (float) (entityBox.getZsize() / 2.0);

        float startBoxX = (float) (entityBox.getCenter().x + bodyDelta.x);
        float startBoxY = (float) (entityBox.getCenter().y + bodyDelta.y);
        float startBoxZ = (float) (entityBox.getCenter().z + bodyDelta.z);

        float[] outResult = OUT_RESULT.get();

        // Native call for safety check and path finding on body surface
        EntityBridge.nHandleSneak(
                ptrs, transforms, count,
                boxHx, boxHy, boxHz,
                startBoxX, startBoxY, startBoxZ,
                (float) playerDelta.x, (float) playerDelta.z, entity.maxUpStep(),
                outResult
        );

        return new Vec3(outResult[0] + bodyDelta.x, totalMovement.y, outResult[1] + bodyDelta.z);
    }

    /**
     * Performs a fast intersection check between an entity's AABB and physics bodies.
     *
     * @param entity    The entity.
     * @param entityBox The AABB to check.
     * @return True if intersecting any physics body.
     */
    public static boolean isColliding(Entity entity, AABB entityBox) {
        if (entity.level() == null) return false;

        VxAbstractBodyManager manager = null;
        if (entity.level() instanceof ServerLevel serverLevel) {
            VxPhysicsWorld world = VxPhysicsWorld.get(serverLevel.dimension());
            if (world != null) {
                manager = world.getBodyManager();
            }
        } else {
            manager = VxClientBodyManager.getInstance();
        }

        if (manager == null) return false;

        Collection<VxBody> bodies = manager.getAllBodies();
        if (bodies == null || bodies.isEmpty()) return false;

        ensureCapacity(bodies.size());
        long[] ptrs = SHAPE_PTRS.get();
        float[] transforms = TRANSFORMS.get();

        int count = 0;
        VxTransform t = new VxTransform();

        for (VxBody body : bodies) {
            VxCollisionShape shape = body.getShape();
            if (shape == null) continue;

            ShapeRefC shapeRef = SHAPE_CACHE.computeIfAbsent(shape, VxCollisionShape::createShapeRef);
            long va = shapeRef.getPtr().targetVa();
            if (va == 0) continue;

            body.getTransform(t);

            ptrs[count] = va;
            int tIdx = count * 7;
            transforms[tIdx] = (float) t.getTranslation().x();
            transforms[tIdx + 1] = (float) t.getTranslation().y();
            transforms[tIdx + 2] = (float) t.getTranslation().z();
            transforms[tIdx + 3] = t.getRotation().getX();
            transforms[tIdx + 4] = t.getRotation().getY();
            transforms[tIdx + 5] = t.getRotation().getZ();
            transforms[tIdx + 6] = t.getRotation().getW();

            count++;
        }

        if (count == 0) return false;

        float boxHx = (float) (entityBox.getXsize() / 2.0);
        float boxHy = (float) (entityBox.getYsize() / 2.0);
        float boxHz = (float) (entityBox.getZsize() / 2.0);
        float boxX = (float) entityBox.getCenter().x;
        float boxY = (float) entityBox.getCenter().y;
        float boxZ = (float) entityBox.getCenter().z;

        return EntityBridge.nIsColliding(
                ptrs, transforms, count,
                boxHx, boxHy, boxHz,
                boxX, boxY, boxZ
        );
    }
}