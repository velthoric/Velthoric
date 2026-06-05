/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.event.api;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Event fired when the client renders the 2D heads-up display (HUD) overlay.
 * This can be used to render custom user interface components.
 *
 * @author xI-Mx-Ix
 */
public class VxClientRenderHudEvent {

    /**
     * The single global event instance.
     */
    public static final Event<Listener> EVENT = EventFactory.createLoop();

    /**
     * The GuiGraphics instance used to draw 2D screen elements.
     */
    private final GuiGraphics guiGraphics;

    /**
     * The partial ticks interpolation factor.
     */
    private final float partialTick;

    /**
     * Constructs a new VxClientRenderHudEvent instance.
     *
     * @param guiGraphics the HUD graphics context
     * @param partialTick the partial ticks value
     */
    public VxClientRenderHudEvent(GuiGraphics guiGraphics, float partialTick) {
        this.guiGraphics = guiGraphics;
        this.partialTick = partialTick;
    }

    /**
     * Gets the GuiGraphics instance for custom GUI drawing.
     *
     * @return the HUD graphics context
     */
    public GuiGraphics getGuiGraphics() {
        return guiGraphics;
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
     * Listener interface for handling VxClientRenderHudEvent.
     */
    @FunctionalInterface
    public interface Listener {
        /**
         * Called when the 2D HUD is rendered.
         *
         * @param event the HUD render event details
         */
        void onRenderHud(VxClientRenderHudEvent event);
    }
}