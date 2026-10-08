package com.hbm_m.inventory.recipes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.material.NTMMaterial;
import com.hbm_m.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.BedrockOreType;
import com.hbm_m.item.material.ItemScraps;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code ArcFurnaceRecipes}: feste Eintraege (Silizium, Bor, Quarzglas), die Grundgestein-Erzstufen, die
 * Material-Autogenerierung (jede Form eines schmelzbaren Materials wird fluessig), Sonder-Schmelzwerte aus
 * {@code MatDistribution} und Ofenrezepte fuer Erze, Barren, Platten und Bloecke. Wie im Original gewinnt pro Eingabe
 * und Ausgabeart der zuerst registrierte Eintrag.
 */
public final class ArcFurnaceRecipes {

    /** Eingabe (Gegenstueck zu {@code AStack}) und Rezept. */
    public record Entry(Predicate<ItemStack> input, List<ItemStack> display, ArcFurnaceRecipe recipe) { }

    public static final List<Entry> recipeList = new ArrayList<>();
    private static final Map<Item, Optional<ArcFurnaceRecipe>> fastCacheSolid = new HashMap<>();
    private static final Map<Item, Optional<ArcFurnaceRecipe>> fastCacheLiquid = new HashMap<>();
    private static boolean loaded;

    private ArcFurnaceRecipes() { }

    public static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        registerDefaults();
    }

    private static void registerDefaults() {

        Item siliconNugget = ModMaterialItems.item(ModMaterials.SILICON, MaterialShape.NUGGET);

        register(tag(ItemTags.SAND), new ArcFurnaceRecipe().solid(new ItemStack(siliconNugget)).fluid(new MaterialStack(Mats.MAT_SILICON, MaterialShapes.NUGGET.q(1))));
        register(item(Items.FLINT), new ArcFurnaceRecipe().solid(new ItemStack(siliconNugget, 4)).fluid(new MaterialStack(Mats.MAT_SILICON, MaterialShapes.INGOT.q(1, 2))));
        register(item(Items.QUARTZ), new ArcFurnaceRecipe().solid(new ItemStack(siliconNugget, 3)).fluid(new MaterialStack(Mats.MAT_SILICON, MaterialShapes.NUGGET.q(3))));
        register(item(ModMaterialItems.item(ModMaterials.QUARTZ, MaterialShape.POWDER)), new ArcFurnaceRecipe().solid(new ItemStack(siliconNugget, 3)).fluid(new MaterialStack(Mats.MAT_SILICON, MaterialShapes.NUGGET.q(3))));
        register(item(Items.QUARTZ_BLOCK), new ArcFurnaceRecipe().solid(new ItemStack(siliconNugget, 12)).fluid(new MaterialStack(Mats.MAT_SILICON, MaterialShapes.NUGGET.q(12))));
        register(item(ModMaterialItems.item(ModMaterials.FIBERGLASS, MaterialShape.INGOT)), new ArcFurnaceRecipe().solid(new ItemStack(siliconNugget, 4)).fluid(new MaterialStack(Mats.MAT_SILICON, MaterialShapes.INGOT.q(1, 2))));
        register(item(ModBlocks.BLOCK_FIBERGLASS.get().asItem()), new ArcFurnaceRecipe().solid(new ItemStack(siliconNugget, 40)).fluid(new MaterialStack(Mats.MAT_SILICON, MaterialShapes.INGOT.q(9, 2))));
        register(item(ModMaterialItems.item(ModMaterials.ASBESTOS, MaterialShape.INGOT)), new ArcFurnaceRecipe().solid(new ItemStack(siliconNugget, 4)).fluid(new MaterialStack(Mats.MAT_SILICON, MaterialShapes.INGOT.q(1, 2))));
        register(item(ModMaterialItems.item(ModMaterials.ASBESTOS, MaterialShape.POWDER)), new ArcFurnaceRecipe().solid(new ItemStack(siliconNugget, 4)).fluid(new MaterialStack(Mats.MAT_SILICON, MaterialShapes.INGOT.q(1, 2))));
        register(item(ModBlocks.BLOCK_ASBESTOS.get().asItem()), new ArcFurnaceRecipe().solid(new ItemStack(siliconNugget, 40)).fluid(new MaterialStack(Mats.MAT_SILICON, MaterialShapes.INGOT.q(9, 2))));

        // Original: sand_mix (QUARTZ) - im Port als eigener Block sand_quartz
        register(item(ModBlocks.SAND_QUARTZ.get().asItem()), new ArcFurnaceRecipe().solid(new ItemStack(ModBlocks.GLASS_QUARTZ.get())));
        register(item(ModItems.BORAX.get()), new ArcFurnaceRecipe().solid(new ItemStack(ModMaterialItems.item(ModMaterials.BORON, MaterialShape.POWDER_TINY), 3)).fluid(new MaterialStack(Mats.MAT_BORON, MaterialShapes.NUGGET.q(3))));

        for (BedrockOreType type : BedrockOreType.values()) {
            bedrock(type, "sulfuric_byproduct", new ArcFurnaceRecipe().solid(stack(type.item("sulfuric_arc"), 2)));
            bedrock(type, "sulfuric_roasted", new ArcFurnaceRecipe().solid(stack(type.item("sulfuric_arc"), 4)));
            bedrock(type, "solvent_byproduct", new ArcFurnaceRecipe().solid(stack(type.item("solvent_arc"), 2)));
            bedrock(type, "solvent_roasted", new ArcFurnaceRecipe().solid(stack(type.item("solvent_arc"), 4)));
            bedrock(type, "rad_byproduct", new ArcFurnaceRecipe().solid(stack(type.item("rad_arc"), 2)));
            bedrock(type, "rad_roasted", new ArcFurnaceRecipe().solid(stack(type.item("rad_arc"), 4)));

            bedrock(type, "primary_first", new ArcFurnaceRecipe().fluidNull(BedrockOreType.toFluid(type.primary1, 5), BedrockOreType.toFluid(type.primary2, 2)));
            bedrock(type, "primary_second", new ArcFurnaceRecipe().fluidNull(BedrockOreType.toFluid(type.primary1, 2), BedrockOreType.toFluid(type.primary2, 5)));
            bedrock(type, "crumbs", new ArcFurnaceRecipe().fluidNull(BedrockOreType.toFluid(type.primary1, 1), BedrockOreType.toFluid(type.primary2, 1)));

            int i3 = 3;
            bedrock(type, "sulfuric_washed", new ArcFurnaceRecipe().fluidNull(BedrockOreType.toFluid(type.byproductAcid1, i3), BedrockOreType.toFluid(type.byproductAcid2, i3), BedrockOreType.toFluid(type.byproductAcid3, i3)));
            bedrock(type, "solvent_washed", new ArcFurnaceRecipe().fluidNull(BedrockOreType.toFluid(type.byproductSolvent1, i3), BedrockOreType.toFluid(type.byproductSolvent2, i3), BedrockOreType.toFluid(type.byproductSolvent3, i3)));
            bedrock(type, "rad_washed", new ArcFurnaceRecipe().fluidNull(BedrockOreType.toFluid(type.byproductRad1, i3), BedrockOreType.toFluid(type.byproductRad2, i3), BedrockOreType.toFluid(type.byproductRad3, i3)));
        }
    }

    private static ItemStack stack(@Nullable Item item, int count) {
        return item == null ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static void bedrock(BedrockOreType type, String grade, ArcFurnaceRecipe recipe) {
        Item in = type.item(grade);
        if (in == null) return;
        if (recipe.solidOutput != null && recipe.solidOutput.isEmpty()) return;
        register(item(in), recipe);
    }

    private static Entry item(Item item) {
        return new Entry(s -> s.is(item), List.of(new ItemStack(item)), null);
    }

    private static Entry tag(TagKey<Item> tag) {
        List<ItemStack> display = new ArrayList<>();
        BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(h -> display.add(new ItemStack(h.value())));
        return new Entry(s -> s.is(tag), display, null);
    }

    /** Original {@code register}: belegte Eingaben je Ausgabeart werden nicht ueberschrieben (Lookup-Reihenfolge). */
    public static void register(Entry input, ArcFurnaceRecipe output) {
        recipeList.add(new Entry(input.input(), input.display(), output));
    }

    // ═══════════════════════════ Suche ═══════════════════════════

    @Nullable
    public static ArcFurnaceRecipe getOutput(ItemStack stack, boolean liquid) {
        return getOutput(stack, liquid, null);
    }

    /** 1:1 {@code getOutput}; {@code level} fuer die Ofenrezepte (Original: FurnaceRecipes, hier der RecipeManager). */
    @Nullable
    public static ArcFurnaceRecipe getOutput(ItemStack stack, boolean liquid, @Nullable Level level) {

        if (stack == null || stack.isEmpty()) return null;
        ensureLoaded();

        if (ItemScraps.isScrap(stack) && liquid) {
            MaterialStack mats = ItemScraps.getMats(stack);
            if (mats == null || mats.material == null) return null;
            if (mats.material.smeltable == SmeltingBehavior.SMELTABLE) {
                return new ArcFurnaceRecipe().fluid(mats);
            }
        }

        Map<Item, Optional<ArcFurnaceRecipe>> cache = liquid ? fastCacheLiquid : fastCacheSolid;
        Optional<ArcFurnaceRecipe> cached = cache.get(stack.getItem());
        if (cached != null && (cached.isPresent() || liquid || level == null)) return cached.orElse(null);

        ArcFurnaceRecipe found = lookup(stack, liquid, level);
        if (found != null || liquid || level != null) cache.put(stack.getItem(), Optional.ofNullable(found));
        return found;
    }

    @Nullable
    private static ArcFurnaceRecipe lookup(ItemStack stack, boolean liquid, @Nullable Level level) {

        for (Entry entry : recipeList) {
            if (entry.input().test(stack)) {
                ArcFurnaceRecipe rec = entry.recipe();
                if ((liquid && rec.fluidOutput != null) || (!liquid && rec.solidOutput != null)) return rec;
            }
        }

        if (liquid) {
            // Autogen for simple single type items
            Mats.MatShape shape = Mats.getShape(stack);
            if (shape != null && !shape.shape().noAutogen) {
                NTMMaterial material = shape.material();
                NTMMaterial convert = material.smeltsInto;
                if (convert.smeltable == SmeltingBehavior.SMELTABLE) {
                    return new ArcFurnaceRecipe().fluid(new MaterialStack(convert, (int) (shape.shape().q(1) * material.convOut / material.convIn)));
                }
            }

            // Autogen for custom smeltables (Erz-Eintraege, dann Item-Eintraege)
            com.hbm_m.inventory.material.MatDistribution.ensureLoaded();
            String oreKey = Mats.oreItemKeys.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            if (oreKey == null) {
                for (TagKey<Item> tag : (Iterable<TagKey<Item>>) stack.getTags()::iterator) {
                    String key = tag.location().toString();
                    if (Mats.materialOreEntries.containsKey(key)) { oreKey = key; break; }
                }
            }
            if (oreKey != null) {
                ArcFurnaceRecipe r = customSmeltable(Mats.materialOreEntries.get(oreKey));
                if (r != null) return r;
            }
            List<MaterialStack> entries = Mats.materialEntries.get(stack.getItem());
            if (entries != null) {
                ArcFurnaceRecipe r = customSmeltable(entries);
                if (r != null) return r;
            }
            return null;
        }

        // Autogen for furnace recipes
        if (level != null) {
            Optional<SmeltingRecipe> smelt = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(stack.copyWithCount(1)), level);
            if (smelt.isPresent()) {
                ItemStack output = smelt.get().getResultItem(level.registryAccess());
                if (!output.isEmpty() && (isArcSmeltable(stack) || isArcSmeltable(output))) {
                    return new ArcFurnaceRecipe().solid(output.copy());
                }
            }
        }

        return null;
    }

    @Nullable
    private static ArcFurnaceRecipe customSmeltable(List<MaterialStack> mats) {
        List<MaterialStack> smeltables = new ArrayList<>();
        for (MaterialStack mat : mats) {
            if (mat.material.smeltable == SmeltingBehavior.SMELTABLE) smeltables.add(mat);
        }
        if (smeltables.isEmpty()) return null;
        return new ArcFurnaceRecipe().fluid(smeltables.toArray(new MaterialStack[0]));
    }

    private static final String[] ARC_TAG_FOLDERS = { "ores", "ingots", "plates", "storage_blocks" };

    /**
     * Original {@code OreDictManager.arcSmeltable}: alles mit Ore-Dictionary-Namen ingot*, ore*, plate*, block* plus die
     * Vanilla-Erze, -Bloecke und Eisen-/Goldbarren. Hier ueber die entsprechenden Forge-Tags.
     */
    public static boolean isArcSmeltable(ItemStack stack) {
        for (TagKey<Item> tag : (Iterable<TagKey<Item>>) stack.getTags()::iterator) {
            ResourceLocation loc = tag.location();
            if (!loc.getNamespace().equals("forge")) continue;
            for (String f : ARC_TAG_FOLDERS) {
                if (loc.getPath().equals(f) || loc.getPath().startsWith(f + "/")) return true;
            }
        }
        Item i = stack.getItem();
        return i == Items.GOLD_ORE || i == Items.IRON_ORE || i == Items.LAPIS_ORE || i == Items.DIAMOND_ORE || i == Items.REDSTONE_ORE
                || i == Items.EMERALD_ORE || i == Items.NETHER_QUARTZ_ORE || i == Items.GOLD_BLOCK || i == Items.IRON_BLOCK
                || i == Items.LAPIS_BLOCK || i == Items.DIAMOND_BLOCK || i == Items.REDSTONE_BLOCK || i == Items.EMERALD_BLOCK
                || i == Items.QUARTZ_BLOCK || i == Items.IRON_INGOT || i == Items.GOLD_INGOT;
    }

    public static class ArcFurnaceRecipe {

        @Nullable public MaterialStack[] fluidOutput;
        @Nullable public ItemStack solidOutput;

        public ArcFurnaceRecipe fluid(MaterialStack... outputs) {
            this.fluidOutput = outputs;
            return this;
        }

        public ArcFurnaceRecipe fluidNull(MaterialStack... outputs) {
            List<MaterialStack> mat = new ArrayList<>();
            for (MaterialStack stack : outputs) if (stack != null) mat.add(stack);
            if (!mat.isEmpty()) this.fluidOutput = mat.toArray(new MaterialStack[0]);
            return this;
        }

        public ArcFurnaceRecipe solid(ItemStack output) {
            this.solidOutput = output;
            return this;
        }
    }
}
