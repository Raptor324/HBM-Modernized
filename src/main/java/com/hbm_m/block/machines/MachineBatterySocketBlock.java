package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.BatterySocketBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;
import com.hbm_m.util.EnergyFormatter;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
//? if forge {
//?}
//? if forge {

//?}
/**
 * 2×2×2 multiblock cube.
 */
public class MachineBatterySocketBlock extends BaseEntityBlock implements IMultiblockController, com.hbm_m.interfaces.ILookOverlay {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static MultiblockStructureHelper STRUCTURE_HELPER;
    private final MultiblockStructureHelper structureHelper;

    public MachineBatterySocketBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = getStructureHelperStatic();
    }

    public static MultiblockStructureHelper getStructureHelperStatic() {
        if (STRUCTURE_HELPER == null) {
            STRUCTURE_HELPER = defineStructure();
        }
        return STRUCTURE_HELPER;
    }

    private static MultiblockStructureHelper defineStructure() {
        // 2x2x2: контроллер в нижнем слое, остальные — части.
        String[][] layers = {
            { // Y = 0
                "CE",
                "EE"
            },
            { // Y = 1
                "AA",
                "AA"
            }
        };

        Map<Character, PartRole> roleMap = Map.of(
            'C', PartRole.CONTROLLER,
            'E', PartRole.ENERGY_CONNECTOR,
            'A', PartRole.DEFAULT
        );

        Map<Character, Supplier<BlockState>> symbolMap = Map.of(
            'A', () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState()
        );

        return MultiblockStructureHelper.createFromLayersWithRoles(
            layers,
            symbolMap,
            () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            roleMap
        );
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return structureHelper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        if (structureHelper != null) {
            return structureHelper.resolvePartRole(localOffset, this);
        }
        return PartRole.DEFAULT;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            placeMultiblockStructure(level, pos, state);
        }
    }


    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos) && canSurviveMultiblockPlacement(state, level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            MultiblockStructureHelper helper = getStructureHelper();
            Direction facing = state.getValue(FACING);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof com.hbm_m.blockentity.BaseMachineBlockEntity machine) {
                machine.dropInventoryContents();
            }
            helper.destroyStructure(level, pos, facing);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return openMenu(state, level, pos, player, hand, hit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return openMenu(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
    *///?}

    private InteractionResult openMenu(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, provider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BatterySocketBlockEntity socket) {
            return socket.getComparatorOutput();
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BatterySocketBlockEntity(pos, state);
    }

    /**
     * Порт MachineBatterySocket.printHook: заряд вставленной батареи.
     * Логика передачи энергии в порту ещё не доделана, но HUD показывает
     * фактическое содержимое слота (ориг. socket.syncStack/syncPower/syncMaxPower).
     */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BatterySocketBlockEntity socket)) return;

        net.minecraft.world.item.ItemStack stack = socket.getBatteryStack();
        if (stack.isEmpty()) return;

        long power = socket.getEnergyStored();
        long max = socket.getMaxEnergyStored();

        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();
        text.add(net.minecraft.network.chat.Component.literal(
                EnergyFormatter.format(power) + " / " + EnergyFormatter.format(max) + " HE"));

        double percent = (double) power / (double) max;
        int charge = (int) Math.floor(percent * 10_000D);
        int color = ((int) (0xFF - 0xFF * percent)) << 16 | ((int) (0xFF * percent) << 8);
        text.add(net.minecraft.network.chat.Component.literal((charge / 100D) + "%")
                .withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(color))));

        com.hbm_m.interfaces.ILookOverlay.printGeneric(guiGraphics,
                stack.getHoverName(), 0xffff00, 0x404000, text);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.BATTERY_SOCKET_BE.get(), BatterySocketBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineBatterySocketBlock> CODEC = simpleCodec(MachineBatterySocketBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
