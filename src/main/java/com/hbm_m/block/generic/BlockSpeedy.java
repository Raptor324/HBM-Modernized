package com.hbm_m.block.generic;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.block.IStepTickReceiver;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@link com.hbm.blocks.generic.BlockSpeedy} ({@code asphalt}, {@code asphalt_light}): laufende Spieler werden auf
 * dem Client um den Faktor schneller (x/z-Bewegung * speed), Tooltip "Increases speed by 50%".
 */
public class BlockSpeedy extends Block implements IStepTickReceiver {

    private final double speed;

    public BlockSpeedy(Properties properties, double speed) {
        super(properties);
        this.speed = speed;
    }

    @Override
    public void onPlayerStep(Level world, BlockPos pos, Player player) {
        if (!world.isClientSide)
            return;

        if (player.zza != 0 || player.xxa != 0) {
            Vec3 m = player.getDeltaMovement();
            player.setDeltaMovement(m.x * speed, m.y, m.z * speed);
        }
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter world, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
    *///?}
        list.add(Component.literal("Increases speed by " + (Mth.floor((speed - 1) * 100)) + "%").withStyle(ChatFormatting.BLUE));
    }
}
