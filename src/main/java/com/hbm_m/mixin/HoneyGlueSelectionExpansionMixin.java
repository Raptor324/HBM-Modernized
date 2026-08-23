package com.hbm_m.mixin;

//? if forge || neoforge {
import com.hbm_m.compat.create.GlueOutlineCompat;
import com.hbm_m.compat.create.MultiblockExpander;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.interfaces.IMultiblockPart;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * Визуальное выделение полного мультиблока HBM при выделении МЕДОКЛЕЕМ
 * (simulated / Create Aeronautics).
 *
 * <p><b>Проблема:</b> у медоклея нет кластерного выделения как у супер-клея
 * Create — {@code HoneyGlueClientHandler.renderSelection} рисует единственный
 * AABB между первым и вторым кликом. Если игрок накрыл боксом только часть
 * мультиблока, рамка показывает ровно этот бокс, а остальные части и контроллер
 * никак не подсвечиваются — игрок не видит, что при сборке (благодаря
 * {@code SubLevelAssembleExpansionMixin}) физичной станет вся структура.
 *
 * <p><b>Решение:</b> на TAIL {@code clientTick}, пока идёт выделение
 * ({@code selectedPos != null}), собираем части мультиблоков HBM внутри
 * выделяемого бокса и дорисовываем их полный кластер через родной аутлайнер
 * (рефлексивно, см. {@link GlueOutlineCompat}). Сам бокс медоклея рисует
 * simulated как раньше — мы только ДОБАВЛЯЕМ подсветку остальных частей.
 *
 * <p>Таргет строкой: без simulated класс не загружается и mixin не применяется.
 */
@Pseudo
@Mixin(targets = "dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueClientHandler")
public abstract class HoneyGlueSelectionExpansionMixin {

    /** Не null только в состоянии BINDING (между первым и вторым кликом). */
    @Shadow
    private BlockPos selectedPos;

    @Inject(method = "clientTick", at = @At("TAIL"), remap = false, require = 0)
    private void hbm_m$drawFullMultiblockCluster(Level level, LocalPlayer player, CallbackInfo ci) {
        if (selectedPos == null) {
            return; // Нет активного выделения.
        }
        try {
            Minecraft mc = Minecraft.getInstance();
            if (!(mc.hitResult instanceof BlockHitResult bhr)) {
                return;
            }
            ClientLevel clientLevel = mc.level;
            if (clientLevel == null) {
                return;
            }

            // Тот же бокс, что рисует сам simulated (encapsulatingFullBlocks,
            // но вручную — метод отсутствует на 1.20.1).
            BlockPos p1 = selectedPos;
            BlockPos p2 = bhr.getBlockPos();
            AABB box = new AABB(
                    Math.min(p1.getX(), p2.getX()),
                    Math.min(p1.getY(), p2.getY()),
                    Math.min(p1.getZ(), p2.getZ()),
                    Math.max(p1.getX(), p2.getX()) + 1.0,
                    Math.max(p1.getY(), p2.getY()) + 1.0,
                    Math.max(p1.getZ(), p2.getZ()) + 1.0);

            // Сиды: части/контроллеры HBM внутри бокса (с ограничением объёма скана).
            Set<BlockPos> seeds = null;
            int scanned = 0;
            for (BlockPos pos : BlockPos.betweenClosed(
                    BlockPos.containing(box.minX, box.minY, box.minZ),
                    BlockPos.containing(box.maxX, box.maxY, box.maxZ).offset(1, 1, 1))) {
                if (++scanned > 16384) break;
                BlockEntity be = clientLevel.getBlockEntity(pos);
                if (be instanceof IMultiblockPart || be instanceof IMultiblockController) {
                    if (seeds == null) seeds = new HashSet<>();
                    seeds.add(pos.immutable());
                }
            }
            if (seeds == null || seeds.isEmpty()) {
                return;
            }

            Set<BlockPos> expanded = MultiblockExpander.expandToFullMultiblock(clientLevel, seeds);
            GlueOutlineCompat.showCluster(GlueOutlineCompat.HBM_CLUSTER_SLOT, expanded);
        } catch (Throwable ignored) {
            // Подсветка не должна ломать выделение медоклеем.
        }
    }
}
//?}
