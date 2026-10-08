package com.hbm_m.block.generic;

import com.hbm_m.powerarmor.ModArmorFSBPowered;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/**
 * 1:1 {@code HEVBattery} (1.7.10 Original): Einweg-Ladepad. Rechtsklick (ohne Schleichen) mit vollem FSB-Satz und
 * strombetriebenem Helm laedt jedes getragene Batterie-Teil um 150.000 HE (bis zum Maximum), dann verschwindet der Block.
 */
public class HevBatteryBlock extends Block {

    private static final long CHARGE_AMOUNT = 150_000L;
    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 6, 10);

    public HevBatteryBlock(Properties properties) { super(properties); }

    /** 1:1 Original onBlockActivated: nur ohne Schleichen, voller FSB-Satz mit Strom-Helm, laedt alle Batterie-Teile. */
    private static InteractionResult activate(Level level, BlockPos pos, Player player) {

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;

        } else if (!player.isShiftKeyDown()) {

            if (com.hbm_m.powerarmor.ModArmorFSB.hasFSBArmorIgnoreCharge(player) && player.getInventory().armor.get(3).getItem() instanceof ModArmorFSBPowered) {

                for (ItemStack st : player.getInventory().armor) {

                    if (st.isEmpty())
                        continue;

                    if (st.getItem() instanceof com.hbm_m.api.item.IBatteryItem battery) {

                        long maxcharge = battery.getMaxCharge(st);
                        long charge = battery.getCharge(st);
                        long newcharge = Math.min(charge + CHARGE_AMOUNT, maxcharge);

                        battery.setCharge(st, newcharge);
                    }
                }

                level.playSound(null, player.getX(), player.getY(), player.getZ(), com.hbm_m.sound.HbmSoundsNT.get("hbm:item.battery"), SoundSource.PLAYERS, 1.0F, 1.0F);
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }

            return InteractionResult.CONSUME;
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        return activate(level, pos, player);
        }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        return activate(level, pos, player);
        }
    *///?}

}
