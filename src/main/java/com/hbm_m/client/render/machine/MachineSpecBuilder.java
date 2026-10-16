package com.hbm_m.client.render.machine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Билдер спеки станка. Создаётся через {@link MachineRenderers#machine};
 * завершается {@link #register()}, который регистрирует BER и спеку в реестре.
 *
 * @param <T> класс BlockEntity станка
 */
public final class MachineSpecBuilder<T extends BlockEntity> {

    private final String id;
    private final Class<T> beClass;
    private final net.minecraft.world.level.block.entity.BlockEntityType<T> type;

    private Function<T, BakedModel> modelResolver = MachineRenderers::blockstateModel;
    private Function<T, Direction> facingResolver = MachineRenderers::defaultFacing;
    @Nullable
    private BlockTransform<T> blockTransform; // null = дефолт (T(0.5,0,0.5)+R(90)+R(legacy facing))
    private final List<MachineSpec.PartDef<T>> parts = new ArrayList<>();
    private final Map<String, Integer> boneIds = new HashMap<>(); // имя части → boneId (1.. внутри группы)
    private final List<MachineRenderHook<T>> hooks = new ArrayList<>();
    private int viewDistance = -1;

    MachineSpecBuilder(String id, Class<T> beClass, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        this.id = id;
        this.beClass = beClass;
        this.type = type;
    }

    /** Модель по BE: скины дверей, hot/cold дуговой печи и т.п. По умолчанию — модель blockstate. */
    public MachineSpecBuilder<T> model(Function<T, BakedModel> resolver) {
        this.modelResolver = resolver;
        return this;
    }

    /** Facing станка. По умолчанию — HORIZONTAL_FACING из blockstate, иначе NORTH. */
    public MachineSpecBuilder<T> facing(Function<T, Direction> resolver) {
        this.facingResolver = resolver;
        return this;
    }

    /**
     * Кастомный блочный трансформ (редко нужно; по умолчанию — translate(0.5,0,0.5)
     * + rotate(90) + legacy facing rotation). Через {@code animator.translate/rotate}
     * — он делегирует на PoseStack. Всё содержимое применяется ДО аниматоров частей.
     */
    public MachineSpecBuilder<T> blockTransform(BlockTransform<T> fn) {
        this.blockTransform = fn;
        return this;
    }

    /** Кастомный блочный трансформ спеки. */
    @FunctionalInterface
    public interface BlockTransform<T extends BlockEntity> {
        void apply(T blockEntity, com.hbm_m.client.render.LegacyAnimator animator);
    }

    /** Статическая часть (рисуется в позе блока, без анимации). */
    public MachineSpecBuilder<T> part(String name) {
        parts.add(new MachineSpec.PartDef<>(name, name, null, null, null, 0));
        return this;
    }

    /** Анимированная часть: {@link PartAnimator} задаёт трансформ относительно блока. */
    public MachineSpecBuilder<T> part(String name, PartAnimator<T> animator) {
        parts.add(new MachineSpec.PartDef<>(name, name, animator, null, null, 0));
        return this;
    }

    /**
     * Анимированная часть, ссылающаяся на чужую модель: несколько логических частей
     * поверх одной части OBJ (например, 4 шестерни из части "Cog").
     */
    public MachineSpecBuilder<T> part(String modelPartName, String name, PartAnimator<T> animator) {
        parts.add(new MachineSpec.PartDef<>(name, modelPartName, animator, null, null, 0));
        return this;
    }

    /**
     * Динамическая часть с per-BE геометрией (стены флюид-танка по флюиду, DAE-ноды).
     * VBO кешируется по ключу {@code cacheKeyFn}; возвращаемый квад-лист может быть пустым.
     */
    public MachineSpecBuilder<T> dynamicPart(String name, QuadResolver<T> quads, Function<T, String> cacheKeyFn) {
        parts.add(new MachineSpec.PartDef<>(name, name, null, quads, cacheKeyFn, 0));
        return this;
    }

    /** Динамическая часть с per-BE геометрией И анимацией (например, dish крупного/малого радара). */
    public MachineSpecBuilder<T> dynamicPart(String name, PartAnimator<T> animator,
                                             QuadResolver<T> quads, Function<T, String> cacheKeyFn) {
        parts.add(new MachineSpec.PartDef<>(name, name, animator, quads, cacheKeyFn, 0));
        return this;
    }

    /**
     * Кинематическая группа (GPU bone skinning): части получают per-vertex bone id
     * 1..N в порядке перечисления ВНУТРИ этой группы и автоматически исключаются из
     * MDI-атласа (механизм InstancedStaticPartRenderer.addInstanceGpuBones).
     * Части всё равно нужно объявить через {@link #part(String, PartAnimator)}.
     */
    public MachineSpecBuilder<T> chain(String... partNames) {
        int boneId = 1;
        for (String name : partNames) {
            boneIds.putIfAbsent(name, boneId++);
        }
        return this;
    }

    /** Дополнительный immediate-проход: жидкости, NFPA-алмазы, предметы-иконки. */
    public MachineSpecBuilder<T> hook(MachineRenderHook<T> hook) {
        hooks.add(hook);
        return this;
    }

    /** Дистанция прорисовки BER в блоках. По умолчанию — modelStaticRenderDistance. */
    public MachineSpecBuilder<T> viewDistance(int blocks) {
        this.viewDistance = blocks;
        return this;
    }

    /** Регистрирует BER в ванильном реестре + спеку в {@link MachineRenderRegistry}. */
    public void register() {
        // проставляем boneId частям по chain-объявлениям
        List<MachineSpec.PartDef<T>> resolved = new ArrayList<>(parts.size());
        for (MachineSpec.PartDef<T> p : parts) {
            Integer bone = boneIds.get(p.name());
            if (bone != null) {
                resolved.add(new MachineSpec.PartDef<>(p.name(), p.modelPartName(), p.animator(), p.dynamicQuads(), p.dynamicCacheKey(), bone));
            } else {
                resolved.add(p);
            }
        }
        MachineSpec<T> spec = new MachineSpec<>(id, beClass, modelResolver, facingResolver,
                resolved, hooks, viewDistance, blockTransform);
        BlockEntityRenderers.register(type, ctx -> new MachineBer<>(spec));
        MachineRenderRegistry.register(spec);
    }
}
