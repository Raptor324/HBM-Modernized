package com.hbm_m.event;

import com.hbm_m.item.armor.ModPowerArmorItem;
import com.hbm_m.item.armor.PowerArmorSpecs;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PowerArmorEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        // 1. Проверяем полный сет
        if (!ModPowerArmorItem.hasFullSet(player)) return;

        // Получаем образец брони (берем нагрудник как контроллер)
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof ModPowerArmorItem armorItem)) return;

        PowerArmorSpecs specs = armorItem.getSpecs();
        long energy = armorItem.getEnergy(chestStack);

        // Если энергии нет - защита не работает
        if (energy <= 0) return;

        DamageSource source = event.getSource();
        float originalDamage = event.getAmount();
        float reductionMultiplier = 0.0f;

        // 2. Определяем тип урона и резист
        if (source.is(DamageTypes.FALL)) {
            reductionMultiplier = specs.resFall;
        } else if (source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION)) {
            reductionMultiplier = specs.resExplosion;
        } else if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.LAVA) || source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.HOT_FLOOR)) {
            reductionMultiplier = specs.resFire;
        } else if (source.is(DamageTypes.FREEZE)) {
            reductionMultiplier = specs.resCold;
        } else if (isProjectile(source)) {
            reductionMultiplier = specs.resProjectile;
        } else if (isKinetic(source)) {
            reductionMultiplier = specs.resKinetic;
        }
        // TODO: Добавить проверку кастомных типов урона HBM (Acid, Radiation и т.д.) через ModDamageTypes

        // 3. Если есть резист
        if (reductionMultiplier > 0) {
            // Снижаем урон
            float newDamage = originalDamage * (1.0f - reductionMultiplier);
            event.setAmount(newDamage);

            // 4. Тратим энергию
            // Если режим DRAIN_ON_HIT - тратим энергию за поглощенный урон
            if (specs.mode == PowerArmorSpecs.EnergyMode.DRAIN_ON_HIT) {
                long energyCost = (long) (originalDamage * specs.usagePerDamagePoint);
                distributeEnergyDrain(player, energyCost);
            }
            // Если режим CONSTANT_DRAIN, энергия уже тратится в onArmorTick, дополнительно не снимаем
            // (или можно снимать, если хотите двойную плату)
        }
    }

    // Помощник для равномерного снятия энергии со всех частей брони
    private static void distributeEnergyDrain(Player player, long totalCost) {
        long costPerPiece = totalCost / 4;
        if (costPerPiece == 0) costPerPiece = 1;

        for (ItemStack stack : player.getInventory().armor) {
            if (stack.getItem() instanceof ModPowerArmorItem item) {
                item.extractEnergy(stack, costPerPiece, false);
            }
        }
    }

    private static boolean isProjectile(DamageSource source) {
        return source.isIndirect() || source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile;
    }

    private static boolean isKinetic(DamageSource source) {
        // Кинетика - это прямой урон (мобы, игроки), но не магия и не взрывы
        return !source.isIndirect() && !source.is(DamageTypes.MAGIC) && !source.is(DamageTypes.EXPLOSION);
    }
}