package com.hbm_m.block.bomb;

import javax.annotation.Nullable;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.entity.item.EntityTNTPrimedBase;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * 1:1 {@code BlockPlasticExplosive} ({@code block_c4}, {@code block_semtex}): Vorderseite zeigt zum Spieler
 * ({@code determineOrientation}), Redstone oder Explosion zuenden eine Standard-VNT-Explosion der Staerke 20 ohne
 * Blockdrops.
 */
public class PlasticExplosiveBlock extends BlockDetonatable implements IBomb {

    /** Richtung der Vorderseite (= Gegenrichtung der Original-Meta). */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public PlasticExplosiveBlock(Properties properties) {
        super(properties, 0, 0, 0, false, false);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** {@code determineOrientation}: 0 unten / 1 oben nah am Spieler, sonst Blick S/W/N/O -> 3/4/2/5. */
    public static Direction determineOrientation(BlockPos pos, @Nullable Player player) {
        if (player == null) return Direction.SOUTH;
        if (Mth.abs((float) player.getX() - pos.getX()) < 2.0F && Mth.abs((float) player.getZ() - pos.getZ()) < 2.0F) {
            double d0 = player.getY() + 1.82D;
            if (d0 - pos.getY() > 2.0D) return Direction.DOWN;
            if (pos.getY() - d0 > 0.0D) return Direction.UP;
        }
        int l = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        return switch (l) {
            case 0 -> Direction.SOUTH;
            case 1 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            default -> Direction.EAST;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, determineOrientation(ctx.getClickedPos(), ctx.getPlayer()).getOpposite());
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        if (world.hasNeighborSignal(pos)) explode(world, pos);
    }

    @Override
    public BombReturnCode explode(Level world, BlockPos pos) {
        if (!world.isClientSide) {
            new ExplosionVNT(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20).makeStandard()
                    .setBlockProcessor(new BlockProcessorStandard().setNoDrop()).explode();
        }
        return BombReturnCode.DETONATED;
    }

    @Override
    public void explodeEntity(Level world, double x, double y, double z, @Nullable EntityTNTPrimedBase entity) {
        explode(world, BlockPos.containing(x, y, z));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
