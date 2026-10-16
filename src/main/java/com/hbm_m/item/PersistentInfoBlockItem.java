package com.hbm_m.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.IPersistentInfoProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Обычный BlockItem для блока, чей дропнутый стек несёт {@code persistent}-тег, который нужно
 * показать в тултипе ("1234/16000mB <жидкость>") — порт {@code IPersistentInfoProvider.addInformation}
 * из 1.7.10 (BlockFluidBarrel). Логика тултипа та же, что в {@code MultiblockBlockItem}, но без
 * требования IMultiblockController.
 */
public class PersistentInfoBlockItem extends BlockItem implements com.hbm_m.item.ITooltipProvider {

    public PersistentInfoBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (!(getBlock() instanceof IPersistentInfoProvider provider)) return;
        CompoundTag tag = com.hbm_m.platform.PlatformHooks.getItemTag(stack);
        if (tag == null || !tag.contains(com.hbm_m.blockentity.IPersistentNBT.NBT_PERSISTENT_KEY)) return;
        provider.addInformation(stack, tag.getCompound(com.hbm_m.blockentity.IPersistentNBT.NBT_PERSISTENT_KEY), level, tooltip, flag);
    }
}
