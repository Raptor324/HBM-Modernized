package com.hbm_m.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * Прямой наследник ванильного {@link MultiBufferSource.BufferSource} — в
 * обход фабрики {@code MultiBufferSource.immediate/immediateWithBuffers}.
 *
 * ЗАЧЕМ: ImmediatelyFast редиректит эти фабрики и подменяет ЛЮБОЙ созданный
 * ими источник на свой BatchableBufferSource, чей no-arg endBatch() флашит
 * все зарегистрированные слои безусловно — и падает
 * «Sorting state uninitialized» на ПУСТОМ батче RenderType с
 * sortOnUpload=true (наши nuke_clouds/nuke_flash, когда фильтр near/far или
 * flash-only не записал в тип ни одной вершины). Поэтому ВСЕ наши кастомные
 * sortOnUpload-рендертайпы должны рисоваться только через источник этого
 * класса (см. ParticleEngineNT.buffer()).
 *
 * ВТОРОЕ (главное): на 1.20.1 мы НЕ используем ванильную бухгалтерию батчей
 * (lastState + startedBuffers), а ведём СВОЮ симметричную: begin(X) при
 * первом getBuffer(X), RenderType.end(X) строго в паре — в endCurrentBatch().
 * У ванильной машины есть пути рассинхрона (например endBatch(rt) при
 * lastState != rt просто скипается по startedBuffers.remove() == false,
 * оставляя lastState указывающим на уже закрытый батч; после такого
 * skip-а последующий end() может увидеть sorting-состояние прошлого батча —
 * отсюда «Sorting state uninitialized»). При симметричной паре состояние
 * билдера всегда согласовано с типом: end() вызывается только для типа,
 * чей begin открыт, и сортировка (попиксельная, по дистанции до камеры)
 * работает как в ваниле. Пустой батч не рисуется вовсе
 * (endOrDiscardIfEmpty — терять нечего, вершин ноль).
 */
public class PlainBufferSource extends MultiBufferSource.BufferSource {

    //? if < 1.21.1 {
    /** Тип, чей begin() сейчас открыт на shared-билдере; null — ничего. */
    private RenderType buildingType;

    public PlainBufferSource(com.mojang.blaze3d.vertex.BufferBuilder sharedBuffer) {
        super(sharedBuffer, com.google.common.collect.ImmutableMap.of());
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        if (this.buildingType != renderType) {
            endCurrentBatch();
            this.builder.begin(renderType.mode(), renderType.format());
            this.buildingType = renderType;
        }
        return this.builder;
    }

    @Override
    public void endBatch(RenderType renderType) {
        if (this.buildingType == renderType) {
            endCurrentBatch();
        }
    }

    @Override
    public void endLastBatch() {
        endCurrentBatch();
    }

    @Override
    public void endBatch() {
        endCurrentBatch();
    }

    private void endCurrentBatch() {
        RenderType type = this.buildingType;
        if (type == null) {
            return;
        }
        this.buildingType = null;
        if (this.builder.isCurrentBatchEmpty()) {
            // Пустой батч: сбрасываем билдер без отрисовки (и без
            // сортировочной машины — именно пустые sortOnUpload-батчи и были
            // источником «Sorting state uninitialized» под ImmediatelyFast).
            this.builder.endOrDiscardIfEmpty();
        } else {
            // Полный ванильный путь отрисовки: setQuadSorting (если тип
            // сортируемый) -> end -> setupRenderState -> drawWithShader.
            type.end(this.builder, com.mojang.blaze3d.systems.RenderSystem.getVertexSorting());
        }
    }
    //?} else {
    /*// 1.21.1: ванильный BufferSource уже хранит отдельный BufferBuilder на
    // тип (startedBuilders) и сортирует MeshData.sortQuads — уязвимой
    // разделяемой машины 1.20.1 там нет; достаточно честного наследника.
    public PlainBufferSource(com.mojang.blaze3d.vertex.ByteBufferBuilder sharedBuffer) {
        super(sharedBuffer, java.util.Collections.emptySortedMap());
    }
    *///?}
}
