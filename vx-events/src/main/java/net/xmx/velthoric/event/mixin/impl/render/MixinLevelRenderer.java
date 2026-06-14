/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.mixin.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
/*? if >=1.21.1 {*/
import net.minecraft.client.DeltaTracker;
/*? }*/
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
//? if <=1.21.1 {
import net.minecraft.client.renderer.LightTexture;
//? }
import net.xmx.velthoric.event.api.VxRenderEvent;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin targeting LevelRenderer to insert client side world level rendering events.
 *
 * @author xI-Mx-Ix
 */
@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
/*? if >=1.21.1 {*/
    /**
     * Injects into level rendering after entities are fully processed to trigger custom rendering.
     */
    @Inject(
            method = "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch(Lnet/minecraft/client/renderer/RenderType;)V",
                    ordinal = 3,
                    shift = At.Shift.AFTER
            )
    )
    private void velthoric$onRenderLevelAfterEntities(
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            //? if 1.21.1 {
            LightTexture lightTexture,
            //?}
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {
        PoseStack poseStack = new PoseStack();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        VxRenderEvent.EVENT.invoker().onRenderLevel(
                new VxRenderEvent(
                        (LevelRenderer) (Object) this,
                        poseStack, partialTick,
                        projectionMatrix
                )
        );
    }
/*? } else {*/
    /*/^*
     * Injects into level rendering after entities are fully processed to trigger custom rendering.
     ^/
    @Inject(
            method = "renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch(Lnet/minecraft/client/renderer/RenderType;)V",
                    args = "f=Lnet/minecraft/client/renderer/RenderType;args[0].staticValue=Lnet/minecraft/client/renderer/RenderType;entitySmoothCutout(Lnet/minecraft/resources/ResourceLocation;)",
                    shift = At.Shift.AFTER
            )
    )
    private void velthoric$onRenderLevelAfterEntities(
            PoseStack poseStack,
            float partialTick,
            long finishTimeNano,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {
        VxRenderEvent.EVENT.invoker().onRenderLevel(
                new VxRenderEvent((LevelRenderer) (Object) this, poseStack, partialTick, projectionMatrix)
        );
    }
*//*? }*/
}