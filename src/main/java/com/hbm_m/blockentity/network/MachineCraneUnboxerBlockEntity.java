package com.hbm_m.blockentity.network;

import java.util.EnumMap;
import java.util.Map;

import com.hbm_m.block.machines.MachineCraneUnboxerBlock;
import com.hbm_m.block.network.IConveyorBelt;
import com.hbm_m.block.network.IEnterablePackageBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.conveyor.MovingConveyorItemEntity;
import com.hbm_m.entity.conveyor.MovingConveyorPackageEntity;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.menu.MachineCraneUnboxerMenu;
import com.hbm_m.item.industrial.ItemMachineUpgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Crane Unboxer - Port von {@code TileEntityCraneUnboxer} (1.7.10 Original). Nimmt Pakete
 * ({@link MovingConveyorPackageEntity}) an ihrer {@link #FACING}-Seite an, entpackt sie in einen
 * 21-Slot-Puffer, und gibt daraus periodisch Einzelitems auf der gegenueberliegenden Seite wieder
 * auf ein Foerderband aus ("switcheroo" wie im Original - Annahmeseite und Ausgabeseite sind
 * vertauscht relativ zu Crane Boxer). Stack-/Ejector-Upgrades wie bei Crane Extractor.
 */
public class MachineCraneUnboxerBlockEntity extends BaseMachineBlockEntity implements IEnterablePackageBlock {

    public static final int BUFFER_START = 0;
    public static final int BUFFER_END = 20;
    public static final int SLOT_UPGRADE_STACK = 21;
    public static final int SLOT_UPGRADE_EJECTOR = 22;
    public static final int INVENTORY_SIZE = 23;

    private final UpgradeManager upgradeManager = new UpgradeManager();

    private static final Map<ItemMachineUpgrade.UpgradeType, Integer> UPGRADE_CAPS = new EnumMap<>(ItemMachineUpgrade.UpgradeType.class);
    static {
        UPGRADE_CAPS.put(ItemMachineUpgrade.UpgradeType.STACK, 3);
        UPGRADE_CAPS.put(ItemMachineUpgrade.UpgradeType.EJECTOR, 3);
    }

    public MachineCraneUnboxerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_UNBOXER_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCraneUnboxerBlockEntity be) {
        if (!level.isClientSide) {
            be.serverTick(level, pos, state);
        }
    }

    private void serverTick(Level level, BlockPos pos, BlockState state) {
        upgradeManager.checkSlots(inventory, SLOT_UPGRADE_STACK, SLOT_UPGRADE_EJECTOR, UPGRADE_CAPS);

        int delay = switch (upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.EJECTOR)) {
            case 1 -> 10;
            case 2 -> 5;
            case 3 -> 2;
            default -> 20;
        };

        if (level.getGameTime() % delay != 0) return;
        if (level.hasNeighborSignal(pos)) return;

        int amount = switch (upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.STACK)) {
            case 1 -> 4;
            case 2 -> 16;
            case 3 -> 64;
            default -> 1;
        };

        Direction facing = state.getValue(MachineCraneUnboxerBlock.FACING);
        Direction outputSide = facing.getOpposite();
        BlockPos outPos = pos.relative(outputSide);

        var outBlock = level.getBlockState(outPos).getBlock();
        if (!(outBlock instanceof IConveyorBelt belt)) return;

        for (int i = BUFFER_START; i <= BUFFER_END; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            int toSend = Math.min(amount, stack.getCount());
            ItemStack sendStack = stack.copy();
            sendStack.setCount(toSend);
            stack.shrink(toSend);
            if (stack.isEmpty()) inventory.setStackInSlot(i, ItemStack.EMPTY);

            var snap = belt.snapNewItem(level, outPos, new net.minecraft.world.phys.Vec3(
                    outPos.getX() + 0.5, outPos.getY() + 0.5, outPos.getZ() + 0.5));
            MovingConveyorItemEntity moving = MovingConveyorItemEntity.create(level, snap.x, snap.y, snap.z, sendStack);
            level.addFreshEntity(moving);
            break;
        }

        setChanged();
    }

    // ── IEnterablePackageBlock ────────────────────────────────────────────────

    @Override
    public void onPackageEnter(Level level, BlockPos pos, MovingConveyorPackageEntity item) {
        if (level.isClientSide) return;

        for (ItemStack stack : item.getContents()) {
            if (stack.isEmpty()) continue;
            ItemStack remainder = insertIntoBuffer(stack.copy());
            if (!remainder.isEmpty()) {
                ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, remainder);
                level.addFreshEntity(drop);
            }
        }
        setChanged();
    }

    private ItemStack insertIntoBuffer(ItemStack stack) {
        for (int i = BUFFER_START; i <= BUFFER_END && !stack.isEmpty(); i++) {
            ItemStack current = inventory.getStackInSlot(i);
            if (current.isEmpty()) {
                inventory.setStackInSlot(i, stack);
                return ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameTags(current, stack)) {
                int space = current.getMaxStackSize() - current.getCount();
                if (space > 0) {
                    int toMove = Math.min(space, stack.getCount());
                    current.grow(toMove);
                    stack.shrink(toMove);
                }
            }
        }
        return stack;
    }

    // ── Slot validation / Menu ─────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot >= BUFFER_START && slot <= BUFFER_END) return true;
        if (slot == SLOT_UPGRADE_STACK) return stack.getItem() instanceof ItemMachineUpgrade up && up.getUpgradeType() == ItemMachineUpgrade.UpgradeType.STACK;
        if (slot == SLOT_UPGRADE_EJECTOR) return stack.getItem() instanceof ItemMachineUpgrade up && up.getUpgradeType() == ItemMachineUpgrade.UpgradeType.EJECTOR;
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.crane_unboxer");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineCraneUnboxerMenu.create(id, inventory, this);
    }
}
