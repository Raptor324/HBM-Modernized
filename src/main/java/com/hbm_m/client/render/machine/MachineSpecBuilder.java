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
    private final Map<String, Function<T, Integer>> lightOverrides = new HashMap<>();
    private int viewDistance = -1;
    @Nullable
    private java.util.List<String> itemParts;
    @Nullable
    private java.util.List<String> itemExcept;
    @Nullable
    private java.util.List<net.minecraft.client.renderer.RenderType> chunkRenderTypes;

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
        parts.add(new MachineSpec.PartDef<>(name, name, null, null, null, 0, false, id + "/" + name, lightOverrides.get(name)));
        return this;
    }

    /** Анимированная часть: {@link PartAnimator} задаёт трансформ относительно блока. */
    public MachineSpecBuilder<T> part(String name, PartAnimator<T> animator) {
        parts.add(new MachineSpec.PartDef<>(name, name, animator, null, null, 0, true, id + "/" + name, lightOverrides.get(name)));
        return this;
    }

    /**
     * Анимированная часть, ссылающаяся на чужую модель: несколько логических частей
     * поверх одной части OBJ (например, 4 шестерни из части "Cog").
     */
    public MachineSpecBuilder<T> part(String modelPartName, String name, PartAnimator<T> animator) {
        parts.add(new MachineSpec.PartDef<>(name, modelPartName, animator, null, null, 0, true, id + "/" + name, lightOverrides.get(name)));
        return this;
    }

    /**
     * Форсированный свет части: функция возвращает packedLight (>=0) или -1 для
     * света мира. Порт fullbright-частей оригинала (InnerBurning печей, lightmap 240/240).
     * Вызывать ДО объявления part()/dynamicPart() с этим именем.
     */
    public MachineSpecBuilder<T> lightOverride(String partName, Function<T, Integer> fn) {
        lightOverrides.put(partName, fn);
        return this;
    }

    /**
     * Статическая часть с фиксированным трансформом-«аниматором» (легаси-офсеты запечки:
     * T(-0.5,0,-0.5), yaw-группы). НЕ гейтится по modelUpdateDistance — живёт до
     * статической отсечки, как обычная статика.
     */
    public MachineSpecBuilder<T> staticPart(String name, PartAnimator<T> transform) {
        parts.add(new MachineSpec.PartDef<>(name, name, transform, null, null, 0, false, id + "/" + name, lightOverrides.get(name)));
        return this;
    }

    /**
     * Динамическая часть с per-BE геометрией (стены флюид-танка по флюиду, DAE-ноды).
     * VBO кешируется по ключу {@code cacheKeyFn}; возвращаемый квад-лист может быть пустым.
     */
    public MachineSpecBuilder<T> dynamicPart(String name, QuadResolver<T> quads, Function<T, String> cacheKeyFn) {
        parts.add(new MachineSpec.PartDef<>(name, name, null, quads, cacheKeyFn, 0, false, id + "/" + name, lightOverrides.get(name)));
        return this;
    }

    /** Динамическая часть с per-BE геометрией И анимацией (например, dish крупного/малого радара). */
    public MachineSpecBuilder<T> dynamicPart(String name, PartAnimator<T> animator,
                                             QuadResolver<T> quads, Function<T, String> cacheKeyFn) {
        parts.add(new MachineSpec.PartDef<>(name, name, animator, quads, cacheKeyFn, 0, true, id + "/" + name, lightOverrides.get(name)));
        return this;
    }

    /**
     * Динамическая часть с per-BE геометрией и фиксированным трансформом-«аниматором»
     * (легаси-офсеты). Контент статичен (меняется по состоянию BE, не по времени) —
     * НЕ гейтится по modelUpdateDistance.
     */
    public MachineSpecBuilder<T> dynamicPart(String name, QuadResolver<T> quads, Function<T, String> cacheKeyFn,
                                             PartAnimator<T> transform) {
        parts.add(new MachineSpec.PartDef<>(name, name, transform, quads, cacheKeyFn, 0, false, id + "/" + name, lightOverrides.get(name)));
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

    /**
     * Явный список частей item-рендера multipart-модели. По умолчанию item
     * показывает НЕ-динамические части спеки (см. {@link MachineSpec#deriveItemParts});
     * этот метод нужен, когда динамической части её базовая геометрия в item
     * всё же нужна (танк флюид-хранилища, плита литейщика).
     * Мир-рендер у фабричных станков всегда в BER (chunk mesh пуст) — это
     * следует из самих .part() объявлений и вливается в модель автоматически
     * при {@link #register()} → {@link MachineRenderRegistry#bindBakedModels}.
     */
    public MachineSpecBuilder<T> itemParts(String... parts) {
        this.itemParts = java.util.List.of(parts);
        return this;
    }

    /** Убрать перечисленные части из дефолтного item-набора (не-динамические части спеки). */
    public MachineSpecBuilder<T> itemExcept(String... parts) {
        this.itemExcept = java.util.List.of(parts);
        return this;
    }

    /** Render types чанк-пасса (forge getRenderTypes), когда модель рисуется из chunk mesh. */
    public MachineSpecBuilder<T> chunkRenderTypes(net.minecraft.client.renderer.RenderType... types) {
        this.chunkRenderTypes = java.util.List.of(types);
        return this;
    }

    /** Регистрирует BER в ванильном реестре + спеку в {@link MachineRenderRegistry}. */
    public void register() {
        // проставляем boneId частям по chain-объявлениям
        List<MachineSpec.PartDef<T>> resolved = new ArrayList<>(parts.size());
        for (MachineSpec.PartDef<T> p : parts) {
            Integer bone = boneIds.get(p.name());
            if (bone != null) {
                resolved.add(new MachineSpec.PartDef<>(p.name(), p.modelPartName(), p.animator(), p.dynamicQuads(), p.dynamicCacheKey(), bone, p.animated(), p.staticCacheKey(), p.lightOverride()));
            } else {
                resolved.add(p);
            }
        }
        MachineSpec<T> spec = new MachineSpec<>(id, beClass, type, modelResolver, facingResolver,
                resolved, hooks, viewDistance, blockTransform, itemParts, itemExcept, chunkRenderTypes);
        BlockEntityRenderers.register(type, ctx -> new MachineBer<>(spec));
        MachineRenderRegistry.register(spec);
        // Байпас диспетчера: только типы фабрики MachineRenderers (машины Nucleus).
        com.hbm_m.client.render.NucleusDispatcherBypass.registerManaged(type);
    }
}
