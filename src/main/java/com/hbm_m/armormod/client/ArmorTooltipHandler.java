package com.hbm_m.armormod.client;

// Этот класс отвечает за генерацию компонентов тултипа для брони с модификациями
import com.hbm_m.armormod.item.ItemArmorMod;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.item.armor.ModPowerArmorItem;
import com.hbm_m.item.armor.PowerArmorSpecs;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArmorTooltipHandler {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.###");

    /**
     * Генерирует и возвращает компонент для строки "Сопротивление радиации".
     * @param stack Стак брони
     * @return Optional, содержащий Component, или пустой, если защиты нет.
     */
    public static Optional<Component> getRadResistanceTooltip(ItemStack stack) {
        float absoluteProtection = ArmorModificationHelper.getTotalAbsoluteRadProtection(stack);
        if (absoluteProtection > 0) {
            Component tooltipLine = Component.translatable("tooltip.hbm_m.rad_protection.value",
                    DECIMAL_FORMAT.format(absoluteProtection))
                    .withStyle(ChatFormatting.YELLOW);
            return Optional.of(tooltipLine);
        }
        return Optional.empty();
    }

    /**
     * Генерирует и возвращает ПОЛНЫЙ блок тултипа для списка модификаций.
     * @param stack Стак брони
     * @param isAlwaysVisible Если true, покажет список всегда. Если false, только при нажатии SHIFT.
     * @return Optional, содержащий список Component'ов, или пустой.
     */
    public static Optional<List<Component>> getModificationsTooltip(ItemStack stack, boolean isAlwaysVisible) {
        if (!stack.hasTag() || !stack.getTag().contains(ArmorModificationHelper.MOD_COMPOUND_KEY, 10)) {
            return Optional.empty();
        }
        CompoundTag modsCompound = stack.getTag().getCompound(ArmorModificationHelper.MOD_COMPOUND_KEY);
        if (modsCompound.isEmpty()) {
            return Optional.empty();
        }

        if (!isAlwaysVisible && !Screen.hasShiftDown()) {
            return Optional.of(List.of(Component.translatable("tooltip.hbm_m.hold_shift_for_details").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        }
        
        List<Component> modsTooltipLines = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            String key = ArmorModificationHelper.MOD_SLOT_KEY_PREFIX + i;
            if (modsCompound.contains(key)) {
                ItemStack modStack = ItemStack.of(modsCompound.getCompound(key));
                // Добавляем проверку, что это наш ItemArmorMod
                if (!modStack.isEmpty() && modStack.getItem() instanceof ItemArmorMod mod) {
                    
                    // 1. Берем чистое имя мода.
                    MutableComponent modLine = modStack.getHoverName().copy().withStyle(ChatFormatting.AQUA);
                    
                    // 2. Вызываем наш новый, безопасный метод, чтобы получить ТОЛЬКО строки с эффектами.
                    List<Component> effectLines = mod.getEffectTooltipLines();
                    
                    // 3. Если эффекты есть, форматируем их в скобки.
                    if (!effectLines.isEmpty()) {
                        MutableComponent effectsComponent = Component.literal(" (").withStyle(ChatFormatting.GRAY);
                        boolean firstEffect = true;
                        
                        for (Component effectLine : effectLines) {
                            if (!firstEffect) {
                                effectsComponent.append(", ");
                            }
                            effectsComponent.append(effectLine.copy());
                            firstEffect = false;
                        }
                        effectsComponent.append(")");
                        
                        modLine.append(effectsComponent);
                    }

                    modsTooltipLines.add(Component.literal("  ").append(modLine));
                }
            }
        }

        if (!modsTooltipLines.isEmpty()) {
            List<Component> result = new ArrayList<>();
            result.add(Component.translatable("tooltip.hbm_m.mods").withStyle(ChatFormatting.GOLD));
            result.addAll(modsTooltipLines);
            result.add(Component.empty());
            return Optional.of(result);
        }

        return Optional.empty();
    }

    /**
     * Генерирует тултип для эффектов Full Set Bonus (FSB) силовой брони.
     * Портировано из оригинального addInformation() метода ArmorFSB.java
     *
     * @param stack ItemStack брони
     * @return Optional со списком компонентов тултипа, или пустой если эффектов нет
     */
    public static Optional<List<Component>> getFSBTooltip(ItemStack stack) {
        if (!(stack.getItem() instanceof ModPowerArmorItem armorItem)) {
            return Optional.empty();
        }

        PowerArmorSpecs specs = armorItem.getSpecs();
        List<Component> fsbLines = new ArrayList<>();

        // Эффекты зелий (potion effects)
        if (!specs.passiveEffects.isEmpty()) {
            List<String> potionList = new ArrayList<>();
            for (var effect : specs.passiveEffects) {
                // В 1.20.1 эффект может иметь registry name
                String effectName = effect.getEffect().getDescriptionId();
                potionList.add(Component.translatable(effectName).getString());
            }

            if (!potionList.isEmpty()) {
                fsbLines.add(Component.literal(String.join(", ", potionList))
                    .withStyle(ChatFormatting.AQUA));
            }
        }

        // Специальные эффекты FSB
        if (specs.hasGeigerSound) {
            fsbLines.add(Component.literal("  ")
                .append(Component.translatable("armor.geigerSound"))
                .withStyle(ChatFormatting.GOLD));
        }

        if (specs.hasCustomGeiger) {
            fsbLines.add(Component.literal("  ")
                .append(Component.translatable("armor.geigerHUD"))
                .withStyle(ChatFormatting.GOLD));
        }

        if (specs.hasVats) {
            fsbLines.add(Component.literal("  ")
                .append(Component.translatable("armor.vats"))
                .withStyle(ChatFormatting.RED));
        }

        if (specs.hasThermal) {
            fsbLines.add(Component.literal("  ")
                .append(Component.translatable("armor.thermal"))
                .withStyle(ChatFormatting.RED));
        }

        if (specs.hasHardLanding) {
            fsbLines.add(Component.literal("  ")
                .append(Component.translatable("armor.hardLanding"))
                .withStyle(ChatFormatting.RED));
        }

        if (specs.stepSize != 0) {
            fsbLines.add(Component.literal("  ")
                .append(Component.translatable("armor.stepSize", specs.stepSize))
                .withStyle(ChatFormatting.BLUE));
        }

        if (specs.dashCount > 0) {
            fsbLines.add(Component.literal("  ")
                .append(Component.translatable("armor.dash", specs.dashCount))
                .withStyle(ChatFormatting.AQUA));
        }

        if (!fsbLines.isEmpty()) {
            List<Component> result = new ArrayList<>();
            result.add(Component.translatable("armor.fullSetBonus")
                .withStyle(ChatFormatting.GOLD));
            result.addAll(fsbLines);
            return Optional.of(result);
        }

        return Optional.empty();
    }
}