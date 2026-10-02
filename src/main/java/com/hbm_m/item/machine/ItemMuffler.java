package com.hbm_m.item.machine;

import com.hbm_m.blockentity.LoadedMachineBlockEntity;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.CompatExternal;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 1:1 {@code com.hbm.items.machine.ItemMuffler} ({@code upgrade_muffler}): macht eine Maschine dauerhaft leise. */
public class ItemMuffler extends Item {

    public ItemMuffler(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockEntity te = CompatExternal.getCoreFromPos(world, ctx.getClickedPos());

        if (te instanceof LoadedMachineBlockEntity tile && !tile.isMuffled()) {
            tile.setMuffled(true);
            if (player != null) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:item.upgradePlug"), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            ctx.getItemInHand().shrink(1);
            tile.setChanged();
            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        return InteractionResult.PASS;
    }
}
