package com.hbm_m.item;

import com.hbm_m.interfaces.ILockable;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Порт {@code ItemCounterfeitKeys} (1.7.10) — набор имитации ключей. Клик по запертому
 * замку (если тот {@code cheesable}) расходует набор и выдаёт ДВА поддельных ключа
 * ({@code key_fake}) с кодом замка. К незатрeriруемым замкам («cheesable = false»)
 * не поддающимся имитации замкам ({@code cheesable = false}) подделку сделать нельзя —
 * игроку отправляются те же два сообщения, что в оригинале.
 */
public class ItemCounterfeitKeys extends Item {

    public ItemCounterfeitKeys(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        BlockEntity be = level.getBlockEntity(pos);
        ILockable lockable = com.hbm_m.util.LockHelper.resolve(be);

        if (lockable != null && lockable.isLocked() && player != null && !level.isClientSide) {
            if (!lockable.isCheesable()) {
                // Как в оригинале: обе строки в чат, LIGHT_PURPLE
                player.displayClientMessage(Component.literal("This lock is too elaborate for a counterfeit key to be made").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), false);
                player.displayClientMessage(Component.literal("Perhaps there is another way around here to unlock it").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), false);
                return InteractionResult.CONSUME;
            }

            // Расходуем набор, выдаём 2 поддельных ключа с кодом замка (в инвентарь или дропом)
            ItemStack fake = new ItemStack(ModItems.KEY_FAKE.get());
            ItemKeyPin.setCode(fake, lockable.getPins());
            stack.shrink(1);
            for (int i = 0; i < 2; i++) {
                ItemStack copy = fake.copy();
                if (!player.getInventory().add(copy)) {
                    player.drop(copy, false);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
