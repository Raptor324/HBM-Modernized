package com.hbm_m.item.weapon.sedna.factory;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.hbm_m.api.tile.IRepairable;
import com.hbm_m.api.tile.IRepairable.EnumExtinguishType;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.entity.projectile.EntityBulletBaseMK4;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorBulkie;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm_m.explosion.vanillant.standard.BlockMutatorDebris;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm_m.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.weapon.sedna.BulletConfig;
import com.hbm_m.item.weapon.sedna.Crosshair;
import com.hbm_m.item.weapon.sedna.GunConfig;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm_m.item.weapon.sedna.Receiver;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm_m.item.weapon.sedna.impl.ItemGunChargeThrower;
import com.hbm_m.item.weapon.sedna.mags.MagazineFullReload;
import com.hbm_m.particle.helper.ExplosionCreator;
import com.hbm_m.render.anim.AnimationEnums.GunAnimation;
import com.hbm_m.render.anim.BusAnimation;
import com.hbm_m.render.anim.BusAnimationKeyframe.IType;
import com.hbm_m.render.anim.BusAnimationSequence;
import com.hbm_m.util.CompatExternal;
import com.hbm_m.util.ForgeDirection;
import com.hbm_m.util.MovingObjectPosition;
import com.hbm_m.util.MovingObjectPosition.MovingObjectType;
import com.hbm_m.util.Vec3NT;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code XFactoryTool}: Feuerloescher (Wasser/Schaum/Bor-Sand) und Ladungswerfer (Haken, Moerser).
 * Portabweichung: {@code ammo_fireext} meta 0/1/2 sind im Port {@code ammo_fireext}, {@code ammo_fireext_foam},
 * {@code ammo_fireext_sand}; Schicht-Metadaten (foam_layer/sand_boron_layer) = {@link SnowLayerBlock#LAYERS} - 1.
 */
public class XFactoryTool {

    public static final ResourceLocation scope = ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/misc/scope_tool.png");

    public static BulletConfig fext_water;
    public static BulletConfig fext_foam;
    public static BulletConfig fext_sand;

    public static BulletConfig ct_hook;
    public static BulletConfig ct_mortar;
    public static BulletConfig ct_mortar_charge;

    private static void hiss(EntityBulletBaseMK4 bullet) {
        XFactoryEnergy.playSound(bullet.level(), bullet.getX(), bullet.getY(), bullet.getZ(), SoundEvents.FIRE_EXTINGUISH, 1.0F, 1.5F + bullet.level().random.nextFloat() * 0.5F);
    }

    /** Original {@code block.getMaterial() == Material.fire}. */
    private static boolean isFire(BlockState state) {
        return state.is(BlockTags.FIRE);
    }

    /** Original: Schicht-Block um eine Stufe erhoehen ({@code meta + 1}), ab meta 6 in den Vollblock umwandeln. */
    private static void growLayer(Level world, BlockPos pos, BlockState current, Block full) {
        int meta = current.getValue(SnowLayerBlock.LAYERS) - 1;
        if (meta < 6) world.setBlock(pos, current.setValue(SnowLayerBlock.LAYERS, meta + 1 + 1), 3);
        else world.setBlockAndUpdate(pos, full.defaultBlockState());
    }

    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_WATER_HIT = (bullet, mop) -> {
        if (!bullet.level().isClientSide) {
            int ix = mop.blockX;
            int iy = mop.blockY;
            int iz = mop.blockZ;
            boolean fizz = false;
            for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
                BlockPos p = new BlockPos(ix + i, iy + j, iz + k);
                Block block = bullet.level().getBlockState(p).getBlock();
                if (block == Blocks.FIRE || block == ModBlocks.FOAM_LAYER.get() || block == ModBlocks.BLOCK_FOAM.get()) {
                    bullet.level().setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                    fizz = true;
                }
            }
            BlockEntity core = CompatExternal.getCoreFromPos(bullet.level(), new BlockPos(ix, iy, iz));
            if (core instanceof IRepairable rep) rep.tryExtinguish(bullet.level(), new BlockPos(ix, iy, iz), EnumExtinguishType.WATER);
            if (fizz) hiss(bullet);
            bullet.setDead();
        }
    };

    public static Consumer<Entity> LAMBDA_WATER_UPDATE = (bullet) -> {
        if (bullet.level().isClientSide) {
            Vec3 m = bullet.getDeltaMovement();
            CompoundTag data = new CompoundTag();
            data.putString("type", "vanillaExt");
            data.putString("mode", "blockdust");
            data.putInt("block", Block.getId(Blocks.WATER.defaultBlockState()));
            data.putDouble("posX", bullet.getX()); data.putDouble("posY", bullet.getY()); data.putDouble("posZ", bullet.getZ());
            data.putDouble("mX", m.x + bullet.level().random.nextGaussian() * 0.05);
            data.putDouble("mY", m.y - 0.2 + bullet.level().random.nextGaussian() * 0.05);
            data.putDouble("mZ", m.z + bullet.level().random.nextGaussian() * 0.05);
            com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
        } else {
            int x = (int) Math.floor(bullet.getX());
            int y = (int) Math.floor(bullet.getY());
            int z = (int) Math.floor(bullet.getZ());
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = bullet.level().getBlockState(pos);
            if (state.getBlock() == ModBlocks.VOLCANIC_LAVA_BLOCK.get() && state.getFluidState().isSource()) {
                bullet.level().setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
                ((EntityBulletBaseMK4) bullet).setDead();
            }
        }
    };

    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_FOAM_HIT = (bullet, mop) -> {
        if (!bullet.level().isClientSide) {
            Level world = bullet.level();
            int ix = mop.blockX;
            int iy = mop.blockY;
            int iz = mop.blockZ;
            boolean fizz = false;
            for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
                BlockPos p = new BlockPos(ix + i, iy + j, iz + k);
                if (isFire(world.getBlockState(p))) {
                    world.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                    fizz = true;
                }
            }
            BlockEntity core = CompatExternal.getCoreFromPos(world, new BlockPos(ix, iy, iz));
            if (core instanceof IRepairable rep) { rep.tryExtinguish(world, new BlockPos(ix, iy, iz), EnumExtinguishType.FOAM); return; }
            if (world.random.nextBoolean()) {
                ForgeDirection dir = ForgeDirection.getOrientation(mop.sideHit);
                ix += dir.offsetX; iy += dir.offsetY; iz += dir.offsetZ;
            }
            BlockPos pos = new BlockPos(ix, iy, iz);
            BlockState b = world.getBlockState(pos);
            Block foam = ModBlocks.FOAM_LAYER.get();
            // Original isReplaceable: Material des Schaums war ersetzbar, im Port explizit
            if ((b.canBeReplaced() || b.getBlock() == foam) && foam.defaultBlockState().canSurvive(world, pos)) {
                if (b.getBlock() != foam) {
                    world.setBlockAndUpdate(pos, foam.defaultBlockState());
                } else {
                    growLayer(world, pos, b, ModBlocks.BLOCK_FOAM.get());
                }
            }
            if (fizz) hiss(bullet);
        }
    };

    public static Consumer<Entity> LAMBDA_FOAM_UPDATE = (bullet) -> {
        if (bullet.level().isClientSide) {
            Vec3 m = bullet.getDeltaMovement();
            CompoundTag data = new CompoundTag();
            data.putString("type", "vanillaExt");
            data.putString("mode", "blockdust");
            data.putInt("block", Block.getId(ModBlocks.BLOCK_FOAM.get().defaultBlockState()));
            data.putDouble("posX", bullet.getX()); data.putDouble("posY", bullet.getY()); data.putDouble("posZ", bullet.getZ());
            data.putDouble("mX", m.x + bullet.level().random.nextGaussian() * 0.1);
            data.putDouble("mY", m.y - 0.2 + bullet.level().random.nextGaussian() * 0.1);
            data.putDouble("mZ", m.z + bullet.level().random.nextGaussian() * 0.1);
            com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
        }
    };

    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_SAND_HIT = (bullet, mop) -> {
        if (!bullet.level().isClientSide) {
            Level world = bullet.level();
            int ix = mop.blockX;
            int iy = mop.blockY;
            int iz = mop.blockZ;
            BlockEntity core = CompatExternal.getCoreFromPos(world, new BlockPos(ix, iy, iz));
            if (core instanceof IRepairable rep) { rep.tryExtinguish(world, new BlockPos(ix, iy, iz), EnumExtinguishType.SAND); return; }
            if (world.random.nextBoolean()) {
                ForgeDirection dir = ForgeDirection.getOrientation(mop.sideHit);
                ix += dir.offsetX; iy += dir.offsetY; iz += dir.offsetZ;
            }
            BlockPos pos = new BlockPos(ix, iy, iz);
            BlockState b = world.getBlockState(pos);
            Block layer = ModBlocks.SAND_BORON_LAYER.get();
            if ((b.canBeReplaced() || b.getBlock() == layer) && layer.defaultBlockState().canSurvive(world, pos)) {
                if (b.getBlock() != layer) {
                    world.setBlockAndUpdate(pos, layer.defaultBlockState());
                } else {
                    // Original: sand_mix meta BORON -> im Port eigener Block sand_boron
                    growLayer(world, pos, b, ModBlocks.SAND_BORON.get());
                }
                if (isFire(b)) hiss(bullet);
            }
        }
    };

    public static Consumer<Entity> LAMBDA_SAND_UPDATE = (bullet) -> {
        if (bullet.level().isClientSide) {
            Vec3 m = bullet.getDeltaMovement();
            CompoundTag data = new CompoundTag();
            data.putString("type", "vanillaExt");
            data.putString("mode", "blockdust");
            data.putInt("block", Block.getId(ModBlocks.SAND_BORON.get().defaultBlockState()));
            data.putInt("meta", 0); // Original: EnumSandType.BORON.ordinal() - im Port eigener Block
            data.putDouble("posX", bullet.getX()); data.putDouble("posY", bullet.getY()); data.putDouble("posZ", bullet.getZ());
            data.putDouble("mX", m.x + bullet.level().random.nextGaussian() * 0.1);
            data.putDouble("mY", m.y - 0.2 + bullet.level().random.nextGaussian() * 0.1);
            data.putDouble("mZ", m.z + bullet.level().random.nextGaussian() * 0.1);
            com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
        }
    };

    public static Consumer<Entity> LAMBDA_SET_HOOK = (entity) -> {
        EntityBulletBaseMK4 bullet = (EntityBulletBaseMK4) entity;
        if (!bullet.level().isClientSide && bullet.tickCount < 2 && bullet.getThrower() instanceof Player player) {
            ItemStack held = player.getMainHandItem();
            if (!held.isEmpty() && held.getItem() == WeaponItems.gun("gun_charge_thrower")) {
                ItemGunChargeThrower.setLastHook(held, bullet.getId());
            }
        }
        bullet.noCulling = true;
    };

    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_HOOK = (bullet, mop) -> {
        if (mop.typeOfHit == MovingObjectType.BLOCK) {
            Vec3 m = bullet.getDeltaMovement();
            Vec3NT vec = new Vec3NT(-m.x, -m.y, -m.z).normalize();
            vec.xCoord *= 0.05; vec.yCoord *= 0.05; vec.zCoord *= 0.05; // Original: .multiply(0.05)
            bullet.setPosition(mop.hitVec.xCoord + vec.xCoord, mop.hitVec.yCoord + vec.yCoord, mop.hitVec.zCoord + vec.zCoord);
            bullet.getStuck(new BlockPos(mop.blockX, mop.blockY, mop.blockZ), mop.sideHit);
        }
    };

    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_MORTAR = (bullet, mop) -> {
        if (mop.typeOfHit == MovingObjectType.ENTITY && bullet.tickCount < 3 && mop.entityHit == bullet.getThrower()) return;
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, 5, bullet.getThrower());
        vnt.setBlockAllocator(new BlockAllocatorBulkie(60, 8));
        vnt.setBlockProcessor(new BlockProcessorStandard());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage).setupPiercing(bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
        bullet.setDead();
    };

    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_MORTAR_CHARGE = (bullet, mop) -> {
        if (mop.typeOfHit == MovingObjectType.ENTITY && bullet.tickCount < 3 && mop.entityHit == bullet.getThrower()) return;
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, 15, bullet.getThrower());
        vnt.setBlockAllocator(new BlockAllocatorStandard());
        // Original: BlockMutatorDebris(block_slag, 1) - Metadaten gibt es im Port nicht
        vnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop().withBlockEffect(new BlockMutatorDebris(ModBlocks.BLOCK_SLAG.get())));
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage).setupPiercing(bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        if (bullet.level() instanceof ServerLevel sl) ExplosionCreator.composeEffectSmall(sl, mop.hitVec.xCoord, mop.hitVec.yCoord + 0.5, mop.hitVec.zCoord);
        vnt.explode();
        bullet.setDead();
    };

    public static void init() {

        fext_water = new BulletConfig().setItem(ModItems.AMMO_FIREEXT).setReloadCount(300).setLife(100).setVel(0.75F).setGrav(0.04D).setSpread(0.025F)
                .setOnUpdate(LAMBDA_WATER_UPDATE)
                .setOnEntityHit((bulletEntity, target) -> { if (target.entityHit != null) target.entityHit.clearFire(); })
                .setOnRicochet(LAMBDA_WATER_HIT);
        fext_foam = new BulletConfig().setItem(ModItems.AMMO_FIREEXT_FOAM).setReloadCount(300).setLife(100).setVel(0.75F).setGrav(0.04D).setSpread(0.05F)
                .setOnUpdate(LAMBDA_FOAM_UPDATE)
                .setOnEntityHit((bulletEntity, target) -> { if (target.entityHit != null) target.entityHit.clearFire(); })
                .setOnRicochet(LAMBDA_FOAM_HIT);
        fext_sand = new BulletConfig().setItem(ModItems.AMMO_FIREEXT_SAND).setReloadCount(300).setLife(100).setVel(0.75F).setGrav(0.04D).setSpread(0.05F)
                .setOnUpdate(LAMBDA_SAND_UPDATE)
                .setOnEntityHit((bulletEntity, target) -> { if (target.entityHit != null) target.entityHit.clearFire(); })
                .setOnRicochet(LAMBDA_SAND_HIT);

        ct_hook = new BulletConfig().setItem(EnumAmmo.CT_HOOK).setRenderRotations(false).setLife(6_000).setVel(3F).setGrav(0.035D).setDoesPenetrate(true).setDamageFalloffByPen(false)
                .setOnUpdate(LAMBDA_SET_HOOK).setOnImpact(LAMBDA_HOOK);
        ct_mortar = new BulletConfig().setItem(EnumAmmo.CT_MORTAR).setDamage(2.5F).setLife(200).setVel(3F).setGrav(0.035D)
                .setOnImpact(LAMBDA_MORTAR);
        ct_mortar_charge = new BulletConfig().setItem(EnumAmmo.CT_MORTAR_CHARGE).setDamage(5F).setLife(200).setVel(3F).setGrav(0.035D)
                .setOnImpact(LAMBDA_MORTAR_CHARGE);

        GunFactory.reg("gun_fireext", new ItemGunBaseNT(WeaponQuality.UTILITY, new GunConfig()
                .dura(5_000).draw(10).inspect(55).reloadChangeType(true).hideCrosshair(false).crosshair(Crosshair.L_CIRCLE)
                .rec(new Receiver(0)
                        .dmg(0F).delay(1).dry(0).auto(true).spread(0F).spreadHipfire(0F).reload(20).jam(0).sound("hbm:weapon.extinguisher", 1.0F, 1.0F)
                        .mag(new MagazineFullReload(0, 300).addConfigs(fext_water, fext_foam, fext_sand))
                        .offset(1, -0.0625 * 2.5, -0.25D)
                        .setupStandardFire())
                .setupStandardConfiguration()
                .orchestra(Orchestras.ORCHESTRA_FIREEXT)
                ));

        GunFactory.reg("gun_charge_thrower", new ItemGunChargeThrower(WeaponQuality.UTILITY, new GunConfig()
                .dura(3_000).draw(10).inspect(55).reloadChangeType(true).hideCrosshair(false).crosshair(Crosshair.L_CIRCUMFLEX)
                .rec(new Receiver(0)
                        .dmg(10F).delay(4).dry(10).auto(true).spread(0F).spreadHipfire(0F).reload(60).jam(0).sound("hbm:weapon.fire.grenade", 1.0F, 1.0F)
                        .mag(new MagazineFullReload(0, 1).addConfigs(ct_hook, ct_mortar, ct_mortar_charge))
                        .offset(1, -0.0625 * 2.5, -0.25D)
                        .setupStandardFire().recoil(LAMBDA_RECOIL_CT))
                .setupStandardConfiguration()
                .anim(LAMBDA_CT_ANIMS).orchestra(Orchestras.ORCHESTRA_CHARGE_THROWER)
                ).setDefaultAmmo(EnumAmmo.CT_MORTAR, 3));
    }

    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_CT = (stack, ctx) -> {
        ItemGunBaseNT.setupRecoil(10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
    };

    @SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_CT_ANIMS = (stack, type) -> {
        switch (type) {
            case EQUIP: return new BusAnimation()
                    .addBus("EQUIP", new BusAnimationSequence().addPos(-45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case CYCLE: return new BusAnimation()
                    .addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, -1, 100, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL));
            case RELOAD: return new BusAnimation()
                    .addBus("RAISE", new BusAnimationSequence().addPos(-45, 0, 0, 500, IType.SIN_FULL).hold(2000).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("AMMO", new BusAnimationSequence().setPos(0, -10, -5).hold(500).addPos(0, 0, 5, 750, IType.SIN_FULL).addPos(0, 0, 0, 500, IType.SIN_UP).hold(4000))
                    .addBus("TWIST", new BusAnimationSequence().setPos(0, 0, 25).hold(2000).addPos(0, 0, 0, 150));
            case INSPECT: return new BusAnimation()
                    .addBus("TURN", new BusAnimationSequence().addPos(0, 60, 0, 500, IType.SIN_FULL).hold(1750).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("ROLL", new BusAnimationSequence().hold(750).addPos(0, 0, -90, 500, IType.SIN_FULL).hold(1000).addPos(0, 0, 0, 500, IType.SIN_FULL));
        }

        return null;
    };
}
