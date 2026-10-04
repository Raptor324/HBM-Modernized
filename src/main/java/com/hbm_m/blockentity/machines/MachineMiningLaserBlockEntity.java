package com.hbm_m.blockentity.machines;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardSenderMK2;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.gas.BlockGasBase;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineMiningLaserMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;
import com.hbm_m.recipe.CentrifugeRecipe;
import com.hbm_m.recipe.CrystallizerRecipe;
import com.hbm_m.recipe.ModRecipes;
import com.hbm_m.recipe.ShredderRecipe;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineMiningLaser}: haengt unter der Decke und tastet sich Ebene fuer Ebene nach unten. Pro
 * Ebene wird das erste abbaubare Feld im Quadrat {@code (2*range+1)} gesucht, mit dem Strahl angebrochen
 * ({@code 1/(Haerte*15/Tempo)} Fortschritt pro Durchgang) und abgebaut; Fluessigkeiten werden entfernt und rundherum
 * mit Barrikaden abgedaemmt. Drops im Umkreis werden in die 21 Ausgabeslots gesaugt, Oelerz fuellt den
 * 64.000-mB-Oeltank (+500 mB), Lebewesen am Ziel fangen Feuer. Sonder-Upgrades: Kristallisierer, Zentrifuge,
 * Schredder, Schmelzer, Nullifizierer (verwirft Bruchgestein) und Screm. Ausgabe an Kisten/Truhen/Tresor/Trichter und
 * Oel an Rohre an den vier Anschluessen zwei Bloecke vom Kern; Strom kommt von oben, Redstone an einem der
 * Anschlussbloecke haelt an.
 */
public class MachineMiningLaserBlockEntity extends BaseMachineBlockEntity implements IFluidStandardSenderMK2,
        com.hbm_m.interfaces.IUpgradeInfoProvider {

    /** Original: Slot 0 Batterie, 1-8 Upgrades, 9-29 Ausgabe. */
    public static final int SLOT_BATTERY = 0;
    public static final int UPGRADE_START = 1;
    public static final int UPGRADE_COUNT = 8;
    public static final int OUTPUT_START = 9;
    public static final int OUTPUT_COUNT = 21;
    private static final int SLOT_COUNT = OUTPUT_START + OUTPUT_COUNT;

    public static final long maxPower = 100000000;
    public static final int consumption = 10000;

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = new EnumMap<>(UpgradeType.class);
    static {
        VALID_UPGRADES.put(UpgradeType.SPEED, 12);
        VALID_UPGRADES.put(UpgradeType.POWER, 12);
        VALID_UPGRADES.put(UpgradeType.EFFECT, 12);
        VALID_UPGRADES.put(UpgradeType.FORTUNE, 3);
        VALID_UPGRADES.put(UpgradeType.OVERDRIVE, 9);
    }

    public final FluidTank tank = new FluidTank(ModFluids.CRUDE_OIL.getSource(), 64_000);

    public boolean isOn;
    private boolean redstonePowered;
    public int targetX;
    public int targetY = Integer.MIN_VALUE;
    public int targetZ;
    public int lastTargetX;
    public int lastTargetY;
    public int lastTargetZ;
    public boolean beam;
    double breakProgress;
    private double clientBreakProgress;

    public final UpgradeManager upgradeManager = new UpgradeManager();

    public MachineMiningLaserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINING_LASER_BE.get(), pos, state, SLOT_COUNT, maxPower, maxPower, 0L);
    }

    private record DirPos(BlockPos pos, Direction dir) { }

    private DirPos[] getConPos() {
        BlockPos p = worldPosition;
        return new DirPos[] {
                new DirPos(p.offset(2, 0, 0), Direction.EAST),
                new DirPos(p.offset(-2, 0, 0), Direction.WEST),
                new DirPos(p.offset(0, 0, 2), Direction.SOUTH),
                new DirPos(p.offset(0, 0, -2), Direction.NORTH),
        };
    }

    private boolean isMultiblockRedstonePowered(Level world) {
        for (DirPos con : getConPos()) {
            if (world.hasNeighborSignal(con.pos.relative(con.dir.getOpposite()))) return true;
        }
        return false;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineMiningLaserBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) be.serverTick(serverLevel, pos);
    }

    private void serverTick(ServerLevel world, BlockPos pos) {

        this.trySubscribe(world, pos.getX(), pos.getY() + 2, pos.getZ(), Direction.UP);

        for (DirPos con : getConPos()) {
            this.tryProvide(tank, world, con.pos, con.dir);
        }

        chargeFromBatterySlot(SLOT_BATTERY);

        // Fortschritt zuruecksetzen, wenn sich das Ziel aendert
        if (lastTargetX != targetX || lastTargetY != targetY || lastTargetZ != targetZ)
            breakProgress = 0;

        lastTargetX = targetX;
        lastTargetY = targetY;
        lastTargetZ = targetZ;

        boolean prevRedstone = this.redstonePowered;
        this.redstonePowered = this.isMultiblockRedstonePowered(world);

        if (prevRedstone != this.redstonePowered) {
            this.setChanged();
        }

        boolean shouldRun = this.isOn && !this.redstonePowered;

        if (shouldRun) {

            upgradeManager.checkSlots(inventory, UPGRADE_START, UPGRADE_START + UPGRADE_COUNT - 1, VALID_UPGRADES);
            int cycles = 1 + upgradeManager.getLevel(UpgradeType.OVERDRIVE);
            int speed = 1 + upgradeManager.getLevel(UpgradeType.SPEED);
            int range = 1 + upgradeManager.getLevel(UpgradeType.EFFECT) * 2;
            int fortune = upgradeManager.getLevel(UpgradeType.FORTUNE);
            int consumption = MachineMiningLaserBlockEntity.consumption
                    - (MachineMiningLaserBlockEntity.consumption * upgradeManager.getLevel(UpgradeType.POWER) / 16)
                    + (MachineMiningLaserBlockEntity.consumption * upgradeManager.getLevel(UpgradeType.SPEED) / 16);

            for (int i = 0; i < cycles; i++) {

                if (energy < consumption) {
                    beam = false;
                    break;
                }

                energy -= consumption;

                // Original: targetY <= 0 -> neu ab yCoord - 2 (hier die Weltuntergrenze)
                if (targetY <= world.getMinBuildHeight())
                    targetY = pos.getY() - 2;

                scan(world, pos, range);

                BlockPos target = new BlockPos(targetX, targetY, targetZ);
                BlockState state = world.getBlockState(target);

                if (state.liquid()) {
                    world.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
                    buildDam(world);
                    continue;
                }

                if (beam && canBreak(world, state, target)) {

                    breakProgress += getBreakSpeed(world, speed);
                    clientBreakProgress = Math.min(breakProgress, 1);

                    if (breakProgress < 1) {
                        world.destroyBlockProgress(-1, target, (int) Math.floor(breakProgress * 10));
                    } else {
                        breakBlock(world, fortune);
                        buildDam(world);
                    }
                }
            }
        } else {
            targetY = pos.getY() - 2;
            beam = false;
        }

        for (DirPos con : getConPos()) {
            this.tryFillContainer(world, con.pos);
        }

        setChanged();
        sendUpdateToClient();
    }

    private void buildDam(Level world) {
        for (Direction dir : Direction.values()) {
            BlockPos p = new BlockPos(targetX + dir.getStepX(), targetY + dir.getStepY(), targetZ + dir.getStepZ());
            if (world.getBlockState(p).liquid()) world.setBlock(p, ModBlocks.BARRICADE.get().defaultBlockState(), 3);
        }
    }

    private void tryFillContainer(Level world, BlockPos p) {

        Block b = world.getBlockState(p).getBlock();
        if (b != Blocks.CHEST && b != Blocks.TRAPPED_CHEST && b != ModBlocks.CRATE_IRON.get() && b != ModBlocks.CRATE_DESH.get()
                && b != ModBlocks.CRATE_STEEL.get() && b != ModBlocks.SAFE.get() && b != Blocks.HOPPER)
            return;

        var handler = com.hbm_m.api.item.ItemHandlerAccess.getItemHandler(world, p, null);
        if (handler == null)
            return;

        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT; i++) {

            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                int prev = stack.getCount();
                ItemStack rest = stack.copy();
                // Original InventoryUtil.tryAddItemToInventory: erst aufstocken, dann leere Slots
                for (int j = 0; j < handler.getSlots() && !rest.isEmpty(); j++) {
                    if (!handler.getStackInSlot(j).isEmpty()) rest = handler.insertItem(j, rest, false);
                }
                for (int j = 0; j < handler.getSlots() && !rest.isEmpty(); j++) {
                    if (handler.getStackInSlot(j).isEmpty()) rest = handler.insertItem(j, rest, false);
                }
                inventory.setStackInSlot(i, rest);

                if (rest.isEmpty() || rest.getCount() < prev)
                    return;
            }
        }
    }

    private void breakBlock(ServerLevel world, int fortune) {

        BlockPos target = new BlockPos(targetX, targetY, targetZ);
        BlockState state = world.getBlockState(target);
        Block b = state.getBlock();
        boolean normal = true;
        boolean doesBreak = true;

        ItemStack stack = new ItemStack(b);

        if (!stack.isEmpty()) {
            if (hasCrystallizer()) {

                CrystallizerRecipe result = getCrystallizerOutput(world, stack, ModFluids.PEROXIDE.getSource());
                if (result == null) result = getCrystallizerOutput(world, stack, ModFluids.SULFURIC_ACID.getSource());

                if (result != null) {
                    spawn(world, result.getOutput());
                    normal = false;
                }

            } else if (hasCentrifuge()) {

                CentrifugeRecipe recipe = findRecipe(world, ModRecipes.CENTRIFUGE_TYPE.get(), stack);
                if (recipe != null) {
                    for (ItemStack sta : recipe.getOutputs()) {
                        if (sta != null && !sta.isEmpty()) {
                            spawn(world, sta.copy());
                            normal = false;
                        }
                    }
                }

            } else if (hasShredder()) {

                ShredderRecipe recipe = findRecipe(world, ShredderRecipe.Type.INSTANCE, stack);
                ItemStack result = recipe != null ? recipe.getOutput() : ItemStack.EMPTY;
                if (!result.isEmpty() && result.getItem() != ModMaterialItems.item(ModMaterials.SCRAP, MaterialShape.SCRAP)) {
                    spawn(world, result.copy());
                    normal = false;
                }

            } else if (hasSmelter()) {

                Optional<net.minecraft.world.item.crafting.SmeltingRecipe> recipe = RecipeHooks.getRecipeFor(world, RecipeType.SMELTING, stack);
                if (recipe.isPresent()) {
                    ItemStack result = recipe.get().getResultItem(world.registryAccess());
                    if (!result.isEmpty()) {
                        spawn(world, result.copy());
                        normal = false;
                    }
                }
            }
        }

        if (doesBreak) {
            if (normal) {
                // Original dropBlockAsItem(..., fortune): Werkzeug mit Glueck fuer die Beutetabellen
                ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
                if (fortune > 0) com.hbm_m.platform.ItemHooks.setEnchantmentLevel(tool, world, "minecraft:fortune", fortune);
                Block.dropResources(state, world, target, world.getBlockEntity(target), null, tool);
            }
            world.destroyBlock(target, false);
        }

        suckDrops(world);

        if (doesScream()) {
            world.playSound(null, targetX + 0.5, targetY + 0.5, targetZ + 0.5, com.hbm_m.sound.HbmSoundsNT.get("hbm:block.screm"), SoundSource.BLOCKS, 2000.0F, 1.0F);
        }

        breakProgress = 0;
    }

    private void spawn(Level world, ItemStack stack) {
        world.addFreshEntity(new ItemEntity(world, targetX + 0.5, targetY + 0.5, targetZ + 0.5, stack));
    }

    @Nullable
    private static CrystallizerRecipe getCrystallizerOutput(Level world, ItemStack stack, Fluid acid) {
        for (CrystallizerRecipe r : RecipeHooks.getAllRecipes(world, ModRecipes.CRYSTALLIZER_TYPE.get())) {
            if (!r.matchesInput(stack)) continue;
            if (r.getAcid() == null ? acid == ModFluids.PEROXIDE.getSource() : r.getAcid().getFluid() == acid) return r;
        }
        return null;
    }

    @Nullable
    private static <R extends PlatformRecipe> R findRecipe(Level world, RecipeType<R> type, ItemStack stack) {
        RecipeInputWrapper wrapper = new RecipeInputWrapper(new SimpleContainer(stack.copy()));
        for (R r : RecipeHooks.getAllRecipes(world, type)) {
            if (r.matchesRecipe(wrapper, world)) return r;
        }
        return null;
    }

    private static Set<Item> bad;

    private static Set<Item> bad() {
        if (bad == null) bad = Set.of(
                Blocks.DIRT.asItem(),
                Blocks.STONE.asItem(),
                Blocks.COBBLESTONE.asItem(),
                Blocks.SAND.asItem(),
                Blocks.SANDSTONE.asItem(),
                Blocks.GRAVEL.asItem(),
                ModBlocks.BASALT.get().asItem(),
                ModBlocks.STONE_GNEISS.get().asItem(),
                Items.FLINT,
                Items.SNOWBALL,
                Items.WHEAT_SEEDS);
        return bad;
    }

    // "suck"
    private void suckDrops(Level world) {

        int rangeHor = 3;
        int rangeVer = 1;
        boolean nullifier = hasNullifier();

        List<ItemEntity> items = world.getEntitiesOfClass(ItemEntity.class, new AABB(
                targetX + 0.5 - rangeHor, targetY + 0.5 - rangeVer, targetZ + 0.5 - rangeHor,
                targetX + 0.5 + rangeHor, targetY + 0.5 + rangeVer, targetZ + 0.5 + rangeHor));

        for (ItemEntity item : items) {

            if (!item.isAlive()) continue;

            if (nullifier && bad().contains(item.getItem().getItem())) {
                item.discard();
                continue;
            }

            if (item.getItem().getItem() == ModBlocks.ORE_OIL.get().asItem()) {

                tank.setTankType(ModFluids.CRUDE_OIL.getSource());

                tank.setFill(tank.getFill() + 500);
                if (tank.getFill() > tank.getMaxFill())
                    tank.setFill(tank.getMaxFill());

                item.discard();
                continue;
            }

            ItemStack stack = addToOutput(item.getItem().copy());

            if (stack.isEmpty()) {
                item.discard();
            } else {
                item.setItem(stack.copy());
            }
        }

        List<LivingEntity> mobs = world.getEntitiesOfClass(LivingEntity.class, new AABB(
                targetX + 0.5 - 1, targetY + 0.5 - 1, targetZ + 0.5 - 1,
                targetX + 0.5 + 1, targetY + 0.5 + 1, targetZ + 0.5 + 1));

        for (LivingEntity mob : mobs) {
            com.hbm_m.platform.PlatformHooks.setSecondsOnFire(mob, 5);
        }
    }

    /** Original {@code InventoryUtil.tryAddItemToInventory(slots, 9, 29, stack)}. */
    private ItemStack addToOutput(ItemStack toAdd) {
        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT && !toAdd.isEmpty(); i++) {
            ItemStack slot = inventory.getStackInSlot(i);
            if (!slot.isEmpty() && com.hbm_m.platform.PlatformHooks.isSameItemSameTags(slot, toAdd)) {
                int move = Math.min(slot.getMaxStackSize() - slot.getCount(), toAdd.getCount());
                if (move > 0) {
                    ItemStack grown = slot.copy();
                    grown.grow(move);
                    inventory.setStackInSlot(i, grown);
                    toAdd.shrink(move);
                }
            }
        }
        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT && !toAdd.isEmpty(); i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                inventory.setStackInSlot(i, toAdd.copy());
                toAdd = ItemStack.EMPTY;
            }
        }
        return toAdd;
    }

    public double getBreakSpeed(Level world, int speed) {

        BlockPos target = new BlockPos(targetX, targetY, targetZ);
        float hardness = world.getBlockState(target).getDestroySpeed(world, target) * 15 / speed;

        if (hardness == 0)
            return 1;

        return 1 / hardness;
    }

    public void scan(Level world, BlockPos pos, int range) {

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {

                BlockPos p = new BlockPos(x + pos.getX(), targetY, z + pos.getZ());
                BlockState state = world.getBlockState(p);

                if (state.liquid()) {
                    continue;
                }

                if (canBreak(world, state, p)) {
                    targetX = p.getX();
                    targetZ = p.getZ();
                    beam = true;
                    return;
                }
            }
        }

        beam = false;
        targetY--;
    }

    private static boolean canBreak(Level world, BlockState state, BlockPos p) {
        return !state.isAir() && !(state.getBlock() instanceof BlockGasBase) && state.getDestroySpeed(world, p) >= 0
                && !state.liquid() && state.getBlock() != Blocks.BEDROCK;
    }

    private boolean hasUpgrade(Item upgrade) {
        for (int i = UPGRADE_START; i < UPGRADE_START + UPGRADE_COUNT; i++) {
            if (inventory.getStackInSlot(i).getItem() == upgrade) return true;
        }
        return false;
    }

    public int getRange() {
        int range = 1;
        for (int i = UPGRADE_START; i < UPGRADE_START + UPGRADE_COUNT; i++) {
            Item item = inventory.getStackInSlot(i).getItem();
            if (item == ModItems.UPGRADE_EFFECT_1.get()) range += 2;
            else if (item == ModItems.UPGRADE_EFFECT_2.get()) range += 4;
            else if (item == ModItems.UPGRADE_EFFECT_3.get()) range += 6;
        }
        return Math.min(range, 25);
    }

    public boolean hasNullifier() { return hasUpgrade(ModItems.UPGRADE_NULLIFIER.get()); }
    public boolean hasSmelter() { return hasUpgrade(ModItems.UPGRADE_SMELTER.get()); }
    public boolean hasShredder() { return hasUpgrade(ModItems.UPGRADE_SHREDDER.get()); }
    public boolean hasCentrifuge() { return hasUpgrade(ModItems.UPGRADE_CENTRIFUGE.get()); }
    public boolean hasCrystallizer() { return hasUpgrade(ModItems.UPGRADE_CRYSTALLIZER.get()); }
    public boolean doesScream() { return hasUpgrade(ModItems.UPGRADE_SCREM.get()); }

    public int getConsumption() { return consumption; }

    public int getWidth() { return 1 + getRange() * 2; }

    public long getPowerScaled(long i) { return (energy * i) / maxPower; }

    public int getProgressScaled(int i) { return (int) (breakProgress * i); }

    public boolean isOn() { return isOn; }

    public FluidTank getTank() { return tank; }

    /** Original: {@code AuxButtonPacket} des An/Aus-Knopfs. */
    public void toggleOn() {
        isOn = !isOn;
        setChanged();
        sendUpdateToClient();
    }

    /** Original {@code setInventorySlotContents}: Upgrade einstecken macht das Steckgeraeusch. */
    @Override
    protected ModItemStackHandler createInventoryHandler(int size) {
        return new ModItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                ItemStack stack = getStackInSlot(slot);
                if (level != null && !level.isClientSide && slot >= UPGRADE_START && slot < UPGRADE_START + UPGRADE_COUNT
                        && stack.getItem() instanceof ItemMachineUpgrade) {
                    level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5,
                            com.hbm_m.sound.HbmSoundsNT.get("hbm:item.upgradePlug"), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return isItemValidForSlot(slot, stack);
            }
        };
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return isEnergyProviderItem(stack);
        if (slot >= UPGRADE_START && slot < UPGRADE_START + UPGRADE_COUNT) return stack.getItem() instanceof ItemMachineUpgrade;
        return false;
    }

    // ==================== Fluid ====================

    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tank }; }
    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { tank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tank.writeToNBT(tag, "oil");
        tag.putLong("power", energy);
        tag.putBoolean("isOn", isOn);
        // Client-Sync (Original serialize)
        tag.putInt("ltX", lastTargetX);
        tag.putInt("ltY", lastTargetY);
        tag.putInt("ltZ", lastTargetZ);
        tag.putInt("tX", targetX);
        tag.putInt("tY", targetY);
        tag.putInt("tZ", targetZ);
        tag.putBoolean("beam", beam);
        tag.putDouble("progress", clientBreakProgress);
        tag.putBoolean("redstone", redstonePowered);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tank.readFromNBT(tag, "oil");
        energy = tag.getLong("power");
        isOn = tag.getBoolean("isOn");
        redstonePowered = false;
    }

    @Override
    protected void applyClientUpdate(@NotNull CompoundTag tag) {
        super.applyClientUpdate(tag);
        lastTargetX = tag.getInt("ltX");
        lastTargetY = tag.getInt("ltY");
        lastTargetZ = tag.getInt("ltZ");
        targetX = tag.getInt("tX");
        targetY = tag.getInt("tY");
        targetZ = tag.getInt("tZ");
        beam = tag.getBoolean("beam");
        breakProgress = tag.getDouble("progress");
        redstonePowered = tag.getBoolean("redstone");
    }

    // ==================== Upgrades ====================

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE || type == UpgradeType.EFFECT || type == UpgradeType.FORTUNE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(com.hbm_m.interfaces.IUpgradeInfoProvider.getStandardLabel(getBlockState().getBlock()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (100 - 100 / (level + 1)) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (100 * level / 16) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (100 * level / 16) + "%").withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.EFFECT) {
            info.add(Component.translatable(KEY_RANGE, "+" + (2 * level) + "m").withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.FORTUNE) {
            info.add(Component.translatable(KEY_FORTUNE, "+" + level).withStyle(ChatFormatting.GREEN));
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
        return Component.translatable("container.hbm_m.mining_laser");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MachineMiningLaserMenu(id, inventory, this);
    }

    /** Original: {@code INFINITE_EXTENT_AABB}, Sichtweite 256. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }
}
