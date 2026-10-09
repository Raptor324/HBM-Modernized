//? if forge {
package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import com.hbm_m.inventory.FluidContainerRegistry;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;

/**
 * Ersatz fuer {@code Fluids.X.getDict(menge)} (Ore-Dict "container&lt;menge&gt;&lt;fluessigkeit&gt;"): passt auf jeden in der
 * {@link FluidContainerRegistry} eingetragenen vollen Behaelter mit genau dieser Menge dieser Fluessigkeit.
 * JSON: {@code {"type": "hbm_m:fluid_container", "fluid": "hbm_m:lubricant", "amount": 1000}}.
 */
public class FluidContainerIngredient extends AbstractIngredient {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "fluid_container");

    private final Fluid fluid;
    private final int amount;
    private ItemStack[] display;

    public FluidContainerIngredient(Fluid fluid, int amount) {
        this.fluid = fluid;
        this.amount = amount;
    }

    public static FluidContainerIngredient of(Fluid fluid, int amount) {
        return new FluidContainerIngredient(fluid, amount);
    }

    public static void register() {
        CraftingHelper.register(ID, Serializer.INSTANCE);
    }

    @Override
    public boolean test(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return FluidContainerRegistry.getFluidContent(stack, fluid) == amount;
    }

    @Override
    public ItemStack[] getItems() {
        if (display == null) {
            List<ItemStack> list = new ArrayList<>();
            for (FluidContainerRegistry.FluidContainer c : FluidContainerRegistry.getContainers(fluid)) {
                if (c.content() == amount) list.add(c.fullContainer().copy());
            }
            display = list.toArray(new ItemStack[0]);
        }
        return display;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IIngredientSerializer<? extends net.minecraft.world.item.crafting.Ingredient> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public com.google.gson.JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", ID.toString());
        json.addProperty("fluid", BuiltInRegistries.FLUID.getKey(fluid).toString());
        json.addProperty("amount", amount);
        return json;
    }

    public static class Serializer implements IIngredientSerializer<FluidContainerIngredient> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public FluidContainerIngredient parse(FriendlyByteBuf buffer) {
            Fluid f = BuiltInRegistries.FLUID.get(buffer.readResourceLocation());
            return new FluidContainerIngredient(f, buffer.readVarInt());
        }

        @Override
        public FluidContainerIngredient parse(JsonObject json) {
            Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.parse(GsonHelper.getAsString(json, "fluid")));
            return new FluidContainerIngredient(f, GsonHelper.getAsInt(json, "amount"));
        }

        @Override
        public void write(FriendlyByteBuf buffer, FluidContainerIngredient ingredient) {
            buffer.writeResourceLocation(BuiltInRegistries.FLUID.getKey(ingredient.fluid));
            buffer.writeVarInt(ingredient.amount);
        }
    }
}
//?}
//? if neoforge {
/*package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.hbm_m.inventory.FluidContainerRegistry;
import com.hbm_m.lib.RefStrings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// NeoForge-Fassung von hbm_m:fluid_container (gleiches JSON {"type": "hbm_m:fluid_container", "fluid": .., "amount": ..}).
// Zusaetzlich die Forge-Zutaten forge:nbt / forge:partial_nbt (LegacyNbtIngredients), damit die Datagen-JSONs unveraendert laden.
public class FluidContainerIngredient implements ICustomIngredient {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "fluid_container");

    public static final MapCodec<FluidContainerIngredient> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(f -> f.fluid),
            Codec.INT.fieldOf("amount").forGetter(f -> f.amount)
    ).apply(i, FluidContainerIngredient::new));

    public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, RefStrings.MODID);
    public static final java.util.function.Supplier<IngredientType<FluidContainerIngredient>> TYPE =
            INGREDIENT_TYPES.register("fluid_container", () -> new IngredientType<>(CODEC));

    private final Fluid fluid;
    private final int amount;
    private ItemStack[] display;

    public FluidContainerIngredient(Fluid fluid, int amount) {
        this.fluid = fluid;
        this.amount = amount;
    }

    public static net.minecraft.world.item.crafting.Ingredient of(Fluid fluid, int amount) {
        return new FluidContainerIngredient(fluid, amount).toVanilla();
    }

    public static void register(IEventBus modBus) {
        INGREDIENT_TYPES.register(modBus);
        LegacyNbtIngredients.register(modBus);
    }

    @Override
    public boolean test(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return FluidContainerRegistry.getFluidContent(stack, fluid) == amount;
    }

    @Override
    public Stream<ItemStack> getItems() {
        if (display == null) {
            List<ItemStack> list = new ArrayList<>();
            for (FluidContainerRegistry.FluidContainer c : FluidContainerRegistry.getContainers(fluid)) {
                if (c.content() == amount) list.add(c.fullContainer().copy());
            }
            display = list.toArray(new ItemStack[0]);
        }
        return Stream.of(display);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE.get();
    }
}
*///?}
