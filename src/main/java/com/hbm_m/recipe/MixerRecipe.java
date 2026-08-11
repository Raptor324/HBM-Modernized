package com.hbm_m.recipe;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;

/**
 * Datapack-facing миксер-рецепт ({@code hbm_m:mixer}).
 *
 * <p>Порт 1.7.10 {@code com.hbm.inventory.recipes.MixerRecipes}: миксер объединяет два жидкостных
 * входа в один жидкостный выход, потребляя энергию. Два {@link #inputA} и {@link #inputB} задаются
 * как Architectury {@link dev.architectury.fluid.FluidStack} (с количеством в mB); порядок
 * {@link #matches} симметричный — любой бак может подойти к любой роли (trueTankA используется машиной
 * для выбора, какой бак слить).</p>
 *
 * <p><b>Замена статике:</b> преждний {@code MixerRecipes} хранил {@code List<MixerRecipe>} в Java-статике
 * через {@code static {}}, что блокировало data-driven рецепты. Теперь источник правды —
 * {@code RecipeManager} (JSON), и поиск ведётся через {@code MixerRecipes.findRecipe(level, tankA, tankB)}.</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:mixer",
 *   "input_a": { "fluid": "hbm_m:vitriol", "amount": 1000 },
 *   "input_b": { "fluid": "minecraft:water", "amount": 1000 },
 *   "output":   { "fluid": "hbm_m:sulfuric_acid", "amount": 2000 },
 *   "duration": 100,
 *   "energy_per_tick": 50
 * }
 * }</pre>
 *
 * <p>Отдельный тип рецепта (а не использование {@code CombinationOvenRecipe}/{@code CrystallizerRecipe})
 * нужен, т.к. миксер принципиально «жидкость + жидкость» (без предметных входов) и симметричный.</p>
 */
public class MixerRecipe extends com.hbm_m.platform.recipe.PlatformRecipe {

    private final dev.architectury.fluid.FluidStack inputA;
    private final dev.architectury.fluid.FluidStack inputB;
    private final dev.architectury.fluid.FluidStack output;
    private final int duration;
    private final long energyPerTick;

    public MixerRecipe(net.minecraft.resources.ResourceLocation id,
                       dev.architectury.fluid.FluidStack inputA, dev.architectury.fluid.FluidStack inputB,
                       dev.architectury.fluid.FluidStack output,
                       int duration, long energyPerTick) {
        super(id);
        this.inputA = inputA != null ? inputA : dev.architectury.fluid.FluidStack.empty();
        this.inputB = inputB != null ? inputB : dev.architectury.fluid.FluidStack.empty();
        this.output = output != null ? output : dev.architectury.fluid.FluidStack.empty();
        this.duration = Math.max(1, duration);
        this.energyPerTick = Math.max(0, energyPerTick);
    }

    public dev.architectury.fluid.FluidStack getInputA() { return inputA; }
    public dev.architectury.fluid.FluidStack getInputB() { return inputB; }
    public dev.architectury.fluid.FluidStack getOutput() { return output; }
    public int getDuration() { return duration; }
    public long getEnergyPerTick() { return energyPerTick; }

    /** Симметричный match: порядок баков не важен. */
    public boolean matches(net.minecraft.world.level.material.Fluid tankA, net.minecraft.world.level.material.Fluid tankB) {
        if (tankA == null || tankB == null) return false;
        boolean direct = sameSubstance(tankA, inputA.getFluid()) && sameSubstance(tankB, inputB.getFluid());
        boolean swapped = sameSubstance(tankA, inputB.getFluid()) && sameSubstance(tankB, inputA.getFluid());
        return direct || swapped;
    }

    /** true, если {@code tankA} соответствует {@link #inputA} (т.е. баки НЕ переставлены). */
    public boolean isDirectOrder(net.minecraft.world.level.material.Fluid tankA) {
        return tankA != null && sameSubstance(tankA, inputA.getFluid());
    }

    private static boolean sameSubstance(net.minecraft.world.level.material.Fluid a,
                                          net.minecraft.world.level.material.Fluid b) {
        if (a == b) return true;
        return com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(a, b);
    }

    @Override
    public boolean matchesRecipe(com.hbm_m.platform.recipe.RecipeInputWrapper container, net.minecraft.world.level.Level level) {
        // У миксера нет предметных входов — стандартный мэтчинг неприменим; машина использует #matches напрямую.
        return false;
    }

    @Override
    public ItemStack assembleSafe() { return ItemStack.EMPTY; }

    @Override
    public ItemStack getResultItemSafe() { return ItemStack.EMPTY; }

    @Override
    public net.minecraft.world.item.crafting.RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public net.minecraft.world.item.crafting.RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements net.minecraft.world.item.crafting.RecipeType<MixerRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "mixer";
    }

    public static class Serializer extends com.hbm_m.platform.recipe.PlatformRecipeSerializer<MixerRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final net.minecraft.resources.ResourceLocation ID = new net.minecraft.resources.ResourceLocation(com.hbm_m.lib.RefStrings.MODID, "mixer");
        *///?} else {
        public static final net.minecraft.resources.ResourceLocation ID = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, "mixer");
        //?}

        @Override
        public MixerRecipe readJson(net.minecraft.resources.ResourceLocation recipeId, com.google.gson.JsonObject json) {
            dev.architectury.fluid.FluidStack inputA = readFluid(json, "input_a");
            dev.architectury.fluid.FluidStack inputB = readFluid(json, "input_b");
            dev.architectury.fluid.FluidStack output = readFluid(json, "output");
            int duration = net.minecraft.util.GsonHelper.getAsInt(json, "duration", 100);
            long energy = net.minecraft.util.GsonHelper.getAsLong(json, "energy_per_tick", 0L);
            return new MixerRecipe(recipeId, inputA, inputB, output, duration, energy);
        }

        @Override
        public MixerRecipe readNetwork(net.minecraft.resources.ResourceLocation recipeId, net.minecraft.network.FriendlyByteBuf buf) {
            dev.architectury.fluid.FluidStack inputA = com.hbm_m.platform.recipe.RecipeHooks.readFluidStack(buf);
            dev.architectury.fluid.FluidStack inputB = com.hbm_m.platform.recipe.RecipeHooks.readFluidStack(buf);
            dev.architectury.fluid.FluidStack output = com.hbm_m.platform.recipe.RecipeHooks.readFluidStack(buf);
            int duration = buf.readVarInt();
            long energy = buf.readVarLong();
            return new MixerRecipe(recipeId, inputA, inputB, output, duration, energy);
        }

        @Override
        public void writeNetwork(net.minecraft.network.FriendlyByteBuf buf, MixerRecipe recipe) {
            com.hbm_m.platform.recipe.RecipeHooks.writeFluidStack(buf, recipe.inputA);
            com.hbm_m.platform.recipe.RecipeHooks.writeFluidStack(buf, recipe.inputB);
            com.hbm_m.platform.recipe.RecipeHooks.writeFluidStack(buf, recipe.output);
            buf.writeVarInt(recipe.duration);
            buf.writeVarLong(recipe.energyPerTick);
        }

        private static dev.architectury.fluid.FluidStack readFluid(com.google.gson.JsonObject json, String key) {
            if (!json.has(key)) return dev.architectury.fluid.FluidStack.empty();
            com.google.gson.JsonObject obj = net.minecraft.util.GsonHelper.getAsJsonObject(json, key);
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(
                    net.minecraft.util.GsonHelper.getAsString(obj, "fluid"));
            long amount = net.minecraft.util.GsonHelper.getAsLong(obj, "amount", 0L);
            if (id == null) return dev.architectury.fluid.FluidStack.empty();
            return com.hbm_m.platform.recipe.RecipeHooks.fluidStackOf(id, amount);
        }
    }
}
