package com.hbm_m.blockentity.network;

import com.hbm_m.block.network.RedPylonLargeBlock;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Порт TileEntityPylonLarge (1.7.10): большой пилон, QUAD, радиус 100 м.
 * Четыре крепления по диагонали (широкий низ, узкий верх); требует подстанцию в сети.
 */
public class RedPylonLargeBlockEntity extends PylonBaseBlockEntity {

    public RedPylonLargeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RED_PYLON_LARGE_BE.get(), pos, state);
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.QUAD;
    }

    @Override
    public Vec3[] getMountPos() {
        // Углы соответствуют оригиналу: NORTH→0°, WEST→45°, SOUTH→90°, EAST→135°.
        Direction dir = getBlockState().getValue(RedPylonLargeBlock.FACING);
        double topOff = 0.75 + 0.0625;
        double sideOff = 3.375;
        double angle = switch (dir) {
            case NORTH -> 0.0;
            case WEST -> Math.PI * 0.25;
            case SOUTH -> Math.PI * 0.5;
            case EAST -> Math.PI * 0.75;
            default -> 0.0;
        };
        double vx = Math.cos(angle) * sideOff;
        double vz = Math.sin(angle) * sideOff;
        return new Vec3[] {
                new Vec3(0.5 + vx, 11.5 + topOff, 0.5 + vz),
                new Vec3(0.5 + vx, 11.5 - topOff, 0.5 + vz),
                new Vec3(0.5 - vx, 11.5 + topOff, 0.5 - vz),
                new Vec3(0.5 - vx, 11.5 - topOff, 0.5 - vz)
        };
    }

    @Override
    public double getMaxWireLength() {
        return 100;
    }
}
