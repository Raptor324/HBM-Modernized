package com.hbm_m.world.generator.room;

import com.hbm_m.block.generic.JungleBricks.Trapped.Trap;
import com.hbm_m.world.gen.legacy.L;
import com.hbm_m.world.generator.CellularDungeon;
import com.hbm_m.world.generator.TimedGenerator;

import net.minecraft.world.level.LevelAccessor;

/** 1:1 {@code com.hbm.world.generator.room.JungleDungeonRoomMagic}. */
public class JungleDungeonRoomMagic extends JungleDungeonRoom {

    public JungleDungeonRoomMagic(CellularDungeon parent) {
        super(parent);
    }

    @Override
    public void generateMain(final LevelAccessor world, final int x, final int y, final int z) {
        super.generateMain(world, x, y, z);

        TimedGenerator.addOp(world, () -> {
            int ix = L.rand(world).nextInt(3) + 1;
            int iz = L.rand(world).nextInt(3) + 1;
            trap(world, x + ix, y, z + iz, Trap.MAGIC_CONVERSTION, false);
        });
    }
}
