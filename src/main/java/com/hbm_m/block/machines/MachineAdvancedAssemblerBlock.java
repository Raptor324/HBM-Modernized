package com.hbm_m.block.machines;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.interfaces.IFrameSupportable;
import com.hbm_m.multiblock.MultiblockSideTuples;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code MachineAssemblyMachine} ({@code BlockDummyable}, {@code getDimensions {2,0,1,1,1,1}}, {@code getOffset 1}):
 * 3x3x3 mit dem Kern unten in der Mitte; die acht Randzellen des Sockels sind Anschluesse.
 * {@link MachinePrecAssBlock} erbt - wie im Original - diesen Block und tauscht nur die BlockEntity.
 */
public class MachineAdvancedAssemblerBlock extends DummyableMachineBlock {

    /** Rahmen sichtbar, wenn ueber der Maschine Bloecke stehen. Im BlockState, damit Embeddium/Sodium ihn mitbackt. */
    public static final BooleanProperty FRAME = BooleanProperty.create("frame");

    public MachineAdvancedAssemblerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockState withDefaults(BlockState state) {
        return state.setValue(FRAME, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FRAME);
    }

    @Override
    protected void onStructurePlaced(Level level, BlockPos core) {
        if (level.getBlockEntity(core) instanceof IFrameSupportable be) {
            be.checkForFrame();
        }
    }

    /**
     * Struktur per ASCII-Schichten (unten nach oben).
     * 'C' = Kern (genau einer), 'B' = Anschluss (UNIVERSAL_CONNECTOR), 'A' = gewoehnliche Zelle.
     */
    @Override
    protected MultiblockStructureHelper defineStructure() {
        String[] layer0 = {
            "BBB",
            "BCB",
            "BBB"
        };
        String[] layer = {
            "AAA",
            "AAA",
            "AAA"
        };

        Map<Character, PartRole> roleMap = Map.of(
            'A', PartRole.DEFAULT,
            'B', PartRole.UNIVERSAL_CONNECTOR,
            'C', PartRole.CONTROLLER
        );

        Map<Character, boolean[]> fluidSideMap = Map.of(
            'C', MultiblockSideTuples.fluid(true, true, true, true, true, false),
            'B', MultiblockSideTuples.fluid(true, true, true, true, true, false)
        );
        Map<Character, boolean[]> energySideMap = Map.of(
            'C', MultiblockSideTuples.energy(true, true, true, true, true, false),
            'B', MultiblockSideTuples.energy(true, true, true, true, true, false)
        );

        return MultiblockStructureHelper.createFromLayersWithRolesAndSides(
            new String[][]{layer0, layer, layer},
            null,
            () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            roleMap,
            null,
            energySideMap,
            fluidSideMap
        );
    }

    @Override
    public int[][] getAllPorts(Direction facing) {
        return rotatePorts(new int[][] {
            { -1, 0, -1, Direction.WEST.get3DDataValue() },
            { -1, 0, -1, Direction.NORTH.get3DDataValue() },
            { -1, 0,  0, Direction.WEST.get3DDataValue() },
            { -1, 0,  1, Direction.WEST.get3DDataValue() },
            { -1, 0,  1, Direction.SOUTH.get3DDataValue() },
            {  0, 0, -1, Direction.NORTH.get3DDataValue() },
            {  0, 0,  1, Direction.SOUTH.get3DDataValue() },
            {  1, 0, -1, Direction.EAST.get3DDataValue() },
            {  1, 0, -1, Direction.NORTH.get3DDataValue() },
            {  1, 0,  0, Direction.EAST.get3DDataValue() },
            {  1, 0,  1, Direction.EAST.get3DDataValue() },
            {  1, 0,  1, Direction.SOUTH.get3DDataValue() }
        }, facing);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineAdvancedAssemblerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE_BE.get(), MachineAdvancedAssemblerBlockEntity::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return open(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return open(level, pos, player);
    }
    *///?}

    /** Original {@code standardOpenBehavior}: geschlichen -> true ohne GUI. */
    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && !player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, provider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineAdvancedAssemblerBlock> CODEC = simpleCodec(MachineAdvancedAssemblerBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
