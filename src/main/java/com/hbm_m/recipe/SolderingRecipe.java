package com.hbm_m.recipe;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import dev.architectury.fluid.FluidStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Datapack-facing паяльный-рецепт ({@code hbm_m:soldering_station}).
 *
 * <p>Порт 1.7.10 статического {@code com.hbm.inventory.recipes.SolderingRecipes}: три группы
 * входов (toppings — 3 слота, pcb — 2 слота, solder — 1 слот), каждый элемент — {@link Ingredient}
 * + обязательный {@code count}; опциональное требование жидкости ({@link FluidType}, mB); один
 * предметный выход; {@code duration} (тики) и {@code consumption} (HE/тик).</p>
 *
 * <p><b>Замена статике:</b> прежний {@code SolderingRecipes} хранил {@code List<SolderingRecipe>}
 * в Java-статике, а {@code SolderingRecipes.toppings/pcb/solder} (Set из ingredient-тегов) использовались
 * для валидации слотов в {@code isItemValidForSlot}. Это блокировало data-driven рецепты. Теперь источник
 * правды — {@code RecipeManager}: {@code MachineSolderingStationBlockEntity} ищет рецепт через
 * {@link RecipeHooks#getAllRecipes(Level, RecipeType)}, а валидация слотов проверяет, входит ли предмет
 * хотя бы в один ingredient среди всех рецептов соответствующей группы.</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:soldering_station",
 *   "toppings": [ { "ingredient": { "item": "..." }, "count": 3 }, ... ],   // 0..3
 *   "pcb":      [ { "ingredient": { "item": "..." }, "count": 4 } ],         // 0..2
 *   "solder":   [ { "ingredient": { "tag": "forge:wires_fine/lead" }, "count": 4 } ],  // 0..1
 *   "fluid": { "fluid": "hbm_m:sulfuric_acid", "amount": 1000 },  // optional
 *   "result": { "item": "...", "count": 1 },
 *   "duration": 100,
 *   "consumption": 100
 * }
 * }</pre>
 */
public class SolderingRecipe extends PlatformRecipe {

    private final Ingredient[] toppings;
    private final int[] toppingCounts;
    private final Ingredient[] pcb;
    private final int[] pcbCounts;
    private final Ingredient[] solder;
    private final int[] solderCounts;
    @Nullable
    private final FluidStack fluid;
    private final ItemStack output;
    private final int duration;
    private final long consumption;

    public SolderingRecipe(ResourceLocation id,
                            Ingredient[] toppings, int[] toppingCounts,
                            Ingredient[] pcb, int[] pcbCounts,
                            Ingredient[] solder, int[] solderCounts,
                            @Nullable FluidStack fluid, ItemStack output, int duration, long consumption) {
        super(id);
        this.toppings = toppings; this.toppingCounts = toppingCounts;
        this.pcb = pcb; this.pcbCounts = pcbCounts;
        this.solder = solder; this.solderCounts = solderCounts;
        this.fluid = (fluid != null && !fluid.isEmpty() && fluid.getAmount() > 0) ? fluid : null;
        this.output = output;
        this.duration = Math.max(1, duration);
        this.consumption = Math.max(0, consumption);
    }

    public Ingredient[] getToppings() { return toppings; }
    public int[] getToppingCounts() { return toppingCounts; }
    public Ingredient[] getPcb() { return pcb; }
    public int[] getPcbCounts() { return pcbCounts; }
    public Ingredient[] getSolder() { return solder; }
    public int[] getSolderCounts() { return solderCounts; }
    @Nullable public FluidStack getFluid() { return fluid; }
    public ItemStack getOutput() { return output.copy(); }
    public int getDuration() { return duration; }
    public long getConsumption() { return consumption; }

    /** Мэтчинг по 6 слотам машины: toppings(0..2), pcb(3..4), solder(5). Порядок внутри группы не важен. */
    public boolean matches(ItemStack top0, ItemStack top1, ItemStack top2,
                            ItemStack pcb0, ItemStack pcb1, ItemStack solderStack) {
        return matchesGroup(new ItemStack[]{top0, top1, top2}, toppings, toppingCounts)
                && matchesGroup(new ItemStack[]{pcb0, pcb1}, pcb, pcbCounts)
                && matchesGroup(new ItemStack[]{solderStack}, solder, solderCounts);
    }

    private static boolean matchesGroup(ItemStack[] slots, Ingredient[] required, int[] counts) {
        boolean[] used = new boolean[required.length];
        for (ItemStack stack : slots) {
            if (stack == null || stack.isEmpty()) continue;
            for (int i = 0; i < required.length; i++) {
                if (used[i]) continue;
                if (required[i].test(stack) && stack.getCount() >= counts[i]) {
                    used[i] = true;
                    break;
                }
            }
        }
        for (boolean u : used) if (!u) return false;
        return true;
    }

    public boolean matchesFluid(FluidTank tank) {
        if (fluid == null) return true;
        if (tank == null) return false;
        FluidType stored = FluidType.forFluid(tank.getStoredFluid());
        FluidType required = FluidType.forFluid(fluid.getFluid());
        return stored == required && tank.getFill() >= (int) Math.min(Integer.MAX_VALUE, fluid.getAmount());
    }

    public void consumeFluid(FluidTank tank) {
        if (fluid != null && matchesFluid(tank)) {
            tank.setFill(tank.getFill() - (int) Math.min(Integer.MAX_VALUE, fluid.getAmount()));
        }
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        return !level.isClientSide()
                && matches(container.getItem(0), container.getItem(1), container.getItem(2),
                          container.getItem(3), container.getItem(4), container.getItem(5));
    }

    @Override
    public ItemStack assembleSafe() { return output.copy(); }

    @Override
    public ItemStack getResultItemSafe() { return output.copy(); }

    @Override
    public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    @Override
    public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<SolderingRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "soldering_station";
    }

    public static class Serializer extends PlatformRecipeSerializer<SolderingRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "soldering_station");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "soldering_station");
        //?}

        private static Ingredient[] readGroup(JsonObject json, String key, int[] countsOut) {
            if (!json.has(key)) return new Ingredient[0];
            JsonArray arr = GsonHelper.getAsJsonArray(json, key);
            Ingredient[] out = new Ingredient[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                JsonObject entry = arr.get(i).getAsJsonObject();
                out[i] = RecipeHooks.ingredientFromJson(entry.get("ingredient"));
                countsOut[i] = GsonHelper.getAsInt(entry, "count", 1);
            }
            return out;
        }

        @Override
        public SolderingRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            // Чтение трёх групп: toppings (до 3), pcb (до 2), solder (до 1).
            int tCap = json.has("toppings") ? GsonHelper.getAsJsonArray(json, "toppings").size() : 0;
            int pCap = json.has("pcb") ? GsonHelper.getAsJsonArray(json, "pcb").size() : 0;
            int sCap = json.has("solder") ? GsonHelper.getAsJsonArray(json, "solder").size() : 0;
            Ingredient[] toppings = new Ingredient[tCap]; int[] toppingCounts = new int[tCap];
            Ingredient[] pcb = new Ingredient[pCap]; int[] pcbCounts = new int[pCap];
            Ingredient[] solder = new Ingredient[sCap]; int[] solderCounts = new int[sCap];
            for (int i = 0; i < tCap; i++) {
                JsonObject e = GsonHelper.getAsJsonArray(json, "toppings").get(i).getAsJsonObject();
                toppings[i] = RecipeHooks.ingredientFromJson(e.get("ingredient"));
                toppingCounts[i] = GsonHelper.getAsInt(e, "count", 1);
            }
            for (int i = 0; i < pCap; i++) {
                JsonObject e = GsonHelper.getAsJsonArray(json, "pcb").get(i).getAsJsonObject();
                pcb[i] = RecipeHooks.ingredientFromJson(e.get("ingredient"));
                pcbCounts[i] = GsonHelper.getAsInt(e, "count", 1);
            }
            for (int i = 0; i < sCap; i++) {
                JsonObject e = GsonHelper.getAsJsonArray(json, "solder").get(i).getAsJsonObject();
                solder[i] = RecipeHooks.ingredientFromJson(e.get("ingredient"));
                solderCounts[i] = GsonHelper.getAsInt(e, "count", 1);
            }

            FluidStack fluid = null;
            if (json.has("fluid")) {
                JsonObject fluidObj = GsonHelper.getAsJsonObject(json, "fluid");
                ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(fluidObj, "fluid"));
                int amount = GsonHelper.getAsInt(fluidObj, "amount", 0);
                if (id != null && amount > 0) {
                    fluid = RecipeHooks.fluidStackOf(id, amount);
                }
            }

            ItemStack output = RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int duration = GsonHelper.getAsInt(json, "duration", 200);
            long consumption = GsonHelper.getAsLong(json, "consumption", 0L);
            return new SolderingRecipe(recipeId, toppings, toppingCounts, pcb, pcbCounts, solder, solderCounts,
                                        fluid, output, duration, consumption);
        }

        private static void writeGroup(FriendlyByteBuf buf, Ingredient[] group, int[] counts) {
            buf.writeVarInt(group.length);
            for (int i = 0; i < group.length; i++) {
                RecipeHooks.writeIngredient(buf, group[i]);
                buf.writeVarInt(counts[i]);
            }
        }

        private static Ingredient[] readGroup(FriendlyByteBuf buf, int[] countsOut) {
            int n = buf.readVarInt();
            Ingredient[] out = new Ingredient[n];
            for (int i = 0; i < n; i++) {
                out[i] = RecipeHooks.readIngredient(buf);
                countsOut[i] = buf.readVarInt();
            }
            return out;
        }

        @Override
        public SolderingRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            int tCap = buf.readVarInt(); int[] toppingCounts = new int[tCap];
            Ingredient[] toppings = new Ingredient[tCap];
            for (int i = 0; i < tCap; i++) { toppings[i] = RecipeHooks.readIngredient(buf); toppingCounts[i] = buf.readVarInt(); }
            int pCap = buf.readVarInt(); int[] pcbCounts = new int[pCap];
            Ingredient[] pcb = new Ingredient[pCap];
            for (int i = 0; i < pCap; i++) { pcb[i] = RecipeHooks.readIngredient(buf); pcbCounts[i] = buf.readVarInt(); }
            int sCap = buf.readVarInt(); int[] solderCounts = new int[sCap];
            Ingredient[] solder = new Ingredient[sCap];
            for (int i = 0; i < sCap; i++) { solder[i] = RecipeHooks.readIngredient(buf); solderCounts[i] = buf.readVarInt(); }
            FluidStack fluid = RecipeHooks.readFluidStack(buf);
            ItemStack output = RecipeHooks.readItem(buf);
            int duration = buf.readVarInt();
            long consumption = buf.readVarLong();
            return new SolderingRecipe(recipeId, toppings, toppingCounts, pcb, pcbCounts, solder, solderCounts,
                                        fluid, output, duration, consumption);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, SolderingRecipe recipe) {
            writeGroup(buf, recipe.toppings, recipe.toppingCounts);
            writeGroup(buf, recipe.pcb, recipe.pcbCounts);
            writeGroup(buf, recipe.solder, recipe.solderCounts);
            RecipeHooks.writeFluidStack(buf, recipe.fluid != null ? recipe.fluid : FluidStack.empty());
            RecipeHooks.writeItem(buf, recipe.output);
            buf.writeVarInt(recipe.duration);
            buf.writeVarLong(recipe.consumption);
        }
    }
}
