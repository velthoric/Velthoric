/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.item.tool.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.xmx.velthoric.item.tool.VxToolMode;
import net.xmx.velthoric.item.tool.config.VxToolConfig;
import net.xmx.velthoric.item.tool.config.VxToolProperty;
import net.xmx.velthoric.item.tool.packet.VxToolConfigPacket;
import net.xmx.velthoric.network.VxNetworking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The configuration screen for the currently held tool.
 * <p>
 * This screen dynamically generates UI widgets based on the property types defined
 * in the tool's configuration. It handles input validation for numeric types and
 * synchronizes changes to the server upon closing.
 *
 * @author xI-Mx-Ix
 */
public class VxToolConfigScreen extends Screen {

    private final VxToolMode mode;
    private final Item item;
    private final VxToolConfig config;

    /**
     * Stores the string representation of values to be sent to the server.
     */
    private final Map<String, String> edits = new HashMap<>();
    private final Set<String> invalidFields = new HashSet<>();
    private Button saveButton;

    public VxToolConfigScreen(Item item, VxToolMode mode) {
        super(Component.literal("Tool Configuration"));
        this.item = item;
        this.mode = mode;
        // Retrieve the configuration structure associated with the client player
        this.config = mode.getConfig(Minecraft.getInstance().player.getUUID());
        // Load persistent values from local storage
        this.config.load(BuiltInRegistries.ITEM.getKey(item).getPath());
    }

    @Override
    protected void init() {
        int y = 40;
        int centerX = this.width / 2;

        for (Map.Entry<String, VxToolProperty<?>> entry : config.getProperties().entrySet()) {
            String key = entry.getKey();
            VxToolProperty<?> prop = entry.getValue();
            Class<?> type = prop.getType();

            // Display the property name as a non-interactive button (acting as a label)
            this.addRenderableWidget(Button.builder(Component.literal(key), b -> {
            }).bounds(centerX - 150, y, 100, 20).build()).active = false;

            // Generate specific widgets based on the property type
            if (type == Boolean.class) {
                boolean currentVal = (Boolean) prop.getValue();

                // Toggle button for boolean values
                Button boolBtn = Button.builder(Component.literal(String.valueOf(currentVal)), b -> {
                    boolean newVal = !Boolean.parseBoolean(b.getMessage().getString());
                    b.setMessage(Component.literal(String.valueOf(newVal)));
                    edits.put(key, String.valueOf(newVal));
                }).bounds(centerX - 40, y, 100, 20).build();

                this.addRenderableWidget(boolBtn);
                edits.put(key, String.valueOf(currentVal)); // Initialize edit map

            } else {
                Number min = prop.getMin();
                Number max = prop.getMax();
                boolean isBounded = min != null && max != null;

                EditBox box = new EditBox(this.font, centerX - 40, y, 100, 20, Component.literal(key)) {
                    @Override
                    public void setFocused(boolean focused) {
                        super.setFocused(focused);
                        if (!focused && isBounded) {
                            String current = getValue();
                            String clamped = clampStringValue(prop, current);
                            if (!clamped.equals(current)) {
                                setValue(clamped);
                            }
                        }
                    }
                };
                box.setMaxLength(256);
                String initialVal = String.valueOf(prop.getValue());
                box.setValue(initialVal);

                if (isBounded) {
                    box.setTooltip(Tooltip.create(Component.literal("Min: " + min + " | Max: " + max)));
                }

                if (type == Integer.class || type == Long.class) {
                    box.setFilter(s -> s.isEmpty() || s.matches("-?\\d*"));
                } else if (type == Float.class || type == Double.class) {
                    box.setFilter(s -> s.isEmpty() || s.matches("-?\\d*(\\.\\d*)?"));
                }

                box.setResponder(val -> {
                    if (isBounded && !isValid(prop, val)) {
                        box.setTextColor(0xFF5555);
                        box.setTooltip(Tooltip.create(Component.literal("Out of bounds! [" + min + " - " + max + "]")));
                        invalidFields.add(key);
                    } else {
                        box.setTextColor(0xFFFFFF);
                        if (isBounded) {
                            box.setTooltip(Tooltip.create(Component.literal("Min: " + min + " | Max: " + max)));
                        }
                        invalidFields.remove(key);
                        edits.put(key, val);
                    }
                    updateSaveButton();
                });

                this.addRenderableWidget(box);
                edits.put(key, initialVal);
            }

            y += 25;
        }

        // Reset to Defaults button (Left)
        this.addRenderableWidget(Button.builder(Component.literal("Reset to Defaults"), b -> {
            this.config.resetToDefaults();
            this.edits.clear();
            this.invalidFields.clear();
            this.init(this.minecraft, this.width, this.height);
        }).bounds(centerX - 105, this.height - 30, 100, 20).build());

        // Save & Close button (Right)
        this.saveButton = this.addRenderableWidget(Button.builder(Component.literal("Save & Close"), b -> {
            this.onClose();
        }).bounds(centerX + 5, this.height - 30, 100, 20).build());

        updateSaveButton();
    }

    /**
     * Updates the active state of the Save button based on input validity.
     */
    private void updateSaveButton() {
        if (this.saveButton != null) {
            this.saveButton.active = this.invalidFields.isEmpty();
        }
    }

    /**
     * Clamps a raw string input to the defined property bounds.
     *
     * @param prop  The property configuration.
     * @param value The string value to clamp.
     * @return The clamped value as a string.
     */
    private String clampStringValue(VxToolProperty<?> prop, String value) {
        if (value == null || value.isEmpty()) {
            return String.valueOf(prop.getValue());
        }
        Class<?> type = prop.getType();
        Number min = prop.getMin();
        Number max = prop.getMax();
        if (min == null || max == null) {
            return value;
        }

        try {
            if (type == Integer.class) {
                int v = Integer.parseInt(value);
                int clamped = Math.max(min.intValue(), Math.min(max.intValue(), v));
                return String.valueOf(clamped);
            } else if (type == Long.class) {
                long v = Long.parseLong(value);
                long clamped = Math.max(min.longValue(), Math.min(max.longValue(), v));
                return String.valueOf(clamped);
            } else if (type == Float.class) {
                float v = Float.parseFloat(value);
                if (Float.isNaN(v)) return String.valueOf(min.floatValue());
                float clamped = Math.max(min.floatValue(), Math.min(max.floatValue(), v));
                return String.valueOf(clamped);
            } else if (type == Double.class) {
                double v = Double.parseDouble(value);
                if (Double.isNaN(v)) return String.valueOf(min.doubleValue());
                double clamped = Math.max(min.doubleValue(), Math.min(max.doubleValue(), v));
                return String.valueOf(clamped);
            }
        } catch (NumberFormatException e) {
            return String.valueOf(prop.getValue());
        }

        return value;
    }

    /**
     * Validates whether the given string value satisfies property bounds.
     *
     * @param prop  The property configuration.
     * @param value The raw string input value.
     * @return True if valid within defined bounds, false otherwise.
     */
    private boolean isValid(VxToolProperty<?> prop, String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        Class<?> type = prop.getType();
        Number min = prop.getMin();
        Number max = prop.getMax();
        try {
            if (type == Integer.class) {
                int v = Integer.parseInt(value);
                return min == null || max == null || (v >= min.intValue() && v <= max.intValue());
            } else if (type == Long.class) {
                long v = Long.parseLong(value);
                return min == null || max == null || (v >= min.longValue() && v <= max.longValue());
            } else if (type == Float.class) {
                float v = Float.parseFloat(value);
                return !Float.isNaN(v) && (min == null || max == null || (v >= min.floatValue() && v <= max.floatValue()));
            } else if (type == Double.class) {
                double v = Double.parseDouble(value);
                return !Double.isNaN(v) && (min == null || max == null || (v >= min.doubleValue() && v <= max.doubleValue()));
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        /*? if >=1.21.1 {*/
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        /*? } else {*/
         /*this.renderBackground(guiGraphics);
        *//*? }*/
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        int y = 40;
        int centerX = this.width / 2;
        for (VxToolProperty<?> prop : config.getProperties().values()) {
            if (prop.getMin() != null && prop.getMax() != null && prop.getType() != Boolean.class) {
                String rangeText = "[" + prop.getMin() + " - " + prop.getMax() + "]";
                guiGraphics.drawString(this.font, rangeText, centerX + 65, y + 6, 0x888888, false);
            }
            y += 25;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void removed() {
        for (Map.Entry<String, VxToolProperty<?>> entry : config.getProperties().entrySet()) {
            String key = entry.getKey();
            VxToolProperty<?> prop = entry.getValue();
            if (prop.getMin() != null && prop.getMax() != null && prop.getType() != Boolean.class) {
                String val = edits.get(key);
                if (val != null) {
                    edits.put(key, clampStringValue(prop, val));
                }
            }
        }
        // Apply current UI edits to the local configuration object
        this.config.applyEdits(this.edits);
        // Save the configuration to client's local storage
        this.config.save(BuiltInRegistries.ITEM.getKey(item).getPath());
        // Transmit the accumulated edits to the server when the screen closes
        VxNetworking.sendToServer(new VxToolConfigPacket(Item.getId(item), edits));
    }
}