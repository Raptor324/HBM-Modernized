package com.hbm_m.multiblock;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.space.SpaceApi;
import com.hbm_m.interfaces.IMultiblockController;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;

/**
 * E7 (Fork-Abweichung, <b>nur mit Space-Addon</b>, {@link SpaceApi#forkActive()}): 1:1 NTM Space
 * {@code BlockDummyable.onBlockPlaced/onBlockPlacedBy/drawHighlight}. Multiblock-Maschinen lassen sich an Waende und
 * Decken setzen - die angeklickte Seite bestimmt die Lage:
 * <ul>
 *   <li>Boden/Decke: Blickrichtung des Spielers wie bisher ({@code facingDir} aus dem Gierwinkel).</li>
 *   <li>Wand: {@code facingDir = placedSide} (die angeklickte Seite zeigt aus der Wand heraus).</li>
 *   <li>Kern: {@code placed + dir * -getOffset()}, y + {@code getHeightOffset()} (wie bisher).</li>
 *   <li>Decke ({@code placedSide == DOWN}): Kern um die volle Hoehe ({@code up + down}) nach unten.</li>
 *   <li>Wand: Kern so weit von der Wand weg, dass die Struktur an ihr anliegt
 *       ({@code placed + placedSide * rotDim[placedSide.getOpposite()]}).</li>
 * </ul>
 * Dazu darf der Spieler im gerade gesetzten Multiblock stehen, bis er ihn verlaesst ({@code internalPlayers}).
 *
 * <p>Ohne Addon wird nichts hiervon benutzt - {@link MultiblockBlockItem} und {@link DummyableStructureBuilder}
 * arbeiten exakt wie bisher.
 *
 * <p>Technische Abweichung: HBM kennt je Maschine nur die fertige Struktur (kein {@code getDimensions()} des
 * Hauptquaders), daher werden die Ausdehnungen aus allen Teilen der gedrehten Struktur bestimmt.
 */
public final class ForkMultiblockPlacement {

    private ForkMultiblockPlacement() {}

    /** Spieler, die gerade in einem frisch gesetzten Multiblock stehen (Original {@code internalPlayers}). */
    private static final Map<UUID, Boolean> INTERNAL_PLAYERS = new ConcurrentHashMap<>();

    /**
     * Platzierungskontext am Kern nach den Fork-Regeln, oder null (Fork aus / kein Multiblock). Der Kontext liefert
     * als Blickrichtung {@code facingDir.getOpposite()}, damit {@code getStateForPlacement} die Fork-Ausrichtung setzt.
     */
    @Nullable
    public static BlockPlaceContext shift(BlockPlaceContext context, Block block) {
        if (!SpaceApi.forkActive()) return null;
        if (!(block instanceof IMultiblockController controller)) return null;
        MultiblockStructureHelper helper = controller.getStructureHelper();
        if (helper == null) return null;

        Direction placedSide = context.getClickedFace();
        Direction facingDir = placedSide.getAxis() == Direction.Axis.Y ? context.getHorizontalDirection().getOpposite() : placedSide;
        Direction look = facingDir.getOpposite();

        BlockPos placed = context.getClickedPos();
        BlockState preview = block.getStateForPlacement(withLook(context, look, null));
        if (preview == null || !preview.hasProperty(HorizontalDirectionalBlock.FACING)) return null;
        Direction dir = preview.getValue(HorizontalDirectionalBlock.FACING);

        int offset = controller.getOffset();
        if (offset < 0) offset = helper.getPlacementOffset();
        // Original: placed + facingDir * -getOffset(). HBM legt den Kern (wie MultiblockBlockItem) ueber die
        // Blockausrichtung fest - fuer Maschinen ohne getDirModified ist das dieselbe Zelle.
        int ox = placed.getX() - dir.getStepX() * offset;
        int oy = placed.getY() + controller.getHeightOffset();
        int oz = placed.getZ() - dir.getStepZ() * offset;

        if (placedSide == Direction.DOWN) {
            int[] b = bounds(helper, dir);
            oy -= b[1] - b[0];
        } else if (placedSide != Direction.UP) {
            int ext = extent(helper, dir, placedSide.getOpposite());
            ox = placed.getX() + placedSide.getStepX() * ext;
            oz = placed.getZ() + placedSide.getStepZ() * ext;
        }

        BlockPos core = new BlockPos(ox, oy, oz);
        return withLook(context, look, core);
    }

    /** Kontext mit fester Blickrichtung, optional an einen anderen Block versetzt (Klickseite bleibt). */
    private static BlockPlaceContext withLook(BlockPlaceContext context, Direction look, @Nullable BlockPos at) {
        BlockPos pos = at != null ? at : context.getClickedPos();
        BlockHitResult hit = new BlockHitResult(at != null ? Vec3.atCenterOf(pos) : context.getClickLocation(),
                context.getClickedFace(), pos, context.isInside());
        BlockPlaceContext base = context;
        return new BlockPlaceContext(base.getPlayer(), base.getHand(), base.getItemInHand(), hit) {
            @Override
            public Direction getHorizontalDirection() {
                return look;
            }

            @Override
            public BlockPos getClickedPos() {
                return at != null ? at : base.getClickedPos();
            }

            @Override
            public boolean canPlace() {
                return at != null ? getLevel().getBlockState(at).canBeReplaced(this) : base.canPlace();
            }

            @Override
            public boolean replacingClickedOnBlock() {
                return at != null || base.replacingClickedOnBlock();
            }
        };
    }

    /** {minY, maxY} der gedrehten Struktur relativ zum Kern (Kern eingeschlossen). */
    private static int[] bounds(MultiblockStructureHelper helper, Direction facing) {
        int min = 0, max = 0;
        for (BlockPos rel : helper.getPartOffsets()) {
            BlockPos p = helper.getRotatedPos(BlockPos.ZERO, rel, facing);
            min = Math.min(min, p.getY());
            max = Math.max(max, p.getY());
        }
        return new int[] { min, max };
    }

    /** Wie weit die gedrehte Struktur vom Kern aus in Richtung {@code toward} reicht (Original {@code rotDim[..]}). */
    private static int extent(MultiblockStructureHelper helper, Direction facing, Direction toward) {
        int ext = 0;
        for (BlockPos rel : helper.getPartOffsets()) {
            BlockPos p = helper.getRotatedPos(BlockPos.ZERO, rel, facing);
            ext = Math.max(ext, p.getX() * toward.getStepX() + p.getY() * toward.getStepY() + p.getZ() * toward.getStepZ());
        }
        return ext;
    }

    // ───────────────────────────── Spieler im Multiblock ─────────────────────────────

    /** Nach dem Setzen: steht der Spieler in der Struktur, darf er bis zum Verlassen hindurchgehen. */
    public static void onPlaced(Level level, @Nullable Player player) {
        if (player == null || !SpaceApi.forkActive()) return;
        if (isPlayerInside(level, player)) INTERNAL_PLAYERS.put(player.getUUID(), Boolean.TRUE);
    }

    /** true = keine Kollision fuer dieses Teil (Original {@code addCollisionBoxesToList}: {@code internalPlayers.contains}). */
    public static boolean ignoresCollision(BlockGetter level, CollisionContext context) {
        if (INTERNAL_PLAYERS.isEmpty() || !(context instanceof EntityCollisionContext ec)) return false;
        Entity entity = ec.getEntity();
        if (!(entity instanceof Player player) || !INTERNAL_PLAYERS.containsKey(player.getUUID())) return false;
        if (isPlayerInside(level, player)) return true;
        INTERNAL_PLAYERS.remove(player.getUUID());
        return false;
    }

    /** Original {@code isPlayerInside}: Fuss- oder Kopfblock ist Teil eines Multiblocks. */
    private static boolean isPlayerInside(BlockGetter level, Player player) {
        BlockPos feet = BlockPos.containing(player.getX(), player.getY() + 0.1D, player.getZ());
        return isMultiblock(level.getBlockState(feet)) || isMultiblock(level.getBlockState(feet.above()));
    }

    private static boolean isMultiblock(BlockState state) {
        return state.getBlock() instanceof com.hbm_m.block.UniversalMachinePartBlock || state.getBlock() instanceof IMultiblockController;
    }
}
