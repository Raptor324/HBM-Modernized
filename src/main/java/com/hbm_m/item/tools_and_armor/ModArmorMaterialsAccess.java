package com.hbm_m.item.tools_and_armor;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import com.hbm_m.main.MainRegistry;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

//? if forge {
import net.minecraft.world.item.ArmorMaterial;
//?}

/**
 * Кросс-версионный мост между enum'ом {@link ModArmorMaterials} и ванильным типом материала брони.
 *
 * На 1.20.1 (Forge) {@link ArmorMaterial} — это интерфейс, который сам enum и реализует,
 * поэтому Holder не нужен и {@link #holder(ModArmorMaterials)} просто возвращает сам enum.
 *
 * На 1.21.1 (NeoForge) {@link ArmorMaterial} стал {@code final record} — его нельзя
 * реализовать через enum, и {@link ArmorItem} ожидает {@code Holder<ArmorMaterial>}.
 * Поэтому мы регистрируем 12 материалов через {@code DeferredRegister<ArmorMaterial>}
 * и {@link #holder(ModArmorMaterials)} возвращает зарегистрированный Holder.
 *
 * Централизованный API (вместо кучи stonecutter-веток в каждом call-site):
 *   {@code ModArmorMaterialsAccess.holder(ModArmorMaterials.ALLOY)}
 */
public final class ModArmorMaterialsAccess {

    private ModArmorMaterialsAccess() {}

    //? if neoforge {
    /*/^*
     * NeoForge 1.21.1: реестр ArmorMaterial как полноценный vanilla registry entry.
     * Holder'ы регистрируются лениво, синхронно с ModItems (в MainRegistry).
     ^/
    public static final dev.architectury.registry.registries.DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            dev.architectury.registry.registries.DeferredRegister.create(MainRegistry.MOD_ID, net.minecraft.core.registries.Registries.ARMOR_MATERIAL);

    private static final Map<ModArmorMaterials, dev.architectury.registry.registries.RegistrySupplier<ArmorMaterial>> HOLDERS =
            new EnumMap<>(ModArmorMaterials.class);

    private static final int[] BASE_DURABILITY = { 11, 16, 16, 13 };

    static {
        for (ModArmorMaterials m : ModArmorMaterials.values()) {
            HOLDERS.put(m, ARMOR_MATERIALS.register(m.name().toLowerCase(java.util.Locale.ROOT), () -> buildMaterial(m)));
        }
    }

    private static ArmorMaterial buildMaterial(ModArmorMaterials m) {
        var defense = new java.util.EnumMap<ArmorItem.Type, Integer>(ArmorItem.Type.class);
        int[] p = m.getProtectionAmounts();
        defense.put(ArmorItem.Type.HELMET,     p[0]);
        defense.put(ArmorItem.Type.CHESTPLATE, p[1]);
        defense.put(ArmorItem.Type.LEGGINGS,    p[2]);
        defense.put(ArmorItem.Type.BOOTS,       p[3]);

        // 1.21.1: SoundEvents.ARMOR_EQUIP_* уже Holder<SoundEvent> — обёртка Holder.direct() не нужна.
        Holder<SoundEvent> equip = switch (m.name()) {
            case "STARMETAL" -> SoundEvents.ARMOR_EQUIP_GOLD;
            case "SECURITY"  -> SoundEvents.ARMOR_EQUIP_CHAIN;
            case "HAZMAT"    -> SoundEvents.ARMOR_EQUIP_LEATHER;
            case "PAA"       -> SoundEvents.ARMOR_EQUIP_GOLD;
            default          -> SoundEvents.ARMOR_EQUIP_IRON;
        };

        var layers = java.util.List.of(
                new ArmorMaterial.Layer(
                        //? if < 1.21.1 {
                        new ResourceLocation(MainRegistry.MOD_ID, m.name().toLowerCase(java.util.Locale.ROOT))//?} else {
                        /^ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, m.name().toLowerCase(java.util.Locale.ROOT))
                        ^///?}
                        , "", false)
        );

        return new ArmorMaterial(
                defense,
                m.getEnchantmentValue(),
                equip,
                (Supplier<Ingredient>) () -> m.getRepairIngredientSupplier().get(),
                layers,
                m.getToughness(),
                m.getKnockbackResistance()
        );
    }

    /^* Возвращает Holder<ArmorMaterial> для NeoForge — он передаётся в ArmorItem. ^/
    public static Holder<ArmorMaterial> holder(ModArmorMaterials m) {
        return Holder.direct(HOLDERS.get(m).get());
    }

    /^* Регистрирует ARMOR_MATERIALS через Architectury (NeoForge). ^/
    public static void init() {
        ARMOR_MATERIALS.register();
    }
    *///?}

    //? if forge {
    public static ModArmorMaterials holder(ModArmorMaterials m) {
        return m;
    }
    //?}
}
