package com.hbm_m.block.generic;

import com.hbm_m.item.ModItems;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@link com.hbm.blocks.generic.BlockAshes} ({@code ash_digamma}): fallender Block; auf dem Client zaehlt jeder
 * 25. Anzeige-Tick den Aschezaehler {@link #ashes} hoch (Obergrenze nach Schutz: Aschebrille 25 %, Sand-/Licht-Schutz
 * 75 %, sonst 95 % von 256), der pro Client-Tick um 2 abfaellt. Die Bildschirm-Asche, die ihn anzeigen wuerde, ist im
 * Original deaktiviert (auskommentierter Event-Handler) - deshalb hier ebenfalls ohne Anzeige.
 */
public class BlockAshes extends FallingBlock {

    public static int ashes = 0;

    public BlockAshes(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        super.animateTick(state, world, pos, rand);

        if (rand.nextInt(25) == 0) {
            Player me = Minecraft.getInstance().player;
            if (me == null) return;
            if (me.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ASHGLASSES.get())) {
                if (ashes < 256 * 0.25) {
                    ashes++;
                }
            } else if (com.hbm_m.handler.ArmorRegistry.hasProtection(me, 3, com.hbm_m.handler.HazardClass.SAND)
                    || com.hbm_m.handler.ArmorRegistry.hasProtection(me, 3, com.hbm_m.handler.HazardClass.LIGHT)) {
                if (ashes < 256 * 0.75) {
                    ashes++;
                }
            } else {
                if (ashes < 256 * 0.95) {
                    ashes++;
                }
            }
        }
    }

    /** ModEventHandlerClient.onClientTick (START): begrenzen und abbauen. */
    public static void clientTick() {
        if (ashes > 256) ashes = 256;
        if (ashes > 0) ashes -= 2;
        if (ashes < 0) ashes = 0;
    }
}
