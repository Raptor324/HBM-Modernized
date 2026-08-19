package com.hbm_m.blockentity.nature;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * 1:1-Aequivalent zu {@code BlockBedrockOreTE.TileEntityBedrockOre} aus dem 1.7.10-Original:
 * haelt das per Ring-Mining zu gewinnende Item, den benoetigten Bohr-Fluid-Typ/Menge und den
 * Mindest-Drillbit-Tier. Wird beim Weltgen einmalig gesetzt (siehe {@link com.hbm_m.worldgen.BedrockOreFeature}).
 */
public class OreBedrockBlockEntity extends BaseHbmBlockEntity {

    public ItemStack resource = ItemStack.EMPTY;
    public Fluid acidType = Fluids.EMPTY;
    public int acidAmountMb = 0;
    public int tier = 1;

    public OreBedrockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORE_BEDROCK_BE.get(), pos, state);
    }

    
    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        if (!resource.isEmpty()) {
            // PlatformHooks.saveItemStack null-safe: 1.20.1 — stack.save(tag), 1.21.1 — через Provider
            tag.put("resource", PlatformHooks.saveItemStack(resource, new CompoundTag(), registries));
        }
        tag.putString("acid_type", BuiltInRegistries.FLUID.getKey(acidType).toString());
        tag.putInt("acid_amount", acidAmountMb);
        tag.putInt("tier", tier);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        if (tag.contains("resource")) {
            // PlatformHooks.itemStackOf null-safe: 1.20.1 — ItemStack.of, 1.21.1 — через Provider
            resource = PlatformHooks.itemStackOf(tag.getCompound("resource"), registries);
        }
        if (tag.contains("acid_type")) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("acid_type"));
            acidType = id != null ? BuiltInRegistries.FLUID.get(id) : Fluids.EMPTY;
        }
        acidAmountMb = tag.getInt("acid_amount");
        tier = tag.getInt("tier");
    }

    // getUpdateTag удалён: семантика (super + saveAdditional) полностью покрывается BaseHbmBlockEntity.
}
