package com.hbm_m.blockentity.machines;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachinePrecAssMenu;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.module.machine.MachineModulePrecAss;
import com.hbm_m.recipe.PrecAssRecipe;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachinePrecAss} ("horribly copy-pasted crap device"): Praezisionsassembler. 22 Slots (Batterie,
 * Bauplanordner, zwei Upgrades, 9 Eingaenge, 9 Ausgaenge), je ein Ein-/Ausgangstank, Speicher {@code power * 100} des
 * Rezepts (mindestens 100.000). Zwoelf Anschluesse um den Sockel. Clientseitig drehen sich Ring und vier Arme, die
 * Schlagbolzen klopfen der Reihe nach.
 */
public class MachinePrecAssBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2,
        com.hbm_m.interfaces.IUpgradeInfoProvider {

    public static final int SLOT_COUNT = 22;

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = new EnumMap<>(UpgradeType.class);
    static {
        VALID_UPGRADES.put(UpgradeType.SPEED, 3);
        VALID_UPGRADES.put(UpgradeType.POWER, 3);
        VALID_UPGRADES.put(UpgradeType.OVERDRIVE, 3);
    }

    public final FluidTank inputTank = new FluidTank(ModFluids.NONE.getSource(), 4_000);
    public final FluidTank outputTank = new FluidTank(ModFluids.NONE.getSource(), 4_000);

    public boolean didProcess = false;
    public boolean frame = false;

    public final MachineModulePrecAss assemblerModule;
    public final UpgradeManager upgradeManager = new UpgradeManager();

    // Animation (Client)
    public double prevRing;
    public double ring;
    public double ringSpeed;
    public double ringTarget;
    public int ringDelay;

    public double[] armAngles = new double[] { 45, -15, -5 };
    public double[] prevArmAngles = new double[] { 45, -15, -5 };
    public double[] strikers = new double[4];
    public double[] prevStrikers = new double[4];
    public boolean[] strikerDir = new boolean[4];
    protected int strikerIndex;
    protected int strikerDelay;

    public static final double[] NULL_POSITION = new double[] { 45, -30, 45 };
    public static final double[] WORKING_POSITION = new double[] { 45, -15, -5 };

    public MachinePrecAssBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRECASS_BE.get(), pos, state, SLOT_COUNT, 100_000L, 100_000L);
        this.assemblerModule = new MachineModulePrecAss(this, inventory, inputTank, outputTank, null);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachinePrecAssBlockEntity be) {
        be.assemblerModule.setLevel(level);
        if (level instanceof ServerLevel server) be.serverTick(server);
        else be.clientTick(level, pos);
    }

    private record DirPos(BlockPos pos, Direction dir) { }

    private DirPos[] getConPos() {
        BlockPos p = worldPosition;
        return new DirPos[] {
                new DirPos(p.offset(2, 0, -1), Direction.EAST), new DirPos(p.offset(2, 0, 0), Direction.EAST), new DirPos(p.offset(2, 0, 1), Direction.EAST),
                new DirPos(p.offset(-2, 0, -1), Direction.WEST), new DirPos(p.offset(-2, 0, 0), Direction.WEST), new DirPos(p.offset(-2, 0, 1), Direction.WEST),
                new DirPos(p.offset(-1, 0, 2), Direction.SOUTH), new DirPos(p.offset(0, 0, 2), Direction.SOUTH), new DirPos(p.offset(1, 0, 2), Direction.SOUTH),
                new DirPos(p.offset(-1, 0, -2), Direction.NORTH), new DirPos(p.offset(0, 0, -2), Direction.NORTH), new DirPos(p.offset(1, 0, -2), Direction.NORTH),
        };
    }

    private void serverTick(ServerLevel world) {
        PrecAssRecipe recipe = assemblerModule.peekRecipe();
        long maxPower = recipe != null ? recipe.getPower() * 100 : getMaxEnergyStored();
        maxPower = Math.max(Math.max(energy, maxPower), 100_000L);
        if (maxPower != getMaxEnergyStored()) setEnergyCapacity(maxPower);

        chargeFromBatterySlot(0);
        upgradeManager.checkSlots(inventory, 2, 3, VALID_UPGRADES);

        for (DirPos con : getConPos()) {
            this.trySubscribe(world, con.pos.getX(), con.pos.getY(), con.pos.getZ(), con.dir);
            if (inputTank.getTankType() != ModFluids.NONE.getSource()) this.trySubscribe(inputTank.getTankType(), world, con.pos, con.dir);
            if (outputTank.getFill() > 0) this.tryProvide(outputTank, world, con.pos, con.dir);
        }

        double speed = 1D;
        double pow = 1D;

        speed += Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3) / 3D;
        speed += Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);

        pow -= Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3) * 0.25D;
        pow += Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3) * 1D;
        pow += Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3) * 10D / 3D;

        this.assemblerModule.updatePrecAss(speed, pow, true, inventory.getStackInSlot(1));
        this.didProcess = this.assemblerModule.didProcess;
        if (this.assemblerModule.needsSync) setChanged();

        // networkPackNT(100)
        sendUpdateToClient();
    }

    private void clientTick(Level world, BlockPos pos) {
        if (world.getGameTime() % 20 == 0) {
            frame = !world.getBlockState(pos.above(3)).isAir();
        }

        com.hbm_m.client.sound.MachineLoopSoundClient.tick(this, "hbm:block.motor", this.didProcess, 0.5F, 0.75F, 50);

        System.arraycopy(armAngles, 0, prevArmAngles, 0, 3);
        System.arraycopy(strikers, 0, prevStrikers, 0, 4);
        this.prevRing = this.ring;

        for (int i = 0; i < 4; i++) {
            if (this.strikerDir[i]) {
                this.strikers[i] = -0.75D;
                this.strikerDir[i] = false;
                playClient(world, pos, "hbm:block.assemblerStrike", 0.5F, 1.25F);
            } else {
                this.strikers[i] = Mth.clamp(this.strikers[i] + 0.5D, -0.75D, 0D);
            }
        }

        if (this.ring != this.ringTarget) {
            double ringDelta = Math.abs(this.ringTarget - this.ring);
            if (ringDelta <= this.ringSpeed) this.ring = this.ringTarget;
            if (this.ringTarget > this.ring) this.ring += this.ringSpeed;
            if (this.ringTarget < this.ring) this.ring -= this.ringSpeed;
            if (this.ringTarget == this.ring) {
                double sub = ringTarget >= 360 ? -360D : 360D;
                this.ringTarget += sub;
                this.ring += sub;
                this.prevRing += sub;
                this.ringDelay = 100 + world.random.nextInt(21);
            }
        }

        if (didProcess) {
            if (this.ring == this.ringTarget) {
                if (this.ringDelay > 0) this.ringDelay--;
                if (this.ringDelay <= 0) {
                    this.ringTarget += 45 * (world.random.nextBoolean() ? -1 : 1);
                    this.ringSpeed = 10D + world.random.nextDouble() * 5D;
                    playClient(world, pos, "hbm:block.assemblerStart", 0.25F, 1.25F + world.random.nextFloat() * 0.25F);
                }
            }

            if (!isInWorkingPosition(this.armAngles) && canArmsMove()) move(WORKING_POSITION);

            if (isInWorkingPosition(this.armAngles)) {
                this.strikerDelay--;
                if (this.strikerDelay <= 0) {
                    this.strikerDir[this.strikerIndex] = true;
                    this.strikerIndex = (this.strikerIndex + 1) % this.strikers.length;
                    this.strikerDelay = this.strikerIndex == 3 ? (10 + world.random.nextInt(3)) : 2;
                }
            }
        } else {
            for (int i = 0; i < 4; i++) this.strikerDir[i] = false; // set all strikers to retract
            if (canArmsMove()) move(NULL_POSITION);
        }

        if (this.isInWorkingPosition(prevArmAngles) && !this.isInWorkingPosition(armAngles)) {
            playClient(world, pos, "hbm:block.assemblerStop", 0.25F, 1.25F + world.random.nextFloat() * 0.25F);
        }
    }

    private static void playClient(Level world, BlockPos pos, String key, float volume, float pitch) {
        var ev = HbmSoundsNT.get(key);
        if (ev != null) world.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), ev, SoundSource.BLOCKS, volume, pitch, false);
    }

    private boolean canArmsMove() {
        for (int i = 0; i < 4; i++) if (this.strikers[i] != 0) return false;
        return true;
    }

    private static boolean isInWorkingPosition(double[] arms) {
        for (int i = 0; i < 3; i++) if (arms[i] != WORKING_POSITION[i]) return false;
        return true;
    }

    private void move(double[] targetAngles) {
        for (int i = 0; i < armAngles.length; i++) {
            if (armAngles[i] == targetAngles[i]) continue;
            double angle = armAngles[i];
            double target = targetAngles[i];
            double turn = 15D;
            double delta = Math.abs(angle - target);

            if (delta <= turn) { armAngles[i] = targetAngles[i]; continue; }
            if (angle < target) armAngles[i] += turn;
            else armAngles[i] -= turn;
        }
    }

    // ==================== Rezeptwahl ====================

    @Nullable public ResourceLocation getSelectedRecipeId() { return assemblerModule.getSelectedRecipeId(); }
    @Nullable public PrecAssRecipe getSelectedRecipe() { return assemblerModule.peekRecipe(); }

    public void setSelectedRecipe(@Nullable ResourceLocation recipeId) {
        assemblerModule.setSelectedRecipe(recipeId);
        setChanged();
        if (level != null && !level.isClientSide) sendUpdateToClient();
    }

    public ItemStack getBlueprintFolder() {
        return inventory.getStackInSlot(1);
    }

    public List<PrecAssRecipe> getAvailableRecipes() {
        if (level == null) return List.of();
        String installedPool = com.hbm_m.item.industrial.ItemBlueprints.getBlueprintPool(getBlueprintFolder());
        return com.hbm_m.recipe.index.ModRecipeIndex.of(level.getRecipeManager()).getAll(PrecAssRecipe.Type.INSTANCE).stream().filter(r -> {
            String pool = r.getBlueprintPool();
            if (pool == null || pool.isEmpty()) return true;
            return installedPool != null && !installedPool.isEmpty() && installedPool.equals(pool);
        }).toList();
    }

    public double getProgressFraction() {
        return assemblerModule.progressFrac;
    }

    // ==================== Slots ====================

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return true; // battery
        if (slot == 1 && stack.getItem() instanceof com.hbm_m.item.industrial.ItemBlueprints) return true;
        if (slot >= 2 && slot <= 3 && stack.getItem() instanceof ItemMachineUpgrade) return true; // upgrades
        return this.assemblerModule != null && this.assemblerModule.isItemValidForSlot(slot, stack); // recipe input crap
    }

    // ==================== Fluid ====================

    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { inputTank }; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { outputTank }; }
    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { inputTank, outputTank }; }

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
        inputTank.writeToNBT(tag, "i");
        outputTank.writeToNBT(tag, "o");
        tag.putLong("power", energy);
        tag.putLong("maxPower", getMaxEnergyStored());
        tag.putBoolean("didProcess", didProcess);
        assemblerModule.writeNBT(tag);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        inputTank.readFromNBT(tag, "i");
        outputTank.readFromNBT(tag, "o");
        if (tag.contains("maxPower")) setEnergyCapacity(Math.max(1, tag.getLong("maxPower")));
        if (tag.contains("power")) energy = tag.getLong("power");
        assemblerModule.readNBT(tag);
        didProcess = tag.getBoolean("didProcess");
        assemblerModule.didProcess = didProcess;
    }

    // ==================== Upgrades ====================

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(com.hbm_m.interfaces.IUpgradeInfoProvider.getStandardLabel(getBlockState().getBlock()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_SPEED, "+" + (level * 100 / 3) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 50) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (level * 25) + "%").withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle(System.currentTimeMillis() % 1000 < 500 ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public Map<UpgradeType, Integer> getValidUpgrades() {
        return VALID_UPGRADES;
    }

    // ==================== Sonstiges ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_precass");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachinePrecAssMenu(containerId, playerInventory, this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 3, worldPosition.getZ() + 2);
    }

    //? if forge {
    /** Original {@code ISidedInventory}: Slots {4-21}; Zutaten nach Rezept hinein, Ausgaben ab 13 und verstopfte Eingaenge heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return com.hbm_m.blockentity.SidedItemAccess.range(4, 21); }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot >= 13 || assemblerModule.isSlotClogged(slot); }
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
    //?} elif neoforge {
    /*/^* Original {@code ISidedInventory}: Slots {4-21}; Zutaten nach Rezept hinein, Ausgaben ab 13 und verstopfte Eingaenge heraus. ^/
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return com.hbm_m.blockentity.SidedItemAccess.range(4, 21); }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot >= 13 || assemblerModule.isSlotClogged(slot); }
            });

    @Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getHbmCapability(cap, side);
    }

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        sidedItems.invalidate();
    }
    *///?}
}
