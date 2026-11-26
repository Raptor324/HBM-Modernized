package com.hbm_m.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.joml.Vector3f;

import com.hbm_m.entity.logic.EntityKa52;
import com.hbm_m.entity.logic.EntityPlaneBase;
import com.hbm_m.entity.missile.MissileAntiBallisticEntity;
import com.hbm_m.sound.HbmSoundsNT;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Alles, was im Ka-52 nur der Client sieht: Triebwerkssound, Warntoene, Kamera und das Helm-HUD.
 *
 * <p>Das HUD zeigt Horizont mit Leiter, Kurs, Fahrt und Hoehe in echten Einheiten (1 Block = 1 m), dazu
 * Steigrate, Rotordrehzahl, Zustand, Zielrahmen und Warnungen. Gezeichnet wird in der Farbe eines
 * Helmvisiers. Die Messwerte kommen aus der sichtbaren Bewegung und werden geglaettet, damit die
 * Anzeigen trotz Netzwerktakt ruhig laufen.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public final class Ka52Client {

    private static final int GREEN = 0xFF4DFF7A;
    private static final int DIM = 0x9944DD66;
    private static final int RED = 0xFFFF4A3A;
    private static final int AMBER = 0xFFFFC23A;
    private static final double TARGET_RANGE = 256.0D;
    private static final double MISSILE_RANGE = 500.0D;
    private static final float BASE_CAMERA_DISTANCE = 15.0F;

    private static final Map<EntityKa52, EngineSound> SOUNDS = new WeakHashMap<>();
    private static final List<Entity> TARGETS = new ArrayList<>();

    private static float speedKmh;
    private static float climbMs;
    private static float altitude;
    private static float cameraDistance = BASE_CAMERA_DISTANCE;
    private static boolean missileWarning;
    private static int beepTimer;

    private Ka52Client() {}

    // -- Takt ------------------------------------------------------------------

    /** Einmal pro Client-Tick aus der Entity heraus. */
    public static void onEntityTick(EntityKa52 helicopter) {
        Minecraft minecraft = Minecraft.getInstance();
        EngineSound sound = SOUNDS.get(helicopter);
        if (helicopter.getRotorSpeed() > 0.05F && (sound == null || sound.isStopped())) {
            sound = new EngineSound(helicopter);
            SOUNDS.put(helicopter, sound);
            minecraft.getSoundManager().play(sound);
        }

        LocalPlayer player = minecraft.player;
        if (player == null || player.getVehicle() != helicopter) {
            return;
        }
        double dx = helicopter.getX() - helicopter.xo;
        double dy = helicopter.getY() - helicopter.yo;
        double dz = helicopter.getZ() - helicopter.zo;
        speedKmh = Mth.lerp(0.25F, speedKmh, (float) (Math.sqrt(dx * dx + dz * dz) * 72.0D));
        climbMs = Mth.lerp(0.2F, climbMs, (float) (dy * 20.0D));
        int ground = helicopter.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mth.floor(helicopter.getX()), Mth.floor(helicopter.getZ()));
        altitude = (float) (helicopter.getY() - ground);
        cameraDistance = Mth.lerp(0.05F, cameraDistance, BASE_CAMERA_DISTANCE + speedKmh * 0.025F);

        if (helicopter.tickCount % 5 == 0) {
            scan(helicopter, player);
        }
        if (--beepTimer <= 0 && warningLevel(helicopter) > 0) {
            int level = warningLevel(helicopter);
            beepTimer = level > 1 ? 8 : 20;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                    SoundEvents.NOTE_BLOCK_BIT.value(), level > 1 ? 2.0F : 1.4F, 0.5F));
        }
    }

    private static void scan(EntityKa52 helicopter, LocalPlayer player) {
        TARGETS.clear();
        AABB area = helicopter.getBoundingBox().inflate(TARGET_RANGE);
        Vec3 sensor = helicopter.position().add(0.0D, 1.0D, 0.0D);
        TARGETS.addAll(helicopter.level().getEntitiesOfClass(Mob.class, area,
                m -> m instanceof Enemy && m.isAlive() && inSight(helicopter, sensor, m)));
        TARGETS.addAll(helicopter.level().getEntitiesOfClass(EntityPlaneBase.class, area,
                plane -> plane != helicopter && plane.isAlive() && inSight(helicopter, sensor, plane)));

        missileWarning = false;
        AABB wide = helicopter.getBoundingBox().inflate(MISSILE_RANGE);
        for (MissileAntiBallisticEntity missile : helicopter.level().getEntitiesOfClass(MissileAntiBallisticEntity.class, wide)) {
            Vec3 toUs = helicopter.position().subtract(missile.position());
            Vec3 step = new Vec3(missile.getX() - missile.xo, missile.getY() - missile.yo, missile.getZ() - missile.zo);
            if (step.dot(toUs) > 0.0D) {
                missileWarning = true;
                break;
            }
        }
    }

    /** Die Sensoren sehen nicht durch Gelaende: Mobs in Hoehlen oder hinter Waenden bekommen keinen Rahmen. */
    private static boolean inSight(EntityKa52 helicopter, Vec3 sensor, Entity target) {
        Vec3 point = target.getBoundingBox().getCenter();
        return helicopter.level().clip(new ClipContext(sensor, point, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, helicopter)).getType() == HitResult.Type.MISS;
    }

    /** 0 = ruhig, 1 = Warnung, 2 = sofort handeln. */
    private static int warningLevel(EntityKa52 helicopter) {
        if (missileWarning || pullUp()) {
            return 2;
        }
        return healthFraction(helicopter) < 0.35F ? 1 : 0;
    }

    private static boolean pullUp() {
        return altitude > 0.5F && climbMs < 0.0F && altitude / -climbMs < 2.5F;
    }

    private static float healthFraction(EntityKa52 helicopter) {
        return Mth.clamp(helicopter.getSyncedHealth() / helicopter.getMaxHealth(), 0.0F, 1.0F);
    }

    // -- Kamera ----------------------------------------------------------------

    public static boolean isPiloting(Entity viewer) {
        return viewer != null && viewer.getVehicle() instanceof EntityKa52;
    }

    /** Abstand der Aussenkamera: waechst mit der Fahrt, dadurch wirkt Tempo auch optisch. */
    public static float cameraDistance() {
        return cameraDistance;
    }

    /** Die Kamera legt sich mit in die Kurve, innen voll, aussen nur halb. */
    public static float cameraRoll(Entity viewer, float partialTick, boolean firstPerson) {
        if (!(viewer.getVehicle() instanceof EntityKa52 helicopter)) {
            return 0.0F;
        }
        float roll = helicopter.getRoll() * (firstPerson ? 1.0F : 0.45F);
        float shake = helicopter.getRotorSpeed() * (0.04F + speedKmh * 0.0006F);
        return roll + Mth.sin((viewer.tickCount + partialTick) * 2.9F) * shake;
    }

    /** Kleines Rotorzittern, kaum sichtbar im Stand und deutlich bei Hoechstfahrt. */
    public static float cameraPitchShake(Entity viewer, float partialTick) {
        if (!(viewer.getVehicle() instanceof EntityKa52 helicopter)) {
            return 0.0F;
        }
        return Mth.sin((viewer.tickCount + partialTick) * 3.7F) * helicopter.getRotorSpeed() * (0.03F + speedKmh * 0.0005F);
    }

    /** Sichtfeld weitet sich mit der Fahrt. */
    public static float fovModifier() {
        return 1.0F + Mth.clamp(speedKmh / 250.0F, 0.0F, 1.0F) * 0.12F;
    }

    // -- HUD -------------------------------------------------------------------

    public static void renderHud(GuiGraphics g, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || !(player.getVehicle() instanceof EntityKa52 helicopter)) {
            return;
        }
        int w = g.guiWidth();
        int h = g.guiHeight();
        int cx = w / 2;
        int cy = h / 2;
        float pxPerDegree = h / (float) minecraft.options.fov().get().doubleValue();
        Font font = minecraft.font;

        drawTargets(g, minecraft, font, w, h);
        drawLadder(g, font, helicopter, partialTick, cx, cy, pxPerDegree);
        drawHeading(g, font, helicopter, partialTick, cx);
        drawTape(g, font, 34, cy, speedKmh, 5.0F, 3, "KM/H", false);
        drawTape(g, font, w - 34, cy, altitude, 5.0F, 3, "ALT M", true);
        drawClimb(g, font, w - 62, cy);
        drawGunCross(g, cx, cy);
        drawStatus(g, font, helicopter, 12, h - 58);
        drawWarnings(g, font, helicopter, cx, cy + 46, player.tickCount);
    }

    private static void text(GuiGraphics g, Font font, String text, int x, int y, int color) {
        g.drawString(font, text, x, y, color, false);
    }

    private static void centered(GuiGraphics g, Font font, String text, int x, int y, int color) {
        g.drawString(font, text, x - font.width(text) / 2, y, color, false);
    }

    private static void drawLadder(GuiGraphics g, Font font, EntityKa52 helicopter, float partialTick,
                                   int cx, int cy, float pxPerDegree) {
        float pitch = -Mth.lerp(partialTick, helicopter.xRotO, helicopter.getXRot());
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, 0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(-helicopter.getRoll()));
        for (int degrees = -40; degrees <= 40; degrees += 10) {
            int y = Math.round((pitch - degrees) * -pxPerDegree);
            if (Math.abs(y) > cy - 40) {
                continue;
            }
            if (degrees == 0) {
                g.fill(-96, y, -22, y + 1, GREEN);
                g.fill(22, y, 96, y + 1, GREEN);
                continue;
            }
            int color = degrees > 0 ? GREEN : DIM;
            g.fill(-46, y, -22, y + 1, color);
            g.fill(22, y, 46, y + 1, color);
            text(g, font, Integer.toString(degrees), 49, y - 4, color);
        }
        pose.popPose();
    }

    private static void drawHeading(GuiGraphics g, Font font, EntityKa52 helicopter, float partialTick, int cx) {
        float heading = Mth.positiveModulo(Mth.rotLerp(partialTick, helicopter.yRotO, helicopter.getYRot()) + 180.0F, 360.0F);
        int y = 10;
        for (int step = -40; step <= 40; step += 5) {
            int mark = Math.round(heading / 5.0F) * 5 + step;
            int x = cx + Math.round((mark - heading) * 3.0F);
            int wrapped = Mth.positiveModulo(mark, 360);
            boolean major = wrapped % 15 == 0;
            g.fill(x, y + 10, x + 1, y + (major ? 17 : 14), major ? GREEN : DIM);
            if (wrapped % 30 == 0) {
                String label = switch (wrapped) {
                    case 0 -> "N";
                    case 90 -> "E";
                    case 180 -> "S";
                    case 270 -> "W";
                    default -> String.format("%02d", wrapped / 10);
                };
                centered(g, font, label, x, y, GREEN);
            }
        }
        g.fill(cx - 15, y + 20, cx + 15, y + 31, 0x88000000);
        centered(g, font, String.format("%03d", Math.round(heading) % 360), cx, y + 22, GREEN);
    }

    private static void drawTape(GuiGraphics g, Font font, int x, int cy, float value, float step, int digits,
                                 String label, boolean right) {
        int side = right ? -1 : 1;
        for (int i = -7; i <= 7; i++) {
            float mark = (Math.round(value / step) + i) * step;
            int y = cy - Math.round((mark - value) * 2.4F);
            boolean major = Math.round(mark / step) % 2 == 0;
            int a = x + side * 17;
            int b = a + side * (major ? 6 : 3);
            g.fill(Math.min(a, b), y, Math.max(a, b), y + 1, major ? GREEN : DIM);
        }
        g.fill(x - 17, cy - 6, x + 17, cy + 6, 0xAA000000);
        centered(g, font, String.format("%0" + digits + "d", Math.max(0, Math.round(value))), x, cy - 4, GREEN);
        centered(g, font, label, x, cy + 46, DIM);
    }

    private static void drawClimb(GuiGraphics g, Font font, int x, int cy) {
        int bar = Mth.clamp(Math.round(climbMs * 3.5F), -40, 40);
        g.fill(x, cy - 40, x + 1, cy + 40, DIM);
        g.fill(x - 4, Math.min(cy, cy - bar), x, Math.max(cy, cy - bar) + 1, climbMs < -8.0F ? RED : GREEN);
        centered(g, font, String.format("%+.1f", climbMs), x - 2, cy + 46, DIM);
    }

    private static void drawGunCross(GuiGraphics g, int cx, int cy) {
        g.fill(cx - 13, cy, cx - 5, cy + 1, GREEN);
        g.fill(cx + 6, cy, cx + 14, cy + 1, GREEN);
        g.fill(cx, cy - 13, cx + 1, cy - 5, GREEN);
        g.fill(cx, cy + 6, cx + 1, cy + 14, GREEN);
        g.fill(cx, cy, cx + 1, cy + 1, GREEN);
    }

    private static void drawStatus(GuiGraphics g, Font font, EntityKa52 helicopter, int x, int y) {
        float rotor = helicopter.getRotorSpeed();
        float health = healthFraction(helicopter);
        bar(g, font, "NR", rotor, x, y, rotor >= 0.9F ? GREEN : AMBER);
        bar(g, font, "HP", health, x, y + 12, health > 0.5F ? GREEN : health > 0.25F ? AMBER : RED);
        text(g, font, helicopter.getGearRetraction() > 0.5F ? "GEAR UP" : "GEAR DN", x, y + 26,
                helicopter.getGearRetraction() > 0.5F ? DIM : GREEN);
        text(g, font, "TGT " + TARGETS.size(), x, y + 37, DIM);
    }

    private static void bar(GuiGraphics g, Font font, String label, float fraction, int x, int y, int color) {
        text(g, font, label, x, y, DIM);
        g.fill(x + 18, y + 1, x + 78, y + 7, 0x66000000);
        g.fill(x + 18, y + 1, x + 18 + Math.round(60.0F * fraction), y + 7, color);
        text(g, font, Math.round(fraction * 100.0F) + "%", x + 83, y, color);
    }

    private static void drawTargets(GuiGraphics g, Minecraft minecraft, Font font, int w, int h) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 eye = camera.getPosition();
        Vector3f look = camera.getLookVector();
        Vector3f up = camera.getUpVector();
        Vector3f left = camera.getLeftVector();
        float focal = (h / 2.0F) / (float) Math.tan(Math.toRadians(minecraft.options.fov().get()) * 0.5D);
        for (Entity target : TARGETS) {
            if (!target.isAlive()) {
                continue;
            }
            Vec3 d = target.getBoundingBox().getCenter().subtract(eye);
            float z = (float) (d.x * look.x() + d.y * look.y() + d.z * look.z());
            if (z < 2.0F) {
                continue;
            }
            int x = w / 2 - Math.round((float) (d.x * left.x() + d.y * left.y() + d.z * left.z()) / z * focal);
            int y = h / 2 - Math.round((float) (d.x * up.x() + d.y * up.y() + d.z * up.z()) / z * focal);
            if (x < 0 || y < 0 || x > w || y > h) {
                continue;
            }
            int s = Mth.clamp(Math.round(target.getBbHeight() / z * focal * 0.7F), 5, 28);
            int c = target instanceof EntityPlaneBase ? AMBER : RED;
            corners(g, x, y, s, c);
            centered(g, font, Math.round(d.length()) + "m", x, y + s + 3, c);
        }
    }

    private static void corners(GuiGraphics g, int x, int y, int s, int color) {
        int l = Math.max(2, s / 2);
        g.fill(x - s, y - s, x - s + l, y - s + 1, color);
        g.fill(x - s, y - s, x - s + 1, y - s + l, color);
        g.fill(x + s - l, y - s, x + s, y - s + 1, color);
        g.fill(x + s, y - s, x + s + 1, y - s + l, color);
        g.fill(x - s, y + s, x - s + l, y + s + 1, color);
        g.fill(x - s, y + s - l, x - s + 1, y + s, color);
        g.fill(x + s - l, y + s, x + s, y + s + 1, color);
        g.fill(x + s, y + s - l, x + s + 1, y + s + 1, color);
    }

    private static void drawWarnings(GuiGraphics g, Font font, EntityKa52 helicopter, int cx, int y, int tick) {
        boolean blink = tick / 5 % 2 == 0;
        if (helicopter.getState() == EntityKa52.CRASHING) {
            warning(g, font, "ROTOR FAILURE", cx, y, RED, blink);
            return;
        }
        if (helicopter.getRotorSpeed() < 0.9F) {
            warning(g, font, "ENGINE START", cx, y, AMBER, true);
            y += 13;
        }
        if (missileWarning) {
            warning(g, font, "MISSILE", cx, y, RED, blink);
            y += 13;
        }
        if (pullUp()) {
            warning(g, font, "PULL UP", cx, y, RED, blink);
            y += 13;
        }
        if (healthFraction(helicopter) < 0.35F) {
            warning(g, font, "DAMAGE", cx, y, AMBER, blink);
            y += 13;
        }
        if (altitude < 8.0F && altitude > 0.5F && helicopter.getGearRetraction() > 0.5F) {
            warning(g, font, "GEAR", cx, y, AMBER, true);
        }
    }

    private static void warning(GuiGraphics g, Font font, String text, int cx, int y, int color, boolean visible) {
        if (!visible) {
            return;
        }
        int half = font.width(text) / 2 + 5;
        g.fill(cx - half, y - 2, cx + half, y + 10, 0xAA000000);
        centered(g, font, text, cx, y, color);
    }

    // -- Sound -----------------------------------------------------------------

    /** Dauerton, der dem Hubschrauber folgt: Tonhoehe und Lautstaerke haengen an der Rotordrehzahl. */
    private static final class EngineSound extends AbstractTickableSoundInstance {

        private final EntityKa52 helicopter;

        EngineSound(EntityKa52 helicopter) {
            super(HbmSoundsNT.get("hbm:entity.chopperFlyingLoop"), SoundSource.NEUTRAL, RandomSource.create());
            this.helicopter = helicopter;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.1F;
            this.attenuation = Attenuation.LINEAR;
            tick();
        }

        @Override
        public void tick() {
            if (this.helicopter.isRemoved() || this.helicopter.getRotorSpeed() <= 0.02F) {
                stop();
                return;
            }
            this.x = this.helicopter.getX();
            this.y = this.helicopter.getY();
            this.z = this.helicopter.getZ();
            float rotor = this.helicopter.getRotorSpeed();
            this.volume = rotor * 6.0F;
            this.pitch = 0.45F + rotor * 0.55F + speedKmh * 0.0006F;
        }
    }
}
