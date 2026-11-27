package com.hbm_m.platform.legacy;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

/**
 * [26.x] Ersatz für {@code InteractionResult-Holder}, {@code ItemInteraction-Result.sidedSuccess}
 * und {@code InteractionResult#sidedSuccess}, die in 1.21.2+ entfernt wurden.
 *
 * <p>Die Aufrufe werden von Stonecutter umgeleitet ({@code mc26_irh_*}/{@code mc26_ir_*} in
 * stonecutter.gradle.kts), der 1.20.1/1.21.1-Quelltext bleibt unverändert. Die Methodennamen
 * {@code holder_*} sind Teil der Rückwärts-Regel und dürfen nicht umbenannt werden.
 *
 * <p>Seit 1.21.2 entscheidet der Aufrufort nicht mehr über die Seite: {@code SUCCESS} lässt den
 * Client schwingen, der Server ignoriert den Unterschied.
 */
@SuppressWarnings("unused")
public final class InteractionCompat {

    private InteractionCompat() {}

    public static InteractionResult sidedSuccess(boolean clientSide) {
        return InteractionResult.SUCCESS;
    }

    public static InteractionResult itemSidedSuccess(boolean clientSide) {
        return InteractionResult.SUCCESS;
    }

    public static InteractionResult holder_pass(ItemStack stack) {
        return InteractionResult.PASS;
    }

    public static InteractionResult holder_fail(ItemStack stack) {
        return InteractionResult.FAIL;
    }

    public static InteractionResult holder_success(ItemStack stack) {
        return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
    }

    public static InteractionResult holder_consume(ItemStack stack) {
        return InteractionResult.CONSUME.heldItemTransformedTo(stack);
    }

    public static InteractionResult holder_sidedSuccess(ItemStack stack, boolean clientSide) {
        return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
    }

    // Gleiche Methoden für voll qualifizierte Aufrufe (eigene Rückwärts-Regel mc26_irh_call_fq).
    public static InteractionResult fq_holder_pass(ItemStack stack) { return holder_pass(stack); }
    public static InteractionResult fq_holder_fail(ItemStack stack) { return holder_fail(stack); }
    public static InteractionResult fq_holder_success(ItemStack stack) { return holder_success(stack); }
    public static InteractionResult fq_holder_consume(ItemStack stack) { return holder_consume(stack); }
    public static InteractionResult fq_holder_sidedSuccess(ItemStack stack, boolean clientSide) { return holder_sidedSuccess(stack, clientSide); }
}
