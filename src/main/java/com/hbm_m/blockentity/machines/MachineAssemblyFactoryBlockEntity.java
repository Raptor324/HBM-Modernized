package com.hbm_m.blockentity.machines;

import java.util.List;
import java.util.Random;

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
import com.hbm_m.inventory.menu.MachineAssemblyFactoryMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.industrial.ItemBlueprints;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.module.machine.MachineModuleAdvancedAssembler;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.sound.ClientSoundBootstrap;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.util.BobMathUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
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
 * 1:1 {@code TileEntityMachineAssemblyFactory}: vier Montage-Module ({@code ModuleMachineAssembler}) mit je eigener
 * Rezeptwahl und Bauplanmappe, gemeinsamer Kuehlkreis (Wasser -&gt; Abdampf, 100 mB je arbeitendem Modul).
 * Rezepte laufen doppelt so schnell, brauchen aber doppelt so viel Strom ({@code speed * 2}, {@code pow * 2}).
 *
 * <p>Slots wie im Original (60): 0 Batterie, 1-3 Upgrades, je Modul i: {@code 4+i*14} Bauplanmappe,
 * {@code 5..16+i*14} zwoelf Eingaenge, {@code 17+i*14} Ausgang. Kuehlleitungen reichen nur Wasser/Abdampf
 * durch ({@code DelegateAssemblyFactoy}), die vier Item-Anschluesse ({@code getIOPos}) speisen nur ihr Modul.</p>
 *
 * <p>Die Arm-Animation ({@link TragicYuri}) laeuft wie im Original nur auf dem Client.</p>
 */
public class MachineAssemblyFactoryBlockEntity extends BaseMachineBlockEntity
        implements IFluidStandardTransceiverMK2, IUpgradeInfoProvider, IFluidPartDelegateProvider,
        com.hbm_m.api.redstoneoverradio.IRORValueProvider
        //? if forge {
        , com.hbm_m.interfaces.IConditionalInvAccess
        //?}
{

    private static final String SOUND_INSTANCE = "com.hbm_m.sound.AssemblyFactorySoundInstance";

    public static final int LANE_COUNT = 4;
    public static final int SLOT_COUNT = 60;
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_UPGRADE_START = 1;
    public static final int SLOT_UPGRADE_END = 3;

    public static int blueprintSlot(int lane) { return 4 + lane * 14; }
    public static int inputSlot(int lane, int i) { return 5 + lane * 14 + i; }
    public static int outputSlot(int lane) { return 17 + lane * 14; }

    /** Original {@code getAccessibleSlotsFromSide(side)} ("ho boy, a big fucking array of hand-written values"). */
    private static final int[] ACCESSIBLE = {
             5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
            19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
            33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45,
            47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59
    };

    private static final long BASE_MAX_POWER = 1_000_000L;

    public final FluidTank[] inputTanks = new FluidTank[LANE_COUNT];
    public final FluidTank[] outputTanks = new FluidTank[LANE_COUNT];
    public final FluidTank water;
    public final FluidTank lps;
    private final FluidTank[] allTanks;
    private boolean tanksDirty = false;

    private final MachineModuleAdvancedAssembler[] assemblerModule = new MachineModuleAdvancedAssembler[LANE_COUNT];
    /** Gewaehlte Rezepte je Modul (Original {@code ModuleMachineBase.recipe}, wird mitgespeichert). */
    private final ResourceLocation[] selectedRecipe = new ResourceLocation[LANE_COUNT];
    public final boolean[] didProcess = new boolean[LANE_COUNT];
    private final UpgradeManager upgradeManager = new UpgradeManager();

    private static final java.util.Map<UpgradeType, Integer> VALID_UPGRADES = java.util.Map.of(
            UpgradeType.SPEED, 3,
            UpgradeType.POWER, 3,
            UpgradeType.OVERDRIVE, 3
    );

    private final DelegateAssemblyFactory delegate = new DelegateAssemblyFactory();

    public boolean frame = false;
    public final TragicYuri[] animations = new TragicYuri[2];

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= 0 && index < LANE_COUNT) {
                return assemblerModule[index].getProgressInt();
            }
            if (index >= LANE_COUNT && index < LANE_COUNT * 2) {
                return assemblerModule[index - LANE_COUNT].getMaxProgress();
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

    public MachineAssemblyFactoryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASSEMBLY_FACTORY_BE.get(), pos, state, SLOT_COUNT, BASE_MAX_POWER, BASE_MAX_POWER);

        for (int i = 0; i < animations.length; i++) animations[i] = new TragicYuri(i);

        for (int i = 0; i < LANE_COUNT; i++) {
            inputTanks[i] = new FluidTank(4_000) {
                @Override public void onContentsChanged() { setChanged(); tanksDirty = true; }
            };
            outputTanks[i] = new FluidTank(4_000) {
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
            // Original ModuleMachineAssembler: kein automatisches Rezeptsuchen, nur das gewaehlte Rezept zaehlt
            assemblerModule[i] = new MachineModuleAdvancedAssembler(i, this, inventory, null) {
                @Override @Nullable public AssemblerRecipe findRecipeForInputs() { return null; }
            }.setInputSlots(inputSlot(i, 0), 12).setOutputSlot(outputSlot(i)).setFluidTanks(inputTanks[i], outputTanks[i]);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineAssemblyFactoryBlockEntity be) {
        for (MachineModuleAdvancedAssembler m : be.assemblerModule) m.setLevel(level);

        if (level.isClientSide) {
            be.clientTick();

            boolean any = be.isAnyProcessing();
            for (TragicYuri animation : be.animations) animation.update(any);

            if (level.getGameTime() % 20 == 0) {
                be.frame = !level.getBlockState(pos.above(3)).isAir();
            }
            return;
        }

        be.ensureNetworkInitialized();

        long nextMaxPower = 0;
        for (int i = 0; i < LANE_COUNT; i++) {
            AssemblerRecipe recipe = be.getRecipe(i);
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
            MachineModuleAdvancedAssembler module = be.assemblerModule[i];
            module.update(speed * 2D, pow * 2D, be.canCool(), be.inventory.getStackInSlot(blueprintSlot(i)));
            be.didProcess[i] = module.getDidProcess();
            markDirty |= module.needsSync;
            if (was != be.didProcess[i]) markDirty = true;

            if (be.didProcess[i]) {
                be.water.drainMb(100);
                be.lps.fillMb(be.lps.getConfiguredFluid(), 100);
            }
        }

        if (be.tanksDirty) {
            markDirty = true;
            be.tanksDirty = false;
        }

        // Original networkPackNT(100): laufender Fortschritt wird regelmaessig nachgeschoben
        if (be.isAnyProcessing() && level.getGameTime() % 20L == 0L) markDirty = true;

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
        // Original: ELECTRIC_MOTOR_LOOP (Lautstaerke 0.5, Tonhoehe 0.75), solange ein Modul arbeitet
        ClientSoundBootstrap.updateSound(this, isAnyProcessing(), this::newSoundInstance);
    }

    private Object newSoundInstance() {
        try {
            return Class.forName(SOUND_INSTANCE).getConstructor(BlockPos.class).newInstance(this.getBlockPos());
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

    /** Original {@code MainRegistry.proxy.playSoundClient} - nur auf dem Client aufgerufen. */
    private void playSoundClient(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        if (level == null || !level.isClientSide) return;
        level.playLocalSound(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                sound, SoundSource.BLOCKS, volume, pitch, false);
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

    /** Original {@code DelegateAssemblyFactoy}: an den Kuehlleitungen nur Wasser rein, Abdampf raus. */
    public class DelegateAssemblyFactory implements IFluidStandardTransceiverMK2 {
        @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { water }; }
        @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { lps }; }
        @Override public FluidTank[] getAllTanks() { return MachineAssemblyFactoryBlockEntity.this.getAllTanks(); }
        @Override public boolean isLoaded() { return MachineAssemblyFactoryBlockEntity.this.isLoaded(); }
    }

    // ── Items ───────────────────────────────────────────────────────────────

    public MachineModuleAdvancedAssembler[] getModules() { return assemblerModule; }

    /** Original {@code isItemValidForSlot} (Automatisierung). */
    public boolean isItemValidForAutomation(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return true;
        for (int i = 0; i < LANE_COUNT; i++) if (slot == blueprintSlot(i) && stack.getItem() instanceof ItemBlueprints) return true;
        if (slot >= SLOT_UPGRADE_START && slot <= SLOT_UPGRADE_END && stack.getItem() instanceof ItemMachineUpgrade) return true;
        for (int i = 0; i < LANE_COUNT; i++) if (isValidForLane(i, slot, stack)) return true;
        return false;
    }

    /** Original {@code ModuleMachineAssembler.isItemValid}: nur Zutaten des gewaehlten Rezepts. */
    private boolean isValidForLane(int lane, int slot, ItemStack stack) {
        if (slot < inputSlot(lane, 0) || slot > inputSlot(lane, 11)) return false;
        AssemblerRecipe recipe = getRecipe(lane);
        if (recipe == null) return false;
        for (var in : recipe.getInputDisplaySlots()) if (in.ingredient().test(stack)) return true;
        return false;
    }

    /** Original {@code canExtractItem}: Ausgaenge und verstopfte Eingaenge. */
    public boolean canExtractAutomated(int slot) {
        for (int k = 0; k < LANE_COUNT; k++) if (slot == outputSlot(k)) return true;
        for (int k = 0; k < LANE_COUNT; k++) {
            if (slot >= inputSlot(k, 0) && slot <= inputSlot(k, 11)) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!stack.isEmpty() && !isValidForLane(k, slot, stack)) return true;
            }
        }
        return false;
    }

    //? if forge {
    @Override
    public net.minecraftforge.items.IItemHandler getConditionalItemHandler(BlockPos part, @Nullable Direction side) {
        BlockPos[] io = ioParts();
        for (int i = 0; i < io.length; i++) {
            if (io[i].equals(part)) {
                int[] map = new int[16];
                for (int k = 0; k < 12; k++) map[k] = inputSlot(i, k);
                map[12] = 17; map[13] = 31; map[14] = 45; map[15] = 59; // entering flavor town...
                return new SlotView(map);
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

    /** Menue/GUI: Eingaenge frei bestueckbar wie im Original-Container, Ausgaenge gesperrt (Original addOutputSlots). */
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
            if (slot == outputSlot(i)) return false;
        }
        return true;
    }

    // ── Rezeptwahl (Original receiveControl: index + selection) ─────────────

    @Nullable
    public ResourceLocation getSelectedRecipeId(int lane) {
        return lane >= 0 && lane < LANE_COUNT ? selectedRecipe[lane] : null;
    }

    @Nullable
    public AssemblerRecipe getRecipe(int lane) {
        ResourceLocation id = getSelectedRecipeId(lane);
        if (id == null || level == null) return null;
        return RecipeHooks.getRecipeByKey(level.getRecipeManager(), id)
                .filter(r -> r instanceof AssemblerRecipe)
                .map(r -> (AssemblerRecipe) r)
                .orElse(null);
    }

    public void setSelectedRecipe(int lane, @Nullable ResourceLocation recipeId) {
        if (lane < 0 || lane >= LANE_COUNT) return;
        selectedRecipe[lane] = recipeId;
        assemblerModule[lane].setSelectedRecipeId(recipeId);
        // Original GUI-Auswahl: setRecipe(selection, false)
        assemblerModule[lane].restrictedMode = false;
        if (level != null && !level.isClientSide) {
            setChanged();
            sendUpdateToClient();
        }
    }

    public ItemStack getBlueprintFolder(int lane) {
        return inventory.getStackInSlot(blueprintSlot(lane));
    }

    /** Rezepte, die mit der Mappe des Moduls waehlbar sind (Original {@code ItemBlueprints.grabPool}). */
    public List<AssemblerRecipe> getAvailableRecipes(int lane) {
        if (level == null) return List.of();
        String installedPool = ItemBlueprints.getBlueprintPool(getBlueprintFolder(lane));
        return com.hbm_m.recipe.index.ModRecipeIndex.of(level.getRecipeManager())
                .getAll(AssemblerRecipe.Type.INSTANCE).stream().filter(r -> {
                    String pool = r.getBlueprintPool();
                    if (pool == null || pool.isEmpty()) return true;
                    return installedPool != null && !installedPool.isEmpty() && installedPool.equals(pool);
                }).toList();
    }

    public boolean isAnyProcessing() {
        return didProcess[0] || didProcess[1] || didProcess[2] || didProcess[3];
    }

    // ── Fluids ──────────────────────────────────────────────────────────────

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
        info.add(IUpgradeInfoProvider.getStandardLabel(com.hbm_m.block.ModBlocks.ASSEMBLY_FACTORY.get()));
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
    protected Component getDefaultName() { return Component.translatable("container.machineAssemblyFactory"); }

    @Override
    public Component getDisplayName() { return getDefaultName(); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineAssemblyFactoryMenu(containerId, playerInventory, this, data);
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
        for (int i = 0; i < inputTanks.length; i++) tag.put("i" + i, inputTanks[i].writeNBT(new CompoundTag()));
        for (int i = 0; i < outputTanks.length; i++) tag.put("o" + i, outputTanks[i].writeNBT(new CompoundTag()));
        tag.put("w", water.writeNBT(new CompoundTag()));
        tag.put("s", lps.writeNBT(new CompoundTag()));
        for (int i = 0; i < LANE_COUNT; i++) {
            tag.putBoolean("didProcess" + i, didProcess[i]);
            if (selectedRecipe[i] != null) tag.putString("recipe" + i, selectedRecipe[i].toString());
            assemblerModule[i].writeToNBT(tag);
        }
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        for (int i = 0; i < inputTanks.length; i++) if (tag.contains("i" + i)) inputTanks[i].readNBT(tag.getCompound("i" + i));
        for (int i = 0; i < outputTanks.length; i++) if (tag.contains("o" + i)) outputTanks[i].readNBT(tag.getCompound("o" + i));
        if (tag.contains("w")) water.readNBT(tag.getCompound("w"));
        if (tag.contains("s")) lps.readNBT(tag.getCompound("s"));
        for (int i = 0; i < LANE_COUNT; i++) {
            didProcess[i] = tag.getBoolean("didProcess" + i);
            ResourceLocation id = tag.contains("recipe" + i) ? ResourceLocation.tryParse(tag.getString("recipe" + i)) : null;
            if (!java.util.Objects.equals(id, selectedRecipe[i])) {
                selectedRecipe[i] = id;
                assemblerModule[i].setSelectedRecipeId(id);
            }
            assemblerModule[i].readFromNBT(tag);
            assemblerModule[i].didProcess = didProcess[i];
        }
    }

    // ── Animation (Original TragicYuri, nur Client) ─────────────────────────

    /**
     * Carriage consisting of two arms - a striker and a saw.
     * Movement of both arms is inverted, one pedestal can only be serviced by one arm at a time.
     */
    public class TragicYuri {

        public final AssemblerArm striker;
        public final AssemblerArm saw;

        final Random rand = new Random();
        YuriState state = YuriState.WORKING;
        double slider = 0;
        double prevSlider = 0;
        boolean direction = false;
        int timeUntilReposition;

        public TragicYuri(int group) {
            striker = new AssemblerArm(group == 0 ? 0 : 3);
            saw = new AssemblerArm(group == 0 ? 1 : 2).yepThatsASaw();
            timeUntilReposition = 140 + rand.nextInt(161);
        }

        public void update(boolean working) {
            this.prevSlider = this.slider;

            // eine der Arme muss etwas tun, egal welcher oder wo der Schlitten steht
            if (didProcess[striker.recipeIndex] || didProcess[saw.recipeIndex]) switch (state) {
                case WORKING -> {
                    timeUntilReposition--;
                    if (timeUntilReposition <= 0) {
                        state = YuriState.RETIRING;
                    }
                }
                case RETIRING -> {
                    if (striker.state == ArmState.WAIT && saw.state == ArmState.WAIT) { // erst weiter, wenn beide Arme fertig sind
                        state = YuriState.SLIDING;
                        direction = !direction;
                        playSoundClient(ModSounds.ASSEMBLER_START.get(), 0.25F, 1.25F + (level != null ? level.random.nextFloat() : 0F) * 0.25F);
                    }
                }
                case SLIDING -> {
                    double sliderSpeed = 1D / 10D; // 10 Ticks Fahrt
                    if (direction) {
                        slider += sliderSpeed;
                        if (slider >= 1) {
                            slider = 1;
                            state = YuriState.WORKING;
                        }
                    } else {
                        slider -= sliderSpeed;
                        if (slider <= 0) {
                            slider = 0;
                            state = YuriState.WORKING;
                        }
                    }
                    if (state == YuriState.WORKING) timeUntilReposition = 140 + rand.nextInt(161); // 7 bis 15 Sekunden
                }
            }

            striker.updateArm();
            saw.updateArm();
        }

        public double getSlider(float interp) {
            return this.prevSlider + (this.slider - this.prevSlider) * interp;
        }

        public class AssemblerArm {

            public double[] angles = new double[4];
            public double[] prevAngles = new double[4];
            public double[] targetAngles = new double[4];
            public double[] speed = new double[4];
            public double sawAngle;
            public double prevSawAngle;
            /** Index des bedienten Sockels bei Schlitten in Grundstellung. */
            public int recipeIndex;

            ArmState state = ArmState.REPOSITION;
            int actionDelay = 0;
            boolean saw = false;

            public AssemblerArm(int index) {
                this.recipeIndex = index;
                this.resetSpeed();
                this.chooseNewArmPoistion();
            }

            public AssemblerArm yepThatsASaw() { this.saw = true; this.chooseNewArmPoistion(); return this; }

            private void resetSpeed() {
                speed[0] = 15;  // Pivot
                speed[1] = 15;  // Arm
                speed[2] = 15;  // Piston
                speed[3] = saw ? 0.125 : 0.5; // Striker
            }

            public void updateArm() {
                resetSpeed();

                for (int i = 0; i < angles.length; i++) {
                    prevAngles[i] = angles[i];
                }

                prevSawAngle = sawAngle;

                int serviceIndex = recipeIndex;
                if (slider > 0.5) serviceIndex += (serviceIndex % 2 == 0 ? 1 : -1); // Schlitten verfahren: Indizes tauschen
                if (!didProcess[serviceIndex]) state = ArmState.RETIRE;

                if (state == ArmState.CUT || state == ArmState.EXTEND) {
                    this.sawAngle += 45D;
                }

                if (actionDelay > 0) {
                    actionDelay--;
                    return;
                }

                switch (state) {
                    // Bewegen; danach kurze Pause und EXTEND
                    case REPOSITION -> {
                        if (move()) {
                            actionDelay = 2;
                            state = ArmState.EXTEND;
                            targetAngles[3] = saw ? -0.375D : -0.75D;
                        }
                    }
                    case EXTEND -> {
                        if (move()) {
                            if (saw) {
                                state = ArmState.CUT;
                                targetAngles[2] = -targetAngles[2];
                                playSoundClient(ModSounds.ASSEMBLER_CUT.get(), 0.5F, 1F + rand.nextFloat() * 0.25F);
                            } else {
                                state = ArmState.RETRACT;
                                targetAngles[3] = 0D;
                                playSoundClient(ModSounds.ASSEMBLER_STRIKE_RANDOM.get(), 0.5F, 1F);
                            }
                        }
                    }
                    case CUT -> {
                        speed[2] = Math.abs(targetAngles[2] / 20D);
                        if (move()) {
                            state = ArmState.RETRACT;
                            targetAngles[3] = 0D;
                        }
                    }
                    case RETRACT -> {
                        if (move()) {
                            actionDelay = 2 + rand.nextInt(5);
                            chooseNewArmPoistion();
                            state = TragicYuri.this.state == YuriState.RETIRING ? ArmState.RETIRE : ArmState.REPOSITION;
                        }
                    }
                    case RETIRE -> {
                        this.targetAngles[0] = 0;
                        this.targetAngles[1] = 0;
                        this.targetAngles[2] = 0;
                        this.targetAngles[3] = 0;

                        if (move()) {
                            actionDelay = 2 + rand.nextInt(5);
                            chooseNewArmPoistion();
                            state = ArmState.WAIT;
                        }
                    }
                    case WAIT -> {
                        if (TragicYuri.this.state == YuriState.WORKING) this.state = ArmState.REPOSITION;
                    }
                }
            }

            public void chooseNewArmPoistion() {

                double[][] pos = !saw ? new double[][] {
                        // Striker
                        {10, 10, -10},
                        {15, 15, -15},
                        {25, 10, -15},
                        {30, 0, -10},
                        {-10, 10, 0},
                        {-20, 30, -15}
                } : new double[][] {
                        // Saege
                        {-15, 15, -10},
                        {-15, 15, -15},
                        {-15, 15, 10},
                        {-15, 15, 15},
                        {-15, 15, 2},
                        {-15, 15, -2}
                };

                int chosen = rand.nextInt(pos.length);
                this.targetAngles[0] = pos[chosen][0];
                this.targetAngles[1] = pos[chosen][1];
                this.targetAngles[2] = pos[chosen][2];
            }

            private boolean move() {
                boolean didMove = false;

                for (int i = 0; i < angles.length; i++) {
                    if (angles[i] == targetAngles[i])
                        continue;

                    didMove = true;

                    double angle = angles[i];
                    double target = targetAngles[i];
                    double turn = speed[i];
                    double delta = Math.abs(angle - target);

                    if (delta <= turn) {
                        angles[i] = targetAngles[i];
                        continue;
                    }

                    if (angle < target) {
                        angles[i] += turn;
                    } else {
                        angles[i] -= turn;
                    }
                }

                return !didMove;
            }

            public double[] getPositions(float interp) {
                return new double[] {
                        BobMathUtil.interp(this.prevAngles[0], this.angles[0], interp),
                        BobMathUtil.interp(this.prevAngles[1], this.angles[1], interp),
                        BobMathUtil.interp(this.prevAngles[2], this.angles[2], interp),
                        BobMathUtil.interp(this.prevAngles[3], this.angles[3], interp),
                        BobMathUtil.interp(this.prevSawAngle, this.sawAngle, interp)
                };
            }
        }
    }

    /*
     * Arme laufen REPOSITION -> EXTEND -> CUT (nur Saege) -> RETRACT.
     * Ist eine Fahrt geplant, wechselt der Schlitten auf RETIRING; jeder Arm geht nach RETRACT in RETIRE,
     * in Nullstellung dann WAIT. Warten beide Arme, faehrt der Schlitten (SLIDING), danach wieder WORKING.
     */
    public enum YuriState {
        WORKING,
        RETIRING, // wartet, bis die Arme in WAIT sind
        SLIDING   // Fahrt zur naechsten Position
    }

    public enum ArmState {
        REPOSITION,
        EXTEND,
        CUT,
        RETRACT,
        RETIRE, // zurueck in Nullstellung fuer die Schlittenfahrt
        WAIT    // wartet auf die Fahrt oder ist mitten drin
    }

    // ── Redstone-over-Radio (1:1 TileEntityMachineAssemblyFactory) ──

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
            if ((PREFIX_VALUE + "progress" + i).equals(name))   return "" + (int) Math.round(this.assemblerModule[i].getProgressPercent() * 100);
            if ((PREFIX_VALUE + "recipe" + i).equals(name))     return com.hbm_m.api.redstoneoverradio.RORRecipeNames.name(this.selectedRecipe[i]);
            if ((PREFIX_VALUE + "active" + i).equals(name))     return "" + (this.didProcess[i] ? 1 : 0);
        }
        return null;
    }
}
