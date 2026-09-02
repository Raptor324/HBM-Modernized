package com.hbm_m.explosion;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Кастомная бомба: пересчёт выхода по ингредиентам и выбор типа взрыва.
 * Приоритет: эйфемиум > шрабидиум > водород > ядерный > крупный ТНТ > мелкий ТНТ.
 */
public final class CustomNukeExplosion {

    public record Yields(float tnt, float nuke, float hydro, float schrab, float euph) {}

    private enum Kind { ADD, MULT }

    private record Entry(Kind kind, byte type, float value) {}

    private static final float MAX_TNT = 150F;
    private static final float MAX_NUKE = 200F;
    private static final float MAX_HYDRO = 350F;
    private static final float MAX_SCHRAB = 250F;

    private static final int T_TNT = 0, T_NUKE = 1, T_HYDRO = 2, T_SCHRAB = 3, T_EUPH = 4;

    private static Map<Item, Entry> entries;

    private CustomNukeExplosion() {}

    private static Map<Item, Entry> table() {
        if (entries == null) {
            Map<Item, Entry> map = new HashMap<>();
            // === ТНТ ===
            put(map, Items.GUNPOWDER, Kind.ADD, T_TNT, 0.8F);
            put(map, Items.TNT, Kind.ADD, T_TNT, 4F);
            put(map, ModItems.BALL_TNT.get(), Kind.ADD, T_TNT, 6F);
            put(map, Items.REDSTONE, Kind.MULT, T_TNT, 1.05F);
            put(map, Items.REDSTONE_BLOCK, Kind.MULT, T_TNT, 1.5F);
            // === Ядерные ===
            put(map, ModItems.GADGET_CORE.get(), Kind.ADD, T_NUKE, 30F);
            put(map, ModItems.FAT_MAN_CORE.get(), Kind.ADD, T_NUKE, 30F);
            put(map, ModItems.FAT_MAN_CORE.get(), Kind.ADD, T_NUKE, 25F);
            // === Водород ===
            put(map, ModItems.CELL_DEUTERIUM.get(), Kind.ADD, T_HYDRO, 30F);
            // === Шрабидиум ===
            put(map, ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.BILLET), Kind.ADD, T_SCHRAB, 15F);
            put(map, ModItems.CELL_SAS3.get(), Kind.ADD, T_SCHRAB, 7.5F);
            // === Эйфемиум ===
            put(map, ModMaterialItems.item(ModMaterials.EUPHEMIUM, MaterialShape.PLATE), Kind.ADD, T_EUPH, 25F);
            entries = map;
        }
        return entries;
    }

    private static void put(Map<Item, Entry> map, Item item, Kind kind, int type, float value) {
        map.put(item, new Entry(kind, (byte) type, value));
    }

    /** Пересчёт выходов по всем слотам (значения складываются, множители перемножаются). */
    public static Yields computeYields(Iterable<ItemStack> slots) {
        float tnt = 0, nuke = 0, hydro = 0, schrab = 0, euph = 0;
        float tntMod = 1, nukeMod = 1, hydroMod = 1, schrabMod = 1;

        for (ItemStack stack : slots) {
            if (stack.isEmpty()) continue;
            Entry ent = table().get(stack.getItem());
            if (ent == null) continue;
            float count = stack.getCount();
            switch (ent.type()) {
                case T_TNT -> { if (ent.kind() == Kind.ADD) tnt += ent.value() * count; else tntMod *= ent.value() * count; }
                case T_NUKE -> { if (ent.kind() == Kind.ADD) nuke += ent.value() * count; else nukeMod *= ent.value() * count; }
                case T_HYDRO -> { if (ent.kind() == Kind.ADD) hydro += ent.value() * count; else hydroMod *= ent.value() * count; }
                case T_SCHRAB -> { if (ent.kind() == Kind.ADD) schrab += ent.value() * count; else schrabMod *= ent.value() * count; }
                case T_EUPH -> euph += ent.value() * count;
                default -> {}
            }
        }
        return new Yields(tnt * tntMod, nuke * nukeMod, hydro * hydroMod, schrab * schrabMod, euph);
    }

    /** Запуск подходящего взрыва по вычисленным выходам. Только серверная сторона. */
    public static void explodeCustom(ServerLevel level, double x, double yPos, double z, Yields yields) {
        /// ЭЙФЕМИУМ ///
        if (yields.euph() > 0) {
            PlatformHooks.playSound(level, x, yPos, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 100000.0F, 1.0F);
            FleijaExplosionAPI.start(level, x + 0.5, yPos + 0.5, z + 0.5, 150);
            /// ШРАБИДИУМ ///
        } else if (yields.schrab() > 0) {
            float schrab = Math.min(yields.schrab() + yields.nuke() / 8 + yields.tnt() / 16, MAX_SCHRAB);
            FleijaExplosionAPI.start(level, x + 0.5, yPos + 0.5, z + 0.5, (int) schrab);
            /// ВОДОРОД ///
        } else if (yields.hydro() > 0) {
            float hydro = Math.min(yields.hydro() + yields.nuke() / 2 + yields.tnt() / 4, MAX_HYDRO);
            NuclearExplosionAPI.startLargeNuke(level, x + 0.5, yPos + 0.5, z + 0.5, (int) hydro);
            /// ЯДЕРНЫЙ ///
        } else if (yields.nuke() > 0) {
            float nuke = Math.min(yields.nuke() + yields.tnt() / 2, MAX_NUKE);
            NuclearExplosionAPI.startLargeNuke(level, x + 0.5, yPos + 5, z + 0.5, (int) nuke);
            /// КРУПНЫЙ ТНТ (без радиации) ///
        } else if (yields.tnt() >= 75) {
            NuclearExplosionAPI.startLargeNukeNoRad(level, x + 0.5, yPos + 0.5, z + 0.5,
                    (int) Math.min(yields.tnt(), MAX_TNT));
            /// МЕЛКИЙ ТНТ ///
        } else if (yields.tnt() > 0) {
            level.explode(null, x + 0.5, yPos + 0.5, z + 0.5, yields.tnt(),
                    Level.ExplosionInteraction.TNT);
        }
    }
}
