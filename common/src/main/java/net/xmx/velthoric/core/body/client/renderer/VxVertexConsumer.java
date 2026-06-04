/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.body.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * A wrapper around {@link VertexConsumer} to abstract differences in vertex building
 * APIs between Minecraft 1.20.1 and 1.21.1.
 *
 * @author xI-Mx-Ix
 */
public class VxVertexConsumer {
    private final VertexConsumer delegate;
    /*? if >=1.21.1 {*/
    private VertexConsumer currentChain;
    /*? }*/

    public VxVertexConsumer(VertexConsumer delegate) {
        this.delegate = delegate;
    }

    public static VxVertexConsumer wrap(VertexConsumer delegate) {
        return new VxVertexConsumer(delegate);
    }

    public VertexConsumer getDelegate() {
        return delegate;
    }

    public VxVertexConsumer vertex(PoseStack.Pose pose, float x, float y, float z) {
        /*? if >=1.21.1 {*/
        currentChain = delegate.addVertex(pose, x, y, z);
        /*? } else {*/
         /*delegate.vertex(pose.pose(), x, y, z);
        *//*? }*/
        return this;
    }

    public VxVertexConsumer vertex(PoseStack.Pose pose, double x, double y, double z) {
        /*? if >=1.21.1 {*/
        currentChain = delegate.addVertex(pose, (float) x, (float) y, (float) z);
        /*? } else {*/
         /*delegate.vertex(pose.pose(), (float) x, (float) y, (float) z);
        *//*? }*/
        return this;
    }

    public VxVertexConsumer vertex(Matrix4f pose, float x, float y, float z) {
        /*? if >=1.21.1 {*/
        currentChain = delegate.addVertex(pose, x, y, z);
        /*? } else {*/
         /*delegate.vertex(pose, x, y, z);
        *//*? }*/
        return this;
    }

    public VxVertexConsumer vertex(Matrix4f pose, double x, double y, double z) {
        /*? if >=1.21.1 {*/
        currentChain = delegate.addVertex(pose, (float) x, (float) y, (float) z);
        /*? } else {*/
         /*delegate.vertex(pose, (float) x, (float) y, (float) z);
        *//*? }*/
        return this;
    }

    public VxVertexConsumer vertex(double x, double y, double z) {
        /*? if >=1.21.1 {*/
        currentChain = delegate.addVertex((float) x, (float) y, (float) z);
        /*? } else {*/
         /*delegate.vertex(x, y, z);
        *//*? }*/
        return this;
    }

    public VxVertexConsumer color(int r, int g, int b, int a) {
        /*? if >=1.21.1 {*/
        currentChain.setColor(r, g, b, a);
        /*? } else {*/
         /*delegate.color(r, g, b, a);
        *//*? }*/
        return this;
    }

    public VxVertexConsumer color(int color) {
        /*? if >=1.21.1 {*/
        currentChain.setColor(color);
        /*? } else {*/
         /*delegate.color(color);
        *//*? }*/
        return this;
    }

    public VxVertexConsumer color(float r, float g, float b, float a) {
        /*? if >=1.21.1 {*/
        currentChain.setColor(r, g, b, a);
        /*? } else {*/
         /*delegate.color((int)(r * 255.0F), (int)(g * 255.0F), (int)(b * 255.0F), (int)(a * 255.0F));
        *//*? }*/
        return this;
    }

    public VxVertexConsumer uv(float u, float v) {
        /*? if >=1.21.1 {*/
        currentChain.setUv(u, v);
        /*? } else {*/
         /*delegate.uv(u, v);
        *//*? }*/
        return this;
    }

    public VxVertexConsumer overlay(int overlay) {
        /*? if >=1.21.1 {*/
        currentChain.setOverlay(overlay);
        /*? } else {*/
         /*delegate.overlayCoords(overlay);
        *//*? }*/
        return this;
    }

    public VxVertexConsumer light(int light) {
        /*? if >=1.21.1 {*/
        currentChain.setLight(light);
        /*? } else {*/
         /*delegate.uv2(light);
        *//*? }*/
        return this;
    }

    public VxVertexConsumer normal(float x, float y, float z) {
        /*? if >=1.21.1 {*/
        currentChain.setNormal(x, y, z);
        /*? } else {*/
         /*delegate.normal(x, y, z);
        *//*? }*/
        return this;
    }

    public VxVertexConsumer normal(PoseStack.Pose pose, float x, float y, float z) {
        /*? if >=1.21.1 {*/
        currentChain.setNormal(pose, x, y, z);
        /*? } else {*/
         /*delegate.normal(pose.normal(), x, y, z);
        *//*? }*/
        return this;
    }

    public VxVertexConsumer normal(PoseStack.Pose pose, double x, double y, double z) {
        /*? if >=1.21.1 {*/
        currentChain.setNormal(pose, (float) x, (float) y, (float) z);
        /*? } else {*/
         /*delegate.normal(pose.normal(), (float) x, (float) y, (float) z);
        *//*? }*/
        return this;
    }

    public VxVertexConsumer normal(Matrix3f normalMatrix, float x, float y, float z) {
        /*? if >=1.21.1 {*/
        // In 1.21.1, we manually transform the normal using the matrix since setNormal doesn't accept a matrix.
        float nx = normalMatrix.m00 * x + normalMatrix.m10 * y + normalMatrix.m20 * z;
        float ny = normalMatrix.m01 * x + normalMatrix.m11 * y + normalMatrix.m21 * z;
        float nz = normalMatrix.m02 * x + normalMatrix.m12 * y + normalMatrix.m22 * z;
        currentChain.setNormal(nx, ny, nz);
        /*? } else {*/
         /*delegate.normal(normalMatrix, x, y, z);
        *//*? }*/
        return this;
    }

    public void endVertex() {
        /*? if <1.21.1 {*/
         /*delegate.endVertex();
        *//*? }*/
    }
}