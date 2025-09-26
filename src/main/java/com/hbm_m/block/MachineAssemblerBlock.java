package com.hbm_m.block;

import com.google.common.collect.ImmutableMap;
import com.hbm_m.block.entity.MachineAssemblerBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.multiblock.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;

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
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.network.NetworkHooks;

import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nullable;

public class MachineAssemblerBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    
    // public static final VoxelShape BASE_SHAPE_NORTH = Shapes.box(-1, 0, -1, 2, 2, 2);
    
    static final Lazy<Map<Direction, VoxelShape>> SHAPES_LAZY = Lazy.of(() -> {

        final double PIXEL = 1.0 / 16.0;

        VoxelShape base = Shapes.box(-1, 0, -1, 2, 2, 2);
        VoxelShape conveyorLeft = Shapes.box(-1.5, 0.0, 0.5, -0.5, 1.0, 1.5);
        VoxelShape conveyorRight = Shapes.box(2.0, 0.0, -0.5, 2.5, 1.0, 0.5);

        double wireY1 = 5.5 * PIXEL;
        double wireY2 = 10.5 * PIXEL;
        double wireX1_pos = 1.0 - 2.5 * PIXEL;
        double wireX1_neg = -1.0 + 13.5 * PIXEL;
        double wireX2_pos = 1.0 + 2.5 * PIXEL;
        double wireX2_neg = -1.0 + 18.5 * PIXEL;

        VoxelShape wireFrontLeft = Shapes.box(wireX1_neg, wireY1, -1.5, wireX2_neg, wireY2, -1.0);
        VoxelShape wireFrontRight = Shapes.box(wireX1_pos, wireY1, -1.5, wireX2_pos, wireY2, -1.0);
        
        VoxelShape wireBackLeft = Shapes.box(wireX1_neg, wireY1, 2.0, wireX2_neg, wireY2, 2.5);
        VoxelShape wireBackRight = Shapes.box(wireX1_pos, wireY1, 2.0, wireX2_pos, wireY2, 2.5);

        VoxelShape unalignedShape = Shapes.or(
            base, conveyorLeft, conveyorRight,
            wireFrontLeft, wireFrontRight, wireBackLeft, wireBackRight
        );

        // --- ШАГ 2: Создание ЕДИНОЙ, правильно смещенной мастер-формы для NORTH ---
        // Сначала применяем нужное смещение ОДИН РАЗ. Это наша эталонная форма.
        VoxelShape masterShapeNorth = unalignedShape.move(0.5, 0, 0.5);

        // --- ШАГ 3: Поворачиваем уже смещенную форму ---
        // Теперь мы вращаем не исходную форму, а ту, что уже правильно отцентрована.
        VoxelShape masterShapeS = rotateShape(masterShapeNorth, Rotation.CLOCKWISE_180);
        VoxelShape masterShapeW = rotateShape(masterShapeNorth, Rotation.COUNTERCLOCKWISE_90);
        VoxelShape masterShapeE = rotateShape(masterShapeNorth, Rotation.CLOCKWISE_90);

        // --- ШАГ 4: Собираем карту БЕЗ дополнительных смещений ---
        // Все смещения уже "запечены" в формы на предыдущих шагах.
        return ImmutableMap.<Direction, VoxelShape>builder()
                .put(Direction.NORTH, masterShapeNorth)
                .put(Direction.SOUTH,  masterShapeS)
                .put(Direction.WEST,   masterShapeW)
                .put(Direction.EAST,   masterShapeE)
                .build();
    });

    private static final MultiblockStructureHelper STRUCTURE_HELPER = new MultiblockStructureHelper(defineStructure());

        private static Map<BlockPos, Supplier<BlockState>> defineStructure() {
        ImmutableMap.Builder<BlockPos, Supplier<BlockState>> builder = ImmutableMap.builder();

        for (int y = 0; y <= 1; y++) {
            for (int x = -1; x <= 2; x++) {
                for (int z = -1; z <= 2; z++) {
                    if (y == 0 && x == 0 && z == 0) continue;
                    
                    // Создаем final-переменные для использования в лямбде
                    final int finalX = x;
                    final int finalY = y;
                    final int finalZ = z;

                    // Оборачиваем получение BlockState в лямбду (Supplier).
                    // Этот код НЕ будет выполнен немедленно. Он будет ждать, пока его не вызовут.
                    builder.put(new BlockPos(x, y, z), () -> 
                        ModBlocks.MACHINE_ASSEMBLER_PART.get().defaultBlockState()
                            .setValue(MachineAssemblerPartBlock.OFFSET_X, finalX + 1)
                            .setValue(MachineAssemblerPartBlock.OFFSET_Y, finalY)
                            .setValue(MachineAssemblerPartBlock.OFFSET_Z, finalZ + 1)
                    );
                }
            }
        }
        return builder.build();
    }
    
    public MachineAssemblerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return STRUCTURE_HELPER;
    }

    @Override
    public VoxelShape getCustomMasterVoxelShape(BlockState state) {
        // Наш сборщик - особенный. Мы возвращаем его сложную, вручную
        // созданную форму. Это переопределит стандартное поведение.
        return SHAPES_LAZY.get().get(state.getValue(FACING));
    }

    @Override
    public void setPlacedBy(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nullable LivingEntity placer, @Nonnull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        // Мы больше не проверяем здесь, мы просто строим.
        // BlockItem уже гарантировал, что место свободно.
        if (!level.isClientSide) {
            STRUCTURE_HELPER.placeStructure(level, pos, state.getValue(FACING));
        }
    }

    // РАЗРУШЕНИЕ СТРУКТУРЫ 
    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineAssemblerBlockEntity assemblerEntity) {
                assemblerEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(i));
                    }
                });
            }
            STRUCTURE_HELPER.destroyStructure(level, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, isMoving);
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

    // @Override
    // public VoxelShape getShape(@Nonnull BlockState pState, @Nonnull BlockGetter pLevel, @Nonnull BlockPos pPos, @Nonnull CollisionContext pContext) {
    //     // Просто берем нужную форму из кэша
    //     return SHAPES_LAZY.get().get(pState.getValue(FACING));
    // }

    @Override
    public VoxelShape getShape(@Nonnull BlockState pState, @Nonnull BlockGetter pLevel, @Nonnull BlockPos pPos, @Nonnull CollisionContext pContext) {
        return getCustomMasterVoxelShape(pState);
    }

    // @Override
    // public VoxelShape getCollisionShape(@Nonnull BlockState pState, @Nonnull BlockGetter pLevel, @Nonnull BlockPos pPos, @Nonnull CollisionContext pContext) {
    //     return SHAPES_LAZY.get().get(pState.getValue(FACING));
    // }

    @Override
    public VoxelShape getCollisionShape(@Nonnull BlockState pState, @Nonnull BlockGetter pLevel, @Nonnull BlockPos pPos, @Nonnull CollisionContext pContext) {
        // Физика и выделение для контроллера совпадают.
        return getCustomMasterVoxelShape(pState);
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