package com.hbm_m.block.machines.pile;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.pile.PileBaseBlockEntity;
import com.hbm_m.blockentity.machines.pile.PileCoreBlockEntity;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Gemeinsame Grundlage der drei Meilergeraete - Ladevorrichtung, Geblaese und Steuerstabantrieb.
 *
 * <p><b>Abweichung vom Original:</b> dort sind alle drei ein einziger Block
 * ({@code BlockPileDevice}) mit zwoelf Metadaten, weil 1.7.10 nur diese hatte - vier Richtungen mal
 * drei Bauarten. Dieser Port macht daraus drei eigene Bloecke mit einer Blickrichtung, was auf 1.20
 * dasselbe leistet und den Zustandsraum sauber haelt.</p>
 *
 * <p>{@link HorizontalDirectionalBlock#FACING} zeigt dabei stets <b>vom Meiler weg</b>: der
 * zugehoerige Kanalblock liegt also entgegen der Blickrichtung, und Redstone wird davor
 * abgegriffen.</p>
 */
public abstract class PileDeviceBlock extends BaseEntityBlock {

    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
            HorizontalDirectionalBlock.FACING;

    protected PileDeviceBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Original: {@code onBlockPlaced} nimmt fuer Ladevorrichtung und Geblaese die angeklickte Seite,
     * {@code onBlockPlacedBy} fuer den Steuerstabantrieb die Blickrichtung des Spielers. Beides
     * laeuft hier auf dieselbe Regel hinaus, weil man den Antrieb ohnehin von oben setzt.
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face.getAxis() == Direction.Axis.Y) {
            face = context.getHorizontalDirection().getOpposite();
        }
        return defaultBlockState().setValue(FACING, face);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /**
     * 1:1-Port von {@code BlockPileDevice.onScrew}: der Handbohrer auf einem Geraet wirkt auf den
     * Kanalblock dahinter - man muss den Meiler zum Umbauen also nicht erst freiraeumen.
     */
    protected InteractionResult passDrillThrough(BlockState state, Level level, BlockPos pos,
                                                 Player player, InteractionHand hand, boolean vertical) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() != ModItems.HAND_DRILL.get() && held.getItem() != ModItems.HAND_DRILL_DESH.get()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        Direction facing = state.getValue(FACING);
        BlockPos target = vertical ? pos.below() : pos.relative(facing.getOpposite());
        // Der Bohrer trifft den Kanal von aussen, also aus der Richtung des Geraets.
        Direction drillDir = vertical ? Direction.DOWN : facing.getOpposite();

        if (!level.getBlockState(target).is(ModBlocks.PILE_BLOCK.get())) return InteractionResult.CONSUME;

        BlockEntity tile = level.getBlockEntity(target);
        if (tile instanceof PileBaseBlockEntity pile) {
            PileCoreBlockEntity core = pile.getCore(level);
            if (core != null) core.drillChannel(level, target, drillDir, player);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
    }

    protected abstract InteractionResult interact(BlockState state, Level level, BlockPos pos,
                                                  Player player, InteractionHand hand, BlockHitResult hit);

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        return interact(state, level, pos, player, hand, hit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        return interact(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
    *///?}
}
