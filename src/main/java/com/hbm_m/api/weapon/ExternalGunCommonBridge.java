package com.hbm_m.api.weapon;

import com.hbm_m.handler.EnumKeybind;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Common-side (client + server) SPI allowing an external mod (such as a TACZ addon) to
 * integrate with SEDNA {@link ItemGunBaseNT} weapons without mixins and without HBM-Modernized
 * depending on the external mod.
 *
 * <p>Mirrors {@link com.hbm_m.client.render.weapon.bridge.ExternalGunRenderBridge}: {@link #get()}
 * never returns {@code null}, falling back to a zero-overhead no-op implementation ({@link None})
 * whenever no bridge is registered.
 *
 * <p>Uses only vanilla Minecraft and HBM types (no Forge/NeoForge or external-mod imports) so it
 * compiles identically across all Stonecutter targets.
 */
public abstract class ExternalGunCommonBridge {

    private static volatile ExternalGunCommonBridge current = None.INSTANCE;

    /** Returns the registered bridge, or a no-op default if none is registered. */
    public static ExternalGunCommonBridge get() {
        return current;
    }

    /** Registers the external bridge; passing {@code null} resets to the no-op default. */
    public static void register(@Nullable ExternalGunCommonBridge bridge) {
        current = bridge == null ? None.INSTANCE : bridge;
    }

    /**
     * Returns {@code true} when the external mod owns handling for this weapon stack on the
     * common/server side. Must safely return {@code false} for arbitrary non-gun or empty stacks.
     */
    public boolean isHandled(@Nullable ItemStack stack) {
        return false;
    }

    /**
     * Returns {@code true} when native SEDNA weapon sounds played through {@code Lego.playSound}
     * should be suppressed for this stack because the external mod owns weapon audio.
     */
    public boolean shouldSuppressNativeSound(@Nullable ItemStack stack) {
        return isHandled(stack);
    }

    /**
     * Returns {@code true} when SEDNA weapons with secondary-key auto deciders (such as STG-77 /
     * Steyr AUG) should use the standard primary-hold auto decider instead.
     */
    public boolean shouldUseStandardAutoDecider(@Nullable ItemStack stack) {
        return false;
    }

    /**
     * Invoked at the start of {@link ItemGunBaseNT#handleKeybind(LivingEntity, Container, ItemStack, EnumKeybind, boolean)}
     * (after the {@code enableGuns} guard).
     */
    public void onKeybind(
            ItemGunBaseNT gun,
            LivingEntity entity,
            @Nullable Container inventory,
            ItemStack stack,
            EnumKeybind keybind,
            boolean pressed
    ) {
    }

    /**
     * Invoked at the end of server-side {@link ItemGunBaseNT#inventoryTick} (both when unequipped
     * with {@code isHeld == false} and after the state machine tick with {@code isHeld == true}).
     */
    public void onPostInventoryTick(
            ItemGunBaseNT gun,
            ItemStack stack,
            Level level,
            Entity entity,
            int slot,
            boolean isHeld
    ) {
    }

    private static final class None extends ExternalGunCommonBridge {
        static final None INSTANCE = new None();

        private None() {
        }
    }
}
