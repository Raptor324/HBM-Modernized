package com.hbm_m.item.tools_and_armor;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.main.MainRegistry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

/**
 * Перечень материалов брони мода.
 *
 * На 1.20.1 (Forge) enum реализует {@code ArmorMaterial} интерфейс — это ванильный способ.
 *
 * На 1.21.1 (NeoForge) {@code ArmorMaterial} стал {@code final record} — его нельзя
 * реализовать через enum. Поэтому здесь enum НЕ реализует {@code ArmorMaterial}, а
 * действительный материал регистрируется через {@link ModArmorMaterialsAccess#holder}.
 *
 * Чтобы не плодить stonecutter-ветки в call-sites, все реальные параметры публикуются
 * через публичные геттеры — {@code ModArmorMaterialsAccess} использует их при сборке
 * материала на NeoForge.
 */

//? if < 1.21.1 {
import net.minecraft.world.item.ArmorMaterial;
//?}

//? if < 1.21.1 {
public enum ModArmorMaterials implements ArmorMaterial {
//?} else {
/*public enum ModArmorMaterials {
*///?}

    ALLOY("alloy", 26, new int[]{ 5, 7, 5, 4 }, 25,
            SoundEvents.ARMOR_EQUIP_IRON, 1.25f, 0.05f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))),

    STARMETAL("starmetal", 26, new int[]{ 8, 8, 8, 8 }, 25,
            SoundEvents.ARMOR_EQUIP_GOLD, 2f, 0.2f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))),

    SECURITY("security", 26, new int[]{ 4, 6, 4, 3 }, 25,
            SoundEvents.ARMOR_EQUIP_CHAIN, 1.25f, 0.03f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))),

    HAZMAT("hazmat", 26, new int[]{ 2, 4, 2, 1 }, 25,
            SoundEvents.ARMOR_EQUIP_LEATHER, 0f, 0f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))),

    PAA("paa", 26, new int[]{ 5, 7, 5, 4 }, 25,
            SoundEvents.ARMOR_EQUIP_GOLD, 1.75f, 0.07f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))),

    LIQUIDATOR("liquidator", 26, new int[]{ 5, 7, 6, 4 }, 25,
            SoundEvents.ARMOR_EQUIP_IRON, 1.5f, 0.1f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))),

    STEEL("steel", 26, new int[]{ 4, 5, 3, 2 }, 25,
            SoundEvents.ARMOR_EQUIP_IRON, 1f, 0.03f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))),

    // The two jackts are steel-tier in the original (aMatSteel) but carry their own skin, and
    // the shimmer weapons check for them by identity - so they need their own material rather
    // than sharing STEEL's texture.
    JACKT("jackt", 26, new int[]{ 4, 5, 3, 2 }, 25,
            SoundEvents.ARMOR_EQUIP_LEATHER, 1f, 0.03f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))),

    JACKT2("jackt2", 26, new int[]{ 4, 5, 3, 2 }, 25,
            SoundEvents.ARMOR_EQUIP_LEATHER, 1f, 0.03f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))),

    COBALT("cobalt", 26, new int[]{ 2, 4, 2, 1 }, 25,
            SoundEvents.ARMOR_EQUIP_IRON, 0.25f, 0f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))),

    AJR("ajr", 26, new int[]{ 7, 8, 6, 5 }, 25,
            SoundEvents.ARMOR_EQUIP_IRON, 2f, 0.3f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))),

    ASBESTOS("asbestos", 26, new int[]{ 3, 5, 3, 2 }, 25,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))),

    TITANIUM("titanium", 26, new int[]{ 5, 7, 5, 4 }, 15,
            SoundEvents.ARMOR_EQUIP_IRON, 1f, 0.05f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))),

    BISMUTH("bismuth", 100, new int[]{ 3, 8, 6, 3 }, 100,
            SoundEvents.ARMOR_EQUIP_IRON, 2f, 0.2f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.BISMUTH, MaterialShape.PLATE))),

    /** Противогазы: только шлем, символическая защита (лёгкие защищает фильтр). */
    GAS_MASK("gas_mask", 5, new int[]{ 1, 0, 0, 0 }, 5,
            SoundEvents.ARMOR_EQUIP_LEATHER, 0f, 0f, () -> Ingredient.of(ModItems.RAG_DAMP.get()));

    private final String name;
    private final int durabilityMultiplier;
    private final int[] protectionAmounts;
    private final int enchantmentValue;
    private final Object equipSound;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repairIngredient;

    private static final int[] BASE_DURABILITY = { 11, 16, 16, 13 };

    ModArmorMaterials(String name, int durabilityMultiplier, int[] protectionAmounts, int enchantmentValue, Object equipSound,
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

    public String getName() {
        return MainRegistry.MOD_ID + ":" + this.name;
    }

    public int[] getProtectionAmounts() {
        return this.protectionAmounts;
    }

    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    public float getToughness() {
        return this.toughness;
    }

    public float getKnockbackResistance() {
        return this.knockbackResistance;
    }

    public Supplier<Ingredient> getRepairIngredientSupplier() {
        return this.repairIngredient;
    }

    //? if >= 1.21.1 {
    /*public net.minecraft.core.Holder<SoundEvent> getEquipSound() {
        return (net.minecraft.core.Holder<SoundEvent>) this.equipSound;
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public int getDurabilityForType(ArmorItem.Type pType) {
        return BASE_DURABILITY[pType.ordinal()] * this.durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type pType) {
        return this.protectionAmounts[pType.ordinal()];
    }

    @Override
    public SoundEvent getEquipSound() {
        return (SoundEvent) this.equipSound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }
    //?}
}