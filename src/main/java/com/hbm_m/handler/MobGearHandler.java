package com.hbm_m.handler;

import com.hbm_m.item.ModItems;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;

public class MobGearHandler {

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
        if (zombie.getRandom().nextFloat() >= 0.07f) return; // 7% шанс экипировки

        // Выбираем случайный сет брони
        EquipmentSet armorSet = chooseZombieArmor(zombie);
        armorSet.apply(zombie);

        // Выбираем случайное оружие
        ItemStack weapon = chooseZombieWeapon(zombie);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, weapon);

        // Устанавливаем шанс дропа (как у ванильных)
        setDropChances(zombie, 0.05f);
    }

    private static EquipmentSet chooseZombieArmor(Zombie zombie) {
        float roll = zombie.getRandom().nextFloat();

        if (roll < 0.25f) {
            return new EquipmentSet(
                    ModItems.STEEL_HELMET.get(),
                    ModItems.STEEL_CHESTPLATE.get(),
                    ModItems.STEEL_LEGGINGS.get(),
                    ModItems.STEEL_BOOTS.get()
            );
        }
        if (roll < 0.45f) {
            return new EquipmentSet(
                    ModItems.ALLOY_HELMET.get(),
                    ModItems.ALLOY_CHESTPLATE.get(),
                    ModItems.ALLOY_LEGGINGS.get(),
                    ModItems.ALLOY_BOOTS.get()
            );
        }
        if (roll < 0.65f) {
            return new EquipmentSet(
                    ModItems.COBALT_HELMET.get(),
                    ModItems.COBALT_CHESTPLATE.get(),
                    ModItems.COBALT_LEGGINGS.get(),
                    ModItems.COBALT_BOOTS.get()
            );
        }
        if (roll < 0.85f) {
            return new EquipmentSet(
                    ModItems.TITANIUM_HELMET.get(),
                    ModItems.TITANIUM_CHESTPLATE.get(),
                    ModItems.TITANIUM_LEGGINGS.get(),
                    ModItems.TITANIUM_BOOTS.get()
            );
        }
        if (roll < 0.92f) {
            return new EquipmentSet(
                    ModItems.HAZMAT_HELMET.get(),
                    ModItems.HAZMAT_CHESTPLATE.get(),
                    ModItems.HAZMAT_LEGGINGS.get(),
                    ModItems.HAZMAT_BOOTS.get()
            );
        }
        return new EquipmentSet(
                ModItems.SECURITY_HELMET.get(),
                ModItems.SECURITY_CHESTPLATE.get(),
                ModItems.SECURITY_LEGGINGS.get(),
                ModItems.SECURITY_BOOTS.get()
        );
    }

    private static ItemStack chooseZombieWeapon(Zombie zombie) {
        float roll = zombie.getRandom().nextFloat();

        if (roll < 0.15f) return new ItemStack(ModItems.ALLOY_SWORD.get());
        if (roll < 0.30f) return new ItemStack(ModItems.ALLOY_PICKAXE.get());
        if (roll < 0.45f) return new ItemStack(ModItems.ALLOY_SHOVEL.get());
        if (roll < 0.60f) return new ItemStack(ModItems.TITANIUM_SWORD.get());
        if (roll < 0.75f) return new ItemStack(ModItems.TITANIUM_PICKAXE.get());
        if (roll < 0.85f) return new ItemStack(ModItems.TITANIUM_SHOVEL.get());
        if (roll < 0.90f) return new ItemStack(ModItems.STEEL_SWORD.get());
        if (roll < 0.95f) return new ItemStack(ModItems.STEEL_PICKAXE.get());
        return new ItemStack(ModItems.STEEL_SHOVEL.get());
    }

    // ══════════════════════════ Экипировка скелета ════════════════════════════

    private static void equipSkeleton(Skeleton skeleton) {
        if (skeleton.getRandom().nextFloat() >= 0.07f) return;

        EquipmentSet armorSet = chooseSkeletonArmor(skeleton);
        armorSet.apply(skeleton);

        setDropChances(skeleton, 0.1f);
    }

    private static EquipmentSet chooseSkeletonArmor(Skeleton skeleton) {
        float roll = skeleton.getRandom().nextFloat();

        if (roll < 0.25f) {
            return new EquipmentSet(
                    ModItems.STEEL_HELMET.get(),
                    ModItems.STEEL_CHESTPLATE.get(),
                    ModItems.STEEL_LEGGINGS.get(),
                    ModItems.STEEL_BOOTS.get()
            );
        }
        if (roll < 0.50f) {
            return new EquipmentSet(
                    ModItems.TITANIUM_HELMET.get(),
                    ModItems.TITANIUM_CHESTPLATE.get(),
                    ModItems.TITANIUM_LEGGINGS.get(),
                    ModItems.TITANIUM_BOOTS.get()
            );
        }
        if (roll < 0.75f) {
            return new EquipmentSet(
                    ModItems.COBALT_HELMET.get(),
                    ModItems.COBALT_CHESTPLATE.get(),
                    ModItems.COBALT_LEGGINGS.get(),
                    ModItems.COBALT_BOOTS.get()
            );
        }
        return new EquipmentSet(
                ModItems.ALLOY_HELMET.get(),
                ModItems.ALLOY_CHESTPLATE.get(),
                ModItems.ALLOY_LEGGINGS.get(),
                ModItems.ALLOY_BOOTS.get()
        );
    }

    // ══════════════════════════ Вспомогательные методы ════════════════════════

    private static void setDropChances(Mob mob, float chance) {
        mob.setDropChance(EquipmentSlot.HEAD,  chance);
        mob.setDropChance(EquipmentSlot.CHEST, chance);
        mob.setDropChance(EquipmentSlot.LEGS,  chance);
        mob.setDropChance(EquipmentSlot.FEET,  chance);
    }

    // ══════════════════════════ Вспомогательный класс ═════════════════════════

    private record EquipmentSet(
            net.minecraft.world.item.Item helmet,
            net.minecraft.world.item.Item chestplate,
            net.minecraft.world.item.Item leggings,
            net.minecraft.world.item.Item boots
    ) {
        void apply(Mob mob) {
            mob.setItemSlot(EquipmentSlot.HEAD,  new ItemStack(helmet));
            mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(chestplate));
            mob.setItemSlot(EquipmentSlot.LEGS,  new ItemStack(leggings));
            mob.setItemSlot(EquipmentSlot.FEET,  new ItemStack(boots));
        }
    }
}