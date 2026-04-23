package com.hbm_m.item.missile;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Прототип предмета ракеты для пусковых площадок.
 *
 * Логика максимально повторяет старый ItemMissile из 1.7.10,
 * но адаптирована под систему переводов 1.20.1.
 */
public class MissileItem extends Item {

    public final MissileFormFactor formFactor;
    public final MissileTier tier;
    public final MissileFuel fuel;
    public int fuelCap;
    public boolean launchable = true;

    public MissileItem(MissileFormFactor form, MissileTier tier) {
        this(form, tier, form.defaultFuel);
    }

    public MissileItem(MissileFormFactor form, MissileTier tier, MissileFuel fuel) {
        super(new Item.Properties());
        this.formFactor = form;
        this.tier = tier;
        this.fuel = fuel;
        this.setFuelCap(this.fuel.defaultCap);
    }

    public MissileItem notLaunchable() {
        this.launchable = false;
        return this;
    }

    public MissileItem setFuelCap(int fuelCap) {
        this.fuelCap = fuelCap;
        return this;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // Tier localized: missile.tier.tier0, missile.tier.tier1, ...
        String tierKey = "item.hbm_m.missile.tier." + this.tier.name().toLowerCase();
        tooltip.add(Component.translatable(tierKey).withStyle(ChatFormatting.ITALIC));

        if (!this.launchable) {
            tooltip.add(Component.translatable("item.hbm_m.missile.desc.notLaunchable").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("item.hbm_m.missile.desc.fuel")
                    .append(": ")
                    .append(this.fuel.getDisplay()));
            if (this.fuelCap > 0) {
                tooltip.add(Component.translatable("item.hbm_m.missile.desc.fuelCapacity")
                        .append(": ")
                        .append(String.valueOf(this.fuelCap))
                        .append("mB"));
            }
        }
    }

    public enum MissileFormFactor {
        ABM(MissileFuel.SOLID),
        MICRO(MissileFuel.SOLID),
        V2(MissileFuel.ETHANOL_PEROXIDE),
        STRONG(MissileFuel.KEROSENE_PEROXIDE),
        HUGE(MissileFuel.KEROSENE_LOXY),
        ATLAS(MissileFuel.JETFUEL_LOXY),
        OTHER(MissileFuel.KEROSENE_PEROXIDE);

        protected final MissileFuel defaultFuel;

        MissileFormFactor(MissileFuel defaultFuel) {
            this.defaultFuel = defaultFuel;
        }
    }

    public enum MissileTier {
        TIER0,
        TIER1,
        TIER2,
        TIER3,
        TIER4
    }

    public enum MissileFuel {
        SOLID("item.hbm_m.missile.fuel.solid.prefueled", ChatFormatting.GOLD, 0),
        ETHANOL_PEROXIDE("item.hbm_m.missile.fuel.ethanol_peroxide", ChatFormatting.AQUA, 4_000),
        KEROSENE_PEROXIDE("item.hbm_m.missile.fuel.kerosene_peroxide", ChatFormatting.BLUE, 8_000),
        KEROSENE_LOXY("item.hbm_m.missile.fuel.kerosene_loxy", ChatFormatting.LIGHT_PURPLE, 12_000),
        JETFUEL_LOXY("item.hbm_m.missile.fuel.jetfuel_loxy", ChatFormatting.RED, 16_000);

        private final String key;
        public final ChatFormatting color;
        public final int defaultCap;

        MissileFuel(String key, ChatFormatting color, int defaultCap) {
            this.key = key;
            this.color = color;
            this.defaultCap = defaultCap;
        }

        /** Возвращает цветной локализованный текст для всплывающей подсказки. */
        public Component getDisplay() {
            return Component.translatable(this.key).withStyle(this.color);
        }
    }
}

