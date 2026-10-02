package com.hbm_m.powerarmor;

import java.util.EnumSet;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.handler.HazardClass;
import com.hbm_m.item.gasmask.GasMaskUtil;
import com.hbm_m.item.gasmask.IGasMask;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 1:1 {@code com.hbm.items.armor.ArmorLiquidatorMask}: Liquidatorhaube mit Filteraufnahme. */
public class ArmorLiquidatorMask extends ArmorLiquidator implements IGasMask {

    public ArmorLiquidatorMask(ModArmorMaterials material, Type type, Properties properties, String texture) {
        super(material, type, properties, texture);
    }

    @Override
    public EnumSet<HazardClass> getBlacklist() {
        return EnumSet.noneOf(HazardClass.class); // full hood has no restrictions
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            ItemStack stack = player.getItemInHand(hand);
            ItemStack filter = GasMaskUtil.takeFilter(stack);
            if (!filter.isEmpty()) {
                if (!level.isClientSide) {
                    if (!player.getInventory().add(filter)) {
                        player.drop(filter, true);
                    }
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        }
        return super.use(level, player, hand);
    }

    /** Original: nur der Masken-Tooltip, ohne FSB-Block. */
    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        GasMaskUtil.addGasMaskTooltip(stack, list, getBlacklist());
    }
}
