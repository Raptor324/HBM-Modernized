package com.hbm_m.platform;

/**
 * Nur 1.21.1: Bruecke fuer {@code Block#use(...)} (1.20.1) -> {@code useItemOn(...)} (1.21.1).
 * Der alte Rumpf bleibt unveraendert in einer privaten Methode, {@code useItemOn} ruft sie mit
 * derselben Hand auf und uebersetzt das Ergebnis (PASS -> PASS_TO_DEFAULT_BLOCK_INTERACTION, damit
 * Vanilla wie bisher weiterreicht). Muster siehe VERSIONPORT.md.
 */
public final class BlockUseHooks {
    private BlockUseHooks() {}

    //? if >= 1.21.1 {
    /*public static net.minecraft.world.ItemInteractionResult item(net.minecraft.world.InteractionResult r) {
        return switch (r) {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> net.minecraft.world.ItemInteractionResult.SUCCESS;
            case CONSUME -> net.minecraft.world.ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> net.minecraft.world.ItemInteractionResult.CONSUME_PARTIAL;
            case FAIL -> net.minecraft.world.ItemInteractionResult.FAIL;
            case PASS -> net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }
    *///?}
}
