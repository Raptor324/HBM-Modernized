package com.hbm_m.world.generator.room;

import com.hbm_m.block.generic.JungleBricks.Trapped.Trap;
import com.hbm_m.world.gen.legacy.L;
import com.hbm_m.world.generator.CellularDungeon;
import com.hbm_m.world.generator.TimedGenerator;

import net.minecraft.world.level.LevelAccessor;

/** 1:1 {@code com.hbm.world.generator.room.JungleDungeonRoomWeb}. */
public class JungleDungeonRoomWeb extends JungleDungeonRoom {

    public JungleDungeonRoomWeb(CellularDungeon parent) {
        super(parent);
    }

    @Override
    public void generateMain(final LevelAccessor world, final int x, final int y, final int z) {
        super.generateMain(world, x, y, z);

        TimedGenerator.addOp(world, () -> {
            for (int a = 1; a < 4; a++) {
                for (int b = 1; b < 4; b++) {
                    if (L.rand(world).nextInt(3) == 0) {
                        trap(world, x + a, y, z + b, Trap.WEB, false);
                    }
                }
            }
        });
    }
}
