package com.hbm_m.item.missile;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code ItemCustomMissilePart}: ein Bauteil der Baukasten-Rakete (Chip, Sprengkopf, Rumpf, Leitwerk, Triebwerk).
 * Die Attribute haengen vom Typ ab:
 * <ul>
 * <li>Chip: [0] Ungenauigkeit</li>
 * <li>Sprengkopf: [0] {@link WarheadType}, [1] Staerke/Radius/Anzahl, [2] Gewicht</li>
 * <li>Rumpf: [0] {@link FuelType}, [1] Tankgroesse</li>
 * <li>Leitwerk: [0] Ungenauigkeit</li>
 * <li>Triebwerk: [0] {@link FuelType}, [1] Verbrauch, [2] Nutzlast</li>
 * </ul>
 */
public class ItemCustomMissilePart extends Item {

    public enum PartType { CHIP, WARHEAD, FUSELAGE, FINS, THRUSTER }

    public enum PartSize {
        /** for chips */
        ANY,
        /** for missile tips and thrusters */
        NONE,
        /** regular sizes, 1.0m, 1.5m and 2.0m */
        SIZE_10, SIZE_15, SIZE_20
    }

    public enum WarheadType {
        HE, INC, BUSTER, CLUSTER, NUCLEAR, TX, N2, BALEFIRE, SCHRAB, TAINT, CLOUD, TURBINE,
        CUSTOM0, CUSTOM1, CUSTOM2, CUSTOM3, CUSTOM4, CUSTOM5, CUSTOM6, CUSTOM7, CUSTOM8, CUSTOM9;

        /** Overrides that type's impact effect. Only runs serverside */
        public Consumer<com.hbm_m.entity.missile.EntityMissileCustom> impactCustom = null;
        /** Runs at the beginning of the missile's update cycle, both client and serverside. */
        public Consumer<com.hbm_m.entity.missile.EntityMissileCustom> updateCustom = null;
        /** Override for the warhead's name in the missile description */
        public String labelCustom = null;
    }

    public enum FuelType { KEROSENE, SOLID, HYDROGEN, XENON, BALEFIRE }

    public enum Rarity {
        COMMON("item.missile.part.rarity.common", ChatFormatting.GRAY),
        UNCOMMON("item.missile.part.rarity.uncommon", ChatFormatting.YELLOW),
        RARE("item.missile.part.rarity.rare", ChatFormatting.AQUA),
        EPIC("item.missile.part.rarity.epic", ChatFormatting.LIGHT_PURPLE),
        LEGENDARY("item.missile.part.rarity.legendary", ChatFormatting.DARK_GREEN),
        SEWS_CLOTHES_AND_SUCKS_HORSE_COCK("item.missile.part.rarity.strange", ChatFormatting.DARK_AQUA);

        private final String key;
        private final ChatFormatting color;

        Rarity(String key, ChatFormatting color) {
            this.key = key;
            this.color = color;
        }

        public Component getDisplay() {
            return Component.translatable(key).withStyle(color);
        }
    }

    public final PartType type;
    public final PartSize top;
    public final PartSize bottom;
    public final Object[] attributes;
    public final float health;
    @Nullable public final Rarity rarity;
    @Nullable private final String title;
    @Nullable private final String author;
    @Nullable private final String witty;

    public ItemCustomMissilePart(Properties props, PartType type, PartSize top, PartSize bottom, Object[] attributes, float health,
                                 @Nullable Rarity rarity, @Nullable String title, @Nullable String author, @Nullable String witty) {
        super(props.stacksTo(1));
        this.type = type;
        this.top = top;
        this.bottom = bottom;
        this.attributes = attributes;
        this.health = health;
        this.rarity = rarity;
        this.title = title;
        this.author = author;
        this.witty = witty;
    }

    private static MutableComponent line(String key, Component value) {
        return Component.translatable(key).withStyle(ChatFormatting.BOLD).append(Component.literal(": ").withStyle(ChatFormatting.BOLD)).append(value.copy().withStyle(s -> s.withBold(false)));
    }

    private static Component gray(String s) {
        return Component.literal(s).withStyle(ChatFormatting.GRAY);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {

        if (title != null) list.add(Component.literal("\"" + title + "\"").withStyle(ChatFormatting.DARK_PURPLE));

        try {
            switch (type) {
                case CHIP -> list.add(line("item.missile.part.inaccuracy", gray((Float) attributes[0] * 100 + "%")));
                case WARHEAD -> {
                    list.add(line("item.missile.part.size", getSize(bottom)));
                    list.add(line("item.missile.part.type", getWarhead((WarheadType) attributes[0])));
                    list.add(line("item.missile.part.strength", gray("" + attributes[1])));
                    list.add(line("item.missile.part.weight", gray(attributes[2] + "t")));
                }
                case FUSELAGE -> {
                    list.add(line("item.missile.part.topSize", getSize(top)));
                    list.add(line("item.missile.part.bottomSize", getSize(bottom)));
                    list.add(line("item.missile.part.fuelType", getFuel((FuelType) attributes[0])));
                    list.add(line("item.missile.part.fuelAmount", gray(attributes[1] + "l")));
                }
                case FINS -> {
                    list.add(line("item.missile.part.size", getSize(top)));
                    list.add(line("item.missile.part.inaccuracy", gray((Float) attributes[0] * 100 + "%")));
                }
                case THRUSTER -> {
                    list.add(line("item.missile.part.size", getSize(top)));
                    list.add(line("item.missile.part.fuelType", getFuel((FuelType) attributes[0])));
                    list.add(line("item.missile.part.fuelConsumption", gray(attributes[1] + "l/tick")));
                    list.add(line("item.missile.part.maxPayload", gray(attributes[2] + "t")));
                }
            }
        } catch (Exception ex) {
            list.add(Component.translatable("error.generic"));
        }

        if (type != PartType.CHIP) list.add(line("item.missile.part.health", gray(health + "HP")));

        if (this.rarity != null) list.add(line("item.missile.part.rarity", this.rarity.getDisplay()));
        if (author != null) list.add(Component.literal("   ").append(Component.translatable("item.missile.part.by")).append(" " + author).withStyle(ChatFormatting.WHITE));
        if (witty != null) list.add(Component.literal("   \"" + witty + "\"").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
    }

    public static Component getSize(PartSize size) {
        return switch (size) {
            case ANY -> Component.translatable("item.missile.part.size.any").withStyle(ChatFormatting.GRAY);
            case SIZE_10 -> gray("1.0m");
            case SIZE_15 -> gray("1.5m");
            case SIZE_20 -> gray("2.0m");
            default -> Component.translatable("item.missile.part.size.none").withStyle(ChatFormatting.GRAY);
        };
    }

    public static Component getWarhead(WarheadType type) {
        if (type.labelCustom != null) return Component.literal(type.labelCustom);
        return switch (type) {
            case HE -> Component.translatable("item.warhead.desc.he").withStyle(ChatFormatting.YELLOW);
            case INC -> Component.translatable("item.warhead.desc.incendiary").withStyle(ChatFormatting.GOLD);
            case CLUSTER -> Component.translatable("item.warhead.desc.cluster").withStyle(ChatFormatting.GRAY);
            case BUSTER -> Component.translatable("item.warhead.desc.bunker_buster").withStyle(ChatFormatting.WHITE);
            case NUCLEAR -> Component.translatable("item.warhead.desc.nuclear").withStyle(ChatFormatting.DARK_GREEN);
            case TX -> Component.translatable("item.warhead.desc.thermonuclear").withStyle(ChatFormatting.DARK_PURPLE);
            case N2 -> Component.translatable("item.warhead.desc.n2").withStyle(ChatFormatting.RED);
            case BALEFIRE -> Component.translatable("item.warhead.desc.balefire").withStyle(ChatFormatting.GREEN);
            case SCHRAB -> Component.translatable("item.warhead.desc.schrabidium").withStyle(ChatFormatting.AQUA);
            case TAINT -> Component.translatable("item.warhead.desc.taint").withStyle(ChatFormatting.DARK_PURPLE);
            case CLOUD -> Component.translatable("item.warhead.desc.cloud").withStyle(ChatFormatting.LIGHT_PURPLE);
            case TURBINE -> Component.translatable("item.warhead.desc.turbine").withStyle(System.currentTimeMillis() % 1000 < 500 ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE);
            default -> Component.translatable("general.na").withStyle(ChatFormatting.BOLD);
        };
    }

    public static Component getFuel(FuelType type) {
        return switch (type) {
            case KEROSENE -> Component.translatable("item.missile.fuel.kerosene_peroxide").withStyle(ChatFormatting.LIGHT_PURPLE);
            case SOLID -> Component.translatable("item.missile.fuel.solid").withStyle(ChatFormatting.GOLD);
            case HYDROGEN -> Component.translatable("item.missile.fuel.hydrogen").withStyle(ChatFormatting.DARK_AQUA);
            case XENON -> Component.translatable("item.missile.fuel.xenon").withStyle(ChatFormatting.DARK_PURPLE);
            case BALEFIRE -> Component.translatable("item.missile.fuel.balefire").withStyle(ChatFormatting.GREEN);
        };
    }
}
