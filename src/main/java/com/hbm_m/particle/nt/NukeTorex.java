package com.hbm_m.particle.nt;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.ImmediateVertexWriter;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.particle.explosions.basic.CameraShakeHandler;
import com.hbm_m.sound.ModSounds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
/**
 * Toroidal convection mushroom cloud effect
 */
public class NukeTorex extends ParticleNT implements FarCapableParticle {

    protected int type = 0;
    protected float scale = 1;

    public double coreHeight = 3;
    public double convectionHeight = 3;
    public double torusWidth = 3;
    public double rollerSize = 1;
    public double heat = 1;
    public double lastSpawnY = -1;
    public final List<Cloudlet> cloudlets = new ArrayList<>();

    public boolean didPlaySound = false;
    public boolean didShake = false;

    /** Жёсткий лимит на общее число cloudlets (зависит от качества). */
    private static final int MAX_CLOUDLETS_FANCY = 12_000;
    private static final int MAX_CLOUDLETS_FAST = 6_000;

    //? if fabric && < 1.21.1 {
    /*private static final ResourceLocation CLOUDLET = new ResourceLocation(RefStrings.MODID, "textures/particle/particle_base.png");
    *///?} else {
        private static final ResourceLocation CLOUDLET = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/particle/particle_base.png");
    //?}

    //? if fabric && < 1.21.1 {
    /*private static final ResourceLocation FLASH = new ResourceLocation(RefStrings.MODID, "textures/particle/flare.png");
    *///?} else {
        private static final ResourceLocation FLASH = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/particle/flare.png");
    //?}


    public NukeTorex(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Override
    public void tick() {
        this.age++;

        double s = 1.5;
        double cs = 1.5;
        int maxAge = this.getMaxAge();

        if (age == 1) this.setScale((float) s, false);

        if (lastSpawnY == -1) {
            lastSpawnY = this.y - 3;
        }

        if (age < 100) this.level.setSkyFlashTime(5);

        int spawnTarget = level.getHeight(Heightmap.Types.WORLD_SURFACE, (int) x, (int) z) - 3;
        double moveSpeed = 0.5;

        if (Math.abs(spawnTarget - lastSpawnY) < moveSpeed) {
            lastSpawnY = spawnTarget;
        } else {
            lastSpawnY += moveSpeed * Math.signum(spawnTarget - lastSpawnY);
        }

        double range = (torusWidth - rollerSize) * 0.25;
        double simSpeed = getSimulationSpeed();

        // LOD по дистанции до камеры: вдали спавним меньше cloudlets.
        double lodFactor = getLodFactor();
        int toSpawn = (int) Math.ceil(10 * simSpeed * simSpeed * lodFactor);
        int lifetime = Math.min((age * age) + 200, maxAge - age + 200);

        int maxCloudlets = getMaxCloudletsForQuality();
        for (int i = 0; i < toSpawn && cloudlets.size() < maxCloudlets; i++) {
            double lx = this.x + random.nextGaussian() * range;
            double lz = this.z + random.nextGaussian() * range;
            Cloudlet cloud = new Cloudlet(lx, lastSpawnY, lz, (float) (random.nextDouble() * 2 * Math.PI), 0, lifetime);
            cloud.setScale(1F + this.age * 0.005F * (float) cs, 5F * (float) cs);
            cloudlets.add(cloud);
        }

        if (age < 150 && cloudlets.size() < maxCloudlets) {
            int cloudCount = age * 5;
            int shockLife = Math.max(300 - age * 20, 50);

            for (int i = 0; i < cloudCount && cloudlets.size() < maxCloudlets; i++) {
                float rot = (float) (Math.PI * 2 * random.nextDouble());
                Vec3 vec = new Vec3((age * 1.5 + random.nextDouble()) * 1.5, 0, 0).yRot(rot);
                this.cloudlets.add(new Cloudlet(vec.x + this.x, level.getHeight(Heightmap.Types.WORLD_SURFACE, (int) (vec.x + this.x), (int) (vec.z + this.z)), vec.z + this.z, rot, 0, shockLife, TorexType.SHOCK)
                        .setScale(7F, 2F)
                        .setMotion(age > 15 ? 0.75 : 0));
            }

            if (!didPlaySound) {
                Player player = Minecraft.getInstance().player;
                if (player != null) {
                    double dist = Math.sqrt(player.distanceToSqr(x, y, z));
                    double radius = (age * 1.5 + 1) * 1.5;
                    if (dist < radius) {
                        level.playLocalSound(x, y, z, ModSounds.NUCLEAR_EXPLOSION.get(), SoundSource.AMBIENT, 10_000F, 1F, false);
                        didPlaySound = true;
                    }
                }
            }
        }

        if (age < 130 * s && cloudlets.size() < maxCloudlets) {
            int ringLifetime = (int) (lifetime * s);
            for (int i = 0; i < 2 && cloudlets.size() < maxCloudlets; i++) {
                Cloudlet cloud = new Cloudlet(x, y + coreHeight, z, (float) (random.nextDouble() * 2 * Math.PI), 0, ringLifetime, TorexType.RING);
                cloud.setScale(1F + this.age * 0.0025F * (float) (cs * cs), 3F * (float) (cs * cs));
                cloudlets.add(cloud);
            }
        }

        if (age > 130 * s && age < 600 * s && cloudlets.size() < maxCloudlets) {
            for (int i = 0; i < 20 && cloudlets.size() < maxCloudlets; i++) {
                for (int j = 0; j < 4 && cloudlets.size() < maxCloudlets; j++) {
                    float angle = (float) (Math.PI * 2 * random.nextDouble());
                    Vec3 vec = new Vec3(torusWidth + rollerSize * (5 + random.nextDouble()), 0, 0);
                    vec = vec.zRot((float) (Math.PI / 45 * j)).yRot(angle);
                    Cloudlet cloud = new Cloudlet(x + vec.x, y + coreHeight - 5 + j * s, z + vec.z, angle, 0, (int) ((20 + age / 10) * (1 + random.nextDouble() * 0.1)), TorexType.CONDENSATION);
                    cloud.setScale(0.125F * (float) cs, 3F * (float) cs);
                    cloudlets.add(cloud);
                }
            }
        }
        if (age > 200 * s && age < 600 * s && cloudlets.size() < maxCloudlets) {
            for (int i = 0; i < 20 && cloudlets.size() < maxCloudlets; i++) {
                for (int j = 0; j < 4 && cloudlets.size() < maxCloudlets; j++) {
                    float angle = (float) (Math.PI * 2 * random.nextDouble());
                    Vec3 vec = new Vec3(torusWidth + rollerSize * (3 + random.nextDouble() * 0.5), 0, 0);
                    vec = vec.zRot((float) (Math.PI / 45 * j)).yRot(angle);
                    Cloudlet cloud = new Cloudlet(x + vec.x, y + coreHeight + 25 + j * cs, z + vec.z, angle, 0, (int) ((20 + age / 10) * (1 + random.nextDouble() * 0.1)), TorexType.CONDENSATION);
                    cloud.setScale(0.125F * (float) cs, 3F * (float) cs);
                    cloudlets.add(cloud);
                }
            }
        }

        for (Cloudlet cloud : cloudlets) {
            cloud.update();
        }

        coreHeight += 0.15 / s;
        torusWidth += 0.05 / s;
        rollerSize = torusWidth * 0.35;
        convectionHeight = coreHeight + rollerSize;

        int maxHeat = (int) (50 * cs);
        heat = maxHeat - Math.pow((maxHeat * this.age) / maxAge, 1);

        cloudlets.removeIf(c -> c.isDead);

        if (this.age > maxAge) this.remove();
    }

    public NukeTorex setScale(float scale, boolean changeScale) {
        if (changeScale) this.scale = scale;
        this.coreHeight = this.coreHeight / 1.5 * scale;
        this.convectionHeight = this.convectionHeight / 1.5 * scale;
        this.torusWidth = this.torusWidth / 1.5 * scale;
        this.rollerSize = this.rollerSize / 1.5 * scale;
        return this;
    }

    public NukeTorex setType(int type) {
        this.type = type;
        return this;
    }

    public double getSimulationSpeed() {
        int lifetime = getMaxAge();
        int simSlow = lifetime / 4;
        int simStop = lifetime / 2;
        int life = this.age;
        if (life > simStop) return 0;
        if (life > simSlow) return 1.0 - ((double) (life - simSlow) / (double) (simStop - simSlow));
        return 1.0;
    }

    /** Простой LOD-фактор в зависимости от расстояния до камеры и настроек графики. */
    private double getLodFactor() {
        Minecraft mc = Minecraft.getInstance();
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 camPos = cam.getPosition();
        double distSq = camPos.distanceToSqr(this.x, this.y, this.z);

        double dist = Math.sqrt(distSq);
        // Чем дальше взрыв, тем меньше деталей.
        if (dist < 128) {
            return 1.0;
        } else if (dist < 256) {
            return 0.75;
        } else if (dist < 384) {
            return 0.5;
        } else {
            return 0.35;
        }
    }

    private int getMaxCloudletsForQuality() {
        Minecraft mc = Minecraft.getInstance();
        GraphicsStatus graphics = mc.options.graphicsMode().get();
        boolean fancy = graphics == GraphicsStatus.FANCY || graphics == GraphicsStatus.FABULOUS;
        return fancy ? MAX_CLOUDLETS_FANCY : MAX_CLOUDLETS_FAST;
    }

    public double getScale() { return this.scale; }

    public double getGreying() {
        int lifetime = getMaxAge();
        int greying = lifetime * 3 / 4;
        if (this.age > greying) return 1 + ((double) (this.age - greying) / (double) (lifetime - greying));
        return 1.0;
    }

    public float getAlpha() {
        int lifetime = getMaxAge();
        int fadeOut = lifetime * 3 / 4;
        if (this.age > fadeOut) {
            float fac = (float) (this.age - fadeOut) / (float) (lifetime - fadeOut);
            return 1F - fac;
        }
        return 1.0F;
    }

    public int getMaxAge() {
        return (int) (45 * 20 * this.getScale());
    }

    public class Cloudlet {
        public double posX, posY, posZ;
        public double prevPosX, prevPosY, prevPosZ;
        public double motionX, motionY, motionZ;
        public int age;
        public int cloudletLife;
        public float angle;
        public boolean isDead = false;
        float rangeMod;
        public float colorMod;
        public Vec3 color;
        public Vec3 prevColor;
        public TorexType type;

        public Cloudlet(double posX, double posY, double posZ, float angle, int age, int maxAge) {
            this(posX, posY, posZ, angle, age, maxAge, TorexType.STANDARD);
        }

        public Cloudlet(double posX, double posY, double posZ, float angle, int age, int maxAge, TorexType type) {
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.age = age;
            this.cloudletLife = maxAge;
            this.angle = angle;
            this.rangeMod = 0.3F + random.nextFloat() * 0.7F;
            this.colorMod = 0.8F + random.nextFloat() * 0.2F;
            this.type = type;
            this.updateColor();
            this.prevColor = this.color;
        }

        private void update() {
            age++;
            if (age > cloudletLife) this.isDead = true;
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;

            Vec3 simPos = new Vec3(NukeTorex.this.x - this.posX, 0, NukeTorex.this.z - this.posZ);
            double simPosX = NukeTorex.this.x + simPos.length();
            double simPosZ = NukeTorex.this.z;

            if (this.type == TorexType.STANDARD) {
                Vec3 convection = getConvectionMotion(simPosX, simPosZ);
                Vec3 lift = getLiftMotion(simPosX);
                double factor = Mth.clamp((this.posY - NukeTorex.this.y) / NukeTorex.this.coreHeight, 0.0, 1.0);
                this.motionX = convection.x * factor + lift.x * (1.0 - factor);
                this.motionY = convection.y * factor + lift.y * (1.0 - factor);
                this.motionZ = convection.z * factor + lift.z * (1.0 - factor);
            } else if (this.type == TorexType.SHOCK) {
                double factor = Mth.clamp((this.posY - NukeTorex.this.y) / NukeTorex.this.coreHeight, 0, 1);
                Vec3 motion = new Vec3(1, 0, 0).yRot(this.angle);
                this.motionX = motion.x * factor;
                this.motionY = motion.y * factor;
                this.motionZ = motion.z * factor;
            } else if (this.type == TorexType.RING) {
                Vec3 motion = getRingMotion(simPosX, simPosZ);
                this.motionX = motion.x;
                this.motionY = motion.y;
                this.motionZ = motion.z;
            } else if (this.type == TorexType.CONDENSATION) {
                Vec3 motion = getCondensationMotion();
                this.motionX = motion.x;
                this.motionY = motion.y;
                this.motionZ = motion.z;
            }

            double mult = this.motionMult * getSimulationSpeed();
            this.posX += this.motionX * mult;
            this.posY += this.motionY * mult;
            this.posZ += this.motionZ * mult;
            this.updateColor();
        }

        private Vec3 getCondensationMotion() {
            Vec3 delta = new Vec3(posX - NukeTorex.this.x, 0, posZ - NukeTorex.this.z);
            double speed = 0.00002 * NukeTorex.this.age;
            return new Vec3(delta.x * speed, 0, delta.z * speed);
        }

        private Vec3 getRingMotion(double simPosX, double simPosZ) {
            if (simPosX > NukeTorex.this.x + torusWidth * 2) return new Vec3(0, 0, 0);
            Vec3 torusPos = new Vec3(NukeTorex.this.x + torusWidth, NukeTorex.this.y + coreHeight * 0.5, NukeTorex.this.z);
            Vec3 delta = new Vec3(torusPos.x - simPosX, torusPos.y - this.posY, torusPos.z - simPosZ);
            double roller = NukeTorex.this.rollerSize * this.rangeMod * 0.25;
            double dist = delta.length() / roller - 1.0;
            double func = 1.0 - Math.pow(Math.E, -dist);
            float angle = (float) (func * Math.PI * 0.5);
            Vec3 rot = new Vec3(-delta.x / dist, -delta.y / dist, -delta.z / dist).zRot(angle);
            Vec3 motion = new Vec3(torusPos.x + rot.x - simPosX, torusPos.y + rot.y - this.posY, torusPos.z + rot.z - simPosZ);
            double speed = 0.001;
            motion = new Vec3(motion.x * speed, motion.y * speed, motion.z * speed).yRot(this.angle).normalize();
            return motion;
        }

        private Vec3 getConvectionMotion(double simPosX, double simPosZ) {
            Vec3 torusPos = new Vec3(NukeTorex.this.x + torusWidth, NukeTorex.this.y + coreHeight, NukeTorex.this.z);
            Vec3 delta = new Vec3(torusPos.x - simPosX, torusPos.y - this.posY, torusPos.z - simPosZ);
            double roller = NukeTorex.this.rollerSize * this.rangeMod;
            double dist = delta.length() / roller - 1.0;
            double func = 1.0 - Math.pow(Math.E, -dist);
            float angle = (float) (func * Math.PI * 0.5);
            Vec3 rot = new Vec3(-delta.x / dist, -delta.y / dist, -delta.z / dist).zRot(angle);
            Vec3 motion = new Vec3(torusPos.x + rot.x - simPosX, torusPos.y + rot.y - this.posY, torusPos.z + rot.z - simPosZ).yRot(this.angle);
            return motion.normalize();
        }

        private Vec3 getLiftMotion(double simPosX) {
            double scale = Mth.clamp(1.0 - (simPosX - (NukeTorex.this.x + torusWidth)), 0, 1);
            Vec3 motion = new Vec3(NukeTorex.this.x - this.posX, (NukeTorex.this.y + convectionHeight) - this.posY, NukeTorex.this.z - this.posZ).normalize();
            return new Vec3(motion.x * scale, motion.y * scale, motion.z * scale);
        }

        private void updateColor() {
            this.prevColor = this.color;
            double exX = NukeTorex.this.x, exY = NukeTorex.this.y + NukeTorex.this.coreHeight, exZ = NukeTorex.this.z;
            double distX = exX - posX, distY = exY - posY, distZ = exZ - posZ;
            double distSq = distX * distX + distY * distY + distZ * distZ;
            distSq /= NukeTorex.this.heat;
            double dist = Math.sqrt(distSq);
            dist = Math.max(dist, 1);
            double col = 2.0 / dist;
            int nukeType = NukeTorex.this.type;
            if (nukeType == 1) {
                this.color = new Vec3(Math.max(col * 1, 0.25), Math.max(col * 2, 0.25), Math.max(col * 0.5, 0.25));
            } else if (nukeType == 2) {
                Color awt = Color.getHSBColor(this.angle / 2F / (float) Math.PI, 1F, 1F);
                if (this.type == TorexType.RING) {
                    this.color = new Vec3(Math.max(col * 1, 0.25), Math.max(col * 1, 0.25), Math.max(col * 1, 0.25));
                } else {
                    this.color = new Vec3(awt.getRed() / 255.0, awt.getGreen() / 255.0, awt.getBlue() / 255.0);
                }
            } else {
                this.color = new Vec3(Math.max(col * 2, 0.25), Math.max(col * 1.5, 0.25), Math.max(col * 0.5, 0.25));
            }
        }

        public Vec3 getInterpPos(float partialTicks) {
            float scale = (float) NukeTorex.this.getScale();
            Vec3 base = new Vec3(prevPosX + (posX - prevPosX) * partialTicks, prevPosY + (posY - prevPosY) * partialTicks, prevPosZ + (posZ - prevPosZ) * partialTicks);
            if (this.type != TorexType.SHOCK) {
                base = new Vec3((base.x - NukeTorex.this.x) * scale + NukeTorex.this.x, (base.y - NukeTorex.this.y) * scale + NukeTorex.this.y, (base.z - NukeTorex.this.z) * scale + NukeTorex.this.z);
            }
            return base;
        }

        public Vec3 getInterpColor(float partialTicks) {
            if (this.type == TorexType.CONDENSATION) return new Vec3(1, 1, 1);
            Vec3 prev = prevColor != null ? prevColor : color;
            double greying = NukeTorex.this.getGreying();
            if (this.type == TorexType.RING) greying += 1;
            double r = (prev.x + (color.x - prev.x) * partialTicks) * greying;
            double g = (prev.y + (color.y - prev.y) * partialTicks) * greying;
            double b = (prev.z + (color.z - prev.z) * partialTicks) * greying;
            return new Vec3(r, g, b);
        }

        public float getAlpha() {
            float alpha = (1F - ((float) age / (float) cloudletLife)) * NukeTorex.this.getAlpha();
            if (this.type == TorexType.CONDENSATION) alpha *= 0.25;
            return alpha;
        }

        private float startingScale = 1;
        private float growingScale = 5F;

        public float getScale() {
            float base = startingScale + ((float) age / (float) cloudletLife) * growingScale;
            if (this.type != TorexType.SHOCK) base *= (float) NukeTorex.this.getScale();
            return base;
        }

        public Cloudlet setScale(float start, float grow) {
            this.startingScale = start;
            this.growingScale = grow;
            return this;
        }

        private double motionMult = 1;

        public Cloudlet setMotion(double mult) {
            this.motionMult = mult;
            return this;
        }
    }

    @Override
    public net.minecraft.resources.ResourceLocation hbm$getFarTexture() { return CLOUDLET; }

    public enum TorexType { STANDARD, SHOCK, RING, CONDENSATION }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float partialTicks, PoseStack levelPoseStack) {
        Vec3 camPos = camera.getPosition();
        // Fallback-виртуализация ТОЛЬКО когда DH не рендерит (см. ParticleNT):
        // центр гриба приближается к границе прорисовки, чтобы шляпка не
        // клипалась ванильным far plane. Размер квайдов сжимается тем же
        // коэффициентом — иначе угловой размер перестаёт уменьшаться и
        // «эффект далёкого взрыва» теряется.
        float vScale = virtualScale(this.x, this.y, this.z, camera);
        Vec3 centerOffset = virtualizedOffset(this.x, this.y, this.z, camera);

        //  Свой PoseStack с ТОЛЬКО трансляцией. Поворот камеры уже в ModelViewMat (шейдер).
        PoseStack localPose = new PoseStack();
        localPose.translate(centerOffset.x, centerOffset.y, centerOffset.z);

        FogRenderer.setupNoFog();
        cloudletWrapper(partialTicks, localPose, ignored, vScale);

        long now = System.currentTimeMillis();
        //  Ядерная вспышка и тряска HUD живут в ModEventHandlerClient (Forge-only оверлей).
        //  CameraShakeHandler.addShake() работает на всех лоадерах и даёт ещё и тряску камеры.
        //? if forge {
        if (this.age < 10 && now - com.hbm_m.powerarmor.ModEventHandlerClient.flashTimestamp > 1_000) {
            com.hbm_m.powerarmor.ModEventHandlerClient.triggerNuclearFlash();
        }
        if (this.didPlaySound && !this.didShake && now - com.hbm_m.powerarmor.ModEventHandlerClient.shakeTimestamp > 1_000) {
            com.hbm_m.powerarmor.ModEventHandlerClient.shakeTimestamp = now;
            this.didShake = true;
            CameraShakeHandler.addShake(1.0F, 30); //  ~1.5с тряски камеры+GUI, как в оригинале (1500мс)
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                player.hurtDuration = 15;
                player.hurtTime = 15;
            }
        }
        //?} else {
        /*if (this.didPlaySound && !this.didShake) {
            this.didShake = true;
            CameraShakeHandler.addShake(1.0F, 30);
        }
        
        *///?}
    }

    @Override
    public void renderFlashOnly(MultiBufferSource buffer, Camera camera, float partialTicks, PoseStack levelPoseStack) {
        if (this.age >= 101) return;
        // Вспышка — с той же fallback-виртуализацией (со сжатием размера).
        Vec3 centerOffset = virtualizedOffset(this.x, this.y, this.z, camera);
        float vScale = virtualScale(this.x, this.y, this.z, camera);

        //  Тоже свой PoseStack - только трансляция
        PoseStack localPose = new PoseStack();
        localPose.translate(centerOffset.x, centerOffset.y, centerOffset.z);

        FogRenderer.setupNoFog();
        flashWrapper(partialTicks, localPose, buffer, vScale);
    }

    private void flashWrapper(float partialTicks, PoseStack poseStack, MultiBufferSource buffer, float vScale) {
        double age = Math.min(this.age + partialTicks, 100);
        float alpha = (float) ((100 - age) / 100F);
        Random rand = new Random(this.hashCode());
        Matrix4f matrix = poseStack.last().pose();

        VertexConsumer consumer = buffer.getBuffer(ClientRenderHandler.CustomRenderTypes.NUKE_FLASH.apply(FLASH));
        for (int i = 0; i < 3; i++) {
            float lx = (float) (rand.nextGaussian() * 0.5 * this.rollerSize * vScale);
            float ly = (float) (rand.nextGaussian() * 0.5 * this.rollerSize * vScale);
            float lz = (float) (rand.nextGaussian() * 0.5 * this.rollerSize * vScale);
            renderFlash(matrix, consumer, lx, (float) (ly + this.coreHeight * vScale), lz, (float) (25 * this.rollerSize * vScale), alpha);
        }
    }

    private void cloudletWrapper(float partialTicks, PoseStack poseStack, VertexConsumer consumer, float vScale) {
        Matrix4f matrix = poseStack.last().pose();
        // Порядок отрисовки cloudlets: от дальних к ближним относительно
        // камеры (painter's algorithm). sortOnUpload у рендертайпа выключен
        // (нестабилен в модпаке — «Sorting state uninitialized»), порядок
        // квайдов задаём сами — иначе ножка/шляпка/роллеры блендятся в
        // порядке спавна, а не по глубине.
        Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        double cx = cam.getPosition().x;
        double cy = cam.getPosition().y;
        double cz = cam.getPosition().z;
        this.cloudlets.sort((a, b) -> {
            Vec3 va = a.getInterpPos(partialTicks);
            Vec3 vb = b.getInterpPos(partialTicks);
            double da = (va.x - cx) * (va.x - cx) + (va.y - cy) * (va.y - cy) + (va.z - cz) * (va.z - cz);
            double db = (vb.x - cx) * (vb.x - cx) + (vb.y - cy) * (vb.y - cy) + (vb.z - cz) * (vb.z - cz);
            return Double.compare(db, da);
        });
        for (Cloudlet cloudlet : cloudlets) {
            Vec3 vec = cloudlet.getInterpPos(partialTicks);
            float lx = (float) ((vec.x - this.x) * vScale);
            float ly = (float) ((vec.y - this.y) * vScale);
            float lz = (float) ((vec.z - this.z) * vScale);
            renderCloudlet(matrix, consumer, lx, ly, lz, cloudlet, partialTicks, vScale);
        }
    }

    private void renderCloudlet(Matrix4f matrix, VertexConsumer consumer, float posX, float posY, float posZ, Cloudlet cloud, float partialTicks, float vScale) {
        // Окклюзия — попиксельно в шейдере nuke_cloud (DhOcclusionGpu).
        float alpha = cloud.getAlpha();
        float scale = cloud.getScale() * vScale;
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vector3f l = new Vector3f(camera.getLeftVector()).mul(scale);
        Vector3f u = new Vector3f(camera.getUpVector()).mul(scale);
        float brightness = cloud.type == TorexType.CONDENSATION ? 0.9F : 0.75F * cloud.colorMod;
        Vec3 interpColor = cloud.getInterpColor(partialTicks);
        float r = (float) interpColor.x * brightness;
        float g = (float) interpColor.y * brightness;
        float b = (float) interpColor.z * brightness;
        ImmediateVertexWriter.billboardQuad(consumer, matrix, posX, posY, posZ, l, u, r, g, b, alpha, 0, 0, 1, 1);
    }

    private void renderFlash(Matrix4f matrix, VertexConsumer consumer,
            float posX, float posY, float posZ,
            float scale, float alpha) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vector3f r = new Vector3f(camera.getLeftVector()).mul(scale);
        Vector3f u = new Vector3f(camera.getUpVector()).mul(scale);
        ImmediateVertexWriter.billboardQuad(consumer, matrix, posX, posY, posZ, r, u,
                1.0F, 1.0F, 1.0F, alpha, 0, 0, 1, 1);
    }

    @Override
    public RenderType getRenderType() {
        // Основной рендер идёт через NUKE_CLOUDS, но возвращаем тип для совместимости.
        return ClientRenderHandler.CustomRenderTypes.nukeClouds(CLOUDLET);
    }
}



