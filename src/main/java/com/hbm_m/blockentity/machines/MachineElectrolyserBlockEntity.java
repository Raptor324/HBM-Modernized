package com.hbm_m.blockentity.machines;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.interfaces.IUpgradeInfoProvider;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.menu.MachineElectrolyserFluidMenu;
import com.hbm_m.inventory.menu.MachineElectrolyserMetalMenu;
import com.hbm_m.inventory.recipes.ElectrolyserFluidRecipes;
import com.hbm_m.inventory.recipes.ElectrolyserFluidRecipes.ElectrolysisRecipe;
import com.hbm_m.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm_m.inventory.recipes.ElectrolyserMetalRecipes.ElectrolysisMetalRecipe;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.util.CrucibleUtil;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityElectrolyser}: 11x3-Elektrolyseur mit zwei Seiten. Fluidseite: Eingangsfluid (Typ ueber den
 * Identifikator) zu zwei Ausgangsfluiden plus Nebenprodukten. Metallseite: Kristalle und Grundgestein-Erze mit 100 mB
 * Salpetersaeure zu zwei Schmelzen, die an beiden Enden 5,875 Bloecke vor dem Kern in Giessanlagen fliessen.
 * Speed-, Power- und Overdrive-Upgrades wie im Original; die GUI-Seite wird per Steuerpaket umgeschaltet.
 */
public class MachineElectrolyserBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2, IControlReceiver, IUpgradeInfoProvider {

    //0: Battery
    //1-2: Upgrades
    //// FLUID
    //3-4: Fluid ID
    //5-10: Fluid IO
    //11-13: Byproducts
    //// METAL
    //14: Crystal
    //15-20: Outputs
    public static final int INVENTORY_SIZE = 21;

    public static final long maxPower = 20000000;
    public static final int usageOreBase = 10_000;
    public static final int usageFluidBase = 10_000;
    public int usageOre;
    public int usageFluid;

    public int progressFluid;
    public int processFluidTime = 100;
    public int progressOre;
    public int processOreTime = 600;

    @Nullable public MaterialStack leftStack;
    @Nullable public MaterialStack rightStack;
    public int maxMaterial = MaterialShapes.BLOCK.q(16);

    private int lastSelectedGUI = 0;

    public final FluidTank[] tanks;

    public final UpgradeManager upgradeManager = new UpgradeManager();

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = Map.of(
            UpgradeType.SPEED, 3,
            UpgradeType.POWER, 3,
            UpgradeType.OVERDRIVE, 3);

    public MachineElectrolyserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTROLYSER_BE.get(), pos, state, INVENTORY_SIZE, maxPower, maxPower);
        tanks = new FluidTank[4];
        tanks[0] = new FluidTank(ModFluids.WATER.getSource(), 16000);
        tanks[1] = new FluidTank(ModFluids.HYDROGEN.getSource(), 16000);
        tanks[2] = new FluidTank(ModFluids.OXYGEN.getSource(), 16000);
        tanks[3] = new FluidTank(ModFluids.NITRIC_ACID.getSource(), 16000);
    }

    public long getPower() { return energy; }

    private Direction getDir() {
        BlockState state = getBlockState();
        return state.hasProperty(DummyableMachineBlock.FACING) ? state.getValue(DummyableMachineBlock.FACING) : Direction.NORTH;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineElectrolyserBlockEntity be) {
        if (!level.isClientSide()) be.serverTick((ServerLevel) level, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos) {

        this.chargeFromBatterySlot(0);
        ItemStack[] slots = slotArray();
        boolean changed = this.tanks[0].setType(3, 4, slots);
        changed |= this.tanks[0].loadTank(5, 6, slots);
        changed |= this.tanks[1].unloadTank(7, 8, slots);
        changed |= this.tanks[2].unloadTank(9, 10, slots);
        if (changed) applySlotArray(slots);

        if (level.getGameTime() % 20 == 0) {
            for (BlockPos[] con : this.getConPos()) {
                Direction d = con[1] == null ? getDir() : getDir().getOpposite();
                BlockPos p = con[0];
                this.trySubscribe(tanks[0].getTankType(), level, p, d);
                this.trySubscribe(tanks[3].getTankType(), level, p, d);

                if (tanks[1].getFill() > 0) this.tryProvide(tanks[1], level, p, d);
                if (tanks[2].getFill() > 0) this.tryProvide(tanks[2], level, p, d);
            }
        }

        upgradeManager.checkSlots(inventory, 1, 2, VALID_UPGRADES);
        int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = upgradeManager.getLevel(UpgradeType.POWER);

        usageOre = usageOreBase - usageOreBase * powerLevel / 4 + usageOreBase * speedLevel;
        usageFluid = usageFluidBase - usageFluidBase * powerLevel / 4 + usageFluidBase * speedLevel;

        for (int i = 0; i < getCycleCount(); i++) {
            if (this.canProcessFluid()) {
                this.progressFluid++;
                this.energy -= this.usageFluid;

                if (this.progressFluid >= this.getDurationFluid()) {
                    this.processFluids();
                    this.progressFluid = 0;
                    this.setChanged();
                }
            }

            if (this.canProcessMetal()) {
                this.progressOre++;
                this.energy -= this.usageOre;

                if (this.progressOre >= this.getDurationMetal()) {
                    this.processMetal();
                    this.progressOre = 0;
                    this.setChanged();
                }
            }
        }

        if (this.leftStack != null) {
            this.leftStack = pour(level, pos, getDir().getOpposite(), this.leftStack, speedLevel);
        }

        if (this.rightStack != null) {
            this.rightStack = pour(level, pos, getDir(), this.rightStack, speedLevel);
        }

        this.processFluidTime = getDurationFluid();
        this.processOreTime = getDurationMetal();

        setChanged();
        sendUpdateToClient();
    }

    /** Original: {@code pourFullStack} 5,875 Bloecke in Richtung {@code dir}, Partikelstrahl dazu. */
    @Nullable
    private MaterialStack pour(ServerLevel level, BlockPos pos, Direction dir, MaterialStack stack, int speedLevel) {
        List<MaterialStack> toCast = new ArrayList<>();
        toCast.add(stack);

        double[] impact = new double[3];
        MaterialStack didPour = CrucibleUtil.pourFullStack(level, pos.getX() + 0.5D + dir.getStepX() * 5.875D, pos.getY() + 2D, pos.getZ() + 0.5D + dir.getStepZ() * 5.875D, 6, true, toCast, MaterialShapes.NUGGET.q(3) * Math.max(getCycleCount() * speedLevel, 1), impact);

        if (didPour != null) {
            CompoundTag data = new CompoundTag();
            data.putString("type", "foundry");
            data.putInt("color", didPour.material.moltenColor);
            data.putByte("dir", (byte) dir.get3DDataValue());
            data.putFloat("off", 0.625F);
            data.putFloat("base", 0.625F);
            data.putFloat("len", Math.max(1F, pos.getY() - (float) (Math.ceil(impact[1]) - 0.875) + 2));
            com.hbm_m.particle.helper.IParticleCreator.sendPacket(level, pos.getX() + 0.5D + dir.getStepX() * 5.875D, pos.getY() + 2, pos.getZ() + 0.5D + dir.getStepZ() * 5.875D, 50, data);

            if (stack.amount <= 0) return null;
        }
        return stack;
    }

    /**
     * Original {@code getConPos()}: je drei Zellen sechs Bloecke hinter und vor dem Kern. Zweites Feld nur als Marker:
     * {@code null} = Richtung {@code dir}, sonst {@code dir.getOpposite()}.
     */
    public BlockPos[][] getConPos() {
        Direction dir = getDir();
        Direction rot = dir.getClockWise();
        BlockPos p = worldPosition;
        BlockPos back = p.relative(dir, -6);
        BlockPos front = p.relative(dir, 6);

        return new BlockPos[][] {
                { back, back },
                { back.relative(rot), back },
                { back.relative(rot, -1), back },
                { front, null },
                { front.relative(rot), null },
                { front.relative(rot, -1), null }
        };
    }

    public boolean canProcessFluid() {

        if (this.energy < usageFluid) return false;

        ElectrolysisRecipe recipe = ElectrolyserFluidRecipes.getRecipe(tanks[0].getTankType());

        if (recipe == null) return false;
        tanks[1].setTankType(recipe.output1.type());
        tanks[2].setTankType(recipe.output2.type());
        if (recipe.amount > tanks[0].getFill()) return false;
        if (recipe.output1.fill() + tanks[1].getFill() > tanks[1].getMaxFill()) return false;
        if (recipe.output2.fill() + tanks[2].getFill() > tanks[2].getMaxFill()) return false;

        if (recipe.byproduct != null) {

            for (int i = 0; i < recipe.byproduct.length; i++) {
                ItemStack slot = inventory.getStackInSlot(11 + i);
                ItemStack byproduct = recipe.byproduct[i];

                if (slot.isEmpty()) continue;
                if (!ItemStack.isSameItem(slot, byproduct)) return false;
                if (slot.getCount() + byproduct.getCount() > slot.getMaxStackSize()) return false;
            }
        }

        return true;
    }

    public void processFluids() {

        ElectrolysisRecipe recipe = ElectrolyserFluidRecipes.getRecipe(tanks[0].getTankType());
        tanks[0].setFill(tanks[0].getFill() - recipe.amount);
        tanks[1].setTankType(recipe.output1.type());
        tanks[2].setTankType(recipe.output2.type());
        tanks[1].setFill(tanks[1].getFill() + recipe.output1.fill());
        tanks[2].setFill(tanks[2].getFill() + recipe.output2.fill());

        if (recipe.byproduct != null) {

            for (int i = 0; i < recipe.byproduct.length; i++) {
                ItemStack slot = inventory.getStackInSlot(11 + i);
                ItemStack byproduct = recipe.byproduct[i];

                if (slot.isEmpty()) {
                    inventory.setStackInSlot(11 + i, byproduct.copy());
                } else {
                    ItemStack grown = slot.copy();
                    grown.grow(byproduct.getCount());
                    inventory.setStackInSlot(11 + i, grown);
                }
            }
        }
    }

    public boolean canProcessMetal() {

        ItemStack crystal = inventory.getStackInSlot(14);
        if (crystal.isEmpty()) return false;
        if (this.energy < usageOre) return false;
        if (this.tanks[3].getFill() < 100) return false;

        ElectrolysisMetalRecipe recipe = ElectrolyserMetalRecipes.getRecipe(crystal);
        if (recipe == null) return false;

        if (leftStack != null && recipe.output1 != null) {
            if (recipe.output1.material != leftStack.material) return false;
            if (recipe.output1.amount + leftStack.amount > this.maxMaterial) return false;
        }

        if (rightStack != null && recipe.output2 != null) {
            if (recipe.output2.material != rightStack.material) return false;
            if (recipe.output2.amount + rightStack.amount > this.maxMaterial) return false;
        }

        if (recipe.byproduct != null) {

            for (int i = 0; i < recipe.byproduct.length; i++) {
                ItemStack slot = inventory.getStackInSlot(15 + i);
                ItemStack byproduct = recipe.byproduct[i];

                if (slot.isEmpty()) continue;
                if (!ItemStack.isSameItem(slot, byproduct)) return false;
                if (slot.getCount() + byproduct.getCount() > slot.getMaxStackSize()) return false;
            }
        }

        return true;
    }

    public void processMetal() {

        ElectrolysisMetalRecipe recipe = ElectrolyserMetalRecipes.getRecipe(inventory.getStackInSlot(14));
        if (recipe.output1 != null) {
            if (leftStack == null) {
                leftStack = new MaterialStack(recipe.output1.material, recipe.output1.amount);
            } else {
                leftStack.amount += recipe.output1.amount;
            }
        }

        if (recipe.output2 != null) {
            if (rightStack == null) {
                rightStack = new MaterialStack(recipe.output2.material, recipe.output2.amount);
            } else {
                rightStack.amount += recipe.output2.amount;
            }
        }

        if (recipe.byproduct != null) {

            for (int i = 0; i < recipe.byproduct.length; i++) {
                ItemStack slot = inventory.getStackInSlot(15 + i);
                ItemStack byproduct = recipe.byproduct[i];

                if (slot.isEmpty()) {
                    inventory.setStackInSlot(15 + i, byproduct.copy());
                } else {
                    ItemStack grown = slot.copy();
                    grown.grow(byproduct.getCount());
                    inventory.setStackInSlot(15 + i, grown);
                }
            }
        }

        this.tanks[3].setFill(this.tanks[3].getFill() - 100);
        ItemStack crystal = inventory.getStackInSlot(14).copy();
        crystal.shrink(1);
        inventory.setStackInSlot(14, crystal.isEmpty() ? ItemStack.EMPTY : crystal);
    }

    public int getDurationMetal() {
        ElectrolysisMetalRecipe result = ElectrolyserMetalRecipes.getRecipe(inventory.getStackInSlot(14));
        int base = result != null ? result.duration : 600;
        int speed = upgradeManager.getLevel(UpgradeType.SPEED) - Math.min(upgradeManager.getLevel(UpgradeType.POWER), 1);
        return (int) Math.ceil((base * Math.max(1F - 0.25F * speed, 0.2)));
    }

    public int getDurationFluid() {
        ElectrolysisRecipe result = ElectrolyserFluidRecipes.getRecipe(tanks[0].getTankType());
        int base = result != null ? result.duration : 100;
        int speed = upgradeManager.getLevel(UpgradeType.SPEED) - Math.min(upgradeManager.getLevel(UpgradeType.POWER), 1);
        return (int) Math.ceil((base * Math.max(1F - 0.25F * speed, 0.2)));
    }

    public int getCycleCount() {
        int speed = upgradeManager.getLevel(UpgradeType.OVERDRIVE);
        return Math.min(1 + speed * 2, 7);
    }

    // ── Inventar ─────────────────────────────────────────────────────────────

    private ItemStack[] slotArray() {
        ItemStack[] arr = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) arr[i] = inventory.getStackInSlot(i);
        return arr;
    }

    private void applySlotArray(ItemStack[] arr) {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setStackInSlot(i, arr[i] == null ? ItemStack.EMPTY : arr[i]);
        }
        setChanged();
    }

    /**
     * Original {@code isItemValidForSlot} gilt nur fuer Automatisierung (Slot 14 mit Rezept); die GUI-Plaetze fuer
     * Batterie, Upgrades, Identifikator und Behaelter sind dort frei belegbar und bleiben es hier.
     */
    @Override
    protected boolean isItemValidForSlot(int i, ItemStack stack) {
        return switch (i) {
            case 0 -> isEnergyProviderItem(stack);
            case 1, 2 -> stack.getItem() instanceof ItemMachineUpgrade;
            case 3 -> stack.getItem() instanceof IItemFluidIdentifier;
            case 5, 7, 9 -> true;
            case 14 -> ElectrolyserMetalRecipes.getRecipe(stack) != null;
            default -> false;
        };
    }

    //? if forge {
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> sided;

    /** Original {@code getAccessibleSlotsFromSide {11-20}}: nur Slot 14 einfuegbar, alles ausser 14 entnehmbar. */
    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) {
            if (sided == null) sided = net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.IItemHandler() {
                @Override public int getSlots() { return 10; }
                @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(11 + slot); }
                @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    if (11 + slot != 14 || ElectrolyserMetalRecipes.getRecipe(stack) == null) return stack;
                    return inventory.insertItem(14, stack, simulate);
                }
                @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                    if (11 + slot == 14) return ItemStack.EMPTY;
                    return inventory.extractItem(11 + slot, amount, simulate);
                }
                @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(11 + slot); }
                @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return 11 + slot == 14 && ElectrolyserMetalRecipes.getRecipe(stack) != null; }
            });
            return sided.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        if (sided != null) sided.invalidate();
        sided = null;
    }
    //?} elif neoforge {
    /*private com.hbm_m.platform.LazyCap<net.neoforged.neoforge.items.IItemHandler> sided;

    /^* Original {@code getAccessibleSlotsFromSide {11-20}}: nur Slot 14 einfuegbar, alles ausser 14 entnehmbar. ^/
    @Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER && side != null) {
            if (sided == null) sided = com.hbm_m.platform.LazyCap.of(() -> new net.neoforged.neoforge.items.IItemHandler() {
                @Override public int getSlots() { return 10; }
                @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(11 + slot); }
                @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    if (11 + slot != 14 || ElectrolyserMetalRecipes.getRecipe(stack) == null) return stack;
                    return inventory.insertItem(14, stack, simulate);
                }
                @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                    if (11 + slot == 14) return ItemStack.EMPTY;
                    return inventory.extractItem(11 + slot, amount, simulate);
                }
                @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(11 + slot); }
                @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return 11 + slot == 14 && ElectrolyserMetalRecipes.getRecipe(stack) != null; }
            });
            return sided.cast();
        }
        return super.getHbmCapability(cap, side);
    }

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        if (sided != null) sided.invalidate();
        sided = null;
    }
    *///?}

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putInt("progressFluid", this.progressFluid);
        nbt.putInt("progressOre", this.progressOre);
        nbt.putInt("processFluidTime", this.processFluidTime);
        nbt.putInt("processOreTime", this.processOreTime);
        nbt.putInt("usageOre", this.usageOre);
        nbt.putInt("usageFluid", this.usageFluid);
        if (this.leftStack != null) {
            nbt.putInt("leftType", leftStack.material.id);
            nbt.putInt("leftAmount", leftStack.amount);
        }
        if (this.rightStack != null) {
            nbt.putInt("rightType", rightStack.material.id);
            nbt.putInt("rightAmount", rightStack.amount);
        }
        for (int i = 0; i < 4; i++) tanks[i].writeToNBT(nbt, "t" + i);
        nbt.putInt("lastSelectedGUI", this.lastSelectedGUI);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        this.progressFluid = nbt.getInt("progressFluid");
        this.progressOre = nbt.getInt("progressOre");
        this.processFluidTime = Math.max(1, nbt.getInt("processFluidTime"));
        this.processOreTime = Math.max(1, nbt.getInt("processOreTime"));
        this.usageOre = nbt.getInt("usageOre");
        this.usageFluid = nbt.getInt("usageFluid");
        this.leftStack = nbt.contains("leftType") && Mats.matById.get(nbt.getInt("leftType")) != null
                ? new MaterialStack(Mats.matById.get(nbt.getInt("leftType")), nbt.getInt("leftAmount")) : null;
        this.rightStack = nbt.contains("rightType") && Mats.matById.get(nbt.getInt("rightType")) != null
                ? new MaterialStack(Mats.matById.get(nbt.getInt("rightType")), nbt.getInt("rightAmount")) : null;
        for (int i = 0; i < 4; i++) tanks[i].readFromNBT(nbt, "t" + i);
        this.lastSelectedGUI = nbt.getInt("lastSelectedGUI");
    }

    // ── Fluid ────────────────────────────────────────────────────────────────

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1], tanks[2] }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0], tanks[3] }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ── GUI ──────────────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineElectrolyser");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    /** Original {@code provideContainer(-1)}: die zuletzt gewaehlte Seite. */
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        if (lastSelectedGUI == 0) return new MachineElectrolyserFluidMenu(id, inv, this);
        return new MachineElectrolyserMetalMenu(id, inv, this);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 256;
    }

    @Override
    public void receiveControl(CompoundTag data) { }

    /** Original: {@code sgm}/{@code sgf} waehlt die Seite und oeffnet die GUI neu. */
    @Override
    public void receiveControl(Player player, CompoundTag data) {

        if (data.contains("sgm")) lastSelectedGUI = 1;
        if (data.contains("sgf")) lastSelectedGUI = 0;

        if (player instanceof ServerPlayer sp) {
            BlockPos pos = worldPosition;
            MenuRegistry.openExtendedMenu(sp, this, buf -> buf.writeBlockPos(pos));
        }
    }

    // ── Upgrades ─────────────────────────────────────────────────────────────

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModBlocks.ELECTROLYSER.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (level * 25) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 100) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (level * 25) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_DELAY, "+" + (25) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle((System.currentTimeMillis() / 500) % 2 == 0 ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public Map<UpgradeType, Integer> getValidUpgrades() {
        return VALID_UPGRADES;
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 5, worldPosition.getY(), worldPosition.getZ() - 5,
                worldPosition.getX() + 6, worldPosition.getY() + 4, worldPosition.getZ() + 6);
        return bb;
    }
}
