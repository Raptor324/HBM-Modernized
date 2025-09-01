package com.hbm_m.block;

import com.hbm_m.block.entity.AdvancedAssemblyMachinePartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class AdvancedAssemblyMachinePartBlock extends BaseEntityBlock {

    public static final IntegerProperty OFFSET_X = IntegerProperty.create("offset_x", 0, 2);
    public static final IntegerProperty OFFSET_Y = IntegerProperty.create("offset_y", 0, 2);
    public static final IntegerProperty OFFSET_Z = IntegerProperty.create("offset_z", 0, 2);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public AdvancedAssemblyMachinePartBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(OFFSET_X, 1)
                .setValue(OFFSET_Y, 0)
                .setValue(OFFSET_Z, 1)
                .setValue(FACING, Direction.NORTH));
    }

    // --- ЛОГИКА VOXELSHAPE (ИСПРАВЛЕНО) ---
    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        // Получаем смещение этого блока относительно контроллера (-1, 0, 1 и т.д.)
        int offsetX = pState.getValue(OFFSET_X) - 1;
        int offsetY = pState.getValue(OFFSET_Y);
        int offsetZ = pState.getValue(OFFSET_Z) - 1;

        // Поворачиваем вектор смещения в соответствии с направлением всей машины
        BlockPos rotatedOffset = rotate(new BlockPos(offsetX, offsetY, offsetZ), pState.getValue(FACING));

        // Смещаем МАСТЕР-ФОРМУ на ОБРАТНЫЙ вектор смещения.
        // Это заставит полную форму "переместиться" и центрироваться на этой части.
        return AdvancedAssemblyMachineBlock.MASTER_SHAPE.move(-rotatedOffset.getX(), -rotatedOffset.getY(), -rotatedOffset.getZ());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return this.getShape(pState, pLevel, pPos, pContext);
    }

    // --- Остальной код ---
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(OFFSET_X, OFFSET_Y, OFFSET_Z, FACING);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack(ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get());
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pState.is(pNewState.getBlock())) {
            if (pLevel.getBlockEntity(pPos) instanceof AdvancedAssemblyMachinePartBlockEntity partBe) {
                BlockPos controllerPos = partBe.getControllerPos();
                if (controllerPos != null && pLevel.getBlockState(controllerPos).is(ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get())) {
                    pLevel.destroyBlock(controllerPos, true);
                }
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }
    
    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide) return InteractionResult.SUCCESS;
        if (pLevel.getBlockEntity(pPos) instanceof AdvancedAssemblyMachinePartBlockEntity partBe) {
            BlockPos controllerPos = partBe.getControllerPos();
            if (controllerPos != null) {
                BlockState controllerState = pLevel.getBlockState(controllerPos);
                return controllerState.use(pLevel, pPlayer, pHand, pHit.withPosition(controllerPos));
            }
        }
        return InteractionResult.FAIL;
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new AdvancedAssemblyMachinePartBlockEntity(pPos, pState);
    }
    
    // Вспомогательный метод для поворота вектора смещения
    private static BlockPos rotate(BlockPos pos, Direction facing) {
        return switch (facing) {
            case SOUTH -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            case WEST -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            case EAST -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            default -> pos; // NORTH
        };
    }
}