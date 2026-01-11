package com.hbm_m.powerarmor;

import com.hbm_m.armormod.item.ItemArmorMod;
import com.hbm_m.armormod.util.ArmorModificationHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ArmorTooltipHandler {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.###");

    // Map to track which items belong to which armor sets (similar to original itemInfoSet)
    private static final HashMap<Item, List<DamageResistanceHandler.ArmorSet>> itemToSetsMap = new HashMap<>();

    static {
        // Initialize the item-to-sets mapping based on registered armor sets
        for (Map.Entry<DamageResistanceHandler.ArmorSet, DamageResistanceHandler.ResistanceStats> entry : DamageResistanceHandler.setStats.entrySet()) {
            DamageResistanceHandler.ArmorSet set = entry.getKey();
            if (set.helmet != null) addItemToSet(set.helmet, set);
            if (set.chestplate != null) addItemToSet(set.chestplate, set);
            if (set.leggings != null) addItemToSet(set.leggings, set);
            if (set.boots != null) addItemToSet(set.boots, set);
        }
    }

    private static void addItemToSet(Item item, DamageResistanceHandler.ArmorSet set) {
        itemToSetsMap.computeIfAbsent(item, k -> new ArrayList<>()).add(set);
    }

    /**
     * Генерирует строку сопротивления радиации (желтая, внизу).
     */
    public static Optional<Component> getRadResistanceTooltip(ItemStack stack) {
        float absoluteProtection = ArmorModificationHelper.getTotalAbsoluteRadProtection(stack);
        if (absoluteProtection > 0) {
            MutableComponent tooltipLine = Component.translatable("tooltip.hbm_m.rad_protection.value",
                    DECIMAL_FORMAT.format(absoluteProtection));
            return Optional.of(tooltipLine.withStyle(ChatFormatting.YELLOW));
        }
        return Optional.empty();
    }

    /**
     * ЕДИНАЯ подсказка "Зажмите Shift".
     */
    public static Optional<Component> getContextualHelpTooltip(ItemStack stack, boolean isAlwaysVisible) {
        if (Screen.hasShiftDown() || isAlwaysVisible) {
            return Optional.empty();
        }

        boolean hasMods = false;
        if (stack.hasTag() && stack.getTag().contains(ArmorModificationHelper.MOD_COMPOUND_KEY, 10)) {
            CompoundTag modsCompound = stack.getTag().getCompound(ArmorModificationHelper.MOD_COMPOUND_KEY);
            hasMods = !modsCompound.isEmpty();
        }

        boolean hasHiddenFSBStats = stack.getItem() instanceof ModPowerArmorItem;

        if (hasMods || hasHiddenFSBStats) {
            MutableComponent text = Component.translatable("tooltip.hbm_m.hold_shift_for_details");
            return Optional.of(text.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        return Optional.empty();
    }

    /**
     * Тултип списка модификаций.
     */
    public static Optional<List<Component>> getModificationsTooltip(ItemStack stack, boolean isAlwaysVisible) {
        if (!isAlwaysVisible && !Screen.hasShiftDown()) {
            return Optional.empty();
        }

        if (!stack.hasTag() || !stack.getTag().contains(ArmorModificationHelper.MOD_COMPOUND_KEY, 10)) {
            return Optional.empty();
        }
        CompoundTag modsCompound = stack.getTag().getCompound(ArmorModificationHelper.MOD_COMPOUND_KEY);
        if (modsCompound.isEmpty()) {
            return Optional.empty();
        }
        
        List<Component> modsTooltipLines = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            String key = ArmorModificationHelper.MOD_SLOT_KEY_PREFIX + i;
            if (modsCompound.contains(key)) {
                ItemStack modStack = ItemStack.of(modsCompound.getCompound(key));
                if (!modStack.isEmpty() && modStack.getItem() instanceof ItemArmorMod) {
                    ItemArmorMod mod = (ItemArmorMod) modStack.getItem();
                    
                    MutableComponent modLine = modStack.getHoverName().copy().withStyle(ChatFormatting.AQUA);
                    List<Component> effectLines = mod.getEffectTooltipLines();
                    
                    if (!effectLines.isEmpty()) {
                        MutableComponent effectsComponent = Component.literal(" (").withStyle(ChatFormatting.GRAY);
                        boolean firstEffect = true;
                        for (Component effectLine : effectLines) {
                            if (!firstEffect) effectsComponent.append(", ");
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
     * Тултип БОНУСОВ полного набора (Способности/Эффекты).
     */
    public static Optional<List<Component>> getFSBTooltip(ItemStack stack) {
        if (!(stack.getItem() instanceof ModArmorFSB)) {
            return Optional.empty();
        }
        ModArmorFSB armorItem = (ModArmorFSB) stack.getItem();

        List<Component> fsbLines = new ArrayList<>();
        
        // Potion Effects
        if (!armorItem.effects.isEmpty()) {
            for (MobEffectInstance effect : armorItem.effects) {
                MutableComponent effectName = Component.translatable(effect.getDescriptionId());
                if (effect.getAmplifier() > 0) {
                    effectName = Component.literal("").append(effectName).append(" ").append(Component.translatable("potion.potency." + effect.getAmplifier()));
                }
                fsbLines.add(Component.literal("  ").append(effectName).withStyle(ChatFormatting.AQUA));
            }
        }

        // Capabilities
        addAbilityLine(fsbLines, armorItem.hasGeigerSound, "armor.fsb.geigerCounter", ChatFormatting.GOLD);
        addAbilityLine(fsbLines, armorItem.customGeiger, "armor.fsb.geigerHUD", ChatFormatting.GOLD);
        addAbilityLine(fsbLines, armorItem.vats, "armor.fsb.vats", ChatFormatting.RED);
        addAbilityLine(fsbLines, armorItem.thermal, "armor.fsb.thermalVision", ChatFormatting.RED);
        addAbilityLine(fsbLines, armorItem.hardLanding, "armor.fsb.hardLanding", ChatFormatting.RED);
        
        if (armorItem.stepSize != 0) {
            fsbLines.add(Component.literal("  ").append(Component.translatable("armor.fsb.enhancedMobility")).withStyle(ChatFormatting.BLUE));
        }
        if (armorItem.dashCount > 0) {
            fsbLines.add(Component.literal("  ").append(Component.translatable("armor.fsb.dashAbility")).append(" (" + armorItem.dashCount + ")").withStyle(ChatFormatting.AQUA));
        }

        // Power Armor overrides for abilities
        if (stack.getItem() instanceof ModPowerArmorItem) {
            ModPowerArmorItem powerArmor = (ModPowerArmorItem) stack.getItem();
            var specs = powerArmor.getSpecs();
            if (specs != null) {
                // Power Armor overrides for abilities
                if (specs.hasGeigerSound && !armorItem.hasGeigerSound) addAbilityLine(fsbLines, true, "armor.fsb.geigerCounter", ChatFormatting.GOLD);
                if (specs.hasCustomGeiger && !armorItem.customGeiger) addAbilityLine(fsbLines, true, "armor.fsb.geigerHUD", ChatFormatting.GOLD);
                if (specs.hasVats && !armorItem.vats) addAbilityLine(fsbLines, true, "armor.fsb.vats", ChatFormatting.RED);
                if (specs.hasThermal && !armorItem.thermal) addAbilityLine(fsbLines, true, "armor.fsb.thermalVision", ChatFormatting.RED);
                if (specs.hasHardLanding && !armorItem.hardLanding) addAbilityLine(fsbLines, true, "armor.fsb.hardLanding", ChatFormatting.RED);

                if (specs.stepSize != 0 && armorItem.stepSize == 0) {
                    fsbLines.add(Component.literal("  ").append(Component.translatable("armor.fsb.stepSize")).withStyle(ChatFormatting.BLUE));
                }
                if (specs.dashCount > 0 && armorItem.dashCount == 0) {
                    fsbLines.add(Component.literal("  ").append(Component.translatable("armor.fsb.dash")).append(" (" + specs.dashCount + ")").withStyle(ChatFormatting.AQUA));
                }
            }
        }

        if (!fsbLines.isEmpty()) {
            List<Component> result = new ArrayList<>();
            result.add(Component.translatable("tooltip.hbm_m.fsb_bonus").withStyle(ChatFormatting.GOLD));
            result.addAll(fsbLines);
            return Optional.of(result);
        }

        return Optional.empty();
    }

    /**
     * Тултип СОПРОТИВЛЕНИЙ (Resistances) полного набора.
     * Адаптирован под оригинальную систему сопротивлений HBM 1.7.10
     */
    public static Optional<List<Component>> getFSBResistancesTooltip(ItemStack stack) {
        if (!Screen.hasShiftDown()) {
            return Optional.empty();
        }

        Item item = stack.getItem();
        if (item == null) {
            return Optional.empty();
        }

        List<Component> result = new ArrayList<>();

        // Check for set resistances (similar to original addInfo method)
        if (itemToSetsMap.containsKey(item)) {
            List<DamageResistanceHandler.ArmorSet> sets = itemToSetsMap.get(item);

            for (DamageResistanceHandler.ArmorSet set : sets) {
                DamageResistanceHandler.ResistanceStats stats = DamageResistanceHandler.setStats.get(set);
                if (stats == null) continue;

                List<Component> toAdd = new ArrayList<>();

                // Add category resistances
                for (Map.Entry<String, DamageResistanceHandler.Resistance> entry : stats.categoryResistances.entrySet()) {
                    toAdd.add(Component.literal(getDamageCategoryName(entry.getKey()) + ": " +
                        entry.getValue().threshold + "/" + ((int)(entry.getValue().resistance * 100)) + "%")
                        .withStyle(ChatFormatting.GRAY));
                }

                // Add exact resistances
                for (Map.Entry<String, DamageResistanceHandler.Resistance> entry : stats.exactResistances.entrySet()) {
                    toAdd.add(Component.literal(getDamageExactName(entry.getKey()) + ": " +
                        entry.getValue().threshold + "/" + ((int)(entry.getValue().resistance * 100)) + "%")
                        .withStyle(ChatFormatting.GRAY));
                }

                // Add other resistance
                if (stats.otherResistance != null) {
                    toAdd.add(Component.literal(Component.translatable("damage.other").getString() + ": " +
                        stats.otherResistance.threshold + "/" + ((int)(stats.otherResistance.resistance * 100)) + "%")
                        .withStyle(ChatFormatting.GRAY));
                }

                if (!toAdd.isEmpty()) {
                    result.add(Component.translatable("damage.inset").withStyle(ChatFormatting.DARK_PURPLE));

                    // Add armor piece names
                    if (set.helmet != null) result.add(Component.literal("  ").append(Component.translatable(set.helmet.getDescriptionId())).withStyle(ChatFormatting.DARK_PURPLE));
                    if (set.chestplate != null) result.add(Component.literal("  ").append(Component.translatable(set.chestplate.getDescriptionId())).withStyle(ChatFormatting.DARK_PURPLE));
                    if (set.leggings != null) result.add(Component.literal("  ").append(Component.translatable(set.leggings.getDescriptionId())).withStyle(ChatFormatting.DARK_PURPLE));
                    if (set.boots != null) result.add(Component.literal("  ").append(Component.translatable(set.boots.getDescriptionId())).withStyle(ChatFormatting.DARK_PURPLE));

                    result.addAll(toAdd);
                }

                break; // Only show one set for now
            }
        }

        // Check for individual item resistances
        if (DamageResistanceHandler.itemStats.containsKey(item)) {
            DamageResistanceHandler.ResistanceStats stats = DamageResistanceHandler.itemStats.get(item);

            List<Component> toAdd = new ArrayList<>();

            // Add category resistances
            for (Map.Entry<String, DamageResistanceHandler.Resistance> entry : stats.categoryResistances.entrySet()) {
                toAdd.add(Component.literal(getDamageCategoryName(entry.getKey()) + ": " +
                    entry.getValue().threshold + "/" + ((int)(entry.getValue().resistance * 100)) + "%")
                    .withStyle(ChatFormatting.GRAY));
            }

            // Add exact resistances
            for (Map.Entry<String, DamageResistanceHandler.Resistance> entry : stats.exactResistances.entrySet()) {
                toAdd.add(Component.literal(getDamageExactName(entry.getKey()) + ": " +
                    entry.getValue().threshold + "/" + ((int)(entry.getValue().resistance * 100)) + "%")
                    .withStyle(ChatFormatting.GRAY));
            }

            // Add other resistance
            if (stats.otherResistance != null) {
                toAdd.add(Component.literal(Component.translatable("damage.other").getString() + ": " +
                    stats.otherResistance.threshold + "/" + ((int)(stats.otherResistance.resistance * 100)) + "%")
                    .withStyle(ChatFormatting.GRAY));
            }

            if (!toAdd.isEmpty()) {
                if (result.isEmpty()) {
                    result.add(Component.translatable("damage.item").withStyle(ChatFormatting.DARK_PURPLE));
                }
                result.addAll(toAdd);
            }
        }

        return result.isEmpty() ? Optional.empty() : Optional.of(result);
    }

    private static String getDamageCategoryName(String category) {
        return switch (category) {
            case "EXPL" -> Component.translatable("damage.category.EXPL").getString();
            case "FIRE" -> Component.translatable("damage.category.FIRE").getString();
            case "PHYS" -> Component.translatable("damage.category.PHYS").getString();
            case "EN" -> Component.translatable("damage.category.EN").getString();
            default -> category;
        };
    }

    private static String getDamageExactName(String exact) {
        return switch (exact) {
            case "fall" -> Component.translatable("damage.exact.fall").getString();
            default -> exact;
        };
    }

    private static void addAbilityLine(List<Component> list, boolean condition, String translationKey, ChatFormatting color) {
        if (condition) {
            list.add(Component.literal("  ").append(Component.translatable(translationKey)).withStyle(color));
        }
    }

    private static void addResistanceLine(List<Component> list, String key, float val, float cap) {
        if (val > 0 || cap > 0) {
            String v = DECIMAL_FORMAT.format(val);
            String c = String.format("%.0f", cap * 100); 
            
            MutableComponent line = Component.translatable(key);
            line.append(": " + v + "/" + c + "%");
            
            list.add(line.withStyle(ChatFormatting.GRAY));
        }
    }
}