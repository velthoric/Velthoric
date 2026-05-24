/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.mixin.impl.entity.dragging;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.xmx.velthoric.core.entity.interaction.VxEntityCollisionManager;
import net.xmx.velthoric.core.entity.interaction.client.VxClientEntityCollisionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Visual mixin that adjusts the PoseStack coordinate translations exclusively during the render frame.
 * Smoothes out interpolation artifacts created by other entities resting on moving bodies.
 *
 * @author xI-Mx-Ix
 */
@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {

    /**
     * Offsets the rendering matrix stack by the difference between mathematically precise 
     * arc-interpolation and standard linear interpolation.
     *
     * @param entity The entity being rendered.
     * @param x Camera-relative X coordinate.
     * @param y Camera-relative Y coordinate.
     * @param z Camera-relative Z coordinate.
     * @param cameraYaw The interpolated yaw orientation.
     * @param partialTick The frame interpolation alpha parameter.
     * @param poseStack The active transformation matrix stack.
     * @param bufferSource The active buffer rendering source.
     * @param packedLight The baked lighting bounds.
     * @param ci Callback info instance.
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void velthoric_offsetEntityRender(Entity entity, double x, double y, double z, float cameraYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        int groundIdx = VxEntityCollisionManager.getGroundSlotIdx(entity);
        if (groundIdx >= 0) {
            // Entities are rendered starting from the feet, so we use 0.0 for the eye height parameter
            Vec3 exactRenderPos = VxClientEntityCollisionManager.getExactInterpolatedRenderPos(entity, groundIdx, partialTick, 0.0);
            
            if (exactRenderPos != null) {
                // Calculate Minecraft's standard linear block interpolation target
                Vec3 linearPos = new Vec3(
                        Mth.lerp(partialTick, entity.xo, entity.getX()),
                        Mth.lerp(partialTick, entity.yo, entity.getY()),
                        Mth.lerp(partialTick, entity.zo, entity.getZ())
                );
                
                // Calculate the deviation error vector
                Vec3 offset = exactRenderPos.subtract(linearPos);
                
                // Translate the pose stack so that the entity is optically rendered at the correct arc-trajectory
                poseStack.translate(offset.x, offset.y, offset.z);
            }
        }
    }
}