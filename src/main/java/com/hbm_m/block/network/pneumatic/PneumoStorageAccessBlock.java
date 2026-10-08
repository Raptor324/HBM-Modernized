package com.hbm_m.block.network.pneumatic;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.pneumatic.PneumoStorageAccessBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Port von {@code PneumoStorageAccess} (1.7.10). Das Zugangsterminal: zeigt alles, was im Netz in Reichweite liegt, als eine Liste.
 * Die 6-Wege-Ausrichtung ({@link #FACING}) bestimmt im Original nur die Fronttextur - das Tile liest kein Metadatum.
 */
public class PneumoStorageAccessBlock extends PneumaticStorageBlockBase {

    /** Original Metadatum 0-5 aus {@code BlockPistonBase.determineOrientation}; die Seite {@code meta == side} zeigt die Front. */
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
            net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

    public PneumoStorageAccessBlock(Properties properties) {
        super(properties);
        // Original-Standardmetadatum 0 = unten
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.DOWN));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Original {@code onBlockPlacedBy}: {@code BlockPistonBase.determineOrientation}, Front zeigt zum Spieler. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        net.minecraft.world.entity.player.Player player = context.getPlayer();
        if (player == null) return this.defaultBlockState();
        return this.defaultBlockState().setValue(FACING, determineOrientation(context.getClickedPos(), player));
    }

    /** 1:1 {@code BlockPistonBase.determineOrientation} (Augenhoehe = Fuesse + 1,82 wie {@code posY + 1.82 - yOffset}). */
    public static net.minecraft.core.Direction determineOrientation(BlockPos pos, net.minecraft.world.entity.LivingEntity player) {
        if (Math.abs((float) player.getX() - (float) pos.getX()) < 2.0F && Math.abs((float) player.getZ() - (float) pos.getZ()) < 2.0F) {
            double d0 = player.getY() + 1.82D;

            if (d0 - (double) pos.getY() > 2.0D) {
                return net.minecraft.core.Direction.UP;
            }

            if ((double) pos.getY() - d0 > 0.0D) {
                return net.minecraft.core.Direction.DOWN;
            }
        }

        int l = net.minecraft.util.Mth.floor((double) (player.getYRot() * 4.0F / 360.0F) + 0.5D) & 3;
        return l == 0 ? net.minecraft.core.Direction.NORTH : l == 1 ? net.minecraft.core.Direction.EAST
                : l == 2 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.WEST;
    }

    @Override
    public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PneumoStorageAccessBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PNEUMO_STORAGE_ACCESS_BE.get(),
                (lvl, p, st, be) -> PneumoStorageAccessBlockEntity.tick(lvl, p, st, (PneumoStorageAccessBlockEntity) be));
    }

    /** audit10: Original PneumoStorageAccess hat kein breakBlock - beim Entfernen faellt nichts heraus. */
    @Override
    protected boolean spillOnRemove(Level level, BlockPos pos) {
        return false;
    }
}
