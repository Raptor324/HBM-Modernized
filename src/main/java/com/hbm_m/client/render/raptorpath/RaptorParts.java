package com.hbm_m.client.render.raptorpath;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.machine.MachineSpecBuilder;
import com.hbm_m.client.render.machine.PartAnimator;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Kleine Bausteine fuer die Render-Migration: Teile aus {@link ObjAtlasMesh} an Raptors
 * {@code MachineSpecBuilder.dynamicPart} haengen (Auswahl je BE-Zustand, an/aus), ohne Raptors Klassen zu aendern.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public final class RaptorParts {

    private RaptorParts() {}

    /** Schluessel fuer "nicht zeichnen" (leere Teilgeometrie, kein GPU-Puffer). */
    public static final String NONE = "none";

    /** Statisches Teil (zeichnet bis zur statischen Sichtweite). */
    public static <T extends BlockEntity> MachineSpecBuilder<T> mesh(MachineSpecBuilder<T> b, String name, ObjAtlasMesh m) {
        return b.dynamicPart(name, be -> m.quads(), be -> m.cacheKey());
    }

    /** Bewegtes Teil (Raptor: nur bis zur Animations-Sichtweite). */
    public static <T extends BlockEntity> MachineSpecBuilder<T> mesh(MachineSpecBuilder<T> b, String name, PartAnimator<T> anim, ObjAtlasMesh m) {
        return b.dynamicPart(name, anim, be -> m.quads(), be -> m.cacheKey());
    }

    /** Statisches Teil, Geometrie je Zustand gewaehlt ({@code null} = nichts). */
    public static <T extends BlockEntity> MachineSpecBuilder<T> select(MachineSpecBuilder<T> b, String name,
                                                                       Function<T, ObjAtlasMesh> sel) {
        return b.dynamicPart(name, be -> quads(sel.apply(be)), be -> key(sel.apply(be)));
    }

    /** Bewegtes Teil, Geometrie je Zustand gewaehlt ({@code null} = nichts). */
    public static <T extends BlockEntity> MachineSpecBuilder<T> select(MachineSpecBuilder<T> b, String name, PartAnimator<T> anim,
                                                                       Function<T, ObjAtlasMesh> sel) {
        return b.dynamicPart(name, anim, be -> quads(sel.apply(be)), be -> key(sel.apply(be)));
    }

    /** Statisches Teil nur, wenn {@code cond}. */
    public static <T extends BlockEntity> MachineSpecBuilder<T> when(MachineSpecBuilder<T> b, String name, Predicate<T> cond, ObjAtlasMesh m) {
        return select(b, name, be -> cond.test(be) ? m : null);
    }

    /** Bewegtes Teil nur, wenn {@code cond}. */
    public static <T extends BlockEntity> MachineSpecBuilder<T> when(MachineSpecBuilder<T> b, String name, PartAnimator<T> anim,
                                                                     Predicate<T> cond, ObjAtlasMesh m) {
        return select(b, name, anim, be -> cond.test(be) ? m : null);
    }

    /**
     * Teil, das sich nur zeitweise bewegt und in Ruhe exakt in Modelllage steht (Animator = Identitaet, z.B. Rotor
     * aus, Motor ohne Treibstoff): in Ruhe statisch (bleibt wie vorher bis zur statischen Sichtweite sichtbar),
     * nur waehrend der Bewegung als bewegtes Teil.
     */
    public static <T extends BlockEntity> MachineSpecBuilder<T> idleStatic(MachineSpecBuilder<T> b, String name, PartAnimator<T> anim,
                                                                         Predicate<T> moving, ObjAtlasMesh m) {
        when(b, name, anim, moving, m);
        return when(b, name + "_idle", moving.negate(), m);
    }

    private static List<net.minecraft.client.renderer.block.model.BakedQuad> quads(@Nullable ObjAtlasMesh m) {
        return m == null ? List.of() : m.quads();
    }

    private static String key(@Nullable ObjAtlasMesh m) {
        return m == null ? NONE : m.cacheKey();
    }

    /** {@code GL11.glRotatef(deg, 0, 1, 0)} im Blocktransform. */
    public static void rotY(LegacyAnimator a, float deg) {
        a.rotate(deg, 0, 1, 0);
    }
}
