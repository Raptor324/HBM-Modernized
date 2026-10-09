//? if neoforge {
/*package com.hbm_m.main;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.handler.ImpactWorldHandler;
import com.hbm_m.network.ImpactSyncPacket;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.saveddata.TomSaveData;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

// NeoForge-Gegenstueck zu ModEventHandlerImpact (Tom-Einschlag), gleiches Verhalten.
@EventBusSubscriber(modid = MainRegistry.MOD_ID)
public class ModEventHandlerImpactNeoForge {

    @SubscribeEvent
    public static void worldTick(LevelTickEvent.Pre event) {

        if (event.getLevel() instanceof ServerLevel world) {
            float settle = 1F / 14400000F;     // 600 days to completely clear all dust.
            float cool = 1F / 24000F;          // One MC day between initial impact and total darkness.

            ImpactWorldHandler.impactEffects(world);
            TomSaveData data = TomSaveData.forWorld(world);

            if (data.dust > 0 && data.fire == 0) {
                data.dust = Math.max(0, data.dust - settle);
                data.markDirty();
            }

            if (data.fire > 0) {
                data.fire = Math.max(0, (data.fire - cool));
                data.dust = Math.min(1, (data.dust + cool));
                data.markDirty();
            }

            if (world.dimension() == Level.OVERWORLD && data.fire > 0 && data.dust < 0.75F) {

                List<Entity> oList = new ArrayList<>();
                world.getAllEntities().forEach(oList::add);

                for (Entity e : oList) {
                    if (e instanceof LivingEntity entity) {
                        if (world.getBrightness(LightLayer.SKY, BlockPos.containing((int) entity.getX(), (int) entity.getY(), (int) entity.getZ())) > 7) {
                            PlatformHooks.setSecondsOnFire(entity, 5);
                            entity.hurt(world.damageSources().onFire(), 2);
                        }
                    }
                }
            }

            boolean active = data.impact || data.dust > 0 || data.fire > 0;
            if (active || world.getGameTime() % 20 == 0) {
                for (ServerPlayer player : world.players()) ImpactSyncPacket.sendTo(player);
            }
        }
    }

    // Original extinction (CheckSpawn) und onPopulate (Populate.ANIMALS).
    @SubscribeEvent
    public static void extinction(FinalizeSpawnEvent event) {

        TomSaveData data = TomSaveData.forWorld(event.getLevel());
        if (data == null || !data.impact) return;

        MobSpawnType type = event.getSpawnType();

        if (type == MobSpawnType.CHUNK_GENERATION) {
            event.setSpawnCancelled(true);
            return;
        }

        if (type != MobSpawnType.NATURAL && type != MobSpawnType.SPAWNER) return;

        LivingEntity living = event.getEntity();
        if (living instanceof Player) return;

        if (event.getLevel().getLevel().dimension() == Level.OVERWORLD) {
            if (living.getBbHeight() >= 0.85F || living.getBbWidth() >= 0.85F && !(living instanceof WaterAnimal) && !living.isBaby()) {
                event.setSpawnCancelled(true);
                return;
            }
        }
        if (living instanceof WaterAnimal) {
            if (event.getLevel().getRandom().nextInt(5) != 0) {
                event.setSpawnCancelled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLoad(LevelEvent.Load event) {

        TomSaveData.resetLastCached();

        if (event.getLevel() instanceof ServerLevel world) TomSaveData.forWorld(world);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ImpactSyncPacket.sendTo(player);
    }

    @SubscribeEvent
    public static void onChangeDim(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ImpactSyncPacket.sendTo(player);
    }

    @SubscribeEvent
    public static void populateChunkPost(ChunkEvent.Load event) {

        if (!event.isNewChunk()) return;
        if (!(event.getLevel() instanceof ServerLevel world)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;

        TomSaveData data = TomSaveData.forWorld(world);
        if (!data.impact) return;

        boolean surface = data.dust > 0 || data.fire > 0;
        boolean strip = data.dust > 0.25 || data.fire > 0;
        if (!surface) return;

        BlockState impactDirt = ModBlocks.IMPACT_DIRT.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int s = 0; s < sections.length; s++) {
            LevelChunkSection storage = sections[s];
            if (storage == null || storage.hasOnlyAir()) continue;
            int baseY = chunk.getSectionYFromSectionIndex(s) << 4;

            for (int x = 0; x < 16; ++x) {
                for (int y = 0; y < 16; ++y) {
                    for (int z = 0; z < 16; ++z) {

                        BlockState state = storage.getBlockState(x, y, z);
                        Block b = state.getBlock();
                        BlockState replacement = null;

                        if (b == Blocks.GRASS_BLOCK) {
                            replacement = impactDirt;
                        } else if (strip) {
                            if (state.is(BlockTags.LOGS)) {
                                replacement = air;
                            } else if (b instanceof LeavesBlock || state.is(BlockTags.LEAVES)) {
                                replacement = air;
                            } else if (b instanceof BushBlock || b instanceof SugarCaneBlock || b instanceof CocoaBlock) {
                                replacement = air;
                            }
                        }

                        if (replacement != null) {
                            pos.set(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z);
                            chunk.setBlockState(pos, replacement, false);
                        }
                    }
                }
            }
        }
        chunk.setUnsaved(true);
    }
}
*///?}
