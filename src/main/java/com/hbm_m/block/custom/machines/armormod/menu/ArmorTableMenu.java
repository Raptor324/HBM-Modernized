package com.hbm_m.block.custom.machines.armormod.menu;

// Этот класс отвечает за логику контейнера (меню) стола модификации брони
// Он управляет слотами, перемещением предметов и взаимодействием с инвентарем игрока
import com.hbm_m.block.custom.machines.armormod.item.ItemArmorMod;
import com.hbm_m.block.custom.machines.armormod.util.ArmorModificationHelper;
import com.hbm_m.block.ModBlocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import com.hbm_m.datagen.assets.ModItemTagProvider;
import com.hbm_m.menu.ModMenuTypes;
import com.hbm_m.sound.ModSounds;

import java.util.Map;
import javax.annotation.Nonnull;

public class ArmorTableMenu extends AbstractContainerMenu {

    private final ItemStackHandler armorInventory = new ItemStackHandler(1);
    private final ItemStackHandler modsInventory = new ItemStackHandler(9);
    private final ContainerLevelAccess access;
    private final Player player;
    
    // Индексы для quickMoveStack
    // Правильные индексы для вашего кода

    // Слоты стола
    private static final int SLOT_ARMOR_IN = 0;
    private static final int SLOT_MOD_START = 1;
    // private static final int SLOT_MOD_END = 9; // 9 слотов (1-9)

    // Слоты инвентаря
    private static final int PLAYER_INVENTORY_START = 10;
    // private static final int PLAYER_INVENTORY_END = 36;
    private static final int PLAYER_HOTBAR_START = 37;
    private static final int PLAYER_HOTBAR_END = 45;

    // Боковая панель брони
    public static final int SLOT_ARMOR_SIDE_HELMET = 46;
    public static final int SLOT_ARMOR_SIDE_CHEST = 47;
    public static final int SLOT_ARMOR_SIDE_LEGS = 48;
    public static final int SLOT_ARMOR_SIDE_BOOTS = 49;

    // Общие диапазоны для удобства
    // private static final int TABLE_SLOTS_START = 0;
    // private static final int TABLE_SLOTS_END = 9;
    // private static final int INVENTORY_SLOTS_START = 10;
    // private static final int INVENTORY_SLOTS_END = 45;
    private static final int ARMOR_PANEL_START = 46;
    private static final int ARMOR_PANEL_END = 49;


    /**
     * Конструктор для открытия GUI на сервере (через Block.use)
     */
    public ArmorTableMenu(int pContainerId, Inventory pPlayerInventory, final BlockPos pPos) {
        super(ModMenuTypes.ARMOR_TABLE_MENU.get(), pContainerId);
        this.player = pPlayerInventory.player;
        this.access = ContainerLevelAccess.create(pPlayerInventory.player.level(), pPos);

        // Слот 0: Центральный слот для брони
        this.addSlot(new CentralArmorSlot(armorInventory, 0, 44, 63));
        // Слоты 1-9: Слоты для модов
        this.addSlot(new ModificationSlot(modsInventory, 0, 26, 27));
        this.addSlot(new ModificationSlot(modsInventory, 1, 62, 27));
        this.addSlot(new ModificationSlot(modsInventory, 2, 98, 27));
        this.addSlot(new ModificationSlot(modsInventory, 3, 134, 45));
        this.addSlot(new ModificationSlot(modsInventory, 4, 8, 63));
        this.addSlot(new ModificationSlot(modsInventory, 5, 26, 99));
        this.addSlot(new ModificationSlot(modsInventory, 6, 62, 99));
        this.addSlot(new ModificationSlot(modsInventory, 7, 98, 99));
        this.addSlot(new ModificationSlot(modsInventory, 8, 134, 81));
        
        addPlayerInventory(pPlayerInventory);
        addPlayerHotbar(pPlayerInventory);
        addPlayerArmorSlots(pPlayerInventory);
    }
    
    /**
     * Этот конструктор вызывается на клиенте для синхронизации. Он обязателен.
     */
    public ArmorTableMenu(int pContainerId, Inventory pPlayerInventory, FriendlyByteBuf extraData) {
        this(pContainerId, pPlayerInventory, extraData.readBlockPos());
    }
    
    @Override
    public void removed(@Nonnull Player pPlayer) {
        super.removed(pPlayer);
        if (!pPlayer.level().isClientSide) {
            pPlayer.drop(this.armorInventory.getStackInSlot(0), false);
        }
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(@Nonnull Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack sourceStack = slot.getItem();
            itemstack = sourceStack.copy();

            // Сценарий 1: Клик в инвентаре/хотбаре игрока
            if (pIndex >= PLAYER_INVENTORY_START && pIndex <= PLAYER_HOTBAR_END) {
                // Если предмет - броня
                if (sourceStack.getItem() instanceof ArmorItem armorItem) {
                    // Пытаемся положить в центральный слот -> затем надеть
                    if (!this.moveItemStackTo(sourceStack, SLOT_ARMOR_IN, SLOT_ARMOR_IN + 1, false)) {
                        EquipmentSlot equipmentSlot = armorItem.getEquipmentSlot();
                        int targetSlotIndex = -1;
                        if (equipmentSlot == EquipmentSlot.HEAD) targetSlotIndex = SLOT_ARMOR_SIDE_HELMET;
                        else if (equipmentSlot == EquipmentSlot.CHEST) targetSlotIndex = SLOT_ARMOR_SIDE_CHEST;
                        else if (equipmentSlot == EquipmentSlot.LEGS) targetSlotIndex = SLOT_ARMOR_SIDE_LEGS;
                        else if (equipmentSlot == EquipmentSlot.FEET) targetSlotIndex = SLOT_ARMOR_SIDE_BOOTS;
                        
                        if (targetSlotIndex == -1 || !this.moveItemStackTo(sourceStack, targetSlotIndex, targetSlotIndex + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
                else if (sourceStack.getItem() instanceof ItemArmorMod mod) {
                    boolean movedToTable = false;

                    // Проверяем, есть ли броня в центральном слоте
                    ItemStack armorStack = this.slots.get(SLOT_ARMOR_IN).getItem();
                    if (!armorStack.isEmpty()) {
                        // Определяем целевой слот на основе типа мода
                        // Слот мода в меню = тип мода + 1 (т.к. слот 0 это броня)
                        int targetModSlotIndex = mod.type + SLOT_MOD_START; 
                        Slot targetSlot = this.slots.get(targetModSlotIndex);

                        // Проверяем, можно ли поместить мод в этот слот
                        // mayPlace уже содержит всю нужную логику: совместимость с броней, тип слота и т.д.
                        // Также проверяем, что слот свободен
                        if (!targetSlot.hasItem() && targetSlot.mayPlace(sourceStack)) {
                            // Если все проверки пройдены, пытаемся переместить мод в его слот
                            if (this.moveItemStackTo(sourceStack, targetModSlotIndex, targetModSlotIndex + 1, false)) {
                                pPlayer.level().playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), ModSounds.REPAIR_RANDOM.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
                                movedToTable = true;
                            }
                        }
                    }

                    // Если переместить в стол не удалось (нет брони, слот занят, несовместимость),
                    // выполняем стандартное перемещение инвентарь <-> хотбар
                    if (!movedToTable) {
                        if (pIndex >= PLAYER_INVENTORY_START && pIndex < PLAYER_HOTBAR_START) {
                            if (!this.moveItemStackTo(sourceStack, PLAYER_HOTBAR_START, PLAYER_HOTBAR_END + 1, false)) return ItemStack.EMPTY;
                        } else {
                            if (!this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_START, false)) return ItemStack.EMPTY;
                        }
                    }
                }
                // Для всех остальных предметов
                else {
                    if (pIndex >= PLAYER_INVENTORY_START && pIndex < PLAYER_HOTBAR_START) {
                        if (!this.moveItemStackTo(sourceStack, PLAYER_HOTBAR_START, PLAYER_HOTBAR_END + 1, false)) return ItemStack.EMPTY;
                    } else {
                        if (!this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_START, false)) return ItemStack.EMPTY;
                    }
                }
            }
            // Сценарий 2: Клик в слотах GUI стола
            else {
                // Клик по надетой броне (боковая панель)
                if (pIndex >= ARMOR_PANEL_START && pIndex <= ARMOR_PANEL_END) {
                    // Сначала пытаемся переместить в центральный слот
                    boolean movedToCenter = this.moveItemStackTo(sourceStack, SLOT_ARMOR_IN, SLOT_ARMOR_IN + 1, false);

                    if (movedToCenter) {
                        // Если перемещение удалось, воспроизводим звук снятия брони
                        if (itemstack.getItem() instanceof ArmorItem armorItem) {
                            pPlayer.level().playSound(null, pPlayer.blockPosition(), armorItem.getEquipSound(), SoundSource.PLAYERS, 1.0F, 1.0F);
                        }
                    } else {
                        // Если в центр переместить не удалось, пытаемся в инвентарь
                        if (!this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
                //ц Клик по броне в центральном слоте
                else if (pIndex == SLOT_ARMOR_IN) {
                    // ПОПЫТКА 1: Экипировать броню (переместить в боковую панель)
                    int targetSlotIndex = -1;
                    if (sourceStack.getItem() instanceof ArmorItem armorItem) {
                        EquipmentSlot slotType = armorItem.getEquipmentSlot();
                        if (slotType == EquipmentSlot.HEAD) targetSlotIndex = SLOT_ARMOR_SIDE_HELMET;
                        else if (slotType == EquipmentSlot.CHEST) targetSlotIndex = SLOT_ARMOR_SIDE_CHEST;
                        else if (slotType == EquipmentSlot.LEGS) targetSlotIndex = SLOT_ARMOR_SIDE_LEGS;
                        else if (slotType == EquipmentSlot.FEET) targetSlotIndex = SLOT_ARMOR_SIDE_BOOTS;
                    }

                    boolean equipped = targetSlotIndex != -1 && this.moveItemStackTo(sourceStack, targetSlotIndex, targetSlotIndex + 1, false);

                    // ПОПЫТКА 2: Если экипировать не удалось, переместить в инвентарь игрока
                    if (!equipped) {
                        if (!this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
                // Для всех остальных слотов стола (моды) -> в инвентарь
                else if (!this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END + 1, false)) {
                    pPlayer.level().playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), ModSounds.EXTRACT_RANDOM.get(), SoundSource.PLAYERS, 0.4F, 1.0F);
                    return ItemStack.EMPTY;
                }
            }

            // Стандартный код завершения
            if (sourceStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (sourceStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(pPlayer, sourceStack);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(@Nonnull Player pPlayer) {
        return stillValid(access, pPlayer, ModBlocks.ARMOR_TABLE.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 140 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 198));
        }
    }

    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private void addPlayerArmorSlots(Inventory playerInventory) {
        for(int i = 0; i < ARMOR_SLOTS.length; ++i) {
            final EquipmentSlot slotType = ARMOR_SLOTS[i];
            
            this.addSlot(new ArmorSidePanelSlot(
                    playerInventory,     // 1. Инвентарь, которому принадлежит слот
                    39 - i,              // 2. Индекс слота в инвентаре игрока
                    -17, 36 + i * 18,    // 3. Координаты X и Y
                    player,              // 4. Игрок (для звука)
                    slotType             // 5. Тип слота (для звука и валидации)
            ));
        }
    }
    private class CentralArmorSlot extends SlotItemHandler implements IHasTooltip {
        
        public CentralArmorSlot(ItemStackHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public Component getEmptyTooltip() {
            return Component.translatable("tooltip.hbm_m.armor_table.main_slot").withStyle(ChatFormatting.YELLOW);
        }
        
        @Override
        public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof ArmorItem; }
        @Override
        public int getMaxStackSize() { return 1; }
        @Override
        public void set(ItemStack pStack) {
            super.set(pStack);
            // Когда кладем броню, загружаем моды из ее NBT в стол
            ArmorModificationHelper.loadModsIntoTable(pStack, modsInventory);
        }
    }

    private class ModificationSlot extends SlotItemHandler implements IHasTooltip {
        
        private static final Map<Integer, String> TOOLTIP_KEYS = Map.of(
            0, "tooltip.hbm_m.armor_table.helmet_slot",
            1, "tooltip.hbm_m.armor_table.chestplate_slot",
            2, "tooltip.hbm_m.armor_table.leggings_slot",
            3, "tooltip.hbm_m.armor_table.boots_slot",
            4, "tooltip.hbm_m.armor_table.battery_slot",
            5, "tooltip.hbm_m.armor_table.special_slot",
            6, "tooltip.hbm_m.armor_table.plating_slot",
            7, "tooltip.hbm_m.armor_table.casing_slot",
            8, "tooltip.hbm_m.armor_table.servos_slot"
        );
        
        public ModificationSlot(ItemStackHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public Component getEmptyTooltip() {
            String key = TOOLTIP_KEYS.getOrDefault(this.getSlotIndex(), "");
            return Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // Проверка 1: Это вообще мод?
            if (!(stack.getItem() instanceof ItemArmorMod mod)) {
                return false;
            }

            // Проверка 2: Есть ли броня в центральном слоте? (остается)
            ItemStack armorStack = armorInventory.getStackInSlot(0);
            if (armorStack.isEmpty() || !(armorStack.getItem() instanceof ArmorItem armorItem)) {
                return false;
            }
            
            // Проверка 3: Соответствует ли тип мода этому физическому слоту? (остается)
            if (mod.type != this.getSlotIndex()) {
                return false;
            }
            // Проверка 4: Совместим ли мод с ТИПОМ БРОНИ?
            ArmorItem.Type armorType = armorItem.getType();

            return switch (armorType) {
                case HELMET -> stack.is(ModItemTagProvider.REQUIRES_HELMET);
                case CHESTPLATE -> stack.is(ModItemTagProvider.REQUIRES_CHESTPLATE);
                case LEGGINGS -> stack.is(ModItemTagProvider.REQUIRES_LEGGINGS);
                case BOOTS -> stack.is(ModItemTagProvider.REQUIRES_BOOTS);
                default -> false; // На случай будущих типов брони
            };
        }

        @Override
        public void set(ItemStack pStack) {
            ItemStack oldStack = this.getItem(); // Запоминаем, что было в слоте
            super.set(pStack); // Помещаем новый предмет

            // Проверяем, что мы именно ПОЛОЖИЛИ новый предмет, а не забрали старый
            if (!pStack.isEmpty() && !ItemStack.matches(oldStack, pStack)) {
                ArmorTableMenu.this.player.level().playSound(null, ArmorTableMenu.this.player.getX(), ArmorTableMenu.this.player.getY(), ArmorTableMenu.this.player.getZ(), ModSounds.REPAIR_RANDOM.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
            }

            // Когда СТАВИМ мод, пересчитываем NBT брони
            ItemStack armor = armorInventory.getStackInSlot(0);
            if (!armor.isEmpty()) {
                ArmorModificationHelper.saveTableToArmor(armor, modsInventory, ArmorTableMenu.this.player);
            }
        }

        @Override
        public void onTake(@Nonnull Player pPlayer, @Nonnull ItemStack pStack) {
            super.onTake(pPlayer, pStack);
            pPlayer.level().playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), ModSounds.EXTRACT_RANDOM.get(), SoundSource.PLAYERS, 0.4F, 1.0F);
            // Когда ЗАБИРАЕМ мод, тоже пересчитываем NBT брони
            ItemStack armor = armorInventory.getStackInSlot(0);
            if (!armor.isEmpty()) {
                ArmorModificationHelper.saveTableToArmor(armor, modsInventory, ArmorTableMenu.this.player);
            }
        }
    }
}