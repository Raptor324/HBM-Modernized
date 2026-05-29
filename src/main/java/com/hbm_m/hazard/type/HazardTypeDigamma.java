package com.hbm_m.hazard.type;

import java.util.List;

import com.hbm_m.hazard.modifier.HazardModifier;
import com.hbm_m.util.ContaminationUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HazardTypeDigamma extends HazardTypeBase {

    @Override
    public void onUpdate(LivingEntity target, float level, ItemStack stack) {
        ContaminationUtil.applyDigammaData(target, level / 20F);
    }

    @Override
    public void updateEntity(ItemEntity item, float level) {
    }

    @Override
    public void addHazardInformation(Player player, List<Component> list, float level, ItemStack stack,
            List<HazardModifier> modifiers) {
        level = HazardModifier.evalAllModifiers(stack, player, level, modifiers);

        float display = floorDigammaDisplay(level);
        addBracketedTrait(list, "trait.digamma", ChatFormatting.RED);
        list.add(Component.translatable("hazard.hbm_m.digamma.format", display).withStyle(ChatFormatting.DARK_RED));

        if (stack.getCount() > 1) {
            float stackLevel = floorDigammaDisplay(level * stack.getCount());
            list.add(Component.translatable("hazard.hbm_m.digamma.stack", stackLevel).withStyle(ChatFormatting.DARK_RED));
        }
    }
}
