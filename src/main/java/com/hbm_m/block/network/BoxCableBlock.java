package com.hbm_m.block.network;

import java.util.concurrent.ConcurrentHashMap;

import com.hbm_m.api.energy.WireBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Порт PowerCableBox (1.7.10): цельноблочный короб-кабель из красной меди, 5 мета-размеров (0-4).
 * В оригинале — один блок с метаданными; здесь, по конвенции порта, один блок на размер
 * (red_cable_box, red_cable_box_1 .. red_cable_box_4).
 * Энергосеть — через WireBlockEntity (аналог TileEntityCableBaseNT).
 */
public class BoxCableBlock extends WireBlock {

    public static final IntegerProperty SIZE = IntegerProperty.create("size", 0, 4);

    private static final VoxelShape[] SHAPE_CACHE = new VoxelShape[5 * 64];

    private final int size;

    public BoxCableBlock(Properties properties, int size) {
        super(properties);
        this.size = size;
        this.registerDefaultState(this.defaultBlockState().setValue(SIZE, size));
    }

    //? if > 1.20.1 {
    /*@Override
    protected com.mojang.serialization.MapCodec<? extends WireBlock> codec() {
        return simpleCodec(props -> new BoxCableBlock(props, this.size));
    }
    *///?}

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SIZE);
    }

    public int getSize() {
        return size;
    }

    /** Порт setBlockBoundsBasedOnState: box сжимается с размером и вытягивается к подключениям. */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int size = state.getValue(SIZE);
        int mask = connectionMask(state);
        int key = size * 64 + mask;
        VoxelShape cached = SHAPE_CACHE[key];
        if (cached != null) return cached;
        VoxelShape shape = computeShape(size, mask);
        SHAPE_CACHE[key] = shape;
        return shape;
    }

    public static int connectionMask(BlockState state) {
        boolean nX = state.getValue(WEST), pX = state.getValue(EAST);
        boolean nY = state.getValue(DOWN), pY = state.getValue(UP);
        boolean nZ = state.getValue(NORTH), pZ = state.getValue(SOUTH);
        return (pX ? 32 : 0) + (nX ? 16 : 0) + (pY ? 8 : 0) + (nY ? 4 : 0) + (pZ ? 2 : 0) + (nZ ? 1 : 0);
    }

    private static VoxelShape computeShape(int size, int mask) {
        double lower = (0.125D + 0.0625D * size) * 16;
        double upper = (0.875D - 0.0625D * size) * 16;

        if (mask == 0) {
            return Block.box(lower, lower, lower, upper, upper, upper);
        }
        // Только ось X: сплошной прогон по X (биты Y и Z чисты)
        if ((mask & 0b001111) == 0) {
            return Block.box(0, lower, lower, 16, upper, upper);
        }
        // Только ось Y (биты X и Z чисты: pX|nX|pZ|nZ = 0b110011)
        if ((mask & 0b110011) == 0) {
            return Block.box(lower, 0, lower, upper, 16, upper);
        }
        // Только ось Z (биты X и Y чисты: pX|nX|pY|nY = 0b111100)
        if ((mask & 0b111100) == 0) {
            return Block.box(lower, lower, 0, upper, upper, 16);
        }
        // Общий случай: ядро + вытянутые к подключениям грани
        boolean nX = (mask & 16) != 0, pX = (mask & 32) != 0;
        boolean nY = (mask & 4) != 0, pY = (mask & 8) != 0;
        boolean nZ = (mask & 1) != 0, pZ = (mask & 2) != 0;
        return Block.box(
                nX ? 0 : lower,
                nY ? 0 : lower,
                nZ ? 0 : lower,
                pX ? 16 : upper,
                pY ? 16 : upper,
                pZ ? 16 : upper);
    }
}
