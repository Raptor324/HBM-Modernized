package com.hbm_m.item;

import com.hbm_m.interfaces.ILockable;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Порт {@code ItemLock} (1.7.10) — навесной замок. Вырезается на кейфордже (наследует
 * {@link ItemKeyPin}), вешается кликом по запираемому блоку: переносит свой код в
 * {@link ILockable}, запирает его и расходуется. На уже запертом ничего не делает.
 */
public class ItemLock extends ItemKeyPin {

    /** Базовый шанс взлома, передаётся в замок при навешивании. */
    private final double lockMod;

    public ItemLock(Properties properties, double lockMod) {
        super(properties);
        this.lockMod = lockMod;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        int pins = getCode(stack);
        if (pins <= 0) {
            return InteractionResult.PASS; // Невырезанный замок — как в оригинале (код 0)
        }

        BlockEntity be = level.getBlockEntity(pos);
        // Мультиблоки (двери): замок вешается на контроллер; ванильные контейнеры —
        // через адаптер persistent data (см. LockHelper)
        ILockable lockable = com.hbm_m.util.LockHelper.resolve(be);

        if (lockable != null) {
            if (lockable.isLocked()) {
                return InteractionResult.PASS; // Уже заперто
            }
            if (!level.isClientSide) {
                lockable.setPins(pins);
                lockable.lock();
                lockable.setLockMod(lockMod);
                PlatformHooks.playSound(level, pos, ModSounds.LOCK_HANG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                stack.shrink(1);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }
}
