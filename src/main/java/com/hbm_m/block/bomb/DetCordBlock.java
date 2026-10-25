package com.hbm_m.block.bomb;

import java.util.Map;

import javax.annotation.Nullable;

import com.hbm_m.api.block.IDetConnectible;
import com.hbm_m.entity.item.EntityTNTPrimedBase;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code DetCord}: Zuendschnur. Redstone laesst sie mit Staerke 1.5 platzen; Explosionen machen daraus einen
 * sofort zuendenden Sprengkoerper, so laeuft die Kette die Schnur entlang. Die sechs Anschluss-Eigenschaften spiegeln
 * {@code RenderDetCord} (Nachbarn mit {@link IDetConnectible}).
 */
public class DetCordBlock extends BlockDetonatable implements IDetConnectible {

    private static final Map<Direction, BooleanProperty> PROPS = PipeBlock.PROPERTY_BY_DIRECTION;

    public DetCordBlock(Properties properties) {
        super(properties, 0, 0, 0, false, false);
        BlockState s = stateDefinition.any();
        for (BooleanProperty p : PROPS.values()) s = s.setValue(p, false);
        registerDefaultState(s);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PROPS.values().toArray(new BooleanProperty[0]));
    }

    private BlockState connect(BlockGetter world, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values()) {
            state = state.setValue(PROPS.get(d), IDetConnectible.isConnectible(world, pos.relative(d), d.getOpposite()));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return connect(ctx.getLevel(), ctx.getClickedPos(), defaultBlockState());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PROPS.get(dir), IDetConnectible.isConnectible(world, neighborPos, dir.getOpposite()));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        return Shapes.block();
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        if (world.hasNeighborSignal(pos)) explode(world, pos);
    }

    public void explode(Level world, BlockPos pos) {
        if (!world.isClientSide) {
            world.removeBlock(pos, false);
            world.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1.5F, Level.ExplosionInteraction.TNT);
        }
    }

    @Override
    public void explodeEntity(Level world, double x, double y, double z, @Nullable EntityTNTPrimedBase entity) {
        explode(world, BlockPos.containing(x, y, z));
    }
}
