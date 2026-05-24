/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.mixin.impl.entity.dragging;

import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import net.xmx.velthoric.core.entity.interaction.VxEntityCollisionManager;
import net.xmx.velthoric.core.entity.interaction.client.VxClientEntityCollisionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Visual mixin that adjusts the camera translation exclusively during the render frame.
 * Smoothes out interpolation artifacts created by angular rotation velocity.
 *
 * @author xI-Mx-Ix
 */
@Mixin(Camera.class)
public abstract class MixinCamera {

    @Shadow private Vec3 position;
    @Shadow protected abstract void setPosition(Vec3 pos);
    @Shadow private float eyeHeightOld;
    @Shadow private float eyeHeight;

    /**
     * Intercepts the camera setup method to adjust the visual position matrix,
     * compensating for body movement during high framerate rendering frames.
     *
     * @param level The active level area.
     * @param entity The camera target entity.
     * @param detached True if rendering in third person mode.
     * @param thirdPersonReverse True if rendering third person front mode.
     * @param partialTick The frame interpolation alpha parameter.
     * @param ci Callback info instance.
     */
    @Inject(method = "setup", at = @At("TAIL"))
    private void velthoric_smoothCamera(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        int groundIdx = VxEntityCollisionManager.getGroundSlotIdx(entity);
        if (groundIdx >= 0) {
            double currentEyeHeight = Mth.lerp(partialTick, this.eyeHeightOld, this.eyeHeight);
            Vec3 exactRenderPos = VxClientEntityCollisionManager.getExactInterpolatedRenderPos(entity, groundIdx, partialTick, currentEyeHeight);
            
            if (exactRenderPos != null) {
                // Calculate Minecraft's standard linear block interpolation target
                Vec3 linearPos = new Vec3(
                        Mth.lerp(partialTick, entity.xo, entity.getX()),
                        Mth.lerp(partialTick, entity.yo, entity.getY()) + currentEyeHeight,
                        Mth.lerp(partialTick, entity.zo, entity.getZ())
                );
                
                // Calculate the deviation error vector and apply it as a visual offset to the camera
                Vec3 offset = exactRenderPos.subtract(linearPos);
                this.setPosition(this.position.add(offset));
            }
        }
    }
}