package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.lib.RefStrings;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * Schreibt die Item-Tags {@code hbm_m:oredict/...} aus {@link OreDictTagData}: das Gegenstueck zu den
 * Ore-Dictionary-Schluesseln des Originals, damit Rezeptzutaten wie {@code STEEL.plateCast()} oder
 * {@code ANY_PLASTIC.ingot()} genau dieselben Gegenstaende annehmen. Eintraege mit {@code #} sind Tags,
 * mit Endung {@code ?} optional (Forge-Konventionstags anderer Mods).
 */
public class OreDictTagProvider extends ItemTagsProvider {

    public OreDictTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                              CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper helper) {
        super(output, lookup, blockTags, RefStrings.MODID, helper);
    }

    @Override
    protected void addTags(@NotNull HolderLookup.Provider provider) {
        java.util.Set<String> missing = new java.util.TreeSet<>();
        for (Map.Entry<String, List<String>> e : OreDictTagData.TAGS.entrySet()) {
            TagAppender<Item> tag = this.tag(OreDictIngredients.oreTag(e.getKey()));
            for (String raw : e.getValue()) {
                boolean optional = raw.endsWith("?");
                String v = optional ? raw.substring(0, raw.length() - 1) : raw;
                if (v.startsWith("#")) {
                    ResourceLocation rl = ResourceLocation.tryParse(v.substring(1));
                    if (optional || !rl.getNamespace().equals(RefStrings.MODID)) tag.addOptionalTag(rl);
                    else tag.addTag(TagKey.create(Registries.ITEM, rl));
                } else {
                    ResourceLocation rl = ResourceLocation.tryParse(v);
                    if (optional) tag.addOptional(rl);
                    else if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(rl)) missing.add(e.getKey() + " -> " + v);
                    else tag.add(ResourceKey.create(Registries.ITEM, rl));
                }
            }
        }
        if (!missing.isEmpty()) throw new IllegalStateException("OreDict-Tags: unbekannte Gegenstaende " + missing);
    }

    @Override
    public @NotNull String getName() {
        return "OreDict-Tags (hbm_m:oredict)";
    }
}
//?}
