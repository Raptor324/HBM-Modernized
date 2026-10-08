package com.hbm_m.powerarmor;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;
import com.hbm_m.item.tools_and_armor.ModArmorMaterialsAccess;
import com.hbm_m.util.ArmorUtil;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

/** 1:1 {@code com.hbm.items.armor.ArmorEuphemium}: Vollset gibt Regeneration/Resistenz/Feuerschutz/Saettigung 127 und bremst den Fall. */
public class ArmorEuphemium extends ArmorItem {

    public ArmorEuphemium(ModArmorMaterials material, Type type, Properties properties) {
        super(ModArmorMaterialsAccess.holder(material), type, properties.rarity(Rarity.EPIC));
    }

    //? if forge {
    @Override
    //?}
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        if (stack.getItem() == ModItems.EUPHEMIUM_HELMET.get() || stack.getItem() == ModItems.EUPHEMIUM_PLATE.get() || stack.getItem() == ModItems.EUPHEMIUM_BOOTS.get()) {
            return "hbm_m:textures/armor/euphemium_1.png";
        }
        if (stack.getItem() == ModItems.EUPHEMIUM_LEGS.get()) {
            return "hbm_m:textures/armor/euphemium_2.png";
        }
        return null;
    }

    //? if neoforge {
    /*/^* NeoForge: Textur ueber die String-Variante (1.20.1 Forge {@code getArmorTexture(.., String type)}). ^/
    @Override
    public net.minecraft.resources.ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot,
            net.minecraft.world.item.ArmorMaterial.Layer layer, boolean innerModel) {
        String tex = this.getArmorTexture(stack, entity, slot, (String) null);
        return tex == null ? null : net.minecraft.resources.ResourceLocation.parse(tex);
    }
    *///?}

    //? if forge {
    @Override
    @SuppressWarnings("removal")
    //?}
    // NeoForge: Aufruf ueber ArmorTickNeoForge
    public void onArmorTick(@NotNull ItemStack armor, @NotNull Level world, @NotNull Player player) {
        if (ArmorUtil.checkArmor(player, ModItems.EUPHEMIUM_HELMET.get(), ModItems.EUPHEMIUM_PLATE.get(), ModItems.EUPHEMIUM_LEGS.get(), ModItems.EUPHEMIUM_BOOTS.get())) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 5, 127, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5, 127, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 5, 127, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 5, 127, true, true));

            if (player.getDeltaMovement().y < -0.25D) {
                player.setDeltaMovement(player.getDeltaMovement().x, -0.25D, player.getDeltaMovement().z);
                player.fallDistance = 0;
            }
        }
    }

    //do literally nothing lole
    @Override
    public void setDamage(ItemStack stack, int damage) { }
}
