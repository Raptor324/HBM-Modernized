package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.albion.CooledMachineBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Gemeinsames Menue aller Bauteile des Teilchenbeschleunigers.
 *
 * <p>Die fuenf Bauteile unterscheiden sich nur in ihren Steckplaetzen, darum steht hier eine
 * parametrierte Fassung statt fuenf fast gleicher Klassen. Die Koordinaten stammen 1:1 aus den
 * {@code ContainerPA*}-Klassen von 1.7.10; das Spielerinventar sitzt bei allen auf Zeile 122
 * beziehungsweise 180.</p>
 */
public class PAMenu extends AbstractContainerMenu {

    /** Ein Steckplatz: Index in der Maschine, Bildschirmkoordinaten, und ob man hineinlegen darf. */
    public record SlotSpec(int index, int x, int y, boolean insertable) {
        public static SlotSpec in(int index, int x, int y)  { return new SlotSpec(index, x, y, true); }
        public static SlotSpec out(int index, int x, int y) { return new SlotSpec(index, x, y, false); }
    }

    private final CooledMachineBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;
    private final Block block;
    private final int machineSlots;

    public PAMenu(MenuType<?> type, int id, Inventory inv, CooledMachineBlockEntity blockEntity,
                  ContainerData data, Block block, SlotSpec... specs) {
        super(type, id);
        this.blockEntity = blockEntity;
        this.level = inv.player.level();
        this.data = data;
        this.block = block;
        this.machineSlots = specs.length;

        if (data != null) {
            addDataSlots(data);
        }

        ModItemStackHandlerContainer container =
                new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        for (SlotSpec spec : specs) {
            this.addSlot(new Slot(container, spec.index(), spec.x(), spec.y()) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return spec.insertable() && super.mayPlace(stack);
                }
            });
        }

        // Original: Spielerinventar bei 122, Schnellleiste bei 180.
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 122 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, 180));
        }
    }

    protected static CooledMachineBlockEntity readBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof CooledMachineBlockEntity cooled) {
            return cooled;
        }
        throw new IllegalStateException("BlockEntity is not a particle accelerator part");
    }

    public CooledMachineBlockEntity getBlockEntity() {
        return blockEntity;
    }

    protected ContainerData data() {
        return data;
    }

    /** Temperatur in Kelvin - alle Bauteile zeigen sie an. */
    public float getTemperature() {
        return blockEntity.getTemperature();
    }

    public boolean isCool() {
        return blockEntity.isCool();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(stack, 0, machineSlots, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, block);
    }
}
