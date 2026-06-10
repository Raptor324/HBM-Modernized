package com.hbm_m.recipe;

import javax.annotation.Nullable;

import dev.architectury.fluid.FluidStack;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Рецепт рудного окислителя (Crystallizer).
 *
 * <p>В оригинальном HBM это класс {@code CrystallizerRecipes.CrystallizerRecipe}.
 * В новой версии используем стандартные для 1.20.1 типы: Ingredient для входа
 * (поддерживает теги, конкретные предметы и комбинации) и FluidStack для кислоты.</p>
 *
 * <p>Поля:</p>
 * <ul>
 *   <li>{@code input} — что должен принимать слот 0 (например, Tags.Items.ORES_IRON).</li>
 *   <li>{@code inputCount} — сколько штук тратится за крафт (1 для большинства, 4-16 для редких).</li>
 *   <li>{@code acid} — какая кислота и сколько mB (например, 500 mB sulfuric_acid).
 *       Если {@code null} — рецепт работает с любой кислотой в баке (старое поведение
 *       для рецептов без указания типа в оригинале).</li>
 *   <li>{@code output} — что выдаётся в слот 2.</li>
 *   <li>{@code duration} — сколько тиков занимает (600 = 30 сек, базовое).</li>
 *   <li>{@code productivity} — шанс не потратить вход [0..1]. Применяется по уровню апгрейда EFFECT.</li>
 * </ul>
 */
public class CrystallizerRecipe {

    private final Ingredient input;
    private final int inputCount;
    @Nullable
    private final FluidStack acid;
    private final ItemStack output;
    private final int duration;
    private final float productivity;

    public CrystallizerRecipe(Ingredient input, int inputCount, @Nullable FluidStack acid,
                              ItemStack output, int duration, float productivity) {
        this.input = input;
        this.inputCount = Math.max(1, inputCount);
        this.acid = acid;
        this.output = output;
        this.duration = Math.max(1, duration);
        this.productivity = Math.max(0f, Math.min(1f, productivity));
    }

    public Ingredient getInput() { return input; }
    public int getInputCount() { return inputCount; }
    @Nullable
    public FluidStack getAcid() { return acid; }
    public ItemStack getOutput() { return output; }
    public int getDuration() { return duration; }
    public float getProductivity() { return productivity; }

    public int getAcidAmount() {
        return acid == null ? 0 : (int) Math.min(Integer.MAX_VALUE, acid.getAmount());
    }

    /**
     * Проверяет совпадает ли вход рецепта со стэком в слоте.
     * Не проверяет количество — это делается отдельно в machine, чтобы понять
     * хватит ли его на крафт.
     */
    public boolean matchesInput(ItemStack stack) {
        return input.test(stack);
    }

    /**
     * Проверяет совпадает ли требуемая кислота с тем, что в баке.
     * Если рецепт не требует конкретной кислоты — возвращает true.
     */
    public boolean matchesAcid(FluidStack tankFluid) {
        if (acid == null) return true;
        if (tankFluid.isEmpty()) return false;
        return tankFluid.getFluid() == acid.getFluid();
    }
}
