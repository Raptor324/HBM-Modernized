package com.hbm_m.hazard.type;

import java.util.List;

import com.hbm_m.config.GeneralConfig;
import com.hbm_m.hazard.modifier.HazardModifier;
import com.hbm_m.util.BobMathUtil;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HazardTypeRadiation extends HazardTypeBase {

    @Override
    public void onUpdate(LivingEntity target, float level, ItemStack stack) {
        level *= stack.getCount();

        if (level > 0) {
            float rad = level / 20F;
            if (GeneralConfig.enable528) {
                rad = (float) (rad / 49F);
            } else {
                rad = (float) BobMathUtil.squirt(rad);
            }
            ContaminationUtil.contaminate(target, HazardType.RADIATION, ContaminationType.CREATIVE, rad);
        }
    }

    @Override
    public void updateEntity(ItemEntity item, float level) {
    }

    @Override
    public void addHazardInformation(Player player, List<Component> list, float level, ItemStack stack,
            List<HazardModifier> modifiers) {
        level = HazardModifier.evalAllModifiers(stack, player, level, modifiers);

        if (level < 1e-5F) {
            return;
        }

        addBracketedTrait(list, "trait.radioactive", ChatFormatting.GREEN);
        float display = floorThousandths(level);
        list.add(Component.translatable("hazard.hbm_m.radiation.format", display).withStyle(ChatFormatting.YELLOW));

        if (stack.getCount() > 1) {
            float stackLevel = floorThousandths(level * stack.getCount());
            list.add(Component.translatable("hazard.hbm_m.radiation.stack", stackLevel).withStyle(ChatFormatting.YELLOW));
        }
    }
}
