package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.redstoneoverradio.IRORValueProvider;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityHeatBoiler}: zieht Waerme aus der {@link IHeatSource} unter dem Kern (Diffusion 0,1, max.
 * 3,2 Mio. TU) und wandelt das Eingangsfluid nach dessen erster {@link FT_Heatable}-Stufe um (Wasser -> Dampf); der
 * Ausgangstank waechst mit dem Umrechnungsverhaeltnis. Anschluesse seitlich zwei Bloecke vom Kern ({@code rot}) und
 * oben. Laeuft der Ausgangstank voll, platzt der Kessel (oberer Teil wird geraeumt, Explosion Staerke 5); danach
 * liefert er nur noch Stahl und Kupfer. Rampant-Heizen durch den Tom-Brand kommt mit dem Einschlag (R9).
 */
public class MachineBoilerBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2, IRORValueProvider {

    /* KONFIGURIERBAR (Original IConfigurableMachine "boiler") */
    public static int maxHeat = 3_200_000;
    public static double diffusion = 0.1D;
    public static boolean canExplode = true;

    public int heat;
    public final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(ModFluids.WATER.getSource(), 16_000),
            new FluidTank(ModFluids.STEAM.getSource(), 16_000 * 100)
    };
    public boolean isOn;
    public boolean hasExploded = false;

    public MachineBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOILER_BE.get(), pos, state, 0, 0L, 0L, 0L);
    }

    /** Original {@code getRotation(UP)} der Blickrichtung. */
    private Direction rot() {
        BlockState state = getBlockState();
        Direction dir = state.hasProperty(com.hbm_m.block.machines.MachineBoilerBlock.FACING)
                ? state.getValue(com.hbm_m.block.machines.MachineBoilerBlock.FACING) : Direction.NORTH;
        return dir.getClockWise();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineBoilerBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) be.serverTick(serverLevel);
        else be.clientTick();
    }

    private void serverTick(ServerLevel world) {

        if (!this.hasExploded) {
            this.setupTanks();
            this.updateConnections(world);
            this.tryPullHeat();

            this.isOn = false;
            this.tryConvert(world);

            if (this.tanks[1].getFill() > 0) {
                this.sendFluid(world);
            }
        }

        setChanged();
        sendUpdateToClient();
    }

    private void clientTick() {
        com.hbm_m.sound.ClientSoundBootstrap.updateSound(this, !hasExploded && audioTime(), this::createAudioLoop);
    }

    private int audioTime;

    /** Original: {@code if(isOn) audioTime = 20;} - der Loop laeuft eine Sekunde nach. */
    private boolean audioTime() {
        if (this.isOn) audioTime = 20;
        if (audioTime > 0) {
            audioTime--;
            return true;
        }
        return false;
    }

    private Object createAudioLoop() {
        try {
            return Class.forName("com.hbm_m.client.sound.BoilerLoopSoundFactory").getMethod("create", MachineBoilerBlockEntity.class).invoke(null, this);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
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

            if (outputOps == 0 && canExplode) {
                this.hasExploded = true;
                syncExplodedState();
                int xCoord = worldPosition.getX(), yCoord = worldPosition.getY(), zCoord = worldPosition.getZ();
                com.hbm_m.multiblock.MultiblockStructureHelper.runSafeRemove(() -> {
                    for (int x = xCoord - 1; x <= xCoord + 1; x++) {
                        for (int y = yCoord + 2; y <= yCoord + 3; y++) {
                            for (int z = zCoord - 1; z <= zCoord + 1; z++) {
                                world.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                            }
                        }
                    }
                    world.setBlock(worldPosition.above(), Blocks.AIR.defaultBlockState(), 3);

                    ExplosionVNT xnt = new ExplosionVNT(world, xCoord + 0.5, yCoord + 2, zCoord + 0.5, 5F);
                    xnt.setEntityProcessor(new EntityProcessorStandard().withRangeMod(3F));
                    xnt.setPlayerProcessor(new PlayerProcessorStandard());
                    xnt.setSFX(new ExplosionEffectStandard());
                    xnt.explode();
                });
            }
        }
    }

    /**
     * Setzt {@code hasExploded} in den Blockzustand; der steuert Drops und das Item des geplatzten Kessels.
     */
    private void syncExplodedState() {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(com.hbm_m.block.machines.MachineBoilerBlock.EXPLODED)) return;
        if (state.getValue(com.hbm_m.block.machines.MachineBoilerBlock.EXPLODED) == this.hasExploded) return;
        level.setBlock(worldPosition, state.setValue(com.hbm_m.block.machines.MachineBoilerBlock.EXPLODED, this.hasExploded), 3);
    }

    /** Anschluss samt Richtung (Original {@code DirPos}). */
    private record DirPos(BlockPos pos, Direction dir) { }

    private DirPos[] getConPos() {
        Direction dir = rot();
        return new DirPos[] {
                new DirPos(worldPosition.relative(dir, 2), dir),
                new DirPos(worldPosition.relative(dir, -2), dir.getOpposite()),
                new DirPos(worldPosition.above(4), Direction.UP),
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
        if (hasExploded) {
            if ((PREFIX_VALUE + "input").equals(name)) return "0";
            if ((PREFIX_VALUE + "output").equals(name)) return "0";
            return null;
        }
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
        tag.putBoolean("exploded", hasExploded);
        tag.putBoolean("isOn", isOn);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tanks[0].readFromNBT(tag, "water");
        tanks[1].readFromNBT(tag, "steam");
        heat = tag.getInt("heat");
        hasExploded = tag.getBoolean("exploded");
        isOn = tag.getBoolean("isOn");
    }

    // ==================== Sonstiges ====================

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
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null; // Kein GUI im Original.
    }

    public FluidTank getWaterTank() { return tanks[0]; }
    public FluidTank getSteamTank() { return tanks[1]; }
    public int getHeat() { return heat; }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 4, worldPosition.getZ() + 2);
        return bb;
    }
}
