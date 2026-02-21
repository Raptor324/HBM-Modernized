package com.hbm_m.block.custom.machines.anvils;

import com.hbm_m.block.entity.custom.machines.AnvilBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnvilBlock extends FallingBlock implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE_X = Shapes.box(4 / 16.0D, 0, 0, 12 / 16.0D, 12 / 16.0D, 1);
    private static final VoxelShape SHAPE_Z = Shapes.box(0, 0, 4 / 16.0D, 1, 12 / 16.0D, 12 / 16.0D);

    private final AnvilTier tier;

    public AnvilBlock(Properties properties, AnvilTier tier) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public AnvilTier getTier() {
        return tier;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnvilBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // AnvilBlockEntity не требует тикера
        return null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_Z : SHAPE_X;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof AnvilBlockEntity be) {
                be.drops();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AnvilBlockEntity be) {
            NetworkHooks.openScreen((ServerPlayer) player, be, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Настраивает параметры урона для падающей наковальни.
     * Вызывается при создании FallingBlockEntity.
     * Скопировано из ванильной наковальни Minecraft.
     */
    @Override
    protected void falling(FallingBlockEntity fallingEntity) {
        // Устанавливаем урон: 2.0F за единицу расстояния падения, максимум 40 урона
        fallingEntity.setHurtsEntities(2.0F, 40);
    }

    /**
     * Возвращает источник урона для падающей наковальни.
     * Используется для правильного сообщения о смерти: "раздавлен упавшей наковальней".
     * Скопировано из ванильной наковальни Minecraft.
     */
    @Override
    public DamageSource getFallDamageSource(Entity entity) {
        return entity.damageSources().anvil(entity);
    }

    /**
     * Обработка приземления падающей наковальни.
     * Воспроизводит звук приземления.
     * Скопировано из ванильной наковальни Minecraft.
     */
    @Override
    public void onLand(Level level, BlockPos pos, BlockState fallingState, BlockState groundState, FallingBlockEntity fallingEntity) {
        if (!fallingEntity.isSilent()) {
            level.levelEvent(1031, pos, 0);
        }
    }

    /**
     * Обработка разрушения наковальни после падения.
     * Воспроизводит звук разрушения.
     * Скопировано из ванильной наковальни Minecraft.
     */
    @Override
    public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity fallingEntity) {
        if (!fallingEntity.isSilent()) {
            level.levelEvent(1029, pos, 0);
        }
    }
}