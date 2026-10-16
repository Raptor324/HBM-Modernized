package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Heatable;
import com.hbm_m.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm_m.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm_m.interfaces.IHeatSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * Basic Boiler - Port von {@code TileEntityHeatBoiler} (1.7.10 Original). Kein Inventar, kein GUI
 * (Original zeigt Status nur per Hover-Tooltip). Zieht Waerme vom Block direkt darunter (sofern
 * dieser {@link IHeatSource} implementiert - z.B. spaeter ein Ofen/Reaktor) und wandelt Wasser
 * darueber in Dampf um, ueber die bereits vorhandene {@code FT_Heatable}/{@code HeatingType.BOILER}
 * Fluid-Eigenschaft (Wasser: 200 Waerme + 1mB Wasser -&gt; 100mB Dampf, 1:1 aus dem Original).
 * Der erzeugte Dampf speist die {@link MachineSteamEngineBlockEntity}.
 * <p>
 * SCOPE-Entscheidungen:
 * <ul>
 *   <li>Explosions-Notabschaltung (Original: 3x3x2-Bereich wird geraeumt + {@code ExplosionVNT})
 *   1:1 im Ausloese-Zustand ({@code outputOps==0}, Dampftank voll), aber vereinfacht auf eine
 *   normale {@link Level#explode}-Explosion ohne Sonder-Bereichsraeumung.</li>
 *   <li>Multiblock ueber dieses Repo-eigene Framework statt des veralteten {@code BlockDummyable}-
 *   Systems - vereinfacht-aber-proportionales Footprint.</li>
 *   <li>Umgebungsgeraeusch-Loop/Stoehn-Sounds des Originals entfallen (rein akustisch).</li>
 * </ul>
 */
public class MachineBoilerBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    private static final int WATER_TANK_CAPACITY_MB = 16_000;
    private static final int STEAM_TANK_CAPACITY_MB = 1_600_000;
    private static final int MAX_HEAT = 3_200_000;
    private static final double DIFFUSION = 0.1D;

    private final FluidTank waterTank = new FluidTank(ModFluids.WATER.getSource(), WATER_TANK_CAPACITY_MB);
    private final FluidTank steamTank = new FluidTank(ModFluids.STEAM.getSource(), STEAM_TANK_CAPACITY_MB);

    private int heat = 0;
    private boolean hasExploded = false;

    public MachineBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOILER_BE.get(), pos, state, 0, 0L, 0L, 0L);
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return waterTank.getForgeFluidCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    public static void tick(Level level, BlockPos pos, BlockState state, MachineBoilerBlockEntity be) {
        if (level.isClientSide() || be.hasExploded || !(level instanceof ServerLevel serverLevel)) return;
        be.serverTick(serverLevel, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                trySubscribe(waterTank.getTankType(), level, pos.relative(dir), dir);
                tryProvide(steamTank, level, pos.relative(dir), dir);
            }
        }

        pullOrDecayHeat(level, pos);
        tryConvert(level, pos);

        setChanged();
        sendUpdateToClient();
    }

    private void pullOrDecayHeat(Level level, BlockPos pos) {
        BlockEntity below = level.getBlockEntity(pos.below());
        if (below instanceof IHeatSource source) {
            int diff = source.getHeatStored() - heat;
            if (diff > 0) {
                int pulled = Math.min((int) Math.ceil(diff * DIFFUSION), MAX_HEAT - heat);
                if (pulled > 0) {
                    source.useUpHeat(pulled);
                    heat += pulled;
                    return;
                }
            }
        }
        heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
    }

    private void tryConvert(ServerLevel level, BlockPos pos) {
        FT_Heatable trait = FluidType.getTrait(waterTank.getStoredFluid(), FT_Heatable.class);
        if (trait == null) return;

        HeatingStep step = trait.getFirstStep();
        if (step == null) return;

        int heatReq = Math.max((int) (step.heatReq / Math.max(trait.getEfficiency(HeatingType.BOILER), 0.0001D)), 1);

        int inputOps = waterTank.getFluidAmountMb() / step.amountReq;
        int outputOps = (steamTank.getCapacityMb() - steamTank.getFluidAmountMb()) / step.amountProduced;
        int heatOps = heat / heatReq;

        if (outputOps == 0 && heat > 0) {
            explode(level, pos);
            return;
        }

        int ops = Math.min(Math.min(inputOps, outputOps), heatOps);
        if (ops <= 0) return;

        waterTank.drainMb(step.amountReq * ops);
        steamTank.fillMb(step.typeProduced, step.amountProduced * ops);
        heat -= heatReq * ops;
    }

    private void explode(ServerLevel level, BlockPos pos) {
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                5.0F, Level.ExplosionInteraction.BLOCK);
        com.hbm_m.platform.PlatformHooks.playSound(level, pos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 2.0F, 1.0F);
        hasExploded = true;
        heat = 0;
    }

    // ==================== IFluidUserMK2 / MK2-Netz ====================

    @Override
    public FluidTank[] getAllTanks() { return new FluidTank[] { waterTank, steamTank }; }

    @Override
    public FluidTank[] getSendingTanks() { return new FluidTank[] { steamTank }; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { waterTank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null;
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("heat", heat);
        tag.putBoolean("has_exploded", hasExploded);
        waterTank.writeToNBT(tag, "tank_water");
        steamTank.writeToNBT(tag, "tank_steam");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        heat = tag.getInt("heat");
        hasExploded = tag.getBoolean("has_exploded");
        waterTank.readFromNBT(tag, "tank_water");
        steamTank.readFromNBT(tag, "tank_steam");
    }

    // ==================== GETTERS / MENU ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.boiler");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false; // Kein Inventar - siehe Klassenkommentar.
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null; // Kein GUI im Original.
    }

    public FluidTank getWaterTank() {
        return waterTank;
    }

    public FluidTank getSteamTank() {
        return steamTank;
    }

    public int getHeat() {
        return heat;
    }
}
