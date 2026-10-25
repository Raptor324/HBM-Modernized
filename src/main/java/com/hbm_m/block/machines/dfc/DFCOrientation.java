package com.hbm_m.block.machines.dfc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;

/**
 * 1:1 {@code BlockPistonBase.determineOrientation}, wie es {@code CoreComponent.onBlockPlacedBy} fuer
 * Emitter, Empfaenger, Injektor und Stabilisator nutzt: der Block zeigt zum Spieler, senkrecht nur,
 * wenn der Spieler waagerecht naeher als 2 Bloecke steht.
 */
public final class DFCOrientation {

    private DFCOrientation() {}

    public static Direction determineOrientation(BlockPlaceContext ctx) {
        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos();
        if (player == null) return Direction.NORTH;

        if (Math.abs(player.getX() - pos.getX()) < 2.0F && Math.abs(player.getZ() - pos.getZ()) < 2.0F) {
            // Original: posY + 1.82 - yOffset (Serverseite: yOffset 0, posY = Fusshoehe)
            double d0 = player.getY() + 1.82D;
            if (d0 - pos.getY() > 2.0D) return Direction.UP;
            if (pos.getY() - d0 > 0.0D) return Direction.DOWN;
        }

        int l = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        return l == 0 ? Direction.NORTH : (l == 1 ? Direction.EAST : (l == 2 ? Direction.SOUTH : Direction.WEST));
    }
}
