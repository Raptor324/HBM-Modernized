package com.hbm_m.block.entity.machines;

import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.nature.DepthOreBlock;
import com.hbm_m.inventory.menu.MachineMiningDrillMenu;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/**
 * MVP-Port des Large Mining Drill. Original-1.7.10-Quelle war in diesem Checkout nicht
 * vorhanden (nur die {@code IMiningDrill}/{@code IDrillInteraction}-API-Interfaces) - die
 * Logik hier ist eine eigenstaendige, plausible Naeherung: die Maschine graebt sich unterhalb
 * des Controllers Block fuer Block in die Tiefe, verbraucht dabei Energie + Drillbit-Haltbarkeit,
 * und erntet dabei {@link DepthOreBlock}-Vorkommen (die per Hand nicht abbaubar sind) in ihr
 * Ausgabe-Inventar.
 */
public class MachineMiningDrillBlockEntity extends BaseMachineBlockEntity {

    public static final int SLOT_DRILLBIT = 0;
    private static final int OUTPUT_START = 1;
    private static final int OUTPUT_COUNT = 9;
    public static final int SLOT_BATTERY = 10;
    private static final int SLOT_COUNT = 11;

    private static final long CAPACITY = 1_000_000L;
    private static final long MAX_RECEIVE = 2_000L;
    private static final long ENERGY_PER_BLOCK = 1_000L;
    private static final int DEFAULT_MAX_PROGRESS = 100;

    /** Haltbarkeit (Bloecke, die abgebaut werden koennen) je Drillbit-Tier. */
    private static final Map<Item, Integer> DRILLBIT_DURABILITY = Map.ofEntries(
            Map.entry(ModItems.DRILLBIT_HSS.get(), 100),
            Map.entry(ModItems.DRILLBIT_HSS_DIAMOND.get(), 150),
            Map.entry(ModItems.DRILLBIT_STEEL.get(), 150),
            Map.entry(ModItems.DRILLBIT_STEEL_DIAMOND.get(), 225),
            Map.entry(ModItems.DRILLBIT_FERRO.get(), 250),
            Map.entry(ModItems.DRILLBIT_FERRO_DIAMOND.get(), 375),
            Map.entry(ModItems.DRILLBIT_DESH.get(), 400),
            Map.entry(ModItems.DRILLBIT_DESH_DIAMOND.get(), 600),
            Map.entry(ModItems.DRILLBIT_TCALLOY.get(), 600),
            Map.entry(ModItems.DRILLBIT_TCALLOY_DIAMOND.get(), 900)
    );

    private int progress = 0;
    private int maxProgress = DEFAULT_MAX_PROGRESS;
    private boolean active = false;
    private int drillDepth = 0;
    private int drillBitUsesLeft = 0;

    /** Client-seitige Rotationsanimation des Bohrkopfs (siehe MachineMiningDrillRenderer). */
    public float prevAngle;
    public float angle;

    public MachineMiningDrillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINING_DRILL_BE.get(), pos, state, SLOT_COUNT, CAPACITY, MAX_RECEIVE, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineMiningDrillBlockEntity be) {
        if (level.isClientSide) {
            be.clientTick();
        } else {
            be.serverTick(level, pos);
        }
    }

    private void clientTick() {
        prevAngle = angle;
        if (active) {
            angle += 18.0F;
            if (angle >= 360.0F) {
                angle -= 360.0F;
                prevAngle -= 360.0F;
            }
        }
    }

    private void serverTick(Level level, BlockPos pos) {
        ensureNetworkInitialized();
        chargeFromBatterySlot(SLOT_BATTERY);

        boolean wasActive = active;
        active = canWork(level, pos);

        if (active) {
            progress++;
            if (progress >= maxProgress) {
                progress = 0;
                mineNextBlock(level, pos);
            }
        } else {
            progress = 0;
        }

        if (wasActive != active) {
            setChanged();
            sendUpdateToClient();
        }
    }

    private boolean canWork(Level level, BlockPos pos) {
        if (getEnergyStored() < ENERGY_PER_BLOCK) return false;
        if (findDrillBitSlotItem().isEmpty()) return false;
        if (!hasOutputRoom()) return false;
        return getDrillTargetPos(pos).getY() > level.getMinBuildHeight();
    }

    private BlockPos getDrillTargetPos(BlockPos controllerPos) {
        return controllerPos.below(1 + drillDepth);
    }

    private ItemStack findDrillBitSlotItem() {
        return inventory.getStackInSlot(SLOT_DRILLBIT);
    }

    private boolean hasOutputRoom() {
        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private boolean insertOutput(ItemStack toInsert) {
        for (int pass = 0; pass < 2 && !toInsert.isEmpty(); pass++) {
            for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT && !toInsert.isEmpty(); i++) {
                ItemStack slotStack = inventory.getStackInSlot(i);
                if (pass == 0 && !slotStack.isEmpty() && ItemStack.isSameItemSameTags(slotStack, toInsert)) {
                    int room = slotStack.getMaxStackSize() - slotStack.getCount();
                    int move = Math.min(room, toInsert.getCount());
                    if (move > 0) {
                        slotStack.grow(move);
                        toInsert.shrink(move);
                    }
                } else if (pass == 1 && slotStack.isEmpty()) {
                    inventory.setStackInSlot(i, toInsert.copy());
                    toInsert.setCount(0);
                }
            }
        }
        return toInsert.isEmpty();
    }

    private void mineNextBlock(Level level, BlockPos pos) {
        BlockPos target = getDrillTargetPos(pos);
        BlockState targetState = level.getBlockState(target);
        Block targetBlock = targetState.getBlock();

        if (targetBlock == Blocks.BEDROCK) {
            return; // Endstation erreicht, Fortschritt friert ein.
        }

        if (targetBlock instanceof DepthOreBlock) {
            insertOutput(new ItemStack(targetBlock));
        }

        if (!targetState.isAir()) {
            level.removeBlock(target, false);
            level.playSound(null, target, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
        }

        drillDepth++;
        consumeDrillBitDurability();
        setEnergyStored(getEnergyStored() - ENERGY_PER_BLOCK);
        setChanged();
    }

    private void consumeDrillBitDurability() {
        ItemStack bit = findDrillBitSlotItem();
        if (bit.isEmpty()) return;

        if (drillBitUsesLeft <= 0) {
            drillBitUsesLeft = DRILLBIT_DURABILITY.getOrDefault(bit.getItem(), 100);
        }
        drillBitUsesLeft--;
        if (drillBitUsesLeft <= 0) {
            bit.shrink(1);
            drillBitUsesLeft = 0;
        }
    }

    public int getProgressScaled(int scale) {
        if (maxProgress <= 0) return 0;
        return progress * scale / maxProgress;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public int getDrillDepth() {
        return drillDepth;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("progress", progress);
        tag.putInt("max_progress", maxProgress);
        tag.putBoolean("active", active);
        tag.putInt("drill_depth", drillDepth);
        tag.putInt("drillbit_uses_left", drillBitUsesLeft);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("max_progress");
        if (maxProgress <= 0) {
            maxProgress = DEFAULT_MAX_PROGRESS;
        }
        active = tag.getBoolean("active");
        drillDepth = tag.getInt("drill_depth");
        drillBitUsesLeft = tag.getInt("drillbit_uses_left");
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
            return DRILLBIT_DURABILITY.containsKey(stack.getItem());
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
}
