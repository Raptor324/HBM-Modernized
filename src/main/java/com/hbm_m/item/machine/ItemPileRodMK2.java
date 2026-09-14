package com.hbm_m.item.machine;

import com.hbm_m.item.ITooltipProvider;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1-Port von {@code ItemPileRodMK2} (1.7.10): die Brennstaebe des Uranmeilers.
 *
 * <p>Ein Stab hat drei Kennzahlen. Der <b>Reaktionsfaktor</b> sagt, wie stark er auf ankommende
 * Neutronen antwortet - und zwar nicht geradlinig, sondern ueber die Wurzelkurve
 * {@link #squirt(double)} des Originals, weshalb ein Stab bei viel Fluss immer weniger zusetzt.
 * Die <b>Lebensdauer</b> ist die Menge Neutronen, die er vertraegt, bevor er sich in den naechsten
 * Stab der Reihe verwandelt. Der <b>Waermefaktor</b> sagt, wieviel Hitze je erzeugtem Neutron im
 * Kanal bleibt.</p>
 *
 * <p>Die beiden Quellen ({@code RA226BE}, {@code PO210BE}) reagieren gar nicht, sondern geben
 * stumpf ein Neutron je Tick ab - ohne sie kommt der Meiler nie in Gang. Zirkonium ist ein reiner
 * Platzhalter ohne jede Wirkung.</p>
 *
 * <p>Der Abbrand steht als {@code depletion} im NBT des Stapels und wird beim Auswerfen aus dem
 * Kanal wieder entfernt, damit sich Staebe im Lager wieder stapeln.</p>
 */
public class ItemPileRodMK2 extends Item implements ITooltipProvider {

    /** Original: {@code KEY_NBT_DEPLETION}. */
    public static final String KEY_NBT_DEPLETION = "depletion";

    /**
     * Original: {@code EnumPileRod}. Die Reihenfolge entspricht den Metadaten des Originals, weil
     * {@link #turnsInto} genau darauf zeigt.
     */
    public enum EnumPileRod {
        /* 0 */ RA226BE(1D),
        /* 1 */ PO210BE(1D),
        /* 2 */ ZR(0D, 0D, 0D, 2),
        /* 3 */ NU(1D, 25_000D, 0.25D, 4),
        /* 4 */ PU239(1D, 500D, 0.5D, 5),
        /* 5 */ RGP(1D, 1_000D, 0.5D, 6),
        /* 6 */ WASTE(1D, 0D, 1.5D, 6);

        public final double reactionMult;
        public final double life;
        public final double heatMult;
        public final double neutronSource;
        /** Index des Stabs, in den sich dieser nach Ablauf der Lebensdauer verwandelt. */
        public final int turnsInto;

        /** Neutronenquelle: reagiert nicht, gibt nur ab. */
        EnumPileRod(double neutronSource) {
            this.neutronSource = neutronSource;
            this.reactionMult = 0D;
            this.life = 0D;
            this.heatMult = 0D;
            this.turnsInto = 0;
        }

        EnumPileRod(double reaction, double life, double heat, int turnsInto) {
            this.reactionMult = reaction;
            this.life = life;
            this.heatMult = heat;
            this.turnsInto = turnsInto;
            this.neutronSource = 0D;
        }

        public String getName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private final EnumPileRod rod;

    public ItemPileRodMK2(Properties properties, EnumPileRod rod) {
        super(properties);
        this.rod = rod;
    }

    public EnumPileRod getRod() {
        return rod;
    }

    /**
     * Original: {@code BobMathUtil.squirt} - eine gedaempfte Wurzel, die bei null durch null geht
     * und mit wachsendem Fluss immer flacher wird. Sie sorgt dafuer, dass ein Meiler nicht
     * beliebig hochlaeuft, sondern in eine Saettigung geht.
     */
    public static double squirt(double x) {
        return Math.sqrt(x + 1D / ((x + 2D) * (x + 2D))) - 1D / (x + 2D);
    }

    // ── Abbrand ─────────────────────────────────────────────────────────────

    public static double getDepletion(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0D : tag.getDouble(KEY_NBT_DEPLETION);
    }

    public static void setDepletion(ItemStack stack, double depletion) {
        stack.getOrCreateTag().putDouble(KEY_NBT_DEPLETION, depletion);
    }

    /** Original: {@code getDepletionPercent}. Gibt 0 zurueck, wenn der Stab nicht abbrennt. */
    public static double getDepletionPercent(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0D;
        EnumPileRod rod = rodOf(stack);
        if (rod == null || rod.life <= 0D) return 0D;
        return (getDepletion(stack) / rod.life) * 100D;
    }

    @Nullable
    public static EnumPileRod rodOf(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return stack.getItem() instanceof ItemPileRodMK2 pileRod ? pileRod.rod : null;
    }

    // ── Reaktion ────────────────────────────────────────────────────────────

    /** Original: {@code getReactivity} - Quellenanteil plus gedaempfte Antwort auf den Zufluss. */
    public static double getReactivity(ItemStack stack, double inFlux) {
        EnumPileRod rod = rodOf(stack);
        if (rod == null) return 0D;

        double outFlux = rod.neutronSource;
        if (rod.reactionMult > 0D) {
            outFlux += squirt(inFlux) * rod.reactionMult;
        }
        return outFlux;
    }

    public static double getHeatPerNeutron(ItemStack stack) {
        EnumPileRod rod = rodOf(stack);
        return rod == null ? 0D : rod.heatMult;
    }

    /**
     * Original: {@code react} - erhoeht den Abbrand und gibt den Nachfolgestab zurueck, sobald die
     * Lebensdauer erschoepft ist.
     */
    public static ItemStack react(ItemStack stack, double inFlux) {
        EnumPileRod rod = rodOf(stack);
        if (rod == null || rod.life <= 0D) return stack;

        double depletion = getDepletion(stack) + inFlux;

        if (depletion < rod.life) {
            setDepletion(stack, depletion);
            return stack;
        }

        EnumPileRod next = EnumPileRod.values()[rod.turnsInto];
        return new ItemStack(ModPileRods.of(next));
    }

    // ── Anzeige ─────────────────────────────────────────────────────────────

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getDepletionPercent(stack) > 0D;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        // Voller Balken bei frischem Stab, leer bei erschoepftem - wie die Haltbarkeitsanzeige.
        double left = 1D - getDepletionPercent(stack) / 100D;
        return (int) Math.round(13D * Math.max(0D, Math.min(1D, left)));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x33CC33;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (rod.life > 0D) {
            tooltip.add(Component.translatable("tooltip.hbm_m.pile_rod.lifetime", (int) Math.round(rod.life))
                    .withStyle(ChatFormatting.GRAY));

            double depletion = getDepletionPercent(stack);
            if (depletion > 0D) {
                tooltip.add(Component.translatable("tooltip.hbm_m.pile_rod.depletion", (int) Math.round(depletion))
                        .withStyle(ChatFormatting.GRAY));
            }
        }

        if (rod.neutronSource > 0D) {
            tooltip.add(Component.translatable("tooltip.hbm_m.pile_rod.source")
                    .withStyle(ChatFormatting.YELLOW));
        }
    }
}
