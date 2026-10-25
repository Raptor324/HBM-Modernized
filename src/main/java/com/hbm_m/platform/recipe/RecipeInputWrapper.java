package com.hbm_m.platform.recipe;

import net.minecraft.world.item.ItemStack;

//? if < 1.21.1 {
import net.minecraft.world.Container;
//?} else {
/*import net.minecraft.world.item.crafting.RecipeInput;
*///?}

/**
 * Кросс-версионный «снимок» инвентаря для матчинга рецептов.
 *
 * <p>На 1.20.1 оборачивает {@link net.minecraft.world.Container Container} (живая ссылка).
 * На 1.21.1 хранит живую ссылку на {@link RecipeInput} (копия массива на каждый
 * {@code matches()} аллоцировала сотни массивов за тик при поиске по рецептам);
 * {@code SimpleContainer} адаптируется лёгким анонимным {@code RecipeInput}-вью.
 * Матчинг рецептов только читает слоты, поэтому живые ссылки безопасны.
 */
public class RecipeInputWrapper {
    //? if < 1.21.1 {

    private final Container container;
    public RecipeInputWrapper(Container container) { this.container = container; }
    public ItemStack getItem(int slot) { return container.getItem(slot); }
    public int size() { return container.getContainerSize(); }
    //?} else {
    /*private final RecipeInput input;

    public RecipeInputWrapper(RecipeInput input) { this.input = input; }

    public RecipeInputWrapper(net.minecraft.world.SimpleContainer container) {
        this.input = new RecipeInput() {
            @Override public ItemStack getItem(int slot) { return container.getItem(slot); }
            @Override public int size() { return container.getContainerSize(); }
        };
    }

    public ItemStack getItem(int slot) { return this.input.getItem(slot); }
    public int size() { return this.input.size(); }
    *///?}
}
