package com.hbm_m.particle.nt;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Custom particle base (not tied to vanilla ParticleEngine).
 * Rendered by ParticleEngineNT after weather so effects are not clipped by depth.
 */
public abstract class ParticleNT {
    private static final AABB INITIAL_AABB = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    private static final double MAXIMUM_COLLISION_VELOCITY_SQUARED = Mth.square(100.0);

    protected final ClientLevel level;
    public double xo, yo, zo;
    public double x, y, z;
    public double xd, yd, zd;
    public float quadSize;
    private AABB bb;
    public boolean onGround;
    public boolean noClip;
    public boolean dead;
    protected float bbWidth = 0.6F;
    protected float bbHeight = 1.8F;
    protected final RandomSource random;
    public int age;
    public int lifetime;
    public float gravity;
    public float rCol = 1.0F, gCol = 1.0F, bCol = 1.0F, alpha = 1.0F;
    protected float roll, oRoll;
    protected float friction = 0.98F;
    public boolean verticalCollision, horizontalCollision;
    protected boolean speedUpWhenYMotionIsBlocked = false;

    protected ParticleNT(ClientLevel level, double x, double y, double z) {
        this.bb = INITIAL_AABB;
        this.noClip = false;
        this.level = level;
        this.random = RandomSource.create();
        setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.lifetime = (int) (4.0F / (this.random.nextFloat() * 0.9F + 0.1F));
        this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;
        this.verticalCollision = false;
        this.horizontalCollision = false;
    }

    public ParticleNT(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        this(level, x, y, z);
        this.xd = xSpeed + (Math.random() * 2.0 - 1.0) * 0.4;
        this.yd = ySpeed + (Math.random() * 2.0 - 1.0) * 0.4;
        this.zd = zSpeed + (Math.random() * 2.0 - 1.0) * 0.4;
        double d0 = (Math.random() + Math.random() + 1.0) * 0.15;
        double d1 = Math.sqrt(this.xd * this.xd + this.yd * this.yd + this.zd * this.zd);
        this.xd = this.xd / d1 * d0 * 0.4;
        this.yd = this.yd / d1 * d0 * 0.4 + 0.1;
        this.zd = this.zd / d1 * d0 * 0.4;
    }

    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.dead = true;
        } else {
            this.yd -= 0.04 * this.gravity;
            this.move(this.xd, this.yd, this.zd);
            if (this.speedUpWhenYMotionIsBlocked && this.y == this.yo) {
                this.xd *= 1.1;
                this.zd *= 1.1;
            }
            this.xd *= this.friction;
            this.yd *= this.friction;
            this.zd *= this.friction;
            if (this.onGround) {
                this.xd *= 0.7F;
                this.zd *= 0.7F;
            }
        }
    }

    public abstract void render(VertexConsumer consumer, Camera camera, float partialTicks, PoseStack levelPoseStack);

    /** Рендер flash-частицы поверх остальных (по умолчанию пусто; NukeTorex переопределяет). */
    public void renderFlashOnly(MultiBufferSource buffer, Camera camera, float partialTicks, PoseStack levelPoseStack) {}

    public abstract RenderType getRenderType();

    public void setPos(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        double f = this.bbWidth / 2.0;
        double f1 = this.bbHeight;
        this.setBoundingBox(new AABB(x - f, y, z - f, x + f, y + f1, z + f));
    }

    public void move(double x, double y, double z) {
        double d0 = x, d1 = y, d2 = z;
        if (!this.noClip && (x != 0 || y != 0 || z != 0) && x * x + y * y + z * z < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
            Vec3 pos = new Vec3(x, y, z);
            Vec3 vec3 = Entity.collideBoundingBox(null, pos, this.getBoundingBox(), this.level, List.of());
            this.horizontalCollision = !Mth.equal(pos.x, vec3.x) || !Mth.equal(pos.z, vec3.z);
            this.verticalCollision = pos.y != vec3.y;
            x = vec3.x;
            y = vec3.y;
            z = vec3.z;
        }
        if (x != 0 || y != 0 || z != 0) {
            this.setBoundingBox(this.getBoundingBox().move(x, y, z));
            this.setLocationFromBoundingbox();
        }
        this.onGround = d1 != y && d1 < 0.0;
        if (this.onGround) {
            this.xd = 0;
            this.yd = 0;
            this.zd = 0;
        } else {
            if (d0 != x) this.xd = 0;
            if (d2 != z) this.zd = 0;
        }
    }

    protected void setLocationFromBoundingbox() {
        AABB aabb = this.getBoundingBox();
        this.x = (aabb.minX + aabb.maxX) / 2.0;
        this.y = aabb.minY;
        this.z = (aabb.minZ + aabb.maxZ) / 2.0;
    }

    protected int getLightColor() {
        BlockPos blockpos = BlockPos.containing(this.x, this.y, this.z);
        return this.level.hasChunkAt(blockpos) ? LevelRenderer.getLightColor(this.level, blockpos) : 0;
    }

    public AABB getBoundingBox() { return this.bb; }

    public void setBoundingBox(AABB bb) { this.bb = bb; }

    public void remove() { this.dead = true; }

    /**
     * Смещение относительно камеры с FALLBACK-ВИРТУАЛИЗАЦИЕЙ.
     *
     * Когда DH рендерит — возвращается истинное смещение: дальний контент
     * рисуется в проходе EngineHandler с удлинённой проекцией (нет клипа).
     * Когда DH НЕ рендерит (не установлен / выключен / завис) — контент за
     * границей безопасной дистанции приближается к камере (старый виртуальный
     * рендер) и остаётся видим в ванильной проекции с её коротким far plane.
     */
    protected Vec3 virtualizedOffset(double wx, double wy, double wz, Camera camera) {
        Vec3 camPos = camera.getPosition();
        double rx = wx - camPos.x;
        double ry = wy - camPos.y;
        double rz = wz - camPos.z;
        boolean dhActive = com.hbm_m.client.compat.dh.DhClientState.isActive();
        if (!dhActive) {
            double max = com.hbm_m.client.missile.track.MissileTrackWorldRender.maxSafeRenderDistanceBlocks();
            double dSq = rx * rx + ry * ry + rz * rz;
            if (dSq > max * max && dSq > 1.0E-8D) {
                // Диагностика: если виртуализация стреляет при активном DH,
                // это видно здесь (dhActive должен быть false в этом логе).
                if (++virtualizeDiagCounter % 300 == 1) {
                    com.hbm_m.main.MainRegistry.LOGGER.info(
                            "HBM NT virtualize: {} dist={} max={} dhActive={}",
                            this.getClass().getSimpleName(),
                            (long) Math.sqrt(dSq), (long) max, dhActive);
                }
                double s = max / Math.sqrt(dSq);
                return new Vec3(rx * s, ry * s, rz * s);
            }
        }
        return new Vec3(rx, ry, rz);
    }

    /**
     * Коэффициент сжатия виртуализации (1.0 = истинная позиция и размер).
     *
     * Виртуализация приближает контент к границе прорисовки; чтобы сохранилась
     * перспективная иллюзия «объект действительно далеко», угловой размер
     * должен продолжать уменьшаться с дистанцией → геометрию нужно домножать
     * на тот же коэффициент max/dist, что и смещение (virtualizedOffset).
     */
    protected float virtualScale(double wx, double wy, double wz, Camera camera) {
        boolean dhActive = com.hbm_m.client.compat.dh.DhClientState.isActive();
        if (dhActive) return 1.0F;
        Vec3 camPos = camera.getPosition();
        double rx = wx - camPos.x, ry = wy - camPos.y, rz = wz - camPos.z;
        double dSq = rx * rx + ry * ry + rz * rz;
        double max = com.hbm_m.client.missile.track.MissileTrackWorldRender.maxSafeRenderDistanceBlocks();
        if (dSq > max * max && dSq > 1.0E-8D) {
            return (float) (max / Math.sqrt(dSq));
        }
        return 1.0F;
    }

    private static int virtualizeDiagCounter = 0;
}
