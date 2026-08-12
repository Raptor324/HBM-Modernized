package com.hbm_m.particle.nt;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Клиентская частица «кости» — падающая часть скелета (череп, туловище, конечность).
 * Порт {@code ParticleSkeleton} из HBM 1.7.10 под базовый класс {@link ParticleNT}.
 *
 * Модель: {@code assets/hbm_m/models/effect/skeleton.obj}, части "Skull", "Torso", "Limb", "SkullVillager".
 * Текстура: {@code assets/hbm_m/textures/particle/skeleton.png} / {@code skoilet.png}.
 */
public class ParticleSkeletonNT extends ParticleNT {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/particle/skeleton.png");
    private static final ResourceLocation TEXTURE_EXT = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/particle/skoilet.png");
    private static final ResourceLocation SKELETON_OBJ = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "models/effect/skeleton.obj");

    private static volatile boolean loaded = false;
    private static final Map<String, List<Tri>> PARTS = new HashMap<>();

    public enum BoneKind {
        // Ванильный baby-humanoid: голова ×0.75 к телу ×0.5. У baby-жителя голова чуть
        // меньше (0.6), иначе череп вилладжера с носом выглядит непропорционально огромным.
        SKULL(0.75F),
        TORSO(0.5F),
        LIMB(0.5F),
        SKULL_VILLAGER(0.6F);

        /** Масштаб кости у baby-моба: тело ужато вдвое, голова крупнее тела, но не полная. */
        private final float babyScale;

        BoneKind(float babyScale) {
            this.babyScale = babyScale;
        }

        public float babyScale() {
            return babyScale;
        }
    }

    private final BoneKind kind;
    private float rCol0, gCol0, bCol0;
    /** Масштаб кости: 1.0 обычный, 0.5 baby-версии тела; детский череп = 1.0 (ванильная схема baby). */
    private float scale = 1.0F;
    private float momentumYaw;
    private float momentumPitch;
    private int initialDelay = 20;

    private float yaw, pitch, prevYaw, prevPitch;

    private static final int MAX_AGE_BASE = 1200;
    private static final int FADE_TICKS = 40;

    public ParticleSkeletonNT(ClientLevel level, double x, double y, double z,
                              float r, float g, float b, BoneKind kind) {
        this(level, x, y, z, r, g, b, kind, 1.0F);
    }

    public ParticleSkeletonNT(ClientLevel level, double x, double y, double z,
                              float r, float g, float b, BoneKind kind, float scale) {
        super(level, x, y, z);
        this.kind = kind;
        this.scale = scale;
        this.rCol0 = r;
        this.gCol0 = g;
        this.bCol0 = b;
        this.lifetime = MAX_AGE_BASE + this.random.nextInt(20);
        // gravity stays default 0; vertical motion applied manually below with custom coefficient 0.02
        this.noClip = false;
        this.bbWidth = 0.3F;
        this.bbHeight = 0.3F;
        setPos(x, y, z);
        this.momentumPitch = this.random.nextFloat() * 5 * (this.random.nextBoolean() ? 1 : -1);
        this.momentumYaw = this.random.nextFloat() * 5 * (this.random.nextBoolean() ? 1 : -1);
    }

    public void setRotation(float yaw, float pitch) {
        this.yaw = this.prevYaw = yaw;
        this.pitch = this.prevPitch = pitch;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.prevYaw = this.yaw;
        this.prevPitch = this.pitch;

        if (initialDelay-- > 0) return;

        if (initialDelay == -1) {
            this.xd = (this.random.nextGaussian()) * 0.025;
            this.zd = (this.random.nextGaussian()) * 0.025;
        }

        if (this.age++ >= this.lifetime) {
            this.dead = true;
            return;
        }

        boolean wasOnGround = this.onGround;

        this.yd -= 0.02; // 1.7.10 particleGravity = 0.02
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.98D;
        this.yd *= 0.98D;
        this.zd *= 0.98D;

        if (!this.onGround) {
            this.pitch += this.momentumPitch;
            this.yaw += this.momentumYaw;
        } else {
            this.xd = 0;
            this.yd = 0;
            this.zd = 0;
            if (!wasOnGround) {
                level.playLocalSound(this.x, this.y, this.z,
                        SoundEvents.SKELETON_HURT, SoundSource.HOSTILE,
                        0.25F, 0.8F + this.random.nextFloat() * 0.4F, false);
            }
        }
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks, PoseStack levelPoseStack) {
        // ВАЖНО: во время initialDelay кость РЕНДЕРИТСЯ стоя (как статуя),
        // а не скрывается. Скрытие давало баг «моб умер, секунду пусто, потом скелет упал».

        ensureLoaded();
        List<Tri> tris = PARTS.get(objGroupFor(kind));
        if (tris == null || tris.isEmpty()) return;

        // Правильное освещение 1.7.10: в блоке замаскируем позицию губ сущности:
        BlockPos lightPos = BlockPos.containing(this.x, this.y + 0.5, this.z);
        int packedLight = level.hasChunkAt(lightPos) ? LevelRenderer.getLightColor(level, lightPos) : 0;
        // 1.7.10: brightness passed через r/g/b и задействован в glColor4f; движок умножает это на lightmap.

        // Камера-относительная позиция: вершины уже в пространстве камеры, PoseStack не нужен.
        // Паттернкак у MukeWaveParticle / MukeCloudParticle — матрицы не трогаем.
        Vec3 camPos = camera.getPosition();
        float pX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x);
        float pY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y);
        float pZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z);

        float timeLeft = this.lifetime - (this.age + partialTicks);
        float alpha = timeLeft < FADE_TICKS ? Mth.clamp(timeLeft / FADE_TICKS, 0F, 1F) : 1F;

        float interpYaw = Mth.lerp(partialTicks, this.prevYaw, this.yaw);
        float interpPitch = Mth.lerp(partialTicks, this.prevPitch, this.pitch);

        int cr = Mth.clamp((int) (this.rCol0 * 255F), 0, 255);
        int cg = Mth.clamp((int) (this.gCol0 * 255F), 0, 255);
        int cb = Mth.clamp((int) (this.bCol0 * 255F), 0, 255);
        int ca = Mth.clamp((int) (alpha * 255F), 0, 255);

        // Вершины OBJ в локальном пространстве кости. Преобразуем вручную:
        // 1) Yaw вокруг Y (interpYaw), 2) Pitch вокруг X (interpPitch), 3) -90 yaw,
        // затем смещаем к позиции относительно камеры: pX + x, pY + y, pZ + z.
        float cy = Mth.cos(interpYaw * Mth.DEG_TO_RAD);
        float sy = Mth.sin(interpYaw * Mth.DEG_TO_RAD);
        float cp = Mth.cos(interpPitch * Mth.DEG_TO_RAD);
        float sp = Mth.sin(interpPitch * Mth.DEG_TO_RAD);

        for (Tri tri : tris) {
            // Нормаль грани — в том же преобразовании (RotY/Pitch) что и вершины.
            // Для NEW_ENTITY-шейдера (освещение по нормалям) + back-face culling её нужно задавать.
            float[] nrm = rotatedFaceNormal(tri, cy, sy, cp, sp);
            emitVertexCameraSpace(consumer, tri.a(), tri.ua(), pX, pY, pZ, cy, sy, cp, sp, scale, packedLight, cr, cg, cb, ca, nrm);
            emitVertexCameraSpace(consumer, tri.b(), tri.ub(), pX, pY, pZ, cy, sy, cp, sp, scale, packedLight, cr, cg, cb, ca, nrm);
            emitVertexCameraSpace(consumer, tri.c(), tri.uc(), pX, pY, pZ, cy, sy, cp, sp, scale, packedLight, cr, cg, cb, ca, nrm);
        }
    }

    /** Нормаль треугольника после поворотов: RotY(interpYaw) * RotX(interpPitch) * RotY(-90) * n_obj. */
    private static float[] rotatedFaceNormal(Tri tri, float cy, float sy, float cp, float sp) {
        Vert n = tri.normal();
        // Если нормали в OBJ не заданы — рассчитываем граньки.
        float nx = n.x(), ny = n.y(), nz = n.z();
        if (nx == 0F && ny == 0F && nz == 0F) {
            Vert a = tri.a(), b = tri.b(), c = tri.c();
            float ux = b.x() - a.x(), uy = b.y() - a.y(), uz = b.z() - a.z();
            float vx = c.x() - a.x(), vy = c.y() - a.y(), vz = c.z() - a.z();
            nx = uy * vz - uz * vy;
            ny = uz * vx - ux * vz;
            nz = ux * vy - uy * vx;
        }
        // RotY(-90): (x,y,z) -> (-z, y, x)
        float x1 = -nz, y1 = ny, z1 = nx;
        // RotX(pitch)
        float x2 = x1, y2 = y1 * cp - z1 * sp, z2 = y1 * sp + z1 * cp;
        // RotY(yaw)
        float x3 = x2 * cy + z2 * sy;
        float y3 = y2;
        float z3 = -x2 * sy + z2 * cy;
        float len = (float) Math.sqrt(x3 * x3 + y3 * y3 + z3 * z3);
        if (len < 1.0E-6F) return new float[]{0F, 1F, 0F};
        return new float[]{x3 / len, y3 / len, z3 / len};
    }

    /**
     * Emits one OBJ vertex already rotated and translated into camera-relative space.
     *
     * Order of rotations (matches 1.7.10 ParticleSkeleton):
     *   v = RotY(interpYaw) * RotX(interpPitch) * RotY(-90) * v_obj
     *   then translate by (pX, pY, pZ).
     *
     * consumer.vertex(x, y, z) is used WITHOUT PoseStack — just like MukeWaveParticle.
     */
    private static void emitVertexCameraSpace(VertexConsumer consumer, Vert v, UV uv,
                                               float pX, float pY, float pZ,
                                               float cy, float sy, float cp, float sp,
                                               float scale,
                                               int light, int r, int g, int b, int a,
                                               float[] nrm) {
        // RotY(-90) applied to OBJ coordinates first: x' = -z, y' = y, z' = x
        // (since RotY(-90): (x, y, z) -> (x*cos(-90) + z*sin(-90), y, -x*sin(-90) + z*cos(-90))
        //  = (x*0 + z*(-1), y, -x*1 + z*0) = (-z, y, x)
        float x1 = -v.z() * scale;
        float y1 = v.y() * scale;
        float z1 = v.x() * scale;

        // RotX(interpPitch): y' = y1*cp - z1*sp, z' = y1*sp + z1*cp
        float x2 = x1;
        float y2 = y1 * cp - z1 * sp;
        float z2 = y1 * sp + z1 * cp;

        // RotY(interpYaw): x' = x2*cy + z2*sy, z' = -x2*sy + z2*cy
        float x3 = x2 * cy + z2 * sy;
        float y3 = y2;
        float z3 = -x2 * sy + z2 * cy;

        // Format = NEW_ENTITY: position, color, UV0 (texture), overlay UV1, UV2 (lightmap), normal
        //? if < 1.21.1 {
        consumer.vertex(pX + x3, pY + y3, pZ + z3)
                .color(r, g, b, a)
                .uv(uv.u(), 1.0F - uv.v())
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(nrm[0], nrm[1], nrm[2])
                .endVertex();
        //?} else {
        /*// 1.21.1: vertex->addVertex, color->setColor, uv->setUv, overlayCoords->setOverlay,
        // uv2->setLight, normal->setNormal, endVertex удалён.
        consumer.addVertex(pX + x3, pY + y3, pZ + z3)
                .setColor(r, g, b, a)
                .setUv(uv.u(), 1.0F - uv.v())
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(nrm[0], nrm[1], nrm[2]);
        *///?}
    }

    @Override
    public RenderType getRenderType() {
        // 1.7.10 parity: blend SRC_ALPHA/ONE_MINUS_SRC_ALPHA + depth test LEQUAL + depth write ON + cull ON.
        // entityTranslucent has COLOR-only write, which is why bones didn't occlude each other in the old code.
        ResourceLocation useTexture = (kind == BoneKind.SKULL_VILLAGER) ? TEXTURE_EXT : TEXTURE;
        return com.hbm_m.client.ClientRenderHandler.CustomRenderTypes.SKELETON_PARTICLES.apply(useTexture);
    }

    private static String objGroupFor(BoneKind kind) {
        return switch (kind) {
            case SKULL -> "Skull";
            case TORSO -> "Torso";
            case LIMB -> "Limb";
            case SKULL_VILLAGER -> "SkullVillager";
        };
    }

    // --- OBJ loading (read all groups once, lazily) ---

    private record Vert(float x, float y, float z) {}
    private record UV(float u, float v) {}
    private record Tri(Vert a, Vert b, Vert c, UV ua, UV ub, UV uc, Vert normal) {}

    private static void ensureLoaded() {
        if (loaded) return;
        synchronized (ParticleSkeletonNT.class) {
            if (loaded) return;
            load(SKELETON_OBJ);
            loaded = true;
        }
    }

    private static void load(ResourceLocation obj) {
        try {
            Minecraft mc = Minecraft.getInstance();
            ResourceManager rm = mc.getResourceManager();
            Resource resource = rm.getResource(obj).orElse(null);
            if (resource == null) {
                MainRegistry.LOGGER.warn("Skeleton particle OBJ missing: {}", obj);
                return;
            }

            List<Vert> positions = new ArrayList<>();
            List<UV> uvs = new ArrayList<>();
            List<Vert> normals = new ArrayList<>();
            String currentGroup = null;
            String[] allGroups = {"Skull", "Torso", "Limb", "SkullVillager"};

            try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    if (line.startsWith("o ")) {
                        currentGroup = line.substring(2).trim();
                        continue;
                    }
                    if (line.startsWith("v ")) {
                        String[] p = line.split("\\s+");
                        positions.add(new Vert(Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])));
                        continue;
                    }
                    if (line.startsWith("vt ")) {
                        String[] p = line.split("\\s+");
                        uvs.add(new UV(Float.parseFloat(p[1]), Float.parseFloat(p[2])));
                        continue;
                    }
                    if (line.startsWith("vn ")) {
                        String[] p = line.split("\\s+");
                        normals.add(new Vert(Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])));
                        continue;
                    }
                    if (line.startsWith("f ")) {
                        if (currentGroup == null) continue;
                        boolean interesting = false;
                        for (String g : allGroups) if (g.equals(currentGroup)) { interesting = true; break; }
                        if (!interesting) continue;

                        String[] parts = line.substring(2).trim().split("\\s+");
                        if (parts.length < 3) continue;

                        Vert[] verts = new Vert[parts.length];
                        UV[] tex = new UV[parts.length];
                        Vert[] norms = new Vert[parts.length];

                        for (int i = 0; i < parts.length; i++) {
                            String[] idx = parts[i].split("/");
                            int vi = parseIndex(idx[0], positions.size());
                            int vti = idx.length > 1 && !idx[1].isEmpty() ? parseIndex(idx[1], uvs.size()) : 0;
                            int vni = idx.length > 2 && !idx[2].isEmpty() ? parseIndex(idx[2], normals.size()) : 0;
                            verts[i] = positions.get(vi - 1);
                            tex[i] = vti > 0 ? uvs.get(vti - 1) : new UV(0, 0);
                            norms[i] = vni > 0 ? normals.get(vni - 1) : new Vert(0, 1, 0);
                        }

                        for (int i = 1; i < parts.length - 1; i++) {
                            Vert n = faceNormal(verts[0], verts[i], verts[i + 1], norms[0], norms[i], norms[i + 1]);
                            Tri tri = new Tri(verts[0], verts[i], verts[i + 1], tex[0], tex[i], tex[i + 1], n);
                            PARTS.computeIfAbsent(currentGroup, k -> new ArrayList<>()).add(tri);
                        }
                    }
                }
            }

            int total = PARTS.values().stream().mapToInt(List::size).sum();
            MainRegistry.LOGGER.info("Loaded skeleton particle OBJ: {} faces across {} groups", total, PARTS.size());
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to load skeleton particle OBJ {}", obj, e);
        }
    }

    private static int parseIndex(String s, int size) {
        if (s.isEmpty()) return 0;
        int i = Integer.parseInt(s);
        if (i > 0) return i;
        return size + i + 1;
    }

    private static Vert faceNormal(Vert a, Vert b, Vert c, Vert na, Vert nb, Vert nc) {
        if (na != null && nb != null && nc != null) {
            return new Vert((na.x + nb.x + nc.x) / 3F, (na.y + nb.y + nc.y) / 3F, (na.z + nb.z + nc.z) / 3F);
        }
        // compute from positions
        float ux = b.x - a.x, uy = b.y - a.y, uz = b.z - a.z;
        float vx = c.x - a.x, vy = c.y - a.y, vz = c.z - a.z;
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len <= 1e-6) return new Vert(0, 1, 0);
        return new Vert(nx / len, ny / len, nz / len);
    }
}
