package com.hbm_m.network;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.packets.PowerArmorDashPacket;
import com.hbm_m.network.sounds.GeigerSoundPacket;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.PacketContext;
import dev.architectury.utils.Env;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * Кроссплатформенный обработчик пакетов.
 * Использует Architectury NetworkManager — один код для Forge и Fabric.
 *
 * Регистрация пакета:
 *   registerClientReceivers() — S2C (сервер → клиент), только из {@link com.hbm_m.client.ClientSetup#initClient()}
 *   register() — C2S (клиент → сервер), из common setup на клиенте и сервере
 *
 * Отправка:
 *   sendToPlayer(player, packet)       — сервер → конкретный игрок
 *   sendToServer(packet)               — клиент → сервер
 */

@SuppressWarnings("removal")
public class ModPacketHandler {

    private static boolean REGISTERED = false;
    private static boolean CLIENT_RECEIVERS_REGISTERED = false;
    private static final boolean NET_DEBUG_PACKETS = Boolean.getBoolean("hbm_m.netDebugPackets");
    private static final boolean DISABLE_S2C = Boolean.getBoolean("hbm_m.disableS2C");

    // ══════════════════════════ ID пакетов ════════════════════════════════════

    // S2C
    public static final ResourceLocation GEIGER_SOUND          = id("geiger_sound");
    public static final ResourceLocation RADIATION_DATA        = id("radiation_data");
    public static final ResourceLocation INFO_TOAST           = id("info_toast");
    public static final ResourceLocation CHUNK_RAD_DEBUG_BATCH = id("chunk_rad_debug_batch");
    public static final ResourceLocation HIGHLIGHT_BLOCKS      = id("highlight_blocks");
    public static final ResourceLocation SYNC_ENERGY           = id("sync_energy");
    public static final ResourceLocation AUX_PARTICLE          = id("aux_particle");
    public static final ResourceLocation PWR_PRINTER_SCAN      = id("pwr_printer_scan");
    public static final ResourceLocation VANILLA_EXPLOSION     = id("vanilla_explosion");
    public static final ResourceLocation DOOR_CONTRAPTION_STATE = id("door_contrap_state");
    public static final ResourceLocation CONFIG_SYNC           = id("config_sync");

    // C2S
    public static final ResourceLocation GIVE_TEMPLATE         = id("give_template");
    public static final ResourceLocation UPDATE_BATTERY        = id("update_battery");
    public static final ResourceLocation SET_ASSEMBLER_RECIPE  = id("set_assembler_recipe");
    public static final ResourceLocation SET_CHEM_RECIPE       = id("set_chem_recipe");
    public static final ResourceLocation TOGGLE_WOOD_BURNER    = id("toggle_wood_burner");
    public static final ResourceLocation BUILD_MISSILE         = id("build_missile");
    public static final ResourceLocation FLUID_TANK_MODE       = id("fluid_tank_mode");
    public static final ResourceLocation DETONATE_ALL          = id("detonate_all");
    public static final ResourceLocation SET_ACTIVE_POINT      = id("set_active_point");
    public static final ResourceLocation CLEAR_POINT           = id("clear_point");
    public static final ResourceLocation SYNC_POINT            = id("sync_point");
    public static final ResourceLocation ANVIL_CRAFT           = id("anvil_craft");
    public static final ResourceLocation ANVIL_SELECT_RECIPE   = id("anvil_select_recipe");
    public static final ResourceLocation POWER_ARMOR_DASH      = id("power_armor_dash");
    public static final ResourceLocation DOOR_MODEL            = id("door_model");
    public static final ResourceLocation FLUID_IDENTIFIER_CTRL = id("fluid_identifier_ctrl");
    public static final ResourceLocation ITEM_DESIGNATOR       = id("item_designator");
    public static final ResourceLocation UPDATE_RADAR          = id("update_radar");
        public static final ResourceLocation ZIRNOX_CONTROL        = id("zirnox_control");
    public static final ResourceLocation WATZ_CONTROL          = id("watz_control");
    public static final ResourceLocation PWR_CONTROL           = id("pwr_control");
    public static final ResourceLocation RBMK_CONSOLE_CONTROL        = id("rbmk_console_control");
    public static final ResourceLocation RBMK_CONTROL_CONTROL        = id("rbmk_control_control");
    public static final ResourceLocation RBMK_CRANE_CONTROL          = id("rbmk_crane_control");
    public static final ResourceLocation SOYUZ_LAUNCHER_CONTROL      = id("soyuz_launcher_control");
    public static final ResourceLocation SOLDERING_STATION_CONTROL   = id("soldering_station_control");
    public static final ResourceLocation ORPHANED_PHANTOMS     = id("orphaned_phantoms");
    public static final ResourceLocation SPAWN_PARTICLE        = id("spawn_particle");
    public static final ResourceLocation MISSILE_TRACK        = id("missile_track");
    public static final ResourceLocation MISSILE_TRACK_STOP   = id("missile_track_stop");
    public static final ResourceLocation TURRET_CONTROL        = id("turret_control");
    public static final ResourceLocation MINING_DRILL_TOGGLE   = id("mining_drill_toggle");
    public static final ResourceLocation CONFIG_EDIT           = id("config_edit");
    public static final ResourceLocation MICROWAVE_SPEED       = id("microwave_speed");
    public static final ResourceLocation ANNIHILATOR_POOL      = id("annihilator_pool");
    public static final ResourceLocation FUNNEL_MODE            = id("funnel_mode");
    public static final ResourceLocation RADIO_TORCH_CONTROL    = id("radio_torch_control");


    // ══════════════════════════ Регистрация ═══════════════════════════════════

    /**
     * Регистрация приёмников S2C — вызывать только с клиентского setup (Forge {@code FMLClientSetupEvent} /
     * Fabric client entrypoint). Не вызывать из {@code LifecycleEvent.SETUP}: на Forge порядок/класслоадер
     * отличается от настоящего клиента, Architectury может не привязать S2C → обрыв с UTF при входе на сервер.
     */
    public static void registerClientReceivers() {
        if (CLIENT_RECEIVERS_REGISTERED) return;
        CLIENT_RECEIVERS_REGISTERED = true;

        registerS2C(GEIGER_SOUND,
                GeigerSoundPacket::decode,
                GeigerSoundPacket::handle);

        registerS2C(INFO_TOAST,
                InfoToastPacket::decode,
                InfoToastPacket::handle);

        registerS2C(RADIATION_DATA,
                RadiationDataPacket::decode,
                RadiationDataPacket::handle);

        registerS2C(CHUNK_RAD_DEBUG_BATCH,
                ChunkRadiationDebugBatchPacket::decode,
                ChunkRadiationDebugBatchPacket::handle);

        registerS2C(HIGHLIGHT_BLOCKS,
                HighlightBlocksPacket::fromBytes,
                HighlightBlocksPacket::handle);

        registerS2C(SYNC_ENERGY,
                com.hbm_m.network.packet.PacketSyncEnergy::decode,
                com.hbm_m.network.packet.PacketSyncEnergy::handle);

        registerS2C(AUX_PARTICLE,
                AuxParticlePacket::decode,
                AuxParticlePacket::handle);

        registerS2C(PWR_PRINTER_SCAN,
                PWRPrinterScanPacket::decode,
                PWRPrinterScanPacket::handle);

        registerS2C(VANILLA_EXPLOSION,
                VanillaExplosionPacket::decode,
                VanillaExplosionPacket::handle);

        registerS2C(DOOR_CONTRAPTION_STATE,
                DoorContraptionStatePacket::decode,
                DoorContraptionStatePacket::handle);

        registerS2C(POWER_ARMOR_DASH,
                PowerArmorDashPacket::decode,
                PowerArmorDashPacket::handle);

        registerS2C(ORPHANED_PHANTOMS,
                HighlightBlocksPacket.OrphanedPhantomsPacket::fromBytes,
                HighlightBlocksPacket.OrphanedPhantomsPacket::handle);

        registerS2C(SPAWN_PARTICLE,
                SpawnAlwaysVisibleParticlePacket::decode,
                SpawnAlwaysVisibleParticlePacket::handle);

        registerS2C(MISSILE_TRACK,
                com.hbm_m.network.missile.S2CMissileTrackPacket::decode,
                com.hbm_m.network.missile.S2CMissileTrackPacket::handle);

        registerS2C(MISSILE_TRACK_STOP,
                com.hbm_m.network.missile.S2CMissileTrackStopPacket::decode,
                com.hbm_m.network.missile.S2CMissileTrackStopPacket::handle);

        registerS2C(CONFIG_SYNC,
                ConfigSyncS2CPacket::decode,
                ConfigSyncS2CPacket::handle);

    }

    public static void register() {
        if (REGISTERED) return;
        REGISTERED = true;

        // ── C2S (клиент → сервер) ────────────────────────────────────────────

        registerC2S(GIVE_TEMPLATE,
                GiveTemplateC2SPacket::decode,
                GiveTemplateC2SPacket::handle);

        registerC2S(UPDATE_BATTERY,
                UpdateBatteryC2SPacket::decode,
                UpdateBatteryC2SPacket::handle);

        registerC2S(SET_ASSEMBLER_RECIPE,
                SetAssemblerRecipeC2SPacket::decode,
                SetAssemblerRecipeC2SPacket::handle);

        registerC2S(SET_CHEM_RECIPE,
                SetChemPlantRecipeC2SPacket::decode,
                SetChemPlantRecipeC2SPacket::handle);

        registerC2S(TOGGLE_WOOD_BURNER,
                ToggleWoodBurnerPacket::decode,
                ToggleWoodBurnerPacket::handle);

        registerC2S(BUILD_MISSILE,
                BuildMissilePacket::decode,
                BuildMissilePacket::handle);

        registerC2S(FLUID_TANK_MODE,
                FluidTankModePacket::decode,
                FluidTankModePacket::handle);

        registerC2S(DETONATE_ALL,
                DetonateAllPacket::decode,
                DetonateAllPacket::handle);

        registerC2S(SET_ACTIVE_POINT,
                SetActivePointPacket::decode,
                SetActivePointPacket::handle);

        registerC2S(CLEAR_POINT,
                ClearPointPacket::decode,
                ClearPointPacket::handle);

        registerC2S(SYNC_POINT,
                SyncPointPacket::decode,
                SyncPointPacket::handle);

        registerC2S(ANVIL_CRAFT,
                AnvilCraftC2SPacket::decode,
                AnvilCraftC2SPacket::handle);

        registerC2S(ANVIL_SELECT_RECIPE,
                AnvilSelectRecipeC2SPacket::decode,
                AnvilSelectRecipeC2SPacket::handle);

        registerC2S(DOOR_MODEL,
                ServerboundDoorModelPacket::decode,
                ServerboundDoorModelPacket::handle);

        registerC2S(FLUID_IDENTIFIER_CTRL,
                FluidIdentifierControlPacket::decode,
                FluidIdentifierControlPacket::handle);

        registerC2S(ITEM_DESIGNATOR,
                ItemDesignatorPacket::decode,
                ItemDesignatorPacket::handle);

        registerC2S(UPDATE_RADAR,
                UpdateRadarC2SPacket::decode,
                UpdateRadarC2SPacket::handle);

        registerC2S(ZIRNOX_CONTROL,
                ZirnoxControlPacket::decode,
                ZirnoxControlPacket::handle);

        registerC2S(WATZ_CONTROL,
                WatzControlPacket::decode,
                WatzControlPacket::handle);

        registerC2S(PWR_CONTROL,
                PWRControlPacket::decode,
                PWRControlPacket::handle);

        registerC2S(RBMK_CONSOLE_CONTROL,
                RBMKConsoleControlPacket::decode,
                RBMKConsoleControlPacket::handle);

        registerC2S(RBMK_CRANE_CONTROL,
                RBMKCraneControlPacket::decode,
                RBMKCraneControlPacket::handle);

        registerC2S(RBMK_CONTROL_CONTROL,
                RBMKControlPacket::decode,
                RBMKControlPacket::handle);

        registerC2S(SOYUZ_LAUNCHER_CONTROL,
                SoyuzLauncherControlPacket::decode,
                SoyuzLauncherControlPacket::handle);

        registerC2S(SOLDERING_STATION_CONTROL,
                SolderingStationControlPacket::decode,
                SolderingStationControlPacket::handle);

        registerC2S(TURRET_CONTROL,
                TurretControlPacket::decode,
                TurretControlPacket::handle);

        registerC2S(MINING_DRILL_TOGGLE,
                MiningDrillToggleC2SPacket::decode,
                MiningDrillToggleC2SPacket::handle);

        registerC2S(CONFIG_EDIT,
                ConfigEditC2SPacket::decode,
                ConfigEditC2SPacket::handle);

        registerC2S(MICROWAVE_SPEED,
                MicrowaveSpeedC2SPacket::decode,
                MicrowaveSpeedC2SPacket::handle);

        registerC2S(ANNIHILATOR_POOL,
                AnnihilatorPoolC2SPacket::decode,
                AnnihilatorPoolC2SPacket::handle);

        registerC2S(FUNNEL_MODE,
                FunnelModeC2SPacket::decode,
                FunnelModeC2SPacket::handle);

        registerC2S(RADIO_TORCH_CONTROL,
                RadioTorchControlPacket::decode,
                RadioTorchControlPacket::handle);
    }

    // ══════════════════════ Вспомогательные методы ════════════════════════════

    private static ResourceLocation id(String path) {
        //? if fabric && < 1.21.1 {
        /*return new ResourceLocation(RefStrings.MODID, path);
        *///?} else {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path);
        //?}
    }

    /**
     * Регистрирует S2C пакет (сервер → клиент).
     *
     * @param id      идентификатор пакета
     * @param decoder десериализатор из FriendlyByteBuf → T
     * @param handler обработчик на клиенте: принимает T и PacketContext
     */
    private static <T> void registerS2C(
            ResourceLocation id,
            Function<FriendlyByteBuf, T> decoder,
            java.util.function.BiConsumer<T, PacketContext> handler) {
        //? if >= 1.21.1 {
        /*// NeoForge 1.21+: пейлоад участвует в negotiation, поэтому должен быть
        // зарегистрирован на ОБОИХ сторонах. На клиенте — с приёмником, на выделенном
        // сервере — только объявление типа (иначе checkPacket уронит отправку S2C).
        if (dev.architectury.platform.Platform.getEnvironment() == Env.CLIENT) {
            NetworkManager.registerReceiver(
                    NetworkManager.Side.S2C,
                    id,
                    (buf, context) -> handler.accept(decoder.apply(buf), context)
            );
        } else {
            NetworkManager.registerS2CPayloadType(id);
        }
        *///?}
        //? if < 1.21.1 {
        if (dev.architectury.platform.Platform.getEnvironment() == Env.CLIENT) {
            NetworkManager.registerReceiver(
                    NetworkManager.Side.S2C,
                    id,
                    (buf, context) -> handler.accept(decoder.apply(buf), context)
            );
        }
        //?}

    }

    /**
     * Регистрирует C2S пакет (клиент → сервер).
     *
     * @param id      идентификатор пакета
     * @param decoder десериализатор из FriendlyByteBuf → T
     * @param handler обработчик на сервере: принимает T и PacketContext
     */
    private static <T> void registerC2S(
            ResourceLocation id,
            Function<FriendlyByteBuf, T> decoder,
            java.util.function.BiConsumer<T, PacketContext> handler) {
        NetworkManager.registerReceiver(
                NetworkManager.Side.C2S,
                id,
                (buf, context) -> handler.accept(decoder.apply(buf), context)
        );
    }

    // ══════════════════════════ Отправка пакетов ══════════════════════════════

    /**
     * Отправить пакет конкретному игроку (S2C).
     * Вызывается с серверной стороны.
     */
    public static void sendToPlayer(ServerPlayer player, ResourceLocation id, S2CPacket packet) {
        if (DISABLE_S2C) {
            if (NET_DEBUG_PACKETS) {
                com.hbm_m.main.MainRegistry.LOGGER.info(
                        "[NET-DBG] S2C disabled, drop -> {} id={}",
                        player.getGameProfile().getName(),
                        id
                );
            }
            return;
        }
        //? if < 1.21.1 {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        packet.write(buf);
        //?} else {
        /*net.minecraft.network.RegistryFriendlyByteBuf buf = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), player.server.registryAccess());
        packet.write(buf);
        *///?}
        
        if (NET_DEBUG_PACKETS) {
            com.hbm_m.main.MainRegistry.LOGGER.info(
                    "[NET-DBG] S2C -> {} id={} bytes={}",
                    player.getGameProfile().getName(),
                    id,
                    buf.readableBytes()
            );
        }
        // Защита S2C-отправки для «неполноценных» соединений (напр. Flashback ReplayServer):
        // ReplayServer создаёт игрока через placeNewPlayer без полноценного config-handshake,
        // поэтому NeoForge NetworkRegistry.checkPacket бросает UnsupportedOperationException
        // для кастомных пэйлоадов, не прошедших negotiation (config_sync на join,
        // chunk_rad_debug_batch на тике и т.д.). В обычной игре исключение никогда не срабатывает.
        try {
            NetworkManager.sendToPlayer(player, id, buf);
        } catch (UnsupportedOperationException e) {
            if (NET_DEBUG_PACKETS) {
                com.hbm_m.main.MainRegistry.LOGGER.info(
                        "[NET-DBG] S2C dropped for {} id={}: {}",
                        player.getGameProfile().getName(),
                        id,
                        e.getMessage()
                );
            }
        }
    }

    /**
     * Отправить пакет всем игрокам на сервере (S2C broadcast).
     */
    public static void sendToAll(net.minecraft.server.MinecraftServer server,
                                 ResourceLocation id, S2CPacket packet) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendToPlayer(player, id, packet);
        }
    }

    /**
     * Отправить пакет игрокам рядом с точкой (S2C).
     * Полностью кроссплатформенно: без PacketDistributor, только vanilla/Architectury.
     */
    public static void sendToPlayersNear(ServerLevel level, Vec3 pos, double range,
                                         ResourceLocation id, S2CPacket packet) {
        if (level == null) return;

        double rangeSq = range * range;
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level) continue;
            if (player.position().distanceToSqr(pos) > rangeSq) continue;
            sendToPlayer(player, id, packet);
        }
    }

    public static void sendToPlayersNear(ServerLevel level, double x, double y, double z, double range,
                                         ResourceLocation id, S2CPacket packet) {
        sendToPlayersNear(level, new Vec3(x, y, z), range, id, packet);
    }

	//? if >= 1.21.1 {
    /*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    private static net.minecraft.core.RegistryAccess getClientRegistryAccess() {
        if (net.minecraft.client.Minecraft.getInstance().level != null) {
            return net.minecraft.client.Minecraft.getInstance().level.registryAccess();
        }
        if (net.minecraft.client.Minecraft.getInstance().getConnection() != null) {
            return net.minecraft.client.Minecraft.getInstance().getConnection().registryAccess();
        }
        return net.minecraft.core.RegistryAccess.EMPTY;
    }
    *///?}

    /**
     * Отправить пакет на сервер (C2S).
     * Вызывается с клиентской стороны.
     */
    public static void sendToServer(ResourceLocation id, C2SPacket packet) {
        //? if < 1.21.1 {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        packet.write(buf);
        //?} else {
        /*net.minecraft.network.RegistryFriendlyByteBuf buf = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), getClientRegistryAccess());
        packet.write(buf);
        *///?}
        
        if (NET_DEBUG_PACKETS) {
            com.hbm_m.main.MainRegistry.LOGGER.info(
                    "[NET-DBG] C2S id={} bytes={}",
                    id,
                    buf.readableBytes()
            );
        }
        NetworkManager.sendToServer(id, buf);
    }
}