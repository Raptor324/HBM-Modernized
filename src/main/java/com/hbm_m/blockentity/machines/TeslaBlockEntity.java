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
 * Meteoritenakku unter der Spule, der sie im Original gratis speist, ist ebenfalls nicht portiert.</p>
 */
public class TeslaBlockEntity extends BaseMachineBlockEntity {

    /** Original: {@code maxPower = 100000}. */
    private static final long MAX_POWER = 100_000L;
    /** Original: {@code if(power >= 5000) power -= 5000;} */
    private static final long POWER_PER_ZAP = 5_000L;
    /** Original: {@code range = 10}. */
    private static final double RANGE = 10.0D;
    /** Original: {@code offset = 1.75} - die Entladung geht von der Spulenspitze aus. */
    private static final double ORIGIN_OFFSET_Y = 1.75D;

    /** Positionen der zuletzt getroffenen Ziele - fuer die Blitzdarstellung. */
    private final List<Vec3> targets = new ArrayList<>();

    public TeslaBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TESLA_BE.get(), pos, state, 0, MAX_POWER, MAX_POWER, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TeslaBlockEntity be) {
        if (level.isClientSide()) return;

        be.ensureNetworkInitialized();
        be.targets.clear();

        if (be.getEnergyStored() >= POWER_PER_ZAP) {
            be.setEnergyStored(be.getEnergyStored() - POWER_PER_ZAP);

            be.targets.addAll(zap(level,
                    pos.getX() + 0.5D, pos.getY() + ORIGIN_OFFSET_Y, pos.getZ() + 0.5D,
                    RANGE, null));
        }

        be.setChanged();
        be.sendUpdateToClient();
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
                            SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }

            hits.add(new Vec3(e.getX(), eyeY, e.getZ()));
        }

        return hits;
    }

    /** Original: {@code Library.isObstructed} - Sichtlinie zwischen Spule und Ziel. */
    private static boolean isObstructed(Level level, double x, double y, double z,
                                        double tx, double ty, double tz) {
        BlockHitResult hit = level.clip(new ClipContext(
                new Vec3(x, y, z), new Vec3(tx, ty, tz),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        return hit.getType() != HitResult.Type.MISS;
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
}
