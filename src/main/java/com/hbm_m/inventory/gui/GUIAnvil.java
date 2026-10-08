package com.hbm_m.inventory.gui;

import com.hbm_m.client.GuiCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import com.hbm_m.inventory.menu.AnvilMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.AnvilCraftC2SPacket;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.recipe.AnvilRecipe;
import com.hbm_m.recipe.AnvilRecipe.AStack;
import com.hbm_m.recipe.AnvilRecipeManager;
import com.hbm_m.platform.recipe.RecipeHooks;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code GUIAnvil}: Schmiede-Slots oben, darunter die Konstruktionsliste (2 Zeilen, 5 Spalten, blaettern per
 * Pfeil oder Mausrad), Suchfeld, Herstellknopf (Shift = so oft wie moeglich) und die ausklappende Zutatenliste rechts.
 */
public class GUIAnvil extends AbstractContainerScreen<AnvilMenu> {

    //? if fabric && < 1.21.1 {
    /*private static final ResourceLocation TEXTURE = new ResourceLocation(
            RefStrings.MODID, "textures/gui/processing/gui_anvil.png");
    *///?} else {
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/processing/gui_anvil.png");
    //?}

    private final int tier;
    private final List<AnvilRecipe> originList = new ArrayList<>();
    private final List<AnvilRecipe> recipes = new ArrayList<>();
    int index;
    int size;
    int selection;
    private EditBox search;
    private final Inventory playerInventory;
    int lastSize = 1;
    private ItemStack hoveredRecipeStack = ItemStack.EMPTY;

    public GUIAnvil(AnvilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.tier = menu.tier.getLegacyId();
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.playerInventory = inventory;

        if (inventory.player.level() != null) {
            for (AnvilRecipe recipe : AnvilRecipeManager.getConstruction(inventory.player.level())) {
                if (recipe.canCraftOn(menu.tier)) this.originList.add(recipe);
            }
        }

        regenerateRecipes();
    }

    @Override
    protected void init() {
        super.init();

        this.search = new EditBox(this.font, leftPos + 10, topPos + 111, 84, 12, Component.empty());
        this.search.setTextColor(-1);
        this.search.setTextColorUneditable(-1);
        this.search.setBordered(false);
        this.search.setMaxLength(25);
        this.addWidget(this.search);
    }

    /** Von JEI aus: Rezept in der Liste anwaehlen ({@code focusRecipe}). */
    public void focusRecipe(AnvilRecipe target) {
        if (target == null || !this.originList.contains(target)) return;
        search.setValue("");
        regenerateRecipes();

        int pos = this.recipes.indexOf(target);
        if (pos < 0) return;

        this.selection = pos;
        this.index = Mth.clamp(pos / 2, 0, this.size);
    }

    private void regenerateRecipes() {
        this.recipes.clear();
        this.recipes.addAll(this.originList);
        resetPaging();
    }

    private void search(String search) {
        search = search.toLowerCase(Locale.US);
        this.recipes.clear();

        if (search.isEmpty()) {
            this.recipes.addAll(this.originList);
        } else {
            for (AnvilRecipe recipe : this.originList) {
                List<String> list = recipeToSearchList(recipe);
                for (String s : list) {
                    if (s.contains(search)) {
                        this.recipes.add(recipe);
                        break;
                    }
                }
            }
        }

        resetPaging();
    }

    private void resetPaging() {
        this.index = 0;
        this.selection = -1;
        this.size = Math.max(0, (int) Math.ceil((this.recipes.size() - 10) / 2D));
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private boolean in(double x, double y, int bx, int by, int w, int h) {
        return leftPos + bx <= x && leftPos + bx + w > x && topPos + by < y && topPos + by + h >= y;
    }

    //? if < 1.21.1 {
    @Override
    public boolean mouseScrolled(double x, double y, double scroll) {
    //?} else {
    /*@Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scroll) {
    *///?}
        if (leftPos <= x && leftPos + imageWidth > x && topPos < y && topPos + imageHeight >= y && this.hoveredSlot == null) {
            if (scroll > 0 && this.index > 0) this.index--;
            if (scroll < 0 && this.index < this.size) this.index++;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double x, double y, int k) {
        boolean handled = super.mouseClicked(x, y, k);

        this.search.setFocused(this.search.isMouseOver(x, y));
        if (this.search.isFocused()) {
            this.search.mouseClicked(x, y, k);
            this.setFocused(this.search);
        }

        if (in(x, y, 7, 71, 9, 36)) {
            click();
            if (this.index > 0) this.index--;
            return true;
        }

        if (in(x, y, 106, 71, 9, 36)) {
            click();
            if (this.index < this.size) this.index++;
            return true;
        }

        if (in(x, y, 52, 53, 18, 18)) {
            if (this.selection == -1) return true;

            click();
            AnvilRecipe recipe = this.recipes.get(this.selection);
            ResourceLocation id = RecipeHooks.recipeId(this.minecraft.level.getRecipeManager(), AnvilRecipe.Type.INSTANCE, recipe);
            ModPacketHandler.sendToServer(ModPacketHandler.ANVIL_CRAFT,
                    new AnvilCraftC2SPacket(id, GLFW.glfwGetKey(this.minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ? 1 : 0));
            return true;
        }

        for (int i = index * 2; i < index * 2 + 10; i++) {
            if (i >= this.recipes.size()) break;

            int ind = i - index * 2;
            int ix = 16 + 18 * (ind / 2);
            int iy = 71 + 18 * (ind % 2);
            if (in(x, y, ix, iy, 18, 18)) {
                if (this.selection != i) this.selection = i;
                else this.selection = -1;

                click();
                return true;
            }
        }

        return handled || this.search.isFocused();
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mX, int mY) {
        Component name = Component.translatable("container.hbm_m.anvil", tier);
        g.drawString(this.font, name, 61 - this.font.width(name) / 2, 8, 4210752, false);
        g.drawString(this.font, Component.translatable("container.inventory"), 8, this.imageHeight - 96 + 2, 4210752, false);

        if (this.selection >= 0) {
            AnvilRecipe recipe = recipes.get(this.selection);
            List<String> list = recipeToList(recipe, playerInventory);
            int longest = 0;

            for (String s : list) {
                int length = this.font.width(s);
                if (length > longest) longest = length;
            }

            float scale = 0.5F;
            g.pose().pushPose();
            g.pose().scale(scale, scale, 1F);
            int offset = 0;
            for (String s : list) {
                g.drawString(this.font, s, 260, 50 + offset, 0xffffff, false);
                offset += 9;
            }
            g.pose().popPose();

            this.lastSize = (int) (longest * scale);
        } else {
            this.lastSize = 0;
        }
    }

    /** Die gegliederte Zutatenliste; rot, was im Inventar fehlt. */
    public List<String> recipeToList(AnvilRecipe recipe, Inventory inventory) {
        List<String> list = new ArrayList<>();

        list.add(ChatFormatting.YELLOW + Component.translatable("gui.hbm_m.anvil.inputs").getString());

        for (AStack stack : recipe.getInputs()) {
            List<ItemStack> ores = stack.displayStacks();
            if (ores.isEmpty()) {
                list.add("I AM ERROR");
                continue;
            }
            boolean hasItem = false;
            int amount = 0;
            for (ItemStack stackItem : inventory.items) {
                if (stackItem.isEmpty()) continue;
                if (stack.matchesIgnoreSize(stackItem)) {
                    hasItem = true;
                    amount += stackItem.getCount();
                }
            }
            ItemStack inStack = stack.comparable() ? ores.get(0) : ores.get((int) (Math.abs(System.currentTimeMillis() / 1000) % ores.size()));
            String line = ">" + stack.count() + "x " + inStack.getHoverName().getString();
            list.add(hasItem && amount >= stack.count() ? line : ChatFormatting.RED + line);
        }

        list.add("");
        list.add(ChatFormatting.YELLOW + Component.translatable("gui.hbm_m.anvil.outputs").getString());

        for (AnvilRecipe.ResultEntry stack : recipe.getOutputs()) {
            list.add(">" + stack.stack().getCount() + "x " + stack.stack().getHoverName().getString()
                    + (stack.chance() != 1F ? (" (" + (stack.chance() * 100) + "%)") : ""));
        }

        return list;
    }

    /** Ungegliederte Suchbegriffe: Zutaten (alle OreDict-Varianten) und Ausgaben. */
    public List<String> recipeToSearchList(AnvilRecipe recipe) {
        List<String> list = new ArrayList<>();

        for (AStack stack : recipe.getInputs()) {
            if (stack.comparable()) {
                try { list.add(stack.firstStack().getHoverName().getString().toLowerCase(Locale.US)); } catch (Exception ex) { list.add("I AM ERROR"); }
            } else {
                for (ItemStack ore : stack.displayStacks()) {
                    try { list.add(ore.getHoverName().getString().toLowerCase(Locale.US)); } catch (Exception ex) { list.add("I AM ERROR"); }
                }
            }
        }

        for (AnvilRecipe.ResultEntry stack : recipe.getOutputs()) {
            list.add(stack.stack().getHoverName().getString().toLowerCase(Locale.US));
        }

        return list;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float inter, int mX, int mY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, this.imageWidth, this.imageHeight);

        int slide = Mth.clamp(this.lastSize - 42, 0, 1000);

        int mul = 1;
        while (true) {
            if (slide >= 51 * mul) {
                g.blit(TEXTURE, leftPos + 125 + 51 * mul, topPos + 17, 125, 17, 54, 108);
                mul++;
            } else {
                break;
            }
        }

        g.blit(TEXTURE, leftPos + 125 + slide, topPos + 17, 125, 17, 54, 108);

        if (this.search.isFocused()) {
            g.blit(TEXTURE, leftPos + 8, topPos + 108, 168, 222, 88, 16);
        }

        if (in(mX, mY, 7, 71, 9, 36)) g.blit(TEXTURE, leftPos + 7, topPos + 71, 176, 186, 9, 36);
        if (in(mX, mY, 106, 71, 9, 36)) g.blit(TEXTURE, leftPos + 106, topPos + 71, 185, 186, 9, 36);
        if (in(mX, mY, 52, 53, 18, 18)) g.blit(TEXTURE, leftPos + 52, topPos + 53, 176, 150, 18, 18);

        hoveredRecipeStack = ItemStack.EMPTY;
        for (int i = index * 2; i < index * 2 + 10; i++) {
            if (i >= recipes.size()) break;

            int ind = i - index * 2;
            AnvilRecipe recipe = recipes.get(i);
            ItemStack display = recipe.getDisplayStack();
            int ix = leftPos + 17 + 18 * (ind / 2);
            int iy = topPos + 72 + 18 * (ind % 2);

            g.renderItem(display, ix, iy);
            g.renderItemDecorations(this.font, display, ix, iy);

            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            g.blit(TEXTURE, ix - 1, iy - 1, 18 + 18 * recipe.getOverlay().ordinal(), 222, 18, 18);
            if (selection == i) g.blit(TEXTURE, ix - 1, iy - 1, 0, 222, 18, 18);
            g.pose().popPose();

            if (mX >= ix - 1 && mX < ix + 17 && mY >= iy - 1 && mY < iy + 17) hoveredRecipeStack = display;
        }

        this.search.render(g, mX, mY, inter);
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float delta) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, delta);
        super.render(g, mouseX, mouseY, delta);
        this.renderTooltip(g, mouseX, mouseY);
        if (!hoveredRecipeStack.isEmpty() && this.hoveredSlot == null) {
            g.renderTooltip(this.font, hoveredRecipeStack, mouseX, mouseY);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        //? if < 1.21.1 {
        this.search.tick();
        //?}
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (this.search.isFocused() && this.search.charTyped(c, modifiers)) {
            search(this.search.getValue());
            return true;
        }
        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (this.search.isFocused() && key != GLFW.GLFW_KEY_ESCAPE) {
            String before = this.search.getValue();
            this.search.keyPressed(key, scan, modifiers);
            if (!before.equals(this.search.getValue())) search(this.search.getValue());
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }
}
