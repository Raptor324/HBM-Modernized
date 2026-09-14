package com.hbm_m.item.machine;

import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.ModItems;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1-Port von {@code ItemICFPellet} (1.7.10): die Brennstoffkapsel des Traegheitsfusionsreaktors.
 *
 * <p>Eine Kapsel traegt <b>zwei</b> Brennstoffe, und die beiden bestimmen zusammen ihr Verhalten.
 * Der {@link EnumICFFuel#fusingDifficulty Zuendaufwand} beider wird multipliziert und ergibt die
 * Laserleistung, die noetig ist, damit die Kapsel ueberhaupt anspringt. Der
 * {@link EnumICFFuel#reactionMult Reaktionsfaktor} beider wird ebenfalls multipliziert und sagt,
 * wieviel Hitze aus der eingestrahlten Leistung wird. Die
 * {@link EnumICFFuel#depletionSpeed Abbrandgeschwindigkeit} teilt die Lebensdauer.</p>
 *
 * <p>Schwere Brennstoffe brauchen also viel mehr Laser, geben dafuer aber ein Vielfaches an Hitze
 * zurueck - Bor mit Zuendaufwand 3,5 ist die teuerste, Wasserstoff mit 1,0 die billigste Zuendung.
 * Eine <b>myonenkatalysierte</b> Kapsel viertelt den Zuendaufwand.</p>
 *
 * <p>Die Kapsel traegt die <b>Mischfarbe</b> ihrer beiden Brennstoffe: unten der eingefaerbte
 * Huellring, darueber die unveraenderte Pelletgrafik - man sieht einem Pellet also schon im
 * Inventar an, womit es gefuellt ist.</p>
 */
public class ItemICFPellet extends Item implements ITooltipProvider {

    /** Original: {@code KEY_NBT} - Abbrand, Brennstoffe und Myonenmarke stehen im NBT. */
    public static final String KEY_DEPLETION = "depletion";
    public static final String KEY_TYPE_1 = "type1";
    public static final String KEY_TYPE_2 = "type2";
    public static final String KEY_MUON = "muon";

    /** Original: {@code base = 50_000_000_000L}, geteilt durch beide Abbrandgeschwindigkeiten. */
    private static final long BASE_DEPLETION = 50_000_000_000L;
    /** Original: {@code base = 10_000_000L}, mal beiden Zuendaufwaenden. */
    private static final long BASE_DIFFICULTY = 10_000_000L;

    /** 1:1-Port von {@code EnumICFFuel}. Farbe, Reaktionsfaktor, Abbrand, Zuendaufwand. */
    public enum EnumICFFuel {
        HYDROGEN (0x4040FF, 1.00D, 0.85D, 1.00D),
        DEUTERIUM(0x2828CB, 1.25D, 1.00D, 1.00D),
        TRITIUM  (0x000092, 1.50D, 1.00D, 1.05D),
        HELIUM3  (0xFFF09F, 1.75D, 1.00D, 1.25D),
        HELIUM4  (0xFF9B60, 2.00D, 1.00D, 1.50D),
        LITHIUM  (0xE9E9E9, 1.25D, 0.85D, 2.00D),
        BERYLLIUM(0xA79D80, 2.00D, 1.00D, 2.50D),
        BORON    (0x697F89, 3.00D, 0.50D, 3.50D),
        CARBON   (0x454545, 2.00D, 1.00D, 5.00D),
        OXYGEN   (0xB4E2FF, 1.25D, 1.50D, 7.50D),
        SODIUM   (0xDFE4E7, 3.00D, 0.75D, 8.75D),
        CHLORINE (0xDAE598, 2.50D, 1.00D, 9.25D),
        CALCIUM  (0xD2C7A9, 3.00D, 1.00D, 9.75D);

        public final int color;
        public final double reactionMult;
        public final double depletionSpeed;
        public final double fusingDifficulty;

        EnumICFFuel(int color, double react, double depl, double laser) {
            this.color = color;
            this.reactionMult = react;
            this.depletionSpeed = depl;
            this.fusingDifficulty = laser;
        }

        public String getName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Original: {@code fluidMap} - welche Fluessigkeit welchen Brennstoff ergibt. */
    private static final Map<Fluid, EnumICFFuel> FLUID_MAP = new HashMap<>();
    /** Original: {@code materialMap} - dasselbe fuer feste Barren. */
    private static final Map<Item, EnumICFFuel> ITEM_MAP = new HashMap<>();
    private static boolean initialized = false;

    /**
     * 1:1-Port von {@code init}. Die festen Brennstoffe gehen im Original ueber das
     * Materialsystem ({@code Mats.getMaterialsFromItem} mit genau einem Barren); dieser Port hat
     * das nicht und prueft darum den Gegenstand unmittelbar. Lithium liegt hier als Kristall statt
     * als Barren vor, Natrium als eigenes {@code ingot_sodium}.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        FLUID_MAP.put(ModFluids.HYDROGEN.getSource(),  EnumICFFuel.HYDROGEN);
        FLUID_MAP.put(ModFluids.DEUTERIUM.getSource(), EnumICFFuel.DEUTERIUM);
        FLUID_MAP.put(ModFluids.TRITIUM.getSource(),   EnumICFFuel.TRITIUM);
        FLUID_MAP.put(ModFluids.HELIUM3.getSource(),   EnumICFFuel.HELIUM3);
        FLUID_MAP.put(ModFluids.HELIUM4.getSource(),   EnumICFFuel.HELIUM4);
        FLUID_MAP.put(ModFluids.OXYGEN.getSource(),    EnumICFFuel.OXYGEN);
        FLUID_MAP.put(ModFluids.CHLORINE.getSource(),  EnumICFFuel.CHLORINE);

        ITEM_MAP.put(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.CRYSTAL), EnumICFFuel.LITHIUM);
        ITEM_MAP.put(ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.INGOT), EnumICFFuel.BERYLLIUM);
        ITEM_MAP.put(ModMaterialItems.item(ModMaterials.BORON, MaterialShape.INGOT), EnumICFFuel.BORON);
        ITEM_MAP.put(ModMaterialItems.item(ModMaterials.GRAPHITE, MaterialShape.INGOT), EnumICFFuel.CARBON);
        ITEM_MAP.put(ModMaterialItems.item(ModMaterials.CALCIUM, MaterialShape.INGOT), EnumICFFuel.CALCIUM);
        ITEM_MAP.put(ModItems.INGOT_SODIUM.get(), EnumICFFuel.SODIUM);
    }

    @Nullable
    public static EnumICFFuel fuelFromFluid(Fluid fluid) {
        init();
        return FLUID_MAP.get(fluid);
    }

    @Nullable
    public static EnumICFFuel fuelFromItem(ItemStack stack) {
        init();
        return stack.isEmpty() ? null : ITEM_MAP.get(stack.getItem());
    }

    public ItemICFPellet(Properties properties) {
        super(properties.stacksTo(1));
    }

    // ── Kennzahlen ──────────────────────────────────────────────────────────

    /** Original: {@code getMaxDepletion}. */
    public static long getMaxDepletion(ItemStack stack) {
        long base = BASE_DEPLETION;
        base /= getType(stack, true).depletionSpeed;
        base /= getType(stack, false).depletionSpeed;
        return base;
    }

    /** Original: {@code getFusingDifficulty} - Myonenkatalyse viertelt den Aufwand. */
    public static long getFusingDifficulty(ItemStack stack) {
        long base = BASE_DIFFICULTY;
        base *= getType(stack, true).fusingDifficulty * getType(stack, false).fusingDifficulty;
        if (isMuon(stack)) base /= 4;
        return base;
    }

    public static long getDepletion(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0L : tag.getLong(KEY_DEPLETION);
    }

    /**
     * Original: {@code react} - die eingestrahlte Laserleistung wird als Abbrand angeschrieben und
     * mit dem Reaktionsfaktor beider Brennstoffe zu Hitze.
     */
    public static long react(ItemStack stack, long heat) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putLong(KEY_DEPLETION, tag.getLong(KEY_DEPLETION) + heat);
        return (long) (heat * getType(stack, true).reactionMult * getType(stack, false).reactionMult);
    }

    public static boolean isMuon(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(KEY_MUON);
    }

    /** Original: {@code setup} - eine frische Kapsel mit beiden Brennstoffen beschreiben. */
    public static ItemStack setup(EnumICFFuel type1, EnumICFFuel type2, boolean muon) {
        return setup(new ItemStack(ModItems.ICF_PELLET.get()), type1, type2, muon);
    }

    public static ItemStack setup(ItemStack stack, EnumICFFuel type1, EnumICFFuel type2, boolean muon) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putByte(KEY_TYPE_1, (byte) type1.ordinal());
        tag.putByte(KEY_TYPE_2, (byte) type2.ordinal());
        tag.putBoolean(KEY_MUON, muon);
        return stack;
    }

    /** Original: ohne NBT gilt eine Kapsel als Deuterium/Tritium. */
    /**
     * 1:1-Port von {@code getColorFromItemStack}: die untere Lage traegt den Mittelwert der
     * beiden Brennstofffarben - kanalweise gemittelt, nicht als Zahl. So sieht man einem Pellet
     * schon im Inventar an, womit es gefuellt ist.
     */
    public static int getMixedColor(ItemStack stack) {
        EnumICFFuel type1 = getType(stack, true);
        EnumICFFuel type2 = getType(stack, false);

        int r = (((type1.color & 0xff0000) >> 16) + ((type2.color & 0xff0000) >> 16)) / 2;
        int g = (((type1.color & 0x00ff00) >> 8) + ((type2.color & 0x00ff00) >> 8)) / 2;
        int b = ((type1.color & 0x0000ff) + (type2.color & 0x0000ff)) / 2;

        return r << 16 | g << 8 | b;
    }

    public static EnumICFFuel getType(ItemStack stack, boolean first) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return first ? EnumICFFuel.DEUTERIUM : EnumICFFuel.TRITIUM;

        int ordinal = tag.getByte(first ? KEY_TYPE_1 : KEY_TYPE_2);
        EnumICFFuel[] values = EnumICFFuel.values();
        if (ordinal < 0 || ordinal >= values.length) return first ? EnumICFFuel.DEUTERIUM : EnumICFFuel.TRITIUM;
        return values[ordinal];
    }

    // ── Anzeige ─────────────────────────────────────────────────────────────

    private static double depletionFraction(ItemStack stack) {
        long max = getMaxDepletion(stack);
        return max <= 0 ? 0D : (double) getDepletion(stack) / (double) max;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return depletionFraction(stack) > 0D;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        double left = 1D - depletionFraction(stack);
        return (int) Math.round(13D * Math.max(0D, Math.min(1D, left)));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        // Die Mischfarbe der beiden Brennstoffe, wie im Original der Beutel.
        EnumICFFuel a = getType(stack, true);
        EnumICFFuel b = getType(stack, false);
        int r = (((a.color & 0xFF0000) >> 16) + ((b.color & 0xFF0000) >> 16)) / 2;
        int g = (((a.color & 0x00FF00) >> 8) + ((b.color & 0x00FF00) >> 8)) / 2;
        int bl = ((a.color & 0xFF) + (b.color & 0xFF)) / 2;
        return r << 16 | g << 8 | bl;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hbm_m.icf_pellet.depletion",
                String.format(Locale.US, "%.1f", depletionFraction(stack) * 100D))
                .withStyle(ChatFormatting.GREEN));

        tooltip.add(Component.translatable("tooltip.hbm_m.icf_pellet.fuel",
                Component.translatable("icffuel.hbm_m." + getType(stack, true).getName()),
                Component.translatable("icffuel.hbm_m." + getType(stack, false).getName()))
                .withStyle(ChatFormatting.YELLOW));

        tooltip.add(Component.translatable("tooltip.hbm_m.icf_pellet.heat", getFusingDifficulty(stack))
                .withStyle(ChatFormatting.YELLOW));

        double mult = getType(stack, true).reactionMult * getType(stack, false).reactionMult;
        tooltip.add(Component.translatable("tooltip.hbm_m.icf_pellet.reactivity",
                String.format(Locale.US, "%.2f", mult))
                .withStyle(ChatFormatting.YELLOW));

        if (isMuon(stack)) {
            tooltip.add(Component.translatable("tooltip.hbm_m.icf_pellet.muon")
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
