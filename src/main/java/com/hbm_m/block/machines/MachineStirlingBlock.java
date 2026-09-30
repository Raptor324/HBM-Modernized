package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineStirlingBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Stirling Engine - eine Blockklasse fuer alle 3 Varianten (regulaer/Stahl/kreativ), siehe
 * Klassenkommentar in {@link MachineStirlingBlockEntity}. Kein GUI - Rechtsklick mit einem
 * Grosszahnrad repariert die Maschine nach einer Overspeed-Explosion, sonst passiert nichts.
 */
public class MachineStirlingBlock extends BaseEntityBlock implements com.hbm_m.interfaces.ILookOverlay {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public MachineStirlingBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineStirlingBlockEntity(pos, state);
    }

    /** Порт MachineStirling.printHook: тепло, отдача HE/t, процент и перегрев. */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineStirlingBlockEntity stirling)) return;

        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();
        text.add(net.minecraft.network.chat.Component.literal(stirling.getHeat() + "TU/t"));
        text.add(net.minecraft.network.chat.Component.literal(
                (stirling.hasCog() ? stirling.getEnergyStored() : 0) + "HE/t"));

        if (!stirling.isCreative()) {
            int maxHeat = stirling.getMaxHeat();
            double percent = (double) stirling.getHeat() / (double) maxHeat;
            int color = ((int) (0xFF - 0xFF * percent)) << 16 | ((int) (0xFF * percent) << 8);

            if (percent > 1D)
                color = 0xff0000;

            final int heatColor = color;
            text.add(net.minecraft.network.chat.Component.literal(((stirling.getHeat() * 1000 / maxHeat) / 10D) + "%")
                    .withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(heatColor))));

            if (stirling.getHeat() > maxHeat) {
                boolean blink = System.currentTimeMillis() % 1000 < 500;
                text.add(net.minecraft.network.chat.Component.literal("! ! ! OVERSPEED ! ! !")
                        .withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(blink ? 0xff0000 : 0xffff00))));
            }

            if (!stirling.hasCog()) {
                text.add(net.minecraft.network.chat.Component.literal("Gear missing!")
                        .withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(0xff0000))));
            }
        }

        com.hbm_m.interfaces.ILookOverlay.printGeneric(guiGraphics,
                net.minecraft.network.chat.Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof MachineStirlingBlockEntity stirling) {
            ItemStack held = player.getItemInHand(hand);
            if (stirling.tryRepair(player, held)) {
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
        }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof MachineStirlingBlockEntity stirling) {
            ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (stirling.tryRepair(player, held)) {
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
        }
    *///?}


    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.STIRLING_BE.get(), MachineStirlingBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineStirlingBlock> CODEC = simpleCodec(MachineStirlingBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
