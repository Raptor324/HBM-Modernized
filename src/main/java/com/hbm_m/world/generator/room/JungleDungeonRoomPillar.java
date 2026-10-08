package com.hbm_m.world.generator.room;

import com.hbm_m.block.generic.JungleBricks.Trapped.Trap;
import com.hbm_m.world.generator.CellularDungeon;
import com.hbm_m.world.generator.TimedGenerator;

import net.minecraft.world.level.LevelAccessor;

/** 1:1 {@code com.hbm.world.generator.room.JungleDungeonRoomPillar}. */
public class JungleDungeonRoomPillar extends JungleDungeonRoom {

    public JungleDungeonRoomPillar(CellularDungeon parent) {
        super(parent);
    }

    @Override
    public void generateMain(final LevelAccessor world, final int x, final int y, final int z) {
        super.generateMain(world, x, y, z);

        TimedGenerator.addOp(world, () -> {
            for (int a = 0; a < 3; a++) {
                for (int b = 0; b < 3; b++) {
                    if (a == 1 && b == 1)
                        continue;
                    trap(world, x + 1 + a, y + 4, z + 1 + b, Trap.PILLAR, true);
                }
            }
        });
    }
}
