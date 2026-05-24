/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.mixin.impl.entity.dragging;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor interface for ServerGamePacketListenerImpl to update tracking positions
 * during server-side programmatic player dragging.
 *
 * @author xI-Mx-Ix
 */
@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerImplAccessor {

    @Accessor("firstGoodX")
    double velthoric_getFirstGoodX();

    @Accessor("firstGoodX")
    void velthoric_setFirstGoodX(double x);

    @Accessor("firstGoodY")
    double velthoric_getFirstGoodY();

    @Accessor("firstGoodY")
    void velthoric_setFirstGoodY(double y);

    @Accessor("firstGoodZ")
    double velthoric_getFirstGoodZ();

    @Accessor("firstGoodZ")
    void velthoric_setFirstGoodZ(double z);

    @Accessor("lastGoodX")
    double velthoric_getLastGoodX();

    @Accessor("lastGoodX")
    void velthoric_setLastGoodX(double x);

    @Accessor("lastGoodY")
    double velthoric_getLastGoodY();

    @Accessor("lastGoodY")
    void velthoric_setLastGoodY(double y);

    @Accessor("lastGoodZ")
    double velthoric_getLastGoodZ();

    @Accessor("lastGoodZ")
    void velthoric_setLastGoodZ(double z);
}