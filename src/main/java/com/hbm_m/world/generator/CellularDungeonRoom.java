package com.hbm_m.world.generator;

import com.hbm_m.util.ForgeDirection;
import com.hbm_m.world.gen.legacy.MetaBlock;
import com.hbm_m.world.gen.legacy.VB;

import net.minecraft.world.level.LevelAccessor;

/** 1:1 {@code com.hbm.world.generator.CellularDungeonRoom}. */
public class CellularDungeonRoom {

    protected CellularDungeon parent;
    protected CellularDungeonRoom daisyChain = null;
    protected ForgeDirection daisyDirection = ForgeDirection.UNKNOWN;

    public CellularDungeonRoom(CellularDungeon parent) {
        this.parent = parent;
    }

    // je Bau nur eine Tuer; mehrere Tueren entstehen durch Verkettung. Der Startraum nutzt eine ungueltige Richtung.
    public void generate(LevelAccessor world, int x, int y, int z, ForgeDirection door) {

        generateMain(world, x, y, z);

        for (int i = 2; i < 6; i++) {
            ForgeDirection dir = ForgeDirection.getOrientation(i);
            generateWall(world, x, y, z, dir, dir == door);
        }
    }

    public void generateMain(LevelAccessor world, int x, int y, int z) {
        DungeonToolbox.generateBox(world, x, y, z, parent.width, 1, parent.width, parent.floor);
        DungeonToolbox.generateBox(world, x, y + 1, z, parent.width, parent.height - 1, parent.width, new MetaBlock(VB.air));
        DungeonToolbox.generateBox(world, x, y + parent.height - 1, z, parent.width, 1, parent.width, parent.ceiling);
    }

    public void generateWall(LevelAccessor world, int x, int y, int z, ForgeDirection wall, boolean door) {

        if (wall == ForgeDirection.NORTH) {
            DungeonToolbox.generateBox(world, x, y + 1, z, parent.width, parent.height - 2, 1, parent.wall);
            if (door)
                DungeonToolbox.generateBox(world, x + parent.width / 2, y + 1, z, 1, 2, 1, new MetaBlock(VB.air));
        }

        if (wall == ForgeDirection.SOUTH) {
            DungeonToolbox.generateBox(world, x, y + 1, z + parent.width - 1, parent.width, parent.height - 2, 1, parent.wall);
            if (door)
                DungeonToolbox.generateBox(world, x + parent.width / 2, y + 1, z + parent.width - 1, 1, 2, 1, new MetaBlock(VB.air));
        }

        if (wall == ForgeDirection.WEST) {
            DungeonToolbox.generateBox(world, x, y + 1, z, 1, parent.height - 2, parent.width, parent.wall);
            if (door)
                DungeonToolbox.generateBox(world, x, y + 1, z + parent.width / 2, 1, 2, 1, new MetaBlock(VB.air));
        }

        if (wall == ForgeDirection.EAST) {
            DungeonToolbox.generateBox(world, x + parent.width - 1, y + 1, z, 1, parent.height - 2, parent.width, parent.wall);
            if (door)
                DungeonToolbox.generateBox(world, x + parent.width - 1, y + 1, z + parent.width / 2, 1, 2, 1, new MetaBlock(VB.air));
        }
    }
}
