package com.hbm_m.block.custom.machines.armormod.util;

// Хелпер для управления модификациями брони: сохранение/загрузка из NBT,
// применение атрибутов, вычисление защиты от радиации и т.п.
import com.google.common.collect.Multimap;
import com.hbm_m.block.custom.machines.armormod.item.ItemArmorMod;
import com.hbm_m.block.custom.machines.armormod.item.ItemModRadProtection;
import com.hbm_m.datagen.assets.ModItemTagProvider;
import com.hbm_m.hazard.HazardSystem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ArmorModificationHelper {

    public static final String MOD_COMPOUND_KEY = "hbm_armor_mods";
    public static final String MOD_SLOT_KEY_PREFIX = "mod_slot_";
    public static final String MODIFIER_MARKER_KEY = "hbm_mod_attribute";

    // Уникальные UUID для идентификации наших модификаторов
    public static final Map<Integer, UUID> MODIFIER_UUIDS = Map.of(
            0, UUID.fromString("8d6e5c77-133e-4056-9c80-a9e42a1a0b65"), // helmet
            1, UUID.fromString("b1b7ee0e-1d14-4400-8037-f7f2e02f21ca"), // chest
            2, UUID.fromString("30b50d2a-4858-4e5b-88d4-3e3612224238"), // legs
            3, UUID.fromString("426ee0d0-7587-4697-aaef-4772ab202e78")  // feet
    );

    public static void loadModsIntoTable(ItemStack armorStack, IItemHandler tableInventory) {
        // Очищаем стол перед загрузкой
        for (int i = 0; i < tableInventory.getSlots(); i++) {
            tableInventory.extractItem(i, tableInventory.getStackInSlot(i).getCount(), false);
        }

        if (!armorStack.hasTag() || !armorStack.getTag().contains(MOD_COMPOUND_KEY)) {
            return;
        }

        CompoundTag mods = armorStack.getTag().getCompound(MOD_COMPOUND_KEY);
        for (int i = 0; i < 9; i++) { // 9 слотов модов
            String key = MOD_SLOT_KEY_PREFIX + i;
            if (mods.contains(key)) {
                ItemStack modStack = ItemStack.of(mods.getCompound(key));
                tableInventory.insertItem(i, modStack, false);
            }
        }
    }


    public static void saveTableToArmor(ItemStack armorStack, IItemHandler tableInventory, Player player) {
        if (!(armorStack.getItem() instanceof ArmorItem armorItem)) {
            return;
        }

        // final Multimap<Attribute, AttributeModifier> oldAttributeModifiers = armorStack.getAttributeModifiers(armorItem.getEquipmentSlot());
        CompoundTag mainTag = armorStack.getOrCreateTag();
        CompoundTag modsCompound = new CompoundTag();

        // ШАГ 1: СОХРАНЕНИЕ КОНФИГУРАЦИИ МОДОВ В НАШ ТЕГ
        for (int i = 0; i < 9; i++) {
            ItemStack modStack = tableInventory.getStackInSlot(i);
            if (!modStack.isEmpty()) {
                modsCompound.put(MOD_SLOT_KEY_PREFIX + i, modStack.save(new CompoundTag()));
            }
        }
        if (modsCompound.isEmpty()) {
            mainTag.remove(MOD_COMPOUND_KEY);
        } else {
            mainTag.put(MOD_COMPOUND_KEY, modsCompound);
        }


        // ШАГ 2: БЕЗОПАСНАЯ ПЕРЕСБОРКА СПИСКА АТРИБУТОВ

        ListTag preservedModifiers = new ListTag();

        // 2a: Фильтруем существующие атрибуты. Сохраняем все, что не помечено нашим маркером.
        if (mainTag.contains("AttributeModifiers", 9)) { // 9 = ListTag.TAG_LIST
            ListTag currentModifiers = mainTag.getList("AttributeModifiers", 10); // 10 = CompoundTag.TAG_COMPOUND
            for (Tag tag : currentModifiers) {
                if (tag instanceof CompoundTag compoundTag) {
                    // Если у атрибута НЕТ нашего маркера, он от ванили или другого мода. Сохраняем его.
                    if (!compoundTag.getBoolean(MODIFIER_MARKER_KEY)) {
                        preservedModifiers.add(tag.copy());
                    }
                }
            }
        } else {
            // Если тега "AttributeModifiers" нет, добавляем ванильные по умолчанию. Они не будут иметь маркера.
            Multimap<Attribute, AttributeModifier> defaultModifiers = armorItem.getDefaultAttributeModifiers(armorItem.getEquipmentSlot());
            for (Map.Entry<Attribute, AttributeModifier> entry : defaultModifiers.entries()) {
                CompoundTag modifierTag = entry.getValue().save();
                modifierTag.putString("AttributeName", ForgeRegistries.ATTRIBUTES.getKey(entry.getKey()).toString());
                modifierTag.putString("Slot", armorItem.getEquipmentSlot().getName());
                preservedModifiers.add(modifierTag);
            }
        }

        // 2b: Добавляем атрибуты от наших модов, устанавливая им маркер.
        for (int i = 0; i < 9; i++) {
            ItemStack modStack = tableInventory.getStackInSlot(i);
            if (modStack.getItem() instanceof ItemArmorMod mod) {
                boolean isCompatible = switch (armorItem.getType()) {
                    case HELMET -> modStack.is(ModItemTagProvider.REQUIRES_HELMET);
                    case CHESTPLATE -> modStack.is(ModItemTagProvider.REQUIRES_CHESTPLATE);
                    case LEGGINGS -> modStack.is(ModItemTagProvider.REQUIRES_LEGGINGS);
                    case BOOTS -> modStack.is(ModItemTagProvider.REQUIRES_BOOTS);
                    default -> false;
                };

                if (isCompatible) {
                    Multimap<Attribute, AttributeModifier> modModifiers = mod.getModifiers(armorStack);
                    if (modModifiers != null) {
                        for (Map.Entry<Attribute, AttributeModifier> entry : modModifiers.entries()) {
                            CompoundTag modifierTag = entry.getValue().save();
                            modifierTag.putString("AttributeName", ForgeRegistries.ATTRIBUTES.getKey(entry.getKey()).toString());
                            modifierTag.putString("Slot", armorItem.getEquipmentSlot().getName());
                            // ВАЖНО: Добавляем наш маркер, чтобы идентифицировать этот атрибут в будущем.
                            modifierTag.putBoolean(MODIFIER_MARKER_KEY, true);
                            preservedModifiers.add(modifierTag);
                        }
                    }
                }
            }
        }

        // Заменяем старый список новым, отфильтрованным и дополненным.
        mainTag.put("AttributeModifiers", preservedModifiers);
        if (mainTag.isEmpty()) { armorStack.setTag(null); }
    }
    /**
     * Читает NBT-тег брони и возвращает список установленных модов.
     * @param armorStack Стак брони для проверки.
     * @return Список ItemStack'ов модов или пустой список, если модов нет.
     */
    public static List<ItemStack> getModsFromArmor(ItemStack armorStack) {
        if (!armorStack.hasTag() || !armorStack.getTag().contains(MOD_COMPOUND_KEY, 10)) {
            return Collections.emptyList();
        }

        CompoundTag modsCompound = armorStack.getTag().getCompound(MOD_COMPOUND_KEY);
        if (modsCompound.isEmpty()) {
            return Collections.emptyList();
        }

        List<ItemStack> modsList = new ArrayList<>();
        for (int i = 0; i < 9; i++) { // Проверяем все 9 слотов
            String key = MOD_SLOT_KEY_PREFIX + i;
            if (modsCompound.contains(key)) {
                ItemStack modStack = ItemStack.of(modsCompound.getCompound(key));
                if (!modStack.isEmpty()) {
                    modsList.add(modStack);
                }
            }
        }
        return modsList;
    }

    // Вычисляет СУММАРНУЮ абсолютную защиту от радиации для стака брони со всеми модами.
    
    public static float getTotalAbsoluteRadProtection(ItemStack armorStack) {
        if (!(armorStack.getItem() instanceof ArmorItem)) {
            return 0.0f;
        }
        
        // 1. Получаем базовую защиту от самой брони
        float totalProtection = HazardSystem.getArmorProtection(armorStack);

        // 2. Добавляем защиту от каждого установленного мода
        List<ItemStack> mods = getModsFromArmor(armorStack);
        for (ItemStack modStack : mods) {
            if (modStack.getItem() instanceof ItemModRadProtection radMod) {
                totalProtection += radMod.getProtectionValue();
            }
        }
        
        return totalProtection;
    }

    // Конвертирует абсолютное значение защиты в процент.

    public static float convertAbsoluteToPercent(float absoluteValue) {
        if (absoluteValue <= 0) return 0.0f;
        // Наша формула: 1 - e^(-2.3 * x)
        return (float) (1.0 - Math.exp(-2.35 * absoluteValue));
    }
}