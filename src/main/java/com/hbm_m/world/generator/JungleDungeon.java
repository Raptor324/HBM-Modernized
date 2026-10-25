package com.hbm_m.world.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.hbm_m.world.gen.legacy.L;
import com.hbm_m.world.gen.legacy.MB;
import com.hbm_m.world.gen.legacy.MetaBlock;

import net.minecraft.world.level.LevelAccessor;

/** 1:1 {@code com.hbm.world.generator.JungleDungeon}: Enargit-Verlies im Dschungel (drei Ebenen). */
public class JungleDungeon extends CellularDungeon {

    public boolean hasHole = false;

    public JungleDungeon(int width, int height, int dimX, int dimZ, int tries, int branches) {
        super(width, height, dimX, dimZ, tries, branches);

        this.floor.add(new MetaBlock(MB.brick_jungle));
        this.floor.add(new MetaBlock(MB.brick_jungle_cracked));

        for (int i = 0; i < 50; i++) {
            this.wall.add(new MetaBlock(MB.brick_jungle));
            this.wall.add(new MetaBlock(MB.brick_jungle_cracked));
        }
        for (int i = 0; i < 16; i++) {
            this.wall.add(new MetaBlock(MB.brick_jungle_glyph, i));
        }

        this.ceiling.add(new MetaBlock(MB.brick_jungle));
        this.ceiling.add(new MetaBlock(MB.brick_jungle_cracked));
    }

    @Override
    public void generate(final LevelAccessor world, final int x, final int y, final int z, final Random rand) {
        super.generate(world, x, y, z, rand);

        TimedGenerator.addOp(world, () -> {

            JungleDungeon that = JungleDungeon.this;

            // kein Loch gemacht -> unterste Ebene
            if (!that.hasHole) {

                List<int[]> rooms = new ArrayList<>();

                for (int i = 0; i < that.cells.length; i++) {
                    for (int j = 0; j < that.cells[0].length; j++) {
                        if (that.cells[i][j] != null)
                            rooms.add(new int[] { i, j });
                    }
                }

                if (!rooms.isEmpty()) {

                    int ix = x - dimX * width / 2;
                    int iz = z - dimZ * width / 2;

                    int[] room = rooms.get(L.rand(world).nextInt(rooms.size()));
                    L.setBlock(world, ix + room[0] * (width - 1) + width / 2, y, iz + room[1] * (width - 1) + width / 2, MB.brick_jungle_circle);
                }
            }

            that.hasHole = false;
        });

        // da alles ueber Auftraege gebaut wird, muss das hier auch einer sein; die Reihenfolge bleibt erhalten
    }
}
