package com.hbm_m.particle.nt;

import java.util.Random;

import com.hbm_m.client.ClientRenderHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1-Port von {@code ParticleDebris}: ein fliegender Brocken aus echten Bloecken, den
 * {@code ExplosionCreator} aus der Umgebung des Einschlags zusammenklaubt. Das Original legt die
 * Bloecke in eine {@code WorldInAJar} und zeichnet sie mit {@code RenderBlocks}; hier ist die
 * "Welt im Glas" ein schlichtes {@link BlockState}-Feld, gezeichnet ueber den Blockrenderer.
 * Jeder dritte Brocken zieht eine Raketenflamme hinter sich her.
 */
public class ParticleDebrisNT extends ParticleNT {

    private static final Random RNG = new Random();
    private static int nextId = 0;

    public final int sizeX, sizeY, sizeZ;
    private final BlockState[] jar;
    private final int id = nextId++;
    private float rotationPitch, prevRotationPitch, rotationYaw, prevRotationYaw;

    public ParticleDebrisNT(ClientLevel level, double x, double y, double z, double mx, double my, double mz, int size) {
        super(level, x, y, z);
        double mult = 3;
        this.xd = mx * mult;
        this.yd = my * mult;
        this.zd = mz * mult;
        this.lifetime = 100;
        this.gravity = 0.15F;
        this.noClip = true;
        this.sizeX = this.sizeY = this.sizeZ = Math.max(size, 0);
        this.jar = new BlockState[Math.max(1, sizeX * sizeY * sizeZ)];
    }

    public BlockState getBlock(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= sizeX || y >= sizeY || z >= sizeZ) return null;
        return jar[(x * sizeY + y) * sizeZ + z];
    }

    public boolean isAir(int x, int y, int z) {
        BlockState s = getBlock(x, y, z);
        return s == null || s.isAir();
    }

    public void setBlock(int x, int y, int z, BlockState state) {
        if (x < 0 || y < 0 || z < 0 || x >= sizeX || y >= sizeY || z >= sizeZ) return;
        jar[(x * sizeY + y) * sizeZ + z] = state;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age > 5) this.noClip = false;

        RNG.setSeed(this.id);
        this.prevRotationPitch = this.rotationPitch;
        this.prevRotationYaw = this.rotationYaw;
        this.rotationPitch += RNG.nextFloat() * 10;
        this.rotationYaw += RNG.nextFloat() * 10;

        if (this.id % 3 == 0) {
            ParticleRocketFlameNT fx = new ParticleRocketFlameNT(level, x, y, z).setScale(1F * Math.max(sizeY, 6) / 16F);
            fx.setMaxAge(50);
            ParticleEngineNT.INSTANCE.add(fx);
        }

        this.yd -= this.gravity;
        this.move(this.xd, this.yd, this.zd);
        this.age++;
        if (this.onGround) this.dead = true;
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float pt, PoseStack levelPose) {
        if (sizeX == 0) return;
        Vec3 p = virtualizedOffset(Mth.lerp(pt, xo, x), Mth.lerp(pt, yo, y), Mth.lerp(pt, zo, z), camera);
        int light = LevelRenderer.getLightColor(level, BlockPos.containing(x, y, z));

        PoseStack pose = new PoseStack();
        pose.translate(p.x, p.y, p.z);
        pose.mulPose(Axis.YP.rotationDegrees(prevRotationPitch + (rotationPitch - prevRotationPitch) * pt));
        pose.mulPose(Axis.ZP.rotationDegrees(prevRotationYaw + (rotationYaw - prevRotationYaw) * pt));
        pose.translate(-sizeX / 2D, -sizeY / 2D, -sizeZ / 2D);

        var dispatcher = Minecraft.getInstance().getBlockRenderer();
        for (int ix = 0; ix < sizeX; ix++) {
            for (int iy = 0; iy < sizeY; iy++) {
                for (int iz = 0; iz < sizeZ; iz++) {
                    BlockState state = getBlock(ix, iy, iz);
                    if (state == null || state.isAir() || state.getRenderShape() != RenderShape.MODEL) continue;
                    pose.pushPose();
                    pose.translate(ix, iy, iz);
                    try {
                        dispatcher.renderSingleBlock(state, pose, ParticleEngineNT.buffer(), light, OverlayTexture.NO_OVERLAY);
                    } catch (Exception ignoredEx) { }
                    pose.popPose();
                }
            }
        }
    }

    @Override
    public RenderType getRenderType() {
        return ClientRenderHandler.CustomRenderTypes.TOWER_PARTICLES.apply(ParticleQuadNT.PARTICLE_BASE);
    }
}
