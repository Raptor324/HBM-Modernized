package com.hbm_m.item.special;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.ModItems;
import com.hbm_m.main.Polaroid;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 1:1 {@code ItemAMSCore}: Kerne des AMS (Leistung/Hitze/Brennstoff-Basis) mit ihren Beschreibungen. */
public class ItemAMSCore extends Item implements ITooltipProvider {

    public final long powerBase;
    public final int heatBase;
    public final int fuelBase;

    public ItemAMSCore(long powerBase, int heatBase, int fuelBase, Properties properties) {
        super(properties.stacksTo(1));
        this.powerBase = powerBase;
        this.heatBase = heatBase;
        this.fuelBase = fuelBase;
        //? if >= 1.21.1 {
        /*com.hbm_m.platform.ItemComponentHooks.deferRarity(this, this::hbmRarity);
        *///?}
    }

    public static long getPowerBase(ItemStack stack) {
        return stack.getItem() instanceof ItemAMSCore core ? core.powerBase : 0;
    }

    public static int getHeatBase(ItemStack stack) {
        return stack.getItem() instanceof ItemAMSCore core ? core.heatBase : 0;
    }

    public static int getFuelBase(ItemStack stack) {
        return stack.getItem() instanceof ItemAMSCore core ? core.fuelBase : 0;
    }

    private static void lines(List<Component> list, String... l) {
        for (String s : l) list.add(Component.literal(s));
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        if (this == ModItems.AMS_CORE_SING.get()) {
            lines(list, "A modified undefined state of spacetime", "used to aid in inter-gluon fusion and",
                    "spacetime annihilation. Yes, this destroys", "the universe itself, slowly but steadily,",
                    "but at least you can power your toaster with", "this, so it's all good.");
        }
        if (this == ModItems.AMS_CORE_WORMHOLE.get()) {
            lines(list, "A cloud of billions of nano-wormholes which", "deliberately fail at tunneling matter from",
                    "another dimension, rather it converts all", "that matter into pure energy. That means",
                    "you're actively contributing to the destruction", "of another dimension, sucking it dry like a",
                    "juicebox.", "That dimension probably sucked, anyways. I", "bet it was full of wasps or some crap, man,",
                    "I hate these things.");
        }
        if (this == ModItems.AMS_CORE_EYEOFHARMONY.get()) {
            lines(list, "A star collapsing in on itself, mere nanoseconds", "away from being turned into a black hole,",
                    "frozen in time. If I didn't know better I", "would say this is some deep space magic",
                    "bullcrap some guy made up to sound intellectual.", "Probably Steve from accounting. You still owe me",
                    "ten bucks.");
        }
        if (this == ModItems.AMS_CORE_THINGY.get()) {
            if (Polaroid.id() == 11) {
                lines(list, "Yeah I'm not even gonna question that one.");
            } else {
                lines(list, "...", "...", "...am I even holding this right?", "It's a small metal thing. I dunno where it's from",
                        "or what it does, maybe they found it on a", "junkyard and sold it as some kind of antique",
                        "artifact. If it weren't for the fact that I can", "actually stuff this into some great big laser",
                        "reactor thing, I'd probably bring it back to where", "it belongs. In the trash.");
            }
        }
    }

    //? if < 1.21.1 {
    @Override
    public Rarity getRarity(ItemStack stack) {
    //?} else {
    /*private Rarity hbmRarity() {
    *///?}
        return this == ModItems.AMS_CORE_THINGY.get() ? Rarity.EPIC : Rarity.UNCOMMON;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return this == ModItems.AMS_CORE_THINGY.get() && Polaroid.id() == 11;
    }
}
