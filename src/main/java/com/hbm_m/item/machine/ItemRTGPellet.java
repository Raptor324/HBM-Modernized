package com.hbm_m.item.machine;

import com.hbm_m.item.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 1:1-Port von {@code ItemRTGPellet} (1.7.10): die Waermequelle eines Radioisotopengenerators.
 *
 * <p>Jedes Pellet hat eine feste Heizleistung und - bis auf wenige Ausnahmen - eine
 * <b>Lebensdauer</b>. Die ist aus der echten Halbwertszeit gerechnet
 * ({@link #lifespan(float, HalfLifeType)}), mal anderthalb. Ein Radiumpellet haelt damit
 * jahrelang und heizt kaum, ein Bleipellet ist in Minuten durch und heizt zweihundertfach - das
 * ist die eigentliche Entscheidung beim Bau eines RTG.</p>
 *
 * <p>Ist die Lebensdauer aufgebraucht, wird das Pellet zu seinem <b>Zerfallsprodukt</b>: Radium zu
 * Blei, Strontium zu Zirconium, Cobalt zu Nickel. Solange {@link #scalePower} gilt, sinkt die
 * Heizleistung dabei mit der Restlaufzeit, statt bis zuletzt voll zu bleiben.</p>
 *
 * <p><b>Anmerkung:</b> das Original haengt Heizleistung und Zerfall an zwei Konfigurationsschalter
 * ({@code rtgDecay}, {@code scaleRTGPower}). Beide stehen hier fest auf an - das ist die
 * Voreinstellung des Originals.</p>
 */
public class ItemRTGPellet extends Item {

    /** Alle angemeldeten Pellets - das Original fuehrt dieselbe Liste fuer die Rezeptanzeige. */
    public static final List<ItemRTGPellet> PELLETS = new ArrayList<>();

    /** Original: {@code PELLET_DEPLETION}. */
    public static final String KEY_DEPLETION = "PELLET_DEPLETION";

    /** Original: {@code VersatileConfig.rtgDecay()} - Voreinstellung an. */
    public static final boolean DECAY_ENABLED = true;
    /** Original: {@code VersatileConfig.scaleRTGPower()} - Voreinstellung an. */
    public static final boolean SCALE_POWER = true;

    /** Original: {@code HalfLifeType} - in welchen Einheiten die Halbwertszeit angegeben ist. */
    public enum HalfLifeType { SHORT, MEDIUM, LONG }

    private final short heat;
    private final long lifespan;
    @Nullable
    private final Supplier<Item> decayItem;

    /** Ein Pellet ohne Zerfall - es haelt ewig. */
    public ItemRTGPellet(int heat, Properties properties) {
        this(heat, 0L, null, properties);
    }

    public ItemRTGPellet(int heat, long lifespan, @Nullable Supplier<Item> decayItem, Properties properties) {
        super(properties.stacksTo(1));
        this.heat = (short) heat;
        this.lifespan = lifespan;
        this.decayItem = decayItem;
        PELLETS.add(this);
    }

    /**
     * 1:1-Port von {@code RTGUtil.getLifespan}: aus der Halbwertszeit werden Ticks.
     *
     * <p>Ein Minecraftjahr sind hier hundert Tage zu je 48000 Ticks - das Original rechnet
     * ausdruecklich nicht mit echten 365.</p>
     */
    public static long lifespan(float halfLife, HalfLifeType type) {
        return switch (type) {
            case LONG -> (long) ((48000L * 100L * 100L) * halfLife);
            case MEDIUM -> (long) ((48000L * 100L) * halfLife);
            case SHORT -> (long) (48000L * halfLife);
        };
    }

    /** Dasselbe mal anderthalb - so steht es an jeder Pelletzeile des Originals. */
    public static long lifespan15(float halfLife, HalfLifeType type) {
        return (long) (lifespan(halfLife, type) * 1.5D);
    }

    public short getHeat()       { return heat; }
    public long getMaxLifespan() { return lifespan; }
    public boolean doesDecay()   { return decayItem != null && lifespan > 0L; }

    @Nullable
    public ItemStack getDecayItem() {
        return decayItem == null ? null : new ItemStack(decayItem.get());
    }

    /** Original: {@code getLifespan} - ohne Marke gilt das Pellet als frisch. */
    public long getLifespan(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemRTGPellet)) return 0L;

        if (stack.hasTag() && stack.getTag().contains(KEY_DEPLETION)) {
            return stack.getTag().getLong(KEY_DEPLETION);
        }

        stack.getOrCreateTag().putLong(KEY_DEPLETION, getMaxLifespan());
        return getMaxLifespan();
    }

    /** Original: {@code decay} - ein Tick weniger. */
    public void decay(ItemStack stack) {
        if (!doesDecay()) return;
        stack.getOrCreateTag().putLong(KEY_DEPLETION, getLifespan(stack) - 1L);
    }

    /** Original: {@code getScaledPower} - die Heizleistung sinkt mit der Restlaufzeit. */
    public static short getScaledPower(ItemRTGPellet fuel, ItemStack stack) {
        if (fuel.getMaxLifespan() <= 0L) return fuel.getHeat();
        return (short) Math.ceil(fuel.getHeat()
                * ((double) fuel.getLifespan(stack) / (double) fuel.getMaxLifespan()));
    }

    /** Original: {@code RTGUtil.getPower}. */
    public static short getPower(ItemRTGPellet fuel, ItemStack stack) {
        return SCALE_POWER && fuel.doesDecay() ? getScaledPower(fuel, stack) : fuel.getHeat();
    }

    /**
     * Original: {@code handleDecay} - ist die Laufzeit herum, wird das Pellet zum Zerfallsprodukt,
     * sonst altert es um einen Tick.
     */
    public static ItemStack handleDecay(ItemStack stack, ItemRTGPellet pellet) {
        if (!pellet.doesDecay() || !DECAY_ENABLED) return stack;

        if (pellet.getLifespan(stack) <= 0L) {
            ItemStack decayed = pellet.getDecayItem();
            return decayed == null ? ItemStack.EMPTY : decayed;
        }

        pellet.decay(stack);
        return stack;
    }

    // ── Die Sammelrechnung der Maschinen ────────────────────────────────────

    /** Original: {@code RTGUtil.hasHeat}. */
    public static boolean hasHeat(com.hbm_m.platform.ModItemStackHandler inventory, int[] slots) {
        for (int slot : slots) {
            if (inventory.getStackInSlot(slot).getItem() instanceof ItemRTGPellet) return true;
        }
        return false;
    }

    /**
     * 1:1-Port von {@code RTGUtil.updateRTGs}: die Summe der Heizleistung aller eingelegten
     * Pellets - und im selben Zug altert jedes von ihnen um einen Tick.
     */
    public static int updateRTGs(com.hbm_m.platform.ModItemStackHandler inventory, int[] slots) {
        int newHeat = 0;

        for (int slot : slots) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!(stack.getItem() instanceof ItemRTGPellet pellet)) continue;

            newHeat += getPower(pellet, stack);
            inventory.setStackInSlot(slot, handleDecay(stack, pellet));
        }

        return newHeat;
    }

    // ── Kurzinfo ────────────────────────────────────────────────────────────

    //? if < 1.21.1 {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        addInfo(stack, tooltip);
    }
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addInfo(stack, tooltip);
    }
    *///?}

    private void addInfo(ItemStack stack, List<Component> tooltip) {
        tooltip.add(Component.translatable("desc.item.rtgHeat", getPower(this, stack))
                .withStyle(ChatFormatting.GOLD));

        if (!doesDecay()) return;

        ItemStack decayed = getDecayItem();
        if (decayed != null) {
            tooltip.add(Component.translatable("desc.item.rtgDecay", decayed.getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        }

        long left = getLifespan(stack);
        int percent = getMaxLifespan() > 0 ? (int) (left * 100L / getMaxLifespan()) : 0;
        tooltip.add(Component.literal(percent + "%").withStyle(ChatFormatting.GRAY));
    }

    /** Original: {@code showDurabilityBar} - der Balken erscheint erst nach dem ersten Tick. */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return doesDecay() && getLifespan(stack) != getMaxLifespan();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        if (getMaxLifespan() <= 0L) return 13;
        return (int) Math.round(13.0D * getLifespan(stack) / getMaxLifespan());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x4CC44C;
    }

    /** Damit {@link ModItems} die Zerfallsprodukte ohne Ringschluss anmelden kann. */
    public static Supplier<Item> depleted(Supplier<Item> item) {
        return item;
    }
}
