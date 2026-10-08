package com.hbm_m.recipe;

import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm_m.block.machines.anvils.AnvilTier;
import com.hbm_m.inventory.material.Mats;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code AnvilRecipes}: ein Rezepttyp fuer beide Original-Listen.
 * <ul>
 *   <li>{@link Kind#SMITHING} = {@code AnvilSmithingRecipe} (zwei Slots links/rechts, Ergebnis im Ausgabeslot),
 *       samt Sonderformen {@code AnvilSmithingHotRecipe} ({@code hot}), {@code AnvilSmithingMold}
 *       ({@code mold_shape}/{@code mold_items}), {@code AnvilSmithingCyanideRecipe} und
 *       {@code AnvilSmithingRenameRecipe} ({@code special}).</li>
 *   <li>{@link Kind#CONSTRUCTION} = {@code AnvilConstructionRecipe} (Zutaten aus dem Spielerinventar,
 *       Ausgaben mit Chance, Stufenbereich, Overlay).</li>
 * </ul>
 * {@code sort} haelt die Registrierungsreihenfolge des Originals (GUI-Liste, erste passende Schmiede-Regel).
 */
public class AnvilRecipe extends PlatformRecipe {

    public enum Kind { SMITHING, CONSTRUCTION }

    /** {@code AStack}: Zutat mit Menge. {@code comparable} = {@code ComparableStack} (kein OreDict-Schluessel). */
    public record AStack(Ingredient ingredient, int count, boolean comparable) {

        /** {@code matchesRecipe(stack, true)}: nur die Sorte, nicht die Menge. */
        public boolean matchesIgnoreSize(ItemStack stack) {
            return !stack.isEmpty() && ingredient.test(stack);
        }

        /** {@code matchesRecipe(stack, false)}: Sorte und mindestens die geforderte Menge. */
        public boolean matches(ItemStack stack) {
            return matchesIgnoreSize(stack) && stack.getCount() >= count;
        }

        /** {@code extractForNEI()} */
        public List<ItemStack> displayStacks() {
            List<ItemStack> list = new ArrayList<>();
            for (ItemStack s : ingredient.getItems()) {
                ItemStack c = s.copy();
                c.setCount(Math.max(1, count));
                list.add(c);
            }
            return list;
        }

        /** {@code ComparableStack.toStack()} */
        public ItemStack firstStack() {
            List<ItemStack> l = displayStacks();
            return l.isEmpty() ? ItemStack.EMPTY : l.get(0);
        }
    }

    /** {@code AnvilOutput} */
    public record ResultEntry(ItemStack stack, float chance) { }

    public enum OverlayType {
        NONE,
        CONSTRUCTION,
        RECYCLING,
        SMITHING;

        private static OverlayType byName(String name) {
            for (OverlayType type : values()) {
                if (type.name().equalsIgnoreCase(name)) return type;
            }
            return NONE;
        }
    }

    private final Kind kind;
    private final int sort;
    // Schmieden
    @Nullable private final AStack left;
    @Nullable private final AStack right;
    private final boolean consumeLeft;
    private final boolean consumeRight;
    private final boolean hot;
    private final String special;
    private final String moldShape;
    private final List<Item> moldItems;
    // Konstruktion
    private final List<AStack> inputs;
    private final List<ResultEntry> outputs;
    private final AnvilTier requiredTier;
    @Nullable private final AnvilTier upperTier;
    private final OverlayType overlay;

    public AnvilRecipe(ResourceLocation id, Kind kind, int sort, @Nullable AStack left, @Nullable AStack right,
                       boolean consumeLeft, boolean consumeRight, boolean hot, String special, String moldShape, List<Item> moldItems,
                       List<AStack> inputs, List<ResultEntry> outputs, AnvilTier requiredTier, @Nullable AnvilTier upperTier,
                       OverlayType overlay) {
        super(id);
        if (outputs.isEmpty()) throw new IllegalArgumentException("Anvil recipe " + id + " must define at least one output");
        this.kind = kind;
        this.sort = sort;
        this.left = left;
        this.right = right;
        this.consumeLeft = consumeLeft;
        this.consumeRight = consumeRight;
        this.hot = hot;
        this.special = special == null ? "" : special;
        this.moldShape = moldShape == null ? "" : moldShape;
        this.moldItems = List.copyOf(moldItems);
        this.inputs = Collections.unmodifiableList(new ArrayList<>(inputs));
        this.outputs = Collections.unmodifiableList(new ArrayList<>(outputs));
        this.requiredTier = requiredTier;
        this.upperTier = upperTier;
        this.overlay = overlay;
    }

    public Kind getKind() { return kind; }
    public boolean isSmithing() { return kind == Kind.SMITHING; }
    public boolean isConstruction() { return kind == Kind.CONSTRUCTION; }
    public int getSort() { return sort; }
    @Nullable public AStack getLeft() { return left; }
    @Nullable public AStack getRight() { return right; }
    public boolean isHot() { return hot; }
    public String getSpecial() { return special; }
    public List<AStack> getInputs() { return inputs; }
    public List<ResultEntry> getOutputs() { return outputs; }
    public AnvilTier getRequiredTier() { return requiredTier; }
    @Nullable public AnvilTier getUpperTier() { return upperTier; }
    public OverlayType getOverlay() { return overlay; }

    // ═══════════════════════════ Schmieden (AnvilSmithingRecipe) ═══════════════════════════

    /** {@code AnvilSmithingRecipe.matches} samt Unterklassen; nicht spiegelbildlich ({@code shapeless} ist nirgends gesetzt). */
    public boolean matchesSmithing(ItemStack l, ItemStack r) {
        if (!isSmithing() || left == null || right == null) return false;
        switch (special) {
            case "cyanide":
                // AnvilSmithingCyanideRecipe: rechts Plan C oder rote Pille, links beliebige Nahrung
                return (doesStackMatch(r, right) || r.getItem() == itemById("pill_red")) && l.isEdible();
            case "rename":
                // AnvilSmithingRenameRecipe: links beliebig, rechts ein benanntes Namensschild
                return doesStackMatch(r, right) && r.hasCustomHoverName();
            default:
                break;
        }
        if (!moldShape.isEmpty() || !moldItems.isEmpty()) {
            // AnvilSmithingMold: rechts die Formvorlage, links genau die Menge der passenden Form
            if (!doesStackMatch(r, right)) return false;
            if (!moldShape.isEmpty() && l.getCount() == left.count()) {
                Mats.MatShape ms = Mats.getShape(l);
                if (ms != null) {
                    for (String prefix : ms.shape().prefixes) {
                        if (prefix.equals(moldShape)) return true;
                    }
                }
            }
            for (Item item : moldItems) {
                if (l.getItem() == item && l.getCount() == 1) return true;
            }
            return false;
        }
        return doesStackMatch(l, left) && doesStackMatch(r, right);
    }

    /** {@code doesStackMatch}; heisse Rezepte verlangen bei ItemHot mindestens halbe Hitze. */
    private boolean doesStackMatch(ItemStack input, AStack recipe) {
        if (hot && !isHotEnough(input)) return false;
        return recipe.matches(input);
    }

    /** {@code AnvilSmithingHotRecipe.doesStackMatch}: ItemHot unter halber Hitze passt nicht. */
    public static boolean isHotEnough(ItemStack stack) {
        if (stack.getItem() instanceof com.hbm_m.item.special.ItemHot) {
            return com.hbm_m.item.special.ItemHot.getHeat(stack) >= 0.5D;
        }
        return true;
    }

    /** {@code getOutput(left, right)} */
    public ItemStack getSmithingOutput(ItemStack l, ItemStack r) {
        switch (special) {
            case "cyanide": {
                ItemStack out = l.copy();
                out.setCount(1);
                PlatformHooks.putBoolean(out, r.getItem() == itemById("pill_red") ? "ntmRedPill" : "ntmCyanide", true);
                return out;
            }
            case "rename": {
                ItemStack out = l.copy();
                out.setCount(1);
                if (r.hasCustomHoverName()) {
                    String name = r.getHoverName().getString().replace("\\&", "§");
                    out.setHoverName(Component.literal("§r" + name));
                }
                return out;
            }
            default:
                break;
        }
        ItemStack output = getResultItemSafe();
        if (hot && l.getItem() instanceof com.hbm_m.item.special.ItemHot && r.getItem() instanceof com.hbm_m.item.special.ItemHot
                && output.getItem() instanceof com.hbm_m.item.special.ItemHot) {
            double h1 = com.hbm_m.item.special.ItemHot.getHeat(l);
            double h2 = com.hbm_m.item.special.ItemHot.getHeat(r);
            com.hbm_m.item.special.ItemHot.heatUp(output, (h1 + h2) / 2D);
        }
        return output;
    }

    /** {@code amountConsumed(index, false)}: Form behaelt die Vorlage, Umbenennen behaelt das Namensschild. */
    public int amountConsumed(int index) {
        if (index == 0) return consumeLeft && left != null ? left.count() : 0;
        if (index == 1) return consumeRight && right != null ? right.count() : 0;
        return 0;
    }

    /** {@code getLeft()} fuer die Rezeptanzeige; heisse Rezepte zeigen die Zutat erhitzt. */
    public List<ItemStack> getLeftDisplay() {
        return heated(left);
    }

    public List<ItemStack> getRightDisplay() {
        return heated(right);
    }

    private List<ItemStack> heated(@Nullable AStack stack) {
        if (stack == null) return List.of();
        List<ItemStack> list = stack.displayStacks();
        if (hot && !list.isEmpty() && list.get(0).getItem() instanceof com.hbm_m.item.special.ItemHot) {
            ItemStack first = list.get(0).copy();
            com.hbm_m.item.special.ItemHot.heatUp(first);
            return List.of(first);
        }
        return list;
    }

    private static Item itemById(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, path));
    }

    // ═══════════════════════════ Stufe ═══════════════════════════

    /** {@code isTierValid} (Konstruktion) bzw. {@code rec.tier <= tier} (Schmieden). */
    public boolean canCraftOn(AnvilTier tier) {
        int current = tier.getLegacyId();
        if (current < requiredTier.getLegacyId()) return false;
        if (upperTier != null) return current <= upperTier.getLegacyId();
        return true;
    }

    /** {@code getDisplay()}: Recycling zeigt die erste {@code ComparableStack}-Zutat, sonst die erste Ausgabe. */
    public ItemStack getDisplayStack() {
        if (overlay == OverlayType.RECYCLING) {
            for (AStack stack : inputs) {
                if (stack.comparable()) return stack.firstStack();
            }
        }
        return outputs.get(0).stack().copy();
    }

    public boolean isRecycling() {
        return overlay == OverlayType.RECYCLING;
    }

    // ═══════════════════════════ Recipe ═══════════════════════════

    @Override
    public boolean matchesRecipe(RecipeInputWrapper container, Level level) {
        if (container.size() < 2) return false;
        return matchesSmithing(container.getItem(0), container.getItem(1));
    }

    @Override
    public ItemStack assembleSafe() {
        return getResultItemSafe();
    }

    @Override
    public ItemStack getResultItemSafe() {
        return outputs.get(0).stack().copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements RecipeType<AnvilRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "anvil";
    }

    // ═══════════════════════════ JSON / Netz ═══════════════════════════

    public static JsonObject aStackToJson(AStack stack) {
        JsonElement element = RecipeHooks.ingredientToJson(stack.ingredient());
        JsonObject result;
        if (element.isJsonArray()) {
            result = new JsonObject();
            result.add("items", element);
        } else {
            result = element.getAsJsonObject().deepCopy();
        }
        result.addProperty("count", stack.count());
        if (stack.comparable()) result.addProperty("comparable", true);
        return result;
    }

    public static AStack aStackFromJson(JsonObject obj) {
        int count = GsonHelper.getAsInt(obj, "count", 1);
        boolean comparable = GsonHelper.getAsBoolean(obj, "comparable", false);
        Ingredient ingredient;
        if (obj.has("items")) {
            ingredient = RecipeHooks.ingredientFromJson(obj.get("items"));
        } else {
            JsonObject clone = obj.deepCopy();
            clone.remove("count");
            clone.remove("comparable");
            ingredient = RecipeHooks.ingredientFromJson(clone);
        }
        return new AStack(ingredient, count, comparable);
    }

    private static void writeAStack(FriendlyByteBuf buf, @Nullable AStack s) {
        buf.writeBoolean(s != null);
        if (s == null) return;
        RecipeHooks.writeIngredient(buf, s.ingredient());
        buf.writeVarInt(s.count());
        buf.writeBoolean(s.comparable());
    }

    @Nullable
    private static AStack readAStack(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) return null;
        Ingredient ing = RecipeHooks.readIngredient(buf);
        int count = buf.readVarInt();
        boolean comparable = buf.readBoolean();
        return new AStack(ing, count, comparable);
    }

    public static class Serializer extends PlatformRecipeSerializer<AnvilRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public AnvilRecipe readJson(ResourceLocation id, JsonObject json) {
            Kind kind = "smithing".equals(GsonHelper.getAsString(json, "kind", "construction")) ? Kind.SMITHING : Kind.CONSTRUCTION;
            int sort = GsonHelper.getAsInt(json, "sort", 0);
            AStack left = json.has("left") ? aStackFromJson(GsonHelper.getAsJsonObject(json, "left")) : null;
            AStack right = json.has("right") ? aStackFromJson(GsonHelper.getAsJsonObject(json, "right")) : null;
            boolean consumeLeft = GsonHelper.getAsBoolean(json, "consume_left", true);
            boolean consumeRight = GsonHelper.getAsBoolean(json, "consume_right", true);
            boolean hot = GsonHelper.getAsBoolean(json, "hot", false);
            String special = GsonHelper.getAsString(json, "special", "");
            String moldShape = GsonHelper.getAsString(json, "mold_shape", "");
            List<Item> moldItems = new ArrayList<>();
            if (json.has("mold_items")) {
                for (JsonElement e : GsonHelper.getAsJsonArray(json, "mold_items")) {
                    moldItems.add(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(e.getAsString())));
                }
            }

            List<AStack> inputs = new ArrayList<>();
            if (json.has("inputs")) {
                for (JsonElement e : GsonHelper.getAsJsonArray(json, "inputs")) inputs.add(aStackFromJson(e.getAsJsonObject()));
            }

            List<ResultEntry> outputs = new ArrayList<>();
            for (JsonElement e : GsonHelper.getAsJsonArray(json, "outputs")) {
                JsonObject o = e.getAsJsonObject();
                ItemStack stack = RecipeHooks.itemStackFromJson(o);
                float chance = o.has("chance") ? Mth.clamp(GsonHelper.getAsFloat(o, "chance"), 0.0F, 1.0F) : 1.0F;
                outputs.add(new ResultEntry(stack, chance));
            }

            AnvilTier tier = AnvilTier.valueOf(GsonHelper.getAsString(json, "tier", "iron").toUpperCase(Locale.ROOT));
            AnvilTier upper = json.has("tier_upper") ? AnvilTier.valueOf(GsonHelper.getAsString(json, "tier_upper").toUpperCase(Locale.ROOT)) : null;
            // Original enableLBSM && enableLBSMUnlockAnvil: setTier/AnvilSmithingRecipe -> Stufe 1, setTierRange -> 1..1
            if (com.hbm_m.recipe.condition.ConfigRecipeFlags.test("lbsm_anvil")) {
                tier = AnvilTier.IRON;
                if (upper != null) upper = AnvilTier.IRON;
            }
            OverlayType overlay = OverlayType.byName(GsonHelper.getAsString(json, "overlay", "none"));

            return new AnvilRecipe(id, kind, sort, left, right, consumeLeft, consumeRight, hot, special, moldShape, moldItems,
                    inputs, outputs, tier, upper, overlay);
        }

        @Override
        public AnvilRecipe readNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Kind kind = Kind.values()[buf.readVarInt()];
            int sort = buf.readVarInt();
            AStack left = readAStack(buf);
            AStack right = readAStack(buf);
            boolean consumeLeft = buf.readBoolean();
            boolean consumeRight = buf.readBoolean();
            boolean hot = buf.readBoolean();
            String special = buf.readUtf();
            String moldShape = buf.readUtf();
            int nm = buf.readVarInt();
            List<Item> moldItems = new ArrayList<>();
            for (int i = 0; i < nm; i++) moldItems.add(BuiltInRegistries.ITEM.byId(buf.readVarInt()));
            int ni = buf.readVarInt();
            List<AStack> inputs = new ArrayList<>();
            for (int i = 0; i < ni; i++) inputs.add(readAStack(buf));
            int no = buf.readVarInt();
            List<ResultEntry> outputs = new ArrayList<>();
            for (int i = 0; i < no; i++) {
                ItemStack stack = RecipeHooks.readItem(buf);
                outputs.add(new ResultEntry(stack, buf.readFloat()));
            }
            AnvilTier tier = AnvilTier.values()[buf.readVarInt()];
            int up = buf.readVarInt();
            AnvilTier upper = up >= 0 ? AnvilTier.values()[up] : null;
            OverlayType overlay = OverlayType.values()[buf.readVarInt()];
            return new AnvilRecipe(id, kind, sort, left, right, consumeLeft, consumeRight, hot, special, moldShape, moldItems,
                    inputs, outputs, tier, upper, overlay);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, AnvilRecipe r) {
            buf.writeVarInt(r.kind.ordinal());
            buf.writeVarInt(r.sort);
            writeAStack(buf, r.left);
            writeAStack(buf, r.right);
            buf.writeBoolean(r.consumeLeft);
            buf.writeBoolean(r.consumeRight);
            buf.writeBoolean(r.hot);
            buf.writeUtf(r.special);
            buf.writeUtf(r.moldShape);
            buf.writeVarInt(r.moldItems.size());
            for (Item i : r.moldItems) buf.writeVarInt(BuiltInRegistries.ITEM.getId(i));
            buf.writeVarInt(r.inputs.size());
            for (AStack s : r.inputs) writeAStack(buf, s);
            buf.writeVarInt(r.outputs.size());
            for (ResultEntry e : r.outputs) {
                RecipeHooks.writeItem(buf, e.stack());
                buf.writeFloat(e.chance());
            }
            buf.writeVarInt(r.requiredTier.ordinal());
            buf.writeVarInt(r.upperTier != null ? r.upperTier.ordinal() : -1);
            buf.writeVarInt(r.overlay.ordinal());
        }
    }
}
