package com.hbm_m.radiation;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.RadiationDataPacket;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.hazard.HazardType;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;

import com.mojang.brigadier.arguments.FloatArgumentType;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

/**
 * Обработчик радиации для игроков
 */
public class PlayerRadiationHandler {
    
    // Хранит текущий уровень радиации для каждого игрока
    private static final HashMap<UUID, Float> playerRads = new HashMap<>();
    
    // Ключ для хранения радиации в данных игрока
    private static final String NBT_KEY_PLAYER_RADIATION = "hbm_m_player_radiation";
    
    /**
     * Получает уровень радиации игрока
     * @param player игрок
     * @return уровень радиации
     */
    public static float getPlayerRads(Player player) {
        if (player == null) return 0;
        
        UUID uuid = player.getUUID();
        Float rad = playerRads.get(uuid);
        return rad != null ? rad : 0;
    }
    
    /**
     * Устанавливает уровень радиации игрока
     * @param player игрок
     * @param rads новый уровень радиации
     */
    public static void setPlayerRads(Player player, float rads) {
        if (player == null) return;
        
        UUID uuid = player.getUUID();
        float clamped = Math.round(Math.max(0, rads) * 10.0f) / 10.0f; // Округляем до одной цифры после запятой
        playerRads.put(uuid, clamped);
        
        // Если игрок на сервере, синхронизируем данные с клиентом
        if (player instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.connection != null) {
                // 1. Получаем радиацию окружающей среды
                float environmentRad = ChunkRadiationManager.getRadiation(serverPlayer.level(), serverPlayer.blockPosition().getX(), serverPlayer.blockPosition().getY(), serverPlayer.blockPosition().getZ());
                
                // 2. Отправляем пакет с ДВУМЯ значениями
                ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new RadiationDataPacket(environmentRad, clamped));
                
                if (ModClothConfig.get().enableDebugLogging) {
                    MainRegistry.LOGGER.debug("SERVER: Sent RadiationDataPacket (from setPlayerRads) to player {} with EnvRad: {}, PlayerRad: {}", player.getName().getString(), environmentRad, clamped);
                }
            }
        }
    }
    
    /**
     * Увеличивает уровень радиации игрока
     * @param player игрок
     * @param rads величина увеличения
     */
    public static void incrementPlayerRads(Player player, float rads) {
        if (player == null || rads <= 0) return;
        
        setPlayerRads(player, getPlayerRads(player) + rads);
    }
    
    /**
     * Уменьшает уровень радиации игрока
     * @param player игрок
     * @param rads величина уменьшения
     */
    public static void decrementPlayerRads(Player player, float rads) {
        if (player == null || rads <= 0) return;
        
        setPlayerRads(player, Math.max(0, getPlayerRads(player) - rads));
    }
    
    /**
     * Обработчик загрузки данных игрока
     */
    @SubscribeEvent
    public void onPlayerLoad(PlayerEvent.LoadFromFile event) {
        Player player = event.getEntity();

        if (player instanceof ServerPlayer) {
            CompoundTag persistentData = player.getPersistentData();
            float rads = 0.0F;
            if (persistentData.contains(NBT_KEY_PLAYER_RADIATION)) {
                CompoundTag data = persistentData.getCompound(NBT_KEY_PLAYER_RADIATION);
                rads = Math.round(data.getFloat("radiationLevel") * 10.0f) / 10.0f; // округление при загрузке
            }
            setPlayerRads(player, rads); // Здесь не отправляем пакет, так как соединение еще не установлено
            MainRegistry.LOGGER.debug("Loaded radiation data for player {}: {} RAD",
                    player.getName().getString(), rads);
        }
    }

    /**
     * Обработчик сохранения данных игрока
     */
    @SubscribeEvent
    public void onPlayerSave(PlayerEvent.SaveToFile event) {
        Player player = event.getEntity();

        if (player instanceof ServerPlayer) {
            CompoundTag persistentData = player.getPersistentData();
            CompoundTag data = new CompoundTag();
            float rounded = Math.round(getPlayerRads(player) * 10.0f) / 10.0f; // округление при сохранении
            data.putFloat("radiationLevel", rounded);
            persistentData.put(NBT_KEY_PLAYER_RADIATION, data);

            MainRegistry.LOGGER.debug("Saving radiation data for player {}: {} RAD",
                    player.getName().getString(), rounded);
        }
    }
    
    /**
     * Обработчик выхода игрока
     */
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        playerRads.remove(player.getUUID());
    }
    
    // Счетчик тиков для периодического обновления
    private int tickCounter = 0;
    
    /**
     * Возвращает радиацию от всех радиоактивных предметов в инвентаре игрока (за тик)
     */
    public static float getInventoryRadiation(Player player) {
        float totalRads = 0;
        for (ItemStack stack : player.getInventory().items) {
            totalRads += getRadiationFromItemStack(stack);
        }
        for (ItemStack stack : player.getInventory().armor) {
            totalRads += getRadiationFromItemStack(stack);
        }
        ItemStack offhand = player.getOffhandItem();
        totalRads += getRadiationFromItemStack(offhand);
        return totalRads;
    }

    private static float getRadiationFromItemStack(ItemStack stack) {
    if (stack.isEmpty()) {
        return 0.0F;
    }
    // 2. Получаем уровень радиации для ОДНОГО предмета из нашей новой универсальной системы.
    // Вся старая логика с `instanceof` и вызовами `.getRadiationLevel()` заменяется одной строкой.
    float perItemRadiation = HazardSystem.getHazardLevelFromStack(stack, HazardType.RADIATION);
        return perItemRadiation * stack.getCount();
    }

    /**
     * Обработчик тика игрока, обновляет радиацию
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Сброс радиации при смерти в любом режиме
        if (event.player.isDeadOrDying()) {
            setPlayerRads(event.player, 0F);
            return;
        }

        if (!ModClothConfig.get().enableRadiation || event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }

        Player player = event.player;
        tickCounter++;

        // Обновляем радиацию каждые 20 тиков (1 секунда)
        if (tickCounter >= 20) {
        tickCounter = 0;

        if (!player.isCreative() && !player.isSpectator()) {
            float chunkRad = 0F;
            float invRad = 0F;
            if (ModClothConfig.get().enableChunkRads) {
                chunkRad = ChunkRadiationManager.getRadiation(player.level(), player.blockPosition().getX(), player.blockPosition().getY(), player.blockPosition().getZ());
            }
            if (ModClothConfig.get().enableRadiation) {
                invRad = getInventoryRadiation(player);
            }
            float totalRad = chunkRad + invRad;
            
            // Расчет входящей радиации С УЧЕТОМ защиты
            float totalAbsoluteProtection = 0f;
            for (ItemStack armorStack : player.getArmorSlots()) {
                totalAbsoluteProtection += ArmorModificationHelper.getTotalAbsoluteRadProtection(armorStack);
            }
            float protectionPercent = ArmorModificationHelper.convertAbsoluteToPercent(totalAbsoluteProtection);
            float resultingRad = totalRad * (1.0f - protectionPercent);
            
            // Увеличиваем радиацию, только если она положительна
            if (resultingRad > 0) {
                incrementPlayerRads(player, resultingRad);
                if (ModClothConfig.get().enableDebugLogging) { // Логгер теперь внутри if
                    MainRegistry.LOGGER.debug("Add total radiation to player {}: chunk={} inv={} total={} prot={} final={}", 
                        player.getName().getString(), chunkRad, invRad, totalRad, String.format("%.2f%%", protectionPercent * 100), resultingRad);
                }
            }

            // ЭТИ ДВА МЕТОДА ВЫЗЫВАЮТСЯ ВСЕГДА (для игрока в выживании) 
            // Уменьшаем радиацию (естественный распад)
            decrementPlayerRads(player, ModClothConfig.get().radDecay);
            // Применяем эффекты от текущего уровня радиации
            applyRadiationEffects(player);
            // ----------------------------------------------------------------------
        }
            // Отправляем текущее значение радиации игроку на клиент только если он в режиме выживания
            if (player instanceof ServerPlayer serverPlayer && !player.isCreative() && !player.isSpectator()) {
                float currentRads = getPlayerRads(player);
                float environmentRad = ChunkRadiationManager.getRadiation(serverPlayer.level(), serverPlayer.blockPosition().getX(), serverPlayer.blockPosition().getY(), serverPlayer.blockPosition().getZ());
                
                // Отправляем пакет с ДВУМЯ значениями
                ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new RadiationDataPacket(environmentRad, currentRads));
                if (ModClothConfig.get().enableDebugLogging) {
                    MainRegistry.LOGGER.debug("SERVER: NOT sending periodic RadiationDataPacket to player {} because isCreative: {}, isSpectator: {}", player.getName().getString(), player.isCreative(), player.isSpectator());
                }
            }
        }
        
    }
    
    /**
     * Применяет эффекты в зависимости от уровня радиации
     */
    private void applyRadiationEffects(Player player) {
        float rads = getPlayerRads(player);
        
        // Проверяем достижения
        if (player instanceof ServerPlayer serverPlayer) {
            var server = serverPlayer.getServer();
            if (server != null) { // Добавлена проверка на null
                ServerAdvancementManager advancementManager = server.getAdvancements();

                // Достижение "Ура, Радиация!" (200 РАД)
                Advancement rad200Advancement = advancementManager.getAdvancement(ResourceLocation.fromNamespaceAndPath("hbm_m", "radiation_200"));
                if (rad200Advancement != null) {
                    AdvancementProgress progress = serverPlayer.getAdvancements().getOrStartProgress(rad200Advancement);
                    if (!progress.isDone()) {
                        if (ModClothConfig.get().enableDebugLogging) {
                            MainRegistry.LOGGER.debug("SERVER: Checking radiation_200 advancement for player {}. Current rads: {}, isDone: {}", serverPlayer.getName().getString(), rads, progress.isDone());
                        }
                            if (rads >= 200.0F) {
                            for (String criterion : progress.getRemainingCriteria()) {
                                serverPlayer.getAdvancements().award(rad200Advancement, criterion);
                                if (ModClothConfig.get().enableDebugLogging) {
                                    MainRegistry.LOGGER.info("SERVER: Awarded radiation_200 advancement to player {} for criterion {}", serverPlayer.getName().getString(), criterion);
                                }
                            }
                        }
                    }
                }

                // Испытание "Ай, Радиация!" (1000 РАД)
                Advancement rad1000Advancement = advancementManager.getAdvancement(ResourceLocation.fromNamespaceAndPath("hbm_m", "radiation_1000"));
                if (rad1000Advancement != null) {
                    AdvancementProgress progress = serverPlayer.getAdvancements().getOrStartProgress(rad1000Advancement);
                    if (!progress.isDone()) {
                        if (ModClothConfig.get().enableDebugLogging) {
                            MainRegistry.LOGGER.debug("SERVER: Checking radiation_1000 advancement for player {}. Current rads: {}, isDone: {}", serverPlayer.getName().getString(), rads, progress.isDone());
                        }
                            if (rads >= ModClothConfig.get().maxPlayerRad) {
                            for (String criterion : progress.getRemainingCriteria()) {
                                serverPlayer.getAdvancements().award(rad1000Advancement, criterion);
                                if (ModClothConfig.get().enableDebugLogging) {
                                    MainRegistry.LOGGER.info("SERVER: Awarded radiation_1000 advancement to player {} for criterion {}", serverPlayer.getName().getString(), criterion);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Если радиация достигла летального порога, игрок умирает
        if (rads >= ModClothConfig.get().maxPlayerRad) {
            MainRegistry.LOGGER.debug("SERVER: Player {} radiation ({}) reached maxPlayerRad ({}). Killing player and resetting radiation.", player.getName().getString(), rads, ModClothConfig.get().maxPlayerRad);
            player.hurt(ModDamageSources.radiation(player.level()), Float.MAX_VALUE);
            setPlayerRads(player, 0F); // Сброс радиации после смерти
            return; // Прекращаем применение других эффектов, так как игрок мертв
        }

        // Если радиация выше порога урона, наносим урон
        if (rads > ModClothConfig.get().radDamageThreshold) {
            player.hurt(player.damageSources().magic(), ModClothConfig.get().radDamage);
        }
        
        // Применяем различные эффекты в зависимости от уровня радиации
        if (rads > ModClothConfig.get().radBlindness) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 220, 0));
        }
        
        if (rads > ModClothConfig.get().radConfusion) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 220, 0));
        }
        
        if (rads > ModClothConfig.get().radWater) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 220, 2));
        }
        
        if (rads > ModClothConfig.get().radSickness) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 220, 0));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 220, 0));
        }
    }
    
    /**
     * Регистрация команд радиации
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("hbm_m")
                .then(Commands.literal("rad")
                    .then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.literal("clear")
                            .executes(ctx -> {
                                Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
                                final int[] count = {0};
                                for (Entity e : targets) {
                                    if (e instanceof Player p) {
                                        setPlayerRads(p, 0F);
                                        count[0]++;
                                    }
                                }
                                ctx.getSource().sendSuccess(() -> Component.translatable("commands.hbm_m.rad.cleared", count[0]), true);
                                return count[0];
                            })
                        )
                        .then(Commands.literal("add")
                            .then(Commands.argument("amount", FloatArgumentType.floatArg())
                                .executes(ctx -> {
                                    float amount = FloatArgumentType.getFloat(ctx, "amount");
                                    Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
                                    final int[] count = {0};
                                    for (Entity e : targets) {
                                        if (e instanceof Player p) {
                                            incrementPlayerRads(p, amount);
                                            count[0]++;
                                        }
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.translatable("commands.hbm_m.rad.added", amount, count[0]), true);
                                    return count[0];
                                })
                            )
                        )
                        .then(Commands.literal("remove")
                            .then(Commands.argument("amount", FloatArgumentType.floatArg())
                                .executes(ctx -> {
                                    float amount = FloatArgumentType.getFloat(ctx, "amount");
                                    Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
                                    final int[] count = {0};
                                    for (Entity e : targets) {
                                        if (e instanceof Player p) {
                                            decrementPlayerRads(p, amount);
                                            count[0]++;
                                        }
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.translatable("commands.hbm_m.rad.removed", amount, count[0]), true);
                                    return count[0];
                                })
                            )
                        )
                    )
                    // Без targets — по умолчанию @s
                    .then(Commands.literal("clear")
                        .executes(ctx -> {
                            Entity self = ctx.getSource().getEntity();
                            if (self instanceof Player p) {
                                setPlayerRads(p, 0F);
                                ctx.getSource().sendSuccess(() -> Component.translatable("commands.hbm_m.rad.cleared.self"), true);
                                return 1;
                            }
                            return 0;
                        })
                    )
                    .then(Commands.literal("add")
                        .then(Commands.argument("amount", FloatArgumentType.floatArg())
                            .executes(ctx -> {
                                Entity self = ctx.getSource().getEntity();
                                float amount = FloatArgumentType.getFloat(ctx, "amount");
                                if (self instanceof Player p) {
                                    incrementPlayerRads(p, amount);
                                    ctx.getSource().sendSuccess(() -> Component.translatable("commands.hbm_m.rad.added.self", amount), true);
                                    return 1;
                                }
                                return 0;
                            })
                        )
                    )
                    .then(Commands.literal("remove")
                        .then(Commands.argument("amount", FloatArgumentType.floatArg())
                            .executes(ctx -> {
                                Entity self = ctx.getSource().getEntity();
                                float amount = FloatArgumentType.getFloat(ctx, "amount");
                                if (self instanceof Player p) {
                                    decrementPlayerRads(p, amount);
                                    ctx.getSource().sendSuccess(() -> Component.translatable("commands.hbm_m.rad.removed.self", amount), true);
                                    return 1;
                                }
                                return 0;
                            })
                        )
                    )
                )
        );
    }
}