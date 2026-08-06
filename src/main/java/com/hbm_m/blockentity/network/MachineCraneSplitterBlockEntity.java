package com.hbm_m.blockentity.network;

import com.hbm_m.block.network.IEnterableBlock;
import com.hbm_m.block.machines.MachineCraneSplitterBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.conveyor.MovingConveyorItemEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Crane Splitter - Port von {@code TileEntityCraneSplitter} (1.7.10 Original). Kein Inventar, kein
 * GUI im Original - reiner Foerderband-Abzweig, der ankommende Items abwechselnd nach links/rechts
 * (relativ zu {@link MachineCraneSplitterBlock#FACING}) umleitet, im Verhaeltnis {@code ratio}:1
 * (leftRatio:rightRatio), mit persistentem Alternierungs-Zaehler ({@code remaining}) fuer eine
 * ueber viele Items hinweg stabile Verteilung statt reinem Pro-Item-Runden.
 * <p>
 * SCOPE-Vereinfachung: Das Original erlaubt leftRatio UND rightRatio unabhaengig (1-16, je per
 * Screwdriver-Sneak-Klick einstellbar). Hier: ein einzelner {@code ratio}-Wert (1-16, Zyklus
 * 1-2-4-8-16), der leftRatio:rightRatio als ratio:1 ausdrueckt - deckt den Kernmechanismus
 * (stabile ratiobasierte Alternierung) ab, ohne zwei unabhaengige Regler nachzubilden. Normaler
 * Screwdriver-Klick rotiert FACING (wie bei allen anderen Foerderband-Bloecken in diesem Port),
 * Sneak-Klick zykelt den Ratio-Wert.
 */
public class MachineCraneSplitterBlockEntity extends BlockEntity implements IEnterableBlock {

    private int ratio = 1;
    private boolean towardLeft = true;
    private int remaining = 1;
    private Direction activeDirection = Direction.NORTH;

    public MachineCraneSplitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_SPLITTER_BE.get(), pos, state);
    }

    public Direction getActiveDirection(Direction facing) {
        return activeDirection;
    }

    @Override
    public void onItemEnter(net.minecraft.world.level.Level level, BlockPos pos, MovingConveyorItemEntity item) {
        if (level.isClientSide) return;

        Direction facing = getBlockState().getValue(MachineCraneSplitterBlock.FACING);
        activeDirection = towardLeft ? facing.getCounterClockWise() : facing.getClockWise();

        remaining--;
        if (remaining <= 0) {
            towardLeft = !towardLeft;
            remaining = towardLeft ? ratio : 1;
        }

        setChanged();
    }

    public void cycleRatio() {
        ratio = switch (ratio) {
            case 1 -> 2;
            case 2 -> 4;
            case 4 -> 8;
            case 8 -> 16;
            default -> 1;
        };
        remaining = towardLeft ? ratio : 1;
        setChanged();
    }

    public int getRatio() { return ratio; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("ratio", ratio);
        tag.putBoolean("towardLeft", towardLeft);
        tag.putInt("remaining", remaining);
        tag.putString("activeDirection", activeDirection.getSerializedName());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ratio = tag.contains("ratio") ? tag.getInt("ratio") : 1;
        towardLeft = tag.getBoolean("towardLeft");
        remaining = tag.contains("remaining") ? tag.getInt("remaining") : 1;
        activeDirection = Direction.byName(tag.getString("activeDirection"));
        if (activeDirection == null) activeDirection = Direction.NORTH;
    }
}
