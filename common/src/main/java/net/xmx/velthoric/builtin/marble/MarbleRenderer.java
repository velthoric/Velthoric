/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.builtin.marble;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//? if >=26.1 {
/*import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.data.AtlasIds;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.data.AtlasIds;
*///? } else {
import net.minecraft.client.resources.model.BakedModel;
 //? }
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.xmx.velthoric.core.body.client.VxRenderState;
import net.xmx.velthoric.core.body.client.renderer.VxBodyRenderer;
import net.xmx.velthoric.core.body.client.renderer.VxVertexConsumer;
import net.xmx.velthoric.core.body.VxBody;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Renderer for the {@link MarbleRigidBody}.
 *
 * @author xI-Mx-Ix
 * @author timtaran
 */
public class MarbleRenderer extends VxBodyRenderer<VxBody> {
    //? if >=26.1 {
    /*private static final ResourceLocation MARBLE_IDENTIFIER = ResourceLocation.withDefaultNamespace("item/magma_cream");
    *///? } else {
    private static final ItemStack MARBLE_ITEM_STACK = new ItemStack(Items.MAGMA_CREAM);
    //? }

    @Override
    public void render(VxBody body, LevelRenderer levelRenderer, PoseStack poseStack, MultiBufferSource bufferSource, float partialTicks, int packedLight, VxRenderState renderState) {
        poseStack.pushPose();

        float radius = body.get(MarbleRigidBody.DATA_RADIUS);
        poseStack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());

        //? if >=26.1 {
        /*TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().get(new SpriteId(AtlasIds.PARTICLES, MARBLE_IDENTIFIER));
        *///? } else {
        BakedModel itemModel = Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(MARBLE_ITEM_STACK);
        TextureAtlasSprite sprite = itemModel.getParticleIcon();
         //? }

        VxVertexConsumer vertexConsumer = VxVertexConsumer.wrap(bufferSource.getBuffer(RenderType.entityTranslucent(
                //? if >=26.1 {
                /*AtlasIds.ITEMS
                *///? } else {
                 InventoryMenu.BLOCK_ATLAS
                //? }
        )));
        Matrix4f matrix4f = poseStack.last().pose();
        Matrix3f matrix3f = poseStack.last().normal();

        addVertex(vertexConsumer, matrix4f, matrix3f, -radius, -radius, sprite.getU0(), sprite.getV1(), packedLight);
        addVertex(vertexConsumer, matrix4f, matrix3f,  radius, -radius, sprite.getU1(), sprite.getV1(), packedLight);
        addVertex(vertexConsumer, matrix4f, matrix3f,  radius,  radius, sprite.getU1(), sprite.getV0(), packedLight);
        addVertex(vertexConsumer, matrix4f, matrix3f, -radius,  radius, sprite.getU0(), sprite.getV0(), packedLight);

        poseStack.popPose();
    }

    private void addVertex(VxVertexConsumer consumer, Matrix4f pose, Matrix3f normalMatrix, float x, float y, float u, float v, int packedLight) {
        consumer.vertex(pose, x, y, 0.0f)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlay(OverlayTexture.NO_OVERLAY)
                .light(packedLight)
                .normal(normalMatrix, 0, 1, 0)
                .endVertex();
    }
}