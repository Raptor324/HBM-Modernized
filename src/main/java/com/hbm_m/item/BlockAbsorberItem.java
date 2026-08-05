package com.hbm_m.item;

import com.hbm_m.block.generic.BlockAbsorber;
import com.hbm_m.block.generic.BlockAbsorber.EnumAbsorberTier;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockItem для {@link BlockAbsorber} — локализованное имя по уровню (meta в 1.7.10).
 */
public class BlockAbsorberItem extends BlockItem {

    public BlockAbsorberItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    public static ItemStack forTier(Block block, EnumAbsorberTier tier) {
        ItemStack stack = new ItemStack(block);
        CompoundTag blockStateTag = new CompoundTag();
        blockStateTag.putString("tier", tier.getSerializedName());
        PlatformHooks.put(stack, "BlockStateTag", blockStateTag);
        return stack;
    }

    public static EnumAbsorberTier readTier(ItemStack stack) {
        CompoundTag blockStateTag = stack.getTagElement("BlockStateTag");
        if (blockStateTag != null && blockStateTag.contains("tier")) {
            String name = blockStateTag.getString("tier");
            for (EnumAbsorberTier tier : EnumAbsorberTier.values()) {
                if (tier.getSerializedName().equals(name)) {
                    return tier;
                }
            }
        }
        return EnumAbsorberTier.BASE;
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        if (getBlock() instanceof BlockAbsorber) {
            return "block.hbm_m.rad_absorber." + readTier(stack).getSerializedName();
        }
        return super.getDescriptionId(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack));
    }

    @Override
    public BlockState getPlacementState(net.minecraft.world.item.context.BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state != null && getBlock() instanceof BlockAbsorber) {
            return state.setValue(BlockAbsorber.TIER, readTier(context.getItemInHand()));
        }
        return state;
    }
}
