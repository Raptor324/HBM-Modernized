package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.hbm_m.client.render.machine.MachineBer;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;

@OnlyIn(Dist.CLIENT)
/**
 * <b>Байпас диспетчера BlockEntity в MAIN-проходе</b> для машин движка Nucleus
 * ({@code MachineRenderers}) — и ТОЛЬКО для них; остальные BE мода идут штатно.
 * <p>
 * Теневой проход НЕ байпасится намеренно: диспетчер в тени стоит ~3% кадра и
 * там записи работают штатно (профиль 0914 04:04: обрыв теней при теневом
 * байпасе). Весь выигрыш — main (25.4%: обход 600 машин Sodium'ом).
 * <p>
 * Схема:
 * <ol>
 *   <li>{@code collectMain} вызывается явно на {@code AFTER_ENTITIES} — ДО
 *       обхода BE Embeddium'ом: плоский цикл по {@link #LIVE}, pose = eventPose
 *       · T(be−cam); флаг {@code mainCollected} ставится ЗАРАНЕЕ.</li>
 *   <li>Миксин диспетчера в main: managed-BE отменяются (уже отрисованы),
 *       новички регистрируются в {@link #LIVE} (отрисуются со следующего кадра;
 *       чтобы не мигали — одиночный рендер тут же, см. shouldBypass).</li>
 *   <li>Теневой проход: миксин всегда {@code false} — всё штатно.</li>
 * </ol>
 * Выгрузка — чистка по {@code be.isRemoved()/level} на каждом collectMain.
 * Kill-switch: {@code -Dhbm.dispatcherBypass=false}.
 */
public final class NucleusDispatcherBypass {

    private static final boolean KILL_SWITCH =
            !"false".equalsIgnoreCase(System.getProperty("hbm.dispatcherBypass", "true"));

    /** Типы BE, управляемые фабрикой MachineRenderers (registerManaged при регистрации спека). */
    private static final Set<BlockEntityType<?>> MANAGED_TYPES = new HashSet<>();

    /** Живые машины клиентского уровня (наполняется диспетчер-вызовами main-прохода). */
    private static final LinkedHashSet<BlockEntity> LIVE = new LinkedHashSet<>(256);

    private static boolean mainCollected = false;

    /** Scratch: нормаль-часть базиса для общего PoseStack. */
    private static final Matrix3f NORMAL_SCRATCH = new Matrix3f();

    private NucleusDispatcherBypass() {}

    public static void registerManaged(BlockEntityType<?> type) {
        MANAGED_TYPES.add(type);
    }

    static boolean isEnabled() {
        return KILL_SWITCH && !ClientRenderFlags.forceVanillaImmediate()
                && ClientRenderFlags.useInstancedBatching();
    }

    /** Сброс флага main-прохода (вызывается из IrisShadowBatchCollector.noteMainFrameStart). */
    public static void noteMainFrameStart() {
        mainCollected = false;
    }

    /** Уведомление о начале shadow-прохода (тени идут через диспетчер штатно). */
    public static void noteShadowPassStart() {
    }

    /**
     * Решение миксина диспетчера. MAIN: отмена собранных + регистрация новичков
     * (одиночный рендер новичка против мигания). SHADOW: всегда false.
     */
    public static boolean shouldBypass(BlockEntity be, float partialTick,
                                       PoseStack poseStack, MultiBufferSource buffers) {
        if (!isEnabled() || !isManaged(be)) {
            return false;
        }
        if (com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isRenderingShadowPass()) {
            return false; // тени — штатный путь (дешёвый и рабочий)
        }
        // Sable sublevel / Create-контрапшен: диспетчер (Sable) даёт уже готовый
        // трансформ, а collectOne не может корректно перевести позицию BE
        // (за 20M+ блоков дистанционный лимит и float-точность её убивают).
        // Не перехватываем такой рендер вовсе — идёт штатным путём диспетчера.
        if (com.hbm_m.compat.ContraptionRenderCompat.isContraptionRender(be)) {
            return false;
        }
        boolean known = LIVE.contains(be);
        if (!known) {
            LIVE.add(be);
        }
        if (!mainCollected) {
            return false; // collect ещё не был — рисуем штатно и копим список
        }
        if (known) {
            return true; // уже отрисованы колл-обходом
        }
        // Новичок кадра: рисуем одиночно, чтобы не мигал до следующего кадра.
        collectOne(be, partialTick, new PoseStack(), buffers, bypassBase(poseStack));
        return true;
    }

    /**
     * Плоский обход живых машин main-прохода. Вызывается на AFTER_ENTITIES —
     * ДО обхода BE; {@code levelPose} — poseStack события (= базис диспетчера).
     */
    public static void collectMain(PoseStack levelPose, MultiBufferSource buffers,
                                   float partialTick) {
        if (!isEnabled() || mainCollected) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        LIVE.removeIf(be -> be.isRemoved() || be.getLevel() != mc.level || !isManaged(be));

        Matrix4f base = new Matrix4f(levelPose.last().pose());
        PoseStack pose = new PoseStack();
        for (BlockEntity be : LIVE) {
            collectOne(be, partialTick, pose, buffers, base, cam, mc);
        }
        mainCollected = true;
        com.hbm_m.client.render.NucleusDebug.recordDraw(0, LIVE.size(), "Bypass main collect");
    }

    /**
     * Базис для одиночного рендера новичка: у диспетчер-вызова poseStack уже
     * несёт T(be−cam) — базис = pose без трансляции.
     */
    private static Matrix4f bypassBase(PoseStack dispatcherPose) {
        Matrix4f base = new Matrix4f(dispatcherPose.last().pose());
        base.m30(0f).m31(0f).m32(0f);
        return base;
    }

    private static boolean isManaged(BlockEntity be) {
        return be != null && be.getLevel() != null
                && be.getLevel() == Minecraft.getInstance().level
                && MANAGED_TYPES.contains(be.getType());
    }

    /** Одна машина: дистанция → renderer → pose = base·T(be−cam) → MachineBer.collectRender. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void collectOne(BlockEntity be, float partialTick, PoseStack pose,
                                   MultiBufferSource buffers, Matrix4f base) {
        collectOne(be, partialTick, pose, buffers, base,
                Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(),
                Minecraft.getInstance());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void collectOne(BlockEntity be, float partialTick, PoseStack pose,
                                   MultiBufferSource buffers, Matrix4f base,
                                   Vec3 cam, Minecraft mc) {
        BlockPos pos = be.getBlockPos();
        // Страховка: сублевел/контрапшен BE не должны собираться байпасом
        // (см. shouldBypass) — их трансформ даёт внешний диспетчер.
        if (com.hbm_m.compat.ContraptionRenderCompat.isContraptionRender(be)) {
            return;
        }
        double dx = pos.getX() - cam.x;
        double dy = pos.getY() - cam.y;
        double dz = pos.getZ() - cam.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        // Страховочный дальний предел: чуть больше максимальной КОНФИГ-дистанции
        // (ползунки статичной/анимированной прорисовки) — реальный мягкий фейд
        // всё равно живёт внутри renderParts, здесь только дешёвый ранний выход.
        double maxDist = Math.max(RenderDistanceHelper.getStaticDistanceBlocks(),
                RenderDistanceHelper.getAnimatedDistanceBlocks()) + 8.0;
        if (distSq > maxDist * maxDist) {
            return;
        }

        BlockEntityRenderer<?> ber = mc.getBlockEntityRenderDispatcher().getRenderer(be);
        if (!(ber instanceof MachineBer<?> machineBer)) {
            return;
        }
        pose.last().pose().set(base);
        // Нормаль-часть базиса — иначе нормали иконок хуков без вращения камеры.
        NORMAL_SCRATCH.set(base);
        pose.last().normal().set(NORMAL_SCRATCH);
        pose.translate((float) dx, (float) dy, (float) dz);
        int packedLight = net.minecraft.client.renderer.LevelRenderer.getLightColor(
                be.getLevel(), be.getBlockState(), pos);
        try {
            ((MachineBer<BlockEntity>) machineBer).collectRender(
                    be, partialTick, pose, buffers, packedLight, true);
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.error(
                    "[HBM-M] NucleusDispatcherBypass: collect failed for {}", pos, t);
        }
    }
}
