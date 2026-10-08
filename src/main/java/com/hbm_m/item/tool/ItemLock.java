package com.hbm_m.item.tool;

import com.hbm_m.api.tile.ILockableTile;
import com.hbm_m.item.ItemKeyPin;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code com.hbm.items.tool.ItemLock}: Vorhaengeschloss mit Stiftcode; auf ein abschliessbares
 * Blockentity angewandt wird es verriegelt ({@code lockMod} = Knack-Chance-Faktor).
 */
public class ItemLock extends ItemKeyPin {

    public double lockMod = 0.1D;

    public ItemLock(Properties properties, double mod) {
        super(properties);
        lockMod = mod;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        ItemStack stack = ctx.getItemInHand();
        int pins = getPins(stack);
        if (pins != 0) {
            Level world = ctx.getLevel();
            BlockEntity te = com.hbm_m.util.CompatExternal.getCoreFromPos(world, ctx.getClickedPos());
            if (te instanceof ILockableTile tile) {
                if (tile.isLocked())
                    return InteractionResult.FAIL;

                tile.setPins(pins);
                tile.lock();
                tile.setMod(lockMod);

                Player player = ctx.getPlayer();
                if (player != null)
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:block.lockHang"), SoundSource.PLAYERS, 1.0F, 1.0F);
                stack.shrink(1);
                return InteractionResult.sidedSuccess(world.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }
}
