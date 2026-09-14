package com.hbm_m.event;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.handler.ArmorRegistry;
import com.hbm_m.handler.HazardClass;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.inventory.fluid.trait.PollutionType;

import net.minecraft.world.effect.MobEffectInstance;

import dev.architectury.event.events.common.BlockEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Пыль при добыче: при разрушении угольных руд вокруг с шансом 50% на каждый
 * воздушный блок ставится угольный газ; при разрушении асбестосодержащих блоков
 * с шансом 10% ставится асбестовый газ.
 * Порт логики из {@code com.hbm.main.ModEventHandler} (BlockBreak) и
 * {@code com.hbm.blocks.BlockOutgas}/{@code BlockOreBasalt} (1.7.10).
 */
public final class LungGasHandler {

    private LungGasHandler() {
    }

    public static void init() {
        BlockEvent.BREAK.register(LungGasHandler::onBreak);
    }

    private static dev.architectury.event.EventResult onBreak(Level level, BlockPos pos, BlockState state,
            ServerPlayer player, dev.architectury.utils.value.IntValue xp) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return dev.architectury.event.EventResult.pass();
        }

        if (isCoalBlock(state)) {
            placeGasAround(serverLevel, pos, ModBlocks.GAS_COAL.get(), 2);
        } else if (isAsbestosBlock(state)) {
            placeGasAround(serverLevel, pos, ModBlocks.GAS_ASBESTOS.get(), 5);
        }

        applyLeadFromBlocks(serverLevel, pos, player);

        return dev.architectury.event.EventResult.pass();
    }

    /**
     * 1:1-Port des Bleiteils aus {@code ModEventHandler.onBlockBreak} (1.7.10): wer ohne
     * Feinstaubschutz in einer schwermetallbelasteten Zelle graebt, wirbelt Blei auf.
     */
    private static void applyLeadFromBlocks(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (player == null) return;
        if (!ModClothConfig.get().enablePollution || !ModClothConfig.get().enableLeadFromBlocks) return;
        if (ArmorRegistry.hasProtection(player, 3, HazardClass.PARTICLE_FINE)) return;

        float metal = PollutionHandler.getPollution(level, pos, PollutionType.HEAVYMETAL);

        if (metal < 5) return;

        int amplifier = metal < 10 ? 0 : (metal < 25 ? 1 : 2);
        player.addEffect(new MobEffectInstance(ModEffects.LEAD.get(), 100, amplifier));
    }

    private static boolean isCoalBlock(BlockState state) {
        return state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE)
                || state.is(Blocks.COAL_BLOCK)
                || state.is(ModBlocks.LIGNITE_ORE.get()) || state.is(ModBlocks.LIGNITE_ORE_DEEPSLATE.get());
    }

    private static boolean isAsbestosBlock(BlockState state) {
        return state.is(ModBlocks.ASBESTOS_ORE.get()) || state.is(ModBlocks.ASBESTOS_ORE_DEEPSLATE.get())
                || state.is(ModBlocks.GNEISS_ASBESTOS_ORE.get())
                || state.is(ModBlocks.RESOURCE_ASBESTOS.get()) || state.is(ModBlocks.STONE_RESOURCE_ASBESTOS.get())
                || state.is(ModBlocks.BRICK_ASBESTOS.get())
                || state.is(ModBlocks.TILE_LAB.get()) || state.is(ModBlocks.TILE_LAB_BROKEN.get())
                || state.is(ModBlocks.TILE_LAB_CRACKED.get())
                || state.is(ModBlocks.COLTAN_ORE.get()) || state.is(ModBlocks.COLTAN_ORE_DEEPSLATE.get());
    }

    /**
     * Для каждого воздушного соседа ставит газ с шансом {@code 1/oneInN}
     * (оригинал: уголь — 50% на соседа, асбест — реже).
     */
    private static void placeGasAround(ServerLevel level, BlockPos pos, Block gas, int oneInN) {
        RandomSource random = level.random;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (level.getBlockState(neighbor).isAir() && random.nextInt(oneInN) == 0) {
                level.setBlock(neighbor, gas.defaultBlockState(), 3);
            }
        }
    }
}
