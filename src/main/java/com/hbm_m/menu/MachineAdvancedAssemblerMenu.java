package com.hbm_m.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.machine.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.item.ItemBlueprintFolder;
import com.hbm_m.util.LongDataPacker; // <-- Убедись, что этот импорт есть
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

public class MachineAdvancedAssemblerMenu extends AbstractContainerMenu {

    public final MachineAdvancedAssemblerBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    // Конструктор, вызываемый с сервера
    public MachineAdvancedAssemblerMenu(int pContainerId, Inventory pPlayerInventory, MachineAdvancedAssemblerBlockEntity pBlockEntity, ContainerData pData) {
        super(ModMenuTypes.ADVANCED_ASSEMBLY_MACHINE_MENU.get(), pContainerId);
        // Убедимся, что данных пришло нужное количество
        checkContainerDataCount(pData, 8);
        this.blockEntity = pBlockEntity;
        this.level = pPlayerInventory.player.level();
        this.data = pData;

        // Добавляем слоты машины
        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            this.addSlot(new SlotItemHandler(handler, 0, 152, 81)); // Energy
            this.addSlot(new SlotItemHandler(handler, 1, 34, 126)); // Blueprint
            this.addSlot(new SlotItemHandler(handler, 2, 152, 108)); // Upgrade
            this.addSlot(new SlotItemHandler(handler, 3, 152, 126)); // Upgrade
            for (int i = 0; i < 4; ++i) for (int j = 0; j < 3; ++j) { // Input
                this.addSlot(new SlotItemHandler(handler, 4 + (i * 3) + j, 8 + j * 18, 18 + i * 18));
            }
            this.addSlot(new SlotItemHandler(handler, 16, 98, 45)); // Output
        });

        addPlayerInventory(pPlayerInventory);
        addPlayerHotbar(pPlayerInventory);
        addDataSlots(pData);
    }

    // Конструктор, вызываемый с клиента
    public MachineAdvancedAssemblerMenu(int pContainerId, Inventory pPlayerInventory, FriendlyByteBuf pExtraData) {
        this(pContainerId, pPlayerInventory, getBlockEntity(pPlayerInventory, pExtraData), new SimpleContainerData(8));
    }

    private static MachineAdvancedAssemblerBlockEntity getBlockEntity(final Inventory playerInventory, final FriendlyByteBuf data) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(data.readBlockPos());
        if (be instanceof MachineAdvancedAssemblerBlockEntity) {
            return (MachineAdvancedAssemblerBlockEntity) be;
        }
        throw new IllegalStateException("BlockEntity не найден или имеет неверный тип!");
    }

    // --- Логика для GUI (с правильными индексами) ---

    public int getProgress() {
        return this.data.get(0);
    }

    public int getMaxProgress() {
        return this.data.get(1);
    }

    public boolean isCrafting() {
        return getProgress() > 0;
    }

    public long getEnergyLong() {
        int high = this.data.get(2); // Индекс 3: energy high
        int low = this.data.get(3);  // Индекс 2: energy low
        return LongDataPacker.unpack(high, low);
    }

    public long getMaxEnergyLong() {
        int high = this.data.get(4); // Индекс 5: maxEnergy high
        int low = this.data.get(5);  // Индекс 4: maxEnergy low
        return LongDataPacker.unpack(high, low);
    }

    public long getEnergyDeltaLong() {
        int high = this.data.get(6); // Индекс 7: delta high
        int low = this.data.get(7);  // Индекс 6: delta low
        return LongDataPacker.unpack(high, low);
    }

    @Override
    public boolean stillValid(@NotNull Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                pPlayer, ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get());
    }

    // Логика Shift-клика (без изменений)
    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player playerIn, int pIndex) {
        // ... (твой код)
        return ItemStack.EMPTY;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 174 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 232));
        }
    }

    public MachineAdvancedAssemblerBlockEntity getBlockEntity() {
        return this.blockEntity;
    }
}