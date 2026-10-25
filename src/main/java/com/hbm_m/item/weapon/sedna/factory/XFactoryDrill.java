package com.hbm_m.item.weapon.sedna.factory;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.weapon.sedna.Crosshair;
import com.hbm_m.item.weapon.sedna.GunConfig;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm_m.item.weapon.sedna.Receiver;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.item.weapon.sedna.impl.ItemGunDrill;
import com.hbm_m.item.weapon.sedna.mags.IMagazine;
import com.hbm_m.item.weapon.sedna.mags.MagazineLiquidEngine;
import com.hbm_m.item.weapon.sedna.mods.XWeaponModManager;
import com.hbm_m.render.anim.AnimationEnums.GunAnimation;
import com.hbm_m.render.anim.BusAnimation;
import com.hbm_m.render.anim.BusAnimationKeyframe.IType;
import com.hbm_m.render.anim.BusAnimationSequence;
import com.hbm_m.util.EntityDamageUtil;
import com.hbm_m.util.MovingObjectPosition;
import com.hbm_m.util.MovingObjectPosition.MovingObjectType;
import com.hbm_m.util.Vec3NT;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code XFactoryDrill}: Bohrer (Werkzeugwaffe mit Fluessig-/Elektromotor, Flaechenabbau). */
public class XFactoryDrill {

    public static final String D_REACH =	"D_REACH";
    public static final String F_DTNEG =	"F_DTNEG";
    public static final String F_PIERCE =	"F_PIERCE";
    public static final String I_AOE =		"I_AOE";
    public static final String I_HARVEST =	"I_HARVEST";

    public static void init() {

        GunFactory.reg("gun_drill", new ItemGunDrill(WeaponQuality.UTILITY, new GunConfig()
                .dura(3_000).draw(10).inspect(55).hideCrosshair(false).crosshair(Crosshair.L_CIRCUMFLEX)
                .rec(new Receiver(0)
                        .dmg(10F).delay(20).dry(30).auto(true).jam(0)
                        .mag(new LazyLiquidEngine(0, 4_000, ModFluids.GASOLINE::getSource, ModFluids.GASOLINE_LEADED::getSource, ModFluids.COALGAS::getSource, ModFluids.COALGAS_LEADED::getSource))
                        .offset(1, -0.0625 * 2.5, -0.25D)
                        .canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(LAMBDA_DRILL_FIRE))
                .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD).decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
                .anim(LAMBDA_DRILL_ANIMS).orchestra(Orchestras.ORCHESTRA_DRILL)
                ));
    }

    /**
     * Portabweichung: die Fluide werden erst nach den Gegenstaenden registriert (Forge registriert Bloecke und
     * Gegenstaende zuerst), daher fuellt dieses Magazin {@link MagazineLiquidEngine#acceptedTypes} beim ersten Zugriff.
     */
    public static class LazyLiquidEngine extends MagazineLiquidEngine {

        private final Supplier<Fluid>[] lazyTypes;

        @SafeVarargs
        public LazyLiquidEngine(int index, int capacity, Supplier<Fluid>... types) {
            super(index, capacity, new Fluid[types.length]);
            this.lazyTypes = types;
        }

        public Fluid[] resolve() {
            for (int i = 0; i < acceptedTypes.length; i++) if (acceptedTypes[i] == null) acceptedTypes[i] = lazyTypes[i].get();
            return acceptedTypes;
        }

        @Override public Fluid getType(ItemStack stack, @Nullable Container inventory) { resolve(); return super.getType(stack, inventory); }
        @Override public int getAmount(ItemStack stack, @Nullable Container inventory) { resolve(); return super.getAmount(stack, inventory); }
    }

    /** Liefert die akzeptierten Fluide eines Fluessigmotors (loest {@link LazyLiquidEngine} vorher auf). */
    public static Fluid[] acceptedTypes(MagazineLiquidEngine engine) {
        if (engine instanceof LazyLiquidEngine lazy) return lazy.resolve();
        return engine.acceptedTypes;
    }

    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_DRILL_FIRE = (stack, ctx) -> {
        doStandardFire(stack, ctx, GunAnimation.CYCLE, true);
    };

    @SuppressWarnings("rawtypes")
    public static void doStandardFire(ItemStack stack, LambdaContext ctx, GunAnimation anim, boolean calcWear) {
        Player player = ctx.getPlayer();
        int index = ctx.configIndex;
        if (anim != null) ItemGunBaseNT.playAnimation(player, stack, anim, ctx.configIndex);

        Receiver primary = ctx.config.getReceivers(stack)[0];
        IMagazine mag = primary.getMagazine(stack);

        // Original ruft getMouseOver ungeprueft mit getPlayer() auf; ohne Spieler gibt es hier keinen Treffer
        MovingObjectPosition mop = player == null ? null : getMouseOver(player, getModdableReach(stack, 5.0D));
        if (mop != null) {
            if (mop.typeOfHit == MovingObjectType.ENTITY) {
                float damage = primary.getBaseDamage(stack);
                if (mop.entityHit instanceof LivingEntity living) {
                    EntityDamageUtil.attackEntityFromNT(living, player.damageSources().playerAttack(player), damage, true, true, 0.1F, getModdableDTNegation(stack, 2F), getModdablePiercing(stack, 0.15F));
                } else {
                    mop.entityHit.hurt(player.damageSources().playerAttack(player), damage);
                }
            }
            if (player != null && mop.typeOfHit == MovingObjectType.BLOCK) {

                int aoe = player.isShiftKeyDown() ? 0 : getModdableAoE(stack, 1);
                for (int i = -aoe; i <= aoe; i++) for (int j = -aoe; j <= aoe; j++) for (int k = -aoe; k <= aoe; k++) {
                    breakExtraBlock(player.level(), mop.blockX + i, mop.blockY + j, mop.blockZ + k, player, mop.blockX, mop.blockY, mop.blockZ);
                }

                didPlink = false;
            }
        }

        int ammoToUse = 10;
        if (XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_ENGINE_ELECTRIC)) ammoToUse = 1_000; // that's 1,000 operations
        mag.useUpAmmo(stack, ctx.inventory, ammoToUse);
        if (calcWear) ItemGunBaseNT.setWear(stack, index, Math.min(ItemGunBaseNT.getWear(stack, index), ctx.config.getDurability(stack)));
    }

    public static boolean didPlink = false;
    /**
     * Portergaenzung: Forge bricht den Abbau ab, wenn {@code canAttackBlock} des gehaltenen Gegenstands false liefert
     * (bei Waffen immer). Waehrend der Bohrer selbst abbaut, erlaubt {@link ItemGunDrill} es ueber diesen Schalter.
     */
    public static boolean harvesting = false;

    public static void breakExtraBlock(Level world, int x, int y, int z, Player playerEntity, int refX, int refY, int refZ) {
        BlockPos pos = new BlockPos(x, y, z);
        if (world.isEmptyBlock(pos)) return;
        if (!(playerEntity instanceof ServerPlayer player)) return;

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (!state.canHarvestBlock(world, pos, player) || (state.getDestroySpeed(world, pos) == -1.0F && state.getDestroyProgress(player, world, pos) == 0.0F) || block == ModBlocks.STONE_KEYHOLE.get()) {
            if (!didPlink) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.5F, 0.8F + world.random.nextFloat() * 0.6F);
                didPlink = true;
            }
            return;

        }

        // we are serverside and tryHarvestBlock already invokes the 2001 packet for every player except the user, so we manually send it for the user as well
        harvesting = true;
        try {
            player.gameMode.destroyBlock(pos);
        } finally {
            harvesting = false;
        }

        if (world.getBlockState(pos).isAir()) { // only do this when the block was destroyed. if the block doesn't create air when broken, this breaks, but it's no big deal
            player.connection.send(new ClientboundLevelEventPacket(2001, pos, Block.getId(state), false));
        }
    }

    // this system technically doesn't need to be part of the GunCfg or Receiver or anything, we can just do this and it works the exact same
    public static double getModdableReach(ItemStack stack, double base) {		return XWeaponModManager.eval(base, stack, D_REACH, WeaponItems.gun("gun_drill"), 0); }
    public static float getModdableDTNegation(ItemStack stack, float base) {	return XWeaponModManager.eval(base, stack, F_DTNEG, WeaponItems.gun("gun_drill"), 0); }
    public static float getModdablePiercing(ItemStack stack, float base) {		return XWeaponModManager.eval(base, stack, F_PIERCE, WeaponItems.gun("gun_drill"), 0); }
    public static int getModdableAoE(ItemStack stack, int base) {				return XWeaponModManager.eval(base, stack, I_AOE, WeaponItems.gun("gun_drill"), 0); }
    public static int getModdableHarvestLevel(ItemStack stack, int base) {		return XWeaponModManager.eval(base, stack, I_HARVEST, WeaponItems.gun("gun_drill"), 0); }

    /** Original {@code EntityDamageUtil.getMouseOver(player, reach)} (im Port nicht vorhanden): Block- und Entitaetsstrahl aus Augenhoehe. */
    @Nullable
    public static MovingObjectPosition getMouseOver(Player attacker, double reach) {
        return getMouseOver(attacker, reach, 0D);
    }

    @Nullable
    /** Delegiert an das Original {@code EntityDamageUtil.getMouseOver}. */
    public static MovingObjectPosition getMouseOver(Player attacker, double reach, double threshold) {
        return com.hbm_m.util.EntityDamageUtil.getMouseOver(attacker, reach, threshold);
    }

    @SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_DRILL_ANIMS = (stack, type) -> {
        switch (type) {
            case EQUIP: return new BusAnimation()
                    .addBus("EQUIP", new BusAnimationSequence().setPos(-1, 0, 0).addPos(0, 0, 0, 750, IType.SIN_DOWN));
            case CYCLE:
                double deploy = com.hbm_m.render.anim.HbmAnimations.getRelevantTransformation("DEPLOY")[0];
                double speed = com.hbm_m.render.anim.HbmAnimations.getRelevantTransformation("SPEED")[0];
                double spin = com.hbm_m.render.anim.HbmAnimations.getRelevantTransformation("SPIN")[0] % 360; // seamlessly continue from the previous animation state
                return new BusAnimation()
                        .addBus("DEPLOY", new BusAnimationSequence().setPos(deploy, 0, 0).addPos(1, 0, 0, (int) (500 * (1 - deploy)), IType.SIN_FULL).hold(1000).addPos(0, 0, 0, 500, IType.SIN_FULL))
                        .addBus("SPIN", new BusAnimationSequence().setPos(spin, 0, 0).addPos(spin + 360 * 1.5, 0, 0, 1500).addPos(360 * 3, 0, 0, 750 + (int) (1000 * (1D - spin / 360D)), IType.SIN_DOWN))
                        .addBus("SPEED", new BusAnimationSequence().setPos(speed, 0, 0).addPos(1, 0, 0, 500).hold(1000).addPos(0, 0, 0, 750 + (int) (1000 * (1D - spin / 360D)), IType.SIN_DOWN));
            case CYCLE_DRY: return new BusAnimation()
                    .addBus("DEPLOY", new BusAnimationSequence().addPos(0.25, 0, 0, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("SPIN", new BusAnimationSequence().addPos(360 * 1, 0, 0, 1500, IType.SIN_DOWN))
                    .addBus("SPEED", new BusAnimationSequence().addPos(0.75, 0, 0, 250).addPos(0, 0, 0, 1000, IType.SIN_DOWN));
            case INSPECT: return new BusAnimation()
                    .addBus("LIFT", new BusAnimationSequence().addPos(-45, 0, 0, 500, IType.SIN_FULL).hold(1000).addPos(0, 0, 0, 500, IType.SIN_DOWN));
        }

        return null;
    };

    /**
     * Called by the ModEventHandlerRenderer if the held item is a drill, cancels the tooltip so we an replace it with this.
     * Should probably make an interface for stuff like this.
     * Port: aus {@code RenderHighlightEvent.Block} aufrufen (Ereignis danach abbrechen); {@code poseStack} ist ein
     * {@code PoseStack}, {@code bufferSource} eine {@code MultiBufferSource} - als Object, damit diese gemeinsame Klasse
     * keine Client-Typen in Signaturen fuehrt. Nur clientseitig aufrufen.
     */
    public static void drawBlockHighlight(Player player, ItemStack drill, float interp, Object poseStack, Object bufferSource) {
        ClientHighlight.draw(player, drill, interp, poseStack, bufferSource);
    }

    /** Client-Haelfte von {@link #drawBlockHighlight} (eigene Klasse, damit der Server sie nie laedt). */
    private static final class ClientHighlight {

        static void draw(Player player, ItemStack drill, float interp, Object poseStackObj, Object bufferSourceObj) {
            MovingObjectPosition mop = getMouseOver(player, getModdableReach(drill, 5.0D));

            if (mop != null && mop.typeOfHit == MovingObjectType.BLOCK) {
                // 1.20: Renderursprung ist die Kamera statt der interpolierten Spielerposition
                Vec3 cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
                double dX = cam.x;
                double dY = cam.y;
                double dZ = cam.z;

                com.mojang.blaze3d.vertex.PoseStack poseStack = (com.mojang.blaze3d.vertex.PoseStack) poseStackObj;
                net.minecraft.client.renderer.MultiBufferSource buffers = (net.minecraft.client.renderer.MultiBufferSource) bufferSourceObj;
                com.mojang.blaze3d.vertex.VertexConsumer lines = buffers.getBuffer(net.minecraft.client.renderer.RenderType.lines());

                int aoe = player.isShiftKeyDown() ? 0 : getModdableAoE(drill, 1);

                float exp = 0.002F;
                AABB aabb = new AABB(0, 0, 0, 1, 1, 1);
                // Original: Farbe -1 = Standard aus ICustomBlockHighlight.setup (schwarz, Alpha 0.4), sonst 0x800000
                if (aoe > 0) net.minecraft.client.renderer.LevelRenderer.renderLineBox(poseStack, lines, aabb.inflate(exp).move(mop.blockX - dX, mop.blockY - dY, mop.blockZ - dZ), 0F, 0F, 0F, 0.4F);
                else net.minecraft.client.renderer.LevelRenderer.renderLineBox(poseStack, lines, aabb.inflate(exp).move(mop.blockX - dX, mop.blockY - dY, mop.blockZ - dZ), 0.5F, 0F, 0F, 1F);

                if (aoe > 0) {
                    aabb = new AABB(-aoe, -aoe, -aoe, 1 + aoe, 1 + aoe, 1 + aoe);
                    net.minecraft.client.renderer.LevelRenderer.renderLineBox(poseStack, lines, aabb.inflate(exp).move(mop.blockX - dX, mop.blockY - dY, mop.blockZ - dZ), 0.5F, 0F, 0F, 1F);
                }
            }
        }
    }
}
