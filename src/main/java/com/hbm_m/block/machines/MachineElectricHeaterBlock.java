package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineElectricHeaterBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.item.ModItems;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
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

/**
 * Electric Heater — порт {@code HeaterElectric} (1.7.10, BlockDummyable
 * {@code {0,0,1,2,1,1}}, offset 2): мультиблок 4×3×1 — 3 клетки вперёд по FACING
 * и 1 назад, контроллер у заднего торца по центру. Отвёртка крутит настройку
 * мощности 0..10, остальной клик — пустой (GUI у нагревателя нет).
 */
public class MachineElectricHeaterBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final MultiblockStructureHelper structureHelper;

    public MachineElectricHeaterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructure();
    }

    /**
     * 4×3×1: строки сетки идут с севера (перед) на юг; контроллер в заднем ряду
     * по центру — как в оригинале (ядро у заднего торца, корпус вперёд).
     */
    private static MultiblockStructureHelper defineStructure() {
        String[] layer = { "OOO", "OOO", "OOO", "OCO" };

        Map<Character, PartRole> roleMap = Map.of(
                'O', PartRole.DEFAULT,
                'C', PartRole.CONTROLLER
        );

        Map<Character, Supplier<BlockState>> symbolMap = Map.of();

        return MultiblockStructureHelper.createFromLayersWithRoles(
                new String[][] { layer },
                symbolMap,
                () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
                roleMap,
                null,
                null
        );
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return this.structureHelper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        return structureHelper.resolvePartRole(localOffset, this);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return structureHelper.generateShapeFromParts(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return structureHelper.getSpecificPartShape(structureHelper.getControllerOffset(), state.getValue(FACING));
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        if (!structureHelper.isFullBlock(structureHelper.getControllerOffset(), state.getValue(FACING))) {
            return Shapes.empty();
        }
        return Shapes.block();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineElectricHeaterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ELECTRIC_HEATER_BE.get(),
                (lvl, pos, st, be) -> MachineElectricHeaterBlockEntity.tick(lvl, pos, st, (MachineElectricHeaterBlockEntity) be));
    }


    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            structureHelper.placeStructure(level, pos, state.getValue(FACING), this);
        }
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hbmOnUse(state, level, pos, player, hand);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return hbmOnUse(state, level, pos, player, InteractionHand.MAIN_HAND);
    }
    *///?}

    private InteractionResult hbmOnUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() == ModItems.SCREWDRIVER.get()) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MachineElectricHeaterBlockEntity be) {
                be.cycleSetting();
                player.displayClientMessage(Component.translatable("gui.hbm_m.electric_heater.setting", be.getSetting()), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            structureHelper.destroyStructure(level, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineElectricHeaterBlock> CODEC = simpleCodec(MachineElectricHeaterBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
