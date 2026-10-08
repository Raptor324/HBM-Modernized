package com.hbm_m.blockentity.machines;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineReactorResearchMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ItemPlateFuel;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.radiation.ChunkRadiationManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityReactorResearch}: Forschungsreaktor mit 12 Plattenbrennstoff-Plaetzen in festem
 * Nachbarschaftsgraphen; der Neutronenfluss geht, skaliert mit der Stabstellung {@code level} (0-1), an die Nachbarn.
 * Die Stellung faehrt mit 0,04 je Tick auf das Ziel, das die GUI ({@link IControlReceiver}, Schluessel "level") oder
 * ein Reaktorsteuerpult setzt. Gekuehlt wird ueber Wasserbloecke rund um die Saeule; ueber 50000 Hitze explodiert er
 * (Staerke 18) und hinterlaesst Stahl, Corium und Stahl. Ohne Abschirmung auf Hoehe des Mittelblocks strahlt er.
 */
public class MachineReactorResearchBlockEntity extends BaseMachineBlockEntity implements IControlReceiver {

    public static final int INVENTORY_SIZE = 12;

    public double lastLevel;
    public double level;
    public double speed = 0.04;
    public double targetLevel;

    public int heat;
    public byte water;
    public final int maxHeat = 50000;
    public int[] slotFlux = new int[12];
    public int totalFlux = 0;

    /** Original {@code fuelMap}: Platte -> heisse Abfallplatte (Metadatum 1 = "cooling"). */
    private static final Map<Item, String> FUEL_TO_WASTE = new HashMap<>();
    static {
        FUEL_TO_WASTE.put(ModItems.PLATE_FUEL_U233.get(), "waste_plate_u233_cooling");
        FUEL_TO_WASTE.put(ModItems.PLATE_FUEL_U235.get(), "waste_plate_u235_cooling");
        FUEL_TO_WASTE.put(ModItems.PLATE_FUEL_MOX.get(), "waste_plate_mox_cooling");
        FUEL_TO_WASTE.put(ModItems.PLATE_FUEL_PU239.get(), "waste_plate_pu239_cooling");
        FUEL_TO_WASTE.put(ModItems.PLATE_FUEL_SA326.get(), "waste_plate_sa326_cooling");
        FUEL_TO_WASTE.put(ModItems.PLATE_FUEL_RA226BE.get(), "waste_plate_ra226be_cooling");
        FUEL_TO_WASTE.put(ModItems.PLATE_FUEL_PU238BE.get(), "waste_plate_pu238be_cooling");
    }

    @Nullable
    private static Item waste(Item fuel) {
        String id = FUEL_TO_WASTE.get(fuel);
        return id != null ? com.hbm_m.item.PartTabMetaItems.itemOrNull(id) : null;
    }

    private static boolean isWasteOutput(ItemStack stack) {
        for (String id : FUEL_TO_WASTE.values()) {
            Item w = com.hbm_m.item.PartTabMetaItems.itemOrNull(id);
            if (w != null && stack.is(w)) return true;
        }
        return false;
    }

    public MachineReactorResearchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_RESEARCH_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, MachineReactorResearchBlockEntity be) {
        be.updateEntity(world, pos);
    }

    private void updateEntity(Level world, BlockPos pos) {

        rodControl(world);

        if (!world.isClientSide) {
            totalFlux = 0;

            if (level > 0) {
                reaction(world);
            }

            if (this.heat > 0) {
                water = getWater(world, pos);

                if (water > 0) {
                    this.heat = (int) (this.heat - (this.heat * (float) 0.07 * water / 12));
                } else if (water == 0) {
                    this.heat -= 1;
                }

                if (this.heat < 0)
                    this.heat = 0;
            }

            if (this.heat > maxHeat) {
                this.explode(world, pos);
                return;
            }

            if (level > 0 && heat > 0 && !(blocksRad(world, pos.offset(1, 1, 0)) && blocksRad(world, pos.offset(-1, 1, 0))
                    && blocksRad(world, pos.offset(0, 1, 1)) && blocksRad(world, pos.offset(0, 1, -1)))) {
                float rad = (float) heat / (float) maxHeat * 50F;
                ChunkRadiationManager.incrementRad(world, pos.getX(), pos.getY(), pos.getZ(), rad);
            }

            setChanged();
            sendUpdateToClient();
        }
    }

    private static boolean isWaterMaterial(Level world, BlockPos p) {
        BlockState s = world.getBlockState(p);
        return s.is(Blocks.WATER) || s.is(Blocks.BUBBLE_COLUMN);
    }

    public byte getWater(Level world, BlockPos pos) {
        byte water = 0;

        for (byte d = 0; d < 6; d++) {
            Direction dir = Direction.from3DDataValue(d);
            if (d < 2) {
                if (isWaterMaterial(world, pos.offset(0, 1 + dir.getStepY() * 2, 0)))
                    water++;
            } else {
                for (byte i = 0; i < 3; i++) {
                    if (isWaterMaterial(world, pos.offset(dir.getStepX(), i, dir.getStepZ())))
                        water++;
                }
            }
        }

        return water;
    }

    /** Original {@code isSubmerged}: Wasser an einer der vier Seiten des Mittelblocks (fuer die Tscherenkow-Huelle). */
    public boolean isSubmerged() {
        if (getLevel() == null) return false;
        BlockPos p = worldPosition;
        return isWaterMaterial(getLevel(), p.offset(1, 1, 0)) || isWaterMaterial(getLevel(), p.offset(0, 1, 1))
                || isWaterMaterial(getLevel(), p.offset(-1, 1, 0)) || isWaterMaterial(getLevel(), p.offset(0, 1, -1));
    }

    private static boolean isBlock(Block b, String id) {
        return BuiltInRegistries.BLOCK.getKey(b).equals(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, id));
    }

    private boolean blocksRad(Level world, BlockPos p) {

        BlockState state = world.getBlockState(p);
        Block b = state.getBlock();

        if (b == Blocks.WATER && state.getFluidState().isSource())
            return true;

        if (isBlock(b, "block_lead") || isBlock(b, "block_desh") || b == ModBlocks.REACTOR_RESEARCH.get() || b == ModBlocks.BREEDER.get())
            return true;

        if (b.getExplosionResistance() >= 100)
            return true;

        return false;
    }

    private static int[] getNeighboringSlots(int id) {
        return switch (id) {
            case 0 -> new int[] { 1, 5 };
            case 1 -> new int[] { 0, 6 };
            case 2 -> new int[] { 3, 7 };
            case 3 -> new int[] { 2, 4, 8 };
            case 4 -> new int[] { 3, 9 };
            case 5 -> new int[] { 0, 6, 0xA };
            case 6 -> new int[] { 1, 5, 0xB };
            case 7 -> new int[] { 2, 8 };
            case 8 -> new int[] { 3, 7, 9 };
            case 9 -> new int[] { 4, 8 };
            case 10 -> new int[] { 5, 0xB };
            case 11 -> new int[] { 6, 0xA };
            default -> null;
        };
    }

    private void reaction(Level world) {
        for (byte i = 0; i < 12; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                slotFlux[i] = 0;
                continue;
            }

            if (stack.getItem() instanceof ItemPlateFuel rod) {

                int outFlux = rod.react(world, stack, slotFlux[i]);
                this.heat += outFlux * 2;
                slotFlux[i] = 0;
                totalFlux += outFlux;

                int[] neighborSlots = getNeighboringSlots(i);

                if (ItemPlateFuel.getLifeTime(stack) > rod.lifeTime) {
                    Item w = waste(stack.getItem());
                    inventory.setStackInSlot(i, w != null ? new ItemStack(w) : ItemStack.EMPTY);
                }

                for (byte j = 0; j < neighborSlots.length; j++) {
                    slotFlux[neighborSlots[j]] += (int) (outFlux * level);
                }
                continue;
            }

            if (stack.is(ModItems.METEORITE_SWORD_BRED.get()))
                inventory.setStackInSlot(i, new ItemStack(ModItems.METEORITE_SWORD_IRRADIATED.get()));

            slotFlux[i] = 0;
        }
    }

    private void explode(Level world, BlockPos pos) {

        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }

        world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());

        for (byte d = 0; d < 6; d++) {
            Direction dir = Direction.from3DDataValue(d);
            if (d < 2) {
                BlockPos p = pos.offset(0, 1 + dir.getStepY() * 2, 0);
                if (isWaterMaterial(world, p)) world.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            } else {
                for (byte i = 0; i < 3; i++) {
                    BlockPos p = pos.offset(dir.getStepX(), i, dir.getStepZ());
                    if (isWaterMaterial(world, p)) world.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                }
            }
        }

        world.explode(null, pos.getX(), pos.getY(), pos.getZ(), 18.0F, Level.ExplosionInteraction.BLOCK);
        world.setBlockAndUpdate(pos, ModBlocks.DECO_STEEL.get().defaultBlockState());
        world.setBlockAndUpdate(pos.above(), ModBlocks.CORIUM_BLOCK.get().defaultBlockState());
        world.setBlockAndUpdate(pos.above(2), ModBlocks.DECO_STEEL.get().defaultBlockState());

        ChunkRadiationManager.incrementRad(world, pos.getX(), pos.getY(), pos.getZ(), 50);

        // Original: MobConfig.enableElementals (Vorgabe an) - Spieler im Umkreis von 100 bekommen radMark
        List<Player> players = world.getEntitiesOfClass(Player.class,
                new AABB(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5).inflate(100, 100, 100));

        for (Player player : players) {
            if (player instanceof ServerPlayer sp) com.hbm_m.handler.BossSpawnHandler.markForRadBeasts(sp);
        }
    }

    // ── Steuerstaebe ─────────────────────────────────────────────────────────

    @Override
    public boolean hasPermission(Player player) {
        return Math.sqrt(player.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())) < 20;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("level")) {
            this.setTarget(data.getDouble("level"));
        }

        this.setChanged();
    }

    public void setTarget(double target) {
        this.targetLevel = target;
    }

    public void rodControl(Level world) {
        if (world.isClientSide) {

            this.lastLevel = this.level;

        } else {

            if (level < targetLevel) {

                level += speed;

                if (level >= targetLevel)
                    level = targetLevel;
            }

            if (level > targetLevel) {

                level -= speed;

                if (level <= targetLevel)
                    level = targetLevel;
            }
        }
    }

    public int[] getDisplayData() {
        int[] data = new int[2];
        data[0] = this.totalFlux;
        data[1] = (int) Math.round((this.heat) * 0.00002 * 980 + 20);
        return data;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public int getHeat()          { return heat; }
    public int getMaxHeat()       { return maxHeat; }
    public int getWater()         { return water; }
    public double getRodLevel()   { return level; }
    public double getTargetLevel() { return targetLevel; }
    public int getTotalFlux()     { return totalFlux; }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("heat", heat);
        tag.putByte("water", water);
        tag.putDouble("level", level);
        tag.putDouble("targetLevel", targetLevel);
        tag.putIntArray("slotFlux", slotFlux);
        tag.putInt("totalFlux", totalFlux);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        heat = tag.getInt("heat");
        water = tag.getByte("water");
        level = tag.getDouble("level");
        targetLevel = tag.getDouble("targetLevel");
        int[] flux = tag.getIntArray("slotFlux");
        if (flux.length == INVENTORY_SIZE) slotFlux = flux;
        totalFlux = tag.getInt("totalFlux");
    }

    // ── Slots ────────────────────────────────────────────────────────────────

    /** In der GUI ist jeder Platz frei belegbar (die Container-Slots des Originals pruefen nichts). */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    //? if forge {
    private final Map<Direction, net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler>> sided = new EnumMap<>(Direction.class);

    /**
     * Original Automatisierung: {@code isItemValidForSlot} laesst wegen {@code i <= 0} nur Platz 0 und nur exakt
     * {@code ItemPlateFuel} zu, entnommen werden nur heisse Abfallplatten.
     */
    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) {
            return sided.computeIfAbsent(side, d -> net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.IItemHandler() {
                @Override public int getSlots() { return INVENTORY_SIZE; }
                @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
                @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    if (!isItemValid(slot, stack)) return stack;
                    return inventory.insertItem(slot, stack, simulate);
                }
                @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                    if (!isWasteOutput(inventory.getStackInSlot(slot))) return ItemStack.EMPTY;
                    return inventory.extractItem(slot, amount, simulate);
                }
                @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
                @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                    return slot == 0 && stack.getItem().getClass() == ItemPlateFuel.class;
                }
            })).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sided.values().forEach(net.minecraftforge.common.util.LazyOptional::invalidate);
        sided.clear();
    }
    //?}

    // ── Menu ────────────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.reactor_research");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineReactorResearchMenu.create(id, inventory, this);
    }

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return AABB.ofSize(worldPosition.getCenter(), 64, 64, 64);
    }
}
