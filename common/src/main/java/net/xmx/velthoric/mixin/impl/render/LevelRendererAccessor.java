package net.xmx.velthoric.mixin.impl.render;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {
    @Accessor("levelRenderState")
    LevelRenderState velthoric$getLevelRenderState();

    @Accessor("submitNodeStorage")
    SubmitNodeStorage velthoric$getSubmitNodeStorage();
}
