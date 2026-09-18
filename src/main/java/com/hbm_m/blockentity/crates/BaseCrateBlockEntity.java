package com.hbm_m.blockentity.crates;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.crates.CrateValidation;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Базовый BlockEntity для всех ящиков HBM.
 * Управляет инвентарём, сериализацией NBT и capability.
 *
 * <p>Поддержка лут-таблиц для структур: если ящик размещён структурой с тегом
 * {@code LootTable} (как ванильный сундук), содержимое генерируется при первом
 * открытии. Это аналог {@code RandomizableContainerBlockEntity} — в оригинале 1.7.10
 * ящики наполнялись напрямую через {@code Component.generateInvContents} /
 * {@code WeightedRandomChestContent.generateChestContents}; современный эквивалент —
 * JSON лут-таблица, назначаемая {@link com.hbm_m.worldgen.StructureLootProcessor}.</p>
 */
public abstract class BaseCrateBlockEntity extends BaseHbmBlockEntity implements MenuProvider, com.hbm_m.interfaces.ILockable {

    protected final ModItemStackHandler itemHandler;

    // ==================== Замок (порт TileEntityCrateBase extends TileEntityLockableBase) ====================
    /** Пин-код замка. 0 = не установлен. */
    private int lock = 0;
    private boolean locked = false;
    private double lockMod = 0.1D;
    private boolean cheesable = true;

    /** Лут-таблица структуры (null у размещённых игроком ящиков). */
    @Nullable
    private ResourceLocation lootTable;
    /** Сид лут-таблицы (0 = не задан). */
    private long lootTableSeed;

    protected BaseCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state);
        this.itemHandler = new ModItemStackHandler(slots) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return CrateValidation.isValidForCrate(stack);
            }
        };
    }

    // Forge item handler capabilities removed for Fabric compilation.

    
    // (устраняет вложенный stonecutter-баг в load())
    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        if (this.lootTable != null) {
            tag.putString("LootTable", this.lootTable.toString());
            if (this.lootTableSeed != 0L) {
                tag.putLong("LootTableSeed", this.lootTableSeed);
            }
        }
        tag.put("inventory", com.hbm_m.platform.ItemStackSerialization.serialize(itemHandler, registries));
        // Замок (ключи как в оригинальном TileEntityLockableBase)
        tag.putInt("lock", lock);
        tag.putBoolean("locked", locked);
        tag.putDouble("lockMod", lockMod);
        tag.putBoolean("cheesable", cheesable);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        if (tag.contains("LootTable", 8)) {
            this.lootTable = ResourceLocation.parse(tag.getString("LootTable"));
            this.lootTableSeed = tag.getLong("LootTableSeed");
        }
        if (tag.contains("inventory")) {
            com.hbm_m.platform.ItemStackSerialization.deserialize(itemHandler, tag.getCompound("inventory"), registries);
        }
        this.lock = tag.getInt("lock");
        this.locked = tag.getBoolean("locked");
        this.lockMod = tag.contains("lockMod") ? tag.getDouble("lockMod") : 0.1D;
        this.cheesable = !tag.contains("cheesable") || tag.getBoolean("cheesable");
    }

    // ==================== ILockable ====================

    @Override
    public int getPins() {
        return lock;
    }

    @Override
    public void setPins(int pins) {
        this.lock = pins;
        setChanged();
    }

    @Override
    public boolean isLocked() {
        return locked;
    }

    @Override
    public void lock() {
        if (lock == 0) {
            com.hbm_m.main.MainRegistry.LOGGER.error("Attempted to lock a crate with no pins set at {}", worldPosition);
        }
        this.locked = true;
        setChanged();
    }

    @Override
    public void unlock() {
        this.locked = false;
        setChanged();
    }

    @Override
    public double getLockMod() {
        return lockMod;
    }

    @Override
    public void setLockMod(double mod) {
        this.lockMod = mod;
        setChanged();
    }

    @Override
    public boolean isCheesable() {
        return cheesable;
    }

    @Override
    public void setCheesable(boolean cheesable) {
        this.cheesable = cheesable;
        setChanged();
    }

    @Override
    public BlockPos getLockPos() {
        return worldPosition;
    }

    @Override
    public Level getLockLevel() {
        return level;
    }

    public boolean isEmpty() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void saveToItem(ItemStack stack) {
        //? if < 1.21.1 {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag);
        if (!tag.isEmpty()) {
            stack.addTagElement("BlockEntityTag", tag);
        }
        //?} else {
        /*// 1.21.1: addTagElement/saveAdditional(CompoundTag) удалены — сохраняем через DataComponents.
        // level.holderLookup() без аргументов и HolderLookup.direct() удалены в 1.21.1 —
        // берём registryAccess() из level (this.level всегда доступен у размещённого BE).
        net.minecraft.core.HolderLookup.Provider registries = this.level.registryAccess();
        CompoundTag tag = this.saveWithoutMetadata(registries);
        if (!tag.isEmpty()) {
            stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,
                    net.minecraft.world.item.component.CustomData.of(tag));
        }
        *///?}
    }

    public ModItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public int getSlotCount() {
        return itemHandler.getSlots();
    }

    /**
     * Генерирует содержимое из назначенной лут-таблицы структуры при первом
     * открытии. Аналог {@code RandomizableContainerBlockEntity.unpackLootTable}.
     * Безопасно вызывать на любой стороне и при отсутствии лут-таблицы.
     */
    public void unpackLootTable(@Nullable Player player) {
        if (this.lootTable == null) {
            return;
        }
        if (this.level == null || this.level.isClientSide() || !(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        //? if < 1.21.1 {
        LootTable table = serverLevel.getServer().getLootData().getLootTable(this.lootTable);
        //?} else {
        /*// 1.21.1: getLootData() → reloadableRegistries(); ключ лут-таблицы теперь ResourceKey<LootTable>.
        LootTable table = serverLevel.getServer().reloadableRegistries().getLootTable(
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, this.lootTable));
        *///?}
        if (table == LootTable.EMPTY) {
            // Таблица не найдена — очищаем ссылку, чтобы не пытаться повторно.
            this.lootTable = null;
            return;
        }
        LootParams params = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.worldPosition))
                .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                .create(LootContextParamSets.CHEST);
        // Лут заполняется во временный Container, затем копируется в слоты ящика
        // (как ванильные сундуки, которые тоже генерируют весь стек сразу).
        SimpleContainer temp = new SimpleContainer(this.getSlotCount());
        table.fill(temp, params, this.lootTableSeed);
        for (int i = 0; i < temp.getContainerSize() && i < itemHandler.getSlots(); i++) {
            ItemStack stack = temp.getItem(i);
            if (!stack.isEmpty()) {
                itemHandler.setStackInSlot(i, stack.copy());
            }
        }
        this.lootTable = null;
        this.setChanged();
    }
}
