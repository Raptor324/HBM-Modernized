package com.hbm_m.block.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@link com.hbm.blocks.generic.BlockNTMGlass}/{@code BlockNTMGlassCT} (Borglas, Bleiglas, Uranglas, Quarzglas,
 * Laminat ...): halbdurchsichtig (BlockBreakable, gleiche Nachbarn verdecken sich), Renderpass 0 = cutout, 1 =
 * durchscheinend (per Modell-render_type), verbundene Texturen ueber den CT-Wrapper, Drops nur mit Behutsamkeit bzw.
 * {@code doesDrop} (Loot-Tabelle).
 */
public class BlockNTMGlass extends HalfTransparentBlock {

    public BlockNTMGlass(Properties properties) {
        super(properties.noOcclusion().isViewBlocking((s, w, p) -> false).isSuffocating((s, w, p) -> false).isRedstoneConductor((s, w, p) -> false));
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter world, BlockPos pos) {
        return true;
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction dir) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, dir);
    }
}
