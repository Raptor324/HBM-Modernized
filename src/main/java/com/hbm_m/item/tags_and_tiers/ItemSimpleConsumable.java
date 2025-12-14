package com.hbm_m.item.tags_and_tiers;
// Простой предмет, который выполняет заданное действие при использовании.
// Действие передается через BiConsumer в конструкторе.

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.function.BiConsumer;

import javax.annotation.Nonnull;

public class ItemSimpleConsumable extends Item {
// BiConsumer принимает игрока и стак, который он использует.
    private final BiConsumer<Player, ItemStack> onUseAction;

    public ItemSimpleConsumable(Properties properties, BiConsumer<Player, ItemStack> onUseAction) {
        super(properties);
        this.onUseAction = onUseAction;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Выполняем заданное действие
        if (this.onUseAction != null) {
            this.onUseAction.accept(player, stack);
        }

        // Возвращаем SUCCESS, чтобы запустить анимацию использования предмета.
        return InteractionResultHolder.success(stack);
    }
}