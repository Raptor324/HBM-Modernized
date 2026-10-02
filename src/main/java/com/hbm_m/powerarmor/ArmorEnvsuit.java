package com.hbm_m.powerarmor;

import java.util.UUID;

import com.hbm_m.armormod.item.ItemModNightVision;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code com.hbm.items.armor.ArmorEnvsuit}: Tauchanzug mit Luftvorrat, Nachtsicht und Schwimmschub. */
public class ArmorEnvsuit extends ModPowerArmorItem {

    public ArmorEnvsuit(ModArmorMaterials material, Type type, Properties properties, String texture,
                        long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, texture, maxPower, chargeRate, consumption, drain);
    }

    private static final UUID speed = UUID.fromString("6ab858ba-d712-485c-bae9-e5e765fc555a");

    @Override
    protected void armorTick(ItemStack stack, Level world, Player player) {
        super.armorTick(stack, world, player);

        if (this != ModItems.ENVSUIT_PLATE.get())
            return;

        /// SPEED ///
        boolean full = hasFSBArmor(player);
        ArmorAttributes.speed(player, speed, "SQUIRREL SPEED", 0.1, full && player.isSprinting());

        if (full) {

            if (player.isInWater()) {

                if (!world.isClientSide) {
                    player.setAirSupply(300);
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 15 * 20, 0));
                }

                double mo = 0.1 * player.zza;
                Vec3 vec = player.getLookAngle().scale(mo);
                player.setDeltaMovement(player.getDeltaMovement().add(vec));
            } else {
                boolean canRemoveNightVision = true;
                ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
                ItemStack helmetMod = ArmorModificationHelper.pryMod(helmet, ArmorModificationHelper.helmet_only); // Get the modification!
                if (helmetMod != null && helmetMod.getItem() instanceof ItemModNightVision) {
                    canRemoveNightVision = false;
                }

                if (!world.isClientSide && canRemoveNightVision) {
                    player.removeEffect(MobEffects.NIGHT_VISION);
                }
            }
        }
    }
}
