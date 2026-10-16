package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import com.hbm_m.inventory.menu.RBMKAutoloaderMenu;
import com.hbm_m.item.rbmk.RBMKRodItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Autoloader: stores fuel rods and automatically loads/unloads adjacent loadable RBMK columns.
 */
public class RBMKAutoloaderBlockEntity extends RBMKColumnBlockEntity implements MenuProvider {

    /**
     * 1:1 with {@code TileEntityRBMKAutoloader}: eighteen slots, split in two halves. Slots 0-8
     * hold fresh rods waiting to go in, slots 9-17 collect the spent ones pulled back out. This
     * port previously had a single nine-slot buffer used for both directions, so recovered rods
     * were dropped back into the same pool the loader feeds from and could be re-inserted.
     */
    public static final int SLOTS = 18;
    public static final int INPUT_SLOTS = 9;
    public final ItemStack[] slots = new ItemStack[SLOTS];

    /**
     * Minimum enrichment, in percent, a rod must still have to be worth loading - and equally the
     * point below which a rod in the reactor counts as spent. The original exposes this as a
     * per-machine setting; the default is 50.
     */
    public int cycle = 50;

    /**
     * Piston travel, 0 (fully retracted) to 1 (fully extended), advanced by {@link #SPEED} per tick
     * - 200 ticks each way, with a 40-tick pause at both ends. The swap happens only at full
     * extension. This whole state machine was missing: the port did the swap instantly on a 20-tick
     * timer, so the loader had no animation, no travel time and no reason for its OBJ model's
     * moving half to exist.
     */
    public double piston = 0;
    public double lastPiston = 0;
    private boolean isRetracting = true;
    private int delay = 0;

    public static final double SPEED = 0.005D;
    private static final int END_DELAY = 40;

    /** Server-side scan interval for "should I start a cycle" - CE checks every 20 ticks. */
    private static final int SCAN_INTERVAL = 20;

    /** Original hasFuel(): is there anything in the input half still rich enough to load? */
    public boolean hasFuel() {
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = slots[i];
            if (!stack.isEmpty() && stack.getItem() instanceof RBMKRodItem
                    && RBMKRodItem.getEnrichment(stack) * 100 >= cycle) return true;
        }
        return false;
    }

    /** Original hasSpace(): is there room left in the output half for a spent rod? */
    public boolean hasSpace() {
        for (int i = INPUT_SLOTS; i < SLOTS; i++) if (slots[i].isEmpty()) return true;
        return false;
    }

    /** Original isItemValidForSlot: only rich-enough rods, and only into the input half. */
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot < INPUT_SLOTS && stack.getItem() instanceof RBMKRodItem
                && RBMKRodItem.getEnrichment(stack) * 100 >= cycle;
    }

    public RBMKAutoloaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_AUTOLOADER_BE.get(), pos, state);
        for (int i = 0; i < SLOTS; i++) slots[i] = ItemStack.EMPTY;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKAutoloaderBlockEntity be) {
        baseTick(level, pos, state, be);

        if (level.isClientSide) {
            be.lastPiston = be.piston;
            return;
        }

        if (be.delay > 0) be.delay--;

        // Retracting
        if (be.delay <= 0 && be.isRetracting && be.piston > 0D) {
            be.piston -= SPEED;
            if (be.piston <= 0) { be.piston = 0; be.delay = END_DELAY; }
            be.setChanged();
        }

        // Decide whether to start a cycle. CE only reaches down for the column DIRECTLY BELOW the
        // loader - it caps a single fuel channel - and only starts when that channel is either
        // empty or holding a rod that has burnt past the cycle threshold. The port used to pull the
        // rod out unconditionally, so a freshly loaded rod was yanked straight back out again.
        if (be.isRetracting && level.getGameTime() % SCAN_INTERVAL == 0 && be.hasFuel() && be.hasSpace()) {
            RBMKRodBlockEntity rod = be.targetRod(level, pos);
            if (rod != null && rod.coldEnoughForAutoloader()) {
                ItemStack inChannel = rod.provideNext();
                boolean spent = inChannel.isEmpty()
                        || (inChannel.getItem() instanceof RBMKRodItem
                            && RBMKRodItem.getEnrichment(inChannel) * 100 < be.cycle);
                if (spent) be.isRetracting = false;
            }
        }

        // Extending
        if (be.delay <= 0 && !be.isRetracting && be.piston < 1D) {
            be.piston += SPEED;
            if (be.piston >= 1) { be.piston = 1; be.delay = END_DELAY; }
            be.setChanged();
        }

        // Fully extended: do the swap.
        if (!be.isRetracting && be.piston >= 1D) {
            be.piston = 1D;
            RBMKRodBlockEntity rod = be.targetRod(level, pos);
            if (rod == null) return;

            if (rod.canUnload() && be.hasSpace()) {
                ItemStack spent = rod.provideNext();
                if (!spent.isEmpty()) {
                    for (int i = INPUT_SLOTS; i < SLOTS; i++) {
                        if (be.slots[i].isEmpty()) {
                            be.slots[i] = spent.copy();
                            rod.unload();
                            break;
                        }
                    }
                }
            }

            if (!rod.canUnload()) {
                for (int i = 0; i < INPUT_SLOTS; i++) {
                    ItemStack stack = be.slots[i];
                    if (stack.isEmpty() || !(stack.getItem() instanceof RBMKRodItem)) continue;
                    if (RBMKRodItem.getEnrichment(stack) * 100 < be.cycle) continue;
                    rod.load(stack);
                    be.slots[i] = ItemStack.EMPTY;
                    break;
                }
            }

            be.isRetracting = true;
            be.delay = END_DELAY;
            be.setChanged();
        }
    }

    /** The fuel channel underneath, resolved through the column's dummy blocks if need be. */
    private RBMKRodBlockEntity targetRod(Level level, BlockPos pos) {
        RBMKColumnBlockEntity col = RBMKSteamInletBlockEntity.findColumnCore(level, pos.below());
        return col instanceof RBMKRodBlockEntity rod ? rod : null;
    }

    /**
     * The autoloader is a machine sitting on top of a channel, not a reactor column - CE derives it
     * from {@code TileEntityMachineBase}, not {@code TileEntityRBMKBase}. It must not swap heat
     * with the columns around it.
     */
    @Override protected boolean participatesInHeatNetwork() { return false; }

    /**
     * The piston travels four blocks <b>down</b> into the fuel channel, so the inherited column box
     * (which only ever grows upward) culls it away as soon as the block itself leaves the frustum.
     */
    //? if forge {
    @Override
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        BlockPos p = getBlockPos();
        return new net.minecraft.world.phys.AABB(p.getX(), p.getY() - 5, p.getZ(),
                                                 p.getX() + 1, p.getY() + 2, p.getZ() + 1);
    }
    //?}

    @Override public Component getDisplayName() { return Component.translatable("block.hbm_m.rbmk_autoloader"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new RBMKAutoloaderMenu(id, inv, this); }
    @Override public RBMKType getRBMKType()      { return RBMKType.OTHER; }
    @Override public ColumnType getConsoleType() { return ColumnType.STORAGE; }

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (int i = 0; i < SLOTS; i++) {
            if (!slots[i].isEmpty()) {
                CompoundTag s = new CompoundTag();
                s.putByte("s", (byte) i);
                s.put("item", safeItemSave(slots[i]));
                list.add(s);
            }
        }
        tag.put("slots", list);
        tag.putInt("cycle", cycle);
        tag.putDouble("piston", piston);
        tag.putBoolean("isRetracting", isRetracting);
        tag.putInt("delay", delay);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("cycle")) cycle = tag.getInt("cycle");
        piston = tag.getDouble("piston");
        lastPiston = piston;
        isRetracting = !tag.contains("isRetracting") || tag.getBoolean("isRetracting");
        delay = tag.getInt("delay");
        for (int i = 0; i < SLOTS; i++) slots[i] = ItemStack.EMPTY;
        ListTag list = tag.getList("slots", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag s = list.getCompound(i);
            int idx = s.getByte("s") & 0xFF;
            if (idx < SLOTS && s.contains("item"))
                slots[idx] = ItemStack.of(s.getCompound("item"));
        }
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (int i = 0; i < SLOTS; i++) {
            if (!slots[i].isEmpty()) {
                CompoundTag s = new CompoundTag();
                s.putByte("s", (byte) i);
                s.put("item", safeItemSave(slots[i], registries));
                list.add(s);
            }
        }
        tag.put("slots", list);
        tag.putInt("cycle", cycle);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("cycle")) cycle = tag.getInt("cycle");
        for (int i = 0; i < SLOTS; i++) slots[i] = ItemStack.EMPTY;
        ListTag list = tag.getList("slots", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag s = list.getCompound(i);
            int idx = s.getByte("s") & 0xFF;
            if (idx < SLOTS && s.contains("item"))
                slots[idx] = ItemStack.parseOptional(registries, s.getCompound("item"));
        }
    }
    *///?}
}

