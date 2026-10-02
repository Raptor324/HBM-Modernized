package com.hbm_m.module;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.handler.FuelHandler;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.module.ModuleBurnTime}: Brennzeit- und Hitzeboni je Brennstoffart. Das Original erkennt die Art
 * an Item und Oredict-Namen (Coke, Coal, Lignite, log*, *Wood*); im Port an Item und Tags bzw. Registriernamen.
 */
public class ModuleBurnTime {

    private static final int modLog = 0;
    private static final int modWood = 1;
    private static final int modCoal = 2;
    private static final int modLignite = 3;
    private static final int modCoke = 4;
    private static final int modSolid = 5;
    private static final int modRocket = 6;
    private static final int modBalefire = 7;

    private final double[] modTime = new double[8];
    private final double[] modHeat = new double[8];

    public ModuleBurnTime() {
        for (int i = 0; i < modTime.length; i++) {
            modTime[i] = 1.0D;
            modHeat[i] = 1.0D;
        }
    }

    public int getBurnTime(ItemStack stack, double def) {
        int fuel = FuelHandler.getBurnTimeFromCache(stack);
        if (fuel == 0) return 0;
        return (int) (fuel * getMod(stack, modTime, def));
    }

    public int getBurnTime(ItemStack stack) {
        return getBurnTime(stack, 1D);
    }

    public int getBurnHeat(int base, ItemStack stack, double def) {
        if (base <= 0) return 0;
        return (int) (base * getMod(stack, modHeat));
    }

    public int getBurnHeat(int base, ItemStack stack) {
        return getBurnHeat(base, stack, 1D);
    }

    public double getMod(ItemStack stack, double[] mod, double def) {
        if (stack == null || stack.isEmpty()) return 0;

        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();

        if (id.equals("solid_fuel") || id.equals("solid_fuel_presto") || id.equals("solid_fuel_presto_triplet")) return mod[modSolid];
        if (id.equals("solid_fuel_bf") || id.equals("solid_fuel_presto_bf") || id.equals("solid_fuel_presto_triplet_bf")) return mod[modBalefire];
        if (id.equals("rocket_fuel")) return mod[modRocket];

        // Oredict-Namen des Originals ~ Registriername + Tags
        List<String> names = new ArrayList<>();
        names.add(id);
        stack.getTags().forEach(t -> names.add(t.location().getPath()));

        for (String name : names) {
            if (name.contains("coke")) return mod[modCoke];
            if (name.contains("coal") && !name.contains("charcoal")) return mod[modCoal];
            if (name.contains("lignite")) return mod[modLignite];
            if (name.startsWith("logs") || name.endsWith("_log") || name.endsWith("_wood") && !name.startsWith("stripped")) return mod[modLog];
            if (name.contains("planks") || name.contains("wooden") || name.equals("stick") || name.equals("rods/wooden")) return mod[modWood];
        }

        return def;
    }

    public double getMod(ItemStack stack, double[] mod) {
        return getMod(stack, mod, 1D);
    }

    public List<Component> getDesc() {
        List<Component> desc = new ArrayList<>();
        desc.addAll(getTimeDesc());
        desc.addAll(getHeatDesc());
        return desc;
    }

    public List<Component> getTimeDesc() {
        List<Component> list = new ArrayList<>();
        list.add(Component.literal("Burn time bonuses:").withStyle(ChatFormatting.GOLD));
        addIf(list, "Logs", modTime[modLog]);
        addIf(list, "Wood", modTime[modWood]);
        addIf(list, "Coal", modTime[modCoal]);
        addIf(list, "Lignite", modTime[modLignite]);
        addIf(list, "Coke", modTime[modCoke]);
        addIf(list, "Solid Fuel", modTime[modSolid]);
        addIf(list, "Rocket Fuel", modTime[modRocket]);
        addIf(list, "Balefire", modTime[modBalefire]);
        if (list.size() == 1) list.clear();
        return list;
    }

    public List<Component> getHeatDesc() {
        List<Component> list = new ArrayList<>();
        list.add(Component.literal("Burn heat bonuses:").withStyle(ChatFormatting.RED));
        addIf(list, "Logs", modHeat[modLog]);
        addIf(list, "Wood", modHeat[modWood]);
        addIf(list, "Coal", modHeat[modCoal]);
        addIf(list, "Lignite", modHeat[modLignite]);
        addIf(list, "Coke", modHeat[modCoke]);
        addIf(list, "Solid Fuel", modHeat[modSolid]);
        addIf(list, "Rocket Fuel", modHeat[modRocket]);
        addIf(list, "Balefire", modHeat[modBalefire]);
        if (list.size() == 1) list.clear();
        return list;
    }

    private void addIf(List<Component> list, String name, double mod) {
        if (mod != 1.0D) list.add(Component.literal("- " + name + ": ").withStyle(ChatFormatting.YELLOW).append(getPercent(mod)));
    }

    private Component getPercent(double mod) {
        mod -= 1D;
        if (mod < 0) return Component.literal("" + (int) (mod * 100) + "%").withStyle(ChatFormatting.RED);
        return Component.literal("+" + (int) (mod * 100) + "%").withStyle(ChatFormatting.GREEN);
    }

    public double[] getModHeat() { return modHeat; }
    public double[] getModTime() { return modTime; }

    public ModuleBurnTime setLogTimeMod(double mod) { this.modTime[modLog] = mod; return this; }
    public ModuleBurnTime setWoodTimeMod(double mod) { this.modTime[modWood] = mod; return this; }
    public ModuleBurnTime setCoalTimeMod(double mod) { this.modTime[modCoal] = mod; return this; }
    public ModuleBurnTime setLigniteTimeMod(double mod) { this.modTime[modLignite] = mod; return this; }
    public ModuleBurnTime setCokeTimeMod(double mod) { this.modTime[modCoke] = mod; return this; }
    public ModuleBurnTime setSolidTimeMod(double mod) { this.modTime[modSolid] = mod; return this; }
    public ModuleBurnTime setRocketTimeMod(double mod) { this.modTime[modRocket] = mod; return this; }
    public ModuleBurnTime setBalefireTimeMod(double mod) { this.modTime[modBalefire] = mod; return this; }

    public ModuleBurnTime setLogHeatMod(double mod) { this.modHeat[modLog] = mod; return this; }
    public ModuleBurnTime setWoodHeatMod(double mod) { this.modHeat[modWood] = mod; return this; }
    public ModuleBurnTime setCoalHeatMod(double mod) { this.modHeat[modCoal] = mod; return this; }
    public ModuleBurnTime setLigniteHeatMod(double mod) { this.modHeat[modLignite] = mod; return this; }
    public ModuleBurnTime setCokeHeatMod(double mod) { this.modHeat[modCoke] = mod; return this; }
    public ModuleBurnTime setSolidHeatMod(double mod) { this.modHeat[modSolid] = mod; return this; }
    public ModuleBurnTime setRocketHeatMod(double mod) { this.modHeat[modRocket] = mod; return this; }
    public ModuleBurnTime setBalefireHeatMod(double mod) { this.modHeat[modBalefire] = mod; return this; }
}
