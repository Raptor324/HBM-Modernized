package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 port of {@code TileEntityMachinePumpSteam}: steam-powered water pump. */
public class MachinePumpSteamBlockEntity extends PumpBlockEntity {

    private final FluidTank steam = new FluidTank(ModFluids.STEAM.getSource(), 1_000);
    private final FluidTank lps = new FluidTank(ModFluids.SPENTSTEAM.getSource(), 10);

    public MachinePumpSteamBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_PUMP_STEAM_BE.get(), pos, state, STEAM_SPEED * 100);
    }

    @Override
    protected void serverTick(ServerLevel level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos conPos = pos.relative(dir, 2);
            trySubscribe(steam.getTankType(), level, conPos, dir);
            if (lps.getFill() > 0) {
                tryProvide(lps, level, conPos, dir);
            }
        }
        super.serverTick(level, pos);
    }

    @Override
    protected boolean canOperate() {
        return steam.getFill() >= 100 && lps.getCapacityMb() - lps.getFill() > 0 && water.getFill() < water.getCapacityMb();
    }

    @Override
    protected void operate() {
        steam.drainMb(100);
        lps.fillMb(lps.getTankType(), 1);
        water.fillMb(water.getTankType(), Math.min(STEAM_SPEED, water.getCapacityMb() - water.getFill()));
    }

    public FluidTank getSteamTank() { return steam; }
    public FluidTank getLpsTank() { return lps; }

    @Override public FluidTank[] getAllTanks() { return new FluidTank[]{ water, steam, lps }; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[]{ water, lps }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[]{ steam }; }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        steam.writeToNBT(tag, "tank_steam");
        lps.writeToNBT(tag, "tank_lps");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        steam.readFromNBT(tag, "tank_steam");
        lps.readFromNBT(tag, "tank_lps");
    }
}
