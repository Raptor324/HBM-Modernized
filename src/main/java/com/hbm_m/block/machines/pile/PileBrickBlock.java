package com.hbm_m.block.machines.pile;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.MachinePWRControllerBlock;
import com.hbm_m.blockentity.machines.pile.PileBaseBlockEntity;
import com.hbm_m.blockentity.machines.pile.PileCoreBlockEntity;
import com.hbm_m.blockentity.machines.pile.PileOrientation;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1-Port von {@code BlockPileBrick} (1.7.10): der Graphitziegel, aus dem der Meiler gemauert wird.
 *
 * <p>Ein Rechtsklick mit dem Handbohrer auf eine <b>senkrechte</b> Wand des Stapels startet den
 * Zusammenbau. Der Ziegel, auf den geklickt wurde, wird dabei zum Kern, und die geklickte Seite
 * legt die Bauachse fest: Kanaele laengs dieser Achse werden spaeter Brennstoffkanaele, Kanaele
 * quer dazu Lueftungskanaele.</p>
 *
 * <p>Der Bohrer tastet zuerst die Ausmasse des Quaders ab - hoechstens
 * {@value #MAX_H_SIZE} Bloecke je Achse, mindestens {@value #MIN_H_SIZE} -, prueft dann, dass der
 * Quader ausschliesslich aus Ziegeln besteht, und wandelt ihn erst danach um. Jeder Fehler nennt
 * die Stelle, an der es hakt.</p>
 *
 * <p>Der Kern darf nicht auf einer Aussenflaeche sitzen: er braucht auf allen vier Seiten - oben,
 * unten, links, rechts - mindestens einen Ziegel, sonst laesst sich kein Kanal an ihm vorbeifuehren.</p>
 */
public class PileBrickBlock extends Block {

    /** Original: {@code MIN_V_SIZE}/{@code MIN_H_SIZE} = 5, {@code MAX_*_SIZE} = 15. */
    public static final int MIN_V_SIZE = 5;
    public static final int MIN_H_SIZE = 5;
    public static final int MAX_V_SIZE = 15;
    public static final int MAX_H_SIZE = 15;

    public PileBrickBlock(Properties properties) {
        super(properties);
    }

    /** 1:1-Port von {@code onScrew}. */
    private InteractionResult assemble(Level level, BlockPos pos, Player player,
                                       InteractionHand hand, BlockHitResult hit) {

        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() != ModItems.HAND_DRILL.get() && held.getItem() != ModItems.HAND_DRILL_DESH.get()) {
            return InteractionResult.PASS;
        }

        Direction side = hit.getDirection();
        // Original: von oben oder unten laesst sich nicht zusammenbauen.
        if (side.getAxis() == Direction.Axis.Y) return InteractionResult.PASS;

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        Direction dir = side.getOpposite();          // in den Stapel hinein
        Direction dirLeft = dir.getCounterClockWise(); // quer dazu

        int negHeight = 0, posHeight = 0, left = 0, right = 0, depth = 0;

        // ── Ausmasse abtasten ──
        for (int i = 1; i <= MAX_V_SIZE - 1; i++) {
            if (!isBrick(level, pos.above(i))) break;
            posHeight = i;
        }
        for (int i = 1; i <= MAX_V_SIZE - posHeight - 1; i++) {
            if (!isBrick(level, pos.below(i))) break;
            negHeight = i;
        }
        for (int i = 1; i <= MAX_H_SIZE - 1; i++) {
            if (!isBrick(level, pos.relative(dirLeft, i))) break;
            left = i;
        }
        for (int i = 1; i <= MAX_H_SIZE - left - 1; i++) {
            if (!isBrick(level, pos.relative(dirLeft, -i))) break;
            right = i;
        }
        for (int i = 1; i <= MAX_H_SIZE; i++) {
            if (!isBrick(level, pos.relative(dir, i))) break;
            depth = i;
        }

        // ── Groessenpruefung ──
        if (posHeight + negHeight + 1 < MIN_V_SIZE) {
            MachinePWRControllerBlock.sendError(level, pos.above(posHeight), "Height too low (<" + MIN_V_SIZE + ")", player);
            return InteractionResult.CONSUME;
        }
        if (left + right + 1 < MIN_H_SIZE) {
            MachinePWRControllerBlock.sendError(level, pos.relative(dirLeft, left), "Width too low (<" + MIN_H_SIZE + ")", player);
            return InteractionResult.CONSUME;
        }
        if (depth + 1 < MIN_H_SIZE) {
            MachinePWRControllerBlock.sendError(level, pos.relative(dir, depth), "Depth too low (<" + MIN_H_SIZE + ")", player);
            return InteractionResult.CONSUME;
        }

        // ── Der Kern darf nicht auf einer Aussenflaeche liegen ──
        if (posHeight == 0 || negHeight == 0 || left == 0 || right == 0) {
            MachinePWRControllerBlock.sendError(level, pos, "Core cannot be on an edge", player);
            return InteractionResult.CONSUME;
        }

        // ── Der ganze Quader muss aus Ziegeln bestehen ──
        for (int h = -negHeight; h <= posHeight; h++) {
            for (int v = -left; v <= right; v++) {
                for (int d = 0; d <= depth; d++) {
                    BlockPos at = pos.relative(dirLeft, -v).relative(dir, d).above(h);
                    if (!isBrick(level, at)) {
                        MachinePWRControllerBlock.sendError(level, at, "Graphite block missing", player);
                        return InteractionResult.CONSUME;
                    }
                }
            }
        }

        // ── Umwandeln ──
        BlockState pileState = ModBlocks.PILE_BLOCK.get().defaultBlockState();

        for (int h = -negHeight; h <= posHeight; h++) {
            for (int v = -left; v <= right; v++) {
                for (int d = 0; d <= depth; d++) {
                    BlockPos at = pos.relative(dirLeft, -v).relative(dir, d).above(h);

                    if (at.equals(pos)) {
                        level.setBlock(at, pileState.setValue(PileBlock.TYPE, PileBlockType.CORE), 3);
                        if (level.getBlockEntity(at) instanceof PileCoreBlockEntity core) {
                            core.setOrientation(PileOrientation.of(dir));
                            core.setupSize(posHeight, negHeight, left, right, depth + 1);
                            core.setChanged();
                        }
                        continue;
                    }

                    // Kante heisst: der Block liegt auf mindestens zwei der drei Aussenflaechen.
                    int edgeCount = 0;
                    if (h == -negHeight || h == posHeight) edgeCount++;
                    if (v == -left || v == right) edgeCount++;
                    if (d == 0 || d == depth) edgeCount++;

                    level.setBlock(at, pileState.setValue(PileBlock.TYPE,
                            edgeCount > 1 ? PileBlockType.EDGE : PileBlockType.DUMMY), 3);

                    BlockEntity be = level.getBlockEntity(at);
                    if (be instanceof PileBaseBlockEntity pile) pile.setCore(pos);
                }
            }
        }

        return InteractionResult.CONSUME;
    }

    private boolean isBrick(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(this);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return assemble(level, pos, player, hand, hit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return assemble(level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
    *///?}
}
