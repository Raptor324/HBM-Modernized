package com.hbm_m.blockentity.decorations;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт {@code BlockLoot.TileEntityLoot} (1.7.10) — содержимое «груды лута»
 * ({@code deco_loot}). Хранит предметы с относительными смещениями (стопка
 * хлама лежит на «коврике»), клиент рендерит их через
 * {@link com.hbm_m.client.render.implementations.DecoLootRenderer}.
 *
 * <p>В 1.7.10 лут заливался генератором структур ({@code LootGenerator.loot*});
 * после конвертации структур в NBT-шаблоны эта информация потеряна, поэтому
 * при первой загрузке на сервере пустой TE генерирует стопку из вневесового
 * пула ниже (эквивалент прежней лут-таблицы blocks/deco_loot).
 */
public class DecoLootBlockEntity extends BaseHbmBlockEntity {

    /** Предмет + смещение относительно блока (в блоках, центр = 0.5). */
    public record LootEntry(ItemStack stack, double dx, double dy, double dz) {}

    /** Пул хлама (порт пулов LootGenerator / старой datagen-таблицы). */
    private static final Object[][] POOL = {
            {Items.BONE, 20}, {Items.STICK, 25}, {Items.STRING, 15},
            {Items.IRON_NUGGET, 20}, {Items.COAL, 15}, {Items.PAPER, 10},
            {com.hbm_m.item.material.ModMaterialItems.item(com.hbm_m.item.material.ModMaterials.SCRAP, com.hbm_m.item.material.MaterialShape.SCRAP), 30},
            {com.hbm_m.item.ModItems.CAP_NUKA.get(), 10},
            {com.hbm_m.item.ModItems.CANNED_BEEF.get(), 8},
            {com.hbm_m.item.ModItems.CANNED_LEFTOVERS.get(), 8},
            {com.hbm_m.item.ModItems.CANNED_MYSTERY.get(), 5},
            {com.hbm_m.item.ModItems.BANDAID.get(), 6},
            {com.hbm_m.item.ModItems.CAPACITOR.get(), 5},
            {com.hbm_m.item.ModItems.BATTERY_POTATO.get(), 3},
            {com.hbm_m.item.ModItems.FRAGMENT_BORON.get(), 2},
            {com.hbm_m.item.ModItems.FRAGMENT_LANTHANIUM.get(), 2},
    };

    private final List<LootEntry> items = new ArrayList<>();

    public DecoLootBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DECO_LOOT_BE.get(), pos, state);
    }

    public List<LootEntry> getItems() {
        return items;
    }

    /**
     * Добавляет стопку в «кучу» с ручным смещением (порт
     * {@code TileEntityLoot.addItem}). Используется генератором красной
     * комнаты; пока список непуст, автогенерация хлама в {@link #onLoad}
     * не запускается.
     */
    public void addItem(ItemStack stack, double dx, double dy, double dz) {
        items.add(new LootEntry(stack, dx, dy, dz));
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && items.isEmpty()) {
            generateLoot(level.random);
        }
    }

    private void generateLoot(RandomSource rand) {
        int totalWeight = 0;
        for (Object[] entry : POOL) totalWeight += (int) entry[1];

        int count = 2 + rand.nextInt(4); // 2..5 стопок
        for (int i = 0; i < count; i++) {
            int roll = rand.nextInt(totalWeight);
            Item item = Items.STICK;
            for (Object[] entry : POOL) {
                roll -= (int) entry[1];
                if (roll < 0) {
                    item = (Item) entry[0];
                    break;
                }
            }
            ItemStack stack = new ItemStack(item, 1 + rand.nextInt(3));
            // Хлам лежит «кучкой»: разброс по горизонтали, стопкой по вертикали
            double dx = (rand.nextDouble() - 0.5D) * 0.5D;
            double dz = (rand.nextDouble() - 0.5D) * 0.5D;
            double dy = i * 0.03125D;
            items.add(new LootEntry(stack, dx, dy, dz));
        }

        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (LootEntry entry : items) {
            if (entry.stack().isEmpty()) continue;
            CompoundTag itemTag = new CompoundTag();
            itemTag.put("stack", PlatformHooks.saveItemStack(entry.stack(), new CompoundTag(), registries));
            itemTag.putDouble("dx", entry.dx());
            itemTag.putDouble("dy", entry.dy());
            itemTag.putDouble("dz", entry.dz());
            list.add(itemTag);
        }
        tag.put("items", list);
    }

    @Override
    protected void readNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        items.clear();
        ListTag list = tag.getList("items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompound(i);
            ItemStack stack = PlatformHooks.itemStackOf(itemTag.getCompound("stack"), registries);
            if (stack.isEmpty()) continue;
            items.add(new LootEntry(stack,
                    itemTag.getDouble("dx"), itemTag.getDouble("dy"), itemTag.getDouble("dz")));
        }
    }
}
