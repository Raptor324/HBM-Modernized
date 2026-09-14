package com.hbm_m.platform.recipe;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

//? if >= 1.21.1 {
/*import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import java.util.stream.Stream;
*///?}

public abstract class PlatformRecipeSerializer<R extends Recipe<?>> implements RecipeSerializer<R> {

    public abstract R readJson(ResourceLocation id, JsonObject json);
    public abstract R readNetwork(ResourceLocation id, FriendlyByteBuf buf);
    public abstract void writeNetwork(FriendlyByteBuf buf, R recipe);

    //? if < 1.21.1 {
    
    @Override
    public @NotNull R fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
        return readJson(id, json);
    }

    @Override
    public R fromNetwork(@NotNull ResourceLocation id, @NotNull FriendlyByteBuf buf) {
        return readNetwork(id, buf);
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buf, @NotNull R recipe) {
        writeNetwork(buf, recipe);
    }
    //?} else {
    
    /*/^* RecipeManager assigns the real id, so the codec only ever needs this placeholder. ^/
    private static final ResourceLocation DUMMY_ID = ResourceLocation.withDefaultNamespace("dummy");

    private final MapCodec<R> mapCodec = new MapCodec<R>() {
        @Override
        public <T> Stream<T> keys(com.mojang.serialization.DynamicOps<T> ops) {
            return Stream.empty();
        }

        @Override
        public <T> DataResult<R> decode(com.mojang.serialization.DynamicOps<T> ops, MapLike<T> input) {
            try {
                T map = ops.createMap(input.entries());
                // Recipes already load through JsonOps. Converting again walks arrays element by
                // element and NPEs on JsonNull, which recipes use for empty grid slots.
                JsonObject json = map instanceof JsonObject obj
                        ? obj
                        : new com.mojang.serialization.Dynamic<>(ops, map).convert(JsonOps.INSTANCE).getValue().getAsJsonObject();
                return DataResult.success(readJson(DUMMY_ID, json));
            } catch (Exception e) {
                // RecipeManager only surfaces the message, so log the cause with its stack trace.
                com.hbm_m.main.MainRegistry.LOGGER.error("Failed to parse recipe", e);
                return DataResult.error(() -> "Failed to parse recipe: " + e.getMessage());
            }
        }

        @Override
        public <T> RecordBuilder<T> encode(R input, com.mojang.serialization.DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return prefix; 
        }
    };

    private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec = new StreamCodec<RegistryFriendlyByteBuf, R>() {
        @Override
        public R decode(RegistryFriendlyByteBuf buf) {
            return readNetwork(DUMMY_ID, buf);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, R recipe) {
            writeNetwork(buf, recipe);
        }
    };

    @Override
    public MapCodec<R> codec() {
        return mapCodec;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
        return streamCodec;
    }
    *///?}
}