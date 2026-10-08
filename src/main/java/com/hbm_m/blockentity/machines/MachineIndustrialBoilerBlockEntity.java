package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.redstoneoverradio.IRORValueProvider;
import com.hbm_m.block.machines.MachineIndustrialBoilerBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IHeatSource;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Heatable;
import com.hbm_m.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm_m.inventory.fluid.trait.FT_Heatable.HeatingType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityHeatBoilerIndustrial}: zieht Waerme aus der {@link IHeatSource} unter dem Kern (Diffusion 0,1,
 * max. 12,8 Mio. TU) und wandelt das Eingangsfluid nach dessen erster {@link FT_Heatable}-Stufe um (Wasser 64.000 mB,
 * Ausgang waechst mit dem Umrechnungsverhaeltnis). Kein GUI - Fluidtyp per Fluid-ID-Rechtsklick, Anzeige als
 * Look-Overlay. Anschluesse zwei Bloecke vom Kern in alle vier Richtungen und fuenf Bloecke darueber. Anders als der
 * kleine Kessel platzt er nicht. Im Tom-Feuersturm heizt er sich unter freiem Himmel von selbst auf (TomSaveData.fire).
 */
public class MachineIndustrialBoilerBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2, IRORValueProvider {

    /* KONFIGURIERBAR (Original IConfigurableMachine "boilerIndustrial") */
    public static int maxHeat = 12_800_000;
    public static double diffusion = 0.1D;

    public int heat;
    public final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(ModFluids.WATER.getSource(), 64_000),
            new FluidTank(ModFluids.STEAM.getSource(), 64_000 * 100)
    };
    public boolean isOn;
    private int audioTime;

    public MachineIndustrialBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUSTRIAL_BOILER_BE.get(), pos, state, 0, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineIndustrialBoilerBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) be.serverTick(serverLevel, state);
        else be.clientTick();
    }

    private void serverTick(ServerLevel world, BlockState state) {
        boolean wasOn = this.isOn;

        this.setupTanks();
        this.updateConnections(world);
        this.tryPullHeat();

        int light = world.getBrightness(net.minecraft.world.level.LightLayer.SKY, worldPosition);
        if (light > 7 && com.hbm_m.saveddata.TomSaveData.forWorld(world).fire > 1e-5) {
            this.heat += ((maxHeat - heat) * 0.000005D); //constantly heat up 0.0005% of the remaining heat buffer for rampant but diminishing heating
        }

        this.isOn = false;
        this.tryConvert(world);

        if (this.tanks[1].getFill() > 0) {
            this.sendFluid(world);
        }

        if (wasOn != isOn && state.hasProperty(MachineIndustrialBoilerBlock.LIT)) {
            world.setBlock(worldPosition, state.setValue(MachineIndustrialBoilerBlock.LIT, isOn), 3);
        }

        setChanged();
        sendUpdateToClient();
    }

    private void clientTick() {
        if (this.isOn) audioTime = 20;
        boolean playing = false;
        if (audioTime > 0) {
            audioTime--;
            playing = true;
        }
        // Original: BOILER_LOOP, Lautstaerke 0,125, Reichweite 10
        com.hbm_m.client.sound.MachineLoopSoundClient.tick(this, "hbm:block.boiler", playing, 0.125F, 1.0F, 10);
    }

    protected void tryPullHeat() {
        BlockEntity con = level.getBlockEntity(worldPosition.below());

        if (con instanceof IHeatSource source) {
            int diff = source.getHeatStored() - this.heat;

            if (diff == 0) {
                return;
            }

            if (diff > 0) {
                diff = (int) Math.ceil(diff * diffusion);
                diff = Math.min(diff, maxHeat - this.heat);
                source.useUpHeat(diff);
                this.heat += diff;
                if (this.heat > maxHeat)
                    this.heat = maxHeat;
                return;
            }
        }

        this.heat = Math.max(this.heat - Math.max(this.heat / 1000, 1), 0);
    }

    protected void setupTanks() {

        FT_Heatable trait = FluidType.getTrait(tanks[0].getTankType(), FT_Heatable.class);
        if (trait != null && trait.getEfficiency(HeatingType.BOILER) > 0) {
            HeatingStep entry = trait.getFirstStep();
            tanks[1].setTankType(entry.typeProduced);
            tanks[1].changeTankSize(tanks[0].getMaxFill() * entry.amountProduced / entry.amountReq);
            return;
        }

        tanks[0].setTankType(ModFluids.NONE.getSource());
        tanks[1].setTankType(ModFluids.NONE.getSource());
    }

    protected void tryConvert(ServerLevel world) {

        FT_Heatable trait = FluidType.getTrait(tanks[0].getTankType(), FT_Heatable.class);
        if (trait != null && trait.getEfficiency(HeatingType.BOILER) > 0) {

            HeatingStep entry = trait.getFirstStep();
            int heatReq = (int) Math.max(entry.heatReq / trait.getEfficiency(HeatingType.BOILER), 1);
            int inputOps = this.tanks[0].getFill() / entry.amountReq;
            int outputOps = (this.tanks[1].getMaxFill() - this.tanks[1].getFill()) / entry.amountProduced;
            int heatOps = this.heat / heatReq;

            int ops = Math.min(inputOps, Math.min(outputOps, heatOps));

            this.tanks[0].setFill(this.tanks[0].getFill() - entry.amountReq * ops);
            this.tanks[1].setFill(this.tanks[1].getFill() + entry.amountProduced * ops);
            this.heat -= heatReq * ops;

            if (ops > 0 && world.random.nextInt(400) == 0) {
                world.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 2, worldPosition.getZ() + 0.5,
                        com.hbm_m.sound.HbmSoundsNT.get("hbm:block.boilerGroan"), SoundSource.BLOCKS, 0.5F, 1.0F);
            }

            if (ops > 0) {
                this.isOn = true;
            }
        }
    }

    /** Anschluss samt Richtung (Original {@code DirPos}). */
    private record DirPos(BlockPos pos, Direction dir) { }

    private DirPos[] getConPos() {
        return new DirPos[] {
                new DirPos(worldPosition.relative(Direction.EAST, 2), Direction.EAST),
                new DirPos(worldPosition.relative(Direction.WEST, 2), Direction.WEST),
                new DirPos(worldPosition.relative(Direction.SOUTH, 2), Direction.SOUTH),
                new DirPos(worldPosition.relative(Direction.NORTH, 2), Direction.NORTH),
                new DirPos(worldPosition.above(5), Direction.UP),
        };
    }

    private void updateConnections(Level world) {
        for (DirPos con : getConPos()) {
            this.trySubscribe(tanks[0].getTankType(), world, con.pos, con.dir);
        }
    }

    private void sendFluid(Level world) {
        for (DirPos con : getConPos()) {
            this.tryProvide(tanks[1], world, con.pos, con.dir);
        }
    }

    // ==================== Fluid ====================

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1] }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ==================== ROR ====================

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_VALUE + "input",
                PREFIX_VALUE + "output"
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "input").equals(name)) return "" + tanks[0].getFill();
        if ((PREFIX_VALUE + "output").equals(name)) return "" + tanks[1].getFill();
        return null;
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tanks[0].writeToNBT(tag, "water");
        tanks[1].writeToNBT(tag, "steam");
        tag.putInt("heat", heat);
        tag.putBoolean("isOn", isOn);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tanks[0].readFromNBT(tag, "water");
        tanks[1].readFromNBT(tag, "steam");
        heat = tag.getInt("heat");
        isOn = tag.getBoolean("isOn");
    }

    // ==================== Sonstiges ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.industrial_boiler");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null; // Kein GUI im Original.
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 5, worldPosition.getZ() + 2);
        return bb;
    }
}
