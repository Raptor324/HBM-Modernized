package com.hbm_m.item.special;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.item.ModItems;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.special.ItemBookLore} ({@code book_lore}): Lore-Buecher aus Strukturen/Loot. Alles steht im
 * NBT: {@code k} = Schluessel ({@code book_lore.<k>.name/.author/.page.N}), {@code p} = Seitenzahl, {@code cov_col} und
 * {@code tit_col} = Einband-/Titelfarbe (Farbschichten 1 und 2), {@code p<N>} = Formatargumente {@code a1, a2 ...} einer Seite.
 */
public class ItemBookLore extends Item {

    public ItemBookLore(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (world.isClientSide) com.hbm_m.inventory.gui.GUIBookLore.open(stack);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        if (!stack.hasTag()) return;
        String key = stack.getTag().getString("k");
        if (key.isEmpty()) return;

        key = "book_lore." + key + ".author";
        if (I18n.exists(key))
            list.add(Component.translatable("book_lore.author", I18n.get(key)));
    }

    @Override
    public Component getName(ItemStack stack) {
        if (!stack.hasTag()) return Component.translatable("book_lore.test.name");
        String key = stack.getTag().getString("k");
        return Component.translatable("book_lore." + (key.isEmpty() ? "test" : key) + ".name");
    }

    /** getColorFromItemStack: Schicht 1 = Einband (Standard 0x303030), Schicht 2 = Titel (Standard weiss). */
    public static int getColor(ItemStack stack, int pass) {
        switch (pass) {
            default: return 0xFFFFFF;
            case 1: //book cover
                if (stack.hasTag()) {
                    int color = stack.getTag().getInt("cov_col");
                    if (color > 0) return color;
                }
                return 0x303030;
            case 2: //title color
                if (stack.hasTag()) {
                    int color = stack.getTag().getInt("tit_col");
                    if (color > 0) return color;
                }
                return 0xFFFFFF;
        }
    }

    public static ItemStack createBook(String key, int pages, int colorCov, int colorTit) {
        ItemStack book = new ItemStack(ModItems.BOOK_LORE.get());
        CompoundTag tag = new CompoundTag();
        tag.putString("k", key);
        tag.putShort("p", (short) pages);
        tag.putInt("cov_col", colorCov);
        tag.putInt("tit_col", colorTit);

        book.setTag(tag);
        return book;
    }

    public static void addArgs(ItemStack book, int page, String... args) {
        if (!book.hasTag()) return;
        CompoundTag data = new CompoundTag();
        for (int i = 0; i < args.length; i++) {
            data.putString("a" + (i + 1), args[i]);
        }

        book.getTag().put("p" + page, data);
    }

    /** HbmChestContents.generateOfficeBook: Buero-Notizen (Strukturloot). */
    public static ItemStack generateOfficeBook(net.minecraft.util.RandomSource rand) {
        String key;
        int pages;
        switch (rand.nextInt(5)) {
            case 0: key = "resignation_note"; pages = 3; break;
            case 1: key = "memo_stocks"; pages = 1; break;
            case 2: key = "memo_schrab_gsa"; pages = 2; break;
            case 3: key = "memo_schrab_rd"; pages = 4; break;
            case 4: key = "memo_schrab_nuke"; pages = 3; break;
            default: return ItemStack.EMPTY;
        }

        return createBook(key, pages, 0x6BC8FF, 0x0A0A0A);
    }

    /** HbmChestContents.generateLabBook: Laborbuecher zur Balefire-Bombe. */
    public static ItemStack generateLabBook(net.minecraft.util.RandomSource rand) {
        String key;
        int pages;

        switch (rand.nextInt(5)) {
            case 0: key = "bf_bomb_1"; pages = 4; break;
            case 1: key = "bf_bomb_2"; pages = 6; break;
            case 2: key = "bf_bomb_3"; pages = 6; break;
            case 3: key = "bf_bomb_4"; pages = 5; break;
            case 4: key = "bf_bomb_5"; pages = 9; break;
            default: return ItemStack.EMPTY;
        }

        return createBook(key, pages, 0x1E1E1E, 0x46EA44);
    }

    /** LootGenerator.lootBooklet: Beacon-Handbuch. */
    public static ItemStack generateBeaconBook() {
        return createBook("beacon", 12, 0x404040, 0xD637B3);
    }
}
