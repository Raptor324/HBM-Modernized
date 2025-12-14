package com.hbm_m.item.custom.tools_and_armor;

import com.hbm_m.item.ModItems;
import com.hbm_m.main.MainRegistry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

public enum ModArmorMaterials implements ArmorMaterial {




    ALLOY("alloy", 26, new int[]{ 5, 7, 5, 4 }, 25,
            SoundEvents.ARMOR_EQUIP_IRON, 1.25f, 0.05f, () -> Ingredient.of(ModItems.PLATE_STEEL.get())),

    STARMETAL("starmetal", 26, new int[]{ 8, 8, 8, 8 }, 25,
            SoundEvents.ARMOR_EQUIP_GOLD, 2f, 0.2f, () -> Ingredient.of(ModItems.PLATE_STEEL.get())),

    SECURITY("security", 26, new int[]{ 4, 6, 4, 3 }, 25,
            SoundEvents.ARMOR_EQUIP_CHAIN, 1.25f, 0.03f, () -> Ingredient.of(ModItems.PLATE_STEEL.get())),

    HAZMAT("hazmat", 26, new int[]{ 2, 4, 2, 1 }, 25,
            SoundEvents.ARMOR_EQUIP_LEATHER, 0f, 0f, () -> Ingredient.of(ModItems.PLATE_STEEL.get())),

    PAA("paa", 26, new int[]{ 5, 7, 5, 4 }, 25,
            SoundEvents.ARMOR_EQUIP_GOLD, 1.75f, 0.07f, () -> Ingredient.of(ModItems.PLATE_STEEL.get())),

    LIQUIDATOR("liquidator", 26, new int[]{ 5, 7, 6, 4 }, 25,
            SoundEvents.ARMOR_EQUIP_IRON, 1.5f, 0.1f, () -> Ingredient.of(ModItems.PLATE_STEEL.get())),

    STEEL("steel", 26, new int[]{ 4, 5, 3, 2 }, 25,
            SoundEvents.ARMOR_EQUIP_IRON, 1f, 0.03f, () -> Ingredient.of(ModItems.PLATE_STEEL.get())),

    COBALT("cobalt", 26, new int[]{ 2, 4, 2, 1 }, 25,
            SoundEvents.ARMOR_EQUIP_IRON, 0.25f, 0f, () -> Ingredient.of(ModItems.PLATE_STEEL.get())),

    AJR("ajr", 26, new int[]{ 7, 8, 6, 5 }, 25,
            SoundEvents.ARMOR_EQUIP_IRON, 2f, 0.3f, () -> Ingredient.of(ModItems.PLATE_STEEL.get())),

    ASBESTOS("asbestos", 26, new int[]{ 3, 5, 3, 2 }, 25,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModItems.PLATE_STEEL.get())),

    TITANIUM("titanium", 26, new int[]{ 5, 7, 5, 4 }, 15,
    SoundEvents.ARMOR_EQUIP_IRON, 1f, 0.05f, () -> Ingredient.of(ModItems.PLATE_IRON.get()));





    private final String name;
    private final int durabilityMultiplier;
    private final int[] protectionAmounts;
    private final int enchantmentValue;
    private final SoundEvent equipSound;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repairIngredient;

    private static final int[] BASE_DURABILITY = { 11, 16, 16, 13 };

    ModArmorMaterials(String name, int durabilityMultiplier, int[] protectionAmounts, int enchantmentValue, SoundEvent equipSound,
                      float toughness, float knockbackResistance, Supplier<Ingredient> repairIngredient) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.protectionAmounts = protectionAmounts;
        this.enchantmentValue = enchantmentValue;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = repairIngredient;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type pType) {
        return BASE_DURABILITY[pType.ordinal()] * this.durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type pType) {
        return this.protectionAmounts[pType.ordinal()];
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return this.equipSound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }

    @Override
    public String getName() {
        return MainRegistry.MOD_ID + ":" + this.name;
    }

    @Override
    public float getToughness() {
        return this.toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return this.knockbackResistance;
    }

}
