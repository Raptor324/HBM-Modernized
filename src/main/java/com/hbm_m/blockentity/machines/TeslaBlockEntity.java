package com.hbm_m.blockentity.machines;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.util.ArmorUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1-Port von {@code TileEntityTesla} (1.7.10): eine Teslaspule, die alles Lebende im Umkreis
 * unter Strom setzt.
 *
 * <p>Solange mindestens 5.000 Energie da sind, entlaedt sie sich jeden Tick um genau diesen Betrag
 * und trifft alle Ziele im Radius 10, die in Sichtlinie stehen. Der Schaden ist die halbe maximale
 * Lebensenergie des Ziels, begrenzt auf 3 bis 20, und wird <b>auf alle Ziele aufgeteilt</b> - eine
 * Spule gegen eine grosse Horde tut jedem Einzelnen also wenig.</p>
 *
 * <p><b>Abweichungen:</b> Die Sonderfaelle des Originals fuer seine eigenen Krabbenarten
 * (Taint-, Tesla- und Cyberkrabbe werden geheilt statt getroffen) entfallen, weil es diese Kreaturen
 * im Port nicht gibt. Ozelots bleiben wie im Original verschont, Creeper werden gezuendet. Der
 * Meteoritenakku unter der Spule speist sie wie im Original gratis.</p>

 */
public class TeslaBlockEntity extends BaseMachineBlockEntity {

    /** Original: {@code maxPower = 100000}. */
    private static final long MAX_POWER = 100_000L;
    /** Original: {@code if(power >= 5000) power -= 5000;} */
    private static final long POWER_PER_ZAP = 5_000L;
    /** Original: {@code range = 10}. */
    private static final double RANGE = 10.0D;
    /** Original: {@code offset = 1.75} - die Entladung geht von der Spulenspitze aus. */
    public static final double ORIGIN_OFFSET_Y = 1.75D;


    /** Positionen der zuletzt getroffenen Ziele - fuer die Blitzdarstellung. */
    private final List<Vec3> targets = new ArrayList<>();

    public TeslaBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TESLA_BE.get(), pos, state, 0, MAX_POWER, MAX_POWER, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TeslaBlockEntity be) {
        if (level.isClientSide()) return;

        be.ensureNetworkInitialized();
        be.targets.clear();

        // Original: ein Meteoritenakku direkt unter der Spule speist sie gratis
        if (level.getBlockState(pos.below()).is(com.hbm_m.block.ModBlocks.METEOR_BATTERY.get())) be.setEnergyStored(MAX_POWER);

        // Discharge (entity query + per-candidate clips) phase-spread, not every tick.
        if (be.probeDue(5) && be.getEnergyStored() >= POWER_PER_ZAP) {
            be.setEnergyStored(be.getEnergyStored() - POWER_PER_ZAP);

            // World-space origin: on a Sable ship the block position is in the plot grid.
            Vec3 origin = com.hbm_m.compat.sable.SableCompat.toWorld(level,
                    pos.getX() + 0.5D, pos.getY() + ORIGIN_OFFSET_Y, pos.getZ() + 0.5D);
            be.targets.addAll(zap(level, origin.x, origin.y, origin.z, RANGE, null));
        }

        // Sync only on a state delta: energy + arc-list fingerprint. No targets (and no prior
        // arcs to clear) means no sync — the renderer has nothing new to draw.
        be.syncIfStateChanged(be.getEnergyStored(), be.arcFingerprint());
    }

    /** Fingerprint of the arc end-point list shipped by writeNbtData. */
    private long arcFingerprint() {
        long h = targets.size();
        for (int i = 0; i < targets.size(); i++) {
            Vec3 t = targets.get(i);
            h = h * 31L + Double.doubleToRawLongBits(t.x);
            h = h * 31L + Double.doubleToRawLongBits(t.y);
            h = h * 31L + Double.doubleToRawLongBits(t.z);
        }
        return h;
    }

    /**
     * 1:1-Port der statischen {@code zap(...)}: liefert die Punkte, an denen ein Blitz endet.
     * Wird im Original auch von der Teslakanone genutzt, darum bleibt sie statisch und nimmt
     * eine auszunehmende Quelle entgegen.
     */
    public static List<Vec3> zap(Level level, double x, double y, double z, double radius, Entity source) {
        List<Vec3> hits = new ArrayList<>();

        AABB box = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box);

        for (LivingEntity e : candidates) {
            // Original: Ozelots und die ausloesende Kreatur bleiben verschont.
            if (e instanceof Ocelot || e == source) continue;

            double eyeY = e.getY() + e.getBbHeight() / 2D;
            Vec3 toTarget = new Vec3(e.getX() - x, eyeY - y, e.getZ() - z);
            if (toTarget.length() > radius) continue;

            if (isObstructed(level, x, y, z, e.getX(), eyeY, e.getZ())) continue;

            // Original: Krabben werden nur angebunden; Tesla-/Taint-Krabben heilen sich dabei.
            if (e instanceof com.hbm_m.entity.mob.EntityTaintCrab) {
                hits.add(new Vec3(e.getX(), e.getY() + 1.25, e.getZ()));
                e.heal(15F);
                continue;
            }

            if (e instanceof com.hbm_m.entity.mob.EntityTeslaCrab) {
                hits.add(new Vec3(e.getX(), e.getY() + 1, e.getZ()));
                e.heal(10F);
                continue;
            }

            if (e instanceof com.hbm_m.entity.mob.EntityCyberCrab) {
                hits.add(new Vec3(e.getX(), eyeY, e.getZ()));
                continue;
            }


            // Original: Creeper werden gezuendet statt getroffen.
            if (e instanceof Creeper creeper) {
                creeper.ignite();
                hits.add(new Vec3(e.getX(), eyeY, e.getZ()));
                continue;
            }

            boolean faraday = e instanceof Player player && ArmorUtil.checkForFaraday(player);

            if (!faraday) {
                // Original: halbe Maximalgesundheit, geklemmt auf 3..20, geteilt durch die Zielzahl.
                float damage = Mth.clamp(e.getMaxHealth() * 0.5F, 3F, 20F) / (float) candidates.size();

                if (e.hurt(ModDamageSources.electricity(level), damage)) {
                    level.playSound(null, e.getX(), e.getY(), e.getZ(),
                            com.hbm_m.sound.HbmSoundsNT.get("hbm:weapon.tesla"), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }

            // Original: clientseitig (Krabben) endet der Blitz bei Spielern auf Fusshoehe
            double offset = 0;
            if (source != null && e instanceof Player && level.isClientSide)
                offset = e.getBbHeight();

            hits.add(new Vec3(e.getX(), eyeY - offset, e.getZ()));

        }

        return hits;
    }

    /** Original: {@code Library.isObstructed} - Sichtlinie zwischen Spule und Ziel. */
    private static boolean isObstructed(Level level, double x, double y, double z,
                                        double tx, double ty, double tz) {
        BlockHitResult hit = level.clip(com.hbm_m.platform.LevelHooks.clipContext(
                new Vec3(x, y, z), new Vec3(tx, ty, tz), ClipContext.Fluid.NONE));
        return hit.getType() != HitResult.Type.MISS;
    }

    // The arcs are drawn by TeslaRenderer; 1.7.10 syncs the end points the same way. Stored
    // relative to the coil origin so the renderer stays in block space.
    @Override
    protected void writeNbtData(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        Vec3 origin = level == null ? Vec3.ZERO : com.hbm_m.compat.sable.SableCompat.toWorld(level,
                worldPosition.getX() + 0.5D, worldPosition.getY() + ORIGIN_OFFSET_Y, worldPosition.getZ() + 0.5D);
        tag.putInt("arcCount", targets.size());
        for (int i = 0; i < targets.size(); i++) {
            Vec3 t = targets.get(i).subtract(origin);
            tag.putDouble("aX" + i, t.x);
            tag.putDouble("aY" + i, t.y);
            tag.putDouble("aZ" + i, t.z);
        }

    }

    @Override
    protected void readNbtData(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        targets.clear();
        int arcs = tag.getInt("arcCount");
        for (int i = 0; i < arcs; i++) {
            targets.add(new Vec3(tag.getDouble("aX" + i), tag.getDouble("aY" + i), tag.getDouble("aZ" + i)));
        }
    }

    public List<Vec3> getTargets() {
        return targets;
    }


    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        // Original: super(0) - kein Inventar.
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.tesla");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
            int id, net.minecraft.world.entity.player.Inventory inv, Player player) {
        // Original: kein GUI (getName() liefert dort einen leeren String).
        return null;
    }

    /** 1:1 TileEntityTesla.getRenderBoundingBox (Render-Migration: Raptors Pfad cullt nach dieser Box, der alte BER zeichnete immer). */
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return com.hbm_m.platform.RenderBounds.INFINITE;
    }

}
