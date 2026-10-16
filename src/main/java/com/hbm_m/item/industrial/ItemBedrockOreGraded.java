package com.hbm_m.item.industrial;

import com.hbm_m.item.ITooltipProvider;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.worldgen.BedrockOreDensity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Eine Veredelungsstufe der Bedrock-Ore-Progression (Mining Drill). 1:1-Port der Namens-/Tooltip-
 * Logik aus {@code ItemBedrockOreNew} (Original-Repo, 1.7.10): Anzeigename = Grade-Vorlage mit dem
 * Typnamen als Platzhalter ("Actinide Bedrock Ore, Roasted Primary Fraction"), Tooltip = eine Zeile
 * pro {@link Trait} des Grades (Faerbung/Text bereits in den lang-Werten enthalten, wie im Original).
 * Die eigentliche Verarbeitungslogik (welche Maschine welchen Grade in welchen ueberfuehrt) lebt in
 * den jeweiligen Rezept-Registrierungen (Centrifuge/Crystallizer/Combination Oven/Arc Furnace), nicht
 * hier - dieser Enum traegt nur Anzeige-Metadaten, exakt wie im Original.
 */
public class ItemBedrockOreGraded extends Item implements ITooltipProvider {

    public enum Trait {
        ROASTED, ARC, WASHED, CENTRIFUGED, SULFURIC, SOLVENT, RAD;

        public String langKey() {
            return "tooltip.hbm_m.bedrock_ore.trait." + name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Grade {
        BASE("base"),
        BASE_ROASTED("base_roasted", Trait.ROASTED),
        BASE_WASHED("base_washed", Trait.WASHED),
        PRIMARY("primary", Trait.CENTRIFUGED),
        PRIMARY_ROASTED("primary_roasted", Trait.ROASTED),
        PRIMARY_SULFURIC("primary_sulfuric", Trait.SULFURIC),
        PRIMARY_NOSULFURIC("primary_nosulfuric", Trait.CENTRIFUGED, Trait.SULFURIC),
        PRIMARY_SOLVENT("primary_solvent", Trait.SOLVENT),
        PRIMARY_NOSOLVENT("primary_nosolvent", Trait.CENTRIFUGED, Trait.SOLVENT),
        PRIMARY_RAD("primary_rad", Trait.RAD),
        PRIMARY_NORAD("primary_norad", Trait.CENTRIFUGED, Trait.RAD),
        PRIMARY_FIRST("primary_first", Trait.CENTRIFUGED),
        PRIMARY_SECOND("primary_second", Trait.CENTRIFUGED),
        CRUMBS("crumbs", Trait.CENTRIFUGED),
        SULFURIC_BYPRODUCT("sulfuric_byproduct", Trait.CENTRIFUGED, Trait.SULFURIC),
        SULFURIC_ROASTED("sulfuric_roasted", Trait.ROASTED, Trait.SULFURIC),
        SULFURIC_ARC("sulfuric_arc", Trait.ARC, Trait.SULFURIC),
        SULFURIC_WASHED("sulfuric_washed", Trait.WASHED, Trait.SULFURIC),
        SOLVENT_BYPRODUCT("solvent_byproduct", Trait.CENTRIFUGED, Trait.SOLVENT),
        SOLVENT_ROASTED("solvent_roasted", Trait.ROASTED, Trait.SOLVENT),
        SOLVENT_ARC("solvent_arc", Trait.ARC, Trait.SOLVENT),
        SOLVENT_WASHED("solvent_washed", Trait.WASHED, Trait.SOLVENT),
        RAD_BYPRODUCT("rad_byproduct", Trait.CENTRIFUGED, Trait.RAD),
        RAD_ROASTED("rad_roasted", Trait.ROASTED, Trait.RAD),
        RAD_ARC("rad_arc", Trait.ARC, Trait.RAD),
        RAD_WASHED("rad_washed", Trait.WASHED, Trait.RAD);

        public final String key;
        public final Trait[] traits;

        Grade(String key, Trait... traits) {
            this.key = key;
            this.traits = traits;
        }
    }

    private final Grade grade;
    private final BedrockOreDensity.Type type;

    public ItemBedrockOreGraded(Properties properties, Grade grade, BedrockOreDensity.Type type) {
        super(properties);
        this.grade = grade;
        this.type = type;
    }

    public Grade getGrade() {
        return grade;
    }

    public BedrockOreDensity.Type getOreType() {
        return type;
    }

    private static String typeKey(BedrockOreDensity.Type type) {
        return "item.hbm_m.bedrock_ore.type." + type.name().toLowerCase(Locale.ROOT) + ".name";
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        Component typeName = Component.translatable(typeKey(type));
        return Component.translatable("item.hbm_m.bedrock_ore.grade." + grade.key + ".name", typeName);
    }

    @Override
    public void appendHbmTooltip(@NotNull ItemStack stack, @Nullable Level level,
                                 @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        for (Trait trait : grade.traits) {
            tooltip.add(Component.translatable(trait.langKey()));
        }
    }
}
