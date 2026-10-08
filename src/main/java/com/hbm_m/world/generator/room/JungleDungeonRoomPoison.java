package com.hbm_m.world.generator.room;

import com.hbm_m.block.generic.JungleBricks.Trapped.Trap;
import com.hbm_m.world.generator.CellularDungeon;
import com.hbm_m.world.generator.TimedGenerator;

import net.minecraft.world.level.LevelAccessor;

/** 1:1 {@code com.hbm.world.generator.room.JungleDungeonRoomPoison}. */
public class JungleDungeonRoomPoison extends JungleDungeonRoom {

    public JungleDungeonRoomPoison(CellularDungeon parent) {
        super(parent);
    }

    @Override
    public void generateMain(final LevelAccessor world, final int x, final int y, final int z) {
        super.generateMain(world, x, y, z);

        TimedGenerator.addOp(world, () -> {
            // zweistufig: erst nach den Waenden eingereiht
            TimedGenerator.addOp(world, () -> {
                for (int i = 1; i < 4; i++) trap(world, x, y + 2, z + i, Trap.POISON_DART, false);
                for (int i = 1; i < 4; i++) trap(world, x + parent.width - 1, y + 2, z + i, Trap.POISON_DART, false);
                for (int i = 1; i < 4; i++) trap(world, x + i, y + 2, z, Trap.POISON_DART, false);
                for (int i = 1; i < 4; i++) trap(world, x + i, y + 2, z + parent.width - 1, Trap.POISON_DART, false);
            });
        });
    }
}
