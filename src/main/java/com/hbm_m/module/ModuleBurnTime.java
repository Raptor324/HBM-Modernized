package com.hbm_m.module;

import com.hbm_m.item.ModItems;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/**
 * Порт {@code com.hbm.module.ModuleBurnTime} (1.7.10): категоризация твёрдого топлива
 * и множители времени горения / тепловыделения на категорию.
 *
 * <p>Оригинал определяет категорию по ore-dict именам ("Coal", "Coke", "Lignite",
 * "log*", "Wood") и идентичности предметов (solid_fuel, rocket_fuel, solid_fuel_bf).
 * В порте ore-dict заменён на предметные проверки + ванильные теги:
 * кокс — {@code COKE_PETROLEUM}/тег {@code forge:coke}, уголь — {@code ItemTags.COALS},
 * лигнит — предметы LIGNITE/LIGNITE_POWDER, брёвна — {@code ItemTags.LOGS},
 * дерево — ростения/доски.
 */
public class ModuleBurnTime {

    /** Категории топлива — порядок и смысл 1:1 с оригиналом (modLog..modBalefire). */
    public enum FuelCategory {
        LOG, WOOD, COAL, LIGNITE, COKE, SOLID, ROCKET, BALEFIRE, NONE
    }

    private final double[] modTime = new double[8];
    private final double[] modHeat = new double[8];

    /** Значения по умолчанию, как в оригинале — все 1.0. */
    public ModuleBurnTime() {
        for (int i = 0; i < 8; i++) {
            this.modTime[i] = 1.0D;
            this.modHeat[i] = 1.0D;
        }
    }

    public ModuleBurnTime setLogTimeMod(double mod) { this.modTime[0] = mod; return this; }
    public ModuleBurnTime setWoodTimeMod(double mod) { this.modTime[1] = mod; return this; }
    public ModuleBurnTime setCoalTimeMod(double mod) { this.modTime[2] = mod; return this; }
    public ModuleBurnTime setLigniteTimeMod(double mod) { this.modTime[3] = mod; return this; }
    public ModuleBurnTime setCokeTimeMod(double mod) { this.modTime[4] = mod; return this; }
    public ModuleBurnTime setSolidTimeMod(double mod) { this.modTime[5] = mod; return this; }
    public ModuleBurnTime setRocketTimeMod(double mod) { this.modTime[6] = mod; return this; }
    public ModuleBurnTime setBalefireTimeMod(double mod) { this.modTime[7] = mod; return this; }

    public ModuleBurnTime setLogHeatMod(double mod) { this.modHeat[0] = mod; return this; }
    public ModuleBurnTime setWoodHeatMod(double mod) { this.modHeat[1] = mod; return this; }
    public ModuleBurnTime setCoalHeatMod(double mod) { this.modHeat[2] = mod; return this; }
    public ModuleBurnTime setLigniteHeatMod(double mod) { this.modHeat[3] = mod; return this; }
    public ModuleBurnTime setCokeHeatMod(double mod) { this.modHeat[4] = mod; return this; }
    public ModuleBurnTime setSolidHeatMod(double mod) { this.modHeat[5] = mod; return this; }
    public ModuleBurnTime setRocketHeatMod(double mod) { this.modHeat[6] = mod; return this; }
    public ModuleBurnTime setBalefireHeatMod(double mod) { this.modHeat[7] = mod; return this; }

    public double getTimeMod(FuelCategory cat) { return modTime[cat.ordinal()]; }
    public double getHeatMod(FuelCategory cat) { return modHeat[cat.ordinal()]; }

    /**
     * Базовое время горения (тики) из печной карты топлива / переопределений предмета.
     * Порт {@code FuelHandler.getBurnTimeFromCache} без кеша ( lookup дешёвый).
     */
    public static int getBaseBurnTime(ItemStack stack) {
        return PlatformHooks.getFuelBurnTime(stack);
    }

    /** Категория топлива для предмета. Порт {@code ModuleBurnTime.getMod}. */
    public static FuelCategory getCategory(ItemStack stack) {
        if (stack.isEmpty()) return FuelCategory.NONE;

        Item item = stack.getItem();
        // Идентичность предмета — как в оригинале (solid_fuel / rocket_fuel / *_bf)
        if (item == ModItems.SOLID_FUEL.get() || item == ModItems.SOLID_FUEL_PRESTO.get()) return FuelCategory.SOLID;
        if (item == ModItems.SOLID_FUEL_BF.get() || item == ModItems.SOLID_FUEL_PRESTO_BF.get()) return FuelCategory.BALEFIRE;
        if (item == ModItems.ROCKET_FUEL.get()) return FuelCategory.ROCKET;
        if (item == ModItems.COKE_PETROLEUM.get()) return FuelCategory.COKE;
        if (item == ModItems.LIGNITE.get() || item == ModItems.LIGNITE_POWDER.get()) return FuelCategory.LIGNITE;

        // Примечание: в оригинале кокс определялся по ore-dict "Coke"; в порте
        // коксовый предмет пока один (coke_petroleum) — см. проверку выше.

        if (stack.is(ItemTags.COALS)) return FuelCategory.COAL;
        if (stack.is(ItemTags.LOGS)) return FuelCategory.LOG;
        if (stack.is(ItemTags.SAPLINGS) || stack.is(ItemTags.PLANKS)) return FuelCategory.WOOD;

        return FuelCategory.NONE;
    }

    /** Время горения с множителем категории. Порт {@code getBurnTime(stack, def)}. */
    public int getBurnTime(ItemStack stack) {
        int fuel = getBaseBurnTime(stack);
        if (fuel == 0) return 0;
        return (int) (fuel * getModTime(stack));
    }

    /** Тепловыделение с множителем категории. Порт {@code getBurnHeat(base, stack)}. */
    public int getBurnHeat(int base, ItemStack stack) {
        if (base <= 0) return 0;
        return (int) (base * getModHeat(stack));
    }

    public double getModTime(ItemStack stack) {
        FuelCategory cat = getCategory(stack);
        return cat == FuelCategory.NONE ? 1.0D : modTime[cat.ordinal()];
    }

    public double getModHeat(ItemStack stack) {
        FuelCategory cat = getCategory(stack);
        return cat == FuelCategory.NONE ? 1.0D : modHeat[cat.ordinal()];
    }

    /**
     * Подсказка для тултипа GUI — перечисляет только неединичные модификаторы.
     * Порт {@code getDesc()}.
     */
    public java.util.List<net.minecraft.network.chat.Component> getDesc() {
        java.util.List<net.minecraft.network.chat.Component> list = new java.util.ArrayList<>();
        String[] names = {
                "log", "wood", "coal", "lignite", "coke", "solid_fuel", "rocket_fuel", "balefire"
        };
        for (int i = 0; i < 8; i++) {
            if (modTime[i] != 1.0D || modHeat[i] != 1.0D) {
                list.add(net.minecraft.network.chat.Component.translatable(
                        "fuel.hbm_m." + names[i],
                        Math.round(modTime[i] * 100.0D),
                        Math.round(modHeat[i] * 100.0D)));
            }
        }
        return list;
    }
}
