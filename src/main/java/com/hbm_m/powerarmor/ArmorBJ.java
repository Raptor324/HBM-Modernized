package com.hbm_m.powerarmor;

import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 1:1 {@code com.hbm.items.armor.ArmorBJ}: Mondrustung; ohne Ladung wirft der Helm den Traeger ab (lunar). */
public class ArmorBJ extends ModPowerArmorItem {

    public ArmorBJ(ModArmorMaterials material, Type type, Properties properties, String texture,
                   long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, texture, maxPower, chargeRate, consumption, drain);
    }

    @Override
    protected void armorTick(ItemStack itemStack, Level world, Player player) {
        super.armorTick(itemStack, world, player);

        if (this == ModItems.BJ_HELMET.get() && ModArmorFSB.hasFSBArmorIgnoreCharge(player) && !ModArmorFSB.hasFSBArmor(player)) {

            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);

            if (!player.getInventory().add(helmet))
                player.drop(helmet, false);

            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);

            player.hurt(ModDamageSources.create(world, ModDamageTypes.LUNAR), 1000);
        }
    }
}
