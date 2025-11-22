package com.hbm_m.handler;

import com.hbm_m.item.ModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "hbm_m")
public class MobGearHandler {

    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        if (entity instanceof Zombie zombie) {
            if (zombie.getRandom().nextFloat() < 0.07f) { // 15% шанс

                zombie.setItemSlot(EquipmentSlot.HEAD,  new ItemStack(ModItems.SECURITY_HELMET.get()));
                zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.SECURITY_CHESTPLATE.get()));
                zombie.setItemSlot(EquipmentSlot.LEGS,  new ItemStack(ModItems.SECURITY_LEGGINGS.get()));
                zombie.setItemSlot(EquipmentSlot.FEET,  new ItemStack(ModItems.SECURITY_BOOTS.get()));

                if (zombie.getRandom().nextBoolean()) {
                    zombie.setItemSlot(EquipmentSlot.HEAD,  new ItemStack(ModItems.HAZMAT_HELMET.get()));
                    zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.HAZMAT_CHESTPLATE.get()));
                    zombie.setItemSlot(EquipmentSlot.LEGS,  new ItemStack(ModItems.HAZMAT_LEGGINGS.get()));
                    zombie.setItemSlot(EquipmentSlot.FEET,  new ItemStack(ModItems.HAZMAT_BOOTS.get()));
                }

                if (zombie.getRandom().nextBoolean()) {
                    zombie.setItemSlot(EquipmentSlot.HEAD,  new ItemStack(ModItems.TITANIUM_HELMET.get()));
                    zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.TITANIUM_CHESTPLATE.get()));
                    zombie.setItemSlot(EquipmentSlot.LEGS,  new ItemStack(ModItems.TITANIUM_LEGGINGS.get()));
                    zombie.setItemSlot(EquipmentSlot.FEET,  new ItemStack(ModItems.TITANIUM_BOOTS.get()));
                }

                if (zombie.getRandom().nextBoolean()) {
                    zombie.setItemSlot(EquipmentSlot.HEAD,  new ItemStack(ModItems.COBALT_HELMET.get()));
                    zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.COBALT_CHESTPLATE.get()));
                    zombie.setItemSlot(EquipmentSlot.LEGS,  new ItemStack(ModItems.COBALT_LEGGINGS.get()));
                    zombie.setItemSlot(EquipmentSlot.FEET,  new ItemStack(ModItems.COBALT_BOOTS.get()));
                }

                if (zombie.getRandom().nextBoolean()) {
                    zombie.setItemSlot(EquipmentSlot.HEAD,  new ItemStack(ModItems.ALLOY_HELMET.get()));
                    zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.ALLOY_CHESTPLATE.get()));
                    zombie.setItemSlot(EquipmentSlot.LEGS,  new ItemStack(ModItems.ALLOY_LEGGINGS.get()));
                    zombie.setItemSlot(EquipmentSlot.FEET,  new ItemStack(ModItems.ALLOY_BOOTS.get()));
                }

                if (zombie.getRandom().nextFloat() < 0.25f) {
                    zombie.setItemSlot(EquipmentSlot.HEAD,  new ItemStack(ModItems.STEEL_HELMET.get()));
                    zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.STEEL_CHESTPLATE.get()));
                    zombie.setItemSlot(EquipmentSlot.LEGS,  new ItemStack(ModItems.STEEL_LEGGINGS.get()));
                    zombie.setItemSlot(EquipmentSlot.FEET,  new ItemStack(ModItems.STEEL_BOOTS.get()));
                }

                // Оружие
                zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.ALLOY_SWORD.get()));
                zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.ALLOY_PICKAXE.get()));
                zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.ALLOY_SHOVEL.get()));
                zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.TITANIUM_SWORD.get()));
                zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.TITANIUM_PICKAXE.get()));
                zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.TITANIUM_SHOVEL.get()));
                zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.STEEL_SWORD.get()));
                zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.STEEL_PICKAXE.get()));
                zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.STEEL_SHOVEL.get()));
                // zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.GUN_REX.get()));

                // Дроп шанса (как у ванильных)
                zombie.setDropChance(EquipmentSlot.HEAD, 0.05f);
                zombie.setDropChance(EquipmentSlot.CHEST, 0.05f);
                zombie.setDropChance(EquipmentSlot.LEGS, 0.05f);
                zombie.setDropChance(EquipmentSlot.FEET, 0.05f);
            }
        }

        if (entity instanceof Skeleton skeleton) {
            if (skeleton.getRandom().nextFloat() < 0.07f) {
                skeleton.setItemSlot(EquipmentSlot.HEAD,  new ItemStack(ModItems.STEEL_HELMET.get()));
                skeleton.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.STEEL_CHESTPLATE.get()));
                skeleton.setItemSlot(EquipmentSlot.LEGS,  new ItemStack(ModItems.STEEL_LEGGINGS.get()));
                skeleton.setItemSlot(EquipmentSlot.FEET,  new ItemStack(ModItems.STEEL_BOOTS.get()));
            }
            if (skeleton.getRandom().nextBoolean()) {
                skeleton.setItemSlot(EquipmentSlot.HEAD,  new ItemStack(ModItems.TITANIUM_HELMET.get()));
                skeleton.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.TITANIUM_CHESTPLATE.get()));
                skeleton.setItemSlot(EquipmentSlot.LEGS,  new ItemStack(ModItems.TITANIUM_LEGGINGS.get()));
                skeleton.setItemSlot(EquipmentSlot.FEET,  new ItemStack(ModItems.TITANIUM_BOOTS.get()));
            }
            if (skeleton.getRandom().nextBoolean()) {
                skeleton.setItemSlot(EquipmentSlot.HEAD,  new ItemStack(ModItems.COBALT_HELMET.get()));
                skeleton.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.COBALT_CHESTPLATE.get()));
                skeleton.setItemSlot(EquipmentSlot.LEGS,  new ItemStack(ModItems.COBALT_LEGGINGS.get()));
                skeleton.setItemSlot(EquipmentSlot.FEET,  new ItemStack(ModItems.COBALT_BOOTS.get()));
            }
            if (skeleton.getRandom().nextBoolean()) {
                skeleton.setItemSlot(EquipmentSlot.HEAD,  new ItemStack(ModItems.ALLOY_HELMET.get()));
                skeleton.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.ALLOY_CHESTPLATE.get()));
                skeleton.setItemSlot(EquipmentSlot.LEGS,  new ItemStack(ModItems.ALLOY_LEGGINGS.get()));
                skeleton.setItemSlot(EquipmentSlot.FEET,  new ItemStack(ModItems.ALLOY_BOOTS.get()));
            }

            // Дроп шанса (как у ванильных)
            skeleton.setDropChance(EquipmentSlot.HEAD, 0.1f);
            skeleton.setDropChance(EquipmentSlot.CHEST, 0.1f);
            skeleton.setDropChance(EquipmentSlot.LEGS, 0.1f);
            skeleton.setDropChance(EquipmentSlot.FEET, 0.1f);

        }
    }
}