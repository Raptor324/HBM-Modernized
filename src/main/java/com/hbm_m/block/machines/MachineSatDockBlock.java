package com.hbm_m.block.machines;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineSatDockBlockEntity;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code com.hbm.blocks.machine.MachineSatDock} ({@code sat_dock}, Frachtschiff-Andockstation): 3x3 Felder,
 * Kern 12/16 hoch ({@code setBlockBounds(0, 0, 0, 1, 12*f, 1)}), die acht {@code dummy_plate_cargo} ringsum
 * 8/16 hoch ({@code setBounds(0, 0, 0, 16, 8, 16)}), Modell {@code sat_dock.obj}.
 */
public class MachineSatDockBlock extends DummyableMachineBlock {

    private static final VoxelShape CORE = Block.box(0, 0, 0, 16, 12, 16);
    /** audit13: Original {@code dummy_plate_cargo} ist nur 8/16 hoch, nicht 12/16 wie der Kern. */
    private static final VoxelShape PLATE = Block.box(0, 0, 0, 16, 8, 16);

    public MachineSatDockBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        Map<BlockPos, Supplier<BlockState>> structure = new LinkedHashMap<>();
        Map<BlockPos, Character> symbols = new LinkedHashMap<>();
        Map<BlockPos, VoxelShape> shapes = new LinkedHashMap<>();
        Supplier<BlockState> part = () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState();

        for (int k = -1; k <= 1; k++) {
            for (int l = -1; l <= 1; l++) {
                BlockPos pos = new BlockPos(k, 0, l);
                shapes.put(pos, k == 0 && l == 0 ? CORE : PLATE);
                if (k == 0 && l == 0) {
                    symbols.put(pos, 'C');
                    continue;
                }
                structure.put(pos, part);
                symbols.put(pos, 'O');
            }
        }

        Map<Character, PartRole> roles = new LinkedHashMap<>();
        roles.put('O', PartRole.DEFAULT);
        roles.put('C', PartRole.CONTROLLER);

        return new MultiblockStructureHelper(structure, part, roles, symbols, shapes, shapes, BlockPos.ZERO).withPlacementOffset(0);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineSatDockBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.SAT_DOCK_BE.get(), MachineSatDockBlockEntity::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    //?} else {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack hbmHeld, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return com.hbm_m.platform.BlockUseHooks.item(hbmUse(state, world, pos, player, hand, hit));
    }
    private InteractionResult hbmUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    *///?}
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        } else if (!player.isShiftKeyDown()) {
            if (world.getBlockEntity(pos) instanceof MachineSatDockBlockEntity dock && player instanceof ServerPlayer sp) {
                MenuRegistry.openExtendedMenu(sp, dock, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineSatDockBlock> CODEC = simpleCodec(MachineSatDockBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
