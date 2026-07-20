//? if forge {
package com.hbm_m.client.compat.create;

import com.hbm_m.block.entity.doors.DoorDecl;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Клиентский звук двери на контрапшене. Полностью управляется из packet-applier
 * (один вызов {@link #onStart} на toggle), НЕ зависит от behaviour-tick (который
 * был хрупким и вызывал заикивание/бесконечный loop).
 */
@OnlyIn(Dist.CLIENT)
public final class ContraptionDoorSoundHelper {

    private static final WeakHashMap<MovementContext, State> ACTIVE = new WeakHashMap<>();

    private ContraptionDoorSoundHelper() {}

    public static void onStart(MovementContext ctx, DoorDecl decl, boolean opening) {
        if (ctx == null || decl == null || ctx.position == null) return;
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        State existing = ACTIVE.get(ctx);
        long now = System.currentTimeMillis();
        if (existing != null && (now - existing.startMs) < 300L) return;

        stopAll(ctx, null, false);

        float vol = decl.getSoundVolume();
        int openTime = Math.max(1, decl.getOpenTime());
        long capMs = openTime * 50L; // Ровно столько, сколько идет анимация
        
        SoundEvent start = opening ? decl.getOpenSoundStart() : decl.getCloseSoundStart();
        SoundEvent end = opening ? decl.getOpenSoundEnd() : decl.getCloseSoundEnd();
        SoundEvent loop = opening ? decl.getOpenSoundLoop() : decl.getCloseSoundLoop();
        SoundEvent loop2 = decl.getSoundLoop2();

        Vec3 p = ctx.position;
        if (start != null) level.playLocalSound(p.x, p.y, p.z, start, SoundSource.BLOCKS, vol, 1f, false);

        State state = new State(now);
        if (loop != null) {
            AbstractTickableSoundInstance s = makeLoop(ctx, loop, vol, state, end, capMs);
            state.loops.add(s);
        }
        if (loop2 != null) {
            AbstractTickableSoundInstance s = makeLoop(ctx, loop2, vol, state, end, capMs);
            state.loops.add(s);
        }
        if (state.loops.isEmpty() && end != null) {
            state.loops.add(makeEndTimer(ctx, state, end, vol, capMs));
        }
        var mgr = Minecraft.getInstance().getSoundManager();
        for (AbstractTickableSoundInstance s : state.loops) mgr.play(s);
        ACTIVE.put(ctx, state);
    }

    public static void stopAll(MovementContext ctx, DoorDecl decl, boolean ignored) {
        if (ctx == null) return;
        State state = ACTIVE.remove(ctx);
        if (state == null) return;
        var mgr = Minecraft.getInstance().getSoundManager();
        for (AbstractTickableSoundInstance s : state.loops) mgr.stop(s);
    }

    private static AbstractTickableSoundInstance makeLoop(MovementContext ctx, SoundEvent sound, float vol,
                                                          State state, SoundEvent endSound, long capMs) {
        final long startMs = state.startMs;
        return new AbstractTickableSoundInstance(sound, SoundSource.BLOCKS, RandomSource.create()) {
            {
                this.volume = vol;
                this.pitch = 1.0f;
                this.looping = true;
                this.relative = false;
                if (ctx.position != null) { this.x = ctx.position.x; this.y = ctx.position.y; this.z = ctx.position.z; }
            }
            @Override public void tick() {
                long age = System.currentTimeMillis() - startMs;
                if (ctx.position == null || isContraptionGone(ctx) || age > capMs + 10000L) { this.stop(); return; }
                if (age >= capMs) {
                    if (!state.endPlayed) {
                        state.endPlayed = true;
                        playEnd(ctx, endSound, vol);
                    }
                    this.stop();
                    return;
                }
                this.x = ctx.position.x; this.y = ctx.position.y; this.z = ctx.position.z;
            }
        };
    }

    private static AbstractTickableSoundInstance makeEndTimer(MovementContext ctx, State state,
                                                              SoundEvent endSound, float vol, long capMs) {
        final long startMs = state.startMs;
        return new AbstractTickableSoundInstance(endSound, SoundSource.BLOCKS, RandomSource.create()) {
            { this.volume = 0.0001f; this.pitch = 1f; this.looping = true; this.relative = false; }
            @Override public void tick() {
                long age = System.currentTimeMillis() - startMs;
                if (isContraptionGone(ctx) || age > capMs + 10000L) { this.stop(); return; }
                if (age >= capMs) {
                    if (!state.endPlayed) { state.endPlayed = true; playEnd(ctx, endSound, vol); }
                    this.stop();
                }
            }
        };
    }

    private static void playEnd(MovementContext ctx, SoundEvent endSound, float vol) {
        if (endSound == null || ctx.position == null) return;
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        Vec3 p = ctx.position;
        // playLocalSound - воспроизводит 3D звук только для локального игрока (как раз то, что нужно на клиенте)
        level.playLocalSound(p.x, p.y, p.z, endSound, SoundSource.BLOCKS, vol, 1f, false);
    }

    private static boolean isContraptionGone(MovementContext ctx) {
        try {
            return ctx.contraption == null || ctx.contraption.entity == null
                    || ctx.contraption.entity.isRemoved();
        } catch (Throwable t) {
            return true;
        }
    }

    private static final class State {
        final long startMs;
        final List<AbstractTickableSoundInstance> loops = new ArrayList<>();
        volatile boolean endPlayed;
        State(long startMs) { this.startMs = startMs; }
    }
}
//?}