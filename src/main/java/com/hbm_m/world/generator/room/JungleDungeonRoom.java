package com.hbm_m.world.generator.room;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.generic.JungleBricks.Trapped.Trap;
import com.hbm_m.util.ForgeDirection;
import com.hbm_m.world.gen.legacy.L;
import com.hbm_m.world.gen.legacy.LB;
import com.hbm_m.world.gen.legacy.MB;
import com.hbm_m.world.gen.legacy.MetaBlock;
import com.hbm_m.world.gen.legacy.VB;
import com.hbm_m.world.generator.CellularDungeon;
import com.hbm_m.world.generator.CellularDungeonRoom;
import com.hbm_m.world.generator.DungeonToolbox;
import com.hbm_m.world.generator.JungleDungeon;
import com.hbm_m.world.generator.TimedGenerator;

import net.minecraft.world.level.LevelAccessor;

/** 1:1 {@code com.hbm.world.generator.room.JungleDungeonRoom}: Grundraum mit Lavaboden, Kiste oder Loch. */
public class JungleDungeonRoom extends CellularDungeonRoom {

    public JungleDungeonRoom(CellularDungeon parent) {
        super(parent);
    }

    /** Port-Hilfe: Ziegel, die zur Falle werden duerfen. */
    protected static boolean isBrick(LB bl, boolean trapToo) {
        return bl == MB.brick_jungle || bl == MB.brick_jungle_cracked || bl == MB.brick_jungle_lava || (trapToo && bl == MB.brick_jungle_trap);
    }

    /** Port-Hilfe: {@code world.setBlock(x, y, z, brick_jungle_trap, trap.ordinal(), 3)} nach Pruefung des Ziegels. */
    protected static void trap(LevelAccessor world, int x, int y, int z, Trap trap, boolean trapToo) {
        if (isBrick(L.getBlock(world, x, y, z), trapToo)) {
            L.setBlock(world, x, y, z, MB.brick_jungle_trap, trap.ordinal(), 3);
        }
    }

    @Override
    public void generateMain(final LevelAccessor world, final int x, final int y, final int z) {

        if (!(this.parent instanceof JungleDungeon))
            return; // nur zur Sicherheit

        TimedGenerator.addOp(world, () -> {

            DungeonToolbox.generateBox(world, x, y, z, parent.width, 1, parent.width, parent.floor);
            DungeonToolbox.generateBox(world, x, y + 1, z, parent.width, parent.height - 1, parent.width, VB.air);
            DungeonToolbox.generateBox(world, x, y + parent.height - 1, z, parent.width, 1, parent.width, parent.ceiling);

            int rtd = L.rand(world).nextInt(50);

            if (rtd < 5) { // 1:10 Lavaboden
                List<MetaBlock> metas = new ArrayList<>();
                metas.add(new MetaBlock(MB.brick_jungle_cracked));
                metas.add(new MetaBlock(MB.brick_jungle_lava));
                metas.add(new MetaBlock(MB.brick_jungle_lava));

                DungeonToolbox.generateBox(world, x + parent.width / 2 - 1, y, z + parent.width / 2 - 1, 3, 1, 3, metas);

            } else if (rtd < 10) { // 1:5 Dschungelkiste
                L.setBlock(world, x + 1 + L.rand(world).nextInt(parent.width - 1), y + 1, z + L.rand(world).nextInt(parent.width - 1), MB.crate_jungle, 0, 2);

            } else if (rtd < 20) { // 1:5 Versuch, ein Loch zu machen

                if (!((JungleDungeon) JungleDungeonRoom.this.parent).hasHole) {

                    boolean punched = false;

                    for (int a = 0; a < 3; a++) {
                        for (int b = 0; b < 3; b++) {

                            LB bl = L.getBlock(world, x + 1 + a, y - 4, z + 1 + b);

                            if (L.getBlock(world, x + 1 + a, y - 1, z + 1 + b) == VB.air) {
                                if (bl == MB.brick_jungle || bl == MB.brick_jungle_cracked || bl == MB.brick_jungle_lava || bl == MB.brick_jungle_trap) {
                                    L.setBlock(world, x + 1 + a, y, z + 1 + b, MB.brick_jungle_fragile);
                                    punched = true;
                                }
                            }
                        }
                    }

                    if (punched)
                        ((JungleDungeon) JungleDungeonRoom.this.parent).hasHole = true;
                }
            }
        });
    }

    @Override
    public void generateWall(final LevelAccessor world, final int x, final int y, final int z, final ForgeDirection wall, final boolean door) {

        TimedGenerator.addOp(world, () -> {

            if (wall == ForgeDirection.NORTH) {
                DungeonToolbox.generateBox(world, x, y + 1, z, parent.width, parent.height - 2, 1, parent.wall);
                if (door)
                    DungeonToolbox.generateBox(world, x + parent.width / 2 - 1, y + 1, z, 3, 3, 1, VB.air);
            }

            if (wall == ForgeDirection.SOUTH) {
                DungeonToolbox.generateBox(world, x, y + 1, z + parent.width - 1, parent.width, parent.height - 2, 1, parent.wall);
                if (door)
                    DungeonToolbox.generateBox(world, x + parent.width / 2 - 1, y + 1, z + parent.width - 1, 3, 3, 1, VB.air);
            }

            if (wall == ForgeDirection.WEST) {
                DungeonToolbox.generateBox(world, x, y + 1, z, 1, parent.height - 2, parent.width, parent.wall);
                if (door)
                    DungeonToolbox.generateBox(world, x, y + 1, z + parent.width / 2 - 1, 1, 3, 3, VB.air);
            }

            if (wall == ForgeDirection.EAST) {
                DungeonToolbox.generateBox(world, x + parent.width - 1, y + 1, z, 1, parent.height - 2, parent.width, parent.wall);
                if (door)
                    DungeonToolbox.generateBox(world, x + parent.width - 1, y + 1, z + parent.width / 2 - 1, 1, 3, 3, VB.air);
            }
        });
    }
}
