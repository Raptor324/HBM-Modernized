package com.hbm_m.item.tool;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.item.ITooltipProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * 1:1 {@code com.hbm.items.tool.ItemMS} ({@code mysteryshovel}, "Brittle Spade"): auf {@code ntm_dirt} angewendet
 * gibt er die drei Bruchstuecke von {@code ingot_u238m2} frei. Block und Bruchstuecke werden ueber ihre Registry-IDs
 * gesucht und wirken, sobald sie portiert sind.
 */
public class ItemMS extends Item implements ITooltipProvider {

    public ItemMS(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Lost but not forgotten"));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("hbm_m", id));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();

        if (!world.isClientSide) {
            Block dirt = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("hbm_m", "ntm_dirt"));

            if (dirt != Blocks.AIR && world.getBlockState(pos).is(dirt)) {
                world.destroyBlock(pos, false);

                List<ItemStack> list = new ArrayList<>();
                for (int i = 1; i <= 3; i++) {
                    Item piece = item("ingot_u238m2_" + i);
                    if (piece != Items.AIR) list.add(new ItemStack(piece));
                }

                RandomSource rand = world.random;
                for (ItemStack sta : list) {
                    float f = rand.nextFloat() * 0.8F + 0.1F;
                    float f1 = rand.nextFloat() * 0.8F + 0.1F;
                    float f2 = rand.nextFloat() * 0.8F + 0.1F;
                    ItemEntity entityitem = new ItemEntity(world, pos.getX() + f, pos.getY() + f1, pos.getZ() + f2, sta);
                    float f3 = 0.05F;
                    entityitem.setDeltaMovement(rand.nextGaussian() * f3, rand.nextGaussian() * f3 + 0.2F, rand.nextGaussian() * f3);
                    world.addFreshEntity(entityitem);
                }
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }
}
