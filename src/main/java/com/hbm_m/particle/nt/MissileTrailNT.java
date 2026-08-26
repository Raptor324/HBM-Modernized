package com.hbm_m.particle.nt;

import com.hbm_m.client.missile.track.MissileTrackWorldRender;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * База ракетных трейловых частиц в NT-движке (ParticleEngineNT).
 *
 * Зачем порт: ванильные частицы рендерятся ванильной проекцией и клипаются
 * far plane'ом (~RD*16*4 блоков) — след баллистической ракеты обрывался на
 * границе прорисовки. NT-частицы рисуются в нашем пайплайне EngineHandler
 * (AFTER_WEATHER): при активном DH — в проходе с удлинённой проекцией без
 * клипа; виртуализация при этом отключена за ненадобностью. Без DH
 * используется старая виртуализация ({@link #virtualize}).
 */
public abstract class MissileTrailNT extends ParticleNT {

    protected MissileTrailNT(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        // Трейлы летят высоко над рельефом — коллизии не нужны и дорогостоящи.
        this.noClip = true;
    }

    /** Кэш UV спрайта из атласа частиц (заполняется через {@link #cacheSpriteUv}). */
    protected float u0, v0, u1, v1;

    protected void cacheSpriteUv(TextureAtlasSprite sprite) {
        this.u0 = sprite.getU0();
        this.u1 = sprite.getU1();
        this.v0 = sprite.getV0();
        this.v1 = sprite.getV1();
    }

    /**
     * Камера-relative позиция с учётом виртуализации для случая «DH нет»:
     * при активном DH virtualizeWorld возвращает истинное смещение (масштаб 1),
     * без DH — приближает далёкие сегменты к границе прорисовки.
     */
    protected Vec3 virtualize(double wx, double wy, double wz, Camera camera) {
        MissileTrackWorldRender.CameraRelativePose pose =
                MissileTrackWorldRender.virtualizeWorld(wx, wy, wz, camera.getPosition());
        return new Vec3(pose.relX(), pose.relY(), pose.relZ());
    }

    /**
     * Эмиссия билборд-квада в формате POSITION_TEX_COLOR (рендертайпы семейства
     * nukeClouds): поворот от камеры, порядок вершин/UV совпадает со старыми
     * ванильными версиями этих частиц.
     */
    protected void emitBillboard(VertexConsumer consumer, Camera camera,
                                 float px, float py, float pz, float halfSize,
                                 float r, float g, float b, float a) {
        Quaternionf rotation = camera.rotation();
        Vector3f c0 = new Vector3f(-halfSize, -halfSize, 0.0F).rotate(rotation);
        Vector3f c1 = new Vector3f(-halfSize, halfSize, 0.0F).rotate(rotation);
        Vector3f c2 = new Vector3f(halfSize, halfSize, 0.0F).rotate(rotation);
        Vector3f c3 = new Vector3f(halfSize, -halfSize, 0.0F).rotate(rotation);

        com.hbm_m.client.render.ImmediateVertexWriter.worldQuad(
                consumer,
                px + c0.x(), py + c0.y(), pz + c0.z(),
                px + c1.x(), py + c1.y(), pz + c1.z(),
                px + c2.x(), py + c2.y(), pz + c2.z(),
                px + c3.x(), py + c3.y(), pz + c3.z(),
                r, g, b, a,
                this.u0, this.v0, this.u1, this.v1);
    }

    /** Первый спрайт набора (набор контрейла однослойный по факту). */
    protected static TextureAtlasSprite randomSprite(SpriteSet sprites, net.minecraft.util.RandomSource random) {
        return sprites.get(0, 1);
    }
}
