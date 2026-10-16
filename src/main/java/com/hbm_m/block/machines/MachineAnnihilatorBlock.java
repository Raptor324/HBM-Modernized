package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineAnnihilatorBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
 * Annihilator - Port von {@code MachineAnnihilator} (1.7.10 Original), auf diesem Repo-eigenen
 * {@link IMultiblockController}/{@link MultiblockStructureHelper}-Framework (NICHT das veraltete
 * 1.7.10 {@code BlockDummyable}/{@code MultiblockHandlerXR}-System), analog zu
 * {@link MachineArcFurnaceBlock}.
 * <p>
 * Footprint: 3 breit x 2 tief, 1 Ebene, Controller mittig hinten. Vereinfacht-aber-proportionaler
 * Ersatz fuer das Original-Dimension-Array {@code {2,0,4,4,1,1}} (plus zusaetzliches Satelliten-
 * Strukturstueck und 3 dekorative "extra"-Bloecke fuer Foerderband/Walzen-Optik) - wie schon bei
 * der Arc Furnace wird das vorhandene kombinierte OBJ-Modell ({@code block/machines/annihilator.json})
 * nur an der Controller-Zelle angezeigt; die rotierende Walze und das laufende Foerderband aus dem
 * Original-Renderer wurden nicht uebernommen (siehe Klassenkommentar in
 * {@link MachineAnnihilatorBlockEntity}).
 */
public class MachineAnnihilatorBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final MultiblockStructureHelper structureHelper;

    public MachineAnnihilatorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructure();
    }

    private static MultiblockStructureHelper defineStructure() {
        String[] layer = {
            "OOO",
            "OCO"
        };

        Map<Character, PartRole> roleMap = Map.of(
                'O', PartRole.DEFAULT,
                'C', PartRole.CONTROLLER
        );

        Map<Character, Supplier<net.minecraft.world.level.block.state.BlockState>> symbolMap = Map.of();

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
        return new MachineAnnihilatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return createTickerHelper(
                type,
                ModBlockEntities.ANNIHILATOR_BE.get(),
                (lvl, pos, st, be) -> MachineAnnihilatorBlockEntity.tick(lvl, pos, st, (MachineAnnihilatorBlockEntity) be)
        );
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {

        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MachineAnnihilatorBlockEntity annihilatorEntity) {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, annihilatorEntity, buf -> buf.writeBlockPos(pos));
            } else {
                throw new IllegalStateException("Container provider is missing!");
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
        }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MachineAnnihilatorBlockEntity annihilatorEntity) {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, annihilatorEntity, buf -> buf.writeBlockPos(pos));
            } else {
                throw new IllegalStateException("Container provider is missing!");
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
        }
    *///?}


    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                          BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineAnnihilatorBlockEntity annihilatorEntity) {
                annihilatorEntity.dropInventoryContents();
            }
            if (!level.isClientSide()) {
                structureHelper.destroyStructure(level, pos, state.getValue(FACING));
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineAnnihilatorBlock> CODEC = simpleCodec(MachineAnnihilatorBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
