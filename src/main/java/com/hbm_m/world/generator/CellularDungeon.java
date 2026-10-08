package com.hbm_m.world.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.hbm_m.util.ForgeDirection;
import com.hbm_m.world.gen.legacy.MetaBlock;

import net.minecraft.world.level.LevelAccessor;

/** 1:1 {@code com.hbm.world.generator.CellularDungeon}: Raster aus Raeumen, zufaellig verzweigt. */
public class CellularDungeon {

    // Puffer-"Karte" der Raeume vor dem Bau
    CellularDungeonRoom[][] cells;
    ForgeDirection[][] doors;
    // Reihenfolge der Abarbeitung
    List<int[]> order = new ArrayList<>();

    int dimX;
    int dimZ;
    /** Grundbreite (und -laenge) eines Raums. */
    public int width;
    /** Raumhoehe. */
    public int height;
    public List<MetaBlock> floor = new ArrayList<>();
    public List<MetaBlock> ceiling = new ArrayList<>();
    public List<MetaBlock> wall = new ArrayList<>();
    public List<CellularDungeonRoom> rooms = new ArrayList<>();
    int tries;
    int branches;
    boolean isGenerating;

    public CellularDungeon(int width, int height, int dimX, int dimZ, int tries, int branches) {
        this.dimX = dimX;
        this.dimZ = dimZ;
        this.width = width;
        this.height = height;
        this.tries = tries;
        this.branches = branches;
    }

    public CellularDungeon(int width, int height, int dimX, int dimZ, int tries, int branches, MetaBlock floor, MetaBlock ceiling, MetaBlock wall) {
        this(width, height, dimX, dimZ, tries, branches);
        this.floor.add(floor);
        this.ceiling.add(ceiling);
        this.wall.add(wall);
    }

    public void generate(LevelAccessor world, int x, int y, int z, Random rand) {

        if (isGenerating) return;

        isGenerating = true;

        x -= dimX * width / 2;
        z -= dimZ * width / 2;

        compose(rand);

        for (int[] coord : order) {

            if (coord == null || coord.length != 2)
                continue;

            int dx = coord[0];
            int dz = coord[1];

            if (cells[dx][dz] != null) {

                if (doors[dx][dz] == null)
                    doors[dx][dz] = ForgeDirection.UNKNOWN;

                cells[dx][dz].generate(world, x + dx * (width - 1), y, z + dz * (width - 1), doors[dx][dz]);
            }
        }

        isGenerating = false;
    }

    int rec = 0;

    public void compose(Random rand) {

        cells = new CellularDungeonRoom[dimX][dimZ];
        doors = new ForgeDirection[dimX][dimZ];
        order.clear();

        int startX = dimX / 2;
        int startZ = dimZ / 2;

        cells[startX][startZ] = DungeonToolbox.getRandom(rooms, rand);
        doors[startX][startZ] = ForgeDirection.UNKNOWN;
        order.add(new int[] { startX, startZ });

        rec = 0;
        addRoom(startX, startZ, rand, ForgeDirection.UNKNOWN, DungeonToolbox.getRandom(rooms, rand));
    }

    // ist x/z belegt, wird einfach das naechste zufaellige Nachbarfeld genommen
    private boolean addRoom(int x, int z, Random rand, ForgeDirection door, CellularDungeonRoom room) {

        rec++;
        if (rec > tries)
            return false;

        if (x < 0 || z < 0 || x >= dimX || z >= dimZ)
            return false;

        if (cells[x][z] != null) {

            ForgeDirection dir = getRandomDir(rand);
            addRoom(x + dir.offsetX, z + dir.offsetZ, rand, dir.getOpposite(), DungeonToolbox.getRandom(rooms, rand));
            return false;
        }

        if (room.daisyChain == null || addRoom(x + room.daisyDirection.offsetX, z + room.daisyDirection.offsetZ, rand, ForgeDirection.UNKNOWN, room.daisyChain)) {
            cells[x][z] = room;
            doors[x][z] = door;
            order.add(new int[] { x, z });
        }

        for (int i = 0; i < branches; i++) {
            ForgeDirection dir = getRandomDir(rand);
            addRoom(x + dir.offsetX, z + dir.offsetZ, rand, dir.getOpposite(), DungeonToolbox.getRandom(rooms, rand));
        }

        return true;
    }

    public static ForgeDirection getRandomDir(Random rand) {
        return ForgeDirection.getOrientation(rand.nextInt(4) + 2);
    }
}
