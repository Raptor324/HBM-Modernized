package com.hbm_m.block.machines;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.PWRBlockEntity;
import com.hbm_m.blockentity.machines.PWRControllerBlockEntity;
import com.hbm_m.item.ModItems;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code MachinePWRController}: ein Klick auf den nicht zusammengebauten Controller flutet den Aufbau hinter seiner
 * Vorderseite ab ({@code assemble}/{@code floodFill}); gelingt es, wird jedes Bauteil zum Traeger {@code pwr_block}
 * (Anschluesse als {@link PWRBlock#PORT}). Zusammengebaut oeffnet der Klick die GUI, ausser man haelt den PWR-Drucker.
 */
public class MachinePWRControllerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final int MAX_SIZE = 4096;

    public MachinePWRControllerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
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

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PWRControllerBlockEntity(pos, state); }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PWR_CONTROLLER_BE.get(), PWRControllerBlockEntity::tick);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return activate(state, level, pos, player, player.getItemInHand(hand));
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return activate(state, level, pos, player, player.getMainHandItem());
    }
    *///?}

    private InteractionResult activate(BlockState state, Level level, BlockPos pos, Player player, ItemStack held) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS; // Original: Client true, Server geschlichen false
        if (!(level.getBlockEntity(pos) instanceof PWRControllerBlockEntity controller)) return InteractionResult.PASS;

        if (!controller.assembled) {
            assemble(level, pos, state.getValue(FACING), player);
        } else {
            if (!held.isEmpty() && held.getItem() == ModItems.PWR_PRINTER.get()) return InteractionResult.PASS;
            MenuRegistry.openExtendedMenu((ServerPlayer) player, controller, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    private static final Map<BlockPos, Block> assembly = new HashMap<>();
    private static final Map<BlockPos, Block> fuelRods = new HashMap<>();
    private static final Map<BlockPos, Block> sources = new HashMap<>();
    private static boolean errored;

    public void assemble(Level world, BlockPos pos, Direction facing, @Nullable Player player) {
        assembly.clear();
        fuelRods.clear();
        sources.clear();
        assembly.put(pos, this);

        Direction dir = facing.getOpposite();

        errored = false;
        floodFill(world, pos.relative(dir), player);

        if (fuelRods.isEmpty()) {
            sendError(world, pos, "Fuel rods required", player);
            errored = true;
        }

        if (sources.isEmpty()) {
            sendError(world, pos, "Neutron sources required", player);
            errored = true;
        }

        if (!(world.getBlockEntity(pos) instanceof PWRControllerBlockEntity controller)) return;

        if (!errored) {
            for (Map.Entry<BlockPos, Block> entry : assembly.entrySet()) {
                BlockPos p = entry.getKey();
                Block block = entry.getValue();

                if (block != ModBlocks.PWR_CONTROLLER.get()) {
                    world.setBlock(p, ModBlocks.PWR_BLOCK.get().defaultBlockState().setValue(PWRBlock.PORT, block == ModBlocks.PWR_PORT.get()), 3);
                    if (world.getBlockEntity(p) instanceof PWRBlockEntity pwr) pwr.setup(block, pos);
                }
            }

            controller.setup(assembly, fuelRods);
        }
        controller.assembled = !errored;
        controller.setChanged();

        assembly.clear();
        fuelRods.clear();
        sources.clear();
    }

    private void floodFill(Level world, BlockPos pos, @Nullable Player player) {

        if (assembly.containsKey(pos)) return;
        if (assembly.size() >= MAX_SIZE) {
            errored = true;
            sendError(world, pos, "Max size exceeded", player);
            return;
        }

        Block block = world.getBlockState(pos).getBlock();

        if (isValidCasing(block)) {
            assembly.put(pos, block);
            return;
        }

        if (isValidCore(block)) {
            assembly.put(pos, block);
            if (block == ModBlocks.PWR_FUEL.get()) fuelRods.put(pos, block);
            if (block == ModBlocks.PWR_NEUTRON_SOURCE.get()) sources.put(pos, block);
            floodFill(world, pos.offset(1, 0, 0), player);
            floodFill(world, pos.offset(-1, 0, 0), player);
            floodFill(world, pos.offset(0, 1, 0), player);
            floodFill(world, pos.offset(0, -1, 0), player);
            floodFill(world, pos.offset(0, 0, 1), player);
            floodFill(world, pos.offset(0, 0, -1), player);
            return;
        }

        sendError(world, pos, "Non-reactor block", player);
        errored = true;
    }

    /** Original: {@code AuxParticlePacketNT} Typ {@code marker}, rot, 5 s, 128 Bloecke. */
    public static void sendError(Level world, BlockPos pos, String message, @Nullable Player player) {
        if (player instanceof ServerPlayer mp) {
            CompoundTag data = new CompoundTag();
            data.putString("type", "marker");
            data.putInt("color", 0xff0000);
            data.putInt("expires", 5_000);
            data.putDouble("dist", 128D);
            if (message != null) data.putString("label", message);
            com.hbm_m.network.AuxParticlePacket.sendTo(mp, data, pos.getX(), pos.getY(), pos.getZ());
        }
    }

    private static boolean isValidCore(Block block) {
        return block instanceof PWRPart p && !p.getKind().isCasing();
    }

    private static boolean isValidCasing(Block block) {
        return block instanceof PWRPart p && p.getKind().isCasing();
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachinePWRControllerBlock> CODEC = simpleCodec(MachinePWRControllerBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
