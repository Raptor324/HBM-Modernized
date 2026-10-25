package com.hbm_m.worldgen;

import java.util.List;
import java.util.function.Supplier;

import com.hbm_m.item.ModItems;

import dev.architectury.event.events.common.LootEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * 1:1 {@code MainRegistry}: {@code ChestGenHooks.addItem(...)} - NTM-Beute in Vanilla-Kisten.
 *
 * <p>In 1.7.10 zieht eine Kiste {@code ChestGenHooks.getCount} Mal aus der gemeinsamen Gewichtsliste. Der Port haengt je
 * Kiste einen eigenen Pool mit derselben Ziehungszahl an; die Vanilla-Eintraege des Originals stehen als leerer Eintrag mit
 * ihrer Gesamtgewichtung (1.7.10-Vanilla + Forge-Zauberbuch) darin. So ist die Wahrscheinlichkeit je Ziehung fuer jedes
 * NTM-Teil genau Gewicht / Gesamtgewicht wie im Original.</p>
 */
public final class ChestGenHooksPort {

    private ChestGenHooksPort() { }

    /** Kennung fuer audit11_worldgen.py. */
    public static final String CHEST_INJECT = "hbm_m:chestgenhooks";

    private record Entry(Supplier<Item> item, int min, int max, int weight) { }

    /**
     * 1.7.10-Kiste: Ziel-Lootliste, Ziehungen ({@code getCount}: min + nextInt(max - min), max exklusiv),
     * Gewichtssumme der Vanilla/Forge-Eintraege, NTM-Eintraege.
     */
    private record Chest(String table, int countMin, int countMax, int vanillaWeight, List<Entry> entries) { }

    private static final List<Chest> CHESTS = List.of(
            // VILLAGE_BLACKSMITH (3, 9) -> Waffenschmied
            new Chest("minecraft:chests/village/village_weaponsmith", 3, 9, 94, List.of(
                    new Entry(() -> ModItems.ARMOR_POLISH.get(), 1, 1, 3),
                    new Entry(() -> ModItems.BATHWATER.get(), 1, 1, 1))),
            // MINESHAFT_CORRIDOR (3, 7)
            new Chest("minecraft:chests/abandoned_mineshaft", 3, 7, 80, List.of(
                    new Entry(() -> ModItems.BATHWATER.get(), 1, 1, 1),
                    new Entry(() -> ModItems.SERUM.get(), 1, 1, 5),
                    new Entry(() -> ModItems.NO9.get(), 1, 1, 5),
                    new Entry(() -> ModItems.KEY_RED_CRACKED.get(), 1, 1, 5))),
            // DUNGEON_CHEST (8, 8)
            new Chest("minecraft:chests/simple_dungeon", 8, 8, 130, List.of(
                    new Entry(() -> ModItems.HEART_PIECE.get(), 1, 1, 1),
                    new Entry(() -> ModItems.KEY_RED_CRACKED.get(), 1, 1, 5),
                    new Entry(() -> ModItems.SCRUMPY.get(), 1, 1, 1))),
            // PYRAMID_DESERT_CHEST (2, 7)
            new Chest("minecraft:chests/desert_pyramid", 2, 7, 169, List.of(
                    new Entry(() -> ModItems.HEART_PIECE.get(), 1, 1, 1),
                    new Entry(() -> ModItems.SCRUMPY.get(), 1, 1, 1))),
            // PYRAMID_JUNGLE_CHEST (2, 7)
            new Chest("minecraft:chests/jungle_temple", 2, 7, 73, List.of(
                    new Entry(() -> ModItems.HEART_PIECE.get(), 1, 1, 1))),
            // BONUS_CHEST (10, 10)
            new Chest("minecraft:chests/spawn_bonus_chest", 10, 10, 64, List.of(
                    new Entry(() -> ModItems.NO9.get(), 1, 1, 7))));

    public static void init() {
        //? if < 1.21.1 {
        LootEvent.MODIFY_LOOT_TABLE.register((lootDataManager, id, context, builtin) -> inject(id.toString(), context, builtin));
        //?} else {
        /*LootEvent.MODIFY_LOOT_TABLE.register((key, context, builtin) -> inject(key.location().toString(), context, builtin));
        *///?}
    }

    private static void inject(String id, LootEvent.LootTableModificationContext context, boolean builtin) {
        if (!builtin) return;
        for (Chest chest : CHESTS) {
            if (!chest.table.equals(id)) continue;
            // getCount: countMin < countMax ? countMin + nextInt(countMax - countMin) : countMin
            int max = chest.countMin < chest.countMax ? chest.countMax - 1 : chest.countMin;
            LootPool.Builder pool = LootPool.lootPool()
                    .setRolls(chest.countMin == max ? ConstantValue.exactly(max) : UniformGenerator.between(chest.countMin, max))
                    .add(EmptyLootItem.emptyItem().setWeight(chest.vanillaWeight));
            for (Entry e : chest.entries) {
                LootItem.Builder<?> item = LootItem.lootTableItem(e.item.get()).setWeight(e.weight);
                // alle Original-Eintraege 1-1 Stueck
                pool.add(item);
            }
            context.addPool(pool);
        }
    }
}
