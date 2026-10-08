package com.hbm_m.recipe;

import com.google.gson.JsonObject;
import com.hbm_m.block.machines.MachineMassStorageBlock;
import com.hbm_m.block.machines.crates.BaseCrateBlock;
import com.hbm_m.item.crates.CrateItem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;

/**
 * 1:1-Port von {@code com.hbm.crafting.handlers.ContainerUpgradeCraftingHandler}: ein geformtes Rezept, dessen
 * Ergebnis die NBT-Daten der ersten Kiste ({@code BlockStorageCrate}) bzw. des ersten Massenspeichers im Gitter
 * uebernimmt - der Inhalt geht also mit (Original: Tresor aus der Stahlkiste).
 *
 * <p>Das Muster selbst ist ein gewoehnliches {@code crafting_shaped}-JSON, nur mit dem Typ
 * {@code hbm_m:container_upgrade}. Original-Hinweis zum Tresor: "voids the last few slots when placed, because a
 * safe's inventory is smaller than a crate's one" - im Port wird das Inventar gleich beim Herstellen auf die
 * Platzzahl des Ergebnisses gekuerzt, weil der Port-Handler sonst beim Laden auf die alte Groesse waechst.</p>
 */
public class ContainerUpgradeRecipe extends ShapedRecipe {

    //? if < 1.21.1 {
    public ContainerUpgradeRecipe(ShapedRecipe base) {
        super(base.getId(), base.getGroup(), base.category(), base.getWidth(), base.getHeight(), base.getIngredients(),
                base.getResultItem(null), base.showNotification());
    }

    @Override
    public ItemStack assemble(net.minecraft.world.inventory.CraftingContainer inv, net.minecraft.core.RegistryAccess registries) {
        ItemStack result = super.assemble(inv, registries);
        ItemStack source = firstContainer(inv);
        if (source == null) return result;
        result.setTag(source.getTag() == null ? null : source.getTag().copy());
        trim(result);
        return result;
    }

    /** Original {@code getFirstContainer}: Gitter zeilenweise, erste Kiste oder erster Massenspeicher. */
    private static ItemStack firstContainer(net.minecraft.world.inventory.CraftingContainer inv) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            Block block = Block.byItem(stack.getItem());
            if (block instanceof BaseCrateBlock || block instanceof MachineMassStorageBlock) return stack;
        }
        return null;
    }
    //?}

    /** Kisteninventar ({@code BlockEntityTag.inventory}) auf die Platzzahl des Ergebnisses kuerzen. */
    private static void trim(ItemStack result) {
        if (!(result.getItem() instanceof CrateItem crate)) return;
        CompoundTag root = result.getTag();
        if (root == null || !root.contains("BlockEntityTag", Tag.TAG_COMPOUND)) return;
        CompoundTag be = root.getCompound("BlockEntityTag");
        if (!be.contains("inventory", Tag.TAG_COMPOUND)) return;
        CompoundTag inventory = be.getCompound("inventory");
        int size = crate.getTotalSlots();
        if (inventory.contains("Size", Tag.TAG_INT) && inventory.getInt("Size") > size) inventory.putInt("Size", size);
        if (inventory.contains("Items", Tag.TAG_LIST)) {
            ListTag items = inventory.getList("Items", Tag.TAG_COMPOUND);
            ListTag kept = new ListTag();
            for (int i = 0; i < items.size(); i++) {
                CompoundTag entry = items.getCompound(i);
                if (entry.getInt("Slot") < size) kept.add(entry);
            }
            inventory.put("Items", kept);
        }
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CONTAINER_UPGRADE.get();
    }

    /** Liest/schreibt wie {@code crafting_shaped}. */
    public static class Serializer implements RecipeSerializer<ContainerUpgradeRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        //? if < 1.21.1 {
        @Override
        public ContainerUpgradeRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new ContainerUpgradeRecipe(RecipeSerializer.SHAPED_RECIPE.fromJson(id, json));
        }

        @Override
        public ContainerUpgradeRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new ContainerUpgradeRecipe(RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ContainerUpgradeRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, recipe);
        }
        //?}
    }
}
