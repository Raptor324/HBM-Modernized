package com.hbm_m.item.tool;

import com.hbm_m.api.block.IToolable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code ItemDefuser} ({@code defuser}): {@code ItemTooling(ToolType.DEFUSER, 100)}; Rechtsklick auf einen Creeper
 * entschaerft ihn ({@code ItemModDefuser.castrateCreeper}). Der Zweig fuer {@code EntityGlyphidNuclear} folgt mit den
 * Glyphiden (Entity-Runde).
 */
public class ItemDefuser extends ItemTooling {

    public ItemDefuser(int durability, Properties properties) {
        super(IToolable.ToolType.DEFUSER, durability, properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (entity instanceof Creeper creeper) {
            return com.hbm_m.armormod.item.ItemModDefuser.castrateCreeper(creeper, player, true)
                    ? InteractionResult.sidedSuccess(player.level().isClientSide) : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }
}
