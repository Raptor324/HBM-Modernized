package com.hbm_m.compat.jei;

import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.material.NTMMaterial;
import com.hbm_m.item.material.ItemScraps;
import com.hbm_m.item.material.ScrapItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * Расплавленный материал — не предмет и не жидкость, JEI-ингредиента для него нет.
 * Рисуем цветной квадрат из {@code MaterialType.color} и подпись с количеством
 * (порт {@code Mats.formatAmount}: блоки/слитки/самородки/кванты),
 * как это делает сам тигель в своём GUI.
 */
public final class JeiMoltenRendering {

    private JeiMoltenRendering() {}

    public static final int SWATCH = 16;

    /**
     * Расплав как субъект JEI: ItemStack жидкого лома ({@code ItemScraps.create(ms, true)}) —
     * именно так NEI-хендлеры тигля ({@code CrucibleSmeltingHandler}, {@code CrucibleAlloyingHandler},
     * {@code CrucibleCastingHandler}) показывали расплавленные материалы: иконка предмета лома,
     * количество — в тултипе ({@code ScrapItem#appendHbmTooltip}).
     *
     * <p>Мост {@code MaterialType} → {@link NTMMaterial} идёт по оригинальному числовому id
     * ({@link Mats#matById}; id совпадают 1:1 — {@code Mats.java:140} против {@code MaterialType.java}).
     * Для материалов без представления в Mats (alloy, nickel) возвращает {@link ItemStack#EMPTY} —
     * категория вместо предмета рисует цветной свотч ({@link #drawSwatch}).</p>
     */
    public static ItemStack liquidScrap(MaterialStack stack) {
        if (stack == null || stack.type == null) return ItemStack.EMPTY;
        NTMMaterial mat = Mats.matById.get(stack.type.id);
        if (mat == null) return ItemStack.EMPTY;
        return ItemScraps.create(new Mats.MaterialStack(mat, stack.amount), true);
    }

    public static void drawSwatch(GuiGraphics graphics, int x, int y, MaterialStack stack) {
        int argb = 0xFF000000 | (stack.type.color & 0xFFFFFF);
        graphics.fill(x, y, x + SWATCH, y + SWATCH, argb);
        graphics.renderOutline(x - 1, y - 1, SWATCH + 2, SWATCH + 2, 0xFF373737);
    }

    /** Подпись «1 ingot Steel» (quant-разбивка {@code Mats.formatAmount}) рядом со свотчем. */
    public static void drawLabel(GuiGraphics graphics, int x, int y, MaterialStack stack) {
        String text = ScrapItem.formatAmount(stack.amount, false).getString() + " " + stack.type.name;
        graphics.drawString(Minecraft.getInstance().font, text, x, y, 0x404040, false);
    }
}
