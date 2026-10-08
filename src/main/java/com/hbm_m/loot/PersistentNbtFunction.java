package com.hbm_m.loot;

import com.hbm_m.platform.StackNbt;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.hbm_m.api.block.IPersistentNBT;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/** {@code IPersistentNBT.getDrops}: schreibt die persistenten Daten der Blockentitaet als BlockEntityTag in den Drop. */
public class PersistentNbtFunction extends LootItemConditionalFunction {

    //? if < 1.21.1 {
    protected PersistentNbtFunction(LootItemCondition[] conditions) {
    //?} else {
    /*public static final com.mojang.serialization.MapCodec<PersistentNbtFunction> CODEC =
            com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(i -> commonFields(i).apply(i, PersistentNbtFunction::new));

    protected PersistentNbtFunction(java.util.List<LootItemCondition> conditions) {
    *///?}
        super(conditions);
    }

    public static LootItemConditionalFunction.Builder<?> persistent() {
        return simpleBuilder(PersistentNbtFunction::new);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext ctx) {
        BlockEntity be = ctx.getParamOrNull(LootContextParams.BLOCK_ENTITY);
        if (be instanceof IPersistentNBT persistent) {
            CompoundTag data = new CompoundTag();
            persistent.writeNBT(data);
            if (!data.isEmpty()) com.hbm_m.platform.BlockEntityItemData.write(stack, be.getType(), data);
        }
        return stack;
    }

    @Override
    //? if < 1.21.1 {
    public LootItemFunctionType getType() {
    //?} else {
    /*public LootItemFunctionType<PersistentNbtFunction> getType() {
    *///?}
        return ModLootFunctions.PERSISTENT_NBT.get();
    }

    //? if < 1.21.1 {
    public static class Serializer extends LootItemConditionalFunction.Serializer<PersistentNbtFunction> {
        @Override
        public void serialize(JsonObject json, PersistentNbtFunction fn, JsonSerializationContext ctx) {
            super.serialize(json, fn, ctx);
        }

        @Override
        public PersistentNbtFunction deserialize(JsonObject json, JsonDeserializationContext ctx, LootItemCondition[] conditions) {
            return new PersistentNbtFunction(conditions);
        }
    }
    //?}
}
