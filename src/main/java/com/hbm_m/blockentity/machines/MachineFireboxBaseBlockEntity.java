package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.MachinePollutingBlockEntity;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.interfaces.IHeatSource;
import com.hbm_m.inventory.fluid.trait.PollutionType;
import com.hbm_m.module.ModuleBurnTime;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityFireboxBase}: Feuerbuechse und Heizofen. Zwei Brennstoffplaetze, Brennzeit und Hitze je Sorte
 * ueber {@link ModuleBurnTime}; die Glut brennt nur herunter, solange der Waermespeicher nicht voll ist. Asche faellt
 * in eine Aschegrube direkt darunter, Russ geht in die Rauchtanks und von dort an die zwoelf Rohranschluesse rund um
 * die 3x3-Grundflaeche. Die Klappe schwingt auf, solange jemand das Menue offen hat.
 */
public abstract class MachineFireboxBaseBlockEntity extends MachinePollutingBlockEntity implements IHeatSource, com.hbm_m.blockentity.IMuffleable {

    /** Original: {@code muffled} aus {@code TileEntityLoadedBase} - der Schalldaempfer ({@code upgrade_muffler}). */
    private boolean muffled = false;

    @Override public boolean isMuffled() { return muffled; }
    @Override public void setMuffled(boolean muffled) { this.muffled = muffled; setChanged(); }

    public static final int INVENTORY_SIZE = 2;

    public int maxBurnTime;
    public int burnTime;
    public int burnHeat;
    public boolean wasOn = false;
    private int playersUsing = 0;

    public float doorAngle = 0;
    public float prevDoorAngle = 0;

    public int heatEnergy;

    /** GUI: Hitze, Hoechsthitze, TU/t, Brennzeit, max. Brennzeit, an. */
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> heatEnergy;
                case 1 -> getMaxHeat();
                case 2 -> burnHeat;
                case 3 -> burnTime;
                case 4 -> maxBurnTime;
                case 5 -> wasOn ? 1 : 0;
                default -> 0;
            };
        }
        @Override public void set(int i, int v) { }
        @Override public int getCount() { return 6; }
    };

    protected MachineFireboxBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, INVENTORY_SIZE, 0L, 0L, 0L, 50);
    }

    public abstract ModuleBurnTime getModule();
    public abstract int getBaseHeat();
    public abstract double getTimeMult();
    public abstract int getMaxHeat();

    public ContainerData getData() { return data; }

    public void openInventory() {
        if (level != null && !level.isClientSide) {
            this.playersUsing++;
            sendUpdateToClient();
        }
    }

    public void closeInventory() {
        if (level != null && !level.isClientSide) {
            this.playersUsing = Math.max(0, this.playersUsing - 1);
            sendUpdateToClient();
        }
    }

    protected Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(HorizontalDirectionalBlock.FACING) ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFireboxBaseBlockEntity be) {
        if (!level.isClientSide) be.serverTick(level, pos);
        else be.clientTick(level, pos);
    }

    protected void serverTick(Level level, BlockPos pos) {

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            Direction rot = dir.getClockWise();
            for (int j = -1; j <= 1; j++) {
                this.sendSmoke(level, pos.relative(dir, 2).relative(rot, j), dir);
            }
        }

        boolean prevOn = wasOn;
        wasOn = false;

        if (burnTime <= 0) {

            for (int i = 0; i < 2; i++) {
                ItemStack slot = inventory.getStackInSlot(i);
                if (!slot.isEmpty()) {

                    int baseTime = getModule().getBurnTime(slot);

                    if (baseTime > 0) {
                        int fuel = (int) (baseTime * getTimeMult());

                        BlockEntity below = level.getBlockEntity(pos.below());

                        if (below instanceof MachineAshpitBlockEntity ashpit) {
                            ashpit.addAsh(getAshFromFuel(slot), baseTime);
                        }

                        this.maxBurnTime = this.burnTime = fuel;
                        this.burnHeat = getModule().getBurnHeat(getBaseHeat(), slot);

                        ItemStack container = slot.hasCraftingRemainingItem() ? slot.getCraftingRemainingItem() : ItemStack.EMPTY;
                        slot.shrink(1);
                        if (slot.isEmpty()) {
                            inventory.setStackInSlot(i, container);
                        }

                        this.wasOn = true;
                        break;
                    }
                }
            }
        } else {

            if (this.heatEnergy < getMaxHeat()) {
                burnTime--;
                if (level.getGameTime() % 20 == 0) this.pollute(PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND * 3);
            }
            this.wasOn = true;

            if (level.random.nextInt(15) == 0 && !this.muffled) {
                level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.0F, 0.5F + level.random.nextFloat() * 0.5F);
            }
        }

        if (wasOn) {
            this.heatEnergy = Math.min(this.heatEnergy + this.burnHeat, getMaxHeat());
        } else {
            this.heatEnergy = Math.max(this.heatEnergy - Math.max(this.heatEnergy / 1000, 1), 0);
            this.burnHeat = 0;
        }

        setChanged();
        if (prevOn != wasOn) sendUpdateToClient();
    }

    protected void clientTick(Level level, BlockPos pos) {
        this.prevDoorAngle = this.doorAngle;
        float swingSpeed = (doorAngle / 10F) + 3;

        if (this.playersUsing > 0) {
            this.doorAngle += swingSpeed;
        } else {
            this.doorAngle -= swingSpeed;
        }

        this.doorAngle = Mth.clamp(this.doorAngle, 0F, 135F);

        if (wasOn && level.getGameTime() % 5 == 0) {
            Direction dir = getFacing();
            double x = pos.getX() + 0.5 + dir.getStepX();
            double y = pos.getY() + 0.25;
            double z = pos.getZ() + 0.5 + dir.getStepZ();
            level.addParticle(ParticleTypes.FLAME, x + level.random.nextDouble() * 0.5 - 0.25, y + level.random.nextDouble() * 0.25, z + level.random.nextDouble() * 0.5 - 0.25, 0, 0, 0);
        }
    }

    /**
     * Original {@code getAshFromFuel}: Oredict-Namen (Coke/Coal/Lignite -> Kohle, log*, *Wood*, *Sapling* -> Holz); im
     * Port ueber Registriernamen und die entsprechenden Tags.
     */
    public static MachineAshpitBlockEntity.AshType getAshFromFuel(ItemStack stack) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();

        if (id.contains("coke") || id.contains("coal") || id.contains("lignite") || stack.is(net.minecraft.tags.ItemTags.COALS)) {
            return MachineAshpitBlockEntity.AshType.COAL;
        }
        if (id.contains("log") || id.contains("wood") || id.contains("plank") || id.contains("sapling")
                || stack.is(net.minecraft.tags.ItemTags.LOGS) || stack.is(net.minecraft.tags.ItemTags.PLANKS)
                || stack.is(net.minecraft.tags.ItemTags.SAPLINGS) || stack.is(net.minecraft.tags.ItemTags.WOODEN_SLABS)
                || stack.is(net.minecraft.tags.ItemTags.WOODEN_STAIRS) || stack.is(net.minecraft.world.item.Items.STICK)) {
            return MachineAshpitBlockEntity.AshType.WOOD;
        }
        return MachineAshpitBlockEntity.AshType.MISC;
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return getModule().getBurnTime(stack) > 0;
    }

    public boolean isBurning() { return wasOn; }
    public int getBurnTime() { return burnTime; }
    public int getMaxBurnTime() { return maxBurnTime; }

    @Override public int getHeatStored() { return heatEnergy; }
    @Override public int getMaxHeatStored() { return getMaxHeat(); }

    @Override
    public void useUpHeat(int heat) {
        this.heatEnergy = Math.max(0, this.heatEnergy - heat);
    }

    @Override
    public boolean canConnect(net.minecraft.world.level.material.Fluid fluid, Direction dir) {
        // Original: alle Seiten ausser unten
        return dir != null && dir != Direction.DOWN;
    }

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 1, worldPosition.getZ() + 2);
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putInt("maxBurnTime", maxBurnTime);
        nbt.putInt("burnTime", burnTime);
        nbt.putInt("burnHeat", burnHeat);
        nbt.putInt("heatEnergy", heatEnergy);
        nbt.putInt("playersUsing", playersUsing);
        nbt.putBoolean("wasOn", wasOn);
        nbt.putBoolean("muffled", muffled);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        this.maxBurnTime = nbt.getInt("maxBurnTime");
        this.burnTime = nbt.getInt("burnTime");
        this.burnHeat = nbt.getInt("burnHeat");
        this.heatEnergy = nbt.getInt("heatEnergy");
        // Nur Sync an den Client (Original: nur im Netzpaket, nicht im Spielstand)
        if (level != null && level.isClientSide()) this.playersUsing = nbt.getInt("playersUsing");
        this.wasOn = nbt.getBoolean("wasOn");
        this.muffled = nbt.getBoolean("muffled");
    }

    //? if forge {
    /** Original {@code ISidedInventory}: Slots {0, 1}; nur Brennbares hinein, nichts heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return new int[] { 0, 1 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return false; }
            });

    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@org.jetbrains.annotations.NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sidedItems.invalidate();
    }
    //?}
}
