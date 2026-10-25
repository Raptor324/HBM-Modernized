package com.hbm_m.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.hbm_m.entity.mob.ai.EntityAIFireGun;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.gasmask.IGasMask;
import com.hbm_m.mixin.MobAccessor;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1:1 {@code com.hbm.util.MobUtil} - die Slot-Pools fuer Logik-/Dungeonbloecke ("gob block", Aktionsbloecke) und
 * {@link #assignItemsToEntity}. Die Russ-Pools fuer den natuerlichen Spawn liegen bereits in {@code MobGearHandler}.
 * Slots wie im Original: 0 Hand, 1 Stiefel, 2 Hose, 3 Brust, 4 Helm. Gegenstaende werden per Registry-ID aufgeloest
 * (beim ersten Zugriff), fehlende IDs fallen aus dem Pool.
 */
public final class MobUtil {

    private MobUtil() {}

    /** {@code WeightedRandomObject}: Gegenstand (null = nichts) mit Gewicht. */
    public record WeightedEntry(ItemStack stack, int weight) { }

    public static Map<Integer, List<WeightedEntry>> slotPoolCommon = new LinkedHashMap<>();
    public static Map<Integer, List<WeightedEntry>> slotPoolRanged = new LinkedHashMap<>();

    public static Map<Integer, List<WeightedEntry>> slotPoolAdv = new LinkedHashMap<>();
    public static Map<Integer, List<WeightedEntry>> slotPoolAdvRanged = new LinkedHashMap<>();

    // "Slop pools"
    public static Map<Integer, List<WeightedEntry>> slotPoolGunsTier1 = new LinkedHashMap<>();
    public static Map<Integer, List<WeightedEntry>> slotPoolGunsTier2 = new LinkedHashMap<>();
    public static Map<Integer, List<WeightedEntry>> slotPoolGunsTier3 = new LinkedHashMap<>();
    public static Map<Integer, List<WeightedEntry>> slotPoolMasks = new LinkedHashMap<>();
    public static Map<Integer, List<WeightedEntry>> slotPoolHelms = new LinkedHashMap<>();
    public static Map<Integer, List<WeightedEntry>> slotPoolTierArmor = new LinkedHashMap<>();
    public static Map<Integer, List<WeightedEntry>> slotPoolMelee = new LinkedHashMap<>();

    private static boolean initialized = false;

    /** Original {@code intializeMobPools} (nur der Teil fuer Logik-/Dungeonbloecke), lazy beim ersten Zugriff. */
    public static synchronized void intializeMobPools() {
        if (initialized) return;
        initialized = true;

        //gob block
        slotPoolCommon.put(4, createSlotPool(0, new Object[][] {
                {"gas_mask_m65", 16}, {"gas_mask_olde", 12}, {"mask_of_infamy", 8},
                {"gas_mask_mono", 8}, {"robes_helmet", 32}, {"no9", 16},
                {"cobalt_helmet", 2}, {"rag_piss", 1}, {"hat", 1}, {"alloy_helmet", 2},
                {"titanium_helmet", 4}, {"steel_helmet", 8}
        }));
        slotPoolCommon.put(3, createSlotPool(10, new Object[][] {
                {"starmetal_plate", 1}, {"cobalt_plate", 2}, {"robes_plate", 32},
                {"jackt", 32}, {"jackt2", 32}, {"alloy_plate", 2},
                {"steel_plate", 2}
        }));
        slotPoolCommon.put(2, createSlotPool(20, new Object[][] {
                {"zirconium_legs", 1}, {"cobalt_legs", 2}, {"steel_legs", 16},
                {"titanium_legs", 8}, {"robes_legs", 32}, {"alloy_legs", 2}
        }));
        slotPoolCommon.put(1, createSlotPool(10, new Object[][] {
                {"robes_boots", 32}, {"steel_boots", 16}, {"cobalt_boots", 2}, {"alloy_boots", 2}
        }));
        slotPoolCommon.put(0, createSlotPool(1000, new Object[][] {
                {"pipe_lead", 30}, {"crowbar", 25}, {"geiger_counter", 20},
                {"reer_graar", 16}, {"steel_pickaxe", 12}, {"stopsign", 10},
                {"sopsign", 8}, {"chernobylsign", 6}, {"steel_sword", 15},
                {"titanium_sword", 8}, {"lead_gavel", 4}, {"wrench_flipped", 2},
                {"wrench", 20}
        }));

        slotPoolRanged.put(4, createSlotPool(0, new Object[][] {
                {"gas_mask_m65", 16}, {"gas_mask_olde", 12}, {"mask_of_infamy", 8},
                {"gas_mask_mono", 8}, {"robes_helmet", 32}, {"no9", 16},
                {"rag_piss", 1}, {"goggles", 1}, {"alloy_helmet", 2},
                {"titanium_helmet", 4}, {"steel_helmet", 8}
        }));
        slotPoolRanged.put(3, createSlotPool(10, new Object[][] {
                {"starmetal_plate", 1}, {"cobalt_plate", 2}, {"alloy_plate", 2},
                {"steel_plate", 8}, {"titanium_plate", 4}
        }));
        slotPoolRanged.put(2, createSlotPool(10, new Object[][] {
                {"zirconium_legs", 1}, {"cobalt_legs", 2}, {"steel_legs", 16},
                {"titanium_legs", 8}, {"robes_legs", 32}, {"alloy_legs", 2},
        }));
        slotPoolRanged.put(1, createSlotPool(10, new Object[][] {
                {"robes_boots", 32}, {"steel_boots", 16}, {"cobalt_boots", 2}, {"alloy_boots", 2},
                {"titanium_boots", 6}
        }));

        slotPoolAdv.put(4, createSlotPool(new Object[][] {
                {"security_helmet", 10}, {"t51_helmet", 4}, {"asbestos_helmet", 12},
                {"liquidator_helmet", 4}, {"no9", 12},
                {"hazmat_helmet", 6}
        }));
        slotPoolAdv.put(3, createSlotPool(new Object[][] {
                {"liquidator_plate", 4}, {"security_plate", 8}, {"asbestos_plate", 12},
                {"t51_plate", 4}, {"hazmat_plate", 6},
                {"steel_plate", 8}
        }));
        slotPoolAdv.put(2, createSlotPool(new Object[][] {
                {"liquidator_legs", 4}, {"security_legs", 8}, {"asbestos_legs", 12},
                {"t51_legs", 4}, {"hazmat_legs", 6},
                {"steel_legs", 8}
        }));
        slotPoolAdv.put(1, createSlotPool(new Object[][] {
                {"liquidator_boots", 4}, {"security_boots", 8}, {"asbestos_boots", 12},
                {"t51_boots", 4}, {"hazmat_boots", 6},
                {"robes_boots", 8}
        }));
        slotPoolAdv.put(0, createSlotPool(500, new Object[][] {
                {"pipe_lead", 20}, {"crowbar", 10}, {"geiger_counter", 10},
                {"reer_graar", 20}, {"wrench_flipped", 20}, {"stopsign", 16},
                {"sopsign", 4}, {"chernobylsign", 16},
                {"titanium_sword", 18}, {"lead_gavel", 8},
                {"wrench", 20}
        }));

        // Fuer Aktionsbloecke
        slotPoolGunsTier1.put(0, createSlotPool(0, new Object[][] {
                {"gun_light_revolver", 16}, {"gun_greasegun", 8}, {"gun_maresleg", 2}, {"gun_flaregun", 1}
        }));

        slotPoolGunsTier2.put(0, createSlotPool(0, new Object[][] {
                {"gun_uzi", 12}, {"gun_maresleg", 8}, {"gun_henry", 12}, {"gun_heavy_revolver", 8}, {"gun_flaregun", 4}, {"gun_star_f", 8}
        }));

        slotPoolGunsTier3.put(0, createSlotPool(0, new Object[][] {
                {"gun_g3", 25}, {"gun_spas12", 20}, {"gun_carbine", 15}, {"gun_star_f", 20}, {"gun_am180", 6}, {"gun_amat", 5}
        }));

        slotPoolAdvRanged.putAll(slotPoolAdv);
        slotPoolAdvRanged.remove(0);
    }

    private static Item item(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("hbm_m", id));
        return item == Items.AIR ? null : item;
    }

    public static List<WeightedEntry> createSlotPool(int nullWeight, Object[][] items) {
        List<WeightedEntry> pool = new ArrayList<>();
        pool.add(new WeightedEntry(null, nullWeight));
        addAll(pool, items);
        return pool;
    }

    public static List<WeightedEntry> createSlotPool(Object[][] items) {
        List<WeightedEntry> pool = new ArrayList<>();
        addAll(pool, items);
        return pool;
    }

    private static void addAll(List<WeightedEntry> pool, Object[][] items) {
        for (Object[] entry : items) {
            Object obj = entry[0];
            int weight = (int) entry[1];

            if (obj instanceof String id) {
                Item item = item(id);
                if (item != null) pool.add(new WeightedEntry(new ItemStack(item), weight));
            } else if (obj instanceof Item item) {
                pool.add(new WeightedEntry(new ItemStack(item), weight));
            } else if (obj instanceof ItemStack stack) {
                pool.add(new WeightedEntry(stack, weight));
            }
        }
    }

    private static EquipmentSlot slot(int slot) {
        switch (slot) {
            case 1: return EquipmentSlot.FEET;
            case 2: return EquipmentSlot.LEGS;
            case 3: return EquipmentSlot.CHEST;
            case 4: return EquipmentSlot.HEAD;
            default: return EquipmentSlot.MAINHAND;
        }
    }

    /** {@code WeightedRandom.getRandomItem}. */
    private static WeightedEntry pick(Random rand, List<WeightedEntry> pool) {
        int total = 0;
        for (WeightedEntry e : pool) total += e.weight();
        if (total <= 0) return null;
        int roll = rand.nextInt(total);
        for (WeightedEntry e : pool) {
            roll -= e.weight();
            if (roll < 0) return e;
        }
        return null;
    }

    public static void assignItemsToEntity(LivingEntity entity, Map<Integer, List<WeightedEntry>> slotPools, Random rand) {
        intializeMobPools();

        for (Map.Entry<Integer, List<WeightedEntry>> entry : slotPools.entrySet()) {
            int slot = entry.getKey();
            List<WeightedEntry> pool = entry.getValue();

            WeightedEntry choice = pick(rand, pool);
            if (choice == null) {
                continue;
            }

            if (choice.stack() == null || choice.stack().isEmpty()) {
                continue;
            }
            ItemStack stack = choice.stack().copy();

            if (stack.getItem() == ModItems.GAS_MASK_M65.get()
                    || stack.getItem() == ModItems.GAS_MASK_OLDE.get()
                    || stack.getItem() == ModItems.GAS_MASK_MONO.get()) {
                if (stack.getItem() instanceof IGasMask) IGasMask.installFilter(stack, ModItems.GAS_MASK_FILTER.get());
            }

            entity.setItemSlot(slot(slot), stack);

            // Skelette mit Waffe bekommen die Feuer-KI
            if (slot == 0 && entity instanceof Skeleton skeleton && pool == slotPools.get(0)) {
                addFireTask(skeleton);
            }
        }
    }

    /** Die Aufgaben stapeln sich sonst - nur einmal anhaengen. */
    public static void addFireTask(Mob entity) {
        addFireTask(entity, new EntityAIFireGun(entity));
    }

    public static void addFireTask(Mob entity, EntityAIFireGun gunTask) {
        entity.setDropChance(EquipmentSlot.MAINHAND, 0F); // Waffen nicht fallen lassen

        GoalSelector tasks = ((MobAccessor) entity).hbm_m$getGoalSelector();
        for (WrappedGoal task : tasks.getAvailableGoals()) {
            if (task.getGoal() instanceof EntityAIFireGun) return;
        }

        tasks.addGoal(3, gunTask);
    }
}
