package com.hbm_m.item.special;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.main.Polaroid;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.special.ItemPolaroid}: Resistenz III unter 10 Leben, Text und Bild (Textur
 * {@code polaroid_<polaroidID>}, Port: Modell-Eigenschaft {@code hbm_m:polaroid}) nach der ausgewuerfelten Zahl.
 */
public class ItemPolaroid extends Item implements ITooltipProvider {

    public ItemPolaroid(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        if (entity instanceof Player player && player.getHealth() < 10F) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 2));
        }
    }

    @Override
    public void appendHbmTooltip(ItemStack itemstack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Fate chosen"));
        list.add(Component.literal(""));

        switch (Polaroid.id()) {
            case 1 -> list.add(Component.literal("..."));
            case 2 -> list.add(Component.literal("Clear as glass."));
            case 3 -> list.add(Component.literal("'M"));
            case 4 -> list.add(Component.literal("It's about time."));
            case 5 -> list.add(Component.literal("If you stare long into the abyss, the abyss stares back."));
            case 6 -> list.add(Component.literal("public Party celebration = new Party();"));
            case 7 -> list.add(Component.literal("V urnerq lbh yvxr EBG13!"));
            case 8 -> list.add(Component.literal("11011100"));
            case 9 -> list.add(Component.literal("Vg'f nobhg gvzr."));
            case 10 -> list.add(Component.literal("Schrabidium dislikes the breeding reactor."));
            case 11 -> list.add(Component.literal("yss stares back.6public Party cel"));
            case 12 -> list.add(Component.literal("Red streaks."));
            case 13 -> list.add(Component.literal("Q1"));
            case 14 -> list.add(Component.literal("Q4"));
            case 15 -> list.add(Component.literal("Q3"));
            case 16 -> list.add(Component.literal("Q2"));
            case 17 -> list.add(Component.literal("Two friends before christmas."));
            case 18 -> {
                list.add(Component.literal("Duchess of the boxcars."));
                list.add(Component.literal(""));
                list.add(Component.literal("\"P.S.: Thirty-one.\""));
                list.add(Component.literal("\"Huh, what does thirty-one mean?\""));
            }
            default -> { }
        }
    }
}
