package com.hbm_m.datagen.recipes.custom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.block.machines.anvils.AnvilTier;
import com.hbm_m.block.network.BoxDuctBlock;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.recipe.AnvilRecipe;
import com.hbm_m.recipe.AnvilRecipe.AStack;
import com.hbm_m.recipe.AnvilRecipe.OverlayType;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.crafting.PartialNBTIngredient;
import com.hbm_m.platform.StackNbt;

/**
 * Datagen fuer {@link AnvilRecipe}: {@link #construction} = {@code AnvilConstructionRecipe},
 * {@link #smithing} = {@code AnvilSmithingRecipe} (samt Hot/Mold/Cyanide/Rename).
 * Die Stufe wird als Original-Zahl angegeben ({@code setTier(n)}).
 */
public class AnvilRecipeBuilder extends BaseRecipeBuilder<AnvilRecipeBuilder> {

    private final AnvilRecipe.Kind kind;
    private int tier;
    private int sort;
    private int tierUpper = -1;
    private OverlayType overlay = OverlayType.NONE;
    private final List<AStack> inputs = new ArrayList<>();
    private final List<ItemStack> outputs = new ArrayList<>();
    private final List<Float> chances = new ArrayList<>();
    private AStack left;
    private AStack right;
    private boolean keepLeft = false;
    private boolean keepRight = false;
    private boolean hot = false;
    private String special = "";
    private String moldShape = "";
    private final List<Item> moldItems = new ArrayList<>();

    private AnvilRecipeBuilder(AnvilRecipe.Kind kind, int tier, int sort) {
        this.kind = kind;
        this.tier = tier;
        this.sort = sort;
    }

    /** {@code new AnvilConstructionRecipe(...).setTier(tier)}; {@code overlay} wie vom Original-Konstruktor bzw. setOverlay. */
    public static AnvilRecipeBuilder construction(int tier, int sort, OverlayType overlay) {
        AnvilRecipeBuilder b = new AnvilRecipeBuilder(AnvilRecipe.Kind.CONSTRUCTION, tier, sort);
        b.overlay = overlay;
        return b;
    }

    /** {@code new AnvilSmithingRecipe(tier, out, left, right)} */
    public static AnvilRecipeBuilder smithing(int tier, int sort, ItemStack output, Ingredient left, int leftCount, Ingredient right, int rightCount) {
        AnvilRecipeBuilder b = new AnvilRecipeBuilder(AnvilRecipe.Kind.SMITHING, tier, sort);
        b.left = new AStack(left, leftCount, false);
        b.right = new AStack(right, rightCount, false);
        b.outputs.add(output.copy());
        b.chances.add(1.0F);
        b.overlay = OverlayType.SMITHING;
        return b;
    }

    public AnvilRecipeBuilder input(Ingredient ingredient, int count) {
        return input(ingredient, count, false);
    }

    /** {@code comparable}: {@code ComparableStack} statt {@code OreDictStack} (wichtig fuer die Recycling-Anzeige). */
    public AnvilRecipeBuilder input(Ingredient ingredient, int count, boolean comparable) {
        inputs.add(new AStack(ingredient, count, comparable));
        return this;
    }

    public AnvilRecipeBuilder output(ItemStack stack) {
        return output(stack, 1.0F);
    }

    public AnvilRecipeBuilder output(ItemStack stack, float chance) {
        outputs.add(stack.copy());
        chances.add(chance);
        return this;
    }

    public AnvilRecipeBuilder tierUpper(int upper) {
        this.tierUpper = upper;
        return this;
    }

    /** {@code AnvilSmithingHotRecipe} */
    public AnvilRecipeBuilder hot() {
        this.hot = true;
        return this;
    }

    public AnvilRecipeBuilder keepLeft() {
        this.keepLeft = true;
        return this;
    }

    public AnvilRecipeBuilder keepRight() {
        this.keepRight = true;
        return this;
    }

    /** {@code "cyanide"} / {@code "rename"} */
    public AnvilRecipeBuilder special(String special) {
        this.special = special;
        return this;
    }

    /** {@code AnvilSmithingMold} mit {@code OreDictStack(prefix, count)}: die linke Menge muss genau stimmen. */
    public AnvilRecipeBuilder moldShape(String prefix, int count) {
        this.moldShape = prefix;
        this.left = new AStack(left.ingredient(), count, false);
        return this;
    }

    /** {@code AnvilSmithingMold} mit {@code ItemStack[]} */
    public AnvilRecipeBuilder moldItems(Item... items) {
        for (Item i : items) moldItems.add(i);
        return this;
    }

    /** Kastenrohr/Kabelkasten mit Original-Meta als Zutat (Meta im {@code BlockStateTag}). */
    public static Ingredient box(RegistrySupplier<Block> block, int meta) {
        ItemStack s = BoxDuctBlock.stack(block.get().asItem(), meta);
        return PartialNBTIngredient.of(s.getItem(), s.getTag());
    }

    /** Stirling mit Original-Meta als Zutat: 0 = mit Zahnrad (ohne {@code no_cog}), 1 = ohne Zahnrad. */
    public static Ingredient stirling(RegistrySupplier<Block> block, int meta) {
        ItemStack s = com.hbm_m.block.machines.MachineStirlingBlock.noCogStack(block.get());
        Ingredient noCog = PartialNBTIngredient.of(s.getItem(), s.getTag());
        if (meta == 1) return noCog;
        return net.minecraftforge.common.crafting.DifferenceIngredient.of(Ingredient.of(s.getItem()), noCog);
    }

    public static ItemStack boxStack(RegistrySupplier<Block> block, int meta, int count) {
        ItemStack s = BoxDuctBlock.stack(block.get().asItem(), meta);
        s.setCount(count);
        return s;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Port-Fassade für AnvilRecipeGenerator (machines-Skelett): gestattet die
    //  ursprüngliche Kettenschreibweise (inventoryRecipe/anvilRecipe + withOrder/
    //  withOverlay + addInventoryRequirement/addOutput) auf diesem Builder.
    //  Reine Zusatzmethoden - der FFA-Satz (construction/smithing/input/output)
    //  bleibt unverändert und wird von AnvilConstructionRecipeGenerator genutzt.
    // ═══════════════════════════════════════════════════════════════════════

    /** Construction-Rezept mit einer Ausgabe; Stufe als {@link AnvilTier}, Sort/Overlay per Kette. */
    public static AnvilRecipeBuilder inventoryRecipe(ItemStack output, AnvilTier tier) {
        AnvilRecipeBuilder b = new AnvilRecipeBuilder(AnvilRecipe.Kind.CONSTRUCTION, tier.getLegacyId(), 0);
        b.outputs.add(output.copy());
        b.chances.add(1.0F);
        return b;
    }

    /** Smithing-Rezept mit {@link AStack}-Zutaten (linke/rechte Maschinen-Slots); Overlay SMITHING wie im Original-Konstruktor. */
    public static AnvilRecipeBuilder anvilRecipe(AStack left, AStack right, ItemStack output, AnvilTier tier) {
        AnvilRecipeBuilder b = new AnvilRecipeBuilder(AnvilRecipe.Kind.SMITHING, tier.getLegacyId(), 0);
        b.left = left;
        b.right = right;
        b.outputs.add(output.copy());
        b.chances.add(1.0F);
        b.overlay = OverlayType.SMITHING;
        return b;
    }

    /** Registrierungsreihenfolge des Originals (GUI-Liste). */
    public AnvilRecipeBuilder withOrder(int order) {
        this.sort = order;
        return this;
    }

    public AnvilRecipeBuilder withOverlay(OverlayType overlay) {
        this.overlay = overlay;
        return this;
    }

    /** Zutat aus dem Spielerinventar als fertiger {@link AStack}. */
    public AnvilRecipeBuilder addInventoryRequirement(AStack stack) {
        this.inputs.add(stack);
        return this;
    }

    /** Zutat aus dem Spielerinventar als exakter Gegenstand mit der Stack-Menge. */
    public AnvilRecipeBuilder addInventoryRequirement(ItemStack stack) {
        this.inputs.add(new AStack(Ingredient.of(stack), Math.max(1, stack.getCount()), true));
        return this;
    }

    public AnvilRecipeBuilder addOutput(ItemStack stack) {
        return output(stack);
    }

    public AnvilRecipeBuilder addOutput(ItemStack stack, float chance) {
        return output(stack, chance);
    }

    /** Für Disassemble-Rezepten: vorgesetzte (leere) Ausgabe verwerfen. */
    public AnvilRecipeBuilder clearOutputs() {
        this.outputs.clear();
        this.chances.clear();
        return this;
    }

    /** Maschinen-Schreibweise für {@link #keepLeft()}: inputA (linker Slot) wird nicht verbraucht. */
    public AnvilRecipeBuilder keepInputA() {
        return keepLeft();
    }

    @Override
    public Item getResult() {
        return outputs.get(0).getItem();
    }

    @Override
    protected JsonObject stackToJson(ItemStack stack) {
        JsonObject obj = new JsonObject();
        obj.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (stack.getCount() > 1) obj.addProperty("count", stack.getCount());
        if (StackNbt.has(stack)) obj.addProperty("nbt", StackNbt.read(stack).toString());
        return obj;
    }

    private static String tierName(int legacy) {
        AnvilTier t = AnvilTier.fromLegacyId(legacy);
        if (t.getLegacyId() != legacy) throw new IllegalStateException("Amboss-Stufe ohne Entsprechung: " + legacy);
        return t.name().toLowerCase(Locale.ROOT);
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.addProperty("kind", kind == AnvilRecipe.Kind.SMITHING ? "smithing" : "construction");
        json.addProperty("sort", sort);
        if (left != null) json.add("left", AnvilRecipe.aStackToJson(left));
        if (right != null) json.add("right", AnvilRecipe.aStackToJson(right));
        if (keepLeft) json.addProperty("consume_left", false);
        if (keepRight) json.addProperty("consume_right", false);
        if (hot) json.addProperty("hot", true);
        if (!special.isEmpty()) json.addProperty("special", special);
        if (!moldShape.isEmpty()) json.addProperty("mold_shape", moldShape);
        if (!moldItems.isEmpty()) {
            JsonArray arr = new JsonArray();
            for (Item i : moldItems) arr.add(BuiltInRegistries.ITEM.getKey(i).toString());
            json.add("mold_items", arr);
        }
        if (!inputs.isEmpty()) {
            JsonArray arr = new JsonArray();
            for (AStack s : inputs) arr.add(AnvilRecipe.aStackToJson(s));
            json.add("inputs", arr);
        }
        if (outputs.isEmpty()) throw new IllegalStateException("Anvil recipe has no outputs");
        JsonArray outs = new JsonArray();
        for (int i = 0; i < outputs.size(); i++) {
            JsonObject o = stackToJson(outputs.get(i));
            if (chances.get(i) < 1.0F) o.addProperty("chance", chances.get(i));
            outs.add(o);
        }
        json.add("outputs", outs);
        json.addProperty("tier", tierName(tier));
        if (tierUpper != -1) json.addProperty("tier_upper", tierName(tierUpper));
        json.addProperty("overlay", overlay.name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return AnvilRecipe.Serializer.INSTANCE;
    }
}
