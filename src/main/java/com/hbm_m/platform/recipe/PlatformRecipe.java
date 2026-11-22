package com.hbm_m.platform.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

//? if < 1.21.1 {
import net.minecraft.world.Container;
import net.minecraft.core.RegistryAccess;
//?} else {
/*import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.core.HolderLookup;
*///?}

public abstract class PlatformRecipe implements net.minecraft.world.item.crafting.Recipe<
        //? if < 1.21.1 {
        Container//?} else {
        /*RecipeInput*///?}
        > {

    protected ResourceLocation id;

    public PlatformRecipe(ResourceLocation id) {
        this.id = id;
    }

    /**
     * Кросс-версионный доступ к id рецепта: в 1.21.1 ванильный {@code Recipe#getId()}
     * удалён (id живёт в RecipeHolder), поэтому общий код зовёт этот метод.
     *
     * <p>На 1.21.1 сериализатор декодирует рецепт с dummy-id ({@code minecraft:dummy});
     * реальный id инжектируется сюда при разворачивании {@code RecipeHolder}
     * ({@code RecipeHooks}/{@code ModRecipeIndex}) — только для {@code PlatformRecipe}.
     */
    public ResourceLocation getRecipeId() {
        return id;
    }

    /** Инъекция реального id из RecipeHolder (1.21.1). Публичный для ModRecipeIndex (др. пакет). */
    public void setRecipeId(ResourceLocation id) {
        // field is final on 1.20.1 semantics; on 1.21.1 the dummy id must be replaceable
        //? if >= 1.21.1 {
        /*this.id = id;
        *///?}
    }
    public abstract boolean matchesRecipe(RecipeInputWrapper input, Level level);
    public abstract ItemStack assembleSafe();
    public abstract ItemStack getResultItemSafe();

    //? if < 1.21.1 {
    
    @Override
    public boolean matches(@NotNull Container container, @NotNull Level level) {
        return matchesRecipe(new RecipeInputWrapper(container), level);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container container, @NotNull RegistryAccess registryAccess) {
        return assembleSafe();
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return getResultItemSafe();
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }
    //?} else {
    /*@Override
    public boolean matches(@NotNull RecipeInput input, @NotNull Level level) {
        return matchesRecipe(new RecipeInputWrapper(input), level);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull RecipeInput input, @NotNull HolderLookup.Provider provider) {
        return assembleSafe();
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull HolderLookup.Provider provider) {
        return getResultItemSafe();
    }
    *///?}

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }
}