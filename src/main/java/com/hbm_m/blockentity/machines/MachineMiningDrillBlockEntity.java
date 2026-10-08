package com.hbm_m.blockentity.machines;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.nature.OreBedrockBlockEntity;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineMiningDrillMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ItemDrillbit;
import com.hbm_m.item.industrial.ItemDrillbit.EnumDrillType;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;
import com.hbm_m.recipe.ModRecipes;
import com.hbm_m.recipe.ShredderRecipe;
import com.hbm_m.worldgen.BedrockOreDensity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code TileEntityMachineExcavator}: grosser Bergbaubohrer. Bohrt Ring um Ring ({@code 1 + 2 * Effekt}) bis
 * vier Bloecke ueber dem Kern nach unten, Arbeitszeit = Summe der Haerten / Bohrkopftempo. Schalter: Bohrer, Brecher
 * (Schredderrezepte), Wand (Barrikaden statt Fluessigkeiten/Luft), Adernabbau (rekursiv, 10 tief, Ore-Tag) und
 * Behutsamkeit. Grundgestein-Erz wird je Stufe des Bohrkopfs (ggf. mit Saeure aus dem Tank) abgebaut. Alles landet
 * zuerst in einem Behaelter, dann auf einem Foerderband vor der Rutsche (4 vor, 3 unter dem Kern), sonst im 9er-Puffer.
 */
public class MachineMiningDrillBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2,
        com.hbm_m.api.tile.IControlReceiver, com.hbm_m.interfaces.IUpgradeInfoProvider {

    /** Original: 0 Batterie, 1 Fluidkennung, 2-3 Aufwertungen, 4 Bohrkopf, 5-13 Puffer. */
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_FLUID_ID = 1;
    public static final int SLOT_UPGRADE_1 = 2;
    public static final int SLOT_UPGRADE_2 = 3;
    public static final int SLOT_DRILLBIT = 4;
    public static final int OUTPUT_START = 5;
    public static final int OUTPUT_END = 13;
    public static final int INVENTORY_SIZE = 14;

    public static final long maxPower = 1_000_000;
    public boolean operational = false;

    public boolean enableDrill = false;
    public boolean enableCrusher = false;
    public boolean enableWalling = false;
    public boolean enableVeinMiner = false;
    public boolean enableSilkTouch = false;

    protected int ticksWorked = 0;
    protected int targetDepth = 0; // 0 ist der erste Block unter der Nullposition
    protected boolean bedrockDrilling = false;

    public float drillRotation = 0F;
    public float prevDrillRotation = 0F;
    public float drillExtension = 0F;
    public float prevDrillExtension = 0F;
    public float crusherRotation = 0F;
    public float prevCrusherRotation = 0F;
    public int chuteTimer = 0;

    public double speed = 1.0D;
    public final long baseConsumption = 10_000L;
    public long consumption = baseConsumption;

    public final FluidTank tank = new FluidTank(ModFluids.NONE.getSource(), 16_000);

    public final UpgradeManager upgradeManager = new UpgradeManager();

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = new EnumMap<>(UpgradeType.class);
    static {
        VALID_UPGRADES.put(UpgradeType.SPEED, 3);
        VALID_UPGRADES.put(UpgradeType.POWER, 3);
        VALID_UPGRADES.put(UpgradeType.EFFECT, 3);
    }

    public MachineMiningDrillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINING_DRILL_BE.get(), pos, state, INVENTORY_SIZE, maxPower, maxPower, 0L);
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(DummyableMachineBlock.FACING) ? state.getValue(DummyableMachineBlock.FACING) : Direction.NORTH;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineMiningDrillBlockEntity be) {
        be.updateEntity(level, pos);
    }

    private void updateEntity(Level world, BlockPos pos) {

        // muss auch clientseitig laufen, fuer die GUI
        upgradeManager.checkSlots(inventory, SLOT_UPGRADE_1, SLOT_UPGRADE_2, VALID_UPGRADES);
        int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = upgradeManager.getLevel(UpgradeType.POWER);

        consumption = baseConsumption * (1 + speedLevel);
        consumption /= (1 + powerLevel);

        if (world instanceof ServerLevel server) {

            ItemStack[] slots = slotArray();
            if (tank.setType(SLOT_FLUID_ID, slots)) applySlotArray(slots);

            if (world.getGameTime() % 20 == 0) {
                tryEjectBuffer(server);

                for (DirPos con : getConPos()) {
                    this.trySubscribe(server, con.pos.getX(), con.pos.getY(), con.pos.getZ(), con.dir);
                    this.trySubscribe(tank.getTankType(), server, con.pos, con.dir);
                }
            }

            if (chuteTimer > 0) chuteTimer--;

            chargeFromBatterySlot(SLOT_BATTERY);
            this.operational = false;
            int radiusLevel = upgradeManager.getLevel(UpgradeType.EFFECT);

            EnumDrillType type = this.getInstalledDrill();
            if (this.enableDrill && type != null && getEnergyStored() >= this.getPowerConsumption()) {

                operational = true;
                setEnergyStored(getEnergyStored() - this.getPowerConsumption());

                this.speed = type.speed;
                this.speed *= (1 + speedLevel / 2D);

                int maxDepth = pos.getY() - 4 - world.getMinBuildHeight();

                if ((bedrockDrilling || targetDepth <= maxDepth) && tryDrill(server, pos, 1 + radiusLevel * 2)) {
                    targetDepth++;

                    if (targetDepth > maxDepth) {
                        this.enableDrill = false;
                    }
                }
            } else {
                this.targetDepth = 0;
            }

            setChanged();
            sendUpdateToClient();

        } else {

            this.prevDrillExtension = this.drillExtension;

            if (this.drillExtension != this.targetDepth) {
                float diff = Math.abs(this.drillExtension - this.targetDepth);
                float speed = Math.max(0.15F, diff / 10F);

                if (diff <= speed) {
                    this.drillExtension = this.targetDepth;
                } else {
                    float sig = Math.signum(this.drillExtension - this.targetDepth);
                    this.drillExtension -= sig * speed;
                }
            }

            this.prevDrillRotation = this.drillRotation;
            this.prevCrusherRotation = this.crusherRotation;

            if (this.operational) {
                this.drillRotation += 15F;

                if (this.enableCrusher) {
                    this.crusherRotation += 15F;
                }
            }

            if (this.drillRotation >= 360F) {
                this.drillRotation -= 360F;
                this.prevDrillRotation -= 360F;
            }

            if (this.crusherRotation >= 360F) {
                this.crusherRotation -= 360F;
                this.prevCrusherRotation -= 360F;
            }
        }
    }

    /** Anschluss samt Richtung (Original {@code DirPos}). */
    private record DirPos(BlockPos pos, Direction dir) { }

    protected DirPos[] getConPos() {
        Direction dir = facing();
        Direction rot = dir.getClockWise();
        BlockPos p = worldPosition.above();

        return new DirPos[] {
                new DirPos(p.relative(dir, 4).relative(rot), dir),
                new DirPos(p.relative(dir, 4).relative(rot, -1), dir),
                new DirPos(p.relative(rot, 4), rot),
                new DirPos(p.relative(rot, -4), rot.getOpposite())
        };
    }

    protected int getY() {
        return worldPosition.getY() - targetDepth - 4;
    }

    /** Original: Ring fuer Ring; true, wenn alle Ringe frei sind und der Bohrer tiefer gehen soll. */
    protected boolean tryDrill(ServerLevel world, BlockPos core, int radius) {
        int y = getY();

        if (targetDepth == 0 || y == world.getMinBuildHeight()) {
            radius = 1;
        }

        for (int ring = 1; ring <= radius; ring++) {

            boolean ignoreAll = true;
            float combinedHardness = 0F;
            BlockPos bedrockOre = null;
            bedrockDrilling = false;

            outer:
            for (int x = core.getX() - ring; x <= core.getX() + ring; x++) {
                for (int z = core.getZ() - ring; z <= core.getZ() + ring; z++) {

                    if (ring == 1 || (x == core.getX() - ring || x == core.getX() + ring || z == core.getZ() - ring || z == core.getZ() + ring)) {

                        BlockPos p = new BlockPos(x, y, z);
                        BlockState b = world.getBlockState(p);

                        if (b.getBlock() == ModBlocks.ORE_BEDROCK.get()) {
                            combinedHardness = 5 * 60 * 20;
                            bedrockOre = p;
                            bedrockDrilling = true;
                            enableCrusher = false;
                            ignoreAll = false;
                            break outer;
                        }

                        // Tiefengestein schaltet den Bohrer ab
                        if (isDepthRock(b)) {
                            this.enableDrill = false;
                        }

                        if (shouldIgnoreBlock(world, b, p)) continue;

                        ignoreAll = false;

                        combinedHardness += b.getDestroySpeed(world, p);
                    }
                }
            }

            if (!ignoreAll) {
                ticksWorked++;

                int ticksToWork = (int) Math.ceil(combinedHardness / this.speed);

                if (ticksWorked >= ticksToWork) {

                    if (bedrockOre == null) {
                        breakBlocks(world, core, ring);
                        buildWall(world, core, ring + 1, ring == radius && this.enableWalling);
                        if (ring == radius) mineOuterOres(world, core, ring + 1);
                        tryCollect(world, core, radius + 1);
                    } else {
                        collectBedrock(world, bedrockOre);
                    }
                    ticksWorked = 0;
                }

                return false;
            } else {
                tryCollect(world, core, radius + 1);
            }
        }

        buildWall(world, core, radius + 1, this.enableWalling);
        ticksWorked = 0;
        return true;
    }

    /** Original {@code instanceof BlockDepth}: das Tiefengestein und seine Erze ({@code depth_*}). */
    private static boolean isDepthRock(BlockState b) {
        net.minecraft.resources.ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b.getBlock());
        return id.getNamespace().equals(com.hbm_m.lib.RefStrings.MODID) && (id.getPath().startsWith("depth_") || id.getPath().startsWith("cluster_depth_"));
    }

    protected void collectBedrock(ServerLevel world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof OreBedrockBlockEntity ore)) return;

        if (ore.resource == null || ore.resource.isEmpty()) return;
        if (ore.tier > this.getInstalledDrill().tier) return;
        if (ore.acidAmountMb > 0) {

            if (ore.acidType != tank.getTankType() || ore.acidAmountMb > tank.getFill()) return;

            tank.setFill(tank.getFill() - ore.acidAmountMb);
        }

        ItemStack stack = ore.resource.copy();
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(stack);

        if (stack.getItem() == ModItems.BEDROCK_ORE_BASE.get()) {
            double mult = 1D + this.getInstalledDrill().fortune * 0.1D;
            CompoundTag nbt = new CompoundTag();
            for (BedrockOreDensity.Type type : BedrockOreDensity.Type.values()) {
                nbt.putDouble(type.name().toLowerCase(java.util.Locale.ROOT), BedrockOreDensity.getDensity(pos.getX(), pos.getZ(), type) * mult);
            }
            PlatformHooks.setItemTag(stack, nbt);
        }

        Direction dir = facing();
        BlockPos chute = worldPosition.relative(dir, 4).below(3);

        supplyContainer(world, chute, stacks, dir.getOpposite());

        if (stack.getCount() <= 0) return;

        supplyConveyor(world, chute, stacks);

        if (stack.getCount() <= 0) return;

        for (int i = OUTPUT_START; i <= OUTPUT_END; i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (!s.isEmpty() && s.getCount() < s.getMaxStackSize() && PlatformHooks.isSameItemSameTags(stack, s)) {
                int toAdd = Math.min(s.getMaxStackSize() - s.getCount(), stack.getCount());
                ItemStack grown = s.copy();
                grown.grow(toAdd);
                inventory.setStackInSlot(i, grown);
                stack.shrink(toAdd);

                chuteTimer = 40;

                if (stack.getCount() <= 0) {
                    return;
                }
            }
        }

        for (int i = OUTPUT_START; i <= OUTPUT_END; i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                chuteTimer = 40;
                inventory.setStackInSlot(i, stack.copy());
                return;
            }
        }
    }

    /** bricht alle Bloecke des Rings ab und laesst sie fallen */
    protected void breakBlocks(ServerLevel world, BlockPos core, int ring) {
        int y = getY();

        for (int x = core.getX() - ring; x <= core.getX() + ring; x++) {
            for (int z = core.getZ() - ring; z <= core.getZ() + ring; z++) {

                if (ring == 1 || (x == core.getX() - ring || x == core.getX() + ring || z == core.getZ() - ring || z == core.getZ() + ring)) {

                    BlockPos p = new BlockPos(x, y, z);
                    if (!this.shouldIgnoreBlock(world, world.getBlockState(p), p)) {
                        tryMineAtLocation(world, p);
                    }
                }
            }
        }
    }

    public void tryMineAtLocation(ServerLevel world, BlockPos pos) {

        BlockState b = world.getBlockState(pos);

        if (this.enableVeinMiner && this.getInstalledDrill().vein) {

            if (isOre(b)) {
                minX = pos.getX();
                minY = pos.getY();
                minZ = pos.getZ();
                maxX = pos.getX();
                maxY = pos.getY();
                maxZ = pos.getZ();
                breakRecursively(world, pos, 10);
                recursionBrake.clear();

                // alle abgebauten Teile an die letzte bohrbare Stelle im Sammelbereich holen
                List<ItemEntity> items = world.getEntitiesOfClass(ItemEntity.class, new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1));
                for (ItemEntity item : items) item.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

                return;
            }
        }
        breakSingleBlock(world, b, pos);
    }

    /** Original: Ore-Dictionary-Name beginnt mit "ore" - hier der Erz-Tag. */
    protected boolean isOre(BlockState b) {
        return b.is(net.minecraft.tags.BlockTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", "ores")));
    }

    private final HashSet<BlockPos> recursionBrake = new HashSet<>();
    private int minX = 0, minY = 0, minZ = 0, maxX = 0, maxY = 0, maxZ = 0;

    protected void breakRecursively(ServerLevel world, BlockPos pos, int depth) {

        if (depth < 0) return;
        if (recursionBrake.contains(pos)) return;
        recursionBrake.add(pos);

        BlockState b = world.getBlockState(pos);

        for (Direction dir : Direction.values()) {
            BlockPos n = pos.relative(dir);
            if (world.getBlockState(n).getBlock() == b.getBlock()) {
                breakRecursively(world, n, depth - 1);
            }
        }

        breakSingleBlock(world, b, pos);

        if (pos.getX() < minX) minX = pos.getX();
        if (pos.getX() > maxX) maxX = pos.getX();
        if (pos.getY() < minY) minY = pos.getY();
        if (pos.getY() > maxY) maxY = pos.getY();
        if (pos.getZ() < minZ) minZ = pos.getZ();
        if (pos.getZ() > maxZ) maxZ = pos.getZ();

        if (this.enableWalling) {
            world.setBlockAndUpdate(pos, ModBlocks.BARRICADE.get().defaultBlockState());
        }
    }

    protected void breakSingleBlock(ServerLevel world, BlockState b, BlockPos pos) {

        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        if (this.getFortuneLevel() > 0) com.hbm_m.platform.ItemHooks.setEnchantmentLevel(tool, world, "fortune", this.getFortuneLevel());
        if (this.canSilkTouch()) com.hbm_m.platform.ItemHooks.setEnchantmentLevel(tool, world, "silk_touch", 1);

        LootParams.Builder builder = new LootParams.Builder(world)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, tool)
                .withParameter(LootContextParams.BLOCK_STATE, b)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, world.getBlockEntity(pos));

        List<ItemStack> items = new ArrayList<>(b.getDrops(builder));

        if (this.enableCrusher) {

            List<ItemStack> list = new ArrayList<>();

            for (ItemStack stack : items) {
                ItemStack crushed = shredderResult(world, stack);

                if (crushed.isEmpty() || crushed.getItem() == item("scrap") || crushed.getItem() == ModItems.DUST.get()) {
                    list.add(stack);
                } else {
                    crushed.setCount(crushed.getCount() * stack.getCount());
                    list.add(crushed);
                }
            }

            items = list;
        }

        if (b.getBlock() == ModBlocks.BARRICADE.get())
            items.clear();

        for (ItemStack item : items) {
            world.addFreshEntity(new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, item));
        }

        world.destroyBlock(pos, false);
    }

    private static net.minecraft.world.item.Item item(String id) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, id));
    }

    private static ItemStack shredderResult(ServerLevel world, ItemStack stack) {
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, new ItemStack(stack.getItem(), 1));
        RecipeInputWrapper wrapper = new RecipeInputWrapper(container);
        for (ShredderRecipe r : RecipeHooks.getAllRecipes(world, ModRecipes.SHREDDER_TYPE.get())) {
            if (r.matchesRecipe(wrapper, world)) return r.getOutput().copy();
        }
        return ItemStack.EMPTY;
    }

    /** setzt eine Wand auf den Ring und ersetzt dabei Fluessigkeiten; mit wallEverything auch Luft und Gras */
    protected void buildWall(ServerLevel world, BlockPos core, int ring, boolean wallEverything) {
        int y = getY();

        for (int x = core.getX() - ring; x <= core.getX() + ring; x++) {
            for (int z = core.getZ() - ring; z <= core.getZ() + ring; z++) {

                BlockPos p = new BlockPos(x, y, z);
                BlockState b = world.getBlockState(p);

                if (x == core.getX() - ring || x == core.getX() + ring || z == core.getZ() - ring || z == core.getZ() + ring) {

                    if (b.canBeReplaced() && (wallEverything || b.liquid())) {
                        world.setBlockAndUpdate(p, ModBlocks.BARRICADE.get().defaultBlockState());
                    }
                } else {

                    if (b.liquid()) {
                        world.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
    }

    protected void mineOuterOres(ServerLevel world, BlockPos core, int ring) {
        int y = getY();

        for (int x = core.getX() - ring; x <= core.getX() + ring; x++) {
            for (int z = core.getZ() - ring; z <= core.getZ() + ring; z++) {

                if (ring == 1 || (x == core.getX() - ring || x == core.getX() + ring || z == core.getZ() - ring || z == core.getZ() + ring)) {

                    BlockPos p = new BlockPos(x, y, z);
                    BlockState b = world.getBlockState(p);

                    if (!this.shouldIgnoreBlock(world, b, p) && this.isOre(b)) {
                        tryMineAtLocation(world, p);
                    }
                }
            }
        }
    }

    protected void tryEjectBuffer(ServerLevel world) {

        Direction dir = facing();
        BlockPos chute = worldPosition.relative(dir, 4).below(3);

        List<ItemStack> items = new ArrayList<>();

        for (int i = OUTPUT_START; i <= OUTPUT_END; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }

        supplyContainer(world, chute, items, dir.getOpposite());
        supplyConveyor(world, chute, items);

        items.removeIf(i -> i == null || i.getCount() <= 0);

        for (int i = OUTPUT_START; i <= OUTPUT_END; i++) {
            int index = i - OUTPUT_START;

            if (items.size() > index) {
                inventory.setStackInSlot(i, items.get(index).copy());
            } else {
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    /** sammelt alles um den Bohrkopf ein und gibt es an die Rutsche oder den Puffer */
    protected void tryCollect(ServerLevel world, BlockPos core, int radius) {
        int yLevel = getY();

        List<ItemEntity> items = world.getEntitiesOfClass(ItemEntity.class,
                new AABB(core.getX() - radius, yLevel - 1, core.getZ() - radius, core.getX() + radius + 1, yLevel + 2, core.getZ() + radius + 1));

        Direction dir = facing();
        BlockPos chute = worldPosition.relative(dir, 4).below(3);

        List<ItemStack> stacks = new ArrayList<>();
        items.forEach(i -> { if (i.isAlive()) stacks.add(i.getItem()); });

        supplyContainer(world, chute, stacks, dir.getOpposite());
        supplyConveyor(world, chute, stacks);

        for (ItemEntity i : items) if (i.isAlive() && i.getItem().getCount() <= 0) i.discard();
        items.removeIf(i -> !i.isAlive() || i.getItem().getCount() <= 0);

        outer:
        for (ItemEntity item : items) {
            if (!item.isAlive()) continue;

            ItemStack stack = item.getItem();

            for (int i = OUTPUT_START; i <= OUTPUT_END; i++) {
                ItemStack s = inventory.getStackInSlot(i);

                if (!s.isEmpty() && s.getCount() < s.getMaxStackSize() && PlatformHooks.isSameItemSameTags(stack, s)) {
                    int toAdd = Math.min(s.getMaxStackSize() - s.getCount(), stack.getCount());
                    ItemStack grown = s.copy();
                    grown.grow(toAdd);
                    inventory.setStackInSlot(i, grown);
                    stack.shrink(toAdd);

                    chuteTimer = 40;

                    if (stack.getCount() <= 0) {
                        item.discard();
                        continue outer;
                    }
                }
            }

            for (int i = OUTPUT_START; i <= OUTPUT_END; i++) {
                if (inventory.getStackInSlot(i).isEmpty()) {
                    chuteTimer = 40;
                    inventory.setStackInSlot(i, stack.copy());
                    item.discard();
                    break;
                }
            }
        }
    }

    /** legt alles in einen angeschlossenen Behaelter, soweit moeglich */
    protected void supplyContainer(ServerLevel world, BlockPos pos, List<ItemStack> items, Direction dir) {
        //? if forge {
        net.minecraftforge.items.IItemHandler inv = com.hbm_m.blockentity.network.CraneInventoryUtil.inventoryAt(world, pos, dir);
        if (inv == null) return;

        for (ItemStack item : items) {
            if (item.getCount() <= 0) continue;
            com.hbm_m.blockentity.network.CraneInventoryUtil.addToInventory(inv, item);
            chuteTimer = 40;
        }
        //?} elif neoforge {
        /*net.neoforged.neoforge.items.IItemHandler inv = com.hbm_m.blockentity.network.CraneInventoryUtil.inventoryAt(world, pos, dir);
        if (inv == null) return;

        for (ItemStack item : items) {
            if (item.getCount() <= 0) continue;
            com.hbm_m.blockentity.network.CraneInventoryUtil.addToInventory(inv, item);
            chuteTimer = 40;
        }
        *///?}
    }

    /** setzt alles auf ein angeschlossenes Foerderband */
    protected void supplyConveyor(ServerLevel world, BlockPos pos, List<ItemStack> items) {
        if (!(world.getBlockState(pos).getBlock() instanceof com.hbm_m.block.network.IConveyorBelt belt)) return;

        for (ItemStack item : items) {

            if (item.getCount() <= 0) continue;

            Vec3 base = new Vec3(pos.getX() + world.random.nextDouble(), pos.getY() + 0.5, pos.getZ() + world.random.nextDouble());
            Vec3 vec = belt.getClosestSnappingPosition(world, pos, base);

            world.addFreshEntity(com.hbm_m.entity.conveyor.MovingConveyorItemEntity.create(world, base.x, vec.y, base.z, item.copy()));
            item.setCount(0);

            chuteTimer = 40;
        }
    }

    public long getPowerConsumption() {
        return consumption;
    }

    public int getFortuneLevel() {
        EnumDrillType type = getInstalledDrill();
        if (type != null) return type.fortune;
        return 0;
    }

    public boolean shouldIgnoreBlock(Level world, BlockState block, BlockPos pos) {
        return block.isAir() || block.getBlock() instanceof com.hbm_m.block.gas.BlockGasBase || block.getDestroySpeed(world, pos) < 0
                || block.liquid() || block.getBlock() == Blocks.BEDROCK;
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 128;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("drill")) this.enableDrill = !this.enableDrill;
        if (data.contains("crusher")) this.enableCrusher = !this.enableCrusher;
        if (data.contains("walling")) this.enableWalling = !this.enableWalling;
        if (data.contains("veinminer")) this.enableVeinMiner = !this.enableVeinMiner;
        if (data.contains("silktouch")) this.enableSilkTouch = !this.enableSilkTouch;

        this.setChanged();
    }

    @Nullable
    public EnumDrillType getInstalledDrill() {
        return ItemDrillbit.typeOf(inventory.getStackInSlot(SLOT_DRILLBIT));
    }

    public boolean canVeinMine() {
        EnumDrillType type = getInstalledDrill();
        return this.enableVeinMiner && type != null && type.vein;
    }

    public boolean canSilkTouch() {
        EnumDrillType type = getInstalledDrill();
        return this.enableSilkTouch && type != null && type.silk;
    }

    // ── Inventar-Hilfen ──────────────────────────────────────────────────────

    private ItemStack[] slotArray() {
        ItemStack[] arr = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) arr[i] = inventory.getStackInSlot(i);
        return arr;
    }

    private void applySlotArray(ItemStack[] arr) {
        for (int i = 0; i < INVENTORY_SIZE; i++) inventory.setStackInSlot(i, arr[i] == null ? ItemStack.EMPTY : arr[i]);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot < OUTPUT_START;
    }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("d", enableDrill);
        tag.putBoolean("c", enableCrusher);
        tag.putBoolean("w", enableWalling);
        tag.putBoolean("v", enableVeinMiner);
        tag.putBoolean("s", enableSilkTouch);
        tag.putInt("t", targetDepth);
        tank.writeToNBT(tag, "tank");
        tag.putBoolean("operational", operational);
        tag.putInt("chuteTimer", chuteTimer);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        this.enableDrill = tag.getBoolean("d");
        this.enableCrusher = tag.getBoolean("c");
        this.enableWalling = tag.getBoolean("w");
        this.enableVeinMiner = tag.getBoolean("v");
        this.enableSilkTouch = tag.getBoolean("s");
        this.targetDepth = tag.getInt("t");
        this.tank.readFromNBT(tag, "tank");
        this.operational = tag.getBoolean("operational");
        this.chuteTimer = tag.getInt("chuteTimer");
    }

    // ── Fluid ────────────────────────────────────────────────────────────────

    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { tank }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    public FluidTank getTank() { return tank; }

    // ── Upgrades ─────────────────────────────────────────────────────────────

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(com.hbm_m.interfaces.IUpgradeInfoProvider.getStandardLabel(getBlockState().getBlock()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (100 - 200 / (level + 2)) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 100) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (100 - 100 / (level + 1)) + "%").withStyle(ChatFormatting.GREEN));
        }
    }

    @Override
    public Map<UpgradeType, Integer> getValidUpgrades() {
        return VALID_UPGRADES;
    }

    // ── Menue ────────────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.mining_drill");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MachineMiningDrillMenu(id, inventory, this);
    }

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 3, worldPosition.getY() - 512, worldPosition.getZ() - 3,
                worldPosition.getX() + 4, worldPosition.getY() + 5, worldPosition.getZ() + 4);
    }
}
