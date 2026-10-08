package com.hbm_m.powerarmor;

import java.util.UUID;

import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.weapon.sedna.impl.IPAMelee;
import com.hbm_m.item.weapon.sedna.impl.IPARanged;
import com.hbm_m.item.weapon.sedna.impl.IPAWeaponsProvider;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.armor.ArmorNCRPA}: NCR-Ranger-Powerarmor, Sprintschub und Nachtsicht bei
 * eingeschaltetem HUD; Nah-/Fernkampfkomponenten fuer gun_pa_melee/gun_pa_ranged.
 */
public class ArmorNCRPA extends ModPowerArmorItem implements IPAWeaponsProvider {

    public ArmorNCRPA(ModArmorMaterials material, Type type, Properties properties, String texture,
                      long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, texture, maxPower, chargeRate, consumption, drain);
    }

    private static final UUID speed = UUID.fromString("6ab858ba-d712-485c-bae9-e5e765fc555a");

    @Override
    protected void armorTick(ItemStack stack, Level world, Player player) {
        super.armorTick(stack, world, player);

        if (this != ModItems.NCRPA_PLATE.get()) return;

        /// SPEED ///
        ArmorAttributes.speed(player, speed, "NCRPA SPEED", 0.1, player.isSprinting());

        if (hasFSBArmor(player)) {
            if (world.getGameTime() % 20 != 0) return;
            if (HbmPlayerProps.getData(player).enableHUD) player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, true));
        }
    }

    public static final ArmorNCRPAMelee meleeComponent = new ArmorNCRPAMelee();
    public static final ArmorNCRPARanged rangedComponent = new ArmorNCRPARanged();

    @Override
    public IPAMelee getMeleeComponent(Player entity) {
        if (hasFSBArmorIgnoreCharge(entity)) return meleeComponent;
        return null;
    }

    @Override
    public IPARanged getRangedComponent(Player entity) {
        if (hasFSBArmorIgnoreCharge(entity)) return rangedComponent;
        return null;
    }
}
