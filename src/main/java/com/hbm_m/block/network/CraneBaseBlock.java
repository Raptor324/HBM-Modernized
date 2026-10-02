package com.hbm_m.block.network;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.block.IToolable;
import com.hbm_m.blockentity.network.CraneBaseBlockEntity;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code BlockCraneBase}: gemeinsame Basis der Foerderband-Krane. {@link #FACING} ist die Eingangsseite (Original:
 * Metadaten), {@link #OUTPUT} die wirksame Ausgangsseite (Original: {@code outputOverride} der BlockEntity bzw. die
 * Gegenseite des Eingangs) - als Blockzustand gefuehrt, damit das Modell die Richtungstexturen des Originals zeigen kann.
 * Schraubendreher: Klick setzt den Eingang auf die angeklickte Seite, mit Schleichen den Ausgang.
 */
public abstract class CraneBaseBlock extends BaseEntityBlock implements IToolable {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final DirectionProperty OUTPUT = DirectionProperty.create("output");

    protected CraneBaseBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OUTPUT, Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OUTPUT);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    /** Original: {@code BlockPistonBase.determineOrientation}. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction dir = determineOrientation(ctx.getClickedPos(), ctx.getPlayer());
        return defaultBlockState().setValue(FACING, dir).setValue(OUTPUT, dir.getOpposite());
    }

    public static Direction determineOrientation(BlockPos pos, @Nullable Player player) {
        if (player == null) return Direction.NORTH;
        if (Mth.abs((float) player.getX() - pos.getX()) < 2.0F && Mth.abs((float) player.getZ() - pos.getZ()) < 2.0F) {
            // Original: posY (Augenhoehe) + 1.82 - yOffset (1.62) = Fusshoehe + 1.82
            double d0 = player.getY() + 1.82D;
            if (d0 - pos.getY() > 2.0D) return Direction.UP;
            if (pos.getY() - d0 > 0.0D) return Direction.DOWN;
        }
        int l = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        return l == 0 ? Direction.NORTH : (l == 1 ? Direction.EAST : (l == 2 ? Direction.SOUTH : Direction.WEST));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return activate(level, pos, player, player.getItemInHand(hand));
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return activate(level, pos, player, player.getMainHandItem());
    }
    *///?}

    /** Original: {@code onBlockActivated} - Werkzeuge und Bandstab haben Vorrang, sonst GUI ohne Schleichen. */
    protected InteractionResult activate(Level level, BlockPos pos, Player player, ItemStack held) {
        if (!held.isEmpty() && held.getItem() instanceof com.hbm_m.item.tool.ItemTooling) return InteractionResult.PASS;
        if (!held.isEmpty() && held.getItem() instanceof com.hbm_m.item.tool.ItemConveyorWand) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!player.isShiftKeyDown()) {
            if (level.getBlockEntity(pos) instanceof MenuProvider menu) {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, menu, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (!(world.getBlockEntity(pos) instanceof CraneBaseBlockEntity crane)) return false;
        if (world.isClientSide) return true;

        if (player.isShiftKeyDown()) {
            crane.setOutputOverride(side);
        } else {
            crane.setInput(side);
        }
        return true;
    }

    public Direction getInputSide(BlockGetter world, BlockPos pos) {
        return world.getBlockState(pos).getValue(FACING);
    }

    /** Original: {@code getOutputSide} - {@code null} entspricht {@code ForgeDirection.UNKNOWN}. */
    @Nullable
    public Direction getOutputSide(BlockGetter world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof CraneBaseBlockEntity crane)) return null;
        return crane.getOutputSide();
    }

    /** Original: {@code dropContents(start, end)} in {@code breakBlock}; {@code null} = nichts. */
    @Nullable
    protected int[] getDropRange() { return null; }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        int[] range = getDropRange();
        if (range != null && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CraneBaseBlockEntity crane) {
            for (int i = range[0]; i < range[1] && i < crane.getInventory().getSlots(); i++) {
                ItemStack stack = crane.getInventory().getStackInSlot(i);
                if (!stack.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
            }
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }
}
