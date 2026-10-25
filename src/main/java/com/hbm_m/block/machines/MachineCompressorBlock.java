package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineCompressorBaseBlockEntity;
import com.hbm_m.blockentity.machines.MachineCompressorBlockEntity;
import com.hbm_m.blockentity.machines.MachineCompressorCompactBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code MachineCompressor} und {@code MachineCompressorCompact}.
 * <ul>
 *   <li>Kompressor: {@code {2,0,1,2,1,1}}, {@code {3,-3,1,1,1,1}}, {@code {8,-4,0,0,1,1}}, {@code getOffset 2};
 *   Anschlusszellen hinten, links, rechts.</li>
 *   <li>Kompakter Kompressor: {@code {2,0,1,1,3,3}}, {@code getOffset 1}; sechs Anschlusszellen auf Hoehe 1.</li>
 * </ul>
 */
public class MachineCompressorBlock extends DummyableMachineBlock {

    private final boolean compact;

    public MachineCompressorBlock(Properties properties) {
        this(properties, false);
    }

    public MachineCompressorBlock(Properties properties, boolean compact) {
        super(properties);
        this.compact = compact;
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // defineStructure laeuft im Superkonstruktor, bevor 'compact' gesetzt ist - daher ueber den Registriernamen
        return isCompactConstruction() ? compactStructure() : largeStructure();
    }

    private static final ThreadLocal<Boolean> BUILDING_COMPACT = ThreadLocal.withInitial(() -> false);

    private static boolean isCompactConstruction() {
        return BUILDING_COMPACT.get();
    }

    /** Baut den kompakten Kompressor (die Struktur muss beim Superkonstruktor feststehen). */
    public static MachineCompressorBlock createCompact(Properties properties) {
        BUILDING_COMPACT.set(true);
        try {
            return new MachineCompressorBlock(properties, true);
        } finally {
            BUILDING_COMPACT.set(false);
        }
    }

    private static MultiblockStructureHelper largeStructure() {
        return DummyableStructureBuilder.create()
                .box(2, 0, 1, 2, 1, 1)
                .box(3, -3, 1, 1, 1, 1)
                .box(8, -4, 0, 0, 1, 1)
                .extra(-1, 0, 0)
                .extra(0, 0, 1)
                .extra(0, 0, -1)
                .placementOffset(2)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    private static MultiblockStructureHelper compactStructure() {
        return DummyableStructureBuilder.create()
                .box(2, 0, 1, 1, 3, 3)
                .extra(0, 1, 3)
                .extra(0, 1, -3)
                .extra(1, 1, 1)
                .extra(1, 1, -1)
                .extra(-1, 1, 1)
                .extra(-1, 1, -1)
                .placementOffset(1)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return compact ? new MachineCompressorCompactBlockEntity(pos, state) : new MachineCompressorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (compact) {
            return createTickerHelper(type, ModBlockEntities.COMPRESSOR_COMPACT_BE.get(),
                    (lvl, pos, st, be) -> MachineCompressorBaseBlockEntity.tick(lvl, pos, st, be));
        }
        return createTickerHelper(type, ModBlockEntities.COMPRESSOR_BE.get(),
                (lvl, pos, st, be) -> MachineCompressorBaseBlockEntity.tick(lvl, pos, st, be));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.sidedSuccess(level.isClientSide()); // Original standardOpenBehavior: geschlichen true ohne GUI
        return open(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.sidedSuccess(level.isClientSide()); // Original standardOpenBehavior: geschlichen true ohne GUI
        return open(level, pos, player);
    }
    *///?}

    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MachineCompressorBaseBlockEntity compressor) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, compressor, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineCompressorBlock> CODEC = simpleCodec(MachineCompressorBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
