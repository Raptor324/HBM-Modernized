package com.hbm_m.inventory.filter;

import java.util.List;

import com.hbm_m.item.industrial.ItemBedrockOreGraded;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.module.ModulePatternMatcher}: je Filterplatz ein Vergleichsmodus. {@link #MODE_EXACT} = Item und
 * Metadaten (hier: Schaden) gleich, {@link #MODE_WILDCARD} = Item gleich, {@link #MODE_BEDROCK} = Bedrock-Erz gleichen
 * Grades, sonst ein Ore-Dictionary-Schluessel - im Port der Item-Tag ({@link #MODE_TAG_PREFIX} + Tag-ID).
 */
public class ModulePatternMatcher {

    public static final String MODE_EXACT = "exact";
    public static final String MODE_WILDCARD = "wildcard";
    public static final String MODE_BEDROCK = "bedrock";
    public static final String MODE_TAG_PREFIX = "tag:";

    private final String[] modes;

    public ModulePatternMatcher() { this(1); }

    public ModulePatternMatcher(int size) {
        this.modes = new String[size];
    }

    public int size() { return modes.length; }
    public String getMode(int index) { return modes[index]; }
    public void setMode(int index, String mode) { modes[index] = mode; }

    /** Ore-Dictionary-Namen des Originals: die Item-Tags, sortiert fuer eine feste Reihenfolge. */
    public static List<String> getOreDictNames(ItemStack stack) {
        return stack.getTags().map(t -> MODE_TAG_PREFIX + t.location()).sorted().toList();
    }

    /** Original {@code initPatternSmart}: bevorzugt Barren/Block/Staub/Nugget/Platte-Schluessel. */
    public void initPatternSmart(int i, ItemStack stack) {
        if (stack.isEmpty()) {
            modes[i] = null;
            return;
        }

        List<String> names = getOreDictNames(stack);

        if (iterateAndCheck(names, i, "forge:ingots/")) return;
        if (iterateAndCheck(names, i, "forge:storage_blocks/")) return;
        if (iterateAndCheck(names, i, "forge:dusts/")) return;
        if (iterateAndCheck(names, i, "forge:nuggets/")) return;
        if (iterateAndCheck(names, i, "forge:plates/")) return;

        initPattern(i, stack);
    }

    private boolean iterateAndCheck(List<String> names, int i, String prefix) {
        for (String s : names) {
            if (s.startsWith(MODE_TAG_PREFIX + prefix)) {
                modes[i] = s;
                return true;
            }
        }
        return false;
    }

    /**
     * Original {@code initPatternStandard}: Bedrock-Erz -> Grade, Items mit Untertypen -> exakt, sonst Wildcard. Im Port
     * sind die Untertypen eigene Items; Untertypen im Sinne des Originals haben daher nur Items ohne Haltbarkeit.
     */
    public void initPattern(int i, ItemStack stack) {
        if (stack.isEmpty()) {
            modes[i] = null;
            return;
        }

        if (stack.getItem() instanceof ItemBedrockOreGraded) {
            modes[i] = MODE_BEDROCK;
        } else if (!stack.isDamageableItem()) {
            modes[i] = MODE_EXACT;
        } else {
            modes[i] = MODE_WILDCARD;
        }
    }

    /** Original {@code nextMode}: exakt -> (Grade) -> Wildcard -> Tag 1 -> ... -> Tag n -> exakt. */
    public void nextMode(int i, ItemStack pattern) {
        if (pattern.isEmpty()) {
            modes[i] = null;
            return;
        }

        if (modes[i] == null) {
            modes[i] = MODE_EXACT;
        } else if (MODE_EXACT.equals(modes[i])) {
            modes[i] = pattern.getItem() instanceof ItemBedrockOreGraded ? MODE_BEDROCK : MODE_WILDCARD;
        } else if (MODE_BEDROCK.equals(modes[i])) {
            modes[i] = MODE_WILDCARD;
        } else if (MODE_WILDCARD.equals(modes[i])) {
            List<String> names = getOreDictNames(pattern);
            modes[i] = names.isEmpty() ? MODE_EXACT : names.get(0);
        } else {
            List<String> names = getOreDictNames(pattern);
            if (names.size() < 2 || modes[i].equals(names.get(names.size() - 1))) {
                modes[i] = MODE_EXACT;
            } else {
                for (int j = 0; j < names.size() - 1; j++) {
                    if (modes[i].equals(names.get(j))) {
                        modes[i] = names.get(j + 1);
                        return;
                    }
                }
                modes[i] = MODE_EXACT;
            }
        }
    }

    public boolean isValidForFilter(ItemStack filter, int index, ItemStack input) {
        if (index < 0 || index >= modes.length) return false;
        String mode = modes[index];

        if (mode == null) {
            modes[index] = mode = MODE_EXACT;
        }

        switch (mode) {
            case MODE_EXACT:
                return input.getItem() == filter.getItem() && input.getDamageValue() == filter.getDamageValue();
            case MODE_WILDCARD:
                return input.getItem() == filter.getItem();
            case MODE_BEDROCK:
                if (input.getItem() != filter.getItem() && !(input.getItem() instanceof ItemBedrockOreGraded)) return false;
                if (!(input.getItem() instanceof ItemBedrockOreGraded in) || !(filter.getItem() instanceof ItemBedrockOreGraded f)) return false;
                return in.getGrade() == f.getGrade();
            default:
                if (!mode.startsWith(MODE_TAG_PREFIX)) return false;
                ResourceLocation tagId = ResourceLocation.tryParse(mode.substring(MODE_TAG_PREFIX.length()));
                if (tagId == null) return false;
                return input.is(TagKey.create(net.minecraft.core.registries.Registries.ITEM, tagId));
        }
    }

    public static String getLabel(String mode) {
        if (mode == null) return "";
        switch (mode) {
            case MODE_EXACT: return ChatFormatting.YELLOW + "Item and meta match";
            case MODE_WILDCARD: return ChatFormatting.YELLOW + "Item matches";
            case MODE_BEDROCK: return ChatFormatting.YELLOW + "Item and bedrock grade match";
            default: return ChatFormatting.YELLOW + "Ore dict key matches: " + (mode.startsWith(MODE_TAG_PREFIX) ? mode.substring(MODE_TAG_PREFIX.length()) : mode);
        }
    }

    public void readFromNBT(CompoundTag nbt) {
        for (int i = 0; i < modes.length; i++) {
            modes[i] = nbt.contains("mode" + i) ? nbt.getString("mode" + i) : null;
        }
    }

    public void writeToNBT(CompoundTag nbt) {
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] != null) nbt.putString("mode" + i, modes[i]);
        }
    }

    public void serialize(FriendlyByteBuf buf) {
        for (String mode : modes) buf.writeUtf(mode == null ? "" : mode);
    }

    public void deserialize(FriendlyByteBuf buf) {
        for (int i = 0; i < modes.length; i++) {
            String s = buf.readUtf();
            modes[i] = s.isEmpty() ? null : s;
        }
    }
}
