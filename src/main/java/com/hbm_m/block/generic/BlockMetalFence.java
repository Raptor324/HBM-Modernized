package com.hbm_m.block.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 порт BlockMetalFence из HBM NTM 1.7.10.
 * В оригинале это был один блок с meta 0 (забор) и meta 1 (столб).
 * В modern версии разделен на два блока: FENCE_METAL (forcePost=false) и FENCE_METAL_POST (forcePost=true).
 *
 * Особенности оригинала:
 * 1. Столб скрывается на прямых линиях (straightX / straightZ) у обычного забора.
 *    У забора-столба (meta 1) столб отображается всегда.
 * 2. Высота коллизии 1.0F (16 пикселей), в отличие от ванильного деревянного забора (1.5F / 24 пикселя).
 * 3. Соединяется сам с собой (забор + столб), с калитками, заборами и сплошными блоками.
 */
public class BlockMetalFence extends FenceBlock {

    public static final BooleanProperty POST = BooleanProperty.create("post");
    private final boolean forcePost;

    public BlockMetalFence(Properties properties, boolean forcePost) {
        super(properties);
        this.forcePost = forcePost;
        this.registerDefaultState(this.defaultBlockState().setValue(POST, forcePost));
    }

    public boolean isForcePost() {
        return forcePost;
    }

    public static boolean calculatePost(boolean forcePost, boolean north, boolean south, boolean east, boolean west) {
        if (forcePost) {
            return true;
        }
        boolean straightX = !north && !south && east && west;
        boolean straightZ = !east && !west && north && south;
        return !straightX && !straightZ;
    }

    public boolean hasPost(BlockState state) {
        if (forcePost) {
            return true;
        }
        if (state.hasProperty(POST)) {
            return state.getValue(POST);
        }
        return calculatePost(
                forcePost,
                state.getValue(NORTH),
                state.getValue(SOUTH),
                state.getValue(EAST),
                state.getValue(WEST)
        );
    }

    @Override
    public boolean connectsTo(BlockState state, boolean isSideSolid, Direction direction) {
        if (state.getBlock() instanceof BlockMetalFence) {
            return true;
        }
        return super.connectsTo(state, isSideSolid, direction);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        boolean post = calculatePost(
                forcePost,
                state.getValue(NORTH),
                state.getValue(SOUTH),
                state.getValue(EAST),
                state.getValue(WEST)
        );
        return state.setValue(POST, post);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        boolean post = calculatePost(
                forcePost,
                updated.getValue(NORTH),
                updated.getValue(SOUTH),
                updated.getValue(EAST),
                updated.getValue(WEST)
        );
        return updated.setValue(POST, post);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Канон 1.7.10 (BlockMetalFence.addCollisionBoxesToList):
        // Высота коллизии 1.0F (16 пикселей), как у обычного блока / getShape(),
        // в отличие от ванильного FenceBlock (1.5F / 24 пикселя).
        return this.getShape(state, level, pos, context);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POST);
    }

    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockMetalFence> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    com.mojang.serialization.Codec.BOOL.fieldOf("force_post").forGetter(BlockMetalFence::isForcePost),
                    propertiesCodec()
            ).apply(instance, (forcePost, properties) -> new BlockMetalFence(properties, forcePost))
    );
    @SuppressWarnings("unchecked")
    @Override
    public com.mojang.serialization.MapCodec<FenceBlock> codec() {
        // Vanilla FenceBlock.codec() returns MapCodec<FenceBlock> exactly (invariant
        // generics), so the subclass codec needs an unchecked wildcard cast.
        return (com.mojang.serialization.MapCodec<FenceBlock>) (com.mojang.serialization.MapCodec<?>) CODEC;
    }
    *///?}
}
