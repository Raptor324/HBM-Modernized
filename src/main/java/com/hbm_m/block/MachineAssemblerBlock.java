package com.hbm_m.block;

import com.google.common.collect.ImmutableMap;
import com.hbm_m.block.entity.MachineAssemblerBlockEntity;
import com.hbm_m.block.entity.MachineAssemblerPartBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.NetworkHooks;

import java.util.Map;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nullable;

public class MachineAssemblerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public static final VoxelShape BASE_SHAPE_NORTH = Shapes.box(-1, 0, -1, 2, 2, 2);

    // КЭШ ДЛЯ ХРАНЕНИЯ ПОВЕРНУТЫХ ФОРМ
    static final Map<Direction, VoxelShape> SHAPES;

    static {

        // Константа для удобства расчетов. 1 пиксель = 1/16 блока.
        final double PIXEL = 1.0 / 16.0;

        // 1. ОПРЕДЕЛЯЕМ ВСЕ ДОПОЛНИТЕЛЬНЫЕ КОЛЛАЙДЕРЫ ДЛЯ НАПРАВЛЕНИЯ NORTH 
        // Все координаты относительны центрального блока (0,0,0)

        // Конвейеры по бокам: размер 1х1 блок, выпирают на 0.5 блока (8 пикселей)
        // Размещаем их по центру боковых граней основного тела (Y от 0.5 до 1.5, Z от 0 до 1)
        VoxelShape conveyorLeft = Shapes.box(
                -1.5,                   // minX
                0.0,           //minY 
                0.5,           //minZ

                -0.5,                   //maxX
                1.0,           //maxY
                1.5            //maxZ
        );
        VoxelShape conveyorRight = Shapes.box(
                2.0,          
                0.0,
                -0.5,

                2.5,           
                1.0,
                0.5
        );

        // Провода
        double wireY1 = 5.5 * PIXEL;
        double wireY2 = 10.5 * PIXEL;
        // ширина
        double wireX1_pos = 1.0 - 2.5 * PIXEL;
        double wireX1_neg = -1.0 + 13.5 * PIXEL;
        double wireX2_pos = 1.0 + 2.5 * PIXEL;
        double wireX2_neg = -1.0 + 18.5 * PIXEL;


        // Передние провода (выпирают из грани Z = -1)
        VoxelShape wireFrontLeft = Shapes.box(wireX1_neg, wireY1, -1.5, wireX2_neg, wireY2, -1.0);
        VoxelShape wireFrontRight = Shapes.box(wireX1_pos, wireY1, -1.5, wireX2_pos, wireY2, -1.0);
        
        // Задние провода (выпирают из грани Z = 2)
        VoxelShape wireBackLeft = Shapes.box(wireX1_neg, wireY1, 2.0, wireX2_neg, wireY2, 2.5);
        VoxelShape wireBackRight = Shapes.box(wireX1_pos, wireY1, 2.0, wireX2_pos, wireY2, 2.5);

        // 2. ОБЪЕДИНЯЕМ ВСЕ ЧАСТИ В ОДНУ ОБЩУЮ ФОРМУ ДЛЯ НАПРАВЛЕНИЯ NORTH 
        VoxelShape masterShapeNorth = Shapes.or(
                BASE_SHAPE_NORTH,
                conveyorLeft,
                conveyorRight,
                wireFrontLeft,
                wireFrontRight,
                wireBackLeft,
                wireBackRight
        );

        // 3. ПОВОРАЧИВАЕМ И СДВИГАЕМ ЭТУ КОМБИНИРОВАННУЮ ФОРМУ 
        
        // Сначала создаем повернутые мастер-формы
        VoxelShape masterShapeS = rotateShape(masterShapeNorth, Rotation.CLOCKWISE_180);
        VoxelShape masterShapeW = rotateShape(masterShapeNorth, Rotation.COUNTERCLOCKWISE_90);
        VoxelShape masterShapeE = rotateShape(masterShapeNorth, Rotation.CLOCKWISE_90);

        // Теперь применяем смещение к каждой повернутой форме
        // Смещение: 0.5 блока "влево" и 0.5 блока "вперед" относительно направления
        SHAPES = ImmutableMap.<Direction, VoxelShape>builder()
                // North (Вперед: -Z, Влево: -X)
                .put(Direction.NORTH, masterShapeNorth.move(0.5, 0, 0.5))
                // South (Вперед: +Z, Влево: +X)
                .put(Direction.SOUTH,  masterShapeS.move(-0.5, 0, -0.5))
                // West (Вперед: -X, Влево: +Z)
                .put(Direction.WEST, masterShapeW.move(0.5, 0, -0.5))
                // East (Вперед: +X, Влево: -Z)
                .put(Direction.EAST, masterShapeE.move(-0.5, 0, 0.5))
                .build();
    }
    
    public MachineAssemblerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public void setPlacedBy(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable LivingEntity placer, @Nonnull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            placeStructure(level, pos, state.getValue(FACING));
        }
    }

    private void placeStructure(Level level, BlockPos controllerPos, Direction facing) {
        for (int yOffset = 0; yOffset <= 1; yOffset++) {
            for (int xzOffset = -1; xzOffset <= 2; xzOffset++) {
                for (int depthOffset = -2; depthOffset <= 1; depthOffset++) { 
                    if (yOffset == 0 && xzOffset == 0 && depthOffset == 0) continue;

                    BlockPos partPos = controllerPos.above(yOffset)
                            .relative(facing.getClockWise(), xzOffset)
                            .relative(facing, depthOffset);


                    BlockState partState = ModBlocks.MACHINE_ASSEMBLER_PART.get().defaultBlockState()
                            .setValue(MachineAssemblerPartBlock.OFFSET_X, xzOffset + 1)
                            .setValue(MachineAssemblerPartBlock.OFFSET_Y, yOffset)
                            .setValue(MachineAssemblerPartBlock.OFFSET_Z, depthOffset + 2)
                            .setValue(MachineAssemblerPartBlock.FACING, facing);

                    level.setBlock(partPos, partState, 3);

                    BlockEntity be = level.getBlockEntity(partPos);
                    if (be instanceof MachineAssemblerPartBlockEntity partBe) {
                        partBe.setControllerPos(controllerPos);
                    }
                }
            }
        }
    }

    // РАЗРУШЕНИЕ СТРУКТУРЫ 
    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineAssemblerBlockEntity assemblerEntity) {
                // "Выбрасываем" все предметы из инвентаря машины в мир
                assemblerEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(i));
                    }
                });
            }
        }
        // Вызываем обновленный метод уничтожения
        destroyStructure(level, pos, state.getValue(FACING));
        super.onRemove(state, level, pos, newState, isMoving);
    }
    
    // УНИЧТОЖЕНИЕ СТРУКТУРЫ 4x4x2 
    private void destroyStructure(Level level, BlockPos controllerPos, Direction facing) {
        for (int yOffset = 0; yOffset <= 1; yOffset++) {
            for (int xzOffset = -1; xzOffset <= 2; xzOffset++) {
                for (int depthOffset = -2; depthOffset <= 1; depthOffset++) {
                    if (yOffset == 0 && xzOffset == 0 && depthOffset == 0) continue;

                    BlockPos partPos = controllerPos.above(yOffset)
                            .relative(facing.getClockWise(), xzOffset)
                            .relative(facing, depthOffset);

                    if (level.getBlockState(partPos).is(ModBlocks.MACHINE_ASSEMBLER_PART.get())) {
                        level.setBlock(partPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    // Взаимодействие (открытие GUI)
    @Override
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MenuProvider) {
                if (player instanceof ServerPlayer serverPlayer) {
                    NetworkHooks.openScreen(serverPlayer, (MenuProvider) entity, pos);
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public VoxelShape getShape(@Nonnull BlockState pState, @Nonnull BlockGetter pLevel, @Nonnull BlockPos pPos, @Nonnull CollisionContext pContext) {
        // Просто берем нужную форму из кэша
        return SHAPES.get(pState.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(@Nonnull BlockState pState, @Nonnull BlockGetter pLevel, @Nonnull BlockPos pPos, @Nonnull CollisionContext pContext) {
        return SHAPES.get(pState.getValue(FACING));
    }

    // ХЕЛПЕР-МЕТОД ДЛЯ ПОВОРОТА VOXELSHAPE 
    private static VoxelShape rotateShape(VoxelShape shape, Rotation rotation) {
        VoxelShape[] buffer = {Shapes.empty()}; // Используем массив, чтобы его можно было изменять внутри лямбды
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            // Центр блока (0.5, 0.5) используется как точка вращения
            buffer[0] = Shapes.or(buffer[0], switch (rotation) {
                case CLOCKWISE_90 -> Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX);
                case CLOCKWISE_180 -> Shapes.box(1 - maxX, minY, 1 - maxZ, 1 - minX, maxY, 1 - minZ);
                case COUNTERCLOCKWISE_90 -> Shapes.box(minZ, minY, 1 - maxX, maxZ, maxY, 1 - minX);
                default -> shape; // Для NONE
            });
        });
        return buffer[0];
    }

    @Override
    public RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    // Стандартный код для BaseEntityBlock 
    @Nullable @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new MachineAssemblerBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MACHINE_ASSEMBLER_BE.get(), MachineAssemblerBlockEntity::tick);
    }
    
    @Nullable @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}