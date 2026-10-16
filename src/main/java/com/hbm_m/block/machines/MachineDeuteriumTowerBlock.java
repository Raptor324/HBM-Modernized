package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineDeuteriumTowerBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockSideTuples;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

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
//?}
import dev.architectury.registry.menu.MenuRegistry;

public class MachineDeuteriumTowerBlock extends BaseEntityBlock implements IMultiblockController, com.hbm_m.interfaces.ILookOverlay {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private final MultiblockStructureHelper structureHelper;

    public MachineDeuteriumTowerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructureNew();
    }

    private static MultiblockStructureHelper defineStructureNew() {
        // GIT DeuteriumTower: 2×2×10 + base-corner fluid/power proxies
        String[] layer0 = {
            "BB",
            "BC",
            "OO"
        };
        String[] layerShaft = {
            "OO",
            "OO"
        };

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
            new String[][] {
                layer0, layerShaft, layerShaft, layerShaft, layerShaft, layerShaft,
                layerShaft, layerShaft, layerShaft, layerShaft
            },
            symbolMap,
            () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            roleMap,
            null,
            energySideMap,
            fluidSideMap
        );
    }

    /**
     * Порт {@code DeuteriumTower.printHook}: запас энергии (красный, если меньше maxPower/20),
     * затем вода (→) и тяжёлая вода (←) в формате "имя: fill/max mB".
     */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineDeuteriumTowerBlockEntity tower)) return;

        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();
        net.minecraft.ChatFormatting powerColor = tower.getEnergyStored() < tower.getMaxEnergyStored() / 20
                ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.GREEN;
        text.add(net.minecraft.network.chat.Component.literal("Power: " + shortNumber(tower.getEnergyStored()) + "HE")
                .withStyle(powerColor));

        com.hbm_m.inventory.fluid.tank.FluidTank[] tanks = tower.getAllTanks();
        for (int i = 0; i < tanks.length; i++) {
            String body = com.hbm_m.inventory.fluid.FluidType.forFluid(tanks[i].getTankType()).getLocalizedName().getString()
                    + ": " + tanks[i].getFill() + "/" + tanks[i].getMaxFill() + "mB";
            text.add(net.minecraft.network.chat.Component.literal(i < 1 ? "-> " : "<- ")
                    .withStyle(i < 1 ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED)
                    .append(net.minecraft.network.chat.Component.literal(body).withStyle(net.minecraft.ChatFormatting.WHITE)));
        }

        com.hbm_m.interfaces.ILookOverlay.printGeneric(guiGraphics,
                net.minecraft.network.chat.Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    /** Порт {@code BobMathUtil.getShortNumber} (как в оригинале: k/M/G/T/P/E с двумя знаками). */
    private static String shortNumber(long value) {
        double res;
        String suffix = "";
        long abs = Math.abs(value);
        if (abs >= 1_000_000_000_000_000_000L) { res = value / 1.0E18; suffix = "E"; }
        else if (abs >= 1_000_000_000_000_000L) { res = value / 1.0E15; suffix = "P"; }
        else if (abs >= 1_000_000_000_000L) { res = value / 1.0E12; suffix = "T"; }
        else if (abs >= 1_000_000_000L) { res = value / 1.0E9; suffix = "G"; }
        else if (abs >= 1_000_000L) { res = value / 1.0E6; suffix = "M"; }
        else if (abs >= 1_000L) { res = value / 1.0E3; suffix = "k"; }
        else { return Long.toString(value); }
        res = res <= -100.0 ? Math.round(res * 10.0) / 10.0 : Math.round(res * 100.0) / 100.0;
        return res + suffix;
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
        return new MachineDeuteriumTowerBlockEntity(pos, state);
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
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider menu) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, menu, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.DEUTERIUM_TOWER_BE.get(), MachineDeuteriumTowerBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineDeuteriumTowerBlock> CODEC = simpleCodec(MachineDeuteriumTowerBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
