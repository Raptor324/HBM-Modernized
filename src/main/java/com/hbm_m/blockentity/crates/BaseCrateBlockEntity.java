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
public abstract class BaseCrateBlockEntity extends BaseHbmBlockEntity implements MenuProvider, com.hbm_m.api.tile.ILockableTile {

    /** 1:1 {@code TileEntityLockableBase}: Schloss der Kiste. */
    public final com.hbm_m.api.tile.LockState lockState = new com.hbm_m.api.tile.LockState();
    /** 1:1 {@code TileEntityCrateBase.hasSpiders}: beim ersten Oeffnen springen drei Hoehlenspinnen heraus. */
    public boolean hasSpiders = false;

    protected final ModItemStackHandler itemHandler;

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

    //? if forge {
    /** Original {@code TileEntityCrateBase}: alle Plaetze von allen Seiten, ein und aus nur ohne Schloss. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> getItemHandler(),
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return com.hbm_m.blockentity.SidedItemAccess.range(0, itemHandler.getSlots() - 1); }
                @Override public boolean canInsert(int slot, ItemStack stack, net.minecraft.core.Direction side) { return canInsertAutomation(slot, stack); }
                @Override public boolean canExtract(int slot, ItemStack stack, net.minecraft.core.Direction side) { return canExtractAutomation(slot, stack); }
            });

    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sidedItems.invalidate();
    }
    //?} elif neoforge {
    /*/^* Original {@code TileEntityCrateBase}: alle Plaetze von allen Seiten, ein und aus nur ohne Schloss. ^/
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> getItemHandler(),
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return com.hbm_m.blockentity.SidedItemAccess.range(0, itemHandler.getSlots() - 1); }
                @Override public boolean canInsert(int slot, ItemStack stack, net.minecraft.core.Direction side) { return canInsertAutomation(slot, stack); }
                @Override public boolean canExtract(int slot, ItemStack stack, net.minecraft.core.Direction side) { return canExtractAutomation(slot, stack); }
            });

    @Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getHbmCapability(cap, side);
    }

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        sidedItems.invalidate();
    }
    *///?}

    /** Original {@code canInsertItem}: {@code isItemValidForSlot && !isLocked}. */
    protected boolean canInsertAutomation(int slot, ItemStack stack) {
        return !isLocked() && itemHandler.isItemValid(slot, stack);
    }

    /** Original {@code canExtractItem}: {@code !isLocked}. */
    protected boolean canExtractAutomation(int slot, ItemStack stack) {
        return !isLocked();
    }

    
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
        if (lockState.isLocked) lockState.write(tag);
        if (hasSpiders) tag.putBoolean("spiders", true);
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
        if (tag.contains("isLocked")) lockState.read(tag);
        hasSpiders = tag.getBoolean("spiders");
    }

    public boolean canAccess(Player player) {
        return lockState.canAccess(level, player);
    }

    // ---- ILockableTile ----
    @Override public com.hbm_m.api.tile.LockState getLockState() { return lockState; }
    @Override public boolean isLocked() { return lockState.isLocked; }
    @Override public void lock() { lockState.lock(this); setChanged(); }
    @Override public void unlock() { lockState.isLocked = false; setChanged(); }
    @Override public void setPins(int pins) { lockState.lock = pins; setChanged(); }
    @Override public int getPins() { return lockState.lock; }
    @Override public void setMod(double mod) { lockState.lockMod = mod; setChanged(); }
    @Override public double getMod() { return lockState.lockMod; }
    @Override public boolean isCheesable() { return lockState.cheesable; }

    /** 1:1 {@code TileEntityCrateBase.fillWithSpiders}. */
    public void fillWithSpiders() {
        this.hasSpiders = true;
        setChanged();
    }

    /** 1:1 {@code TileEntityCrateBase.spawnSpiders}: drei Hoehlenspinnen mit dem Oeffnenden als Ziel. */
    public void spawnSpiders(Player player) {
        if (!hasSpiders || level == null) return;
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 3; i++) {
            net.minecraft.world.entity.monster.CaveSpider spider = net.minecraft.world.entity.EntityType.CAVE_SPIDER.create(level);
            if (spider == null) continue;
            spider.moveTo(worldPosition.getX() + random.nextGaussian() * 2, worldPosition.getY() + 1, worldPosition.getZ() + random.nextGaussian() * 2, random.nextFloat(), 0);
            spider.setTarget(player);
            level.addFreshEntity(spider);
        }
        hasSpiders = false;
        setChanged();
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
