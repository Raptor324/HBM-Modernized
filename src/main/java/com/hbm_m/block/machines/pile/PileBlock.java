package com.hbm_m.block.machines.pile;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.MachinePWRControllerBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.pile.PileBaseBlockEntity;
import com.hbm_m.blockentity.machines.pile.PileCoreBlockEntity;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code BlockPile} (1.7.10): der Baustoff des zusammengebauten Uranmeilers.
 *
 * <p>Jeder Block des Meilers ist dieser eine Block; welche Rolle er spielt, steht in
 * {@link #TYPE}. Genau einer davon ist der Kern und traegt die Rechnung, alle uebrigen merken sich
 * nur, wo dieser Kern steht. Wird einer von ihnen zerschlagen, faellt er in einen gewoehnlichen
 * Graphitziegel zurueck und reisst den Kern mit - der Meiler zerfaellt also als Ganzes.</p>
 *
 * <p>Der Handbohrer treibt Kanaele hinein, siehe
 * {@link PileCoreBlockEntity#drillChannel}. Auf einem vorhandenen Kanaleingang angesetzt schuettet
 * er ihn wieder zu.</p>
 *
 * <p>Die Oberflaeche setzt sich ueber Blockgrenzen hinweg zusammen: benachbarte Meilerbloecke
 * verschmelzen zu einer durchgehenden Ziegelwand, und die Kanaloeffnungen laufen sauber
 * ineinander. Deckel und Boden tragen dabei ein anderes Texturpaar als die Seiten, die Steuerung
 * einen eigenen Deckel und der Kern eine eigene Wand - alles wie im Original
 * ({@code BlockPile.getFragments}).</p>
 */
public class PileBlock extends BaseEntityBlock {

    public static final EnumProperty<PileBlockType> TYPE = EnumProperty.create("type", PileBlockType.class);

    public PileBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(TYPE, PileBlockType.DUMMY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(TYPE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // Original: {@code createNewTileEntity} entscheidet an der Metadatenzahl.
        if (state.getValue(TYPE) == PileBlockType.CORE) return new PileCoreBlockEntity(pos, state);
        return new PileBaseBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (state.getValue(TYPE) == PileBlockType.CORE) {
            return createTickerHelper(type, ModBlockEntities.PILE_CORE_BE.get(),
                    (lvl, pos, st, be) -> PileCoreBlockEntity.tick(lvl, pos, st, (PileCoreBlockEntity) be));
        }
        return createTickerHelper(type, ModBlockEntities.PILE_BASE_BE.get(),
                (lvl, pos, st, be) -> PileBaseBlockEntity.tick(lvl, pos, st, (PileBaseBlockEntity) be));
    }

    /** 1:1-Port von {@code breakBlock}: ein Loch im Meiler nimmt den ganzen Meiler mit. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() == newState.getBlock()) {
            // Nur die Rolle hat sich geaendert (Bohren) - nichts abreissen.
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }

        if (!PileCoreBlockEntity.meltingDown && !level.isClientSide()) {
            BlockEntity tile = level.getBlockEntity(pos);

            if (tile instanceof PileCoreBlockEntity core) {
                core.destroy(level, pos);
            } else if (tile instanceof PileBaseBlockEntity pile) {
                PileCoreBlockEntity core = pile.getCore(level);
                if (core != null && !core.isRemoved()) {
                    core.destroy(level, core.getBlockPos());
                }
                if (pile.getCorePos() != null) {
                    level.setBlock(pos, ModBlocks.PILE_BRICK.get().defaultBlockState(), 3);
                }
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    /** Der Handbohrer bohrt Kanaele. Original: {@code onScrew} mit {@code ToolType.HAND_DRILL}. */
    private InteractionResult drill(BlockState state, Level level, BlockPos pos, Player player,
                                    InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() != ModItems.HAND_DRILL.get() && held.getItem() != ModItems.HAND_DRILL_DESH.get()) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (state.getValue(TYPE) == PileBlockType.CORE) {
            MachinePWRControllerBlock.sendError(level, pos, "Cannot intersect core", player);
            return InteractionResult.CONSUME;
        }

        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof PileBaseBlockEntity pile) {
            PileCoreBlockEntity core = pile.getCore(level);
            if (core != null) {
                Direction dir = hit.getDirection().getOpposite();
                core.drillChannel(level, pos, dir, player);
                return InteractionResult.CONSUME;
            }
        }

        MachinePWRControllerBlock.sendError(level, pos, "No core found", player);
        return InteractionResult.CONSUME;
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return drill(state, level, pos, player, hand, hit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return drill(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
    *///?}
}
