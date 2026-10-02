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

    /** Original HBM_ALLOY (Deprecated), ohne Reparaturmaterial. */
    ALLOY("alloy", 40, new int[]{ 3, 8, 6, 3 }, 12,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.EMPTY),

    /** Original HBM_STARMETAL. */
    STARMETAL("starmetal", 150, new int[]{ 3, 8, 6, 3 }, 100,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.STARMETAL, MaterialShape.INGOT))),

    /** Original HBM_SECURITY. */
    SECURITY("security", 100, new int[]{ 3, 8, 6, 3 }, 15,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModItems.PLATE_KEVLAR.get())),

    /** Original HBM_HAZMAT. */
    HAZMAT("hazmat", 60, new int[]{ 2, 5, 4, 1 }, 5,
            SoundEvents.ARMOR_EQUIP_LEATHER, 0f, 0f, () -> Ingredient.of(ModItems.HAZMAT_CLOTH.get())),

    /** Original HBM_HAZMAT2. */
    HAZMAT_RED("hazmat_red", 60, new int[]{ 2, 5, 4, 1 }, 5,
            SoundEvents.ARMOR_EQUIP_LEATHER, 0f, 0f, () -> Ingredient.of(ModItems.HAZMAT_CLOTH_RED.get())),

    /** Original HBM_HAZMAT3. */
    HAZMAT_GREY("hazmat_grey", 60, new int[]{ 2, 5, 4, 1 }, 5,
            SoundEvents.ARMOR_EQUIP_LEATHER, 0f, 0f, () -> Ingredient.of(ModItems.HAZMAT_CLOTH_GREY.get())),

    /** Original HBM_PAA. */
    PAA("paa", 75, new int[]{ 3, 8, 6, 3 }, 25,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModItems.PLATE_PAA.get())),

    /** Original HBM_LIQUIDATOR. */
    LIQUIDATOR("liquidator", 750, new int[]{ 3, 8, 6, 3 }, 10,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.PLATE))),

    /** Original HBM_STEEL. */
    STEEL("steel", 30, new int[]{ 3, 8, 6, 3 }, 5,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))),

    /** Original aMatSteel (eigene Textur). */
    JACKT("jackt", 30, new int[]{ 3, 8, 6, 3 }, 5,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))),

    /** Original aMatSteel (eigene Textur). */
    JACKT2("jackt2", 30, new int[]{ 3, 8, 6, 3 }, 5,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))),

    /** Original HBM_COBALT. */
    COBALT("cobalt", 70, new int[]{ 3, 8, 6, 3 }, 60,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.INGOT))),

    /** Original HBM_T45AJR (AJR, AJRO, RPA, NCRPA). */
    AJR("ajr", 150, new int[]{ 3, 8, 6, 3 }, 0,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModItems.PLATE_ARMOR_AJR.get())),

    /** Original HBM_ASBESTOS. */
    ASBESTOS("asbestos", 20, new int[]{ 1, 4, 3, 1 }, 5,
            SoundEvents.ARMOR_EQUIP_LEATHER, 0f, 0f, () -> Ingredient.of(ModItems.ASBESTOS_CLOTH.get())),

    /** Original HBM_TITANIUM. */
    TITANIUM("titanium", 25, new int[]{ 3, 8, 6, 3 }, 9,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.INGOT))),

    /** Original HBM_BISMUTH. */
    BISMUTH("bismuth", 100, new int[]{ 3, 8, 6, 3 }, 100,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.BISMUTH, MaterialShape.PLATE))),

    /** Original HBM_SCHRABIDIUM. */
    SCHRABIDIUM("schrabidium", 100, new int[]{ 3, 8, 6, 3 }, 50,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.INGOT))),

    /** Original HBM_EUPHEMIUM. */
    EUPHEMIUM("euphemium", 15000000, new int[]{ 3, 8, 6, 3 }, 100,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.EMPTY),

    /** Original HBM_CMB. */
    CMB("cmb", 60, new int[]{ 3, 8, 6, 3 }, 50,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.COMBINE_STEEL, MaterialShape.INGOT))),

    /** Original HBM_AUSIII. */
    AUS3("aus3", 375, new int[]{ 2, 6, 5, 2 }, 0,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.AUSTRALIUM, MaterialShape.INGOT))),

    /** Original HBM_ZIRCONIUM. */
    ZIRCONIUM("zirconium", 1000, new int[]{ 2, 5, 3, 1 }, 1000,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.INGOT))),

    /** Original HBM_DNT_LOLOLOL. */
    DNT("dnt", 3, new int[]{ 1, 1, 1, 1 }, 0,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.DINEUTRONIUM, MaterialShape.INGOT))),

    /** Original HBM_T51. */
    T51("t51", 150, new int[]{ 3, 8, 6, 3 }, 0,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModItems.PLATE_ARMOR_TITANIUM.get())),

    /** Original HBM_DESH (Dampfanzug). */
    DESH("desh", 150, new int[]{ 3, 8, 6, 3 }, 0,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.INGOT))),

    /** Original HBM_BNUUY (Dieselanzug). */
    DIESEL("diesel", 150, new int[]{ 3, 8, 6, 3 }, 0,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE))),

    /** Original HBM_BLACKJACK. */
    BJ("bj", 150, new int[]{ 3, 8, 6, 3 }, 0,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModItems.PLATE_ARMOR_LUNAR.get())),

    /** Original HBM_ENV. */
    ENV("env", 150, new int[]{ 3, 8, 6, 3 }, 10,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModItems.PLATE_ARMOR_HEV.get())),

    /** Original HBM_HEV. */
    HEV("hev", 150, new int[]{ 3, 8, 6, 3 }, 0,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModItems.PLATE_ARMOR_HEV.get())),

    /** Original HBM_DIGAMMA. */
    FAU("fau", 150, new int[]{ 3, 8, 6, 3 }, 0,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModItems.PLATE_ARMOR_FAU.get())),

    /** Original HBM_DNT_NANO. */
    DNS("dns", 150, new int[]{ 3, 8, 6, 3 }, 0,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModItems.PLATE_ARMOR_DNT.get())),

    /** Original HBM_TRENCH (Taurun, Verzauberbarkeit 10). */
    TAURUN("taurun", 150, new int[]{ 3, 8, 6, 3 }, 10,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))),

    /** Original HBM_TRENCH (Trenchmaster). */
    TRENCH("trench", 150, new int[]{ 3, 8, 6, 3 }, 0,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))),

    /** Original HBM_RAGS. */
    RAGS("rags", 150, new int[]{ 1, 1, 1, 1 }, 0,
            SoundEvents.ARMOR_EQUIP_LEATHER, 0f, 0f, () -> Ingredient.of(ModItems.RAG.get())),

    /** Original ArmorMaterial.CHAIN (Roben, Umhaenge). */
    CHAIN("chain", 15, new int[]{ 2, 5, 4, 1 }, 12,
            SoundEvents.ARMOR_EQUIP_CHAIN, 0f, 0f, () -> Ingredient.of(net.minecraft.world.item.Items.IRON_INGOT)),

    /** Original ArmorMaterial.IRON (Brille, Maske der Schande). */
    IRON("iron", 15, new int[]{ 2, 6, 5, 2 }, 9,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(net.minecraft.world.item.Items.IRON_INGOT)),

    /** Original ArmorGasMask: ArmorMaterial.IRON. */
    GAS_MASK("gas_mask", 15, new int[]{ 2, 6, 5, 2 }, 9,
            SoundEvents.ARMOR_EQUIP_IRON, 0f, 0f, () -> Ingredient.of(net.minecraft.world.item.Items.IRON_INGOT));

    private final String name;
    private final int durabilityMultiplier;
    private final int[] protectionAmounts;
    private final int enchantmentValue;
    private final Object equipSound;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repairIngredient;

    /** 1.7.10 ItemArmor.maxDamageArray: Helm 11, Brust 16, Beine 15, Stiefel 13. */
    private static final int[] BASE_DURABILITY = { 11, 16, 15, 13 };

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