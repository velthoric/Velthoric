/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.mixin.impl.event.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
/*? if >=1.21.1 {*/
import net.minecraft.client.DeltaTracker;
/*?}*/
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.xmx.velthoric.event.api.VxRenderEvent;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author xI-Mx-Ix
 */
@Mixin(LevelRenderer.class)
public class MixinLevelRenderer_VxRenderEvent {

    @Inject(
            method = /*? if >=1.21.1 {*/ "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V" /*?} else {*/ /*"renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V" *//*?}*/,
            at = @At(
                    value = "INVOKE",
                    target = /*? if >=1.21.1 {*/ "Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V" /*?} else {*/ /*"Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V" *//*?}*/,
                    shift = At.Shift.AFTER
            )
    )
    /*? if >=1.21.1 {*/
    private void velthoric_fireRenderStageAfterSky(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        PoseStack poseStack = new PoseStack();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
    /*?} else {*/
    /*private void velthoric_fireRenderStageAfterSky(PoseStack poseStack, float partialTick, long finishTimeNano, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
    *//*?}*/
        VxRenderEvent.ClientRenderLevelStageEvent.EVENT.invoker().onRenderLevelStage(
                new VxRenderEvent.ClientRenderLevelStageEvent(VxRenderEvent.ClientRenderLevelStageEvent.Stage.AFTER_SKY, (LevelRenderer)(Object)this, poseStack, partialTick, lightTexture, projectionMatrix)
        );
    }

    @Inject(
            method = /*? if >=1.21.1 {*/ "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V" /*?} else {*/ /*"renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V" *//*?}*/,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch(Lnet/minecraft/client/renderer/RenderType;)V",
                    /*? if >=1.21.1 {*/
                    ordinal = 3,
                    /*?} else {*/
                    /*args = "f=Lnet/minecraft/client/renderer/RenderType;args[0].staticValue=Lnet/minecraft/client/renderer/RenderType;entitySmoothCutout(Lnet/minecraft/resources/ResourceLocation;)",
                    *//*?}*/
                    shift = At.Shift.AFTER
            )
    )
    /*? if >=1.21.1 {*/
    private void velthoric_fireRenderStageAfterEntities(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        PoseStack poseStack = new PoseStack();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
    /*?} else {*/
    /*private void velthoric_fireRenderStageAfterEntities(PoseStack poseStack, float partialTick, long finishTimeNano, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
    *//*?}*/
        VxRenderEvent.ClientRenderLevelStageEvent.EVENT.invoker().onRenderLevelStage(
                new VxRenderEvent.ClientRenderLevelStageEvent(VxRenderEvent.ClientRenderLevelStageEvent.Stage.AFTER_ENTITIES, (LevelRenderer)(Object)this, poseStack, partialTick, lightTexture, projectionMatrix)
        );
    }

    @Inject(
            method = /*? if >=1.21.1 {*/ "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V" /*?} else {*/ /*"renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V" *//*?}*/,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V",
                    shift = At.Shift.AFTER
            )
    )
    /*? if >=1.21.1 {*/
    private void velthoric_fireRenderStageAfterBlockEntities(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        PoseStack poseStack = new PoseStack();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
    /*?} else {*/
    /*private void velthoric_fireRenderStageAfterBlockEntities(PoseStack poseStack, float partialTick, long finishTimeNano, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
    *//*?}*/
        VxRenderEvent.ClientRenderLevelStageEvent.EVENT.invoker().onRenderLevelStage(
                new VxRenderEvent.ClientRenderLevelStageEvent(VxRenderEvent.ClientRenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES, (LevelRenderer)(Object)this, poseStack, partialTick, lightTexture, projectionMatrix)
        );
    }

    @Inject(
            method = /*? if >=1.21.1 {*/ "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V" /*?} else {*/ /*"renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V" *//*?}*/,
            at = @At(
                    value = "INVOKE",
                    target = /*? if >=1.21.1 {*/ "Lnet/minecraft/client/Options;getCloudsType()Lnet/minecraft/client/CloudStatus;" /*?} else {*/ /*"Lcom/mojang/blaze3d/systems/RenderSystem;getModelViewStack()Lcom/mojang/blaze3d/vertex/PoseStack;" *//*?}*/,
                    shift = At.Shift.BEFORE
            )
    )
    /*? if >=1.21.1 {*/
    private void velthoric_fireRenderStageAfterParticles(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        PoseStack poseStack = new PoseStack();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
    /*?} else {*/
    /*private void velthoric_fireRenderStageAfterParticles(PoseStack poseStack, float partialTick, long finishTimeNano, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
    *//*?}*/
        VxRenderEvent.ClientRenderLevelStageEvent.EVENT.invoker().onRenderLevelStage(
                new VxRenderEvent.ClientRenderLevelStageEvent(VxRenderEvent.ClientRenderLevelStageEvent.Stage.AFTER_PARTICLES, (LevelRenderer)(Object)this, poseStack, partialTick, lightTexture, projectionMatrix)
        );
    }

    @Inject(
            method = /*? if >=1.21.1 {*/ "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V" /*?} else {*/ /*"renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V" *//*?}*/,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V",
                    shift = At.Shift.AFTER
            )
    )
    /*? if >=1.21.1 {*/
    private void velthoric_fireRenderStageAfterWeather(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        PoseStack poseStack = new PoseStack();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
    /*?} else {*/
    /*private void velthoric_fireRenderStageAfterWeather(PoseStack poseStack, float partialTick, long finishTimeNano, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
    *//*?}*/
        VxRenderEvent.ClientRenderLevelStageEvent.EVENT.invoker().onRenderLevelStage(
                new VxRenderEvent.ClientRenderLevelStageEvent(VxRenderEvent.ClientRenderLevelStageEvent.Stage.AFTER_WEATHER, (LevelRenderer)(Object)this, poseStack, partialTick, lightTexture, projectionMatrix)
        );
    }

    @Inject(
            method = /*? if >=1.21.1 {*/ "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V" /*?} else {*/ /*"renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V" *//*?}*/,
            at = @At("TAIL")
    )
    /*? if >=1.21.1 {*/
    private void velthoric_fireRenderStageLevelLast(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        PoseStack poseStack = new PoseStack();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
    /*?} else {*/
    /*private void velthoric_fireRenderStageLevelLast(PoseStack poseStack, float partialTick, long finishTimeNano, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
    *//*?}*/
        VxRenderEvent.ClientRenderLevelStageEvent.EVENT.invoker().onRenderLevelStage(
                new VxRenderEvent.ClientRenderLevelStageEvent(VxRenderEvent.ClientRenderLevelStageEvent.Stage.LEVEL_LAST, (LevelRenderer)(Object)this, poseStack, partialTick, lightTexture, projectionMatrix)
        );
    }
}