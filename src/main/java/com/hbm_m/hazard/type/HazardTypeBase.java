package com.hbm_m.hazard.type;

import java.util.List;

import com.hbm_m.hazard.modifier.HazardModifier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public abstract class HazardTypeBase {

    public abstract void onUpdate(LivingEntity target, float level, ItemStack stack);

    public abstract void updateEntity(ItemEntity item, float level);

    public abstract void addHazardInformation(Player player, List<Component> list, float level, ItemStack stack,
            List<HazardModifier> modifiers);

    /** {@code [trait key]} — как в оригинале HBM 1.7.10. */
    protected static void addBracketedTrait(List<Component> list, String traitKey, ChatFormatting color) {
        MutableComponent line = Component.literal("[").withStyle(color);
        line.append(Component.translatable(traitKey).withStyle(color));
        line.append(Component.literal("]").withStyle(color));
        list.add(line);
    }

    protected static float floorThousandths(float level) {
        return (float) (Math.floor(level * 1000) / 1000);
    }

    protected static float floorDigammaDisplay(float level) {
        return (float) (Math.floor(level * 10000F) / 10F);
    }
}
