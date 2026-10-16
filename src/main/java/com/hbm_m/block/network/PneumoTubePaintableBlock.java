package com.hbm_m.block.network;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.pneumatic.PneumoTubeBlockEntity;
import com.hbm_m.blockentity.network.pneumatic.PneumoTubePaintableBlockEntity;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code PneumoTubePaintableBlock} (1.7.10): das Rohr, das sich tarnen laesst.
 *
 * <p>Es verhaelt sich in allem wie das gewoehnliche {@link PneumoTubeBlock Druckluftrohr} - dieselben
 * Verbindungen, derselbe Schraubendreher-Umlauf, dieselben Oberflaechen. Nur laesst es sich mit
 * einem Rechtsklick unter einem beliebigen festen Block verstecken; der Handbohrer kratzt den
 * Anstrich wieder ab.</p>
 *
 * <p><b>Es ist ein voller Wuerfel</b>, kein duennes Rohr - genau darum laesst es sich buendig in
 * eine Wand setzen. Auf den Seiten liegt eine Markierung: die Einzugsseite und die Ausgabeseite
 * bekommen ein eigenes Zeichen, alle uebrigen ein neutrales. Der Entschaerfer schaltet diese
 * Markierung ab, sobald die Anlage steht und man sie nicht mehr sehen will.</p>
 */
public class PneumoTubePaintableBlock extends PneumoTubeBlock {

    //? if > 1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<PneumoTubePaintableBlock> CODEC =
            simpleCodec(PneumoTubePaintableBlock::new);
    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}

    public PneumoTubePaintableBlock(Properties properties) {
        super(properties);
    }

    /** Gezeichnet wird ausschliesslich im Renderer - er kennt den Tarnblock. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** Original: die bemalbare Fassung ist ein gewoehnlicher Vollblock, kein Rohrgeflecht. */
    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.block();
    }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.block();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PneumoTubePaintableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PNEUMO_TUBE_PAINTABLE_BE.get(),
                (lvl, pos, st, be) -> PneumoTubeBlockEntity.tick(lvl, pos, st, (PneumoTubeBlockEntity) be));
    }

    /**
     * 1:1-Port des zusaetzlichen Zweigs in {@code onBlockActivated} und {@code onScrew}: erst
     * Anstrich und Handbohrer, alles Uebrige macht das gewoehnliche Rohr.
     */
    private InteractionResult paintOrPass(BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand) {
        if (!(level.getBlockEntity(pos) instanceof PneumoTubePaintableBlockEntity tube)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        // Original: der Handbohrer nimmt den Anstrich wieder ab.
        if (held.is(ModItems.HAND_DRILL.get())) {
            if (tube.getCamo() == null) return InteractionResult.PASS;
            if (!level.isClientSide()) tube.setCamo(null);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // Original: der Entschaerfer schaltet die Seitenmarkierung um (die Metadatenzahl).
        if (held.is(ModItems.DEFUSER.get())) {
            if (!level.isClientSide()) tube.setMarkingsHidden(!tube.areMarkingsHidden());
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (held.getItem() instanceof BlockItem blockItem && tube.getCamo() == null) {
            BlockState paint = blockItem.getBlock().defaultBlockState();
            if (allowedPaint(level, pos, blockItem.getBlock(), paint)) {
                if (!level.isClientSide()) tube.setCamo(paint);
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        return InteractionResult.PASS;
    }

    /** 1:1-Port von {@code BlockCablePaintable.allowedPaint}. */
    private static boolean allowedPaint(Level level, BlockPos pos, Block paint, BlockState paintState) {
        if (paint == Blocks.GRASS_BLOCK) return false;
        if (paint == com.hbm_m.block.ModBlocks.PNEUMATIC_TUBE_PAINTABLE.get()) return false;
        return paintState.isSolidRender(level, pos);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        InteractionResult painted = paintOrPass(state, level, pos, player, hand);
        if (painted != InteractionResult.PASS) return painted;

        return super.use(state, level, pos, player, hand, hit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        InteractionResult painted = paintOrPass(state, level, pos, player, InteractionHand.MAIN_HAND);
        if (painted != InteractionResult.PASS) return painted;

        return super.useWithoutItem(state, level, pos, player, hit);
    }
    *///?}
}
