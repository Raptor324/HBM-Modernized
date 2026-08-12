package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineChungusBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Chungus / Leviathan Steam Turbine - das Endgame-Upgrade zur Industrial Turbine.
 * Multiblock-Struktur rekonstruiert aus den Original-Dateien MachineChungus.java,
 * BlockDummyable.java und MultiblockHandlerXR.java (siehe Plan-Dokumentation):
 * Hauptkörper (5 breit x 4 hoch x 4 tief vor dem Controller) + schmalere Kappe oben drauf +
 * ein nach hinten auslaufender, sich verjüngender Heck-Schacht (insgesamt 10 Blöcke hinter
 * dem Controller) mit dem Energie-Port an der Spitze, plus 2 seitliche und 1 vorderer
 * Fluid-Port (UNIVERSAL_CONNECTOR).
 */
public class MachineChungusBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final MultiblockStructureHelper structureHelper;

    public MachineChungusBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructure();
    }

    private static MultiblockStructureHelper defineStructure() {
        // Layer-Raster: jede Zeile = eine Tiefenreihe (Z, Reihe 0 = depth-10 .. Reihe 20 = depth+10),
        // jedes Zeichen = eine Seiten-Spalte (X, Spalte 0 = side-2 .. Spalte 4 = side+2).
        // Symmetrisch auf 21x5 gepolstert, damit der Controller exakt im Array-Zentrum landet (lokal 0,0,0).
        String[] height0 = {
            " OEO ", " OOO ", " OOO ", " OOO ",   // Heck-Schacht fern (depth -10..-7), Rückconnector bei -10
            " OOO ", " OOO ", " OOO ", " OOO ", " OOO ", " OOO ", // Heck-Schacht nah (depth -6..-1)
            "UOCOU",                                              // Controller-Reihe (depth 0) mit Seiten-Connectoren
            "OOOOO", "OOOOO", "OOOOO",                            // Hauptkörper (depth 1..3)
            "     ",                                              // depth+4 (kein Block auf Bodenebene)
            "     ", "     ", "     ", "     ", "     ", "     "  // depth+5..+10 (ungenutztes Padding)
        };
        String[] height1 = {
            " OOO ", " OOO ", " OOO ", " OOO ",
            " OOO ", " OOO ", " OOO ", " OOO ", " OOO ", " OOO ",
            "OOOOO",
            "OOOOO", "OOOOO", "OOOOO",
            "     ",
            "     ", "     ", "     ", "     ", "     ", "     "
        };
        String[] height2 = {
            " OOO ", " OOO ", " OOO ", " OOO ",
            " OOO ", " OOO ", " OOO ", " OOO ", " OOO ", " OOO ",
            "OOOOO",
            "OOOOO", "OOOOO", "OOOOO",
            "  U  ",                                              // Front-Connector bei depth+4
            "     ", "     ", "     ", "     ", "     ", "     "
        };
        String[] height3 = {
            "     ", "     ", "     ", "     ",                   // Heck-Schacht fern endet bei Höhe 2
            " OOO ", " OOO ", " OOO ", " OOO ", " OOO ", " OOO ",
            "OOOOO",
            "OOOOO", "OOOOO", "OOOOO",
            "     ",
            "     ", "     ", "     ", "     ", "     ", "     "
        };
        String[] height4 = {
            "     ", "     ", "     ", "     ",
            "     ", "     ", "     ", "     ", "     ", "     ",
            " OOO ",                                              // Kappe (schmaler als Hauptkörper)
            " OOO ", " OOO ", " OOO ",
            "     ",
            "     ", "     ", "     ", "     ", "     ", "     "
        };

        Map<Character, PartRole> roleMap = Map.of(
                'C', PartRole.CONTROLLER,
                'O', PartRole.DEFAULT,
                'E', PartRole.ENERGY_CONNECTOR,
                'U', PartRole.UNIVERSAL_CONNECTOR
        );

        Map<Character, Supplier<BlockState>> symbolMap = Map.of();

        return MultiblockStructureHelper.createFromLayersWithRoles(
                new String[][] { height0, height1, height2, height3, height4 },
                symbolMap,
                () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
                roleMap,
                null,
                null
        );
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            Direction facing = state.getValue(FACING);
            structureHelper.placeStructure(level, pos, facing, this);

            EnergyNetworkManager.get((ServerLevel) level).addNode(pos);

            // Alle Strukturblöcke registrieren (nicht nur Connector-Rollen), damit die Maschine eine
            // durchgehende physische Kette im EnergyNetworkManager bildet (der Netzwerke rein über
            // direkte Block-Nachbarschaft bildet). Bei Chungus liegt der Energie-Connector bis zu
            // 10 Blöcke vom Controller entfernt - ohne die dazwischenliegenden Knoten würden
            // Controller und Connector in zwei isolierten Netzwerken landen.
            for (BlockPos gridPos : structureHelper.getStructureMap().keySet()) {
                BlockPos worldPos = structureHelper.getRotatedPos(pos, gridPos, facing);
                EnergyNetworkManager.get((ServerLevel) level).addNode(worldPos);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && !level.isClientSide()) {
            Direction facing = state.getValue(FACING);

            EnergyNetworkManager.get((ServerLevel) level).removeNode(pos);

            for (BlockPos gridPos : structureHelper.getStructureMap().keySet()) {
                BlockPos worldPos = structureHelper.getRotatedPos(pos, gridPos, facing);
                EnergyNetworkManager.get((ServerLevel) level).removeNode(worldPos);
            }

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineChungusBlockEntity chungus) {
                chungus.drops();
            }

            structureHelper.destroyStructure(level, pos, facing);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineChungusBlockEntity(pos, state);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return handleUse(state, level, pos, player, hand, hit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return handleUse(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
    *///?}

    private InteractionResult handleUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof MachineChungusBlockEntity chungus) {
                boolean nowOperational = chungus.toggleOperational();
                player.displayClientMessage(Component.translatable(
                        nowOperational ? "chat.hbm_m.chungus.on" : "chat.hbm_m.chungus.off"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MACHINE_CHUNGUS_BE.get(), MachineChungusBlockEntity::tick);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return structureHelper.generateShapeFromParts(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return structureHelper.getSpecificCollisionShape(structureHelper.getControllerOffset(), state.getValue(FACING));
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        if (structureHelper.isFullBlock(structureHelper.getControllerOffset(), state.getValue(FACING))) {
            return Shapes.block();
        }
        return Shapes.empty();
    }

    // --- IMultiblockController ---

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return structureHelper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        return structureHelper.resolvePartRole(localOffset, this);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineChungusBlock> CODEC = simpleCodec(MachineChungusBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
