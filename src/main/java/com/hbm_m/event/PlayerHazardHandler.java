package com.hbm_m.event;

import com.hbm_m.hazard.HazardSystem;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Применение опасностей инвентаря игрока. Порт {@link com.hbm.hazard.HazardSystem#updatePlayerInventory} (1.7.10).
 * <p>
 * ВНИМАНИЕ: здесь НЕТ проверки {@code isCreative()}/{@code isSpectator()} — это намеренно.
 * В 1.7.10 {@code ModEventHandler} вызывает {@code updatePlayerInventory(player)} каждый тик для
 * <b>всех</b> игроков (включая creative). Сама блокировка creative находится внутри
 * {@link com.hbm_m.util.ContaminationUtil#contaminate} (1:1 с 1.7.10), и срабатывает она
 * <b>после</b> того, как {@code radEnv} уже накопил дозу. Это позволяет счётчику Гейгера
 * {@code ItemGeigerCounter} (читающему {@code radBuf ← radEnv}) отображать radiation даже
 * креативщику, если у него в инвентаре лежат радиоактивные предметы — точно как в 1.7.10.
 * Аналогично взрыв MK5 через {@code radiate()} использует {@code RAD_BYPASS}, который игнорирует
 * броню, но НЕ делает исключения для creative при накоплении {@code radEnv} → счётчик регистрирует
 * всплеск радиации в момент взрыва, не начисляя дозу креативщику.
 */
public class PlayerHazardHandler {

    public static void init() {
        TickEvent.PLAYER_POST.register(PlayerHazardHandler::onPlayerTick);
    }

    private static void onPlayerTick(Player player) {
        if (player.level().isClientSide) {
            return;
        }

        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (!stack.isEmpty()) {
                HazardSystem.applyHazards(stack, player);
            }
        }

        for (ItemStack stack : player.getArmorSlots()) {
            if (!stack.isEmpty()) {
                HazardSystem.applyHazards(stack, player);
            }
        }
    }
}
