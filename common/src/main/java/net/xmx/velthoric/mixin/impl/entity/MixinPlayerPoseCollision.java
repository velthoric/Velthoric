/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.mixin.impl.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.xmx.velthoric.core.entity.VxEntityCollisionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin handling pose transitions and height checks for players across versions.
 *
 * @author xI-Mx-Ix
 */
/*? if >=1.21.1 {*/
@Mixin(Player.class)
/*? } else {*/
/*@Mixin(Entity.class)
*//*? }*/
public abstract class MixinPlayerPoseCollision {

    /**
     * Prevents players from standing up (exiting crouch) if a Velthoric physics body blocks them from above.
     */
    /*? if >=1.21.1 {*/
    @Inject(method = "canPlayerFitWithinBlocksAndEntitiesWhen(Lnet/minecraft/world/entity/Pose;)Z", at = @At("RETURN"), cancellable = true)
    private void velthoric_canPlayerFitWithinBlocksAndEntitiesWhen(Pose pose, CallbackInfoReturnable<Boolean> cir) {
        this.velthoric_checkPoseFit(pose, cir);
    }
    /*? } else {*/
    /*@Inject(method = "canEnterPose(Lnet/minecraft/world/entity/Pose;)Z", at = @At("RETURN"), cancellable = true)
    private void velthoric_canEnterPose(Pose pose, CallbackInfoReturnable<Boolean> cir) {
        this.velthoric_checkPoseFit(pose, cir);
    }
    *//*? }*/

    @Unique
    private void velthoric_checkPoseFit(Pose pose, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            Entity entity = (Entity) (Object) this;
            if (entity instanceof Player self) {
                float currentHeight;
                float targetHeight;
                /*? if >=1.21.1 {*/
                currentHeight = self.getDimensions(self.getPose()).height();
                targetHeight = self.getDimensions(pose).height();
                /*? } else {*/
                /*currentHeight = self.getDimensions(self.getPose()).height;
                targetHeight = self.getDimensions(pose).height;
                *//*? }*/
                
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
}