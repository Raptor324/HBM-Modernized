package com.hbm_m.recipe;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.material.MaterialType;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Тигель-сплавление: объединяет несколько расплавленных материалов из пула тигля
 * в один или несколько иных расплавленных материалов, потребляя энергию и время
 * ({@code frequency}). Порт 1.7.10 {@code CrucibleRecipe} (alloying-половина).
 *
 * <p>Data-driven ({@code hbm_m:molten_alloy}). Источник правды — {@code RecipeManager}
 * (JSON), поиск через {@code RecipeHooks.getAllRecipes(level, MoltenAlloyRecipe.Type.INSTANCE)}.
 * Прежняя Java-статика {@code MoltenAlloyRecipes} удалена.</p>
 *
 * <p>JSON-формат (читается {@link Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:molten_alloy",
 *   "frequency": 20,                                  // тиков между попытками (legacy)
 *   "inputs":  [ { "material": "iron",   "amount": 288 }, ... ],
 *   "outputs": [ { "material": "steel",  "amount": 288 }, ... ]
 * }
 * }</pre>
 *
 * <p>{@link MaterialType} разрешается через {@link MaterialType#byName(String)} (строковый
 * {@code name}, без namespace). {@code amount} — в mB.</p>
 */
public class MoltenAlloyRecipe extends PlatformRecipe {

    private final MaterialStack[] inputs;
    private final MaterialStack[] outputs;
    private final int frequency;

    public MoltenAlloyRecipe(ResourceLocation id, MaterialStack[] inputs, MaterialStack[] outputs, int frequency) {
        super(id);
        this.inputs = inputs;
        this.outputs = outputs;
        this.frequency = frequency;
    }

    public MaterialStack[] getInputs()  { return inputs; }
    public MaterialStack[] getOutputs() { return outputs; }
    public int getFrequency()           { return frequency; }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        // Молтен-сплавление не имеет предметного входа — тигель сам управляет пулом.
        return !level.isClientSide();
    }

    @Override
    public ItemStack assembleSafe() { return ItemStack.EMPTY; }

    @Override
    public ItemStack getResultItemSafe() { return ItemStack.EMPTY; }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements RecipeType<MoltenAlloyRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "molten_alloy";
    }

    public static class Serializer extends PlatformRecipeSerializer<MoltenAlloyRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "molten_alloy");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "molten_alloy");
        //?}

        @Override
        public MoltenAlloyRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            int frequency = GsonHelper.getAsInt(json, "frequency", 20);
            MaterialStack[] inputs  = readMaterialArray(json, "inputs");
            MaterialStack[] outputs = readMaterialArray(json, "outputs");
            return new MoltenAlloyRecipe(recipeId, inputs, outputs, frequency);
        }

        private static MaterialStack[] readMaterialArray(JsonObject json, String key) {
            if (!json.has(key)) return new MaterialStack[0];
            JsonArray arr = GsonHelper.getAsJsonArray(json, key);
            MaterialStack[] out = new MaterialStack[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                JsonObject el = arr.get(i).getAsJsonObject();
                String matName = GsonHelper.getAsString(el, "material");
                MaterialType mat = MaterialType.byName(matName);
                if (mat == null) {
                    throw new IllegalStateException("Unknown material '" + matName
                            + "' in molten_alloy recipe " + key);
                }
                int amount = GsonHelper.getAsInt(el, "amount", MaterialStack.MB_PER_INGOT);
                out[i] = new MaterialStack(mat, amount);
            }
            return out;
        }

        @Override
        public MoltenAlloyRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            int frequency = buf.readVarInt();
            MaterialStack[] inputs  = readMaterialArrayNetwork(buf);
            MaterialStack[] outputs = readMaterialArrayNetwork(buf);
            return new MoltenAlloyRecipe(recipeId, inputs, outputs, frequency);
        }

        private static MaterialStack[] readMaterialArrayNetwork(FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            MaterialStack[] arr = new MaterialStack[n];
            for (int i = 0; i < n; i++) {
                String name = buf.readUtf();
                int amount = buf.readVarInt();
                MaterialType mat = MaterialType.byName(name);
                arr[i] = new MaterialStack(mat != null ? mat : MaterialType.IRON, amount);
            }
            return arr;
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, MoltenAlloyRecipe recipe) {
            buf.writeVarInt(recipe.frequency);
            writeMaterialArrayNetwork(buf, recipe.inputs);
            writeMaterialArrayNetwork(buf, recipe.outputs);
        }

        private static void writeMaterialArrayNetwork(FriendlyByteBuf buf, MaterialStack[] arr) {
            buf.writeVarInt(arr.length);
            for (MaterialStack ms : arr) {
                buf.writeUtf(ms.type != null ? ms.type.name : "iron");
                buf.writeVarInt(ms.amount);
            }
        }
    }
}
