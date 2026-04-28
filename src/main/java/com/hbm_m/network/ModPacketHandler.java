package com.hbm_m.network;

import java.util.function.Function;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.packets.PowerArmorDashPacket;
import com.hbm_m.network.sounds.GeigerSoundPacket;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Кроссплатформенный обработчик пакетов.
 * Использует Architectury NetworkManager — один код для Forge и Fabric.
 *
 * Регистрация пакета:
 *   registerS2C(ID, decoder, handler)  — сервер → клиент
 *   registerC2S(ID, decoder, handler)  — клиент → сервер
 *
 * Отправка:
 *   sendToPlayer(player, packet)       — сервер → конкретный игрок
 *   sendToServer(packet)               — клиент → сервер
 */

public class ModPacketHandler {

    private static boolean REGISTERED = false;

    // ══════════════════════════ ID пакетов ════════════════════════════════════

    // S2C
    public static final ResourceLocation GEIGER_SOUND          = id("geiger_sound");
    public static final ResourceLocation RADIATION_DATA        = id("radiation_data");
    public static final ResourceLocation CHUNK_RAD_DEBUG_BATCH = id("chunk_rad_debug_batch");
    public static final ResourceLocation HIGHLIGHT_BLOCKS      = id("highlight_blocks");
    public static final ResourceLocation SYNC_ENERGY           = id("sync_energy");
    public static final ResourceLocation AUX_PARTICLE          = id("aux_particle");

    // C2S
    public static final ResourceLocation GIVE_TEMPLATE         = id("give_template");
    public static final ResourceLocation UPDATE_BATTERY        = id("update_battery");
    public static final ResourceLocation SET_ASSEMBLER_RECIPE  = id("set_assembler_recipe");
    public static final ResourceLocation SET_CHEM_RECIPE       = id("set_chem_recipe");
    public static final ResourceLocation TOGGLE_WOOD_BURNER    = id("toggle_wood_burner");
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
    public static final ResourceLocation ORPHANED_PHANTOMS     = id("orphaned_phantoms");
    public static final ResourceLocation SPAWN_PARTICLE        = id("spawn_particle");


    // ══════════════════════════ Регистрация ═══════════════════════════════════

    public static void register() {
        if (REGISTERED) return;
        REGISTERED = true;

        // ── S2C (сервер → клиент) ────────────────────────────────────────────

        registerS2C(GEIGER_SOUND,
                GeigerSoundPacket::decode,
                GeigerSoundPacket::handle);

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

        registerS2C(POWER_ARMOR_DASH,
                PowerArmorDashPacket::decode,
                PowerArmorDashPacket::handle);

        registerC2S(DOOR_MODEL,
                ServerboundDoorModelPacket::decode,
                ServerboundDoorModelPacket::handle);

        registerC2S(FLUID_IDENTIFIER_CTRL,
                FluidIdentifierControlPacket::decode,
                FluidIdentifierControlPacket::handle);

        registerC2S(ITEM_DESIGNATOR,
                ItemDesignatorPacket::decode,
                ItemDesignatorPacket::handle);

        registerS2C(ORPHANED_PHANTOMS,
                HighlightBlocksPacket.OrphanedPhantomsPacket::fromBytes,
                HighlightBlocksPacket.OrphanedPhantomsPacket::handle);

        registerS2C(SPAWN_PARTICLE,
                SpawnAlwaysVisibleParticlePacket::decode,
                SpawnAlwaysVisibleParticlePacket::handle);
    }

    // ══════════════════════ Вспомогательные методы ════════════════════════════

    private static ResourceLocation id(String path) {
        return new ResourceLocation(RefStrings.MODID, path);
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
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                id,
                (buf, context) -> handler.accept(decoder.apply(buf), context)
        );
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
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        packet.write(buf);
        NetworkManager.sendToPlayer(player, id, buf);
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

    /**
     * Отправить пакет на сервер (C2S).
     * Вызывается с клиентской стороны.
     */
    public static void sendToServer(ResourceLocation id, C2SPacket packet) {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        packet.write(buf);
        NetworkManager.sendToServer(id, buf);
    }
}