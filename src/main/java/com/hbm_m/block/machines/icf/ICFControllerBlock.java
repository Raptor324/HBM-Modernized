package com.hbm_m.block.machines.icf;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.MachinePWRControllerBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.icf.ICFControllerBlockEntity;
import com.hbm_m.blockentity.machines.icf.ICFPhantomBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code MachineICFController} (1.7.10): das Steuerpult, das den Laser zusammenbaut.
 *
 * <p>Ein Rechtsklick startet die Suche. Sie beginnt <b>hinter</b> dem Pult und breitet sich von
 * dort aus: Zellen, Strahler, Kondensatoren und Turbolader werden weiterverfolgt, Huelle und
 * Anschluesse beenden den Weg an dieser Stelle. Trifft die Suche auf etwas anderes als ein
 * Laserbauteil, bricht sie mit einer Fehlermeldung an genau dieser Stelle ab und nichts wird
 * gebaut. Hoechstens {@value #MAX_SIZE} Bloecke.</p>
 *
 * <p>Geht die Suche auf, wird jedes gefundene Bauteil durch einen {@link ICFPhantomBlock
 * Platzhalter} ersetzt, der sich merkt, was er war. Danach zaehlt das Steuerpult aus, wieviel davon
 * wirklich angebunden ist, siehe {@link ICFControllerBlockEntity#setup}.</p>
 *
 * <p><b>Abweichung:</b> das Original arbeitet rekursiv; hier laeuft dieselbe Suche ueber einen
 * Stapel, damit sie bei tausend Bloecken nicht den Aufrufstapel sprengt. Das Ergebnis ist dasselbe.</p>
 */
public class ICFControllerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** Original: {@code maxSize = 1024}. */
    public static final int MAX_SIZE = 1024;

    public ICFControllerBlock(Properties properties) {
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

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // Original: die Vorderseite zeigt zum Spieler, der Laser liegt also dahinter.
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ICFControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ICF_CONTROLLER_BE.get(),
                (lvl, pos, st, be) -> ICFControllerBlockEntity.tick(lvl, pos, st, (ICFControllerBlockEntity) be));
    }

    // ── Zusammenbau ─────────────────────────────────────────────────────────

    /** 1:1-Port von {@code assemble} samt {@code floodFill}. */
    private void assemble(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof ICFControllerBlockEntity controller)) return;

        Map<BlockPos, ICFLaserPart> assembly = new HashMap<>();
        List<BlockPos> ports = new ArrayList<>();
        Set<BlockPos> cells = new HashSet<>();
        Set<BlockPos> emitters = new HashSet<>();
        Set<BlockPos> capacitors = new HashSet<>();
        Set<BlockPos> turbochargers = new HashSet<>();

        // Original: die Suche startet hinter dem Pult.
        Direction dir = level.getBlockState(pos).getValue(FACING).getOpposite();

        boolean errored = !floodFill(level, pos.relative(dir), player, assembly, ports,
                cells, emitters, capacitors, turbochargers);

        if (errored) {
            controller.setAssembled(false);
            return;
        }

        for (Map.Entry<BlockPos, ICFLaserPart> entry : assembly.entrySet()) {
            BlockPos at = entry.getKey();
            ICFLaserPart part = entry.getValue();

            level.setBlock(at, ModBlocks.ICF_BLOCK.get().defaultBlockState()
                    .setValue(ICFPhantomBlock.PORT, part == ICFLaserPart.PORT), 3);

            if (level.getBlockEntity(at) instanceof ICFPhantomBlockEntity phantom) {
                phantom.setup(part, pos);
            }
        }

        controller.setup(ports, cells, emitters, capacitors, turbochargers);
        controller.setAssembled(true);
    }

    /**
     * 1:1-Port von {@code floodFill}, nur ueber einen Stapel statt ueber Rekursion.
     *
     * @return false, sobald irgendwo ein Fremdblock im Weg liegt oder die Groesse gesprengt wird
     */
    private boolean floodFill(Level level, BlockPos start, Player player,
                              Map<BlockPos, ICFLaserPart> assembly, List<BlockPos> ports,
                              Set<BlockPos> cells, Set<BlockPos> emitters,
                              Set<BlockPos> capacitors, Set<BlockPos> turbochargers) {

        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            BlockPos at = queue.poll();
            if (assembly.containsKey(at)) continue;

            if (assembly.size() >= MAX_SIZE) {
                MachinePWRControllerBlock.sendError(level, at, "Max size exceeded", player);
                return false;
            }

            Block block = level.getBlockState(at).getBlock();
            if (!(block instanceof ICFLaserComponentBlock component)) {
                MachinePWRControllerBlock.sendError(level, at, "Non-laser block", player);
                return false;
            }

            ICFLaserPart part = component.getPart();
            assembly.put(at.immutable(), part);

            switch (part) {
                case CASING -> { }
                case PORT -> ports.add(at.immutable());
                case CELL -> cells.add(at.immutable());
                case EMITTER -> emitters.add(at.immutable());
                case CAPACITOR -> capacitors.add(at.immutable());
                case TURBO -> turbochargers.add(at.immutable());
            }

            // Original: die Aussenhaut beendet den Weg, alles andere breitet sich weiter aus.
            if (part.isCasing()) continue;

            for (Direction offset : Direction.values()) queue.add(at.relative(offset));
        }

        return true;
    }

    private InteractionResult interact(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        if (level.getBlockEntity(pos) instanceof ICFControllerBlockEntity controller
                && !controller.isAssembled()) {
            assemble(level, pos, player);
        }
        return InteractionResult.CONSUME;
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return interact(level, pos, player);
    }
    *///?}
}
