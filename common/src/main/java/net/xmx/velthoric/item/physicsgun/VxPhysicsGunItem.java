/*
 * This file is part of Velthoric.
 * Licensed under LGPL 3.0.
 */
package net.xmx.velthoric.item.physicsgun;

import net.minecraft.world.InteractionHand;
//? if >=26.1 {
/*import net.minecraft.world.InteractionResult;
*///? } else {
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
 //? }
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

/**
 * The Physics Gun item.
 *
 * @author xI-Mx-Ix
 */
public class VxPhysicsGunItem extends Item {

    public VxPhysicsGunItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
            //? if >=26.1 {
    /*public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        return InteractionResult.PASS;
    }
    *///? } else {
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        return InteractionResultHolder.pass(pPlayer.getItemInHand(pUsedHand));
    }
    //? }
}
