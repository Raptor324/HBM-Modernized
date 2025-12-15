package com.hbm_m.item.custom.crates;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Item для крейтов с отображением содержимого в тултипе
 */
public class CrateItem extends BlockItem {

    public CrateItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        if (stack.hasTag() && stack.getTag().contains("BlockEntityTag")) {
            CompoundTag beTag = stack.getTag().getCompound("BlockEntityTag");
            if (beTag.contains("inventory")) {
                CompoundTag invTag = beTag.getCompound("inventory");
                if (invTag.contains("Items")) {
                    ListTag items = invTag.getList("Items", 10);

                    if (!items.isEmpty()) {
                        tooltip.add(Component.translatable("container.shulkerBox.itemCount", items.size())
                                .withStyle(net.minecraft.ChatFormatting.GRAY));
                    }
                }
            }
        }
    }
}