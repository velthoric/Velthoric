/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.mixin.impl.entity;

import net.minecraft.world.entity.Entity;
import net.xmx.velthoric.core.entity.interaction.VxEntityAttachment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Mixin to inject the Duck interface fields directly into Entity.
 *
 * @author xI-Mx-Ix
 */
@Mixin(Entity.class)
public abstract class MixinEntityAttachment implements VxEntityAttachment {

    @Unique
    private int velthoric_serverGroundBody = 0;

    @Unique
    private int velthoric_clientGroundBody = 0;

    @Override
    public int velthoric$getServerGroundBody() {
        return this.velthoric_serverGroundBody;
    }

    @Override
    public void velthoric$setServerGroundBody(int value) {
        this.velthoric_serverGroundBody = value;
    }

    @Override
    public int velthoric$getClientGroundBody() {
        return this.velthoric_clientGroundBody;
    }

    @Override
    public void velthoric$setClientGroundBody(int value) {
        this.velthoric_clientGroundBody = value;
    }
}