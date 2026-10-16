package com.hbm_m.handler;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.gasmask.IGasMask;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Экипировка мобов при спавне. Порт {@code ModEventHandler.decorateMob} + {@code MobUtil}
 * (1.7.10): пер-слотовые взвешенные пулы с большим весом «ничего» — поэтому мобы чаще
 * всего спавнятся с одним предметом (например, только противогазом), а не фулл-сетом.
 * Противогазам сразу вкручивается базовый фильтр (как в {@code MobUtil.assignItemsToEntity}).
 *
 * <p>Отличия от оригинала: пулы сокращены до предметов, которые в порту являются
 * носимыми ArmorItem (robes/no9/mask_of_infamy/hat/goggles/jackt в порту — обычные
 * предметы); pollution/soot не портирован, поэтому сажа-гейты опущены, а у скелета
 * оставлено ванильное оружие (guns/EntityAIFireGun не перенесены).</p>
 */
public class MobGearHandler {

    private record Entry(Item item, int weight) {
    }

    private record SlotPool(int nullWeight, List<Entry> entries) {
    }

    /**
     * Лениво загружаемый контейнер пулов: статические поля здесь инициализируются при
     * первом обращении из equipSlot (в рантайме, после регистрации предметов) —
     * обращение к ModItems.get() на этапе регистрации падает ("Registry Object not present").
     */
    private static final class Pools {
        private static final SlotPool ZOMBIE_HELMET = new SlotPool(8000, List.of(
            new Entry(ModItems.GAS_MASK_M65.get(), 16),
            new Entry(ModItems.GAS_MASK_OLDE.get(), 12),
            new Entry(ModItems.GAS_MASK_MONO.get(), 8),
            new Entry(ModItems.MASK_PISS.get(), 1),
            new Entry(ModItems.COBALT_HELMET.get(), 2),
            new Entry(ModItems.ALLOY_HELMET.get(), 2),
            new Entry(ModItems.TITANIUM_HELMET.get(), 4),
            new Entry(ModItems.STEEL_HELMET.get(), 8)));

    private static final SlotPool SKELETON_HELMET = new SlotPool(8000, List.of(
            new Entry(ModItems.GAS_MASK_M65.get(), 16),
            new Entry(ModItems.GAS_MASK_OLDE.get(), 12),
            new Entry(ModItems.GAS_MASK_MONO.get(), 8),
            new Entry(ModItems.MASK_PISS.get(), 1),
            new Entry(ModItems.COBALT_HELMET.get(), 2),
            new Entry(ModItems.ALLOY_HELMET.get(), 2),
            new Entry(ModItems.TITANIUM_HELMET.get(), 4),
            new Entry(ModItems.STEEL_HELMET.get(), 8)));

    private static final SlotPool ZOMBIE_CHEST = new SlotPool(7000, List.of(
            new Entry(ModItems.STARMETAL_CHESTPLATE.get(), 1),
            new Entry(ModItems.COBALT_CHESTPLATE.get(), 2),
            new Entry(ModItems.ALLOY_CHESTPLATE.get(), 2),
            new Entry(ModItems.STEEL_CHESTPLATE.get(), 2)));

    private static final SlotPool SKELETON_CHEST = new SlotPool(7000, List.of(
            new Entry(ModItems.STARMETAL_CHESTPLATE.get(), 1),
            new Entry(ModItems.COBALT_CHESTPLATE.get(), 2),
            new Entry(ModItems.ALLOY_CHESTPLATE.get(), 2),
            new Entry(ModItems.STEEL_CHESTPLATE.get(), 8),
            new Entry(ModItems.TITANIUM_CHESTPLATE.get(), 4)));

    private static final SlotPool ZOMBIE_LEGS = new SlotPool(7000, List.of(
            new Entry(ModItems.ZIRCONIUM_LEGS.get(), 1),
            new Entry(ModItems.COBALT_LEGGINGS.get(), 2),
            new Entry(ModItems.STEEL_LEGGINGS.get(), 16),
            new Entry(ModItems.TITANIUM_LEGGINGS.get(), 8),
            new Entry(ModItems.ALLOY_LEGGINGS.get(), 2)));

    private static final SlotPool SKELETON_LEGS = ZOMBIE_LEGS;

    private static final SlotPool ZOMBIE_BOOTS = new SlotPool(7000, List.of(
            new Entry(ModItems.STEEL_BOOTS.get(), 16),
            new Entry(ModItems.COBALT_BOOTS.get(), 2),
            new Entry(ModItems.ALLOY_BOOTS.get(), 2)));

    private static final SlotPool SKELETON_BOOTS = new SlotPool(10000, List.of(
            new Entry(ModItems.STEEL_BOOTS.get(), 16),
            new Entry(ModItems.COBALT_BOOTS.get(), 2),
            new Entry(ModItems.ALLOY_BOOTS.get(), 2),
            new Entry(ModItems.TITANIUM_BOOTS.get(), 6)));

    /** Рукопашный пул зомби (MobUtil.slotPoolCommonS слот 0; reer_graar в порт не перенесён). */
    private static final SlotPool ZOMBIE_HAND = new SlotPool(10000, List.of(
            new Entry(ModItems.PIPE_LEAD.get(), 30),
            new Entry(ModItems.CROWBAR.get(), 25),
            new Entry(ModItems.GEIGER_COUNTER.get(), 20),
            new Entry(ModItems.STEEL_PICKAXE.get(), 12),
            new Entry(ModItems.STOPSIGN.get(), 10),
            new Entry(ModItems.SOPSIGN.get(), 8),
            new Entry(ModItems.CHERNOBYLSIGN.get(), 6),
            new Entry(ModItems.STEEL_SWORD.get(), 15),
            new Entry(ModItems.TITANIUM_SWORD.get(), 8),
            new Entry(ModItems.LEAD_GAVEL.get(), 4),
            new Entry(ModItems.WRENCH_FLIPPED.get(), 2),
                    new Entry(ModItems.WRENCH.get(), 20)));
    }

    public static void init() {
        EntityEvent.LIVING_CHECK_SPAWN.register((entity, level, x, y, z, type, spawner) -> {
            if (level.isClientSide()) return EventResult.pass();

            if (entity instanceof Zombie zombie) {
                equipZombie(zombie);
            } else if (entity instanceof Skeleton skeleton) {
                equipSkeleton(skeleton);
            }

            return EventResult.pass();
        });
    }

    private static void equipZombie(Zombie zombie) {
        equipSlot(zombie, EquipmentSlot.HEAD, Pools.ZOMBIE_HELMET);
        equipSlot(zombie, EquipmentSlot.CHEST, Pools.ZOMBIE_CHEST);
        equipSlot(zombie, EquipmentSlot.LEGS, Pools.ZOMBIE_LEGS);
        equipSlot(zombie, EquipmentSlot.FEET, Pools.ZOMBIE_BOOTS);
        equipSlot(zombie, EquipmentSlot.MAINHAND, Pools.ZOMBIE_HAND);
    }

    private static void equipSkeleton(Skeleton skeleton) {
        // Оружие — ванильный лук (guns не портированы), броня — ranged-пул оригинала.
        equipSlot(skeleton, EquipmentSlot.HEAD, Pools.SKELETON_HELMET);
        equipSlot(skeleton, EquipmentSlot.CHEST, Pools.SKELETON_CHEST);
        equipSlot(skeleton, EquipmentSlot.LEGS, Pools.SKELETON_LEGS);
        equipSlot(skeleton, EquipmentSlot.FEET, Pools.SKELETON_BOOTS);
    }

    /** Разыгрывает слот: с весом nullWeight остаётся пустым (как WeightedRandom в оригинале). */
    private static void equipSlot(Mob mob, EquipmentSlot slot, SlotPool pool) {
        RandomSource random = mob.getRandom();

        int total = pool.nullWeight();
        for (Entry e : pool.entries()) {
            total += e.weight();
        }

        if (random.nextInt(total) < pool.nullWeight()) {
            return; // слот остаётся пустым
        }
        int roll = random.nextInt(total - pool.nullWeight());
        for (Entry e : pool.entries()) {
            roll -= e.weight();
            if (roll < 0) {
                ItemStack stack = new ItemStack(e.item());
                // Противогазы спавнятся с уже вкрученным фильтром (MobUtil.assignItemsToEntity).
                if (e.item() instanceof IGasMask && !IGasMask.hasFilter(stack)) {
                    IGasMask.installFilter(stack, ModItems.GAS_MASK_FILTER.get());
                }
                mob.setItemSlot(slot, stack);
                mob.setDropChance(slot, 0.085F); // ванильный шанс дропа экипировки
                return;
            }
        }
    }
}
