package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineCatalyticReformerBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockSideTuples;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;
import dev.architectury.registry.menu.MenuRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
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
//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
//?}

public class MachineCatalyticReformerBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private final MultiblockStructureHelper structureHelper;

    public MachineCatalyticReformerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructureNew();
    }

    private static MultiblockStructureHelper defineStructureNew() {
        Map<BlockPos, Character> cells = new HashMap<>();
        addLegacyBox(cells, new int[] { 2, 0, 1, 1, 2, 2 }, BlockPos.ZERO, 'O');
        addLegacyBox(cells, new int[] { 3, -3, 1, 0, -1, 2 }, BlockPos.ZERO, 'O');
        addLegacyBox(cells, new int[] { 6, -3, 1, 1, 2, 0 }, BlockPos.ZERO, 'O');
        for (BlockPos proxy : List.of(
                new BlockPos(1, 0, 1), new BlockPos(1, 0, -1),
                new BlockPos(-1, 0, 1), new BlockPos(-1, 0, -1),
                new BlockPos(0, 0, 2), new BlockPos(0, 0, -2))) {
            cells.put(proxy, 'B');
        }
        cells.put(BlockPos.ZERO, 'C');

        Map<Character, PartRole> roleMap = Map.of(
            'C', PartRole.CONTROLLER,
            'O', PartRole.DEFAULT,
            'B', PartRole.UNIVERSAL_CONNECTOR
        );
        Map<Character, Supplier<BlockState>> symbolMap = Map.of();
        Map<Character, boolean[]> energySideMap = Map.of(
            'B', MultiblockSideTuples.energy(true, true, true, true, true, false),
            'C', MultiblockSideTuples.energy(true, true, true, true, true, false)
        );
        Map<Character, boolean[]> fluidSideMap = Map.of(
            'B', MultiblockSideTuples.fluid(true, true, true, true, true, false),
            'C', MultiblockSideTuples.fluid(true, true, true, true, true, false)
        );

        return MultiblockStructureHelper.createFromLayersWithRolesAndSides(
            cellsToLayers(cells),
            symbolMap,
            () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            roleMap,
            null,
            energySideMap,
            fluidSideMap
        );
    }

    private static int[] rotateLegacyXrNorth(int[] dim) {
        return new int[] { dim[0], dim[1], dim[3], dim[2], dim[5], dim[4] };
    }

    private static void addLegacyBox(Map<BlockPos, Character> cells, int[] dim, BlockPos origin, char symbol) {
        int[] r = rotateLegacyXrNorth(dim);
        int posX = r[5], negX = r[4], posY = r[0], negY = r[1], posZ = r[3], negZ = r[2];
        int yMin = -negY;
        int yMax = posY;
        for (int x = -negX; x <= posX; x++) {
            for (int y = yMin; y <= yMax; y++) {
                for (int z = -negZ; z <= posZ; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!pos.equals(BlockPos.ZERO)) {
                        cells.put(pos, symbol);
                    }
                }
            }
        }
    }

    private static String[][] cellsToLayers(Map<BlockPos, Character> cells) {
        int minY = cells.keySet().stream().mapToInt(BlockPos::getY).min().orElse(0);
        Map<BlockPos, Character> shifted = new HashMap<>();
        for (Map.Entry<BlockPos, Character> entry : cells.entrySet()) {
            shifted.put(entry.getKey().offset(0, -minY, 0), entry.getValue());
        }
        int maxY = shifted.keySet().stream().mapToInt(BlockPos::getY).max().orElse(0);
        List<String[]> layers = new ArrayList<>();
        for (int y = 0; y <= maxY; y++) {
            final int layerY = y;
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
            for (BlockPos pos : shifted.keySet()) {
                if (pos.getY() != layerY) {
                    continue;
                }
                minX = Math.min(minX, pos.getX());
                maxX = Math.max(maxX, pos.getX());
                minZ = Math.min(minZ, pos.getZ());
                maxZ = Math.max(maxZ, pos.getZ());
            }
            String[] rows = new String[maxZ - minZ + 1];
            for (int z = minZ; z <= maxZ; z++) {
                StringBuilder row = new StringBuilder();
                for (int x = minX; x <= maxX; x++) {
                    Character symbol = shifted.get(new BlockPos(x, layerY, z));
                    row.append(symbol != null ? symbol : '.');
                }
                rows[z - minZ] = row.toString();
            }
            layers.add(rows);
        }
        return layers.toArray(new String[0][]);
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return structureHelper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        return structureHelper.resolvePartRole(localOffset, this);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            BlockPos core = placeMultiblockStructure(level, pos, state);
            if (core == null) {
                return;
            }
        }
    }


    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos) && canSurviveMultiblockPlacement(state, level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            structureHelper.destroyStructure(level, pos, state.getValue(FACING));
            if (level.getBlockEntity(pos) instanceof com.hbm_m.blockentity.BaseMachineBlockEntity machine) {
                machine.dropInventoryContents();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return structureHelper.generateShapeFromParts(state.getValue(FACING));
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineCatalyticReformerBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider menu) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, menu, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.CATALYTIC_REFORMER_BE.get(), MachineCatalyticReformerBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineCatalyticReformerBlock> CODEC = simpleCodec(MachineCatalyticReformerBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
