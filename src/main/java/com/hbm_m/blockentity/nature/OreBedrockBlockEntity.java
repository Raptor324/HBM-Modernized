package com.hbm_m.blockentity.nature;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;

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
public class OreBedrockBlockEntity extends BlockEntity {

    public ItemStack resource = ItemStack.EMPTY;
    public Fluid acidType = Fluids.EMPTY;
    public int acidAmountMb = 0;
    public int tier = 1;

    public OreBedrockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORE_BEDROCK_BE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!resource.isEmpty()) {
            tag.put("resource", resource.save(new CompoundTag()));
        }
        tag.putString("acid_type", BuiltInRegistries.FLUID.getKey(acidType).toString());
        tag.putInt("acid_amount", acidAmountMb);
        tag.putInt("tier", tier);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("resource")) {
            resource = ItemStack.of(tag.getCompound("resource"));
        }
        if (tag.contains("acid_type")) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("acid_type"));
            acidType = id != null ? BuiltInRegistries.FLUID.get(id) : Fluids.EMPTY;
        }
        acidAmountMb = tag.getInt("acid_amount");
        tier = tag.getInt("tier");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }
}
