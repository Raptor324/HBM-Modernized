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

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.entity.mob.ai.EntityAIFireGun;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.inventory.fluid.trait.PollutionType;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.mixin.MobAccessor;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

/**
 * Экипировка мобов при спавне. Порт {@code ModEventHandler.decorateMob} + {@code MobUtil}
 * (1.7.10): пер-слотовые взвешенные пулы с большим весом «ничего» — поэтому мобы чаще
 * всего спавнятся с одним предметом (например, только противогазом), а не фулл-сетом.
 * Противогазам сразу вкручивается базовый фильтр (как в {@code MobUtil.assignItemsToEntity}).
 *
 * <p>Отличия от оригинала: пулы сокращены до предметов, которые в порту являются
 * носимыми ArmorItem (robes/no9/mask_of_infamy/hat/goggles/jackt в порту — обычные
 * предметы); сажа-гейт полного хазмата опущен. Скелеты при саже получают SEDNA-оружие
 * ({@code getSkelegun}) и ИИ {@link EntityAIFireGun}.</p>
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
            new Entry(ModItems.STARMETAL_PLATE.get(), 1),
            new Entry(ModItems.COBALT_PLATE.get(), 2),
            new Entry(ModItems.ALLOY_PLATE.get(), 2),
            new Entry(ModItems.STEEL_PLATE.get(), 2)));

    private static final SlotPool SKELETON_CHEST = new SlotPool(7000, List.of(
            new Entry(ModItems.STARMETAL_PLATE.get(), 1),
            new Entry(ModItems.COBALT_PLATE.get(), 2),
            new Entry(ModItems.ALLOY_PLATE.get(), 2),
            new Entry(ModItems.STEEL_PLATE.get(), 8),
            new Entry(ModItems.TITANIUM_PLATE.get(), 4)));

    private static final SlotPool ZOMBIE_LEGS = new SlotPool(7000, List.of(
            new Entry(ModItems.ZIRCONIUM_LEGS.get(), 1),
            new Entry(ModItems.COBALT_LEGS.get(), 2),
            new Entry(ModItems.STEEL_LEGS.get(), 16),
            new Entry(ModItems.TITANIUM_LEGS.get(), 8),
            new Entry(ModItems.ALLOY_LEGS.get(), 2)));

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
            new Entry(ModItems.WEAPON_PIPE_LEAD.get(), 30),
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

        /** {@code MobUtil.slotPoolGuns} (Russ-Stufen 0.3 / 1 / 3 / 5). */
        private static final List<Entry> GUNS_03 = List.of(
                new Entry(gun("gun_light_revolver"), 16), new Entry(gun("gun_greasegun"), 8), new Entry(gun("gun_maresleg"), 2));
        private static final List<Entry> GUNS_1 = List.of(
                new Entry(gun("gun_light_revolver"), 6), new Entry(gun("gun_greasegun"), 8), new Entry(gun("gun_maresleg"), 4), new Entry(gun("gun_henry"), 6));
        private static final List<Entry> GUNS_3 = List.of(
                new Entry(gun("gun_uzi"), 10), new Entry(gun("gun_maresleg"), 8), new Entry(gun("gun_henry"), 12), new Entry(gun("gun_heavy_revolver"), 4), new Entry(gun("gun_flaregun"), 2));
        private static final List<Entry> GUNS_5 = List.of(
                new Entry(gun("gun_am180"), 6), new Entry(gun("gun_uzi"), 10), new Entry(gun("gun_spas12"), 8), new Entry(gun("gun_henry_lincoln"), 2), new Entry(gun("gun_heavy_revolver"), 12), new Entry(gun("gun_flaregun"), 4), new Entry(gun("gun_flamer"), 2));

        private static Item gun(String name) { return WeaponItems.gun(name); }
    }

    /** Original {@code ModEventHandler.getSkelegun}: Waffe statt Bogen je nach Russbelastung, sonst null. */
    private static Item getSkelegun(float soot, RandomSource rand) {
        if (!com.hbm_m.config.MobConfig.enableMobWeapons()) return null;

        soot -= com.hbm_m.config.MobConfig.mobWeaponSootReduction();
        if (rand.nextDouble() > Math.log(soot) * 0.25) return null;

        List<Entry> pool = new ArrayList<>();
        int nullWeight = 0;

        if (soot < 0.3) {
            pool.add(new Entry(WeaponItems.gun("gun_pepperbox"), 5));
            nullWeight = 20;
        } else if (soot > 0.3 && soot < 1) {
            pool.addAll(Pools.GUNS_03);
        } else if (soot < 3) {
            pool.addAll(Pools.GUNS_1);
        } else if (soot < 5) {
            pool.addAll(Pools.GUNS_3);
        } else {
            pool.addAll(Pools.GUNS_5);
        }

        int total = nullWeight;
        for (Entry e : pool) total += e.weight();
        int roll = rand.nextInt(total);
        if (roll < nullWeight) return null;
        roll -= nullWeight;
        for (Entry e : pool) {
            roll -= e.weight();
            if (roll < 0) return e.item();
        }
        return null;
    }

    private static final String SKELEGUN_TAG = "hbm_skelegun";

    /** Original {@code MobUtil.addFireTask}: Waffen fallen nicht, die Feuer-KI wird nur einmal angehaengt. */
    public static void addFireTask(Mob entity) {
        entity.setDropChance(EquipmentSlot.MAINHAND, 0F); // Prevent dropping guns

        GoalSelector tasks = ((MobAccessor) entity).hbm_m$getGoalSelector();
        for (WrappedGoal task : tasks.getAvailableGoals()) {
            if (task.getGoal() instanceof EntityAIFireGun) return;
        }

        tasks.addGoal(3, new EntityAIFireGun(entity));
    }

    public static void init() {
        EntityEvent.LIVING_CHECK_SPAWN.register((entity, level, x, y, z, type, spawner) -> {
            // Original: if(!MobConfig.enableMobGear || entity.isChild() || world.isRemote) return;
            if (level.isClientSide() || !com.hbm_m.config.MobConfig.enableMobGear || entity.isBaby()) return EventResult.pass();

            if (entity instanceof Zombie zombie) {
                equipZombie(zombie);
            } else if (entity instanceof Skeleton skeleton) {
                equipSkeleton(skeleton);
            }

            return EventResult.pass();
        });

        // Original ModEventHandler.addAITasks (EntityJoinWorldEvent): wer eine SEDNA-Waffe haelt, bekommt die Feuer-KI
        EntityEvent.ADD.register((entity, level) -> {
            if (level.isClientSide() || !(entity instanceof Mob living)) return EventResult.pass();
            if (living.getPersistentData().contains(SKELEGUN_TAG)) {
                Item gun = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new net.minecraft.resources.ResourceLocation(living.getPersistentData().getString(SKELEGUN_TAG)));
                living.getPersistentData().remove(SKELEGUN_TAG);
                if (gun != net.minecraft.world.item.Items.AIR) living.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(gun));
            }
            if (living.getMainHandItem().getItem() instanceof ItemGunBaseNT) addFireTask(living);
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
        // Original slotPoolRangedS[0] = createSlotPool(50, {getSkelegun(soot)}): 1 zu 50 eine Waffe statt des Bogens.
        float soot = PollutionHandler.getPollution(skeleton.level(), skeleton.getBlockX(), skeleton.getBlockY(), skeleton.getBlockZ(), PollutionType.SOOT);
        Item bowReplacement = getSkelegun(soot, skeleton.getRandom());
        if (bowReplacement != null && skeleton.getRandom().nextInt(51) >= 50) {
            // Vanilla finalizeSpawn legt danach den Bogen in die Hand -> Waffe erst beim Weltbeitritt setzen
            skeleton.getPersistentData().putString(SKELEGUN_TAG, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(bowReplacement).toString());
        }
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
