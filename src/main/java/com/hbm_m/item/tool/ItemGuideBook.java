package com.hbm_m.item.tool;

import com.hbm_m.platform.StackNbt;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.tool.ItemGuideBook} ({@code book_guide}): die Buchart steht statt in der Metadaten im
 * NBT-Wert "type" (Ordinalzahl von {@link BookType}, ohne Wert 0 = TEST). Rechtsklick oeffnet
 * {@code GUIScreenGuide}. Brennt 200 Ticks (FuelHandler).
 */
public class ItemGuideBook extends Item implements ITooltipProvider {

    public ItemGuideBook(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static ItemStack make(BookType type) {
        ItemStack stack = new ItemStack(ModItems.BOOK_GUIDE.get());
        StackNbt.orCreate(stack).putInt("type", type.ordinal());
        return stack;
    }

    public static BookType getType(ItemStack stack) {
        return BookType.getType(StackNbt.has(stack) ? StackNbt.read(stack).getInt("type") : 0);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (world.isClientSide)
            com.hbm_m.inventory.gui.GUIScreenGuide.open(getType(stack));

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
        return 200;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal(String.join(" ", Component.translatable(getType(stack).title).getString().split("\\$"))));
    }

    /** Die Eintraege, die das Original im Kreativtab zeigt (alle ausser TEST und HADRON). */
    public static List<ItemStack> getSubItems() {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 1; i < BookType.values().length; i++)
            if (i != 2) list.add(make(BookType.values()[i]));
        return list;
    }

    public enum BookType {

        TEST("book.test.cover", 2F, statFacTest()),
        RBMK("book.rbmk.cover", 1.5F, statFacRBMK()),
        HADRON("book.error.cover", 1.5F, statFacHadron()),
        STARTER("book.starter.cover", 1.5F, statFacStarter());

        public List<GuidePage> pages;
        public float titleScale;
        public String title;

        private BookType(String title, float titleScale, List<GuidePage> pages) {
            this.title = title;
            this.titleScale = titleScale;
            this.pages = pages;
        }

        public static BookType getType(int i) {
            return BookType.values()[Math.abs(i) % BookType.values().length];
        }
    }

    private static ResourceLocation tex(String path) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/book/" + path + ".png");
    }

    /** Die Gegenstandsbilder des Originals (textures/items/...) liegen hier unter gui/book/items. */
    private static ResourceLocation item(String name) {
        return tex("items/" + name);
    }

    public static List<GuidePage> statFacTest() {

        List<GuidePage> pages = new ArrayList<>();

        pages.add(new GuidePage().addTitle("Title LMAO", 0x800000, 1F)
                .addText("book.test.page1", 2F)
                .addImage(tex("smileman"), 100, 40, 40));
        pages.add(new GuidePage().addTitle("LA SEXO", 0x800000, 0.5F)
                .addText("book.test.page1", 1.75F)
                .addImage(tex("smileman"), 100, 40, 40));
        pages.add(new GuidePage().addText("test test"));
        pages.add(new GuidePage().addText("test test test"));
        pages.add(new GuidePage().addText("test test"));
        pages.add(new GuidePage().addText("test test test"));
        pages.add(new GuidePage().addText("test test"));

        return pages;
    }

    public static List<GuidePage> statFacRBMK() {

        List<GuidePage> pages = new ArrayList<>();
        pages.add(new GuidePage().addTitle("book.rbmk.title1", 0x800000, 1F)
                .addText("book.rbmk.page1", 2F)
                .addImage(tex("rbmk1"), 90, 80, 60));
        pages.add(new GuidePage().addTitle("book.rbmk.title2", 0x800000, 1F)
                .addText("book.rbmk.page2", 2F)
                .addImage(tex("rbmk2"), 95, 52, 52));
        pages.add(new GuidePage().addTitle("book.rbmk.title3", 0x800000, 1F)
                .addText("book.rbmk.page3", 2F)
                .addImage(tex("rbmk3"), 95, 88, 52));
        pages.add(new GuidePage().addTitle("book.rbmk.title4", 0x800000, 1F)
                .addText("book.rbmk.page4", 2F)
                .addImage(tex("rbmk4"), 95, 88, 52));
        pages.add(new GuidePage().addTitle("book.rbmk.title5", 0x800000, 1F)
                .addText("book.rbmk.page5", 2F)
                .addImage(tex("rbmk5"), 95, 80, 42));
        pages.add(new GuidePage().addTitle("book.rbmk.title6", 0x800000, 1F)
                .addText("book.rbmk.page6", 2F)
                .addImage(tex("rbmk6"), 90, 100, 60));
        pages.add(new GuidePage().addTitle("book.rbmk.title7", 0x800000, 1F)
                .addText("book.rbmk.page7", 2F)
                .addImage(tex("rbmk7"), 95, 52, 52));
        pages.add(new GuidePage().addTitle("book.rbmk.title8", 0x800000, 1F)
                .addText("book.rbmk.page8", 2F)
                .addImage(tex("rbmk8"), 95, 88, 52));
        pages.add(new GuidePage().addTitle("book.rbmk.title9", 0x800000, 1F)
                .addText("book.rbmk.page9", 2F)
                .addImage(tex("rbmk9"), 95, 88, 52));
        pages.add(new GuidePage().addTitle("book.rbmk.title10", 0x800000, 1F)
                .addText("book.rbmk.page10", 2F)
                .addImage(tex("rbmk10"), 95, 88, 52));
        pages.add(new GuidePage().addTitle("book.rbmk.title11", 0x800000, 1F)
                .addText("book.rbmk.page11", 2F)
                .addImage(tex("rbmk11"), 75, 85, 72));
        pages.add(new GuidePage().addTitle("book.rbmk.title12", 0x800000, 1F)
                .addText("book.rbmk.page12", 2F)
                .addImage(tex("rbmk12"), 90, 80, 60));
        pages.add(new GuidePage().addTitle("book.rbmk.title13", 0x800000, 1F)
                .addText("book.rbmk.page13", 2F));
        pages.add(new GuidePage()
                .addText("book.rbmk.page14", 2F)
                .addImage(tex("rbmk13"), 70, 103, 78));
        pages.add(new GuidePage().addTitle("book.rbmk.title15", 0x800000, 1F)
                .addText("book.rbmk.page15", 2F)
                .addImage(tex("rbmk15"), 100, 48, 48));
        pages.add(new GuidePage().addTitle("book.rbmk.title16", 0x800000, 1F)
                .addText("book.rbmk.page16", 2F)
                .addImage(tex("rbmk16"), 50, 70, 100));

        return pages;
    }

    public static List<GuidePage> statFacHadron() {

        List<GuidePage> pages = new ArrayList<>();

        for (int i = 1; i <= 9; i++) {
            pages.add(new GuidePage().addTitle("book.error.title" + i, 0x800000, 1F).addText("book.error.page" + i, 2F));
        }

        return pages;
    }

    public static List<GuidePage> statFacStarter() {

        List<GuidePage> pages = new ArrayList<>();

        pages.add(new GuidePage().addTitle("book.starter.title1", 0x800000, 1F)
                .addText("book.starter.page1", 2F)
                .addImage(tex("starter1"), 96, 101, 56));
        pages.add(new GuidePage().addTitle("book.starter.title2", 0x800000, 1F)
                .addText("book.starter.page2", 2F)
                .addImage(item("mask_piss"), 85, 64, 64));
        pages.add(new GuidePage().addTitle("book.starter.title3", 0x800000, 1F)
                .addText("book.starter.page3", 2F)
                .addImage(tex("starter3"), 89, 100, 64));
        pages.add(new GuidePage().addTitle("book.starter.title4", 0x800000, 1F)
                .addText("book.starter.page4", 1.4F, 0, 6, 72)
                .addImage(item("template_folder"), 72, 30, 24, 24)
                .addImage(item("stamp_iron_flat"), 72, 60, 24, 24)
                .addImage(item("assembly_template"), 72, 90, 24, 24)
                // fehlt auch im Original (Fehltextur)
                .addImage(item("chemistry_template"), 72, 120, 24, 24));
        pages.add(new GuidePage().addTitle("book.starter.title5", 0x800000, 1F)
                .addText("book.starter.page5", 2F));
        pages.add(new GuidePage().addTitle("book.starter.title6", 0x800000, 1F)
                .addText("book.starter.page6a", 2F)
                .addText("book.starter.page6b", 2f, 0, 96, 100)
                .addImage(tex("starter6"), 9, 89, 84, 36));
        pages.add(new GuidePage()
                .addText("book.starter.page7a", 2F)
                .addText("book.starter.page7b", 2F, 0, 95, 100)
                .addImage(tex("starter7"), 9, 67, 84, 58));
        pages.add(new GuidePage().addTitle("book.starter.title8", 0x800000, 1F)
                .addText("book.starter.page8a", 2F, 0, -1, 50)
                .addText("book.starter.page8b", 2F, 50, 70, 50)
                .addImage(tex("starter8a"), 53, 36, 47, 61)
                .addImage(tex("starter8b"), 0, 102, 47, 61));
        pages.add(new GuidePage().addTitle("book.starter.title9", 0x800000, 1F)
                .addText("book.starter.page9", 2F)
                .addImage(item("ingot_polymer"), 4, 106, 24, 24)
                .addImage(item("ingot_desh"), 28, 130, 24, 24)
                .addImage(item("solid_fuel_presto_triplet"), 52, 106, 24, 24)
                // fehlt auch im Original (Fehltextur)
                .addImage(item("canister_gasoline"), 76, 130, 24, 24));
        pages.add(new GuidePage().addTitle("book.starter.title10", 0x800000, 1F)
                .addText("book.starter.page10", 2F)
                .addImage(tex("starter10"), 0, 115, 100, 39));
        pages.add(new GuidePage().addTitle("book.starter.title11", 0x800000, 1F)
                .addText("book.starter.page11", 2F, 0, -1, 60)
                .addImage(tex("starter11a"), 61, 36, 45, 57)
                .addImage(tex("starter11b"), 61, 97, 45, 57));
        pages.add(new GuidePage().addTitle("book.starter.title12", 0xfece00, 1F)
                .addText("book.starter.page12a", 3F)
                .addText("book.starter.page12b", 2F, 0, 20, 100));
        pages.add(new GuidePage().addTitle("book.starter.title13", 0x800000, 1F)
                .addText("book.starter.page13", 2F)
                .addImage(tex("starter13"), 110, 84, 42));
        pages.add(new GuidePage().addTitle("book.starter.title14", 0x800000, 1F)
                .addText("book.starter.page14", 2F, 0, 54, 100)
                .addImage(tex("starter14"), 34, 100, 46));
        pages.add(new GuidePage().addTitle("book.starter.title15", 0x800000, 1F)
                .addText("book.starter.page15", 2F));
        pages.add(new GuidePage().addTitle("book.starter.title16", 0x800000, 1F)
                .addText("book.starter.page16", 2F));
        pages.add(new GuidePage());
        pages.add(new GuidePage().addTitle("book.starter.title18", 0x800000, 1F)
                .addText("book.starter.page18", 2F)
                .addImage(tex("starter18"), 10, 69, 100, 100));

        return pages;
    }

    public static class GuidePage {

        public String title;
        public int titleColor;
        public float titleScale;

        public List<GuideText> texts = new ArrayList<>();
        public List<GuideImage> images = new ArrayList<>();

        public GuidePage() { }

        public GuidePage addTitle(String title, int color, float scale) {
            this.title = title;
            this.titleColor = color;
            this.titleScale = scale;
            return this;
        }

        public GuidePage addText(String text) {
            texts.add(new GuideText(text));
            return this;
        }

        public GuidePage addText(String text, float scale) {
            texts.add(new GuideText(text).setScale(scale));
            return this;
        }

        public GuidePage addText(String text, int xOffset, int yOffset, int width) {
            texts.add(new GuideText(text).setSize(xOffset, yOffset, width));
            return this;
        }

        public GuidePage addText(String text, float scale, int xOffset, int yOffset, int width) {
            texts.add(new GuideText(text).setSize(xOffset, yOffset, width).setScale(scale));
            return this;
        }

        public GuidePage addImage(ResourceLocation image, int xOffset, int yOffset, int sizeX, int sizeY) {
            images.add(new GuideImage(image, xOffset, yOffset, sizeX, sizeY));
            return this;
        }

        //xOffset = -1 for automatic centering
        public GuidePage addImage(ResourceLocation image, int yOffset, int sizeX, int sizeY) {
            images.add(new GuideImage(image, -1, yOffset, sizeX, sizeY));
            return this;
        }
    }

    public static class GuideText {
        public String text;
        public float scale = 1F;
        public int xOffset = 0;
        public int yOffset = -1;
        public int width = 100;

        public GuideText(String text) {
            this.text = text;
        }

        public GuideText setScale(float scale) {
            this.scale = scale;
            return this;
        }

        //yOffset = -1, xOffset = 0 for default
        public GuideText setSize(int xOffset, int yOffset, int width) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.width = width;
            return this;
        }
    }

    public static class GuideImage {
        public ResourceLocation image;
        public int x;
        public int y;
        public int sizeX;
        public int sizeY;

        public GuideImage(ResourceLocation image, int x, int y, int sizeX, int sizeY) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
        }
    }
}
