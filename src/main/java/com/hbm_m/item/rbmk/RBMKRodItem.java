package com.hbm_m.item.rbmk;

import com.hbm_m.blockentity.machines.rbmk.IRBMKFluxReceiver.NType;
import com.hbm_m.handler.rbmk.RBMKDials;
import com.hbm_m.platform.PlatformHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Function;

public class RBMKRodItem extends Item {

    // ─── Static registry of all pellet-derived rods ───────────────────────────

    /** All rods created from a pellet — used by recipe generation and creative tabs. */
    public static final List<RBMKRodItem> craftableRods = new ArrayList<>();

    // ─── Item properties ─────────────────────────────────────────────────────

    public final String fullName;
    public double reactivity;
    public double selfRate      = 0;
    public EnumBurnFunc function     = EnumBurnFunc.LOG_TEN;
    public EnumDepleteFunc depFunc   = EnumDepleteFunc.GENTLE_SLOPE;
    public double xGen          = 0.5;
    public double xBurn         = 50;
    public double heat          = 1.0;
    public double yield;
    public double meltingPoint  = 1000;
    public double diffusion     = 0.02;
    public NType nType          = NType.SLOW;
    public NType rType          = NType.FAST;
    public int colorTint        = 0x304825;
    public double heatCoeffStart  = 0;
    public double heatCoeffLength = 0;
    public boolean specialFluxCurve = false;

    private BiFunction<Double, Double, Double> ratioCurve;
    private BiFunction<Double, Double, Double> fluxCurve;

    // ─── Constructors ─────────────────────────────────────────────────────────

    public RBMKRodItem(RBMKPelletItem pellet, Properties props) {
        super(props.stacksTo(1));
        this.fullName   = pellet.fullName;
        this.yield      = pellet.yield;
        this.reactivity = pellet.reactivity;
        RBMKPelletItem.applyToRod(pellet, this);
        craftableRods.add(this);
    }

    public RBMKRodItem(String fullName, Properties props) {
        super(props.stacksTo(1));
        this.fullName = fullName;
    }

    // ─── Craft remainder (returns empty rod casing) ───────────────────────────

    //? if < 1.21.1 {
    // @Override omitted intentionally — Stonecutter removes this block for >= 1.21.1
    public boolean hasCraftingRemainingItem(ItemStack stack) { return true; }
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return new ItemStack(com.hbm_m.item.ModItems.RBMK_FUEL_EMPTY.get());
    }
    //?} else {
    /*@Override public boolean hasCraftingRemainingItem() { return true; }
    @Override public ItemStack getCraftingRemainingItem() {
        return new ItemStack(com.hbm_m.item.ModItems.RBMK_FUEL_EMPTY.get());
    }
    *///?}

    // ─── Builder setters ──────────────────────────────────────────────────────

    public RBMKRodItem setStats(double reactivity)                    { this.reactivity = reactivity; return this; }
    public RBMKRodItem setStats(double reactivity, double selfRate)   { this.reactivity = reactivity; this.selfRate = selfRate; return this; }
    public RBMKRodItem setYield(double yield)                         { this.yield = yield; return this; }
    public RBMKRodItem setFunction(EnumBurnFunc f)                    { this.function = f; return this; }
    public RBMKRodItem setDepletionFunction(EnumDepleteFunc f)        { this.depFunc = f; return this; }
    public RBMKRodItem setHeat(double heat)                           { this.heat = heat; return this; }
    public RBMKRodItem setDiffusion(double d)                         { this.diffusion = d; return this; }
    public RBMKRodItem setMeltingPoint(double mp)                     { this.meltingPoint = mp; return this; }
    public RBMKRodItem setXenon(double gen, double burn)              { this.xGen = gen; this.xBurn = burn; return this; }
    public RBMKRodItem setNeutronTypes(NType n, NType r)              { this.nType = n; this.rType = r; return this; }
    public RBMKRodItem setTint(int tint)                              { this.colorTint = tint; return this; }
    public RBMKRodItem setHeatCoeff(double start, double length)      { this.heatCoeffStart = start; this.heatCoeffLength = length; return this; }

    public RBMKRodItem setOutputRatioCurve(Function<Double, Double> func) {
        this.specialFluxCurve = true;
        this.ratioCurve = (ratio, dep) -> func.apply(ratio);
        return this;
    }

    public RBMKRodItem setOutputFluxCurve(BiFunction<Double, Double, Double> func) {
        this.fluxCurve = func;
        return this;
    }

    // ─── Simulation ──────────────────────────────────────────────────────────

    public double burn(Level world, ItemStack stack, double inFlux) {
        inFlux += selfRate;

        if (RBMKDials.getXenon(world)) {
            double xenon = getPoison(stack);
            xenon -= xenonBurnFunc(inFlux);
            inFlux *= (1.0 - getPoisonLevel(stack));
            xenon += xenonGenFunc(inFlux);
            setPoison(stack, Mth.clamp(xenon, 0, 100));
        }

        double mult = 1.0;
        double coreHeat = getCoreHeat(stack);
        if (heatCoeffStart != 0 && coreHeat >= heatCoeffStart) {
            double prog = Math.min(1, (coreHeat - heatCoeffStart) / heatCoeffLength);
            mult = Math.sin((prog * Math.PI + Math.PI) / 2);
        }

        double outFlux = reactivityFunc(inFlux, getEnrichment(stack) * mult) * RBMKDials.getReactivityMod(world);

        if (RBMKDials.getDepletion(world)) {
            double y = Math.max(0, getYield(stack) - inFlux);
            setYield(stack, y);
        }

        setCoreHeat(stack, rectify(coreHeat + outFlux * heat));
        return outFlux;
    }

    public void updateHeat(Level world, ItemStack stack, double mod) {
        double core = getCoreHeat(stack);
        double hull = getHullHeat(stack);
        if (core > hull) {
            double mid = (core - hull) / 2.0;
            double step = mid * diffusion * RBMKDials.getFuelDiffusionMod(world) * mod;
            setCoreHeat(stack, rectify(core - step));
            setHullHeat(stack, rectify(hull + step));
        }
    }

    public double provideHeat(Level world, ItemStack stack, double columnHeat, double mod) {
        double hull = getHullHeat(stack);
        if (hull > meltingPoint) {
            double core = getCoreHeat(stack);
            double avg = (columnHeat + hull + core) / 3.0;
            setCoreHeat(stack, avg);
            setHullHeat(stack, avg);
            return avg - columnHeat;
        }
        if (hull <= columnHeat) return 0;
        double ret = (hull - columnHeat) / 2.0 * RBMKDials.getFuelHeatProvision(world) * mod;
        setHullHeat(stack, hull - ret);
        return ret;
    }

    // ─── Flux math ────────────────────────────────────────────────────────────

    public double reactivityFunc(double in, double enrichment) {
        double flux = in * reactivityModByEnrichment(enrichment);
        return switch (function) {
            case PASSIVE      -> selfRate * enrichment;
            case LOG_TEN      -> Math.log10(flux + 1) * 0.5 * reactivity;
            case PLATEU       -> (1 - Math.pow(Math.E, -flux / 25.0)) * reactivity;
            case ARCH         -> Math.max((flux - (flux * flux / 10000.0)) / 100.0 * reactivity, 0);
            case SIGMOID      -> reactivity / (1 + Math.pow(Math.E, -(flux - 50.0) / 10.0));
            case SQUARE_ROOT  -> Math.sqrt(flux) * reactivity / 10.0;
            case LINEAR       -> flux / 100.0 * reactivity;
            case QUADRATIC    -> flux * flux / 10000.0 * reactivity;
            case EXPERIMENTAL -> flux * (Math.sin(flux) + 1) * reactivity;
        };
    }

    public double reactivityModByEnrichment(double e) {
        return switch (depFunc) {
            case LINEAR        -> e;
            case STATIC        -> 1.0;
            case BOOSTED_SLOPE -> e + Math.sin((e - 1) * (e - 1) * Math.PI);
            case RAISING_SLOPE -> e + Math.sin(e * Math.PI) / 2.0;
            case GENTLE_SLOPE  -> e + Math.sin(e * Math.PI) / 3.0;
        };
    }

    public double xenonGenFunc(double flux)  { return flux * xGen; }
    public double xenonBurnFunc(double flux) { return (flux * flux) / xBurn; }

    public double fluxRatioOut(double fluxRatioIn, double depletion) {
        return ratioCurve != null ? Mth.clamp(ratioCurve.apply(fluxRatioIn, depletion), 0, 1) : (rType == NType.SLOW ? 0 : 1);
    }

    public double fluxFromRatio(double quantity, double ratio) {
        return fluxCurve != null ? fluxCurve.apply(quantity, ratio) : quantity;
    }

    private double rectify(double v) {
        if (Double.isNaN(v) || v < 20) return 20;
        return Math.min(v, 1_000_000);
    }

    // ─── Flux function description ────────────────────────────────────────────

    /** Returns a human-readable mathematical formula for the flux function, with current enrichment applied. */
    public String getFuncDescription(ItemStack stack) {
        String formula = switch (function) {
            case PASSIVE      -> ChatFormatting.RED + "" + selfRate;
            case LOG_TEN      -> "log10(%1$s + 1) * 0.5 * %2$s";
            case PLATEU       -> "(1 - e^(-%1$s / 25)) * %2$s";
            case ARCH         -> "(%1$s - %1$s² / 10000) / 100 * %2$s [0;∞]";
            case SIGMOID      -> "%2$s / (1 + e^(-(%1$s - 50) / 10))";
            case SQUARE_ROOT  -> "sqrt(%1$s) * %2$s / 10";
            case LINEAR       -> "%1$s / 100 * %2$s";
            case QUADRATIC    -> "%1$s² / 10000 * %2$s";
            case EXPERIMENTAL -> "%1$s * (sin(%1$s) + 1) * %2$s";
        };

        double enrichment = getEnrichment(stack);
        String xArg = selfRate > 0
            ? "(x" + ChatFormatting.RED + " + " + selfRate + ChatFormatting.WHITE + ")"
            : "x";

        if (enrichment < 1.0) {
            double em = reactivityModByEnrichment(enrichment);
            String rStr = ChatFormatting.YELLOW + "" + ((int)(reactivity * em * 1000) / 1000.0) + ChatFormatting.WHITE;
            String ePct = ChatFormatting.GOLD + " (" + ((int)(em * 1000) / 10.0) + "%)";
            return String.format(Locale.US, formula, xArg, rStr) + ePct;
        }
        return String.format(Locale.US, formula, xArg, reactivity);
    }

    // ─── NBT helpers ─────────────────────────────────────────────────────────

    //? if < 1.21.1 {
    private static CompoundTag getOrCreateTag(ItemStack stack) {
        if (!PlatformHooks.hasItemTag(stack)) {
            CompoundTag tag = new CompoundTag();
            if (stack.getItem() instanceof RBMKRodItem rod) tag.putDouble("yield", rod.yield);
            tag.putDouble("core", 20.0);
            tag.putDouble("hull", 20.0);
            PlatformHooks.setItemTag(stack, tag);
        }
        return stack.getOrCreateTag();
    }
    public static double getYield(ItemStack s)           { return getOrCreateTag(s).getDouble("yield"); }
    public static void   setYield(ItemStack s, double v) { getOrCreateTag(s).putDouble("yield", v); }
    public static double getPoison(ItemStack s)          { return getOrCreateTag(s).getDouble("xenon"); }
    public static void   setPoison(ItemStack s, double v){ getOrCreateTag(s).putDouble("xenon", v); }
    public static double getCoreHeat(ItemStack s)        { return getOrCreateTag(s).getDouble("core"); }
    public static void   setCoreHeat(ItemStack s, double v){ getOrCreateTag(s).putDouble("core", v); }
    public static double getHullHeat(ItemStack s)        { return getOrCreateTag(s).getDouble("hull"); }
    public static void   setHullHeat(ItemStack s, double v){ getOrCreateTag(s).putDouble("hull", v); }
    //?} else {
    /*private static CompoundTag readTag(ItemStack stack) {
        net.minecraft.world.item.component.CustomData data =
            stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data != null) return data.copyTag();
        CompoundTag tag = new CompoundTag();
        if (stack.getItem() instanceof RBMKRodItem rod) tag.putDouble("yield", rod.yield);
        tag.putDouble("core", 20.0);
        tag.putDouble("hull", 20.0);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.of(tag));
        return tag;
    }
    private static void saveTag(ItemStack stack, CompoundTag tag) {
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.of(tag));
    }
    public static double getYield(ItemStack s)           { return readTag(s).getDouble("yield"); }
    public static void   setYield(ItemStack s, double v) { CompoundTag t = readTag(s); t.putDouble("yield", v); saveTag(s, t); }
    public static double getPoison(ItemStack s)          { return readTag(s).getDouble("xenon"); }
    public static void   setPoison(ItemStack s, double v){ CompoundTag t = readTag(s); t.putDouble("xenon", v); saveTag(s, t); }
    public static double getCoreHeat(ItemStack s)        { return readTag(s).getDouble("core"); }
    public static void   setCoreHeat(ItemStack s, double v){ CompoundTag t = readTag(s); t.putDouble("core", v); saveTag(s, t); }
    public static double getHullHeat(ItemStack s)        { return readTag(s).getDouble("hull"); }
    public static void   setHullHeat(ItemStack s, double v){ CompoundTag t = readTag(s); t.putDouble("hull", v); saveTag(s, t); }
    *///?}

    public static double getPoisonLevel(ItemStack stack) { return getPoison(stack) / 100.0; }

    public static double getEnrichment(ItemStack stack) {
        if (!(stack.getItem() instanceof RBMKRodItem rod)) return 0;
        double y = rod.yield;
        return y > 0 ? getYield(stack) / y : 0;
    }

    // ─── Tooltip ─────────────────────────────────────────────────────────────

    //? if < 1.21.1 {
    // @Override omitted intentionally — Stonecutter removes this block for >= 1.21.1
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
    *///?}
        list.add(Component.literal(ChatFormatting.ITALIC + fullName));

        double hull = getHullHeat(stack);
        double core = getCoreHeat(stack);
        if (hull >= 50 || core >= 50)
            list.add(Component.translatable("desc.item.wasteCooling").withStyle(ChatFormatting.GOLD));

        if (selfRate > 0 || function == EnumBurnFunc.SIGMOID)
            list.add(Component.translatable("trait.rbmk.source").withStyle(ChatFormatting.RED));

        list.add(Component.translatable("trait.rbmk.depletion",
            String.format("%.3f%%", (1.0 - getEnrichment(stack)) * 100)));
        list.add(Component.translatable("trait.rbmk.xenon",
            String.format("%.3f%%", getPoison(stack))));
        list.add(Component.translatable("trait.rbmk.splitsWith",
            Component.translatable(nType.unlocalized)).withStyle(ChatFormatting.BLUE));
        list.add(Component.translatable("trait.rbmk.splitsInto",
            Component.translatable(rType.unlocalized)).withStyle(ChatFormatting.BLUE));
        list.add(Component.translatable("trait.rbmk.fluxFunc",
            Component.literal(ChatFormatting.WHITE + getFuncDescription(stack))).withStyle(ChatFormatting.YELLOW));
        list.add(Component.translatable("trait.rbmk.funcType",
            function.title).withStyle(ChatFormatting.YELLOW));
        list.add(Component.translatable("trait.rbmk.xenonGen",
            Component.literal(ChatFormatting.WHITE + "x * " + xGen)).withStyle(ChatFormatting.YELLOW));
        list.add(Component.translatable("trait.rbmk.xenonBurn",
            Component.literal(ChatFormatting.WHITE + "x² / " + xBurn)).withStyle(ChatFormatting.YELLOW));
        list.add(Component.translatable("trait.rbmk.heat",
            heat + "°C").withStyle(ChatFormatting.GOLD));
        list.add(Component.translatable("trait.rbmk.diffusion",
            diffusion + "¹/²").withStyle(ChatFormatting.GOLD));
        list.add(Component.literal(ChatFormatting.RED + "Skin: " + String.format("%.1f°C", hull)));
        list.add(Component.literal(ChatFormatting.RED + "Core: " + String.format("%.1f°C", core)));
        list.add(Component.literal(ChatFormatting.DARK_RED + "Melt: " + meltingPoint + "°C"));
    }

    // ─── Durability bar (depletion) ───────────────────────────────────────────

    @Override public boolean isBarVisible(ItemStack stack)  { return getEnrichment(stack) < 1.0; }
    @Override public int    getBarWidth(ItemStack stack)    { return Math.round((float)(1.0 - getEnrichment(stack)) * 13); }

    // ─── Enums ───────────────────────────────────────────────────────────────

    public enum EnumBurnFunc {
        PASSIVE     ("SAFE / PASSIVE"),
        LOG_TEN     ("MEDIUM / LOGARITHMIC"),
        PLATEU      ("SAFE / EULER"),
        ARCH        ("DANGEROUS / NEGATIVE-QUADRATIC"),
        SIGMOID     ("SAFE / SIGMOID"),
        SQUARE_ROOT ("MEDIUM / SQUARE ROOT"),
        LINEAR      ("DANGEROUS / LINEAR"),
        QUADRATIC   ("DANGEROUS / QUADRATIC"),
        EXPERIMENTAL("EXPERIMENTAL / SINE SLOPE");

        public final String title;
        EnumBurnFunc(String title) { this.title = title; }
    }

    public enum EnumDepleteFunc {
        LINEAR, RAISING_SLOPE, BOOSTED_SLOPE, GENTLE_SLOPE, STATIC
    }
}
