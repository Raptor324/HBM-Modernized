package com.hbm_m.blockentity.machines;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.nature.OreBedrockBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineMiningDrillMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.recipe.ModRecipes;
import com.hbm_m.recipe.ShredderRecipe;
import com.hbm_m.worldgen.BedrockOreDensity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;

/**
 * Port des Large Mining Drill (1.7.10: {@code MachineExcavator}/{@code TileEntityMachineExcavator}).
 * Nachgebaut anhand der Original-Java-Quelle (vom User bereitgestellt) - siehe
 * {@link com.hbm_m.client.render.implementations.MachineMiningDrillRenderer} fuer die Teleskop-/Pendel-
 * Animation und {@link com.hbm_m.block.machines.MachineMiningDrillBlock#getStructureHelper()} fuer die
 * 7x4x7-Struktur.
 * <p>
 * Bewusst NICHT 1:1 uebernommen (Phase 2, siehe Gespraech): Conveyor-Belt-Ausgabe (kein
 * IConveyorBelt-System in diesem Port), Bedrock-Erz/Saeure-Sonderfall (kein BlockDepth/TileEntity-
 * BedrockOre-Aequivalent). Ausgabe geht nur ins interne Puffer-Inventar bzw. als Item-Entity in die Welt,
 * wenn der Puffer voll ist.
 */
public class MachineMiningDrillBlockEntity extends BaseMachineBlockEntity {

    public static final int SLOT_DRILLBIT = 0;
    private static final int OUTPUT_START = 1;
    private static final int OUTPUT_COUNT = 9;
    public static final int SLOT_BATTERY = 10;
    private static final int SLOT_COUNT = 11;

    private static final long CAPACITY = 1_000_000L;
    private static final long MAX_RECEIVE = 2_000L;
    private static final long ENERGY_PER_TICK = 200L;

    /** Eigenschaften je Drillbit-Tier, 1:1 aus {@code ItemDrillbit.EnumDrillType} des Originals. */
    private record DrillProps(double speed, int tier, int fortune, boolean vein, boolean silk) {}

    private static final Map<Item, DrillProps> DRILL_PROPS = Map.ofEntries(
            Map.entry(ModItems.DRILLBIT_STEEL.get(),          new DrillProps(1.0D, 1, 0, false, false)),
            Map.entry(ModItems.DRILLBIT_STEEL_DIAMOND.get(),  new DrillProps(1.0D, 1, 2, false, true)),
            Map.entry(ModItems.DRILLBIT_HSS.get(),            new DrillProps(1.2D, 2, 0, true, false)),
            Map.entry(ModItems.DRILLBIT_HSS_DIAMOND.get(),    new DrillProps(1.2D, 2, 3, true, true)),
            Map.entry(ModItems.DRILLBIT_DESH.get(),           new DrillProps(1.5D, 3, 1, true, true)),
            Map.entry(ModItems.DRILLBIT_DESH_DIAMOND.get(),   new DrillProps(1.5D, 3, 4, true, true)),
            Map.entry(ModItems.DRILLBIT_TCALLOY.get(),        new DrillProps(2.0D, 4, 1, true, true)),
            Map.entry(ModItems.DRILLBIT_TCALLOY_DIAMOND.get(),new DrillProps(2.0D, 4, 4, true, true)),
            Map.entry(ModItems.DRILLBIT_FERRO.get(),          new DrillProps(2.5D, 5, 1, true, true)),
            Map.entry(ModItems.DRILLBIT_FERRO_DIAMOND.get(),  new DrillProps(2.5D, 5, 4, true, true))
    );

    private static final int RADIUS = 1; // kein Effect-Upgrade in diesem Port -> immer ring=1 (3x3)
    private static final Set<Block> IGNORED_BLOCKS = Set.of(Blocks.BEDROCK, Blocks.BARRIER);

    private boolean operational = false;
    private int targetDepth = 0;
    private int ticksWorked = 0;
    private int currentTicksToWork = 100;

    public boolean enableDrill = false;
    public boolean enableCrusher = false;
    public boolean enableWalling = false;
    public boolean enableVeinMiner = false;
    public boolean enableSilkTouch = false;

    /** Client-seitige Animation (siehe MachineMiningDrillRenderer): Rotation Bohrkopf/Crusher, Teleskop-Tiefe. */
    public float drillRotation, prevDrillRotation;
    public float drillExtension, prevDrillExtension;
    public float crusherRotation, prevCrusherRotation;

    private final HashSet<BlockPos> veinRecursionBrake = new HashSet<>();

    /** Bohr-Fluid-Tank fuer Bedrock-Erz (Wasser/Schwefelsaeure/Solvent, siehe {@link BedrockOreDensity}). */
    private final FluidTank tank = new FluidTank(ModFluids.NONE.getSource(), 4000);

    public MachineMiningDrillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINING_DRILL_BE.get(), pos, state, SLOT_COUNT, CAPACITY, MAX_RECEIVE, 0L);
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return tank.getCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    public static void tick(Level level, BlockPos pos, BlockState state, MachineMiningDrillBlockEntity be) {
        if (level.isClientSide) {
            be.clientTick();
        } else {
            be.serverTick((ServerLevel) level, pos);
        }
    }

    private void clientTick() {
        prevDrillExtension = drillExtension;
        if (drillExtension != targetDepth) {
            float diff = Math.abs(drillExtension - targetDepth);
            float speed = Math.max(0.15F, diff / 10F);
            if (diff <= speed) {
                drillExtension = targetDepth;
            } else {
                float sig = Math.signum(drillExtension - targetDepth);
                drillExtension -= sig * speed;
            }
        }

        prevDrillRotation = drillRotation;
        prevCrusherRotation = crusherRotation;

        if (operational) {
            drillRotation += 15F;
            if (enableCrusher) {
                crusherRotation += 15F;
            }
        }

        if (drillRotation >= 360F) {
            drillRotation -= 360F;
            prevDrillRotation -= 360F;
        }
        if (crusherRotation >= 360F) {
            crusherRotation -= 360F;
            prevCrusherRotation -= 360F;
        }
    }

    private void serverTick(ServerLevel level, BlockPos pos) {
        ensureNetworkInitialized();
        chargeFromBatterySlot(SLOT_BATTERY);

        boolean wasOperational = operational;
        operational = canWork(level, pos);

        if (operational) {
            setEnergyStored(getEnergyStored() - ENERGY_PER_TICK);

            if (tryDrill(level, pos)) {
                targetDepth++;
                setChanged();
                sendUpdateToClient();

                if (targetDepth > maxDepth(level, pos)) {
                    enableDrill = false;
                }
            }
        } else {
            ticksWorked = 0;

            // Ausgeschaltet (nicht nur pausiert) -> Bohrkopf faehrt wieder komplett hoch.
            if (!enableDrill && targetDepth != 0) {
                targetDepth = 0;
                setChanged();
                sendUpdateToClient();
            }
        }

        if (wasOperational != operational) {
            setChanged();
            sendUpdateToClient();
        }
    }

    private boolean canWork(Level level, BlockPos pos) {
        if (!enableDrill) return false;
        if (getEnergyStored() < ENERGY_PER_TICK) return false;
        if (getInstalledDrillProps() == null) return false;
        return targetDepth <= maxDepth(level, pos);
    }

    private int maxDepth(Level level, BlockPos pos) {
        return pos.getY() - level.getMinBuildHeight() - 4;
    }

    private int getTargetY(BlockPos pos) {
        return pos.getY() - 1 - targetDepth;
    }

    private DrillProps getInstalledDrillProps() {
        ItemStack stack = inventory.getStackInSlot(SLOT_DRILLBIT);
        if (stack.isEmpty()) return null;
        return DRILL_PROPS.get(stack.getItem());
    }

    private boolean shouldIgnoreBlock(Level level, BlockState state, BlockPos pos) {
        if (state.isAir()) return true;
        if (IGNORED_BLOCKS.contains(state.getBlock())) return true;
        if (!state.getFluidState().isEmpty()) return true;
        return state.getDestroySpeed(level, pos) < 0;
    }

    /**
     * Bohrt einen Ring (bei uns immer 3x3, radius=1 - kein Effect-Upgrade in diesem Port).
     * @return true wenn der Ring frei (fertig abgebaut) ist und die Tiefe erhoeht werden soll.
     */
    private boolean tryDrill(ServerLevel level, BlockPos pos) {
        int y = getTargetY(pos);
        DrillProps drill = getInstalledDrillProps();

        boolean ignoreAll = true;
        float combinedHardness = 0F;

        for (int x = pos.getX() - RADIUS; x <= pos.getX() + RADIUS; x++) {
            for (int z = pos.getZ() - RADIUS; z <= pos.getZ() + RADIUS; z++) {
                BlockPos target = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(target);
                if (shouldIgnoreBlock(level, state, target)) continue;

                ignoreAll = false;
                combinedHardness += state.getDestroySpeed(level, target);
            }
        }

        if (ignoreAll) {
            if (enableWalling) buildWall(level, pos, y);
            ticksWorked = 0;
            return true;
        }

        ticksWorked++;
        currentTicksToWork = Math.max(1, (int) Math.ceil(combinedHardness / drill.speed()));

        if (ticksWorked >= currentTicksToWork) {
            breakRing(level, pos, y, drill);
            ticksWorked = 0;
        }

        return false;
    }

    private void breakRing(ServerLevel level, BlockPos pos, int y, DrillProps drill) {
        for (int x = pos.getX() - RADIUS; x <= pos.getX() + RADIUS; x++) {
            for (int z = pos.getZ() - RADIUS; z <= pos.getZ() + RADIUS; z++) {
                BlockPos target = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(target);
                if (shouldIgnoreBlock(level, state, target)) continue;

                if (enableVeinMiner && drill.vein() && isOre(state)) {
                    veinRecursionBrake.clear();
                    breakVeinRecursively(level, target, state.getBlock(), drill, 64);
                } else {
                    mineSingleBlock(level, target, drill);
                }
            }
        }

        level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    private boolean isOre(BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.create(
                net.minecraft.resources.ResourceLocation.parse("forge:ores")))
                || state.getBlock().builtInRegistryHolder().key().location().getPath().contains("ore");
    }

    private void breakVeinRecursively(ServerLevel level, BlockPos pos, Block match, DrillProps drill, int depth) {
        if (depth < 0 || veinRecursionBrake.contains(pos)) return;
        veinRecursionBrake.add(pos);

        if (level.getBlockState(pos).getBlock() != match) return;

        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (level.getBlockState(neighbor).getBlock() == match) {
                breakVeinRecursively(level, neighbor, match, drill, depth - 1);
            }
        }

        mineSingleBlock(level, pos, drill);
    }

    private void mineSingleBlock(ServerLevel level, BlockPos pos, DrillProps drill) {
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof com.hbm_m.block.nature.OreBedrockBlock) {
            mineBedrockOre(level, pos, drill);
            return;
        }

        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        boolean silk = enableSilkTouch && drill.silk();
        if (silk) {
            tool.enchant(Enchantments.SILK_TOUCH, 1);
        } else if (drill.fortune() > 0) {
            tool.enchant(Enchantments.BLOCK_FORTUNE, drill.fortune());
        }

        LootParams.Builder builder = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, tool)
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, level.getBlockEntity(pos));

        List<ItemStack> drops = new ArrayList<>(state.getDrops(builder));
        level.removeBlock(pos, false);

        if (enableCrusher) {
            drops = crushDrops(level, drops);
        }

        for (ItemStack drop : drops) {
            insertOrDrop(pos, drop);
        }
    }

    /**
     * 1:1-Aequivalent zu {@code TileEntityMachineExcavator.collectBedrock}: Drillbit-Tier muss
     * mindestens {@code ore.tier} sein, und bei Tier 2+ muss der Tank den passenden Fluid-Typ in
     * ausreichender Menge enthalten - sonst bleibt der Block (noch) stehen. Das gewonnene Item ist
     * das generische {@code bedrock_ore_base}, mit den 6 Kategorie-Dichten der Fundposition als NBT
     * getaggt (analog {@code ItemBedrockOreBase.setOreAmount}), skaliert mit Fortune.
     */
    private void mineBedrockOre(ServerLevel level, BlockPos pos, DrillProps drill) {
        if (!(level.getBlockEntity(pos) instanceof OreBedrockBlockEntity ore)) return;
        if (drill.tier() < ore.tier) return;

        if (ore.acidAmountMb > 0) {
            if (tank.getStoredFluid() != ore.acidType || tank.getFluidAmountMb() < ore.acidAmountMb) return;
            tank.drainMb(ore.acidAmountMb);
        }

        level.removeBlock(pos, false);

        double mult = 1.0D + drill.fortune() * 0.1D;
        ItemStack drop = new ItemStack(ModItems.BEDROCK_ORE_BASE.get());
        CompoundTag nbt = new CompoundTag();
        for (BedrockOreDensity.Type type : BedrockOreDensity.Type.values()) {
            double density = BedrockOreDensity.getDensity(pos.getX(), pos.getZ(), type) * mult;
            nbt.putDouble(type.name().toLowerCase(java.util.Locale.ROOT), density);
        }
        drop.setTag(nbt);

        insertOrDrop(pos, drop);
    }

    private List<ItemStack> crushDrops(ServerLevel level, List<ItemStack> drops) {
        List<ItemStack> result = new ArrayList<>(drops.size());
        for (ItemStack stack : drops) {
            SimpleContainer container = new SimpleContainer(1);
            container.setItem(0, new ItemStack(stack.getItem(), 1));
            Optional<ShredderRecipe> recipe = level.getRecipeManager()
                    .getRecipeFor(ModRecipes.SHREDDER_TYPE.get(), container, level);
            if (recipe.isPresent()) {
                ItemStack crushed = recipe.get().getOutput();
                crushed.setCount(crushed.getCount() * stack.getCount());
                result.add(crushed);
            } else {
                result.add(stack);
            }
        }
        return result;
    }

    /** Baut eine duenne Barricade-Wand einen Ring ausserhalb des Bohrbereichs (nur ersetzbare/fluessige Bloecke). */
    private void buildWall(ServerLevel level, BlockPos pos, int y) {
        int ring = RADIUS + 1;
        for (int x = pos.getX() - ring; x <= pos.getX() + ring; x++) {
            for (int z = pos.getZ() - ring; z <= pos.getZ() + ring; z++) {
                if (x != pos.getX() - ring && x != pos.getX() + ring && z != pos.getZ() - ring && z != pos.getZ() + ring) {
                    continue; // nur der aussere Rand
                }
                BlockPos target = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(target);
                if (state.canBeReplaced() || !state.getFluidState().isEmpty()) {
                    level.setBlockAndUpdate(target, ModBlocks.BARRICADE.get().defaultBlockState());
                }
            }
        }
    }

    private void insertOrDrop(BlockPos minedAt, ItemStack toInsert) {
        if (toInsert.isEmpty()) return;

        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT && !toInsert.isEmpty(); i++) {
            ItemStack slotStack = inventory.getStackInSlot(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameTags(slotStack, toInsert)) {
                int room = slotStack.getMaxStackSize() - slotStack.getCount();
                int move = Math.min(room, toInsert.getCount());
                if (move > 0) {
                    slotStack.grow(move);
                    toInsert.shrink(move);
                }
            }
        }
        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT && !toInsert.isEmpty(); i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                inventory.setStackInSlot(i, toInsert.copy());
                toInsert.setCount(0);
            }
        }

        if (!toInsert.isEmpty() && level != null) {
            Block.popResource(level, minedAt, toInsert);
        }
    }

    public void receiveToggle(String key) {
        switch (key) {
            case "drill" -> enableDrill = !enableDrill;
            case "crusher" -> enableCrusher = !enableCrusher;
            case "walling" -> enableWalling = !enableWalling;
            case "veinminer" -> enableVeinMiner = !enableVeinMiner;
            case "silktouch" -> enableSilkTouch = !enableSilkTouch;
            default -> { return; }
        }
        setChanged();
        sendUpdateToClient();
    }

    public boolean canVeinMine() {
        DrillProps drill = getInstalledDrillProps();
        return enableVeinMiner && drill != null && drill.vein();
    }

    public boolean canSilkTouch() {
        DrillProps drill = getInstalledDrillProps();
        return enableSilkTouch && drill != null && drill.silk();
    }

    public int getProgressScaled(int scale) {
        if (currentTicksToWork <= 0) return 0;
        return Math.min(scale, ticksWorked * scale / currentTicksToWork);
    }

    public int getProgress() { return ticksWorked; }
    public int getMaxProgress() { return currentTicksToWork; }
    public int getDrillDepth() { return targetDepth; }
    public boolean isActive() { return operational; }
    public boolean isOperational() { return operational; }
    public boolean hasDrillbitInstalled() { return !inventory.getStackInSlot(SLOT_DRILLBIT).isEmpty(); }
    public long getEnergyPerTick() { return ENERGY_PER_TICK; }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("target_depth", targetDepth);
        tag.putBoolean("enable_drill", enableDrill);
        tag.putBoolean("enable_crusher", enableCrusher);
        tag.putBoolean("enable_walling", enableWalling);
        tag.putBoolean("enable_veinminer", enableVeinMiner);
        tag.putBoolean("enable_silktouch", enableSilkTouch);
        tag.putBoolean("operational", operational);
        tank.writeToNBT(tag, "tank");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        tank.readFromNBT(tag, "tank");
        targetDepth = tag.getInt("target_depth");
        enableDrill = tag.getBoolean("enable_drill");
        enableCrusher = tag.getBoolean("enable_crusher");
        enableWalling = tag.getBoolean("enable_walling");
        enableVeinMiner = tag.getBoolean("enable_veinminer");
        enableSilkTouch = tag.getBoolean("enable_silktouch");
        operational = tag.getBoolean("operational");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.mining_drill");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_DRILLBIT) {
            return DRILL_PROPS.containsKey(stack.getItem());
        }
        if (slot == SLOT_BATTERY) {
            return isEnergyProviderItem(stack);
        }
        return false; // Ausgabe-Slots: kein manuelles Einlegen.
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineMiningDrillMenu.create(id, inventory, this);
    }

    /**
     * Der Bohrkopf/Shaft haengt bis zu {@code targetDepth} Bloecke unterhalb der Maschine (siehe
     * {@link MachineMiningDrillRenderer} bzw. dessen Ur-Aequivalent {@code RenderExcavator}). Ohne
     * diese Erweiterung kuerzt Minecraft den Cull-Check auf die 1x1x1-Blockbox und der Bohrkopf
     * verschwindet, sobald die Maschine selbst nicht mehr im Sichtfeld ist (z.B. wenn man tief im
     * eigenen Schacht steht).
     */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        double depth = targetDepth + 4.0D;
        return super.getRenderBoundingBox().expandTowards(0, -depth, 0);
    }
}
