package com.hbm_m.api.network;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Аналог DirPos из 1.7.10: позиция узла + направление от текущего узла к этому.
 * Direction может быть null (UNKNOWN-аналог) для труб с произвольными соединениями (pipeline/anchor).
 */
public class NodeDirPos {

    private final BlockPos pos;
    @Nullable
    private final Direction dir;

    public NodeDirPos(BlockPos pos, @Nullable Direction dir) {
        this.pos = pos.immutable();
        this.dir = dir;
    }

    public NodeDirPos(int x, int y, int z, @Nullable Direction dir) {
        this(new BlockPos(x, y, z), dir);
    }

    public BlockPos getPos() { return pos; }

    @Nullable
    public Direction getDir() { return dir; }

    public int getX() { return pos.getX(); }
    public int getY() { return pos.getY(); }
    public int getZ() { return pos.getZ(); }
}
