package com.hbm_m.main;

import com.hbm_m.item.ModItems;
import com.hbm_m.network.ConfigSyncS2CPacket;
import com.hbm_m.platform.PlatformHooks;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ModEventHandler {

	private ModEventHandler() {
	}

	public static void register() {
		PlayerEvent.PLAYER_JOIN.register(ModEventHandler::onPlayerJoin);
		EntityEvent.LIVING_DEATH.register(ModEventHandler::onLivingDeath);
	}

	private static void onPlayerJoin(Player player) {
		if (player.level().isClientSide()) {
			return;
		}
		// Синхронизация серверного конфига клиенту при входе.
		// MOTD теперь чисто клиентский (ClientMotdHandler, client.json -> enableMOTD).
		if (player instanceof ServerPlayer sp) {
			ConfigSyncS2CPacket.sendTo(sp);
		}
	}

	/**
	 * Порт блока редких дропов при убийстве игроком ({@code ModEventHandler}, 1.7.10,
	 * LivingDropsEvent → {@code entityLiving.dropItem}). Только реальная рука игрока
	 * (фейковые игроки инфраструктуры исключены). Цепочка кода запуска доомсдей-силоса:
	 * кусок кода запуска (1/250 с враждебных мобов).
	 */
	private static EventResult onLivingDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
		if (entity.level().isClientSide()) {
			return EventResult.pass();
		}
		// 1.7.10 проверял EntityDamageSource с живым игроком; getEntity() - ответственный атакующий.
		if (!(source.getEntity() instanceof Player player)) {
			return EventResult.pass();
		}
		if (PlatformHooks.isFakePlayer(player)) {
			return EventResult.pass();
		}

		var rng = entity.getRandom();

		// Оригинал: CaveSpider проверяется раньше Spider (он его наследник).
		if (entity instanceof net.minecraft.world.entity.monster.CaveSpider) {
			if (rng.nextInt(100) == 0) {
				entity.spawnAtLocation(new ItemStack(ModItems.SERUM.get()));
			}
		} else if (entity instanceof Spider) {
			if (rng.nextInt(500) == 0) {
				entity.spawnAtLocation(new ItemStack(ModItems.SPIDER_MILK.get()));
			}
		}

		if (entity instanceof Animal && rng.nextInt(500) == 0) {
			entity.spawnAtLocation(new ItemStack(ModItems.BANDAID.get()));
		}

		// 1.7.10 IMob = ванильный интерфейс враждебности, в mojmap это Enemy.
		if (entity instanceof Enemy) {
			if (rng.nextInt(1000) == 0) {
				entity.spawnAtLocation(new ItemStack(ModItems.HEART_PIECE.get()));
			}
			if (rng.nextInt(250) == 0) {
				entity.spawnAtLocation(new ItemStack(ModItems.KEY_RED_CRACKED.get()));
			}
			if (rng.nextInt(250) == 0) {
				entity.spawnAtLocation(new ItemStack(ModItems.LAUNCH_CODE_PIECE.get()));
			}
		}

		// CyberCrab (wd40, 1/500) не портирован — сущности нет в порту.

		if (entity instanceof Zombie) {
			if (rng.nextInt(200) == 0) {
				entity.spawnAtLocation(new ItemStack(com.hbm_m.item.material.ModMaterialItems.get(
						com.hbm_m.item.material.ModMaterials.COPPER, com.hbm_m.item.material.MaterialShape.INGOT).get()));
			}
			if (rng.nextInt(200) == 0) {
				entity.spawnAtLocation(new ItemStack(com.hbm_m.item.material.ModMaterialItems.get(
						com.hbm_m.item.material.ModMaterials.ALUMINIUM, com.hbm_m.item.material.MaterialShape.INGOT).get()));
			}
			if (rng.nextInt(200) == 0) {
				entity.spawnAtLocation(new ItemStack(com.hbm_m.item.material.ModMaterialItems.get(
						com.hbm_m.item.material.ModMaterials.TITANIUM, com.hbm_m.item.material.MaterialShape.INGOT).get()));
			}
		}
		return EventResult.pass();
	}
}
