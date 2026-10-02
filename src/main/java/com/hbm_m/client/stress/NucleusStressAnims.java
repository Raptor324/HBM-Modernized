package com.hbm_m.client.stress;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.machines.FireboxBaseBlockEntity;
import com.hbm_m.blockentity.machines.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.blockentity.machines.MachineAssemblerBlockEntity;
import com.hbm_m.blockentity.machines.MachineChemicalFactoryBlockEntity;
import com.hbm_m.blockentity.machines.MachineChemicalPlantBlockEntity;
import com.hbm_m.blockentity.machines.MachinePressBlockEntity;
import com.hbm_m.blockentity.machines.MachineTurbofanBlockEntity;
import com.hbm_m.main.MainRegistry;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The SINGLE place that knows how to turn a machine's NATIVE working animation
 * on and off for the stress scene ({@code /nucleus stress anims}). Block entities
 * know nothing about the stress command: the drivers here set the machines' own
 * working state - the exact fields their Nucleus animators already read - through
 * public setters where they exist and cached reflection for private fields of our
 * own classes (identical in dev and production: mod code is never obfuscated).
 *
 * <p>A machine ported to the Nucleus engine gets ONE line here when (and only
 * when) it should participate in the stress animations - the same trade the
 * {@code ClientTickerRegistry} makes. Machines without an entry simply never
 * animate; nothing in machine code changes. Doors are excluded: they animate
 * through their real server-side state machine (see NucleusStressManager).</p>
 *
 * <p>Note: driving the real working state also plays the machines' real working
 * sounds - that is what "native" means. Clear/off stops the state, sounds stop
 * with it.</p>
 */
@OnlyIn(Dist.CLIENT)
final class NucleusStressAnims {

    @FunctionalInterface
    interface AnimDriver {
        /**
         * Drive the machine's native working animation. Called every client tick
         * while the machine animates ({@code on=true}), and once with
         * {@code on=false} when it stops (drivers may use that to wind down; many
         * machines wind down on their own once the working state is off).
         */
        void drive(BlockEntity be, boolean on, long gameTime);
    }

    private record Entry(Class<? extends BlockEntity> beClass, AnimDriver driver) {}

    private static final List<Entry> DRIVERS = new ArrayList<>();

    // Cached reflective handles (private fields of our own BE classes).
    private static final Field PRESS_VISUAL = field(MachinePressBlockEntity.class, "visualPressPosition");
    private static final Field PRESS_VISUAL_PREV = field(MachinePressBlockEntity.class, "prevVisualPressPosition");
    private static final Field AA_CRAFTING = field(MachineAdvancedAssemblerBlockEntity.class, "clientIsCrafting");
    private static final Field TURBO_WAS_ON = field(MachineTurbofanBlockEntity.class, "wasOn");
    private static final Field CHEMPLANT_ANIM = field(MachineChemicalPlantBlockEntity.class, "anim");
    private static final Field CHEMFACTORY_ANIM = field(MachineChemicalFactoryBlockEntity.class, "anim");
    private static final Field FIREBOX_PLAYERS_USING = field(FireboxBaseBlockEntity.class, "playersUsing");

    static {
        // --- one line per animated machine, as in ClientTickerRegistry ---
        // Press: the head follows the visual press position; drive it with a
        // native-looking cycle (approach 30t, hold 10t, retract 40t). On stop the
        // BE's own clientTick lerps the head back to rest.
        add(MachinePressBlockEntity.class, (be, on, gameTime) -> {
            if (!on || PRESS_VISUAL == null || PRESS_VISUAL_PREV == null) return;
            setFloat(be, PRESS_VISUAL_PREV, pressCycle(gameTime - 1));
            setFloat(be, PRESS_VISUAL, pressCycle(gameTime));
        });
        // Assembler: public crafting flag drives slider/arm/cogs (+ its loop sound).
        add(MachineAssemblerBlockEntity.class, (be, on, gameTime) ->
                ((MachineAssemblerBlockEntity) be).setCrafting(on));
        // Advanced assembler: client crafting flag drives ticker arms/ring.
        add(MachineAdvancedAssemblerBlockEntity.class, (be, on, gameTime) -> setBoolean(be, AA_CRAFTING, on));
        // Turbofan: the synced "on" state builds rotor momentum (spin-down is native).
        add(MachineTurbofanBlockEntity.class, (be, on, gameTime) -> setBoolean(be, TURBO_WAS_ON, on));
        // Chemical plant / factory: advance the shared anim counter the same way the
        // working gate does (one unit per tick; prevAnim is rotated by the BE tick).
        add(MachineChemicalPlantBlockEntity.class, (be, on, gameTime) -> bumpFloat(be, CHEMPLANT_ANIM, on));
        add(MachineChemicalFactoryBlockEntity.class, (be, on, gameTime) -> bumpFloat(be, CHEMFACTORY_ANIM, on));
        // Firebox / heating oven: playersUsing opens the fire door (GUI-open pose).
        add(FireboxBaseBlockEntity.class, (be, on, gameTime) -> {
            if (FIREBOX_PLAYERS_USING == null) return;
            try {
                int current = FIREBOX_PLAYERS_USING.getInt(be);
                int target = on ? Math.max(1, current) : 0;
                if (current != target) FIREBOX_PLAYERS_USING.setInt(be, target);
            } catch (IllegalAccessException ignored) {
            }
        });
    }

    private NucleusStressAnims() {}

    static void add(Class<? extends BlockEntity> beClass, AnimDriver driver) {
        DRIVERS.add(new Entry(beClass, driver));
    }

    /** Applies the driver of the machine's type (no-op for machines without one). */
    static void apply(BlockEntity be, boolean on, long gameTime) {
        for (int i = 0; i < DRIVERS.size(); i++) {
            Entry entry = DRIVERS.get(i);
            if (entry.beClass().isInstance(be)) {
                entry.driver().drive(be, on, gameTime);
                return;
            }
        }
    }

    /** Triangle wave 0..1..0 over 80 ticks: approach 30t, hold 10t, retract 40t. */
    private static float pressCycle(long gameTime) {
        long t = Math.floorMod(gameTime, 80L);
        return t < 30 ? t / 30.0F : t < 40 ? 1.0F : 1.0F - (t - 40) / 40.0F;
    }

    private static void bumpFloat(BlockEntity be, @Nullable Field field, boolean on) {
        if (!on || field == null) return;
        try {
            field.setFloat(be, field.getFloat(be) + 1.0F);
        } catch (IllegalAccessException ignored) {
        }
    }

    private static void setFloat(BlockEntity be, Field field, float value) {
        try {
            field.setFloat(be, value);
        } catch (IllegalAccessException ignored) {
        }
    }

    private static void setBoolean(BlockEntity be, @Nullable Field field, boolean value) {
        if (field == null) return;
        try {
            field.setBoolean(be, value);
        } catch (IllegalAccessException ignored) {
        }
    }

    private static Field field(Class<?> owner, String name) {
        try {
            Field f = owner.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException e) {
            MainRegistry.LOGGER.warn("[NucleusStress] field {}.{} unavailable - stress animation for this machine is skipped",
                    owner.getSimpleName(), name);
            return null;
        }
    }
}
