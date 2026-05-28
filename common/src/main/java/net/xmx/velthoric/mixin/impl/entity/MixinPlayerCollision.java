/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.mixin.impl.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xmx.velthoric.core.entity.VxEntityCollisionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Specific collision mixin for Player entities to override edge-stopping logic
 * and manage crouch-standing pose transitions when interacting with physics bodies.
 *
 * @author xI-Mx-Ix
 */
@Mixin(Player.class)
public abstract class MixinPlayerCollision {

    /**
     * Injected logic to prevent players from sneaking off the edge of physics bodies.
     * Note: Players have a separate implementation of this method than generic entities.
     *
     * @param movement Proposed movement vector.
     * @param moverType The mover type defining the trigger source.
     * @param cir Callback returnable for the adjusted movement vector.
     */
    @Inject(method = "maybeBackOffFromEdge", at = @At("HEAD"), cancellable = true)
    private void velthoric_customBackOff(Vec3 movement, MoverType moverType, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;

        // Checks if the player is actively sneaking, moving downwards, and resting on a custom physics body.
        if (self.isShiftKeyDown() && movement.y <= 0.0D && VxEntityCollisionManager.isStandingOnBody(self)) {
            Vec3 adjusted = VxEntityCollisionManager.handleSneakBackOff(self, movement);
            cir.setReturnValue(adjusted);
        }
    }

    /**
     * Prevents players from standing up (exiting crouch) if a Velthoric physics body blocks them from above.
     */
    @Inject(method = "canPlayerFitWithinBlocksAndEntitiesWhen(Lnet/minecraft/world/entity/Pose;)Z", at = @At("RETURN"), cancellable = true)
    private void velthoric_canPlayerFitWithinBlocksAndEntitiesWhen(Pose pose, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            Player self = (Player) (Object) this;
            float currentHeight = self.getDimensions(self.getPose()).height();
            float targetHeight = self.getDimensions(pose).height();
            
            // Only restrict the pose transition if the player is trying to grow in height.
            if (targetHeight > currentHeight) {
                AABB poseBox = self.getDimensions(pose).makeBoundingBox(self.position());
                
                // Construct a bounding box representing only the newly expanded head volume.
                // This prevents the check from touching the floor/body under the player's feet.
                AABB headBox = new AABB(
                    poseBox.minX, 
                    poseBox.minY + currentHeight, 
                    poseBox.minZ, 
                    poseBox.maxX, 
                    poseBox.maxY, 
                    poseBox.maxZ
                );
                
                if (VxEntityCollisionManager.isColliding(self.level(), headBox)) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}