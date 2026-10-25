package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.google.gson.JsonObject;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.lib.RefStrings;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import static com.hbm_m.datagen.recipes.custom.OreDictIngredients.*;

/**
 * Maschinenrezepte des Originals (ShredderRecipes, CrystallizerRecipes, SmeltingRecipes, PressRecipes, CentrifugeRecipes), die im Port fehlten -
 * 1:1 uebersetzt. AUTOMATISCH ERZEUGT durch {@code rc/machines.py}; Rezepte, deren Eingabe (bzw. Eingabe+Saeure)
 * der Port schon kennt, werden nicht doppelt erzeugt.
 */
public final class OrigMachineRecipeGenerator {

    private OrigMachineRecipeGenerator() {}

    /** Vom Original abweichende Port-Maschinenrezepte (gleiche Eingabe, andere Ausgabe), hier 1:1 neu erzeugt. */
    public static final java.util.Set<String> SUPERSEDED = java.util.Set.of(
            "hbm_m:aluminum_ore_smelting",
            "hbm_m:centrifuge/beryllium_ore",
            "hbm_m:centrifuge/crystal_copper",
            "hbm_m:centrifuge/crystal_gold",
            "hbm_m:centrifuge/crystal_thorium",
            "hbm_m:centrifuge/crystal_uranium",
            "hbm_m:centrifuge/fluorite_ore",
            "hbm_m:centrifuge/lapis_ore",
            "hbm_m:centrifuge/lignite_ore",
            "hbm_m:centrifuge/uranium_ore"
    );

    /** Laesst die ersetzten Port-Maschinenrezepte beim Datagen weg. */
    public static Consumer<FinishedRecipe> filter(Consumer<FinishedRecipe> writer) {
        return r -> {
            if (!SUPERSEDED.contains(r.getId().toString())) writer.accept(r);
        };
    }

    private static FluidStack fl(ModFluids.FluidEntry f, int mb) {
        return FluidStack.create(f.getSource(), (long) mb);
    }

    /** Ofenrezept mit Ausgabemenge (Forge liest "result" als Objekt mit count). */
    private static void smelting(Consumer<FinishedRecipe> w, Ingredient in, ItemStack out, float xp, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path);
        w.accept(new FinishedRecipe() {
            @Override
            public void serializeRecipeData(JsonObject json) {
                json.addProperty("category", "misc");
                json.add("ingredient", in.toJson());
                JsonObject result = new JsonObject();
                result.addProperty("item", BuiltInRegistries.ITEM.getKey(out.getItem()).toString());
                if (out.getCount() > 1) result.addProperty("count", out.getCount());
                json.add("result", result);
                json.addProperty("experience", xp);
                json.addProperty("cookingtime", 200);
            }

            @Override
            public ResourceLocation getId() { return id; }

            @Override
            public RecipeSerializer<?> getType() { return RecipeSerializer.SMELTING_RECIPE; }

            @Override
            public JsonObject serializeAdvancement() { return null; }

            @Override
            public ResourceLocation getAdvancementId() { return null; }
        });
    }

    public static void generate(Consumer<FinishedRecipe> w) {
        shredderRecipes(w);
        crystallizerRecipes(w);
        smeltingRecipes(w);
        pressRecipes(w);
        centrifugeRecipes(w);
        checkMissing("OrigMachineRecipeGenerator");
    }

    private static void shredderRecipes(Consumer<FinishedRecipe> w) {
        // ShredderRecipes:139
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:dust")), stack("hbm_m:dust", 1)).save(w, "orig_machines/shredder/dust_1");
        // ShredderRecipes:140
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:dust_tiny")), stack("hbm_m:dust_tiny", 1)).save(w, "orig_machines/shredder/dust_tiny_1");
        // ShredderRecipes:142
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:quartz_block")), stack("hbm_m:quartz_powder", 4)).save(w, "orig_machines/shredder/quartz_powder_1");
        // ShredderRecipes:143
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:chiseled_quartz_block")), stack("hbm_m:quartz_powder", 4)).save(w, "orig_machines/shredder/quartz_powder_2");
        // ShredderRecipes:144
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:quartz_pillar")), stack("hbm_m:quartz_powder", 4)).save(w, "orig_machines/shredder/quartz_powder_3");
        // ShredderRecipes:145
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:quartz_stairs")), stack("hbm_m:quartz_powder", 3)).save(w, "orig_machines/shredder/quartz_powder_4");
        // ShredderRecipes:147
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:quartz")), stack("hbm_m:quartz_powder", 1)).save(w, "orig_machines/shredder/quartz_powder_5");
        // ShredderRecipes:148
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:nether_quartz_ore")), stack("hbm_m:quartz_powder", 2)).save(w, "orig_machines/shredder/quartz_powder_6");
        // ShredderRecipes:149
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:nether_fire_ore")), stack("hbm_m:fire_powder", 6)).save(w, "orig_machines/shredder/fire_powder_1");
        // ShredderRecipes:150
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:packed_ice")), stack("hbm_m:ice_powder", 1)).save(w, "orig_machines/shredder/ice_powder_1");
        // ShredderRecipes:151
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:brick_light")), stack("minecraft:clay_ball", 4)).save(w, "orig_machines/shredder/clay_ball_1");
        // ShredderRecipes:152
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:concrete")), stack("minecraft:gravel", 1)).save(w, "orig_machines/shredder/gravel_1");
        // ShredderRecipes:153
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:concrete_smooth")), stack("minecraft:gravel", 1)).save(w, "orig_machines/shredder/gravel_2");
        // ShredderRecipes:154
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:brick_concrete")), stack("minecraft:gravel", 1)).save(w, "orig_machines/shredder/gravel_3");
        // ShredderRecipes:155
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:brick_concrete_mossy")), stack("minecraft:gravel", 1)).save(w, "orig_machines/shredder/gravel_4");
        // ShredderRecipes:156
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:brick_concrete_cracked")), stack("minecraft:gravel", 1)).save(w, "orig_machines/shredder/gravel_5");
        // ShredderRecipes:157
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:brick_concrete_broken")), stack("minecraft:gravel", 1)).save(w, "orig_machines/shredder/gravel_6");
        // ShredderRecipes:158
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:brick_obsidian")), stack("hbm_m:gravel_obsidian", 1)).save(w, "orig_machines/shredder/gravel_obsidian_1");
        // ShredderRecipes:159
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:obsidian")), stack("hbm_m:gravel_obsidian", 1)).save(w, "orig_machines/shredder/gravel_obsidian_2");
        // ShredderRecipes:161
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:ore_oil_empty")), stack("minecraft:gravel", 1)).save(w, "orig_machines/shredder/gravel_7");
        // ShredderRecipes:166
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:brick_stairs")), stack("minecraft:clay_ball", 3)).save(w, "orig_machines/shredder/clay_ball_2");
        // ShredderRecipes:167
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:flower_pot")), stack("minecraft:clay_ball", 3)).save(w, "orig_machines/shredder/clay_ball_3");
        // ShredderRecipes:169
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:sandstone")), stack("minecraft:sand", 4)).save(w, "orig_machines/shredder/sand_1");
        // ShredderRecipes:170
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:sandstone_stairs")), stack("minecraft:sand", 6)).save(w, "orig_machines/shredder/sand_2");
        // ShredderRecipes:171
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:clay")), stack("minecraft:clay_ball", 4)).save(w, "orig_machines/shredder/clay_ball_4");
        // ShredderRecipes:172
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:terracotta")), stack("minecraft:clay_ball", 4)).save(w, "orig_machines/shredder/clay_ball_5");
        // ShredderRecipes:174
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:stone_resource_limestone")), stack("hbm_m:limestone_powder", 4)).save(w, "orig_machines/shredder/limestone_powder_1");
        // ShredderRecipes:175
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:stone_gneiss")), stack("hbm_m:lithium_powder_tiny", 1)).save(w, "orig_machines/shredder/lithium_powder_tiny_1");
        // ShredderRecipes:176
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:fragment_neodymium")), stack("hbm_m:neodymium_powder_tiny", 1)).save(w, "orig_machines/shredder/neodymium_powder_tiny_1");
        // ShredderRecipes:177
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:fragment_cobalt")), stack("hbm_m:cobalt_powder_tiny", 1)).save(w, "orig_machines/shredder/cobalt_powder_tiny_1");
        // ShredderRecipes:178
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:fragment_niobium")), stack("hbm_m:niobium_powder_tiny", 1)).save(w, "orig_machines/shredder/niobium_powder_tiny_1");
        // ShredderRecipes:179
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:fragment_cerium")), stack("hbm_m:cerium_powder_tiny", 1)).save(w, "orig_machines/shredder/cerium_powder_tiny_1");
        // ShredderRecipes:180
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:fragment_lanthanium")), stack("hbm_m:lanthanium_powder_tiny", 1)).save(w, "orig_machines/shredder/lanthanium_powder_tiny_1");
        // ShredderRecipes:181
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:fragment_actinium")), stack("hbm_m:actinium_powder_tiny", 1)).save(w, "orig_machines/shredder/actinium_powder_tiny_1");
        // ShredderRecipes:182
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:fragment_boron")), stack("hbm_m:boron_powder_tiny", 1)).save(w, "orig_machines/shredder/boron_powder_tiny_1");
        // ShredderRecipes:183
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:fragment_meteorite")), stack("hbm_m:meteorite_powder_tiny", 1)).save(w, "orig_machines/shredder/meteorite_powder_tiny_1");
        // ShredderRecipes:184
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:block_meteor")), stack("hbm_m:meteorite_powder", 10)).save(w, "orig_machines/shredder/meteorite_powder_1");
        // ShredderRecipes:185
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:enchanted_book")), stack("hbm_m:magic_powder", 1)).save(w, "orig_machines/shredder/magic_powder_1");
        // ShredderRecipes:186
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:meteor_polished")), stack("hbm_m:meteorite_powder", 1)).save(w, "orig_machines/shredder/meteorite_powder_2");
        // ShredderRecipes:187
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:meteor_brick")), stack("hbm_m:meteorite_powder", 1)).save(w, "orig_machines/shredder/meteorite_powder_3");
        // ShredderRecipes:188
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:meteor_brick_mossy")), stack("hbm_m:meteorite_powder", 1)).save(w, "orig_machines/shredder/meteorite_powder_4");
        // ShredderRecipes:189
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:meteor_brick_cracked")), stack("hbm_m:meteorite_powder", 1)).save(w, "orig_machines/shredder/meteorite_powder_5");
        // ShredderRecipes:190
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:meteor_brick_chiseled")), stack("hbm_m:meteorite_powder", 1)).save(w, "orig_machines/shredder/meteorite_powder_6");
        // ShredderRecipes:191
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:meteor_pillar")), stack("hbm_m:meteorite_powder", 1)).save(w, "orig_machines/shredder/meteorite_powder_7");
        // ShredderRecipes:192
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:ore_rare")), stack("hbm_m:powder_desh_mix", 1)).save(w, "orig_machines/shredder/powder_desh_mix_1");
        // ShredderRecipes:193
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:diamond_ore")), stack("hbm_m:gravel_diamond", 2)).save(w, "orig_machines/shredder/gravel_diamond_1");
        // ShredderRecipes:194
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:ore_sellafield_diamond")), stack("hbm_m:gravel_diamond", 2)).save(w, "orig_machines/shredder/gravel_diamond_2");
        // ShredderRecipes:198
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:coal_infernal")), stack("hbm_m:coal_powder", 2)).save(w, "orig_machines/shredder/coal_powder_1");
        // ShredderRecipes:199
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:fermented_spider_eye")), stack("hbm_m:powder_poison", 3)).save(w, "orig_machines/shredder/powder_poison_1");
        // ShredderRecipes:200
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:poisonous_potato")), stack("hbm_m:powder_poison", 1)).save(w, "orig_machines/shredder/powder_poison_2");
        // ShredderRecipes:201
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:ore_tektite_osmiridium")), stack("hbm_m:powder_tektite", 1)).save(w, "orig_machines/shredder/powder_tektite_1");
        // ShredderRecipes:202
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:dirt")), stack("hbm_m:dust", 1)).save(w, "orig_machines/shredder/dust_2");
        // ShredderRecipes:203
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:sugar_cane")), stack("minecraft:sugar", 3)).save(w, "orig_machines/shredder/sugar_1");
        // ShredderRecipes:204
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:apple")), stack("minecraft:sugar", 1)).save(w, "orig_machines/shredder/sugar_2");
        // ShredderRecipes:205
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:carrot")), stack("minecraft:sugar", 1)).save(w, "orig_machines/shredder/sugar_3");
        // ShredderRecipes:206
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:can_empty")), stack("hbm_m:aluminum_powder", 2)).save(w, "orig_machines/shredder/aluminum_powder_1");
        // ShredderRecipes:207
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:rareground_ore_chunk")), stack("hbm_m:powder_desh_mix", 1)).save(w, "orig_machines/shredder/powder_desh_mix_2");
        // ShredderRecipes:208
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:sand")), stack("hbm_m:dust", 2)).save(w, "orig_machines/shredder/dust_3");
        // ShredderRecipes:209
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:block_slag")), stack("hbm_m:cement_powder", 4)).save(w, "orig_machines/shredder/cement_powder_1");
        // ShredderRecipes:210
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:ore_aluminium")), stack("hbm_m:cryolite_chunk", 2)).save(w, "orig_machines/shredder/cryolite_chunk_1");
        // ShredderRecipes:238
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:schraranium_ingot")), stack("hbm_m:nugget_schrabidium", 2)).save(w, "orig_machines/shredder/nugget_schrabidium_1");
        // ShredderRecipes:239
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_coal")), stack("hbm_m:coal_powder", 3)).save(w, "orig_machines/shredder/coal_powder_2");
        // ShredderRecipes:240
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_iron")), stack("hbm_m:iron_powder", 3)).save(w, "orig_machines/shredder/iron_powder_1");
        // ShredderRecipes:241
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_gold")), stack("hbm_m:gold_powder", 3)).save(w, "orig_machines/shredder/gold_powder_1");
        // ShredderRecipes:242
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_redstone")), stack("minecraft:redstone", 8)).save(w, "orig_machines/shredder/redstone_1");
        // ShredderRecipes:243
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_lapis")), stack("hbm_m:lapis_powder", 8)).save(w, "orig_machines/shredder/lapis_powder_1");
        // ShredderRecipes:244
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_diamond")), stack("hbm_m:diamond_powder", 3)).save(w, "orig_machines/shredder/diamond_powder_1");
        // ShredderRecipes:245
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_uranium")), stack("hbm_m:uranium_powder", 3)).save(w, "orig_machines/shredder/uranium_powder_1");
        // ShredderRecipes:246
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_plutonium")), stack("hbm_m:plutonium_powder", 3)).save(w, "orig_machines/shredder/plutonium_powder_1");
        // ShredderRecipes:247
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_thorium")), stack("hbm_m:thorium_powder", 3)).save(w, "orig_machines/shredder/thorium_powder_1");
        // ShredderRecipes:248
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_titanium")), stack("hbm_m:titanium_powder", 3)).save(w, "orig_machines/shredder/titanium_powder_1");
        // ShredderRecipes:249
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_sulfur")), stack("hbm_m:sulfur", 8)).save(w, "orig_machines/shredder/sulfur_1");
        // ShredderRecipes:250
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_niter")), stack("hbm_m:niter", 8)).save(w, "orig_machines/shredder/niter_1");
        // ShredderRecipes:251
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_copper")), stack("hbm_m:copper_powder", 3)).save(w, "orig_machines/shredder/copper_powder_1");
        // ShredderRecipes:252
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_tungsten")), stack("hbm_m:tungsten_powder", 3)).save(w, "orig_machines/shredder/tungsten_powder_1");
        // ShredderRecipes:253
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_aluminium")), stack("hbm_m:aluminum_powder", 3)).save(w, "orig_machines/shredder/aluminum_powder_2");
        // ShredderRecipes:254
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_fluorite")), stack("hbm_m:fluorite", 8)).save(w, "orig_machines/shredder/fluorite_1");
        // ShredderRecipes:255
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_beryllium")), stack("hbm_m:beryllium_powder", 3)).save(w, "orig_machines/shredder/beryllium_powder_1");
        // ShredderRecipes:256
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_lead")), stack("hbm_m:lead_powder", 3)).save(w, "orig_machines/shredder/lead_powder_1");
        // ShredderRecipes:257
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_schraranium")), stack("hbm_m:nugget_schrabidium", 3)).save(w, "orig_machines/shredder/nugget_schrabidium_2");
        // ShredderRecipes:258
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_schrabidium")), stack("hbm_m:schrabidium_powder", 3)).save(w, "orig_machines/shredder/schrabidium_powder_1");
        // ShredderRecipes:259
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_rare")), stack("hbm_m:powder_desh_mix", 2)).save(w, "orig_machines/shredder/powder_desh_mix_3");
        // ShredderRecipes:260
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_phosphorus")), stack("hbm_m:fire_powder", 8)).save(w, "orig_machines/shredder/fire_powder_2");
        // ShredderRecipes:261
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_trixite")), stack("hbm_m:plutonium_powder", 6)).save(w, "orig_machines/shredder/plutonium_powder_2");
        // ShredderRecipes:262
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_lithium")), stack("hbm_m:lithium_powder", 3)).save(w, "orig_machines/shredder/lithium_powder_1");
        // ShredderRecipes:263
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_starmetal")), stack("hbm_m:dura_steel_powder", 6)).save(w, "orig_machines/shredder/dura_steel_powder_1");
        // ShredderRecipes:264
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crystal_cobalt")), stack("hbm_m:cobalt_powder", 3)).save(w, "orig_machines/shredder/cobalt_powder_1");
        // ShredderRecipes:268
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:steel_roof")), stack("hbm_m:steel_powder_tiny", 9)).save(w, "orig_machines/shredder/steel_powder_tiny_1");
        // ShredderRecipes:269
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:steel_wall")), stack("hbm_m:steel_powder_tiny", 9)).save(w, "orig_machines/shredder/steel_powder_tiny_2");
        // ShredderRecipes:271
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:steel_beam")), stack("hbm_m:steel_powder_tiny", 3)).save(w, "orig_machines/shredder/steel_powder_tiny_3");
        // ShredderRecipes:272
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:steel_scaffold"), item("hbm_m:steel_scaffold_red"), item("hbm_m:steel_scaffold_white"), item("hbm_m:steel_scaffold_yellow")), stack("hbm_m:steel_powder_tiny", 4)).save(w, "orig_machines/shredder/steel_powder_tiny_4");
        // ShredderRecipes:273
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:coil_copper")), stack("hbm_m:red_copper_powder", 1)).save(w, "orig_machines/shredder/red_copper_powder_1");
        // ShredderRecipes:274
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:coil_copper_torus")), stack("hbm_m:red_copper_powder", 2)).save(w, "orig_machines/shredder/red_copper_powder_2");
        // ShredderRecipes:275
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:coil_gold")), stack("hbm_m:gold_powder", 1)).save(w, "orig_machines/shredder/gold_powder_2");
        // ShredderRecipes:276
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:coil_gold_torus")), stack("hbm_m:gold_powder", 2)).save(w, "orig_machines/shredder/gold_powder_3");
        // ShredderRecipes:277
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:coil_tungsten")), stack("hbm_m:tungsten_powder", 1)).save(w, "orig_machines/shredder/tungsten_powder_2");
        // ShredderRecipes:278
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:coil_magnetized_tungsten")), stack("hbm_m:magnetized_tungsten_powder", 1)).save(w, "orig_machines/shredder/magnetized_tungsten_powder_1");
        // ShredderRecipes:279
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crate_iron")), stack("hbm_m:iron_powder", 8)).save(w, "orig_machines/shredder/iron_powder_2");
        // ShredderRecipes:280
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crate_steel")), stack("hbm_m:steel_powder", 8)).save(w, "orig_machines/shredder/steel_powder_1");
        // ShredderRecipes:281
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:crate_tungsten")), stack("hbm_m:tungsten_powder", 36)).save(w, "orig_machines/shredder/tungsten_powder_3");
        // ShredderRecipes:282
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("minecraft:anvil")), stack("hbm_m:iron_powder", 31)).save(w, "orig_machines/shredder/iron_powder_3");
        // ShredderRecipes:285
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:pipes_steel")), stack("hbm_m:steel_powder", 27)).save(w, "orig_machines/shredder/steel_powder_2");
        // ShredderRecipes:286
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:bedrock_ore_base"), item("hbm_m:bedrock_ore_base_actinide"), item("hbm_m:bedrock_ore_base_crystal"), item("hbm_m:bedrock_ore_base_heavy"), item("hbm_m:bedrock_ore_base_light"), item("hbm_m:bedrock_ore_base_nonmetal"), item("hbm_m:bedrock_ore_base_rare"), item("hbm_m:bedrock_ore_base_roasted_actinide"), item("hbm_m:bedrock_ore_base_roasted_crystal"), item("hbm_m:bedrock_ore_base_roasted_heavy"), item("hbm_m:bedrock_ore_base_roasted_light"), item("hbm_m:bedrock_ore_base_roasted_nonmetal"), item("hbm_m:bedrock_ore_base_roasted_rare"), item("hbm_m:bedrock_ore_base_washed_actinide"), item("hbm_m:bedrock_ore_base_washed_crystal"), item("hbm_m:bedrock_ore_base_washed_heavy"), item("hbm_m:bedrock_ore_base_washed_light"), item("hbm_m:bedrock_ore_base_washed_nonmetal"), item("hbm_m:bedrock_ore_base_washed_rare"), item("hbm_m:bedrock_ore_crumbs_actinide"), item("hbm_m:bedrock_ore_crumbs_crystal"), item("hbm_m:bedrock_ore_crumbs_heavy"), item("hbm_m:bedrock_ore_crumbs_light"), item("hbm_m:bedrock_ore_crumbs_nonmetal"), item("hbm_m:bedrock_ore_crumbs_rare"), item("hbm_m:bedrock_ore_fragment"), item("hbm_m:bedrock_ore_fragment_aluminium"), item("hbm_m:bedrock_ore_fragment_asbestos"), item("hbm_m:bedrock_ore_fragment_bauxite"), item("hbm_m:bedrock_ore_fragment_beryllium"), item("hbm_m:bedrock_ore_fragment_bismuth"), item("hbm_m:bedrock_ore_fragment_borax"), item("hbm_m:bedrock_ore_fragment_boron"), item("hbm_m:bedrock_ore_fragment_chlorocalcite"), item("hbm_m:bedrock_ore_fragment_cinnabar"), item("hbm_m:bedrock_ore_fragment_coal"), item("hbm_m:bedrock_ore_fragment_cobalt"), item("hbm_m:bedrock_ore_fragment_copper"), item("hbm_m:bedrock_ore_fragment_cryolite"), item("hbm_m:bedrock_ore_fragment_diamond"), item("hbm_m:bedrock_ore_fragment_emerald"), item("hbm_m:bedrock_ore_fragment_fluorite"), item("hbm_m:bedrock_ore_fragment_gold"), item("hbm_m:bedrock_ore_fragment_iron"), item("hbm_m:bedrock_ore_fragment_kno"), item("hbm_m:bedrock_ore_fragment_lanthanium"), item("hbm_m:bedrock_ore_fragment_lead"), item("hbm_m:bedrock_ore_fragment_lignite"), item("hbm_m:bedrock_ore_fragment_lithium"), item("hbm_m:bedrock_ore_fragment_molysite"), item("hbm_m:bedrock_ore_fragment_neodymium"), item("hbm_m:bedrock_ore_fragment_niobium"), item("hbm_m:bedrock_ore_fragment_phosphorus"), item("hbm_m:bedrock_ore_fragment_po210"), item("hbm_m:bedrock_ore_fragment_ra226"), item("hbm_m:bedrock_ore_fragment_rare_earth"), item("hbm_m:bedrock_ore_fragment_redstone"), item("hbm_m:bedrock_ore_fragment_silicon"), item("hbm_m:bedrock_ore_fragment_sodalite"), item("hbm_m:bedrock_ore_fragment_sodium"), item("hbm_m:bedrock_ore_fragment_strontium"), item("hbm_m:bedrock_ore_fragment_sulfur"), item("hbm_m:bedrock_ore_fragment_tantalium"), item("hbm_m:bedrock_ore_fragment_tc99"), item("hbm_m:bedrock_ore_fragment_thorium"), item("hbm_m:bedrock_ore_fragment_titanium"), item("hbm_m:bedrock_ore_fragment_tungsten"), item("hbm_m:bedrock_ore_fragment_u238"), item("hbm_m:bedrock_ore_fragment_uranium"), item("hbm_m:bedrock_ore_fragment_zirconium"), item("hbm_m:bedrock_ore_primary_actinide"), item("hbm_m:bedrock_ore_primary_crystal"), item("hbm_m:bedrock_ore_primary_first_actinide"), item("hbm_m:bedrock_ore_primary_first_crystal"), item("hbm_m:bedrock_ore_primary_first_heavy"), item("hbm_m:bedrock_ore_primary_first_light"), item("hbm_m:bedrock_ore_primary_first_nonmetal"), item("hbm_m:bedrock_ore_primary_first_rare"), item("hbm_m:bedrock_ore_primary_heavy"), item("hbm_m:bedrock_ore_primary_light"), item("hbm_m:bedrock_ore_primary_nonmetal"), item("hbm_m:bedrock_ore_primary_norad_actinide"), item("hbm_m:bedrock_ore_primary_norad_crystal"), item("hbm_m:bedrock_ore_primary_norad_heavy"), item("hbm_m:bedrock_ore_primary_norad_light"), item("hbm_m:bedrock_ore_primary_norad_nonmetal"), item("hbm_m:bedrock_ore_primary_norad_rare"), item("hbm_m:bedrock_ore_primary_nosolvent_actinide"), item("hbm_m:bedrock_ore_primary_nosolvent_crystal"), item("hbm_m:bedrock_ore_primary_nosolvent_heavy"), item("hbm_m:bedrock_ore_primary_nosolvent_light"), item("hbm_m:bedrock_ore_primary_nosolvent_nonmetal"), item("hbm_m:bedrock_ore_primary_nosolvent_rare"), item("hbm_m:bedrock_ore_primary_nosulfuric_actinide"), item("hbm_m:bedrock_ore_primary_nosulfuric_crystal"), item("hbm_m:bedrock_ore_primary_nosulfuric_heavy"), item("hbm_m:bedrock_ore_primary_nosulfuric_light"), item("hbm_m:bedrock_ore_primary_nosulfuric_nonmetal"), item("hbm_m:bedrock_ore_primary_nosulfuric_rare"), item("hbm_m:bedrock_ore_primary_rad_actinide"), item("hbm_m:bedrock_ore_primary_rad_crystal"), item("hbm_m:bedrock_ore_primary_rad_heavy"), item("hbm_m:bedrock_ore_primary_rad_light"), item("hbm_m:bedrock_ore_primary_rad_nonmetal"), item("hbm_m:bedrock_ore_primary_rad_rare"), item("hbm_m:bedrock_ore_primary_rare"), item("hbm_m:bedrock_ore_primary_roasted_actinide"), item("hbm_m:bedrock_ore_primary_roasted_crystal"), item("hbm_m:bedrock_ore_primary_roasted_heavy"), item("hbm_m:bedrock_ore_primary_roasted_light"), item("hbm_m:bedrock_ore_primary_roasted_nonmetal"), item("hbm_m:bedrock_ore_primary_roasted_rare"), item("hbm_m:bedrock_ore_primary_second_actinide"), item("hbm_m:bedrock_ore_primary_second_crystal"), item("hbm_m:bedrock_ore_primary_second_heavy"), item("hbm_m:bedrock_ore_primary_second_light"), item("hbm_m:bedrock_ore_primary_second_nonmetal"), item("hbm_m:bedrock_ore_primary_second_rare"), item("hbm_m:bedrock_ore_primary_solvent_actinide"), item("hbm_m:bedrock_ore_primary_solvent_crystal"), item("hbm_m:bedrock_ore_primary_solvent_heavy"), item("hbm_m:bedrock_ore_primary_solvent_light"), item("hbm_m:bedrock_ore_primary_solvent_nonmetal"), item("hbm_m:bedrock_ore_primary_solvent_rare"), item("hbm_m:bedrock_ore_primary_sulfuric_actinide"), item("hbm_m:bedrock_ore_primary_sulfuric_crystal"), item("hbm_m:bedrock_ore_primary_sulfuric_heavy"), item("hbm_m:bedrock_ore_primary_sulfuric_light"), item("hbm_m:bedrock_ore_primary_sulfuric_nonmetal"), item("hbm_m:bedrock_ore_primary_sulfuric_rare"), item("hbm_m:bedrock_ore_rad_arc_actinide"), item("hbm_m:bedrock_ore_rad_arc_crystal"), item("hbm_m:bedrock_ore_rad_arc_heavy"), item("hbm_m:bedrock_ore_rad_arc_light"), item("hbm_m:bedrock_ore_rad_arc_nonmetal"), item("hbm_m:bedrock_ore_rad_arc_rare"), item("hbm_m:bedrock_ore_rad_byproduct_actinide"), item("hbm_m:bedrock_ore_rad_byproduct_crystal"), item("hbm_m:bedrock_ore_rad_byproduct_heavy"), item("hbm_m:bedrock_ore_rad_byproduct_light"), item("hbm_m:bedrock_ore_rad_byproduct_nonmetal"), item("hbm_m:bedrock_ore_rad_byproduct_rare"), item("hbm_m:bedrock_ore_rad_roasted_actinide"), item("hbm_m:bedrock_ore_rad_roasted_crystal"), item("hbm_m:bedrock_ore_rad_roasted_heavy"), item("hbm_m:bedrock_ore_rad_roasted_light"), item("hbm_m:bedrock_ore_rad_roasted_nonmetal"), item("hbm_m:bedrock_ore_rad_roasted_rare"), item("hbm_m:bedrock_ore_rad_washed_actinide"), item("hbm_m:bedrock_ore_rad_washed_crystal"), item("hbm_m:bedrock_ore_rad_washed_heavy"), item("hbm_m:bedrock_ore_rad_washed_light"), item("hbm_m:bedrock_ore_rad_washed_nonmetal"), item("hbm_m:bedrock_ore_rad_washed_rare"), item("hbm_m:bedrock_ore_solvent_arc_actinide"), item("hbm_m:bedrock_ore_solvent_arc_crystal"), item("hbm_m:bedrock_ore_solvent_arc_heavy"), item("hbm_m:bedrock_ore_solvent_arc_light"), item("hbm_m:bedrock_ore_solvent_arc_nonmetal"), item("hbm_m:bedrock_ore_solvent_arc_rare"), item("hbm_m:bedrock_ore_solvent_byproduct_actinide"), item("hbm_m:bedrock_ore_solvent_byproduct_crystal"), item("hbm_m:bedrock_ore_solvent_byproduct_heavy"), item("hbm_m:bedrock_ore_solvent_byproduct_light"), item("hbm_m:bedrock_ore_solvent_byproduct_nonmetal"), item("hbm_m:bedrock_ore_solvent_byproduct_rare"), item("hbm_m:bedrock_ore_solvent_roasted_actinide"), item("hbm_m:bedrock_ore_solvent_roasted_crystal"), item("hbm_m:bedrock_ore_solvent_roasted_heavy"), item("hbm_m:bedrock_ore_solvent_roasted_light"), item("hbm_m:bedrock_ore_solvent_roasted_nonmetal"), item("hbm_m:bedrock_ore_solvent_roasted_rare"), item("hbm_m:bedrock_ore_solvent_washed_actinide"), item("hbm_m:bedrock_ore_solvent_washed_crystal"), item("hbm_m:bedrock_ore_solvent_washed_heavy"), item("hbm_m:bedrock_ore_solvent_washed_light"), item("hbm_m:bedrock_ore_solvent_washed_nonmetal"), item("hbm_m:bedrock_ore_solvent_washed_rare"), item("hbm_m:bedrock_ore_sulfuric_arc_actinide"), item("hbm_m:bedrock_ore_sulfuric_arc_crystal"), item("hbm_m:bedrock_ore_sulfuric_arc_heavy"), item("hbm_m:bedrock_ore_sulfuric_arc_light"), item("hbm_m:bedrock_ore_sulfuric_arc_nonmetal"), item("hbm_m:bedrock_ore_sulfuric_arc_rare"), item("hbm_m:bedrock_ore_sulfuric_byproduct_actinide"), item("hbm_m:bedrock_ore_sulfuric_byproduct_crystal"), item("hbm_m:bedrock_ore_sulfuric_byproduct_heavy"), item("hbm_m:bedrock_ore_sulfuric_byproduct_light"), item("hbm_m:bedrock_ore_sulfuric_byproduct_nonmetal"), item("hbm_m:bedrock_ore_sulfuric_byproduct_rare"), item("hbm_m:bedrock_ore_sulfuric_roasted_actinide"), item("hbm_m:bedrock_ore_sulfuric_roasted_crystal"), item("hbm_m:bedrock_ore_sulfuric_roasted_heavy"), item("hbm_m:bedrock_ore_sulfuric_roasted_light"), item("hbm_m:bedrock_ore_sulfuric_roasted_nonmetal"), item("hbm_m:bedrock_ore_sulfuric_roasted_rare"), item("hbm_m:bedrock_ore_sulfuric_washed_actinide"), item("hbm_m:bedrock_ore_sulfuric_washed_crystal"), item("hbm_m:bedrock_ore_sulfuric_washed_heavy"), item("hbm_m:bedrock_ore_sulfuric_washed_light"), item("hbm_m:bedrock_ore_sulfuric_washed_nonmetal"), item("hbm_m:bedrock_ore_sulfuric_washed_rare")), stack("minecraft:gravel", 1)).save(w, "orig_machines/shredder/gravel_8");
        // ShredderRecipes:298
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:dirt_dead")), stack("hbm_m:scrap_oil", 1)).save(w, "orig_machines/shredder/scrap_oil_1");
        // ShredderRecipes:299
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:dirt_oily")), stack("hbm_m:scrap_oil", 1)).save(w, "orig_machines/shredder/scrap_oil_2");
        // ShredderRecipes:300
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:sand_dirty")), stack("hbm_m:scrap_oil", 1)).save(w, "orig_machines/shredder/scrap_oil_3");
        // ShredderRecipes:301
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:sand_dirty_red")), stack("hbm_m:scrap_oil", 1)).save(w, "orig_machines/shredder/scrap_oil_4");
        // ShredderRecipes:302
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:stone_cracked")), stack("hbm_m:scrap_oil", 1)).save(w, "orig_machines/shredder/scrap_oil_5");
        // ShredderRecipes:303
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:stone_porous")), stack("hbm_m:scrap_oil", 1)).save(w, "orig_machines/shredder/scrap_oil_6");
        // ShredderRecipes:344
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:debris_concrete")), stack("hbm_m:scrap_nuclear", 2)).save(w, "orig_machines/shredder/scrap_nuclear_1");
        // ShredderRecipes:345
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:debris_shrapnel")), stack("hbm_m:steel_powder_tiny", 5)).save(w, "orig_machines/shredder/steel_powder_tiny_5");
        // ShredderRecipes:346
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:debris_exchanger")), stack("hbm_m:steel_powder", 3)).save(w, "orig_machines/shredder/steel_powder_3");
        // ShredderRecipes:347
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:debris_element")), stack("hbm_m:scrap_nuclear", 4)).save(w, "orig_machines/shredder/scrap_nuclear_2");
        // ShredderRecipes:348
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:debris_metal")), stack("hbm_m:steel_powder_tiny", 3)).save(w, "orig_machines/shredder/steel_powder_tiny_6");
        // ShredderRecipes:349
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(item("hbm_m:debris_graphite")), stack("hbm_m:coal_powder", 1)).save(w, "orig_machines/shredder/coal_powder_3");
    }

    private static void crystallizerRecipes(Consumer<FinishedRecipe> w) {
        // CrystallizerRecipes:72
        CrystallizerRecipeBuilder.crystallizerRecipe(ore("oredict/ore/saltpeter"), 1, fl(ModFluids.PEROXIDE, 500), stack("hbm_m:crystal_niter", 1), 600, 0.05F).save(w, "orig_machines/crystallizer/crystal_niter_1");
        // CrystallizerRecipes:83
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("hbm_m:calcium_powder")), 1, fl(ModFluids.REDMUD, 75), stack("hbm_m:cement_powder", 8), 100, 0.1F).save(w, "orig_machines/crystallizer/cement_powder_1");
        // CrystallizerRecipes:86
        CrystallizerRecipeBuilder.crystallizerRecipe(ore("oredict/ore/rare_earth"), 1, fl(ModFluids.SULFURIC_ACID, 500), stack("hbm_m:crystal_rare", 1), 600, 0.05F).save(w, "orig_machines/crystallizer/crystal_rare_1");
        // CrystallizerRecipes:89
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("hbm_m:nether_fire_ore")), 1, fl(ModFluids.PEROXIDE, 500), stack("hbm_m:crystal_phosphorus", 1), 600, 0.05F).save(w, "orig_machines/crystallizer/crystal_phosphorus_1");
        // CrystallizerRecipes:90
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("hbm_m:tikite_ore")), 1, fl(ModFluids.SULFURIC_ACID, 500), stack("hbm_m:crystal_trixite", 1), 600, 0.05F).save(w, "orig_machines/crystallizer/crystal_trixite_1");
        // CrystallizerRecipes:91
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("hbm_m:gravel_diamond")), 1, fl(ModFluids.PEROXIDE, 500), stack("hbm_m:crystal_diamond", 1), 600, 0.05F).save(w, "orig_machines/crystallizer/crystal_diamond_1");
        // CrystallizerRecipes:92
        CrystallizerRecipeBuilder.crystallizerRecipe(ore("oredict/ingot/schraranium"), 1, fl(ModFluids.PEROXIDE, 500), stack("hbm_m:crystal_schraranium", 1), 600, 0.05F).save(w, "orig_machines/crystallizer/crystal_schraranium_1");
        // CrystallizerRecipes:94
        CrystallizerRecipeBuilder.crystallizerRecipe(ore("oredict/sand"), 1, fl(ModFluids.PEROXIDE, 500), stack("hbm_m:fiberglass_ingot", 1), 100, 0.15F).save(w, "orig_machines/crystallizer/fiberglass_ingot_1");
        // CrystallizerRecipes:95
        CrystallizerRecipeBuilder.crystallizerRecipe(ore("oredict/ingot/silicon"), 1, fl(ModFluids.OXYGEN, 250), stack("minecraft:quartz", 2), 100, 0.1F).save(w, "orig_machines/crystallizer/quartz_1");
        // CrystallizerRecipes:96
        CrystallizerRecipeBuilder.crystallizerRecipe(ore("oredict/block/redstone"), 1, fl(ModFluids.PEROXIDE, 500), stack("hbm_m:nugget_mercury", 1), 600, 0.25F).save(w, "orig_machines/crystallizer/nugget_mercury_1");
        // CrystallizerRecipes:97
        CrystallizerRecipeBuilder.crystallizerRecipe(ore("oredict/crystal/cinnabar"), 1, fl(ModFluids.PEROXIDE, 500), stack("hbm_m:nugget_mercury", 3), 600, 0.25F).save(w, "orig_machines/crystallizer/nugget_mercury_2");
        // CrystallizerRecipes:98
        CrystallizerRecipeBuilder.crystallizerRecipe(ore("oredict/dust/borax"), 1, fl(ModFluids.SULFURIC_ACID, 500), stack("hbm_m:boron_powder_tiny", 3), 600, 0.25F).save(w, "orig_machines/crystallizer/boron_powder_tiny_1");
        // CrystallizerRecipes:99
        CrystallizerRecipeBuilder.crystallizerRecipe(ore("oredict/block/coal"), 1, fl(ModFluids.PEROXIDE, 500), stack("hbm_m:block_graphite", 1), 600, 0.0F).save(w, "orig_machines/crystallizer/block_graphite_1");
        // CrystallizerRecipes:102
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("hbm_m:gravel_obsidian")), 1, fl(ModFluids.PEROXIDE, 500), stack("hbm_m:brick_obsidian", 1), 100, 0.0F).save(w, "orig_machines/crystallizer/brick_obsidian_1");
        // CrystallizerRecipes:104
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("hbm_m:coal_infernal")), 1, fl(ModFluids.PEROXIDE, 500), stack("hbm_m:solid_fuel", 1), 100, 0.0F).save(w, "orig_machines/crystallizer/solid_fuel_1");
        // CrystallizerRecipes:105
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("hbm_m:stone_gneiss")), 1, fl(ModFluids.PEROXIDE, 500), stack("hbm_m:lithium_powder", 1), 100, 0.25F).save(w, "orig_machines/crystallizer/lithium_powder_1");
        // CrystallizerRecipes:106
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("minecraft:bone_meal")), 1, fl(ModFluids.SULFURIC_ACID, 250), stack("minecraft:slime_ball", 4), 20, 0.0F).save(w, "orig_machines/crystallizer/slime_ball_1");
        // CrystallizerRecipes:108
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("hbm_m:plant_item_mustardwillow")), 10, fl(ModFluids.RADIOSOLVENT, 250), stack("hbm_m:cadmium_powder", 1), 100, 0.0F).save(w, "orig_machines/crystallizer/cadmium_powder_1");
        // CrystallizerRecipes:109
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("hbm_m:scrap_oil")), 16, fl(ModFluids.RADIOSOLVENT, 100), stack("hbm_m:nugget_arsenic", 1), 100, 0.3F).save(w, "orig_machines/crystallizer/nugget_arsenic_1");
        // CrystallizerRecipes:110
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("hbm_m:fullerene")), 4, fl(ModFluids.XYLENE, 1000), stack("hbm_m:cft_ingot", 1), 600, 0.1F).save(w, "orig_machines/crystallizer/cft_ingot_1");
        // CrystallizerRecipes:112
        CrystallizerRecipeBuilder.crystallizerRecipe(ore("oredict/dust/diamond"), 1, fl(ModFluids.PEROXIDE, 500), stack("minecraft:diamond", 1), 100, 0.0F).save(w, "orig_machines/crystallizer/diamond_1");
        // CrystallizerRecipes:113
        CrystallizerRecipeBuilder.crystallizerRecipe(ore("oredict/dust/emerald"), 1, fl(ModFluids.PEROXIDE, 500), stack("minecraft:emerald", 1), 100, 0.0F).save(w, "orig_machines/crystallizer/emerald_1");
        // CrystallizerRecipes:114
        CrystallizerRecipeBuilder.crystallizerRecipe(ore("oredict/dust/lapis"), 1, fl(ModFluids.PEROXIDE, 500), stack("minecraft:lapis_lazuli", 1), 100, 0.0F).save(w, "orig_machines/crystallizer/lapis_lazuli_1");
        // CrystallizerRecipes:115
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("hbm_m:semtex_mix_powder")), 1, fl(ModFluids.PEROXIDE, 500), stack("hbm_m:semtex_ingot", 1), 600, 0.0F).save(w, "orig_machines/crystallizer/semtex_ingot_1");
        // CrystallizerRecipes:116
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("hbm_m:desh_ready_powder")), 1, fl(ModFluids.PEROXIDE, 500), stack("hbm_m:desh_ingot", 1), 600, 0.0F).save(w, "orig_machines/crystallizer/desh_ingot_1");
        // CrystallizerRecipes:117
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("hbm_m:meteorite_powder")), 1, fl(ModFluids.PEROXIDE, 500), stack("hbm_m:fragment_meteorite", 1), 100, 0.0F).save(w, "orig_machines/crystallizer/fragment_meteorite_1");
        // CrystallizerRecipes:118
        CrystallizerRecipeBuilder.crystallizerRecipe(ore("oredict/dust/cadmium"), 1, fl(ModFluids.FISHOIL, 4000), stack("hbm_m:rubber_ingot", 16), 100, 0.0F).save(w, "orig_machines/crystallizer/rubber_ingot_1");
        // CrystallizerRecipes:119
        CrystallizerRecipeBuilder.crystallizerRecipe(ore("oredict/ingot/latex"), 1, fl(ModFluids.SOURGAS, 25), stack("hbm_m:rubber_ingot", 1), 20, 0.15F).save(w, "orig_machines/crystallizer/rubber_ingot_2");
        // CrystallizerRecipes:120
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("hbm_m:sawdust_powder")), 1, fl(ModFluids.NITROGLYCERIN, 250), stack("hbm_m:cordite", 1), 20, 0.25F).save(w, "orig_machines/crystallizer/cordite_1");
        // CrystallizerRecipes:124
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("hbm_m:powder_impure_osmiridium")), 1, fl(ModFluids.SCHRABIDIC, 1000), stack("hbm_m:crystal_osmiridium", 1), 600, 0.0F).save(w, "orig_machines/crystallizer/crystal_osmiridium_1");
        // CrystallizerRecipes:212
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("hbm_m:oil_tar_crude")), 1, fl(ModFluids.CHLORINE, 250), stack("hbm_m:oil_tar_wax", 1), 20, 0.0F).save(w, "orig_machines/crystallizer/oil_tar_wax_1");
        // CrystallizerRecipes:213
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("hbm_m:oil_tar_crack")), 1, fl(ModFluids.CHLORINE, 100), stack("hbm_m:oil_tar_wax", 1), 20, 0.0F).save(w, "orig_machines/crystallizer/oil_tar_wax_2");
        // CrystallizerRecipes:214
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("hbm_m:oil_tar_paraffin")), 1, fl(ModFluids.CHLORINE, 100), stack("hbm_m:oil_tar_wax", 1), 20, 0.0F).save(w, "orig_machines/crystallizer/oil_tar_wax_3");
        // CrystallizerRecipes:215
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("hbm_m:oil_tar_wax")), 1, fl(ModFluids.IONGEL, 500), stack("hbm_m:pellet_charged", 1), 200, 0.0F).save(w, "orig_machines/crystallizer/pellet_charged_1");
        // CrystallizerRecipes:216
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("hbm_m:oil_tar_paraffin")), 1, fl(ModFluids.ESTRADIOL, 250), stack("hbm_m:pill_red", 1), 200, 0.0F).save(w, "orig_machines/crystallizer/pill_red_1");
        // CrystallizerRecipes:218
        CrystallizerRecipeBuilder.crystallizerRecipe(ore("oredict/sand"), 1, fl(ModFluids.COLLOID, 1000), stack("minecraft:clay", 1), 20, 0.0F).save(w, "orig_machines/crystallizer/clay_1");
        // CrystallizerRecipes:219
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("hbm_m:sand_quartz")), 1, fl(ModFluids.NITROGLYCERIN, 1000), stack("hbm_m:ball_dynamite", 16), 20, 0.0F).save(w, "orig_machines/crystallizer/ball_dynamite_1");
        // CrystallizerRecipes:220
        CrystallizerRecipeBuilder.crystallizerRecipe(ore("oredict/dust/nether_quartz"), 1, fl(ModFluids.NITROGLYCERIN, 250), stack("hbm_m:ball_dynamite", 4), 20, 0.0F).save(w, "orig_machines/crystallizer/ball_dynamite_2");
        // CrystallizerRecipes:242
        CrystallizerRecipeBuilder.crystallizerRecipe(Ingredient.of(item("hbm_m:moon_turf")), 16, fl(ModFluids.PEROXIDE, 500), stack("hbm_m:moonstone", 1), 1200, 0.0F).save(w, "orig_machines/crystallizer/moonstone_1");
    }

    private static void smeltingRecipes(Consumer<FinishedRecipe> w) {
        // SmeltingRecipes:28
        smelting(w, Ingredient.of(item("hbm_m:glyphid_meat")), stack("hbm_m:glyphid_meat_grilled", 1), 1.0F, "orig_machines/smelting/glyphid_meat_grilled_1");
        // SmeltingRecipes:32
        smelting(w, Ingredient.of(item("hbm_m:ore_uranium_scorched")), stack("hbm_m:uranium_ingot", 1), 6.0F, "orig_machines/smelting/uranium_ingot_1");
        // SmeltingRecipes:33
        smelting(w, Ingredient.of(item("hbm_m:nether_uranium_ore")), stack("hbm_m:uranium_ingot", 1), 12.0F, "orig_machines/smelting/uranium_ingot_2");
        // SmeltingRecipes:34
        smelting(w, Ingredient.of(item("hbm_m:ore_nether_uranium_scorched")), stack("hbm_m:uranium_ingot", 1), 12.0F, "orig_machines/smelting/uranium_ingot_3");
        // SmeltingRecipes:35
        smelting(w, Ingredient.of(item("hbm_m:ore_nether_plutonium")), stack("hbm_m:plutonium_ingot", 1), 24.0F, "orig_machines/smelting/plutonium_ingot_1");
        // SmeltingRecipes:37
        smelting(w, Ingredient.of(item("hbm_m:ore_copper")), stack("minecraft:copper_ingot", 1), 2.5F, "orig_machines/smelting/copper_ingot_1");
        // SmeltingRecipes:39
        smelting(w, Ingredient.of(item("hbm_m:ore_nether_tungsten")), stack("hbm_m:tungsten_ingot", 1), 12.0F, "orig_machines/smelting/tungsten_ingot_1");
        // SmeltingRecipes:40
        smelting(w, Ingredient.of(item("hbm_m:ore_aluminium")), stack("hbm_m:cryolite_chunk", 1), 2.5F, "orig_machines/smelting/cryolite_chunk_1");
        // SmeltingRecipes:43
        smelting(w, Ingredient.of(item("hbm_m:schrabidium_ore")), stack("hbm_m:schrabidium_ingot", 1), 128.0F, "orig_machines/smelting/schrabidium_ingot_1");
        // SmeltingRecipes:44
        smelting(w, Ingredient.of(item("hbm_m:schrabidium_ore_nether")), stack("hbm_m:schrabidium_ingot", 1), 256.0F, "orig_machines/smelting/schrabidium_ingot_2");
        // SmeltingRecipes:46
        smelting(w, Ingredient.of(item("hbm_m:nether_cobalt_ore")), stack("hbm_m:cobalt_ingot", 1), 2.0F, "orig_machines/smelting/cobalt_ingot_1");
        // SmeltingRecipes:54
        smelting(w, Ingredient.of(item("hbm_m:gneiss_iron_ore")), stack("minecraft:iron_ingot", 1), 5.0F, "orig_machines/smelting/iron_ingot_1");
        // SmeltingRecipes:55
        smelting(w, Ingredient.of(item("hbm_m:gneiss_gold_ore")), stack("minecraft:gold_ingot", 1), 5.0F, "orig_machines/smelting/gold_ingot_1");
        // SmeltingRecipes:56
        smelting(w, Ingredient.of(item("hbm_m:gneiss_uranium_ore")), stack("hbm_m:uranium_ingot", 1), 12.0F, "orig_machines/smelting/uranium_ingot_4");
        // SmeltingRecipes:57
        smelting(w, Ingredient.of(item("hbm_m:ore_gneiss_uranium_scorched")), stack("hbm_m:uranium_ingot", 1), 12.0F, "orig_machines/smelting/uranium_ingot_5");
        // SmeltingRecipes:58
        smelting(w, Ingredient.of(item("hbm_m:gneiss_copper_ore")), stack("minecraft:copper_ingot", 1), 5F, "orig_machines/smelting/copper_ingot_2");
        // SmeltingRecipes:59
        smelting(w, Ingredient.of(item("hbm_m:gneiss_lithium_ore")), stack("hbm_m:lithium", 1), 10F, "orig_machines/smelting/lithium_1");
        // SmeltingRecipes:60
        smelting(w, Ingredient.of(item("hbm_m:schrabidium_ore_gneiss")), stack("hbm_m:schrabidium_ingot", 1), 256.0F, "orig_machines/smelting/schrabidium_ingot_3");
        // SmeltingRecipes:62
        smelting(w, Ingredient.of(item("hbm_m:ore_australium")), stack("hbm_m:nugget_australium", 1), 2.5F, "orig_machines/smelting/nugget_australium_1");
        // SmeltingRecipes:65
        smelting(w, Ingredient.of(item("hbm_m:coal_briquette")), stack("hbm_m:coal_coke", 1), 1.0F, "orig_machines/smelting/coal_coke_1");
        // SmeltingRecipes:66
        smelting(w, Ingredient.of(item("hbm_m:lignite_briquette")), stack("hbm_m:lignite_coke", 1), 1.0F, "orig_machines/smelting/lignite_coke_1");
        // SmeltingRecipes:67
        smelting(w, Ingredient.of(item("hbm_m:sawdust_briquette")), stack("minecraft:charcoal", 1), 1.0F, "orig_machines/smelting/charcoal_1");
        // SmeltingRecipes:77
        smelting(w, Ingredient.of(item("hbm_m:copper_powder")), stack("minecraft:copper_ingot", 1), 1.0F, "orig_machines/smelting/copper_ingot_3");
        // SmeltingRecipes:90
        smelting(w, Ingredient.of(item("hbm_m:lithium_powder")), stack("hbm_m:lithium", 1), 1.0F, "orig_machines/smelting/lithium_2");
        // SmeltingRecipes:101
        smelting(w, Ingredient.of(item("hbm_m:powder_tcalloy")), stack("hbm_m:tcalloy_ingot", 1), 1.0F, "orig_machines/smelting/tcalloy_ingot_1");
        // SmeltingRecipes:110
        smelting(w, Ingredient.of(item("hbm_m:ball_resin")), stack("hbm_m:biorubber_ingot", 1), 0.1F, "orig_machines/smelting/biorubber_ingot_1");
        // SmeltingRecipes:112
        smelting(w, Ingredient.of(item("hbm_m:arc_electrode_burnt_graphite")), stack("hbm_m:graphite_ingot", 1), 3.0F, "orig_machines/smelting/graphite_ingot_1");
        // SmeltingRecipes:113
        smelting(w, Ingredient.of(item("hbm_m:arc_electrode_burnt_lanthanium")), stack("hbm_m:lanthanium_ingot", 1), 3.0F, "orig_machines/smelting/lanthanium_ingot_1");
        // SmeltingRecipes:114
        smelting(w, Ingredient.of(item("hbm_m:arc_electrode_burnt_desh")), stack("hbm_m:desh_ingot", 1), 3.0F, "orig_machines/smelting/desh_ingot_1");
        // SmeltingRecipes:115
        smelting(w, Ingredient.of(item("hbm_m:arc_electrode_burnt_saturnite")), stack("hbm_m:saturnite_ingot", 1), 3.0F, "orig_machines/smelting/saturnite_ingot_1");
        // SmeltingRecipes:117
        smelting(w, Ingredient.of(item("hbm_m:combine_scrap")), stack("hbm_m:combine_steel_ingot", 1), 1.0F, "orig_machines/smelting/combine_steel_ingot_1");
        // SmeltingRecipes:118
        smelting(w, Ingredient.of(item("hbm_m:rag_damp")), stack("hbm_m:rag", 1), 0.1F, "orig_machines/smelting/rag_1");
        // SmeltingRecipes:119
        smelting(w, Ingredient.of(item("hbm_m:rag_piss")), stack("hbm_m:rag", 1), 0.1F, "orig_machines/smelting/rag_2");
        // SmeltingRecipes:120
        smelting(w, Ingredient.of(item("hbm_m:plant_flower_tobacco")), stack("hbm_m:plant_item_tobacco", 1), 0.1F, "orig_machines/smelting/plant_item_tobacco_1");
        // SmeltingRecipes:121
        smelting(w, Ingredient.of(item("hbm_m:ball_fireclay")), stack("hbm_m:firebrick", 1), 0.1F, "orig_machines/smelting/firebrick_1");
        // SmeltingRecipes:126
        smelting(w, Ingredient.of(item("hbm_m:gravel_obsidian")), stack("minecraft:obsidian", 1), 0.0F, "orig_machines/smelting/obsidian_1");
        // SmeltingRecipes:127
        smelting(w, Ingredient.of(item("hbm_m:gravel_diamond")), stack("minecraft:diamond", 1), 3.0F, "orig_machines/smelting/diamond_1");
        // SmeltingRecipes:128
        smelting(w, Ingredient.of(item("hbm_m:sand_uranium")), stack("hbm_m:glass_uranium", 1), 0.25F, "orig_machines/smelting/glass_uranium_1");
        // SmeltingRecipes:129
        smelting(w, Ingredient.of(item("hbm_m:sand_polonium")), stack("hbm_m:glass_polonium", 1), 0.75F, "orig_machines/smelting/glass_polonium_1");
        // SmeltingRecipes:130
        smelting(w, Ingredient.of(item("hbm_m:waste_trinitite")), stack("hbm_m:glass_trinitite", 1), 0.25F, "orig_machines/smelting/glass_trinitite_1");
        // SmeltingRecipes:131
        smelting(w, Ingredient.of(item("hbm_m:waste_trinitite_red")), stack("hbm_m:glass_trinitite", 1), 0.25F, "orig_machines/smelting/glass_trinitite_2");
        // SmeltingRecipes:132
        smelting(w, Ingredient.of(item("hbm_m:sand_boron")), stack("hbm_m:glass_boron", 1), 0.25F, "orig_machines/smelting/glass_boron_1");
        // SmeltingRecipes:133
        smelting(w, Ingredient.of(item("hbm_m:sand_lead")), stack("hbm_m:glass_lead", 1), 0.25F, "orig_machines/smelting/glass_lead_1");
        // SmeltingRecipes:134
        smelting(w, Ingredient.of(item("hbm_m:ash_digamma")), stack("hbm_m:glass_ash", 1), 10F, "orig_machines/smelting/glass_ash_1");
        // SmeltingRecipes:135
        smelting(w, Ingredient.of(item("hbm_m:basalt")), stack("hbm_m:basalt_smooth", 1), 0.1F, "orig_machines/smelting/basalt_smooth_1");
        // SmeltingRecipes:137
        smelting(w, Ingredient.of(item("hbm_m:schraranium_ingot")), stack("hbm_m:nugget_schrabidium", 1), 2.0F, "orig_machines/smelting/nugget_schrabidium_1");
        // SmeltingRecipes:139
        smelting(w, Ingredient.of(item("hbm_m:lodestone")), stack("hbm_m:crystal_iron", 1), 5.0F, "orig_machines/smelting/crystal_iron_1");
        // SmeltingRecipes:140
        smelting(w, Ingredient.of(item("hbm_m:crystal_iron")), stack("minecraft:iron_ingot", 2), 2.0F, "orig_machines/smelting/iron_ingot_2");
        // SmeltingRecipes:141
        smelting(w, Ingredient.of(item("hbm_m:crystal_gold")), stack("minecraft:gold_ingot", 2), 2.0F, "orig_machines/smelting/gold_ingot_2");
        // SmeltingRecipes:142
        smelting(w, Ingredient.of(item("hbm_m:crystal_redstone")), stack("minecraft:redstone", 6), 2.0F, "orig_machines/smelting/redstone_1");
        // SmeltingRecipes:143
        smelting(w, Ingredient.of(item("hbm_m:crystal_diamond")), stack("minecraft:diamond", 2), 2.0F, "orig_machines/smelting/diamond_2");
        // SmeltingRecipes:144
        smelting(w, Ingredient.of(item("hbm_m:crystal_uranium")), stack("hbm_m:uranium_ingot", 2), 2.0F, "orig_machines/smelting/uranium_ingot_6");
        // SmeltingRecipes:145
        smelting(w, Ingredient.of(item("hbm_m:crystal_thorium")), stack("hbm_m:th232_ingot", 2), 2.0F, "orig_machines/smelting/th232_ingot_1");
        // SmeltingRecipes:146
        smelting(w, Ingredient.of(item("hbm_m:crystal_plutonium")), stack("hbm_m:plutonium_ingot", 2), 2.0F, "orig_machines/smelting/plutonium_ingot_2");
        // SmeltingRecipes:147
        smelting(w, Ingredient.of(item("hbm_m:crystal_titanium")), stack("hbm_m:titanium_ingot", 2), 2.0F, "orig_machines/smelting/titanium_ingot_1");
        // SmeltingRecipes:148
        smelting(w, Ingredient.of(item("hbm_m:crystal_sulfur")), stack("hbm_m:sulfur", 6), 2.0F, "orig_machines/smelting/sulfur_1");
        // SmeltingRecipes:149
        smelting(w, Ingredient.of(item("hbm_m:crystal_niter")), stack("hbm_m:niter", 6), 2.0F, "orig_machines/smelting/niter_1");
        // SmeltingRecipes:150
        smelting(w, Ingredient.of(item("hbm_m:crystal_copper")), stack("minecraft:copper_ingot", 2), 2.0F, "orig_machines/smelting/copper_ingot_4");
        // SmeltingRecipes:151
        smelting(w, Ingredient.of(item("hbm_m:crystal_tungsten")), stack("hbm_m:tungsten_ingot", 2), 2.0F, "orig_machines/smelting/tungsten_ingot_2");
        // SmeltingRecipes:152
        smelting(w, Ingredient.of(item("hbm_m:crystal_aluminium")), stack("hbm_m:ingot_aluminium", 2), 2.0F, "orig_machines/smelting/ingot_aluminium_1");
        // SmeltingRecipes:153
        smelting(w, Ingredient.of(item("hbm_m:crystal_fluorite")), stack("hbm_m:fluorite", 6), 2.0F, "orig_machines/smelting/fluorite_1");
        // SmeltingRecipes:154
        smelting(w, Ingredient.of(item("hbm_m:crystal_beryllium")), stack("hbm_m:beryllium_ingot", 2), 2.0F, "orig_machines/smelting/beryllium_ingot_1");
        // SmeltingRecipes:155
        smelting(w, Ingredient.of(item("hbm_m:crystal_lead")), stack("hbm_m:lead_ingot", 2), 2.0F, "orig_machines/smelting/lead_ingot_1");
        // SmeltingRecipes:156
        smelting(w, Ingredient.of(item("hbm_m:crystal_schraranium")), stack("hbm_m:nugget_schrabidium", 2), 2.0F, "orig_machines/smelting/nugget_schrabidium_2");
        // SmeltingRecipes:157
        smelting(w, Ingredient.of(item("hbm_m:crystal_schrabidium")), stack("hbm_m:schrabidium_ingot", 2), 2.0F, "orig_machines/smelting/schrabidium_ingot_4");
        // SmeltingRecipes:158
        smelting(w, Ingredient.of(item("hbm_m:crystal_rare")), stack("hbm_m:powder_desh_mix", 1), 2.0F, "orig_machines/smelting/powder_desh_mix_1");
        // SmeltingRecipes:159
        smelting(w, Ingredient.of(item("hbm_m:crystal_phosphorus")), stack("hbm_m:fire_powder", 6), 2.0F, "orig_machines/smelting/fire_powder_1");
        // SmeltingRecipes:160
        smelting(w, Ingredient.of(item("hbm_m:crystal_lithium")), stack("hbm_m:lithium", 2), 2.0F, "orig_machines/smelting/lithium_3");
        // SmeltingRecipes:161
        smelting(w, Ingredient.of(item("hbm_m:crystal_cobalt")), stack("hbm_m:cobalt_ingot", 2), 2.0F, "orig_machines/smelting/cobalt_ingot_2");
        // SmeltingRecipes:162
        smelting(w, Ingredient.of(item("hbm_m:crystal_starmetal")), stack("hbm_m:starmetal_ingot", 2), 2.0F, "orig_machines/smelting/starmetal_ingot_1");
        // SmeltingRecipes:163
        smelting(w, Ingredient.of(item("hbm_m:crystal_trixite")), stack("hbm_m:plutonium_ingot", 4), 2.0F, "orig_machines/smelting/plutonium_ingot_3");
        // SmeltingRecipes:164
        smelting(w, Ingredient.of(item("hbm_m:crystal_cinnebar")), stack("hbm_m:cinnebar", 4), 2.0F, "orig_machines/smelting/cinnebar_1");
        // SmeltingRecipes:165
        smelting(w, Ingredient.of(item("hbm_m:crystal_osmiridium")), stack("hbm_m:osmiridium_ingot", 1), 2.0F, "orig_machines/smelting/osmiridium_ingot_1");
        // SmeltingRecipes:173
        smelting(w, Ingredient.of(item("hbm_m:scrap_plastic"), item("hbm_m:scrap_plastic_board_converter"), item("hbm_m:scrap_plastic_board_transistor"), item("hbm_m:scrap_plastic_bridge_bios"), item("hbm_m:scrap_plastic_bridge_bus"), item("hbm_m:scrap_plastic_bridge_chipset"), item("hbm_m:scrap_plastic_bridge_cmos"), item("hbm_m:scrap_plastic_bridge_io"), item("hbm_m:scrap_plastic_bridge_north"), item("hbm_m:scrap_plastic_bridge_south"), item("hbm_m:scrap_plastic_card_board"), item("hbm_m:scrap_plastic_card_processor"), item("hbm_m:scrap_plastic_cpu_cache"), item("hbm_m:scrap_plastic_cpu_clock"), item("hbm_m:scrap_plastic_cpu_ext"), item("hbm_m:scrap_plastic_cpu_logic"), item("hbm_m:scrap_plastic_cpu_register"), item("hbm_m:scrap_plastic_cpu_socket"), item("hbm_m:scrap_plastic_mem_16k_a"), item("hbm_m:scrap_plastic_mem_16k_b"), item("hbm_m:scrap_plastic_mem_16k_c"), item("hbm_m:scrap_plastic_mem_16k_d"), item("hbm_m:scrap_plastic_mem_socket")), stack("hbm_m:polymer_ingot", 1), 0.1F, "orig_machines/smelting/polymer_ingot_1");
    }

    private static void pressRecipes(Consumer<FinishedRecipe> w) {
        // PressRecipes:61
        PressRecipeBuilder.pressRecipe(stack("minecraft:quartz", 1)).stamp(Ingredient.of(oreTag("stamps/flat"))).material(ore("oredict/dust/nether_quartz")).save(w, "orig_machines/press/quartz_1");
        // PressRecipes:62
        PressRecipeBuilder.pressRecipe(stack("minecraft:lapis_lazuli", 1)).stamp(Ingredient.of(oreTag("stamps/flat"))).material(ore("oredict/dust/lapis")).save(w, "orig_machines/press/lapis_lazuli_1");
        // PressRecipes:63
        PressRecipeBuilder.pressRecipe(stack("minecraft:diamond", 1)).stamp(Ingredient.of(oreTag("stamps/flat"))).material(ore("oredict/dust/diamond")).save(w, "orig_machines/press/diamond_1");
        // PressRecipes:64
        PressRecipeBuilder.pressRecipe(stack("minecraft:emerald", 1)).stamp(Ingredient.of(oreTag("stamps/flat"))).material(ore("oredict/dust/emerald")).save(w, "orig_machines/press/emerald_1");
        // PressRecipes:66
        PressRecipeBuilder.pressRecipe(stack("hbm_m:graphite_ingot", 1)).stamp(Ingredient.of(oreTag("stamps/flat"))).material(ore("oredict/gem/any_coke")).save(w, "orig_machines/press/graphite_ingot_1");
        // PressRecipes:70
        PressRecipeBuilder.pressRecipe(stack("hbm_m:coal_briquette", 1)).stamp(Ingredient.of(oreTag("stamps/flat"))).material(ore("oredict/dust/coal")).save(w, "orig_machines/press/coal_briquette_1");
        // PressRecipes:71
        PressRecipeBuilder.pressRecipe(stack("hbm_m:lignite_briquette", 1)).stamp(Ingredient.of(oreTag("stamps/flat"))).material(ore("oredict/dust/lignite")).save(w, "orig_machines/press/lignite_briquette_1");
        // PressRecipes:72
        PressRecipeBuilder.pressRecipe(stack("hbm_m:sawdust_briquette", 1)).stamp(Ingredient.of(oreTag("stamps/flat"))).material(Ingredient.of(item("hbm_m:sawdust_powder"))).save(w, "orig_machines/press/sawdust_briquette_1");
        // PressRecipes:84
        PressRecipeBuilder.pressRecipe(stack("hbm_m:plate_weaponsteel", 1)).stamp(Ingredient.of(oreTag("stamps/plate"))).material(ore("oredict/ingot/weapon_steel")).save(w, "orig_machines/press/plate_weaponsteel_1");
        // PressRecipes:88
        PressRecipeBuilder.pressRecipe(stack("hbm_m:casing_small", 4)).stamp(Ingredient.of(item("hbm_m:stamp_9"), item("hbm_m:stamp_desh_9"))).material(ore("oredict/plate/gun_metal")).save(w, "orig_machines/press/casing_small_1");
        // PressRecipes:89
        PressRecipeBuilder.pressRecipe(stack("hbm_m:casing_large", 2)).stamp(Ingredient.of(item("hbm_m:stamp_50"), item("hbm_m:stamp_desh_50"))).material(ore("oredict/plate/gun_metal")).save(w, "orig_machines/press/casing_large_1");
        // PressRecipes:90
        PressRecipeBuilder.pressRecipe(stack("hbm_m:casing_small_steel", 4)).stamp(Ingredient.of(item("hbm_m:stamp_9"), item("hbm_m:stamp_desh_9"))).material(ore("oredict/plate/weapon_steel")).save(w, "orig_machines/press/casing_small_steel_1");
        // PressRecipes:91
        PressRecipeBuilder.pressRecipe(stack("hbm_m:casing_large_steel", 2)).stamp(Ingredient.of(item("hbm_m:stamp_50"), item("hbm_m:stamp_desh_50"))).material(ore("oredict/plate/weapon_steel")).save(w, "orig_machines/press/casing_large_steel_1");
    }

    private static void centrifugeRecipes(Consumer<FinishedRecipe> w) {
        // CentrifugeRecipes:48
        CentrifugeRecipeBuilder.recipe(Ingredient.of(item("hbm_m:rareground_ore_chunk")), stack("hbm_m:cobalt_powder_tiny", 2), stack("hbm_m:boron_powder_tiny", 2), stack("hbm_m:niobium_powder_tiny", 2), stack("hbm_m:nugget_zirconium", 3)).save(w, "orig_machines/centrifuge/cobalt_powder_tiny_1");
        // CentrifugeRecipes:60
        CentrifugeRecipeBuilder.recipe(ore("oredict/ore/lignite"), stack("hbm_m:lignite_powder", 2), stack("hbm_m:lignite_powder", 2), stack("hbm_m:lignite_powder", 2), stack("minecraft:gravel", 1)).save(w, "orig_machines/centrifuge/lignite_powder_1");
        // CentrifugeRecipes:96
        CentrifugeRecipeBuilder.recipe(ore("oredict/ore_nether/quartz"), stack("hbm_m:quartz_powder", 1), stack("hbm_m:quartz_powder", 1), stack("hbm_m:lithium_powder_tiny", 1), stack("minecraft:netherrack", 1)).save(w, "orig_machines/centrifuge/quartz_powder_1");
        // CentrifugeRecipes:126
        CentrifugeRecipeBuilder.recipe(ore("oredict/ore/schrabidium"), stack("hbm_m:schrabidium_powder", 1), stack("hbm_m:schrabidium_powder", 1), stack("hbm_m:nugget_solinium", 1), stack("minecraft:gravel", 1)).save(w, "orig_machines/centrifuge/schrabidium_powder_1");
        // CentrifugeRecipes:132
        CentrifugeRecipeBuilder.recipe(ore("oredict/ore/rare_earth"), stack("hbm_m:powder_desh_mix", 1), stack("hbm_m:nugget_zirconium", 1), stack("hbm_m:nugget_zirconium", 1), stack("minecraft:gravel", 1)).save(w, "orig_machines/centrifuge/powder_desh_mix_1");
        // CentrifugeRecipes:138
        CentrifugeRecipeBuilder.recipe(ore("oredict/ore/plutonium"), stack("hbm_m:plutonium_powder", 1), stack("hbm_m:plutonium_powder", 1), stack("hbm_m:nugget_polonium", 3), stack("minecraft:gravel", 1)).save(w, "orig_machines/centrifuge/plutonium_powder_1");
        // CentrifugeRecipes:144
        ConfigRecipes.variants(w)
                .base(b -> CentrifugeRecipeBuilder.recipe(ore("oredict/ore/uranium"), stack("hbm_m:uranium_powder", 1), stack("hbm_m:uranium_powder", 1), stack("hbm_m:nugget_ra226", 1), stack("minecraft:gravel", 1)).save(b, "orig_machines/centrifuge/uranium_powder_1"))
                .variant(v -> CentrifugeRecipeBuilder.recipe(ore("oredict/ore/uranium"), stack("hbm_m:uranium_powder", 2), stack("hbm_m:nugget_technetium", 2), stack("hbm_m:nugget_ra226", 2), stack("minecraft:gravel", 1)).save(v, "orig_machines/centrifuge/uranium_powder_1"), "lbsm_centrifuge")
                .save();
        // CentrifugeRecipes:156
        CentrifugeRecipeBuilder.recipe(ore("oredict/ore/beryllium"), stack("hbm_m:beryllium_powder", 1), stack("hbm_m:beryllium_powder", 1), stack("hbm_m:emerald_powder", 1), stack("minecraft:gravel", 1)).save(w, "orig_machines/centrifuge/beryllium_powder_1");
        // CentrifugeRecipes:162
        CentrifugeRecipeBuilder.recipe(ore("oredict/ore/fluorite"), stack("hbm_m:fluorite", 3), stack("hbm_m:fluorite", 3), stack("hbm_m:gem_sodalite", 1), stack("minecraft:gravel", 1)).save(w, "orig_machines/centrifuge/fluorite_1");
        // CentrifugeRecipes:168
        ConfigRecipes.variants(w)
                .base(b -> CentrifugeRecipeBuilder.recipe(ore("oredict/ore/redstone"), stack("minecraft:redstone", 3), stack("minecraft:redstone", 3), stack("hbm_m:nugget_mercury", 1), stack("minecraft:gravel", 1)).save(b, "orig_machines/centrifuge/redstone_1"))
                .variant(v -> CentrifugeRecipeBuilder.recipe(ore("oredict/ore/redstone"), stack("minecraft:redstone", 3), stack("minecraft:redstone", 3), stack("hbm_m:nugget_mercury", 3), stack("minecraft:gravel", 1)).save(v, "orig_machines/centrifuge/redstone_1"), "lbsm_centrifuge")
                .save();
        // CentrifugeRecipes:174
        CentrifugeRecipeBuilder.recipe(Ingredient.of(item("hbm_m:tikite_ore")), stack("hbm_m:plutonium_powder", 1), stack("hbm_m:cobalt_powder", 2), stack("hbm_m:niobium_powder", 2), stack("minecraft:end_stone", 1)).save(w, "orig_machines/centrifuge/plutonium_powder_2");
        // CentrifugeRecipes:180
        CentrifugeRecipeBuilder.recipe(ore("oredict/ore/lapis"), stack("hbm_m:lapis_powder", 6), stack("hbm_m:cobalt_powder_tiny", 1), stack("hbm_m:gem_sodalite", 1), stack("minecraft:gravel", 1)).save(w, "orig_machines/centrifuge/lapis_powder_1");
        // CentrifugeRecipes:186
        CentrifugeRecipeBuilder.recipe(Ingredient.of(item("hbm_m:block_euphemium_cluster")), stack("hbm_m:nugget_euphemium", 7), stack("hbm_m:schrabidium_powder", 4), stack("hbm_m:starmetal_ingot", 2), stack("hbm_m:nugget_solinium", 2)).save(w, "orig_machines/centrifuge/nugget_euphemium_1");
        // CentrifugeRecipes:192
        CentrifugeRecipeBuilder.recipe(Ingredient.of(item("hbm_m:nether_fire_ore")), stack("minecraft:blaze_powder", 2), stack("hbm_m:fire_powder", 2), stack("hbm_m:phosphorus_ingot", 1), stack("minecraft:netherrack", 1)).save(w, "orig_machines/centrifuge/blaze_powder_1");
        // CentrifugeRecipes:204
        CentrifugeRecipeBuilder.recipe(Ingredient.of(item("hbm_m:powder_tektite")), stack("hbm_m:meteorite_powder_tiny", 1), stack("hbm_m:paleogenite_powder_tiny", 1), stack("hbm_m:meteorite_powder_tiny", 1), stack("hbm_m:dust", 6)).save(w, "orig_machines/centrifuge/meteorite_powder_tiny_1");
        // CentrifugeRecipes:210
        CentrifugeRecipeBuilder.recipe(Ingredient.of(item("hbm_m:block_slag")), stack("minecraft:gravel", 1), stack("hbm_m:fire_powder", 1), stack("hbm_m:calcium_powder", 1), stack("hbm_m:dust", 1)).save(w, "orig_machines/centrifuge/gravel_1");
        // CentrifugeRecipes:216
        CentrifugeRecipeBuilder.recipe(Ingredient.of(item("hbm_m:ash_coal")), stack("hbm_m:coal_powder_tiny", 2), stack("hbm_m:boron_powder_tiny", 1), stack("hbm_m:dust_tiny", 6)).save(w, "orig_machines/centrifuge/coal_powder_tiny_1");
        // CentrifugeRecipes:307
        CentrifugeRecipeBuilder.recipe(Ingredient.of(item("hbm_m:crystal_gold")), stack("hbm_m:gold_powder", 2), stack("hbm_m:gold_powder", 2), stack("hbm_m:nugget_mercury", 1), stack("hbm_m:lithium_powder_tiny", 1)).save(w, "orig_machines/centrifuge/gold_powder_1");
        // CentrifugeRecipes:308
        CentrifugeRecipeBuilder.recipe(Ingredient.of(item("hbm_m:crystal_redstone")), stack("minecraft:redstone", 3), stack("minecraft:redstone", 3), stack("minecraft:redstone", 3), stack("hbm_m:nugget_mercury", 3)).save(w, "orig_machines/centrifuge/redstone_2");
        // CentrifugeRecipes:311
        CentrifugeRecipeBuilder.recipe(Ingredient.of(item("hbm_m:crystal_uranium")), stack("hbm_m:uranium_powder", 2), stack("hbm_m:uranium_powder", 2), stack("hbm_m:nugget_ra226", 2), stack("hbm_m:lithium_powder_tiny", 1)).save(w, "orig_machines/centrifuge/uranium_powder_2");
        // CentrifugeRecipes:312
        CentrifugeRecipeBuilder.recipe(Ingredient.of(item("hbm_m:crystal_thorium")), stack("hbm_m:thorium_powder", 2), stack("hbm_m:thorium_powder", 2), stack("hbm_m:uranium_powder", 1), stack("hbm_m:nugget_ra226", 1)).save(w, "orig_machines/centrifuge/thorium_powder_1");
        // CentrifugeRecipes:315
        CentrifugeRecipeBuilder.recipe(Ingredient.of(item("hbm_m:crystal_sulfur")), stack("hbm_m:sulfur", 4), stack("hbm_m:sulfur", 4), stack("hbm_m:iron_powder", 1), stack("hbm_m:nugget_mercury", 1)).save(w, "orig_machines/centrifuge/sulfur_1");
        // CentrifugeRecipes:316
        CentrifugeRecipeBuilder.recipe(Ingredient.of(item("hbm_m:crystal_niter")), stack("hbm_m:niter", 3), stack("hbm_m:niter", 3), stack("hbm_m:niter", 3), stack("hbm_m:lithium_powder_tiny", 1)).save(w, "orig_machines/centrifuge/niter_1");
        // CentrifugeRecipes:317
        CentrifugeRecipeBuilder.recipe(Ingredient.of(item("hbm_m:crystal_copper")), stack("hbm_m:copper_powder", 2), stack("hbm_m:copper_powder", 2), stack("hbm_m:sulfur", 1), stack("hbm_m:cobalt_powder_tiny", 1)).save(w, "orig_machines/centrifuge/copper_powder_1");
        // CentrifugeRecipes:323
        CentrifugeRecipeBuilder.recipe(Ingredient.of(item("hbm_m:crystal_schraranium")), stack("hbm_m:nugget_schrabidium", 2), stack("hbm_m:nugget_schrabidium", 2), stack("hbm_m:nugget_uranium", 2), stack("hbm_m:nugget_neptunium", 2)).save(w, "orig_machines/centrifuge/nugget_schrabidium_1");
        // CentrifugeRecipes:325
        CentrifugeRecipeBuilder.recipe(Ingredient.of(item("hbm_m:crystal_rare")), stack("hbm_m:powder_desh_mix", 1), stack("hbm_m:powder_desh_mix", 1), stack("hbm_m:nugget_zirconium", 2), stack("hbm_m:nugget_zirconium", 2)).save(w, "orig_machines/centrifuge/powder_desh_mix_2");
        // CentrifugeRecipes:326
        CentrifugeRecipeBuilder.recipe(Ingredient.of(item("hbm_m:crystal_phosphorus")), stack("hbm_m:fire_powder", 3), stack("hbm_m:fire_powder", 3), stack("hbm_m:phosphorus_ingot", 2), stack("minecraft:blaze_powder", 2)).save(w, "orig_machines/centrifuge/fire_powder_1");
        // CentrifugeRecipes:327
        CentrifugeRecipeBuilder.recipe(Ingredient.of(item("hbm_m:crystal_trixite")), stack("hbm_m:plutonium_powder", 2), stack("hbm_m:cobalt_powder", 3), stack("hbm_m:niobium_powder", 2), stack("hbm_m:powder_nitan_mix", 1)).save(w, "orig_machines/centrifuge/plutonium_powder_3");
        // CentrifugeRecipes:329
        CentrifugeRecipeBuilder.recipe(Ingredient.of(item("hbm_m:crystal_starmetal")), stack("hbm_m:dura_steel_powder", 3), stack("hbm_m:cobalt_powder", 3), stack("hbm_m:astatine_powder", 2), stack("hbm_m:nugget_mercury", 5)).save(w, "orig_machines/centrifuge/dura_steel_powder_1");
    }
}
//?}
