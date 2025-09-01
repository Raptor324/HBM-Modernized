package com.hbm_m.block;

import com.hbm_m.block.entity.MachineAssemblerPartBlockEntity;

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

public class MachineAssemblerPartBlock extends BaseEntityBlock {

    public static final IntegerProperty OFFSET_X = IntegerProperty.create("offset_x", 0, 3); // Было: -1, 1
    public static final IntegerProperty OFFSET_Y = IntegerProperty.create("offset_y", 0, 1); // Это свойство было в порядке
    public static final IntegerProperty OFFSET_Z = IntegerProperty.create("offset_z", 0, 3);

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;


    public MachineAssemblerPartBlock(Properties properties) {
        super(properties);
        // Задаем состояние по умолчанию. (0,0,0) - это сам контроллер, но такого блока у нас не будет.
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(OFFSET_X, 1) // Значение по умолчанию для "нулевого" смещения (0 + 1)
                .setValue(OFFSET_Y, 0)
                .setValue(OFFSET_Z, 1) // Значение по умолчанию для "нулевого" смещения (0 + 1)
                .setValue(FACING, Direction.NORTH));
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(OFFSET_X, OFFSET_Y, OFFSET_Z, FACING);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new MachineAssemblerPartBlockEntity(pPos, pState);
    }
    
    
    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        // За физику отвечает только контроллер. Фиктивные блоки всегда нематериальны.
        return this.getShape(pState, pLevel, pPos, pContext);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        Direction facing = pState.getValue(FACING);
        
        // 1. Берем повернутую мастер-форму, как и раньше
        VoxelShape masterShape = MachineAssemblerBlock.SHAPES_LAZY.get().get(facing);

        // 2. Вычисляем локальное смещение блока от контроллера
        // Эта логика восстанавливает исходные координаты x (-1..2) и z (-2..1)
        int localX = pState.getValue(OFFSET_X) - 1;
        int localY = pState.getValue(OFFSET_Y);
        int localZ = pState.getValue(OFFSET_Z) - 1;

        // 3. Создаем вектор смещения. Чтобы отобразить часть структуры,
        // находящуюся в точке (x,y,z), нам нужно сдвинуть всю мастер-форму
        // на вектор (-x, -y, -z).
        BlockPos offsetVector = new BlockPos(-localX, -localY, -localZ);

        // 4. Вращаем этот вектор смещения в соответствии с направлением блока.
        // Это КЛЮЧЕВОЙ шаг, который исправляет ошибку.
        BlockPos rotatedOffset = rotate(offsetVector, facing);

        // 5. Применяем повернутое смещение к мастер-форме.
        return masterShape.move(rotatedOffset.getX(), rotatedOffset.getY(), rotatedOffset.getZ());
    }

    /**
     * Локальный хелпер-метод для вращения вектора смещения.
     * Он должен быть идентичен методу rotate в MultiblockStructureHelper.
     */
    private BlockPos rotate(BlockPos pos, Direction facing) {
        return switch (facing) {
            case SOUTH -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            case WEST -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            case EAST -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            default -> pos; // NORTH
        };
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MachineAssemblerPartBlockEntity part) {
            BlockPos controllerPos = part.getControllerPos();
            if (controllerPos != null) {
                BlockState controllerState = level.getBlockState(controllerPos);

                // Добавим проверку, что контроллер все еще на месте.
                // Это хорошая практика, чтобы избежать крашей, если структура была повреждена.
                if (controllerState.is(ModBlocks.MACHINE_ASSEMBLER.get())) {
                    // Перенаправляем серверный вызов на контроллер.
                    // Метод 'use' контроллера теперь будет отвечать за отправку сообщения
                    // и другую серверную логику.
                    return controllerState.use(level, player, hand, hit.withPosition(controllerPos));
                }
            }
        }

        // Если по какой-то причине контроллер не найден, взаимодействие не удалось.
        return InteractionResult.FAIL;
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pState.is(pNewState.getBlock())) {
            if (pLevel.getBlockEntity(pPos) instanceof MachineAssemblerPartBlockEntity part) {
                BlockPos controllerPos = part.getControllerPos();
                if (controllerPos != null && pLevel.getBlockState(controllerPos).is(ModBlocks.MACHINE_ASSEMBLER.get())) {
                    // Уничтожаем основной блок, что вызовет каскадное уничтожение остальных частей
                    pLevel.destroyBlock(controllerPos, true);
                }
            }
            super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
        }
    }
    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof MachineAssemblerPartBlockEntity part) {
            BlockPos controllerPos = part.getControllerPos();
            if (controllerPos != null) {
                BlockState controllerState = level.getBlockState(controllerPos);
                // Проверяем, что контроллер на месте и является нужным блоком
                if (controllerState.is(ModBlocks.MACHINE_ASSEMBLER.get())) {
                    // Возвращаем предмет, соответствующий главному блоку
                    return new ItemStack(ModBlocks.MACHINE_ASSEMBLER.get());
                }
            }
        }
        // Возвращаем пустой стак, если что-то пошло не так
        return ItemStack.EMPTY;
    }
}