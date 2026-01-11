package com.hbm_m.powerarmor;

import com.hbm_m.damagesource.ModDamageTypes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PowerArmorEventHandler {

    /**
     * POWER ARMOR DAMAGE OVERRIDE - Completely replaces vanilla armor calculations
     * This runs BEFORE vanilla armor calculations in LivingAttackEvent
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        // Skip creative/spectator players
        if (player.getAbilities().instabuild || player.isSpectator()) return;

        // Only override damage for full power armor sets
        if (!ModArmorFSB.hasFSBArmor(player)) return;

        // Check if this is a power armor set
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof ModPowerArmorItem)) return;

        // Calculate damage using our DT+DR system (ignoring vanilla armor)
        float calculatedDamage = DamageResistanceHandler.calculateDamage(
            player,
            event.getSource(),
            event.getAmount(),
            DamageResistanceHandler.currentPDT,
            DamageResistanceHandler.currentPDR
        );

        // If our system reduces damage, spend energy and apply the reduction
        if (calculatedDamage < event.getAmount()) {
            spendEnergyOnDamage(player, event.getAmount());
            // The actual damage application will happen in LivingHurtEvent
            // We just store the calculated damage for later use
            event.getEntity().getPersistentData().putFloat("hbm_power_armor_damage", calculatedDamage);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        // Skip creative/spectator players
        if (player.getAbilities().instabuild || player.isSpectator()) return;

        // Check if this player has power armor pre-calculated damage
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains("hbm_power_armor_damage")) {
            // Use our pre-calculated damage, ignoring vanilla armor calculations
            float powerArmorDamage = persistentData.getFloat("hbm_power_armor_damage");
            event.setAmount(powerArmorDamage);
            persistentData.remove("hbm_power_armor_damage");
            return;
        }

        // For non-power armor, use partial protection logic
        if (!ModArmorFSB.hasFSBArmor(player)) {
            handlePartialArmorProtection(event, player);
            return;
        }

        // For regular FSB armor (non-powered), apply basic effects but don't override vanilla armor
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof ModPowerArmorItem)) {
            // Regular FSB armor - just apply effects, don't modify damage calculation
            return;
        }

        // This should not be reached for power armor due to LivingAttackEvent handling
        // But as a fallback, apply minimal protection
        handlePartialArmorProtection(event, player);
    }

    /**
     * Обработка частичной защиты для неполных комплектов
     */
    private static void handlePartialArmorProtection(LivingHurtEvent event, Player player) {
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof ModPowerArmorItem armorItem)) return;

        // Store original amount before modification
        float originalAmount = event.getAmount();

        // Простая защита только от power armor в нагруднике
        float reduction = calculateSimpleReduction(event.getSource(), armorItem.getSpecs());
        if (reduction > 0) {
            event.setAmount(originalAmount * (1.0f - reduction));
            spendEnergyOnDamage(player, originalAmount);
        }
    }

    /**
     * Простое вычисление редукции для частичной защиты (только DR, без DT)
     */
    private static float calculateSimpleReduction(DamageSource source, com.hbm_m.powerarmor.PowerArmorSpecs specs) {
        if (source.is(DamageTypes.FALL)) {
            return specs.drFall;
        } else if (source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION)) {
            return specs.drExplosion;
        } else if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.LAVA) ||
                   source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.HOT_FLOOR)) {
            return specs.drFire;
        } else if (source.is(DamageTypes.FREEZE)) {
            return specs.drCold;
        } else if (source.isIndirect() || source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile) {
            return specs.drProjectile;
        } else if (source.is(ModDamageTypes.RADIATION)) {
            return specs.drRadiation;
        } else if (source.is(ModDamageTypes.ACID)) {
            return specs.drKinetic; // Кислота считается кинетическим уроном в оригинале
        } else if (source.is(ModDamageTypes.LASER) || source.is(ModDamageTypes.ELECTRICITY) ||
                   source.is(ModDamageTypes.MICROWAVE) || source.is(ModDamageTypes.SUB_ATOMIC)) {
            return specs.drEnergy;
        } else {
            // Для любого другого урона (включая урон от мобов) используем кинетический резист
            return specs.drKinetic;
        }
    }

    /**
     * Расход энергии при получении урона (как в оригинале HBM)
     */
    private static void spendEnergyOnDamage(Player player, float originalDamage) {
        // В оригинале HBM каждый удар приводит к damage=1 для каждой части брони,
        // что тратит consumption (1000) энергии с каждой части
        for (ItemStack stack : player.getInventory().armor) {
            if (!stack.isEmpty() && stack.getItem() instanceof ModArmorFSBPowered item) {
                item.setDamage(stack, 1); // Каждая часть получает damage=1 за удар
            }
        }
    }
}