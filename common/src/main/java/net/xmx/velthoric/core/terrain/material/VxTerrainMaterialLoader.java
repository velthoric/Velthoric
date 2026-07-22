/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.core.terrain.material;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.xmx.velthoric.init.VxMainClass;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads terrain materials from JSON data packs under "data/<namespace>/velthoric_materials/".
 *
 * @author xI-Mx-Ix
 */
public class VxTerrainMaterialLoader extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> output = new HashMap<>();

        // Keep your resource scanning here.
        // If your mappings expose it, scan data/<namespace>/velthoric_materials/*.json
        // and put each parsed file into `output`.

        return output;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        VxTerrainMaterial.clear();

        int loaded = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                if (!json.has("values")) {
                    continue;
                }

                JsonObject values = json.getAsJsonObject("values");
                for (Map.Entry<String, JsonElement> valueEntry : values.entrySet()) {
                    ResourceLocation blockId = ResourceLocation.tryParse(valueEntry.getKey());
                    if (blockId == null) {
                        continue;
                    }

                    JsonObject props = valueEntry.getValue().getAsJsonObject();

                    float friction = props.has("friction") ? props.get("friction").getAsFloat() : 0.75f;
                    float restitution = props.has("restitution") ? props.get("restitution").getAsFloat() : 0.0f;
                    float weight = props.has("weight") ? props.get("weight").getAsFloat() : 100.0f;
                    boolean isFragile = props.has("fragile") && props.get("fragile").getAsBoolean();

                    Block transformTo = null;
                    if (props.has("transform_to")) {
                        ResourceLocation targetId = ResourceLocation.tryParse(props.get("transform_to").getAsString());
                        if (targetId != null) {
                            transformTo = BuiltInRegistries.BLOCK.get(targetId)
                                    .map(Holder::value)
                                    .filter(block -> block != Blocks.AIR)
                                    .orElse(null);
                        }
                    }

                    boolean spawnsParticles = !props.has("particles") || props.get("particles").getAsBoolean();
                    float breakThreshold = props.has("break_threshold") ? props.get("break_threshold").getAsFloat() : 5000.0f;
                    boolean isInteractable = props.has("interactable") && props.get("interactable").getAsBoolean();
                    float interactThreshold = props.has("interact_threshold") ? props.get("interact_threshold").getAsFloat() : 50.0f;
                    float transformThreshold = props.has("transform_threshold") ? props.get("transform_threshold").getAsFloat() : 2000.0f;

                    VxTerrainMaterial.register(
                            blockId,
                            friction,
                            restitution,
                            weight,
                            isFragile,
                            transformTo,
                            spawnsParticles,
                            breakThreshold,
                            isInteractable,
                            interactThreshold,
                            transformThreshold
                    );
                    loaded++;
                }
            } catch (Exception e) {
                VxMainClass.LOGGER.error("Failed to parse Velthoric material file: {}", entry.getKey(), e);
            }
        }

        if (loaded > 0) {
            VxMainClass.LOGGER.info("Loaded {} custom terrain materials from data packs.", loaded);
        }
    }
}