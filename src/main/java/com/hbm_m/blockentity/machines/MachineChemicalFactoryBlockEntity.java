package com.hbm_m.blockentity.machines;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IFluidPartDelegateProvider;
import com.hbm_m.interfaces.IUpgradeInfoProvider;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineChemicalFactoryMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.industrial.ItemBlueprints;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.module.machine.MachineModuleChemplant;
import com.hbm_m.recipe.ChemicalPlantRecipe;
import com.hbm_m.sound.ClientSoundBootstrap;
import com.hbm_m.util.BobMathUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineChemicalFactory}: vier Chemiewerk-Module ({@link MachineModuleChemplant}) mit je
 * eigener Rezeptwahl und Bauplanmappe, gemeinsamer Kuehlkreis (Wasser -&gt; Abdampf, 100 mB je arbeitendem Modul).
 *
 * <p>Slots wie im Original: 0 Batterie, 1-3 Upgrades, je Modul i: {@code 4+i*7} Bauplanmappe,
 * {@code 5..7+i*7} Eingaenge, {@code 8..10+i*7} Ausgaenge. Die Kuehlleitungen (Original-Proxies mit
 * {@code DelegateChemicalFactoy}) reichen nur Wasser/Abdampf durch, die uebrigen Anschluesse nur die Rezepttanks;
 * die vier Item-Anschluesse ({@code getIOPos}) speisen jeweils nur ihr eigenes Modul.</p>
 */
public class MachineChemicalFactoryBlockEntity extends BaseMachineBlockEntity
        implements IFluidStandardTransceiverMK2, IUpgradeInfoProvider, IFluidPartDelegateProvider,
        com.hbm_m.api.redstoneoverradio.IRORValueProvider
        //? if forge {
        , com.hbm_m.interfaces.IConditionalInvAccess
        //?}
{

    private static final String CHEMICAL_PLANT_SOUND_INSTANCE = "com.hbm_m.sound.ChemicalPlantSoundInstance";

    public static final int LANE_COUNT = 4;
    public static final int SLOT_COUNT = 32;
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_UPGRADE_START = 1;
    public static final int SLOT_UPGRADE_END = 3;

    public static int blueprintSlot(int lane) { return 4 + lane * 7; }
    public static int inputSlot(int lane, int i) { return 5 + lane * 7 + i; }
    public static int outputSlot(int lane, int i) { return 8 + lane * 7 + i; }

    /** Original {@code getAccessibleSlotsFromSide(side)}. */
    private static final int[] ACCESSIBLE = {
            5, 6, 7, 8, 9, 10,
            12, 13, 14, 15, 16, 17,
            19, 20, 21, 22, 23, 24,
            26, 27, 28, 29, 30, 31
    };

    private static final long BASE_MAX_POWER = 1_000_000L;

    public final FluidTank[] inputTanks = new FluidTank[12];
    public final FluidTank[] outputTanks = new FluidTank[12];
    public final FluidTank water;
    public final FluidTank lps;
    private final FluidTank[] allTanks;
    private boolean tanksDirty = false;

    private final MachineModuleChemplant[] chemplantModule = new MachineModuleChemplant[LANE_COUNT];
    public final boolean[] didProcess = new boolean[LANE_COUNT];
    private final UpgradeManager upgradeManager = new UpgradeManager();

    private static final java.util.Map<UpgradeType, Integer> VALID_UPGRADES = java.util.Map.of(
            UpgradeType.SPEED, 3,
            UpgradeType.POWER, 3,
            UpgradeType.OVERDRIVE, 3
    );

    private final DelegateChemicalFactory delegate = new DelegateChemicalFactory();

    public int anim;
    public int prevAnim;
    public boolean frame = false;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= 0 && index < LANE_COUNT) {
                return chemplantModule[index].getProgressInt();
            }
            if (index >= LANE_COUNT && index < LANE_COUNT * 2) {
                return chemplantModule[index - LANE_COUNT].getMaxProgress();
            }
            return switch (index - LANE_COUNT * 2) {
                case 0 -> (int) (getEnergyStored() & 0xFFFFFFFFL);
                case 1 -> (int) ((getEnergyStored() >> 32) & 0xFFFFFFFFL);
                case 2 -> (int) (getMaxEnergyStored() & 0xFFFFFFFFL);
                case 3 -> (int) ((getMaxEnergyStored() >> 32) & 0xFFFFFFFFL);
                case 4 -> (didProcess[0] ? 1 : 0) | (didProcess[1] ? 2 : 0) | (didProcess[2] ? 4 : 0) | (didProcess[3] ? 8 : 0);
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return LANE_COUNT * 2 + 5; }
    };

    public MachineChemicalFactoryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEMICAL_FACTORY_BE.get(), pos, state, SLOT_COUNT, BASE_MAX_POWER, BASE_MAX_POWER);

        for (int i = 0; i < 12; i++) {
            inputTanks[i] = new FluidTank(24_000) {
                @Override public void onContentsChanged() { setChanged(); tanksDirty = true; }
            };
            outputTanks[i] = new FluidTank(24_000) {
                @Override public void onContentsChanged() { setChanged(); tanksDirty = true; }
            };
        }
        water = new FluidTank(Fluids.WATER, 4_000) {
            @Override public void onContentsChanged() { setChanged(); tanksDirty = true; }
        };
        lps = new FluidTank(ModFluids.SPENTSTEAM.getSource(), 4_000) {
            @Override public void onContentsChanged() { setChanged(); tanksDirty = true; }
        };

        allTanks = new FluidTank[inputTanks.length + outputTanks.length + 2];
        System.arraycopy(inputTanks, 0, allTanks, 0, inputTanks.length);
        System.arraycopy(outputTanks, 0, allTanks, inputTanks.length, outputTanks.length);
        allTanks[allTanks.length - 2] = water;
        allTanks[allTanks.length - 1] = lps;

        for (int i = 0; i < LANE_COUNT; i++) {
            chemplantModule[i] = new MachineModuleChemplant(i, this, inventory,
                    new int[] { inputSlot(i, 0), inputSlot(i, 1), inputSlot(i, 2) },
                    new int[] { outputSlot(i, 0), outputSlot(i, 1), outputSlot(i, 2) },
                    new FluidTank[] { inputTanks[i * 3], inputTanks[1 + i * 3], inputTanks[2 + i * 3] },
                    new FluidTank[] { outputTanks[i * 3], outputTanks[1 + i * 3], outputTanks[2 + i * 3] },
                    null);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineChemicalFactoryBlockEntity be) {
        for (MachineModuleChemplant m : be.chemplantModule) m.setLevel(level);

        if (level.isClientSide) {
            be.prevAnim = be.anim;
            if (be.isAnyProcessing()) be.anim++;

            if (level.getGameTime() % 20 == 0) {
                be.frame = !level.getBlockState(pos.above(3)).isAir();
            }
            be.clientTick();
            return;
        }

        be.ensureNetworkInitialized();

        long nextMaxPower = 0;
        for (int i = 0; i < LANE_COUNT; i++) {
            ChemicalPlantRecipe recipe = be.chemplantModule[i].peekRecipe(level);
            if (recipe != null) nextMaxPower += recipe.getPowerConsumption() * 100L;
        }
        long maxPower = BobMathUtil.max(be.getEnergyStored(), nextMaxPower, BASE_MAX_POWER);
        if (maxPower != be.getMaxEnergyStored()) be.setEnergyCapacity(maxPower);

        be.chargeFromBattery();
        be.upgradeManager.checkSlots(be.inventory, SLOT_UPGRADE_START, SLOT_UPGRADE_END, VALID_UPGRADES);

        if (level.getGameTime() % 10L == 0L) {
            be.updateEnergyDelta(be.getEnergyStored());
        }

        double speed = 1D;
        double pow = 1D;

        speed += Math.min(be.upgradeManager.getLevel(UpgradeType.SPEED), 3) / 3D;
        speed += Math.min(be.upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);

        pow -= Math.min(be.upgradeManager.getLevel(UpgradeType.POWER), 3) * 0.25D;
        pow += Math.min(be.upgradeManager.getLevel(UpgradeType.SPEED), 3) * 1D;
        pow += Math.min(be.upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3) * 10D / 3D;
        boolean markDirty = false;

        for (int i = 0; i < LANE_COUNT; i++) {
            boolean was = be.didProcess[i];
            markDirty |= be.chemplantModule[i].updateAndGetDirty(speed * 2D, pow * 2D, be.canCool(),
                    be.inventory.getStackInSlot(blueprintSlot(i)));
            be.didProcess[i] = be.chemplantModule[i].getDidProcess();
            if (was != be.didProcess[i]) markDirty = true;

            if (be.didProcess[i]) {
                be.water.drainMb(100);
                be.lps.fillMb(be.lps.getConfiguredFluid(), 100);
            }
        }

        // internal fluid sharing logic
        for (FluidTank in : be.inputTanks) {
            if (!FluidTank.isFluidTypeExplicitlySet(in.getTankType())) continue;
            for (FluidTank out : be.outputTanks) {
                if (!FluidTank.isFluidTypeExplicitlySet(out.getTankType())) continue;
                if (out.getTankType() != in.getTankType()) continue;
                if (out.getPressure() != in.getPressure()) continue;
                int toMove = BobMathUtil.min(in.getCapacityMb() - in.getFluidAmountMb(), out.getFluidAmountMb(), 50);
                if (toMove > 0) {
                    in.fillMb(in.getTankType(), toMove);
                    out.drainMb(toMove);
                }
            }
        }

        if (be.tanksDirty) {
            markDirty = true;
            be.tanksDirty = false;
        }

        if (markDirty) {
            be.setChanged();
            be.sendUpdateToClient();
        }
    }

    public boolean canCool() {
        return water.getFluidAmountMb() >= 100 && lps.getFluidAmountMb() <= lps.getCapacityMb() - 100;
    }

    private void chargeFromBattery() {
        ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (!stack.isEmpty() && stack.getItem() instanceof ItemCreativeBattery) {
            setEnergyStored(getMaxEnergyStored());
            return;
        }
        chargeFromBatterySlot(SLOT_BATTERY);
    }

    //? if forge {
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    //?}
    private void clientTick() {
        // Original: CHEMPLANT_LOOP, solange ein Modul arbeitet
        ClientSoundBootstrap.updateSound(this, isAnyProcessing(), this::newSoundInstance);
    }

    private Object newSoundInstance() {
        try {
            return Class.forName(CHEMICAL_PLANT_SOUND_INSTANCE).getConstructor(BlockPos.class).newInstance(this.getBlockPos());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            ClientSoundBootstrap.updateSound(this, false, null);
        }
    }

    // ── Richtungen (Original: dir = meta - 10, rot = dir.getRotation(UP)) ─────────────

    private Direction getDir() {
        BlockState state = getBlockState();
        return state.hasProperty(HorizontalDirectionalBlock.FACING) ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
    }

    /** Original {@code coolantLine}: die vier Proxies, hinter denen nur der Kuehlkreis liegt. */
    private BlockPos[] coolantLine() {
        Direction dir = getDir();
        Direction rot = dir.getClockWise();
        BlockPos c = worldPosition;
        return new BlockPos[] {
                c.relative(rot, 1).relative(dir, 2),
                c.relative(rot, -1).relative(dir, 2),
                c.relative(rot, 1).relative(dir, -2),
                c.relative(rot, -1).relative(dir, -2),
        };
    }

    /** Original {@code getIOPos}, zurueckgerechnet auf die Proxyzelle davor (Index = Modul). */
    private BlockPos[] ioParts() {
        Direction dir = getDir();
        Direction rot = dir.getClockWise();
        BlockPos c = worldPosition;
        return new BlockPos[] {
                c.relative(dir, 1).relative(rot, 2),
                c.relative(dir, -1).relative(rot, 2),
                c.relative(dir, 1).relative(rot, -2),
                c.relative(dir, -1).relative(rot, -2),
        };
    }

    @Override
    @Nullable
    public Object getFluidDelegateForPart(BlockPos partPos) {
        for (BlockPos p : coolantLine()) if (p.equals(partPos)) return delegate;
        return null;
    }

    /** Original {@code DelegateChemicalFactoy}: an den Kuehlleitungen nur Wasser rein, Abdampf raus. */
    public class DelegateChemicalFactory implements IFluidStandardTransceiverMK2 {
        @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { water }; }
        @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { lps }; }
        @Override public FluidTank[] getAllTanks() { return MachineChemicalFactoryBlockEntity.this.getAllTanks(); }
        @Override public boolean isLoaded() { return MachineChemicalFactoryBlockEntity.this.isLoaded(); }
    }

    // ── Items ───────────────────────────────────────────────────────────────

    public MachineModuleChemplant[] getModules() { return chemplantModule; }

    /** Original {@code isItemValidForSlot} (Automatisierung). */
    public boolean isItemValidForAutomation(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return true;
        for (int i = 0; i < LANE_COUNT; i++) if (slot == blueprintSlot(i) && stack.getItem() instanceof ItemBlueprints) return true;
        if (slot >= SLOT_UPGRADE_START && slot <= SLOT_UPGRADE_END && stack.getItem() instanceof ItemMachineUpgrade) return true;
        for (int i = 0; i < LANE_COUNT; i++) if (chemplantModule[i].isItemValidForSlot(slot, stack)) return true;
        return false;
    }

    /** Original {@code canExtractItem}: Ausgaenge und verstopfte Eingaenge. */
    public boolean canExtractAutomated(int slot) {
        for (int i = 0; i < LANE_COUNT; i++) {
            if (slot >= outputSlot(i, 0) && slot <= outputSlot(i, 2)) return true;
        }
        for (int k = 0; k < LANE_COUNT; k++) if (chemplantModule[k].isSlotClogged(slot)) return true;
        return false;
    }

    //? if forge {
    @Override
    public net.minecraftforge.items.IItemHandler getConditionalItemHandler(BlockPos part, @Nullable Direction side) {
        BlockPos[] io = ioParts();
        for (int i = 0; i < io.length; i++) {
            if (io[i].equals(part)) {
                return new SlotView(new int[] {
                        inputSlot(i, 0), inputSlot(i, 1), inputSlot(i, 2),
                        8, 9, 10,
                        15, 16, 17,
                        22, 23, 24,
                        29, 30, 31
                });
            }
        }
        return new SlotView(ACCESSIBLE);
    }

    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) {
            net.minecraftforge.items.IItemHandler h = new SlotView(ACCESSIBLE);
            return net.minecraftforge.common.util.LazyOptional.of(() -> h).cast();
        }
        return super.getCapability(cap, side);
    }

    /** Sicht auf ausgewaehlte Slots mit den Original-Regeln fuer Einfuegen/Entnehmen. */
    private class SlotView implements net.minecraftforge.items.IItemHandler {
        private final int[] map;
        SlotView(int[] map) { this.map = map; }
        @Override public int getSlots() { return map.length; }
        @Override public @NotNull ItemStack getStackInSlot(int s) { return inventory.getStackInSlot(map[s]); }
        @Override public @NotNull ItemStack insertItem(int s, @NotNull ItemStack stack, boolean simulate) {
            if (!isItemValidForAutomation(map[s], stack)) return stack;
            return inventory.insertItem(map[s], stack, simulate);
        }
        @Override public @NotNull ItemStack extractItem(int s, int amount, boolean simulate) {
            if (!canExtractAutomated(map[s])) return ItemStack.EMPTY;
            return inventory.extractItem(map[s], amount, simulate);
        }
        @Override public int getSlotLimit(int s) { return inventory.getSlotLimit(map[s]); }
        @Override public boolean isItemValid(int s, @NotNull ItemStack stack) { return isItemValidForAutomation(map[s], stack); }
    }
    //?}

    /** Menue/GUI: Eingaenge frei bestueckbar wie im Original-Container, Ausgaenge gesperrt. */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) {
            if (stack.isEmpty()) return false;
            if (stack.getItem() instanceof ItemCreativeBattery) return true;
            return isEnergyProviderItem(stack);
        }
        if (slot >= SLOT_UPGRADE_START && slot <= SLOT_UPGRADE_END) return stack.getItem() instanceof ItemMachineUpgrade;
        for (int i = 0; i < LANE_COUNT; i++) {
            if (slot == blueprintSlot(i)) return stack.getItem() instanceof ItemBlueprints;
            if (slot >= outputSlot(i, 0) && slot <= outputSlot(i, 2)) return false;
        }
        return true;
    }

    // ── Rezeptwahl (Original receiveControl: index + selection) ─────────────

    @Nullable
    public ResourceLocation getSelectedRecipeId(int lane) {
        return chemplantModule[lane].getSelectedRecipeId();
    }

    public void setSelectedRecipe(int lane, @Nullable ResourceLocation recipeId) {
        if (lane < 0 || lane >= LANE_COUNT) return;
        chemplantModule[lane].setSelectedRecipe(recipeId);
        // Original GUI-Auswahl: setRecipe(selection, false)
        chemplantModule[lane].restrictedMode = false;
        if (level != null && !level.isClientSide) {
            chemplantModule[lane].syncTankConfigurationToRecipe(level);
            setChanged();
            sendUpdateToClient();
        }
    }

    public ItemStack getBlueprintFolder(int lane) {
        return inventory.getStackInSlot(blueprintSlot(lane));
    }

    /** Rezepte, die mit der Mappe des Moduls waehlbar sind (Original {@code ItemBlueprints.grabPool}). */
    public List<ChemicalPlantRecipe> getAvailableRecipes(int lane) {
        if (level == null) return List.of();
        String installedPool = ItemBlueprints.getBlueprintPool(getBlueprintFolder(lane));
        return com.hbm_m.recipe.index.ModRecipeIndex.of(level.getRecipeManager())
                .getAll(ChemicalPlantRecipe.Type.INSTANCE).stream().filter(r -> {
                    String pool = r.getBlueprintPool();
                    if (pool == null || pool.isEmpty()) return true;
                    return installedPool != null && !installedPool.isEmpty() && installedPool.equals(pool);
                }).toList();
    }

    public boolean isAnyProcessing() {
        return didProcess[0] || didProcess[1] || didProcess[2] || didProcess[3];
    }

    public float getAnim(float partialTicks) {
        return prevAnim + (anim - prevAnim) * partialTicks;
    }

    // ── Fluids ──────────────────────────────────────────────────────────────

    public FluidTank[] getInputTanks() { return inputTanks; }
    public FluidTank[] getOutputTanks() { return outputTanks; }
    public FluidTank getWaterTank() { return water; }
    public FluidTank getSpentSteamTank() { return lps; }

    @Override public FluidTank[] getReceivingTanks() { return inputTanks; }
    @Override public FluidTank[] getSendingTanks() { return outputTanks; }
    @Override public FluidTank[] getAllTanks() { return allTanks; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ── Upgrades ────────────────────────────────────────────────────────────

    @Override
    public java.util.Map<UpgradeType, Integer> getValidUpgrades() {
        return VALID_UPGRADES;
    }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(com.hbm_m.block.ModBlocks.CHEMICAL_FACTORY.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_SPEED, "+" + (level * 100 / 3) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 50) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (level * 25) + "%").withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle(BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        }
    }

    // ── Menue / Name ────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() { return Component.translatable("container.hbm_m.chemical_factory"); }

    @Override
    public Component getDisplayName() { return getDefaultName(); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineChemicalFactoryMenu(containerId, playerInventory, this, data);
    }

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ() - 2,
                worldPosition.getX() + 3, worldPosition.getY() + 3, worldPosition.getZ() + 3);
    }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        for (int i = 0; i < inputTanks.length; i++) tag.put("inputTank" + i, inputTanks[i].writeNBT(new CompoundTag()));
        for (int i = 0; i < outputTanks.length; i++) tag.put("outputTank" + i, outputTanks[i].writeNBT(new CompoundTag()));
        tag.put("water", water.writeNBT(new CompoundTag()));
        tag.put("lps", lps.writeNBT(new CompoundTag()));
        for (int i = 0; i < LANE_COUNT; i++) {
            tag.putBoolean("didProcess" + i, didProcess[i]);
            CompoundTag laneTag = new CompoundTag();
            chemplantModule[i].writeNBT(laneTag);
            tag.put("lane" + i, laneTag);
        }
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        for (int i = 0; i < inputTanks.length; i++) if (tag.contains("inputTank" + i)) inputTanks[i].readNBT(tag.getCompound("inputTank" + i));
        for (int i = 0; i < outputTanks.length; i++) if (tag.contains("outputTank" + i)) outputTanks[i].readNBT(tag.getCompound("outputTank" + i));
        if (tag.contains("water")) water.readNBT(tag.getCompound("water"));
        if (tag.contains("lps")) lps.readNBT(tag.getCompound("lps"));
        for (int i = 0; i < LANE_COUNT; i++) {
            didProcess[i] = tag.getBoolean("didProcess" + i);
            if (tag.contains("lane" + i)) chemplantModule[i].readNBT(tag.getCompound("lane" + i));
            chemplantModule[i].didProcess = didProcess[i];
        }
    }

    // ── Redstone-over-Radio (1:1 TileEntityMachineChemicalFactory) ──

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_VALUE + "progress1",
                PREFIX_VALUE + "progress2",
                PREFIX_VALUE + "progress3",
                PREFIX_VALUE + "progress4",
                PREFIX_VALUE + "recipe1",
                PREFIX_VALUE + "recipe2",
                PREFIX_VALUE + "recipe3",
                PREFIX_VALUE + "recipe4",
                PREFIX_VALUE + "anyactive",
                PREFIX_VALUE + "active1",
                PREFIX_VALUE + "active2",
                PREFIX_VALUE + "active3",
                PREFIX_VALUE + "active4",
        };
    }

    /** 1:1 inklusive Original-Eigenheit: die Abfrage zaehlt 0-3, die Liste nennt 1-4. */
    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "anyactive").equals(name))          return "" + ((this.didProcess[0] || this.didProcess[1] || this.didProcess[2] || this.didProcess[3]) ? 1 : 0);
        for (int i = 0; i < 4; i++) {
            if ((PREFIX_VALUE + "progress" + i).equals(name))   return "" + (int) Math.round(this.chemplantModule[i].getProgressPercent() * 100);
            if ((PREFIX_VALUE + "recipe" + i).equals(name))     return com.hbm_m.api.redstoneoverradio.RORRecipeNames.name(this.chemplantModule[i].getSelectedRecipeId());
            if ((PREFIX_VALUE + "active" + i).equals(name))     return "" + (this.didProcess[i] ? 1 : 0);
        }
        return null;
    }
}
