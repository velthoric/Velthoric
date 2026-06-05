/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.api;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;

/**
 * Event fired during the 3D level rendering process on the client side.
 * This event triggers specifically after rendering entities, allowing custom world-space rendering.
 *
 * @author xI-Mx-Ix
 */
public class VxRenderEvent {

    /**
     * The single global event instance.
     */
    public static final Event<Listener> EVENT = EventFactory.createLoop();

    /**
     * The LevelRenderer instance responsible for rendering the world.
     */
    private final LevelRenderer levelRenderer;

    /**
     * The PoseStack instance containing current rendering transformations.
     */
    private final PoseStack poseStack;

    /**
     * The partial ticks interpolation factor.
     */
    private final float partialTick;

    /**
     * The current projection matrix.
     */
    private final Matrix4f projectionMatrix;

    /**
     * The light texture currently bound and used for rendering.
     */
    private final LightTexture lightTexture;

    /**
     * Constructs a new VxRenderEvent instance.
     *
     * @param levelRenderer    the level renderer
     * @param poseStack        the current pose stack
     * @param partialTick      the partial ticks interpolation factor
     * @param lightTexture     the active light texture
     * @param projectionMatrix the projection matrix
     */
    public VxRenderEvent(LevelRenderer levelRenderer, PoseStack poseStack, float partialTick, LightTexture lightTexture, Matrix4f projectionMatrix) {
        this.levelRenderer = levelRenderer;
        this.poseStack = poseStack;
        this.partialTick = partialTick;
        this.lightTexture = lightTexture;
        this.projectionMatrix = projectionMatrix;
    }

    /**
     * Gets the LevelRenderer instance.
     *
     * @return the level renderer
     */
    public LevelRenderer getLevelRenderer() {
        return levelRenderer;
    }

    /**
     * Gets the current PoseStack.
     *
     * @return the pose stack
     */
    public PoseStack getPoseStack() {
        return poseStack;
    }

    /**
     * Gets the partial tick interpolation factor.
     *
     * @return the partial tick
     */
    public float getPartialTick() {
        return partialTick;
    }

    /**
     * Gets the projection matrix.
     *
     * @return the projection matrix
     */
    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    /**
     * Gets the light texture.
     *
     * @return the light texture
     */
    public LightTexture getLightTexture() {
        return lightTexture;
    }

    /**
     * Listener interface for handling VxRenderEvent.
     */
    @FunctionalInterface
    public interface Listener {
        /**
         * Called when the level rendering process reaches the post-entity stage.
         *
         * @param event the render event details
         */
        void onRenderLevel(VxRenderEvent event);
    }
}