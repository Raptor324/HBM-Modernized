package com.hbm_m.item.industrial;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.worldgen.BedrockOreDensity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Порт {@code ItemBedrockOreBase} (оригинал 1.7.10): Raw Bedrock Ore.
 * В оригинале addInformation выводит 6 строк параметров ("Light Metal: 1.05 (Moderate)"),
 * значения берутся из NBT стека (шум по координатам игрока записывается при создании
 * стека в креатив-вкладке). В современных версиях стек креатив-вкладки создаётся без
 * игрока/NBT, а getOreAmount стека без NBT возвращает 0 — поэтому, как и в оригинале
 * для стека без NBT, выводим фиксированный дефолт 0.0 с квалификатором "Very Poor"
 * (пороги квалификаторов: ItemOreDensityScanner.translateDensity: <=0.1 Very Poor,
 * <=0.35 Poor, <=0.75 Low, >=1.25 High, >=1.65 Very High, >=1.9 Excellent, иначе Moderate).
 */
public class ItemBedrockOreBase extends Item implements ITooltipProvider {

    /** Квалификаторы плотности из ItemOreDensityScanner, по возрастанию порогов. */
    private static final double[] DENSITY_THRESHOLDS = {0.1, 0.35, 0.75, 1.25, 1.65, 1.9};
    private static final String[] DENSITY_KEYS = {
            "verypoor", "poor", "low", "moderate", "high", "veryhigh", "excellent"};

    public ItemBedrockOreBase(Properties properties) {
        super(properties);
    }

    private static String densityKey(double density) {
        for (int i = 0; i < DENSITY_THRESHOLDS.length; i++) {
            if (density <= DENSITY_THRESHOLDS[i]) return DENSITY_KEYS[i];
        }
        return DENSITY_KEYS[DENSITY_KEYS.length - 1];
    }

    /** Строка параметра одного типа руды, формат оригинала: "Type: amount (qualifier)". */
    public static MutableComponent typeLine(BedrockOreDensity.Type type, double amount) {
        String typeKey = "item.hbm_m.bedrock_ore.type." + type.name().toLowerCase(Locale.ROOT) + ".name";
        // Форматирование числа как в оригинале: ((int)(amount * 100)) / 100D
        String amountText = String.valueOf((int) (amount * 100) / 100D);
        return Component.translatable(typeKey)
                .append(": " + amountText + " (")
                .append(Component.translatable("item.hbm_m.ore_density_scanner." + densityKey(amount))
                        .withStyle(ChatFormatting.DARK_RED))
                .append(Component.literal(")").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void appendHbmTooltip(@NotNull ItemStack stack, @Nullable Level level,
                                 @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        for (BedrockOreDensity.Type type : BedrockOreDensity.Type.values()) {
            tooltip.add(typeLine(type, 0));
        }
    }
}
