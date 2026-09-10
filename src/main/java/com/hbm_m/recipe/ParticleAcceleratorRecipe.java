package com.hbm_m.recipe;

import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonObject;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Rezept des Teilchenbeschleunigers ({@code hbm_m:particle_accelerator}).
 *
 * <p>Port der Statik {@code com.hbm.inventory.recipes.ParticleAcceleratorRecipes} (1.7.10): zwei
 * Ausgangsteilchen treffen mit mindestens {@link #getMomentum()} Impuls aufeinander und ergeben ein
 * oder zwei Produkte. Wie im Original ist die Reihenfolge der Eingaben egal - der Detektor prueft
 * beide Zuordnungen.</p>
 *
 * <p>JSON-Format:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:particle_accelerator",
 *   "input_a":  { "item": "hbm_m:particle_hydrogen" },
 *   "input_b":  { "item": "hbm_m:particle_copper" },
 *   "momentum": 300,
 *   "result":   { "item": "hbm_m:particle_amat" },
 *   "result_b": { "item": "hbm_m:nugget" }        // freiwillig
 * }
 * }</pre>
 */
public class ParticleAcceleratorRecipe extends PlatformRecipe {

    private final Ingredient inputA;
    private final Ingredient inputB;
    private final int momentum;
    private final ItemStack outputA;
    private final ItemStack outputB;

    public ParticleAcceleratorRecipe(ResourceLocation id, Ingredient inputA, Ingredient inputB,
                                     int momentum, ItemStack outputA, ItemStack outputB) {
        super(id);
        this.inputA = inputA;
        this.inputB = inputB;
        this.momentum = momentum;
        this.outputA = outputA;
        this.outputB = outputB;
    }

    public Ingredient getInputA() { return inputA; }
    public Ingredient getInputB() { return inputB; }
    /** Mindestimpuls; darunter meldet der Detektor {@code CRASH_UNDERSPEED}. */
    public int getMomentum()      { return momentum; }
    public ItemStack getOutputA() { return outputA; }
    public ItemStack getOutputB() { return outputB; }

    public boolean hasOutputB() {
        return outputB != null && !outputB.isEmpty();
    }

    /**
     * 1:1 aus {@code ParticleAcceleratorRecipe.matchesRecipe}: die beiden Eingaben duerfen in
     * beliebiger Reihenfolge kommen.
     */
    public boolean matches(ItemStack in1, ItemStack in2) {
        return (inputA.test(in1) && inputB.test(in2))
                || (inputA.test(in2) && inputB.test(in1));
    }

    /** Sucht das passende Rezept zu den beiden mitgereisten Stoffen. */
    @Nullable
    public static ParticleAcceleratorRecipe find(Level level, ItemStack in1, ItemStack in2) {
        if (level == null) return null;
        for (ParticleAcceleratorRecipe recipe : RecipeHooks.getAllRecipes(level, Type.INSTANCE)) {
            if (recipe.matches(in1, in2)) return recipe;
        }
        return null;
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        // Der Detektor prueft selbst ueber find(...) - kein Containerabgleich.
        return false;
    }

    @Override
    public ItemStack assembleSafe() { return outputA.copy(); }

    @Override
    public ItemStack getResultItemSafe() { return outputA; }

    @Override
    public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    @Override
    public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<ParticleAcceleratorRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "particle_accelerator";
    }

    public static class Serializer extends PlatformRecipeSerializer<ParticleAcceleratorRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "particle_accelerator");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "particle_accelerator");
        //?}

        @Override
        public ParticleAcceleratorRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient inputA = RecipeHooks.ingredientFromJson(json.get("input_a"));
            Ingredient inputB = RecipeHooks.ingredientFromJson(json.get("input_b"));
            int momentum = GsonHelper.getAsInt(json, "momentum", 0);
            ItemStack outputA = RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            ItemStack outputB = json.has("result_b")
                    ? RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result_b"))
                    : ItemStack.EMPTY;
            return new ParticleAcceleratorRecipe(recipeId, inputA, inputB, momentum, outputA, outputB);
        }

        @Override
        public ParticleAcceleratorRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            Ingredient inputA = RecipeHooks.readIngredient(buf);
            Ingredient inputB = RecipeHooks.readIngredient(buf);
            int momentum = buf.readVarInt();
            ItemStack outputA = RecipeHooks.readItem(buf);
            ItemStack outputB = RecipeHooks.readItem(buf);
            return new ParticleAcceleratorRecipe(recipeId, inputA, inputB, momentum, outputA, outputB);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, ParticleAcceleratorRecipe recipe) {
            RecipeHooks.writeIngredient(buf, recipe.inputA);
            RecipeHooks.writeIngredient(buf, recipe.inputB);
            buf.writeVarInt(recipe.momentum);
            RecipeHooks.writeItem(buf, recipe.outputA);
            RecipeHooks.writeItem(buf, recipe.outputB == null ? ItemStack.EMPTY : recipe.outputB);
        }
    }
}
