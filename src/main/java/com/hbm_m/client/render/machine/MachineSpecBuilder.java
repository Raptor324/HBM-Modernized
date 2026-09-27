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
    private final List<MachineRenderHook<T>> hooks = new ArrayList<>();
    private final Map<String, Function<T, Integer>> lightOverrides = new HashMap<>();
    private final Map<String, Function<T, float[]>> tintOverrides = new HashMap<>();
    private final Map<String, float[]> tintFalloffs = new HashMap<>();
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
        parts.add(new MachineSpec.PartDef<>(name, name, null, null, null, false, id + "/" + name, lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name)));
        return this;
    }

    /** Анимированная часть: {@link PartAnimator} задаёт трансформ относительно блока. */
    public MachineSpecBuilder<T> part(String name, PartAnimator<T> animator) {
        parts.add(new MachineSpec.PartDef<>(name, name, animator, null, null, true, id + "/" + name, lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name)));
        return this;
    }

    /**
     * Анимированная часть, ссылающаяся на чужую модель: несколько логических частей
     * поверх одной части OBJ (например, 4 шестерни из части "Cog").
     */
    public MachineSpecBuilder<T> part(String modelPartName, String name, PartAnimator<T> animator) {
        parts.add(new MachineSpec.PartDef<>(name, modelPartName, animator, null, null, true, id + "/" + name, lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name)));
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
     * Per-instance RGBA-тинт части ({r,g,b,a}; null/белый RGB = passthrough) — раскалённые
     * сопла, светящиеся окна и т.п. Пишется в рекорд инстанса (attrib 13 InstColor),
     * меняется КАЖДЫЙ КАДР без пересборки VBO; RGB может быть &gt; 1 — overbright-накал.
     * <b>Альфа = сила эмиссии (heat 0..1), НЕ прозрачность</b>: шейдеры доворачивают
     * lightmap к fullbright на alpha*фоллофф ({@link #tintFalloff}), поэтому свечение
     * следует тинту пространственно. Вызывать ДО объявления part()/dynamicPart().
     * <p>
     * Семантика обновления — как у lightOverride: анимированные части получают свежий
     * тинт каждый кадр; статические — только при полном пересборе (dirty/TTL) — для
     * плавно меняющегося тинта на статичной части машина должна звать
     * {@code markRenderDirty()} при смене значения.
     */
    public MachineSpecBuilder<T> tintOverride(String partName, Function<T, float[]> fn) {
        tintOverrides.put(partName, fn);
        return this;
    }

    /**
     * Пространственный фоллофф тинта части: плавное затухание по оси модели (0=x, 1=y,
     * 2=z) от {@code fullCoord} (тинт полный, «источник» — например срез сопла) до
     * {@code zeroCoord} (тинт нулевой). Считается в вершинном шейдере по МОДЕЛЬНЫМ
     * координатам вершины, поэтому одной настройкой покрывает всю часть/модель без
     * пересборки VBO. Вызывать ДО объявления part()/dynamicPart() с этим именем.
     */
    public MachineSpecBuilder<T> tintFalloff(String partName, float axis, float fullCoord, float zeroCoord) {
        tintFalloffs.put(partName, new float[] {axis, fullCoord, zeroCoord, 0.0F});
        return this;
    }

    /**
     * Статическая часть с фиксированным трансформом-«аниматором» (легаси-офсеты запечки:
     * T(-0.5,0,-0.5), yaw-группы). НЕ гейтится по modelUpdateDistance — живёт до
     * статической отсечки, как обычная статика.
     */
    public MachineSpecBuilder<T> staticPart(String name, PartAnimator<T> transform) {
        parts.add(new MachineSpec.PartDef<>(name, name, transform, null, null, false, id + "/" + name, lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name)));
        return this;
    }

    /**
     * Динамическая часть с per-BE геометрией (стены флюид-танка по флюиду, DAE-ноды).
     * VBO кешируется по ключу {@code cacheKeyFn}; возвращаемый квад-лист может быть пустым.
     */
    public MachineSpecBuilder<T> dynamicPart(String name, QuadResolver<T> quads, Function<T, String> cacheKeyFn) {
        parts.add(new MachineSpec.PartDef<>(name, name, null, quads, cacheKeyFn, false, id + "/" + name, lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name)));
        return this;
    }

    /** Динамическая часть с per-BE геометрией И анимацией (например, dish крупного/малого радара). */
    public MachineSpecBuilder<T> dynamicPart(String name, PartAnimator<T> animator,
                                             QuadResolver<T> quads, Function<T, String> cacheKeyFn) {
        parts.add(new MachineSpec.PartDef<>(name, name, animator, quads, cacheKeyFn, true, id + "/" + name, lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name)));
        return this;
    }

    /**
     * Динамическая часть с per-BE геометрией и фиксированным трансформом-«аниматором»
     * (легаси-офсеты). Контент статичен (меняется по состоянию BE, не по времени) —
     * НЕ гейтится по modelUpdateDistance.
     */
    public MachineSpecBuilder<T> dynamicPart(String name, QuadResolver<T> quads, Function<T, String> cacheKeyFn,
                                             PartAnimator<T> transform) {
        parts.add(new MachineSpec.PartDef<>(name, name, transform, quads, cacheKeyFn, false, id + "/" + name, lightOverrides.get(name), null, tintOverrides.get(name), tintFalloffs.get(name)));
        return this;
    }

    /**
     * Динамическая часть с uvRect-резолвером: VBO части хранит sprite-local [0..1] UV,
     * а резолвер отдаёт per-BE {u0,v0,du,dv} ремапа в атлас (текстурно-вариантные
     * части дверей: один VBO на геометрию, скин — в per-instance uvRect).
     */
    public MachineSpecBuilder<T> dynamicPart(String name, QuadResolver<T> quads, Function<T, String> cacheKeyFn,
                                             PartAnimator<T> transform, Function<T, float[]> uvRectFn) {
        parts.add(new MachineSpec.PartDef<>(name, name, transform, quads, cacheKeyFn, false, id + "/" + name, lightOverrides.get(name), uvRectFn, tintOverrides.get(name), tintFalloffs.get(name)));
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
        MachineSpec<T> spec = new MachineSpec<>(id, beClass, type, modelResolver, facingResolver,
                parts, hooks, viewDistance, blockTransform, itemParts, itemExcept, chunkRenderTypes);
        BlockEntityRenderers.register(type, ctx -> new MachineBer<>(spec));
        MachineRenderRegistry.register(spec);
        // Байпас диспетчера: только типы фабрики MachineRenderers (машины Nucleus).
        com.hbm_m.client.render.NucleusDispatcherBypass.registerManaged(type);
    }
}
