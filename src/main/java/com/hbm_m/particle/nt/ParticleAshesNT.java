package com.hbm_m.particle.nt;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Порт {@code ParticleAshes} из HBM 1.7.10.
 * Кусочек пепла: в полёте — повёрнутый к камере билборд, на земле —
 * плоский квад, повёрнутый случайно по yaw. Текстура = particle_base.
 *
 * Паттерн рендера как у {@link ParticleSkeletonNT} / Muke*: вершины пишутся
 * сразу в пространстве камеры (consumer.vertex без PoseStack).
 */
public class ParticleAshesNT extends ParticleNT {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            com.hbm_m.lib.RefStrings.MODID, "textures/particle/particle_base.png");
    private static final float FADE_TICKS = 40F;

    private final float scale0;
    private final float grey;
    private final float spin;      // направление/скорость вращения в полёте
    private final int smokeSlot;   // детерминированный слот: коптит ровно пятая часть пепла (parity: id % 5 == 0)
    private float groundYaw;       // случайный yaw после приземления

    public ParticleAshesNT(ClientLevel level, double x, double y, double z, float scale) {
        super(level, x, y, z);
        this.lifetime = 1200 + random.nextInt(20);
        this.scale0 = scale * 0.9F + random.nextFloat() * 0.2F;
        this.gravity = 1.0F; // unused; см. tick: 0.01 как в оригинале
        this.grey = random.nextFloat() * 0.1F + 0.1F;
        this.spin = 2.0F * (random.nextFloat() < 0.5F ? -0.5F : 0.5F); // id % 2 в оригинале — равновероятно ±0.5 → *2 = ±1
        this.smokeSlot = random.nextInt(5);
        this.groundYaw = 0.0F;
        this.bbWidth = 0.05F;
        this.bbHeight = 0.05F;
        setPos(x, y, z); // применяет bbWidth/bbHeight к AABB
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;

        if (this.age++ >= this.lifetime) {
            this.dead = true;
            return;
        }

        boolean wasOnGround = this.onGround;
        this.yd -= 0.01F; // particleGravity = 0.01

        if (!this.onGround) {
            this.roll += this.spin;
        }

        this.xd *= 0.95D;
        this.yd *= 0.99D;
        this.zd *= 0.95D;

        this.move(this.xd, this.yd, this.zd);

        // Оригинал 1.7.10: yaw на земле задаётся ОДИН раз в момент приземления;
        // флаг onGround при нулевом движении частицы должен сохраняться (moveEntity делал это).
        // ParticleNT.move() сбрасывает onGround, если скорость не направлена вниз. Костыль: проверяем wasOnGround.
        if (!wasOnGround && this.onGround) {
            this.groundYaw = random.nextFloat() * 360F;
        }
        if (wasOnGround && !this.onGround) {
            // Были на земле прошлый тик, не летим вверх — удерживаем onGround=true.
            if (this.yd <= 0) {
                this.onGround = true;
                this.yd = 0;
                this.xd = 0;
                this.zd = 0;
            }
        }

        // Чёрный дымок от неподвижного пепла, как в оригинале: ровно пятая часть пепла коптит, шанс 1/15 на тик.
        if (this.smokeSlot == 0 && this.onGround && random.nextInt(15) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, this.x, this.y + 0.125, this.z, 0.0, 0.05, 0.0);
        }
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks, PoseStack levelPoseStack) {
        Vec3 off = virtualizedOffset(
                Mth.lerp(partialTicks, this.xo, this.x),
                Mth.lerp(partialTicks, this.yo, this.y),
                Mth.lerp(partialTicks, this.zo, this.z),
                camera);
        float pX = (float) off.x;
        float pY = (float) off.y;
        float pZ = (float) off.z;

        float timeLeft = this.lifetime - (this.age + partialTicks);
        float alpha = timeLeft < FADE_TICKS ? Mth.clamp(timeLeft / FADE_TICKS, 0F, 1F) : 1F;

        int light = getLightColor();
        int cr = Mth.clamp((int) (this.grey * 255F), 0, 255);
        int cg = cr;
        int cb = cr;
        int ca = Mth.clamp((int) (alpha * 255F), 0, 255);

        // Fallback-виртуализация: сжатие размера вместе со смещением (см. ParticleNT.virtualScale).
        float vScale = virtualScale(
                Mth.lerp(partialTicks, this.xo, this.x),
                Mth.lerp(partialTicks, this.yo, this.y),
                Mth.lerp(partialTicks, this.zo, this.z),
                camera);
        float s = this.scale0 * vScale;

        if (this.onGround) {
            // Плоский квад на земле: вектор (scale, 0, scale) повёрнут по yaw 4 раза
            emitGroundQuad(consumer, pX, pY + 0.05F, pZ, s, this.groundYaw, light, cr, cg, cb, ca);
        } else {
            // Билборд к камере + вращение вокруг своей нормали
            float roll = Mth.lerp(partialTicks, this.oRoll, this.roll);
            emitBillboardQuad(consumer, camera, pX, pY, pZ, s, roll, light, cr, cg, cb, ca);
        }
    }

    /**
     * Квад, лежащий в плоскости XZ: вектор (s,0,s) повёрнут на yaw*4 по 90°.
     * Y одинаковый у всех 4 вершин (передаётся вызывающим как pY+0.05).
     */
    private static void emitGroundQuad(VertexConsumer consumer, float pX, float pY, float pZ,
                                       float s, float yawDeg, int light, int r, int g, int b, int a) {
        float cy = Mth.cos(yawDeg * Mth.DEG_TO_RAD);
        float sy = Mth.sin(yawDeg * Mth.DEG_TO_RAD);
        // Начальный вектор (s, s) в XZ, повёрнутый на yaw, потом на 90° к циклу.
        float vx = s, vz = s;
        for (int i = 0; i < 4; i++) {
            float rx = vx * cy + vz * sy;
            float rz = -vx * sy + vz * cy;
            float u = (i == 0 || i == 1) ? 1F : 0F; // 0,1 -> MaxU; 2,3 -> MinU
            float v = (i == 0 || i == 3) ? 1F : 0F; // 0,3 -> MaxV; 1,2 -> MinV
            // Format = POSITION_COLOR_TEX_LIGHTMAP: position, color, UV0, UV2
            //? if < 1.21.1 {
            consumer.vertex(pX + rx, pY, pZ + rz)
                    .color(r, g, b, a)
                    .uv(u, v)
                    .uv2(light)
                    .endVertex();
            //?} else {
            /*// 1.21.1: vertex->addVertex, color->setColor, uv->setUv, uv2->setLight, endVertex удалён.
            consumer.addVertex(pX + rx, pY, pZ + rz)
                    .setColor(r, g, b, a)
                    .setUv(u, v)
                    .setLight(light);
            *///?}
            // повернуть (vx, vz) на +90°: (x,z) -> (-z, x)
            float nx = -vz, nz = vx;
            vx = nx; vz = nz;
        }
    }

    /**
     * Билборд к камере с вращением roll в плоскости экрана.
     * Базис: left (наклонён к игроку), up (мировой Y), как в EntityFXRotating.
     */
    private static void emitBillboardQuad(VertexConsumer consumer, Camera camera, float pX, float pY, float pZ,
                                          float s, float rollDeg, int light, int r, int g, int b, int a) {
        org.joml.Quaternionf camQ = new org.joml.Quaternionf(camera.rotation());
        org.joml.Vector3f left = new org.joml.Vector3f(-1, 0, 0).rotate(camQ); // вправо от камеры
        org.joml.Vector3f up   = new org.joml.Vector3f( 0, 1, 0);

        float crRad = rollDeg * Mth.DEG_TO_RAD;
        float rc = Mth.cos(crRad), rs = Mth.sin(crRad);

        // Углы как в оригинале: (+left+up), (-left+up), (-left-up), (+left-up), умноженные на s,
        // затем повернуты в плоскости экрана на roll.
        float[][] uv2d = { { 1,  1}, { -1,  1}, { -1, -1}, { 1, -1} };
        for (float[] corner : uv2d) {
            float cx = corner[0] * s * rc - corner[1] * s * rs;
            float cyScreen = corner[0] * s * rs + corner[1] * s * rc;
            float ox = left.x * cx + up.x * cyScreen;
            float oy = left.y * cx + up.y * cyScreen;
            float oz = left.z * cx + up.z * cyScreen;
            // Format = POSITION_COLOR_TEX_LIGHTMAP: position, color, UV0, UV2
            //? if < 1.21.1 {
            consumer.vertex(pX + ox, pY + oy, pZ + oz)
                    .color(r, g, b, a)
                    .uv(corner[0] * 0.5F + 0.5F, corner[1] * 0.5F + 0.5F)
                    .uv2(light)
                    .endVertex();
            //?} else {
            /*// 1.21.1: vertex->addVertex, color->setColor, uv->setUv, uv2->setLight, endVertex удалён.
            consumer.addVertex(pX + ox, pY + oy, pZ + oz)
                    .setColor(r, g, b, a)
                    .setUv(corner[0] * 0.5F + 0.5F, corner[1] * 0.5F + 0.5F)
                    .setLight(light);
            *///?}
        }
    }

    @Override
    public RenderType getRenderType() {
        // QUADS-вариант lightmap-рендертипа с той же формулой 770/771 как в 1.7.10.
        return com.hbm_m.client.ClientRenderHandler.CustomRenderTypes.ASHES_PARTICLES.apply(TEXTURE);
    }
}
