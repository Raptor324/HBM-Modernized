package com.hbm_m.block.decorations;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.SoyuzLauncherBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.hbm_m.platform.BlockHooks;

public class SoyuzLauncherBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<PartRole> PART_ROLE = EnumProperty.create("part_role", PartRole.class);

    /** Original: Kern vier Bloecke ueber dem Setzpunkt ({@code struct_soyuz_core} platziert den Kern hierueber). */
    public static final int HEIGHT = 4;

    private final MultiblockStructureHelper structureHelper;

    public SoyuzLauncherBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART_ROLE, PartRole.CONTROLLER));

        com.hbm_m.multiblock.DummyableStructureBuilder b = com.hbm_m.multiblock.DummyableStructureBuilder.create()
                .box(0, 1, 6, 6, 6, 6)
                .box(-2, 4, -3, 6, -3, 6)
                .box(-2, 4, 6, -3, -3, 6)
                .box(-2, 4, 6, -3, 6, -3)
                .box(-2, 4, -3, 6, 6, -3)
                .box(0, 4, 1, 1, -6, 8)
                .box(0, 4, 2, 2, 9, -5)
                .placementOffset(0);

        this.structureHelper = b.build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART_ROLE);
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return this.structureHelper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        if (localOffset.equals(BlockPos.ZERO)) return PartRole.CONTROLLER;
        if ((localOffset.getY() == 0 || localOffset.getY() == -1)
                && (Math.abs(localOffset.getX()) == 6 || Math.abs(localOffset.getZ()) == 6)) {
            return PartRole.UNIVERSAL_CONNECTOR;
        }
        return PartRole.DEFAULT;
    }

    /** Original {@code getDirModified}: die Struktur ist immer EAST, nie Spielerrichtung (SoyuzLauncher.java:81-83). */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, Direction.EAST)
                .setValue(PART_ROLE, PartRole.CONTROLLER);
    }

    /** Original {@code getHeightOffset} = {@code height = 4}: der Kern liegt vier Bloecke ueber dem Klickpunkt. */
    @Override
    public int getHeightOffset() {
        return HEIGHT;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART_ROLE) == PartRole.CONTROLLER
                ? new SoyuzLauncherBlockEntity(pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return createTickerHelper(type, ModBlockEntities.SOYUZ_LAUNCHER_BE.get(), SoyuzLauncherBlockEntity::clientTick);
        return createTickerHelper(type, ModBlockEntities.SOYUZ_LAUNCHER_BE.get(), SoyuzLauncherBlockEntity::serverTick);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            structureHelper.placeStructure(level, pos, state.getValue(FACING), this);
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(PART_ROLE) == PartRole.CONTROLLER ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return hbmUse(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
    private InteractionResult hbmUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    *///?}
        if (player.isShiftKeyDown()) return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SoyuzLauncherBlockEntity be) {
            com.hbm_m.handler.BossSpawnHandler.markFBI((ServerPlayer) player);
            MenuRegistry.openExtendedMenu((ServerPlayer) player, be, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static final ThreadLocal<Boolean> SUPPRESS_CREATIVE_DROPS = ThreadLocal.withInitial(() -> false);

    //? if < 1.21.1 {
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.getAbilities().instabuild) {
            SUPPRESS_CREATIVE_DROPS.set(true);
        }
        super.playerWillDestroy(level, pos, state, player);
    }
    //?} else {
    /*@Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.getAbilities().instabuild) {
            SUPPRESS_CREATIVE_DROPS.set(true);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
    *///?}

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        try {
            if (!state.is(newState.getBlock()) && !level.isClientSide()) {
                structureHelper.destroyStructure(level, pos, state.getValue(FACING));
                if (!SUPPRESS_CREATIVE_DROPS.get()) {
                    double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
                    for (int l = 0; l < 6; l++) Containers.dropItemStack(level, x, y, z, new ItemStack(ModBlocks.STRUCT_LAUNCHER.get(), 64));
                    for (int l = 0; l < 4; l++) Containers.dropItemStack(level, x, y, z, new ItemStack(ModBlocks.CONCRETE_SMOOTH.get(), 64));
                    for (int l = 0; l < 6; l++) Containers.dropItemStack(level, x, y, z, new ItemStack(ModBlocks.STRUCT_SCAFFOLD.get(), 64));
                    Containers.dropItemStack(level, x, y, z, new ItemStack(ModBlocks.STRUCT_LAUNCHER.get(), 30));
                    Containers.dropItemStack(level, x, y, z, new ItemStack(ModBlocks.STRUCT_SCAFFOLD.get(), 63));
                    Containers.dropItemStack(level, x, y, z, new ItemStack(ModBlocks.CONCRETE_SMOOTH.get(), 38));
                    Containers.dropItemStack(level, x, y, z, new ItemStack(ModBlocks.STRUCT_SOYUZ_CORE.get(), 1));
                }
                com.hbm_m.block.MachineDrops.dropInventory(level, pos);
            }
        } finally {
            SUPPRESS_CREATIVE_DROPS.remove();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<SoyuzLauncherBlock> CODEC = simpleCodec(SoyuzLauncherBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
