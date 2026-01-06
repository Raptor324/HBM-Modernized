package com.hbm_m.armormod.util;

// Хелпер для управления модификациями брони: сохранение/загрузка из NBT,
// применение атрибутов, вычисление защиты от радиации и т.п.
import com.google.common.collect.Multimap;
import com.hbm_m.armormod.item.ItemArmorMod;
import com.hbm_m.armormod.item.ItemModRadProtection;
import com.hbm_m.datagen.ModItemTagProvider;
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

    // Типы слотов модификаций (как в оригинальном ArmorModHandler)
    public static final int helmet_only = 0;
    public static final int plate_only = 1;
    public static final int legs_only = 2;
    public static final int boots_only = 3;
    public static final int servos = 4;
    public static final int cladding = 5;
    public static final int kevlar = 6;
    public static final int extra = 7;
    public static final int battery = 8;

    public static final int MOD_SLOTS = 9;

    public static final String MOD_COMPOUND_KEY = "hbm_armor_mods";
    public static final String MOD_SLOT_KEY_PREFIX = "mod_slot_";
    public static final String MODIFIER_MARKER_KEY = "hbm_mod_attribute";

    // Уникальные UUID для идентификации наших модификаторов (расширено)
    public static final Map<Integer, UUID> MODIFIER_UUIDS = Map.of(
            0, UUID.fromString("8d6e5c77-133e-4056-9c80-a9e42a1a0b65"), // helmet
            1, UUID.fromString("b1b7ee0e-1d14-4400-8037-f7f2e02f21ca"), // chest
            2, UUID.fromString("30b50d2a-4858-4e5b-88d4-3e3612224238"), // legs
            3, UUID.fromString("426ee0d0-7587-4697-aaef-4772ab202e78"),  // feet
            4, UUID.fromString("e572caf4-3e65-4152-bc79-c4d4048cbd29"),  // servos
            5, UUID.fromString("bed30902-8a6a-4769-9f65-2a9b67469fff"),  // cladding
            6, UUID.fromString("baebf7b3-1eda-4a14-b233-068e2493e9a2"),  // kevlar
            7, UUID.fromString("28016c1b-d992-4324-9409-a9f9f0ffb85c"),  // extra
            8, UUID.fromString("f1c2d3e4-a5b6-4c7d-8e9f-0a1b2c3d4e5f")   // battery
    );

    /**
     * Проверяет, можно ли применить модификацию к данной броне
     * @param armor Броня для проверки
     * @param mod Модификация для проверки
     * @return true если модификацию можно применить
     */
    public static boolean isApplicable(ItemStack armor, ItemStack mod) {
        if (armor.isEmpty() || mod.isEmpty()) {
            return false;
        }

        if (!(armor.getItem() instanceof ArmorItem)) {
            return false;
        }

        if (!(mod.getItem() instanceof ItemArmorMod modItem)) {
            return false;
        }

        ArmorItem armorItem = (ArmorItem) armor.getItem();
        int armorSlot = armorItem.getType().getSlot().getIndex();
        int modType = modItem.type;

        // Проверяем совместимость по типу брони и модификации
        switch (modType) {
            case helmet_only -> {
                return armorSlot == 3; // HEAD
            }
            case plate_only -> {
                return armorSlot == 2; // CHEST
            }
            case legs_only -> {
                return armorSlot == 1; // LEGS
            }
            case boots_only -> {
                return armorSlot == 0; // FEET
            }
            case servos, cladding, kevlar, extra, battery -> {
                // Эти модификации могут применяться к любой броне
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * Применяет модификацию к броне
     * @param armor Броня для модификации
     * @param mod Модификация для применения
     */
    public static void applyMod(ItemStack armor, ItemStack mod) {
        if (!isApplicable(armor, mod)) {
            return;
        }

        if (!(mod.getItem() instanceof ItemArmorMod modItem)) {
            return;
        }

        CompoundTag armorTag = armor.getOrCreateTag();
        CompoundTag modsTag = armorTag.getCompound(MOD_COMPOUND_KEY);

        // Сохраняем ItemStack модификации в NBT
        CompoundTag modTag = new CompoundTag();
        mod.save(modTag);

        int slot = modItem.type;
        modsTag.put(MOD_SLOT_KEY_PREFIX + slot, modTag);
        armorTag.put(MOD_COMPOUND_KEY, modsTag);
    }

    /**
     * Удаляет модификацию из указанного слота
     * @param armor Броня
     * @param slot Слот для очистки
     */
    public static void removeMod(ItemStack armor, int slot) {
        if (armor.isEmpty()) {
            return;
        }

        CompoundTag armorTag = armor.getTag();
        if (armorTag == null || !armorTag.contains(MOD_COMPOUND_KEY)) {
            return;
        }

        CompoundTag modsTag = armorTag.getCompound(MOD_COMPOUND_KEY);
        modsTag.remove(MOD_SLOT_KEY_PREFIX + slot);

        // Если модификаций не осталось, удаляем весь compound
        if (modsTag.isEmpty()) {
            armorTag.remove(MOD_COMPOUND_KEY);
        } else {
            armorTag.put(MOD_COMPOUND_KEY, modsTag);
        }
    }

    /**
     * Проверяет, есть ли модификации у брони
     * @param armor Броня для проверки
     * @return true если есть модификации
     */
    public static boolean hasMods(ItemStack armor) {
        if (armor.isEmpty()) {
            return false;
        }

        CompoundTag armorTag = armor.getTag();
        return armorTag != null && armorTag.contains(MOD_COMPOUND_KEY);
    }

    /**
     * Получает все модификации из брони
     * @param armor Броня
     * @return Массив ItemStack модификаций
     */
    public static ItemStack[] pryMods(ItemStack armor) {
        ItemStack[] slots = new ItemStack[MOD_SLOTS];

        if (!hasMods(armor)) {
            return slots;
        }

        CompoundTag armorTag = armor.getTag();
        if (armorTag == null) {
            return slots;
        }

        CompoundTag modsTag = armorTag.getCompound(MOD_COMPOUND_KEY);

        for (int i = 0; i < MOD_SLOTS; i++) {
            String key = MOD_SLOT_KEY_PREFIX + i;
            if (modsTag.contains(key)) {
                slots[i] = ItemStack.of(modsTag.getCompound(key));
            }
        }

        return slots;
    }

    /**
     * Получает модификацию из конкретного слота
     * @param armor Броня
     * @param slot Слот
     * @return ItemStack модификации или пустой стек
     */
    public static ItemStack pryMod(ItemStack armor, int slot) {
        if (!hasMods(armor)) {
            return ItemStack.EMPTY;
        }

        CompoundTag armorTag = armor.getTag();
        if (armorTag == null) {
            return ItemStack.EMPTY;
        }

        CompoundTag modsTag = armorTag.getCompound(MOD_COMPOUND_KEY);
        String key = MOD_SLOT_KEY_PREFIX + slot;

        if (modsTag.contains(key)) {
            return ItemStack.of(modsTag.getCompound(key));
        }

        return ItemStack.EMPTY;
    }

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

        // ШАГ 3: ОБРЕЗАНИЕ ЭНЕРГИИ ПО НОВОМУ МАКСИМУМУ
        // Если броня - силовая, проверяем и корректируем уровень энергии
        if (armorStack.getItem() instanceof com.hbm_m.item.armor.ModPowerArmorItem powerArmor) {
            long currentEnergy = mainTag.getLong("energy");
            long newMaxCapacity = powerArmor.getModifiedCapacity(armorStack);

            // Если текущая энергия превышает новый максимум, обрезаем ее
            if (currentEnergy > newMaxCapacity) {
                mainTag.putLong("energy", newMaxCapacity);
            }
        }
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