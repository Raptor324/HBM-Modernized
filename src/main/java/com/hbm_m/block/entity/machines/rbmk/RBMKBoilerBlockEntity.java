package com.hbm_m.block.entity.machines.rbmk;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.handler.rbmk.RBMKDials;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.RBMKBoilerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class RBMKBoilerBlockEntity extends RBMKColumnBlockEntity implements MenuProvider {

    public final FluidTank waterTank;
    public final FluidTank steamTank;
    /** 0=STEAM, 1=HOTSTEAM, 2=SUPERHOTSTEAM, 3=ULTRAHOTSTEAM */
    public int steamGrade = 0;
    public int lastConsumption = 0;
    public int lastOutput      = 0;

    private static final double[] HEAT_THRESHOLD = { 100, 300, 450, 600 };
    private static final double[] STEAM_FACTOR   = { 1, 10, 100, 1000 };

    public RBMKBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_BOILER_BE.get(), pos, state);
        waterTank = new FluidTank(10_000);
        steamTank = new FluidTank(1_000_000);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKBoilerBlockEntity be) {
        baseTick(level, pos, state, be);
        if (level.isClientSide) return;

        be.lastConsumption = 0;
        be.lastOutput      = 0;
        double heatAvail = be.heat - HEAT_THRESHOLD[be.steamGrade];

        if (heatAvail > 0 && be.waterTank.getFill() > 0) {
            double heatPerMb = RBMKDials.getBoilerHeatConsumption(level);
            double factor    = STEAM_FACTOR[be.steamGrade];
            int waterUsed, steamProduced;

            if (be.steamGrade == 3) {
                steamProduced = (int) Math.floor((heatAvail / heatPerMb) * 100 / factor);
                waterUsed     = (int) Math.floor(steamProduced / 100.0 * factor);
                if (be.waterTank.getFill() < waterUsed) {
                    steamProduced = (int) Math.floor(be.waterTank.getFill() * 100.0 / factor);
                    waterUsed     = (int) Math.floor(steamProduced / 100.0 * factor);
                }
            } else {
                waterUsed     = (int) Math.min(Math.floor(heatAvail / heatPerMb), be.waterTank.getFill());
                steamProduced = (int) Math.floor(waterUsed * 100.0 / factor);
            }

            if (waterUsed > 0) {
                be.waterTank.setFill(be.waterTank.getFill() - waterUsed);
                int space = be.steamTank.getMaxFill() - be.steamTank.getFill();
                be.steamTank.setFill(be.steamTank.getFill() + Math.min(steamProduced, space));
                be.heat -= waterUsed * heatPerMb;
                be.lastConsumption = waterUsed;
                be.lastOutput      = Math.min(steamProduced, space);
                be.setChanged();
            }
        }
    }

    public void cycleCompressor() {
        steamTank.setFill(steamTank.getFill() / 10);
        steamGrade = (steamGrade + 1) % 4;
        setChanged();
    }

    @Override public Component getDisplayName() { return Component.translatable("block.hbm_m.rbmk_boiler"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new RBMKBoilerMenu(id, inv, this); }
    @Override public RBMKType getRBMKType()      { return RBMKType.OTHER; }
    @Override public ColumnType getConsoleType() { return ColumnType.BOILER; }

    @Override
    public net.minecraft.nbt.CompoundTag getNBTForConsole() {
        net.minecraft.nbt.CompoundTag d = new net.minecraft.nbt.CompoundTag();
        d.putInt("water",    waterTank.getFill());
        d.putInt("maxWater", waterTank.getMaxFill());
        d.putInt("steam",    steamTank.getFill());
        d.putInt("maxSteam", steamTank.getMaxFill());
        d.putShort("steamGrade", (short) steamGrade);
        return d;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        waterTank.writeToNBT(tag, "water");
        steamTank.writeToNBT(tag, "steam");
        tag.putInt("steamGrade", steamGrade);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        waterTank.readFromNBT(tag, "water");
        steamTank.readFromNBT(tag, "steam");
        steamGrade = tag.getInt("steamGrade");
    }
}

