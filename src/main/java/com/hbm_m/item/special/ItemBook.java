package com.hbm_m.item.special;

import java.util.List;

import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.inventory.menu.BookMenu;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Книга Вагонов ({@code book_of_}, в оригинале "The Book of Boxcars") —
 * порт {@code com.hbm.items.special.ItemBook} из 1.7.10.
 *
 * <p>ПКМ открывает контейнер расширенного 4-слотового крафта
 * ({@link BookMenu}); рецепты — {@code MagicRecipes}. Предмет скрыт
 * из креатива в оригинале ({@code setCreativeTab(null)}), поэтому не
 * добавляется ни в одну вкладку.</p>
 */
public class ItemBook extends Item implements ITooltipProvider {

    public ItemBook(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if(!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            MenuRegistry.openExtendedMenu(serverPlayer, new BookMenuProvider(), buf -> {});
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hbm_m.book_of_.desc"));
    }

    /** MenuProvider без привязки к блоку — позиция, как в оригинале, не используется. */
    private record BookMenuProvider() implements MenuProvider {

        @Override
        public Component getDisplayName() {
            return Component.translatable("container.hbm_m.book");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return new BookMenu(id, inventory);
        }
    }
}
