package com.hbm_m.blockentity;

import java.util.Map;

import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Polluting;
import com.hbm_m.inventory.fluid.trait.FluidTrait.FluidReleaseType;
import com.hbm_m.inventory.fluid.trait.PollutionType;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

import org.jetbrains.annotations.Nullable;

/**
 * Die drei Rauchtanks aus {@code TileEntityMachinePolluting} (1.7.10) als eigenstaendiges Stueck.
 *
 * <p>Der Kern des Originals steckt hier: Abgas geht erst in einen Tank, und <b>nur dessen
 * Ueberlauf</b> landet im Verschmutzungsraster. Wer den Rauch per Rohr zu einem Schornstein
 * fuehrt, verschmutzt also deutlich weniger als jemand, der die Maschine ohne Abgasfuehrung
 * laufen laesst.</p>
 *
 * <p>Ein Millibucket Rauch entspricht 1/100 Verschmutzung - daher {@code amount * 100} beim
 * Einfuellen und {@code overflow / 100F} beim Ueberlauf, wie im Original.</p>
 *
 * <p>Als eigene Klasse statt nur als Basisklasse, weil nicht jede rauchende Maschine des Ports
 * unter {@link MachinePollutingBlockEntity} passt: der Drehofen haengt an
 * {@code BaseHbmBlockEntity} und bringt Inventar und Menue selbst mit.</p>
 */
public class SmokeTankSet {

    /** Original: {@code Fluids.SMOKE} - gewoehnlicher Russ. */
    public final FluidTank smoke;
    /** Original: {@code Fluids.SMOKE_LEADED} - bleihaltiges Abgas. */
    public final FluidTank smokeLeaded;
    /** Original: {@code Fluids.SMOKE_POISON} - giftiges Abgas. */
    public final FluidTank smokePoison;

    /** Wird beim Ueberlauf gerufen; standardmaessig zischt die Maschine. */
    private Runnable onOverflow;

    public SmokeTankSet(int bufferMb) {
        this.smoke = new FluidTank(ModFluids.SMOKE.getSource(), bufferMb);
        this.smokeLeaded = new FluidTank(ModFluids.SMOKE_LEADED.getSource(), bufferMb);
        this.smokePoison = new FluidTank(ModFluids.SMOKE_POISON.getSource(), bufferMb);
    }

    /**
     * Ersetzt das Zischen durch etwas anderes. Pyroofen und Drehofen setzen im Original an genau
     * dieser Stelle ihr {@code isVenting}-Merkmal fuer die Abblas-Animation.
     */
    public SmokeTankSet onOverflow(Runnable action) {
        this.onOverflow = action;
        return this;
    }

    public FluidTank[] tanks() {
        return new FluidTank[] { smoke, smokeLeaded, smokePoison };
    }

    /** Original: SOOT geht in smoke, HEAVYMETAL in smoke_leaded, POISON in smoke_poison. */
    private @Nullable FluidTank tankFor(PollutionType type) {
        return switch (type) {
            case SOOT -> smoke;
            case HEAVYMETAL -> smokeLeaded;
            case POISON -> smokePoison;
            // Fallout hat im Original keinen Rauchtank.
            case FALLOUT -> null;
        };
    }

    /** Original: {@code pollute(PollutionType, float)}. */
    public void pollute(Level level, BlockPos pos, PollutionType type, float amount) {
        if (level == null || level.isClientSide()) return;

        FluidTank tank = tankFor(type);
        if (tank == null) return;

        tank.setFill(tank.getFill() + (int) Math.ceil(amount * 100));

        if (tank.getFill() <= tank.getMaxFill()) return;

        int overflow = tank.getFill() - tank.getMaxFill();
        tank.setFill(tank.getMaxFill());
        PollutionHandler.incrementPollution(level, pos, type, overflow / 100F);

        if (onOverflow != null) {
            onOverflow.run();
        } else if (level.random.nextInt(3) == 0) {
            // Original: NTMSounds.VANILLA_HISS ("random.fizz") mit 1/3 Wahrscheinlichkeit.
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.1F, 1.5F);
        }
    }

    /** Original: {@code pollute(FluidType, FluidReleaseType, float)}. */
    public void pollute(Level level, BlockPos pos, Fluid fluid, FluidReleaseType release, float amount) {
        if (release == FluidReleaseType.VOID) return;

        FT_Polluting trait = FluidType.getTrait(fluid, FT_Polluting.class);
        if (trait == null) return;

        Map<PollutionType, Float> map = release == FluidReleaseType.BURN ? trait.burnMap : trait.releaseMap;

        for (Map.Entry<PollutionType, Float> entry : map.entrySet()) {
            pollute(level, pos, entry.getKey(), entry.getValue() * amount);
        }
    }

    // Schluesselnamen wie im Original.
    public void writeToNBT(CompoundTag tag) {
        smoke.writeToNBT(tag, "smoke0");
        smokeLeaded.writeToNBT(tag, "smoke1");
        smokePoison.writeToNBT(tag, "smoke2");
    }

    public void readFromNBT(CompoundTag tag) {
        smoke.readFromNBT(tag, "smoke0");
        smokeLeaded.readFromNBT(tag, "smoke1");
        smokePoison.readFromNBT(tag, "smoke2");
    }
}
