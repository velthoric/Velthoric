/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.mixin.impl.entity.dragging;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xmx.velthoric.core.entity.interaction.VxEntityCollisionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to adjust coordinate differences during LivingEntity's walk animation calculations.
 * Subtracts the active platform dragging displacement, preventing leg swinging animations
 * while standing still on a moving body.
 *
 * @author xI-Mx-Ix
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntityDragging {

    @Unique
    private double velthoric$savedXo;
    @Unique
    private double velthoric$savedYo;
    @Unique
    private double velthoric$savedZo;
    @Unique
    private boolean velthoric$adjusted = false;

    /**
     * Temporarily adjusts the previous tick's coordinates by adding the dragging displacement.
     * This offsets the linear speed calculation inside calculateEntityAnimation, so that
     * only actual relative walking movement of the entity triggers walking animations.
     */
    @Inject(method = "calculateEntityAnimation", at = @At("HEAD"))
    private void velthoric_adjustXoBeforeAnimation(boolean bl, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        int groundIdx = VxEntityCollisionManager.getGroundSlotIdx(self);
        if (groundIdx >= 0) {
            Vec3 displacement = VxEntityCollisionManager.getBodyDisplacement(self, groundIdx);
            if (displacement.lengthSqr() > 1.0E-10) {
                this.velthoric$savedXo = self.xo;
                this.velthoric$savedYo = self.yo;
                this.velthoric$savedZo = self.zo;

                self.xo += displacement.x;
                self.yo += displacement.y;
                self.zo += displacement.z;

                this.velthoric$adjusted = true;
            }
        }
    }

    /**
     * Restores original coordinate states back onto the entity once animation calculations complete,
     * maintaining local positioning consistency across downstream ticking systems.
     */
    @Inject(method = "calculateEntityAnimation", at = @At("TAIL"))
    private void velthoric_restoreXoAfterAnimation(boolean bl, CallbackInfo ci) {
        if (this.velthoric$adjusted) {
            Entity self = (Entity) (Object) this;
            self.xo = this.velthoric$savedXo;
            self.yo = this.velthoric$savedYo;
            self.zo = this.velthoric$savedZo;
            this.velthoric$adjusted = false;
        }
    }
}