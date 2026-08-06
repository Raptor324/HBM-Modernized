package com.hbm_m.blockentity.machines;

import java.util.List;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.block.machines.MachineFanBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Port of {@code MachineFan.TileEntityFan} (1.7.10 Original) - a redstone-powered industrial fan
 * that pushes (or, if {@link #suck} is set, pulls) entities within a directional cone.
 * <p>
 * SCOPE-Vereinfachung: Das Original stoppt den Luftstrom-Scan vorzeitig an Bloecken, die
 * {@code IBlowable} implementieren (z.B. Feuer ausblasen) - diese Schnittstelle existiert in diesem
 * Port nicht, der Scan stoppt daher nur an massiven Bloecken. Die per Hand-Drill umschaltbare
 * {@code falloff}-Option entfaellt (immer an); {@code suck} laesst sich stattdessen per Schleich-
 * Rechtsklick umschalten.
 */
public class MachineFanBlockEntity extends BlockEntity {

    private static final int RANGE = 10;
    private static final double PUSH = 0.1D;

    public boolean suck = false;

    public MachineFanBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_FAN_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFanBlockEntity be) {
        if (level.isClientSide) return;
        if (!level.hasNeighborSignal(pos)) return;

        Direction dir = state.getValue(MachineFanBlock.FACING);

        int effRange = 0;
        for (int i = 1; i <= RANGE; i++) {
            BlockPos scanPos = pos.relative(dir, i);
            if (level.getBlockState(scanPos).canOcclude()) break;
            effRange = i;
        }
        if (effRange <= 0) return;

        int x = dir.getStepX() * effRange;
        int y = dir.getStepY() * effRange;
        int z = dir.getStepZ() * effRange;

        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;
        AABB box = new AABB(
                cx + Math.min(x, 0), cy + Math.min(y, 0), cz + Math.min(z, 0),
                cx + Math.max(x, 0), cy + Math.max(y, 0), cz + Math.max(z, 0)
        ).inflate(0.5);

        List<Entity> affected = level.getEntitiesOfClass(Entity.class, box);
        for (Entity e : affected) {
            double dist = e.position().distanceTo(new net.minecraft.world.phys.Vec3(cx, cy, cz));
            double coeff = PUSH * 1.5 * (1 - dist / RANGE / 2);
            if (be.suck) coeff *= -1;

            e.setDeltaMovement(e.getDeltaMovement().add(dir.getStepX() * coeff, dir.getStepY() * coeff, dir.getStepZ() * coeff));
        }
    }

    public void toggleSuck() {
        suck = !suck;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("suck", suck);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        suck = tag.getBoolean("suck");
    }
}
