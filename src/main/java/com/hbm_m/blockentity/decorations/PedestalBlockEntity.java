package com.hbm_m.blockentity.decorations;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт {@code BlockPedestal.TileEntityPedestal} (1.7.10).
 * Хранит один стек, который клиент рендерит парящим над постаментом
 * ({@link com.hbm_m.client.render.implementations.PedestalRenderer}).
 */
public class PedestalBlockEntity extends BaseHbmBlockEntity {

    private ItemStack item = ItemStack.EMPTY;

    public PedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PEDESTAL_BE.get(), pos, state);
    }

    /** Original {@code updateEntity}: Amulette melden sich jede Sekunde, der Gold-Entschaerfer entschaerft alle 3 s Creeper (25). */
    public static void tick(net.minecraft.world.level.Level world, BlockPos pos, BlockState state, PedestalBlockEntity be) {
        if (world.getGameTime() % 20 != 0 || be.item.isEmpty()) return;

        if (be.item.is(com.hbm_m.item.ModItems.PROTECTION_CHARM.get()))
            com.hbm_m.block.decorations.PedestalBlock.pushPedestalEntry(world, com.hbm_m.block.decorations.PedestalBlock.PedestalEntryType.CHARM_OF_PROTECTION, pos);
        if (be.item.is(com.hbm_m.item.ModItems.METEOR_CHARM.get()))
            com.hbm_m.block.decorations.PedestalBlock.pushPedestalEntry(world, com.hbm_m.block.decorations.PedestalBlock.PedestalEntryType.METEORITE_CHARM, pos);
        if (world.getGameTime() % 60 == 0 && be.item.is(com.hbm_m.item.ModItems.DEFUSER_GOLD.get())) {
            for (net.minecraft.world.entity.monster.Creeper creeper : world.getEntitiesOfClass(net.minecraft.world.entity.monster.Creeper.class, new net.minecraft.world.phys.AABB(pos).inflate(25, 25, 25)))
                com.hbm_m.armormod.item.ItemModDefuser.castrateCreeper(creeper, null, false);
        }
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack stack) {
        this.item = stack;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void writeNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        if (!item.isEmpty()) {
            tag.put("item", PlatformHooks.saveItemStack(item, new net.minecraft.nbt.CompoundTag(), registries));
        }
    }

    @Override
    protected void readNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        item = tag.contains("item") ? PlatformHooks.itemStackOf(tag.getCompound("item"), registries) : ItemStack.EMPTY;
    }
}
