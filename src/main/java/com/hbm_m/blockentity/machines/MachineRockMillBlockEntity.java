package com.hbm_m.blockentity.machines;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineRockMillMenu;
import com.hbm_m.module.machine.MachineModuleRockMill;
import com.hbm_m.recipe.RockMillRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineRockMill}: 8 Slots (Batterie, Blueprint-Ordner, drei Eingaenge, drei Ausgaenge),
 * je ein Ein- und Ausgangstank (4.000 mB, waechst mit dem Rezept), Rezept per Rezeptwaehler. Der Energiespeicher
 * waechst mit dem Rezept ({@code power * 100}, mindestens 2.500). Acht Anschluesse an den Seiten des 5x5-Sockels.
 * Clientseitig laeuft das Mahlrad an und aus, das Geruest erscheint, sobald ueber der Muehle ein Block sitzt, und
 * beim Mahlen spritzt Staub des gemahlenen Blocks.
 */
public class MachineRockMillBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_BLUEPRINT = 1;
    public static final int ITEM_INPUT_START = 2;
    public static final int ITEM_OUTPUT_START = 5;
    public static final int SLOT_COUNT = 8;

    public static final float ACCELERATION = 0.1F;
    public static final float MAX_SPEED = 15F;

    public final FluidTank[] inputTanks = new FluidTank[1];
    public final FluidTank[] outputTanks = new FluidTank[1];

    public boolean didProcess = false;

    public float rotation;
    public float prevRotation;
    public float rotationSpeed = 0F;
    public boolean frame = false;

    public final MachineModuleRockMill rockMillModule;

    public MachineRockMillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROCKMILL_BE.get(), pos, state, SLOT_COUNT, 2_500L, 2_500L);
        inputTanks[0] = new FluidTank(ModFluids.NONE.getSource(), 4_000);
        outputTanks[0] = new FluidTank(ModFluids.NONE.getSource(), 4_000);

        this.rockMillModule = new MachineModuleRockMill(this, inventory,
                new int[] { 2, 3, 4 }, new int[] { 5, 6, 7 },
                inputTanks, outputTanks, null);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineRockMillBlockEntity be) {
        be.rockMillModule.setLevel(level);
        if (level instanceof ServerLevel serverLevel) be.serverTick(serverLevel);
        else be.clientTick(level);
    }

    private record DirPos(BlockPos pos, Direction dir) { }

    /** Original {@code getPorts}: je zwei Anschluesse pro Sockelseite; verbunden wird mit dem Nachbarn davor. */
    private DirPos[] getConPos() {
        BlockPos p = worldPosition;
        return new DirPos[] {
                new DirPos(p.offset(3, 0, 1), Direction.EAST),
                new DirPos(p.offset(3, 0, -1), Direction.EAST),
                new DirPos(p.offset(-3, 0, 1), Direction.WEST),
                new DirPos(p.offset(-3, 0, -1), Direction.WEST),
                new DirPos(p.offset(1, 0, 3), Direction.SOUTH),
                new DirPos(p.offset(-1, 0, 3), Direction.SOUTH),
                new DirPos(p.offset(1, 0, -3), Direction.NORTH),
                new DirPos(p.offset(-1, 0, -3), Direction.NORTH),
        };
    }

    /** Der gemahlene Block (Rezept-Icon = erste Zutat), fuer Staub und Geraeusch; sonst Kies. */
    private net.minecraft.world.level.block.state.BlockState groundBlock() {
        RockMillRecipe recipe = rockMillModule.peekRecipe();
        if (recipe != null && recipe.getResultItemSafe().getItem() instanceof net.minecraft.world.item.BlockItem bi) {
            return bi.getBlock().defaultBlockState();
        }
        return net.minecraft.world.level.block.Blocks.GRAVEL.defaultBlockState();
    }

    private void serverTick(ServerLevel world) {

        RockMillRecipe recipe = rockMillModule.peekRecipe();
        long maxPower = 2_500L;
        if (recipe != null) maxPower = (long) recipe.getPowerConsumption() * 100L;
        maxPower = Math.max(Math.max(energy, maxPower), 2_500L);
        if (maxPower != getMaxEnergyStored()) setEnergyCapacity(maxPower);

        chargeFromBatterySlot(SLOT_BATTERY);

        for (DirPos con : getConPos()) {
            this.trySubscribe(world, con.pos.getX(), con.pos.getY(), con.pos.getZ(), con.dir);
            for (FluidTank tank : inputTanks) if (tank.getTankType() != ModFluids.NONE.getSource()) this.trySubscribe(tank.getTankType(), world, con.pos, con.dir);
            for (FluidTank tank : outputTanks) if (tank.getFill() > 0) this.tryProvide(tank, world, con.pos, con.dir);
        }

        boolean dirty = this.rockMillModule.updateAndGetDirty(1D, 1D, true, inventory.getStackInSlot(SLOT_BLUEPRINT));
        this.didProcess = this.rockMillModule.getDidProcess();
        if (dirty) setChanged();

        // Original: alle 3 Ticks das Schrittgeraeusch des gemahlenen Blocks
        if (this.didProcess && (world.getGameTime() + worldPosition.asLong()) % 3 == 0) {
            var sound = groundBlock().getSoundType().getStepSound();
            world.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5,
                    sound, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.75F);
        }

        sendUpdateToClient();
    }

    private void clientTick(Level world) {
        this.prevRotation = this.rotation;
        this.rotationSpeed += ACCELERATION * (this.didProcess ? 1 : -1);
        this.rotationSpeed = net.minecraft.util.Mth.clamp(this.rotationSpeed, 0F, MAX_SPEED);
        this.rotation += this.rotationSpeed;
        if (this.rotation >= 360F) {
            this.prevRotation -= 360F;
            this.rotation -= 360F;
        }

        if (world.getGameTime() % 20 == 0) {
            frame = !world.getBlockState(worldPosition.above(3)).isAir();
        }

        if (this.didProcess) {
            double angle = world.random.nextDouble() * Math.PI * 2D;
            double vx = Math.cos(angle), vz = Math.sin(angle);
            double speed = 0.125D;
            world.addParticle(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, groundBlock()),
                    worldPosition.getX() + 0.5 + vx * 2.25, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5 + vz * 2.25,
                    vx * speed, -0.1D, vz * speed);
        }
    }

    // ==================== Rezeptwahl ====================

    @Nullable
    public ResourceLocation getSelectedRecipeId() {
        return rockMillModule.getSelectedRecipeId();
    }

    @Nullable
    public RockMillRecipe getSelectedRecipe() {
        return rockMillModule.peekRecipe();
    }

    public void setSelectedRecipe(@Nullable ResourceLocation recipeId) {
        rockMillModule.setSelectedRecipe(recipeId);
        if (level != null && !level.isClientSide) rockMillModule.syncTankConfigurationToRecipe(level);
        setChanged();
        if (level != null && !level.isClientSide) sendUpdateToClient();
    }

    public ItemStack getBlueprintFolder() {
        return inventory.getStackInSlot(SLOT_BLUEPRINT);
    }

    /** Original {@code GUIScreenRecipeSelector}: alle Rezepte, deren Pool frei oder im Ordner installiert ist. */
    public List<RockMillRecipe> getAvailableRecipes() {
        if (level == null) return List.of();
        String installedPool = com.hbm_m.item.industrial.ItemBlueprints.getBlueprintPool(getBlueprintFolder());
        return com.hbm_m.recipe.index.ModRecipeIndex.of(level.getRecipeManager()).getAll(RockMillRecipe.Type.INSTANCE).stream().filter(r -> {
            String pool = r.getBlueprintPool();
            if (pool == null || pool.isEmpty()) return true;
            return installedPool != null && !installedPool.isEmpty() && installedPool.equals(pool);
        }).toList();
    }

    public double getProgressFraction() {
        return rockMillModule.getProgressPercent();
    }

    // ==================== Slots ====================

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return true;
        if (slot == SLOT_BLUEPRINT && stack.getItem() instanceof com.hbm_m.item.industrial.ItemBlueprints) return true;
        return this.rockMillModule != null && this.rockMillModule.isItemValidForSlot(slot, stack);
    }

    // ==================== Fluid ====================

    @Override public FluidTank[] getReceivingTanks() { return inputTanks; }
    @Override public FluidTank[] getSendingTanks() { return outputTanks; }
    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { inputTanks[0], outputTanks[0] }; }

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
        inputTanks[0].writeToNBT(tag, "i");
        outputTanks[0].writeToNBT(tag, "o");
        tag.putLong("power", energy);
        tag.putLong("maxPower", getMaxEnergyStored());
        tag.putBoolean("didProcess", didProcess);
        rockMillModule.writeNBT(tag);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        inputTanks[0].readFromNBT(tag, "i");
        outputTanks[0].readFromNBT(tag, "o");
        if (tag.contains("maxPower")) setEnergyCapacity(Math.max(1, tag.getLong("maxPower")));
        if (tag.contains("power")) energy = tag.getLong("power");
        rockMillModule.readNBT(tag);
        didProcess = tag.getBoolean("didProcess");
        rockMillModule.didProcess = didProcess;
    }

    // ==================== Sonstiges ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_rockmill");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineRockMillMenu(containerId, playerInventory, this);
    }

    /** Original: 5x3x5 um den Kern. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ() - 2,
                worldPosition.getX() + 3, worldPosition.getY() + 3, worldPosition.getZ() + 3);
    }

    //? if forge {
    /** Original {@code ISidedInventory}: Slots 2-7; Zutaten nach Rezept hinein, Ausgaben 5-7 und verstopfte Eingaenge heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return com.hbm_m.blockentity.SidedItemAccess.range(2, 7); }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot >= 5 || rockMillModule.isSlotClogged(slot); }
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
