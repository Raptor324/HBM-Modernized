package com.hbm_m.item.industrial;

import com.hbm_m.item.ITooltipProvider;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Апгрейд машины — порт ItemMachineUpgrade из 1.7.10.
 *
 * Каждый предмет имеет {@link UpgradeType} и числовой tier
 * (Mk.I = 1, Mk.II = 2, Mk.III = 3). Уровень суммируется
 * в {@link com.hbm_m.inventory.UpgradeManager}.
 */
public class ItemMachineUpgrade extends Item implements ITooltipProvider {

    public enum UpgradeType {
        SPEED,
        EFFECT,
        POWER,
        FORTUNE,
        AFTERBURN,
        OVERDRIVE,
        STACK,
        EJECTOR,
        /** Kraftfeld: Radius +16, Verbrauch +500 je Stueck im Stapel. */
        RADIUS,
        /** Kraftfeld: Schild +50, Verbrauch +250 je Stueck im Stapel. */
        HEALTH;

        public String getTranslationKeySuffix() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private final UpgradeType type;
    private final int tier;

    public ItemMachineUpgrade(Properties properties, UpgradeType type, int tier) {
        super(properties.stacksTo(1));
        this.type = type;
        this.tier = tier;
    }

    public ItemMachineUpgrade(Properties properties, UpgradeType type) {
        this(properties, type, 0);
    }

    /**
     * Fuer Aufwertungen, die sich stapeln duerfen. Im Original sind das nur die beiden des
     * Kraftfelds, deren Wirkung sich nach der Stapelgroesse richtet statt nach einer Stufe.
     */
    public ItemMachineUpgrade(Properties properties, UpgradeType type, int tier, int maxStackSize) {
        super(properties.stacksTo(maxStackSize));
        this.type = type;
        this.tier = tier;
    }

    public UpgradeType getUpgradeType() {
        return type;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // Die beiden Kraftfeldaufwertungen haben im Original einen eigenen Hinweistext.
        if (type == UpgradeType.RADIUS || type == UpgradeType.HEALTH) {
            String key = type.getTranslationKeySuffix();
            tooltip.add(Component.translatable("tooltip.hbm_m.upgrade." + key + ".title")
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("tooltip.hbm_m.upgrade." + key + ".desc")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.hbm_m.upgrade.stacks", 16)
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        tooltip.add(Component.translatable("tooltip.hbm_m.upgrade.type." + type.getTranslationKeySuffix())
                .withStyle(ChatFormatting.GRAY));
        if (tier > 0) {
            tooltip.add(Component.translatable("tooltip.hbm_m.upgrade.tier", tier)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
