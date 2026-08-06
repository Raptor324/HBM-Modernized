package com.hbm_m.hazard.type;

import java.util.List;

import com.hbm_m.config.GeneralConfig;
import com.hbm_m.hazard.modifier.HazardModifier;
import com.hbm_m.item.ModItems;
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

    /**
     * 1:1 с {@link com.hbm.hazard.type.HazardTypeRadiation#onUpdate} (1.7.10).
     * <p>Без reacher: {@code rad = level / 20F} (линейно).<br>
     * С reacher: применяется нелинейная нормализация — {@code BobMathUtil.squirt} (обычный режим)
     * или {@code rad / 49F} ({@code 528}-режим, обратно-квадратичный по расстоянию).</p>
     * <p><b>Баг фикс:</b> Modernized применял {@code squirt}/{@code 528}-нормализацию безусловно,
     * что занижало радиацию для всех игроков. Для 1 блока polonium это давало
     * ~122 RAD/s вместо правильных 750 RAD/s.</p>
     */
    @Override
    public void onUpdate(LivingEntity target, float level, ItemStack stack) {
        boolean reacher = false;

        if (target instanceof Player player) {
            reacher = player.getInventory().contains(new ItemStack(ModItems.REACHER.get()));
        }

        level *= stack.getCount();

        if (level > 0) {
            float rad = level / 20F;

            if (GeneralConfig.enable528 && reacher) {
                rad = (float) (rad / 49F);
            } else if (reacher) {
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
