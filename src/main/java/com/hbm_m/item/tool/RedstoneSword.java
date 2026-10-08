package com.hbm_m.item.tool;

import com.hbm_m.platform.ItemHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** 1:1 {@code com.hbm.items.tool.RedstoneSword}: legt beim Rechtsklick Redstone aus (14 Haltbarkeit). */
public class RedstoneSword extends SwordItem {

    //Pridenauer you damn bastard.

    public RedstoneSword(Tier material) {
        //? if < 1.21.1 {
        super(material, 4, -2.4F, new Properties());
        //?} else {
        /*super(material, new Properties().attributes(SwordItem.createAttributes(material, 4, -2.4F)));
        *///?}
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        Player player = context.getPlayer();
        ItemStack itemStack = context.getItemInHand();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());

        if (player != null && !player.mayUseItemAt(pos, context.getClickedFace(), itemStack)) {
            return InteractionResult.FAIL;
        } else {
            if (world.isEmptyBlock(pos)) {
                world.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, world.random.nextFloat() * 0.4F + 0.8F);
                if (!world.isClientSide) world.setBlockAndUpdate(pos, Blocks.REDSTONE_WIRE.defaultBlockState());
            }

            if (player != null) ItemHooks.hurtAndBreak(itemStack, 14, player, EquipmentSlot.MAINHAND);
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
    }

    /** BEWLR fuer die 1.7-IItemRenderer (ItemRenderGavel/Shim/RedstoneSword); greift nur bei builtin/entity-Itemmodellen. */
    //? if forge {
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.weapon.GunItemRenderer.INSTANCE;
            }
        });
    }
    //?} elif neoforge {
    /*@Override
    public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.weapon.GunItemRenderer.INSTANCE;
            }
        });
    }
    *///?}
}
