package com.hbm_m.wiaj.actor;

import java.lang.reflect.Field;

import com.hbm_m.wiaj.VirtualWorld;
import com.hbm_m.wiaj.WiajScene;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.player.Player;

/**
 * Renders a player character inside a WIAJ 3D isometric demonstration.
 * Uses the client's player entity with current skin, cape, and power armor/equipment.
 */
public class ActorPlayer extends ActorBase {

    private float walkSpeed = 0.0F;
    private float prevWalkSpeed = 0.0F;
    private float walkPos = 0.0F;
    private float prevWalkPos = 0.0F;

    private static Field walkPosField;
    private static Field walkSpeedOldField;

    static {
        try {
            walkPosField = WalkAnimationState.class.getDeclaredField("position");
        } catch (NoSuchFieldException e) {
            try {
                walkPosField = WalkAnimationState.class.getDeclaredField("f_267469_");
            } catch (Throwable ignored) {}
        }
        if (walkPosField != null) walkPosField.setAccessible(true);

        try {
            walkSpeedOldField = WalkAnimationState.class.getDeclaredField("speedOld");
        } catch (NoSuchFieldException e) {
            try {
                walkSpeedOldField = WalkAnimationState.class.getDeclaredField("f_267439_");
            } catch (Throwable ignored) {}
        }
        if (walkSpeedOldField != null) walkSpeedOldField.setAccessible(true);
    }

    public ActorPlayer() {
        this.data.putBoolean("visible", true);
    }

    public ActorPlayer(CompoundTag data) {
        this.data = data;
        if (!this.data.contains("visible")) {
            this.data.putBoolean("visible", true);
        }
    }

    @Override
    public void setActorData(CompoundTag data) {
        super.setActorData(data);
        this.walkSpeed = 0.0F;
        this.prevWalkSpeed = 0.0F;
        this.walkPos = 0.0F;
        this.prevWalkPos = 0.0F;
    }

    @Override
    public void drawForegroundComponent(GuiGraphics gui, int w, int h, int ticks, float interp) {
    }

    @Override
    public void drawBackgroundComponent(GuiGraphics gui, VirtualWorld world, int ticks, float interp) {
        if (!data.getBoolean("visible")) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        double x = data.getDouble("x");
        double y = data.getDouble("y");
        double z = data.getDouble("z");
        double prevX = data.contains("prevX") ? data.getDouble("prevX") : x;
        double prevY = data.contains("prevY") ? data.getDouble("prevY") : y;
        double prevZ = data.contains("prevZ") ? data.getDouble("prevZ") : z;

        float yaw = data.getFloat("yaw");
        float prevYaw = data.contains("prevYaw") ? data.getFloat("prevYaw") : yaw;
        float pitch = data.getFloat("pitch");
        float prevPitch = data.contains("prevPitch") ? data.getFloat("prevPitch") : pitch;

        double renderX = Mth.lerp(interp, prevX, x);
        double renderY = Mth.lerp(interp, prevY, y);
        double renderZ = Mth.lerp(interp, prevZ, z);
        float renderYaw = Mth.rotLerp(interp, prevYaw, yaw);
        float renderPitch = Mth.lerp(interp, prevPitch, pitch);

        // Flush any buffered scene geometry before altering lighting uniforms
        gui.flush();

        PoseStack ps = gui.pose();
        ps.pushPose();
        ps.translate(renderX, renderY, renderZ);

        // Undo the negative scale (-10, -10, -10) and Z-flattening (1, 1, 0.5) applied to the normal
        // matrix in GuiWorldInAJar#setupRotation so vertex normals remain orthogonal and unflipped
        if (ps.last().normal().determinant() < 0.0F) {
            float invF3 = 1.0F / Mth.fastInvCubeRoot(2.0F);
            ps.last().normal().scaleLocal(-invF3, -invF3, -0.5F * invF3);
        }

        float oldBodyRot = player.yBodyRot;
        float oldHeadRot = player.yHeadRot;
        float oldYRot = player.getYRot();
        float oldXRot = player.getXRot();
        float oldYBodyRotO = player.yBodyRotO;
        float oldYHeadRotO = player.yHeadRotO;
        float oldXRotO = player.xRotO;
        float oldYRotO = player.yRotO;

        float oldWalkSpeed = player.walkAnimation.speed();
        float oldWalkPos = player.walkAnimation.position();
        float oldSpeedOld = 0.0F;
        try {
            if (walkSpeedOldField != null) oldSpeedOld = walkSpeedOldField.getFloat(player.walkAnimation);
        } catch (Throwable ignored) {}

        // Walking animation progress
        float animSpeed = Mth.lerp(interp, prevWalkSpeed, walkSpeed);
        float animPos = Mth.lerp(interp, prevWalkPos, walkPos);

        // Instant walk animation if displacement is happening within the current tick
        double dx = x - prevX;
        double dz = z - prevZ;
        boolean isHorizMoving = !data.getBoolean("riding") && (dx * dx + dz * dz > 1.0e-5);
        if (isHorizMoving && animSpeed < 0.05F) {
            animSpeed = 1.0F;
            animPos = (ticks + interp) * 0.66F;
        }

        boolean oldShadow = mc.options.entityShadows().get();
        try {
            // Lock body and head rotations to prevent any 20Hz twitching
            player.yBodyRot = renderYaw;
            player.yHeadRot = renderYaw;
            player.setYRot(renderYaw);
            player.setXRot(renderPitch);
            player.yBodyRotO = renderYaw;
            player.yHeadRotO = renderYaw;
            player.xRotO = renderPitch;
            player.yRotO = renderYaw;

            // Apply simulated walking animation
            player.walkAnimation.setSpeed(animSpeed);
            try {
                if (walkSpeedOldField != null) walkSpeedOldField.setFloat(player.walkAnimation, animSpeed);
                if (walkPosField != null) walkPosField.setFloat(player.walkAnimation, animPos);
            } catch (Throwable ignored) {}

            // Top-front diffuse lighting matching the unflipped view-space normal matrix
            Lighting.setupForFlatItems();
            mc.getEntityRenderDispatcher().setRenderShadow(false);
            mc.getEntityRenderDispatcher().render(
                    player,
                    0.0, 0.0, 0.0,
                    renderYaw,
                    1.0F, // 1.0F partial ticks ensures no interpolation against ticking in-world fields
                    ps,
                    gui.bufferSource(),
                    LightTexture.FULL_BRIGHT
            );
            gui.flush();
        } catch (Throwable ignored) {
            // Ignore faux context exceptions
        } finally {
            player.yBodyRot = oldBodyRot;
            player.yHeadRot = oldHeadRot;
            player.setYRot(oldYRot);
            player.setXRot(oldXRot);
            player.yBodyRotO = oldYBodyRotO;
            player.yHeadRotO = oldYHeadRotO;
            player.xRotO = oldXRotO;
            player.yRotO = oldYRotO;

            try {
                player.walkAnimation.setSpeed(oldWalkSpeed);
                if (walkSpeedOldField != null) walkSpeedOldField.setFloat(player.walkAnimation, oldSpeedOld);
                if (walkPosField != null) walkPosField.setFloat(player.walkAnimation, oldWalkPos);
            } catch (Throwable ignored) {}

            mc.getEntityRenderDispatcher().setRenderShadow(oldShadow);
            Lighting.setupFor3DItems();
            ps.popPose();
        }
    }

    @Override
    public void updateActor(WiajScene scene) {
        double curX = data.getDouble("x");
        double curZ = data.getDouble("z");
        double oldPrevX = data.contains("prevX") ? data.getDouble("prevX") : curX;
        double oldPrevZ = data.contains("prevZ") ? data.getDouble("prevZ") : curZ;
        double dx = curX - oldPrevX;
        double dz = curZ - oldPrevZ;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        boolean moving = !data.getBoolean("riding") && dist > 1.0e-4F;
        float targetSpeed = moving ? Math.min(dist * 5.0F, 1.0F) : 0.0F;

        this.prevWalkSpeed = this.walkSpeed;
        this.walkSpeed += (targetSpeed - this.walkSpeed) * 0.4F;
        this.prevWalkPos = this.walkPos;
        this.walkPos += this.walkSpeed * 0.8F;

        data.putDouble("prevX", curX);
        data.putDouble("prevY", data.getDouble("y"));
        data.putDouble("prevZ", curZ);
        data.putFloat("prevYaw", data.getFloat("yaw"));
        data.putFloat("prevPitch", data.getFloat("pitch"));

        if (data.getBoolean("riding")) {
            int elevId = data.getInt("ridingElevatorId");
            ISpecialActor elevActor = scene.script.actors.get(elevId);
            if (elevActor != null && elevActor.getActorData() != null) {
                CompoundTag elevData = elevActor.getActorData();
                double elevY = elevData.getDouble("y");
                double elevExt = elevData.getDouble("extension");
                double offsetY = data.contains("ridingOffsetY") ? data.getDouble("ridingOffsetY") : 1.0;
                data.putDouble("y", elevY + offsetY + elevExt);
            }
        }
    }
}