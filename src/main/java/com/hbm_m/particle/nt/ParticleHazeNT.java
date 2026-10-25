package com.hbm_m.particle.nt;

import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 1:1-Port von {@code ParticleHaze}: ein grosser, kaum sichtbarer Hitzeschleier aus 25 Fetzen,
 * der pro Tick einen Lava-Funken auf die Oberflaeche in der Umgebung setzt.
 *
 * <p>Die Fetzen liegen im Original kumulativ versetzt, weil {@code glTranslatef} innerhalb der
 * Schleife ohne Push/Pop steht - das bleibt so.</p>
 */
public class ParticleHazeNT extends ParticleQuadNT {

    private static final ResourceLocation HAZE = tex("haze");

    public ParticleHazeNT(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, HAZE);
        this.lifetime = 600 + random.nextInt(100);
        this.rCol = this.gCol = this.bCol = 0;
        this.particleScale = 10F;
        this.fullBright = true;
    }

    public ParticleHazeNT(ClientLevel level, double x, double y, double z, float r, float g, float b, float scale) {
        super(level, x, y, z, HAZE);
        this.lifetime = 100 + random.nextInt(40);
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        this.particleScale = scale;
        this.fullBright = true;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.age++;
        if (this.age >= lifetime) this.dead = true;

        this.xd *= 0.9599999785423279D;
        this.yd *= 0.9599999785423279D;
        this.zd *= 0.9599999785423279D;
        if (this.onGround) {
            this.xd *= 0.699999988079071D;
            this.zd *= 0.699999988079071D;
        }

        int ix = (int) Math.floor(x) + random.nextInt(15) - 7;
        int iz = (int) Math.floor(z) + random.nextInt(15) - 7;
        int iy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, ix, iz);
        level.addParticle(ParticleTypes.LAVA, ix + random.nextDouble(), iy + 0.1, iz + random.nextDouble(), 0.0, 0.0, 0.0);
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float pt, PoseStack pose) {
        float a = (float) Math.sin(age * Math.PI / 400F) * 0.25F * 0.1F;
        if (a <= 0) return;

        VertexConsumer c = buffer();
        Vec3 p = camPos(camera, pt);
        Vector3f l = left(camera);
        Vector3f u = up(camera);
        Random rand = new Random(50);
        double tx = 0, ty = 0, tz = 0;

        for (int i = 0; i < 25; i++) {
            tx += rand.nextGaussian() * 2.5D;
            ty += rand.nextGaussian() * 0.15D;
            tz += rand.nextGaussian() * 2.5D;
            double size = (rand.nextDouble() * 0.25 + 0.75) * particleScale;
            float pX = (float) (p.x + rand.nextGaussian() * 0.5 + tx);
            float pY = (float) (p.y + rand.nextGaussian() * 0.5 + ty);
            float pZ = (float) (p.z + rand.nextGaussian() * 0.5 + tz);
            quad(c, pX, pY, pZ, l, u, (float) size, 1F, 1F, 1F, a, 0xF000F0, 0, 0, 1, 1);
        }
    }
}
