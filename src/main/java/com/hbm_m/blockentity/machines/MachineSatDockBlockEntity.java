package com.hbm_m.blockentity.machines;

import java.util.List;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.missile.EntityMinerRocket;
import com.hbm_m.explosion.ExplosionNukeSmall;
import com.hbm_m.item.ISatChip;
import com.hbm_m.satellite.Satellite;
import com.hbm_m.satellite.SatelliteManager;
import com.hbm_m.satellite.SatelliteMiner;
import com.hbm_m.satellite.SatellitePools;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineSatDock}: mit einem Bergbausatelliten-Chip in Platz 15 landet alle 10 Minuten
 * (Echtzeit, wie im Original ueber {@code System.currentTimeMillis}) ein {@link EntityMinerRocket}; beim Entladen
 * kommen 10-15 Ziehungen aus dem Frachtpool in die Plaetze 0-14, die in Container zwei Bloecke entfernt ausgeworfen
 * werden. Eine Rakete mit fremder Frequenz explodiert als Mini-Atombombe.
 */
public class MachineSatDockBlockEntity extends BaseMachineBlockEntity {

    public static final int SLOT_CHIP = 15;

    public MachineSatDockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SAT_DOCK_BE.get(), pos, state, 16, 0L, 0L, 0L);
    }

    public ItemStack slot(int i) {
        return inventory.getStackInSlot(i);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSatDockBlockEntity be) {
        if (level instanceof ServerLevel server) be.updateEntity(server);
    }

    private void updateEntity(ServerLevel world) {
        SatelliteManager data = SatelliteManager.get(world);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();

        // Original (neueres TileEntityMachineSatDock): mit Satelliten-Chip einmal je Sekunde fertige Fracht anfordern
        long time = world.getGameTime() + worldPosition.asLong();
        if (time % 20 == 0 && slot(SLOT_CHIP).is(com.hbm_m.item.ModItems.SAT_CHIP.get())) {
            Satellite sat = data.getSatFromFreq(ISatChip.getFreqS(slot(SLOT_CHIP)));
            if (sat != null && sat.tryRequestItems(world, x, y, z)) data.setDirty();
        }

        if (!slot(SLOT_CHIP).isEmpty()) {
            int freq = ISatChip.getFreqS(slot(SLOT_CHIP));

            Satellite sat = data.getSatFromFreq(freq);

            int delay = 10 * 60 * 1000;

            if (sat instanceof SatelliteMiner miner) {
                if (miner.lastOp + delay < System.currentTimeMillis()) {
                    EntityMinerRocket rocket = new EntityMinerRocket(world);
                    rocket.setPos(x + 0.5, 300, z + 0.5);
                    rocket.setSat(freq);
                    world.addFreshEntity(rocket);
                    miner.lastOp = System.currentTimeMillis();
                    data.setDirty();
                }
            }
        }

        List<EntityMinerRocket> list = world.getEntitiesOfClass(EntityMinerRocket.class,
                new AABB(x - 0.25 + 0.5, y + 0.75, z - 0.25 + 0.5, x + 0.25 + 0.5, y + 2, z + 0.25 + 0.5));

        for (EntityMinerRocket rocket : list) {

            if (!slot(SLOT_CHIP).isEmpty() && ISatChip.getFreqS(slot(SLOT_CHIP)) != rocket.getSat()) {
                rocket.discard();
                ExplosionNukeSmall.explode(world, x + 0.5, y + 0.5, z + 0.5, ExplosionNukeSmall.PARAMS_TOTS);
                break;
            }

            if (rocket.getMode() == 1 && rocket.timer == 50) {
                Satellite sat = data.getSatFromFreq(ISatChip.getFreqS(slot(SLOT_CHIP)));
                if (sat instanceof SatelliteMiner miner) unloadCargo(world, miner);
            }
        }

        ejectInto(x + 2, y, z);
        ejectInto(x - 2, y, z);
        ejectInto(x, y, z + 2);
        ejectInto(x, y, z - 2);
    }

    private void unloadCargo(ServerLevel world, SatelliteMiner satellite) {
        int itemAmount = world.random.nextInt(6) + 10;

        for (int i = 0; i < itemAmount; i++) {
            ItemStack stack = SatellitePools.getStack(satellite.getCargo(), world.random);
            if (!stack.isEmpty()) addToInv(stack);
        }
        setChanged();
    }

    /**
     * Original {@code InventoryUtil.tryAddItemToInventory(slots, 0, 14, stack)} fuer die Landekapsel: fuellt gleiche
     * Stapel auf, dann freie Plaetze; gibt den Rest zurueck.
     */
    public ItemStack tryAddToInventory(ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int i = 0; i < 15 && !rest.isEmpty(); i++) {
            ItemStack s = slot(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameTags(s, rest) && s.getCount() < s.getMaxStackSize()) {
                int toAdd = Math.min(s.getMaxStackSize() - s.getCount(), rest.getCount());
                s.grow(toAdd);
                rest.shrink(toAdd);
            }
        }
        for (int i = 0; i < 15 && !rest.isEmpty(); i++) {
            if (slot(i).isEmpty()) {
                inventory.setStackInSlot(i, rest.copy());
                rest = ItemStack.EMPTY;
            }
        }
        setChanged();
        return rest;
    }

    /** Original addToInv: fuellt gleiche Stapel auf, sonst legt es in den ersten freien Platz nur EIN Stueck. */
    private void addToInv(ItemStack stack) {

        for (int i = 0; i < 15; i++) {
            ItemStack s = slot(i);
            if (!s.isEmpty() && ItemStack.isSameItem(s, stack) && s.getCount() < s.getMaxStackSize()) {
                int toAdd = Math.min(s.getMaxStackSize() - s.getCount(), stack.getCount());
                s.grow(toAdd);
                stack.shrink(toAdd);

                if (stack.getCount() <= 0)
                    return;
            }
        }

        for (int i = 0; i < 15; i++) {
            if (slot(i).isEmpty()) {
                inventory.setStackInSlot(i, stack.copyWithCount(1));
                return;
            }
        }
    }

    private void ejectInto(int x, int y, int z) {
        BlockEntity te = level.getBlockEntity(new BlockPos(x, y, z));

        if (te instanceof Container chest) {

            for (int i = 0; i < 15; i++) {
                ItemStack s = slot(i);
                if (!s.isEmpty()) {
                    for (int j = 0; j < chest.getContainerSize(); j++) {
                        ItemStack c = chest.getItem(j);
                        if (!c.isEmpty() && ItemStack.isSameItemSameTags(c, s) && c.getCount() < c.getMaxStackSize()) {
                            s.shrink(1);
                            if (s.isEmpty()) inventory.setStackInSlot(i, ItemStack.EMPTY);
                            c.grow(1);
                            chest.setChanged();
                            setChanged();
                            return;
                        }
                    }
                }
            }

            for (int i = 0; i < 15; i++) {
                ItemStack s = slot(i);
                if (!s.isEmpty()) {
                    for (int j = 0; j < chest.getContainerSize(); j++) {
                        ItemStack sta = s.copyWithCount(1);
                        if (chest.getItem(j).isEmpty() && chest.canPlaceItem(j, sta)) {
                            s.shrink(1);
                            if (s.isEmpty()) inventory.setStackInSlot(i, ItemStack.EMPTY);
                            chest.setItem(j, sta);
                            setChanged();
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == SLOT_CHIP;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.satDock");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new com.hbm_m.inventory.menu.MachineSatDockMenu(id, inv, this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 1, worldPosition.getZ() + 2);
    }

    //? if forge {
    /** Original {@code ISidedInventory}: Slots {0-14}; nichts hinein (nur der Chip-Platz 15 waere gueltig), alles heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return com.hbm_m.blockentity.SidedItemAccess.range(0, 14); }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return false; }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return true; }
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
    //?}
}
