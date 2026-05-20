/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.mixin.impl.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.xmx.velthoric.core.entity.interaction.VxEntityCollisionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Main collision mixin for Entity to handle movement on and against custom physics bodies.
 * Manages momentum transfer, sliding, and collision resolution.
 *
 * @author xI-Mx-Ix
 */
@Mixin(Entity.class)
public abstract class MixinEntityCollision {

    /**
     * Stores the movement vector from the last narrow-phase collision if sliding occurred.
     * Used to prevent Minecraft from cancelling sprints or zeroing velocity prematurely.
     */
    @Unique
    private Vec3 velthoric_lastSlideVector = null;

    /**
     * Hooks into the collide() method to perform narrow-phase collision resolution
     * using the Jolt Physics backend.
     *
     * @param movement The movement vector after Minecraft's block collision.
     * @param cir The callback returnable containing the final resolved vector.
     */
    @Inject(method = "collide", at = @At("RETURN"), cancellable = true)
    private void velthoric_onCollide(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        Vec3 currentMovement = cir.getReturnValue();

        // Pass the movement to the collision manager for native Jolt resolution
        Vec3 newMovement = VxEntityCollisionManager.handleCollision(self, currentMovement);

        if (currentMovement != newMovement) {
            cir.setReturnValue(newMovement);

            // If the horizontal movement changed significantly, we are sliding against a body
            if (Math.abs(currentMovement.x - newMovement.x) > 1.0E-5 ||
                    Math.abs(currentMovement.z - newMovement.z) > 1.0E-5) {
                this.velthoric_lastSlideVector = newMovement;
            } else {
                this.velthoric_lastSlideVector = null;
            }
        } else {
            this.velthoric_lastSlideVector = null;
        }
    }

    /**
     * Prevents the entity from losing sprint state when sliding along a physics body.
     *
     * @param deltaMovement Current delta movement of the entity.
     * @param cir Callback returnable containing the evaluation result.
     */
    @Inject(method = "isHorizontalCollisionMinor", at = @At("HEAD"), cancellable = true)
    private void velthoric_keepSprintWhenSliding(Vec3 deltaMovement, CallbackInfoReturnable<Boolean> cir) {
        if (this.velthoric_lastSlideVector != null) {
            double horizontalSpeedSq = deltaMovement.x * deltaMovement.x + deltaMovement.z * deltaMovement.z;

            // If moving horizontally and sliding, treat collision as minor
            if (horizontalSpeedSq > 1.0E-5) {
                cir.setReturnValue(true);
            }
        }
    }

    /**
     * Redirects the zeroing of delta movement to allow continued horizontal velocity
     * if the entity is sliding along a slanted physics body.
     *
     * @param instance Target Entity instance.
     * @param x Proposed delta X movement.
     * @param y Proposed delta Y movement.
     * @param z Proposed delta Z movement.
     */
    @Redirect(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(DDD)V"))
    private void velthoric_redirectHorizontalZeroing(Entity instance, double x, double y, double z) {
        if (this.velthoric_lastSlideVector != null) {
            Vec3 slide = this.velthoric_lastSlideVector;

            double platformVx = 0.0;
            double platformVz = 0.0;
            int groundSlotIdx = VxEntityCollisionManager.getGroundSlotIdx(instance);

            // Fetch exact frame movement of the supporting platform
            if (groundSlotIdx >= 0) {
                Vec3 pVel = VxEntityCollisionManager.getPlatformVelocity(instance, groundSlotIdx);
                platformVx = pVel.x;
                platformVz = pVel.z;
            }

            // Adjust delta deceleration around platform movement to retain local velocity.
            if (x == 0.0D) {
                double relativeSlideX = slide.x - platformVx;
                if (Math.abs(relativeSlideX) > 1.0E-5) {
                    x = relativeSlideX;
                }
            }
            if (z == 0.0D) {
                double relativeSlideZ = slide.z - platformVz;
                if (Math.abs(relativeSlideZ) > 1.0E-5) {
                    z = relativeSlideZ;
                }
            }
        }

        instance.setDeltaMovement(x, y, z);
    }

    /**
     * Clears the slide vector state at the end of the move call.
     *
     * @param type MoverType defining origin context.
     * @param pos New spatial coordinates of the entity.
     * @param ci Callback info instance.
     */
    @Inject(method = "move", at = @At("TAIL"))
    private void velthoric_clearSlideVector(MoverType type, Vec3 pos, CallbackInfo ci) {
        this.velthoric_lastSlideVector = null;
    }

    /**
     * Hook to invoke sneak edge-blocking capabilities on generic physics bodies.
     *
     * @param movement Proposed movement vector.
     * @param moverType Trigger MoverType context.
     * @param cir Callback returnable containing adjusted movement vector.
     */
    @Inject(method = "maybeBackOffFromEdge", at = @At("HEAD"), cancellable = true)
    private void velthoric_customBackOff(Vec3 movement, MoverType moverType, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        // Check if the entity is sneaking and standing on a physics platform
        if (self.isShiftKeyDown() && movement.y <= 0.0D && VxEntityCollisionManager.isStandingOnPlatform(self)) {
            Vec3 adjusted = VxEntityCollisionManager.handleSneakBackOff(self, movement);
            cir.setReturnValue(adjusted);
        }
    }
}