package com.hbm_m.block.bomb;

import javax.annotation.Nullable;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.bomb.CrashedBombBlockEntity;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.logic.EntityBalefireExplosion;
import com.hbm_m.entity.logic.EntityNukeExplosionMK5;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.main.Polaroid;
import com.hbm_m.particle.helper.ExplosionCreator;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code BlockCrashedBomb} ({@code crashed_bomb}, Untertypen als Bloecke {@code dud_balefire/conventional/nuke/
 * salted}): Blindgaenger, unzerstoerbar. Der Defuser zerlegt ihn in seine Bestandteile, ein Zuender bringt ihn
 * hoch. Das Blockentity strahlt (Balefire/Atom/gesalzen).
 */
public class CrashedBombBlock extends BaseEntityBlock implements IBomb {

    public enum DudType { BALEFIRE, CONVENTIONAL, NUKE, SALTED }

    public final DudType type;

    public CrashedBombBlock(Properties properties, DudType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrashedBombBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> t) {
        return level.isClientSide ? null : createTickerHelper(t, ModBlockEntities.CRASHED_BOMB.get(), CrashedBombBlockEntity::serverTick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return defuse(world, pos, player, hand);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return defuse(world, pos, player, InteractionHand.MAIN_HAND);
    }
    *///?}

    private InteractionResult defuse(Level world, BlockPos pos, Player player, InteractionHand hand) {
        if (world.isClientSide) return InteractionResult.SUCCESS;
        if (!player.getItemInHand(hand).is(ModItems.DEFUSER.get())) return InteractionResult.PASS;

        switch (type) {
            case BALEFIRE -> dropItems(world, pos, new ItemStack(ModItems.EGG_BALEFIRE_SHARD.get()));
            case CONVENTIONAL -> dropItems(world, pos, new ItemStack(ModItems.BALL_TNT.get(), 16));
            case NUKE -> dropItems(world, pos, new ItemStack(ModItems.BALL_TNT.get(), 8),
                    new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.BILLET), 4));
            case SALTED -> dropItems(world, pos, new ItemStack(ModItems.BALL_TNT.get(), 8),
                    new ItemStack(ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.BILLET), 2),
                    new ItemStack(ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.INGOT), 12));
        }
        world.destroyBlock(pos, false);
        return InteractionResult.SUCCESS;
    }

    public static void dropItems(Level world, BlockPos pos, ItemStack... drops) {
        for (ItemStack drop : drops) world.addFreshEntity(new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop));
    }

    @Override
    public BombReturnCode explode(Level world, BlockPos pos) {
        if (world instanceof ServerLevel server) {
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            world.removeBlock(pos, false);

            switch (type) {
                case BALEFIRE -> {
                    EntityBalefireExplosion bf = EntityBalefireExplosion.statFac(world, x, y, z, (int) (ModClothConfig.get().fatmanRadius * 1.25));
                    bf.speed = 1;
                    world.addFreshEntity(bf);
                    spawnMush(server, x, y, z, true);
                }
                case CONVENTIONAL -> {
                    ExplosionVNT xnt = new ExplosionVNT(world, x + 0.5, y + 0.5, z + 0.5, 35F);
                    xnt.setBlockAllocator(new BlockAllocatorStandard(24));
                    xnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop());
                    xnt.setEntityProcessor(new EntityProcessorCross(5D).withRangeMod(1.5F));
                    xnt.setPlayerProcessor(new PlayerProcessorStandard());
                    xnt.explode();
                    ExplosionCreator.composeEffectLarge(server, x + 0.5, y + 0.5, z + 0.5);
                }
                case NUKE -> {
                    EntityNukeExplosionMK5.start(world, 35, x + 0.5, y + 0.5, z + 0.5);
                    spawnMush(server, x, y, z, Polaroid.id() == 11 || world.random.nextInt(100) == 0);
                }
                case SALTED -> {
                    EntityNukeExplosionMK5.start(world, 25, x + 0.5, y + 0.5, z + 0.5).setFalloutAdd(25);
                    spawnMush(server, x, y, z, Polaroid.id() == 11 || world.random.nextInt(100) == 0);
                }
            }
        }
        return BombReturnCode.DETONATED;
    }

    public static void spawnMush(ServerLevel world, int x, int y, int z, boolean balefire) {
        world.playSound(null, x + 0.5, y + 0.5, z + 0.5, HbmSoundsNT.get("weapon.mukeExplosion"), SoundSource.BLOCKS, 15.0F, 1.0F);
        CompoundTag data = new CompoundTag();
        data.putString("type", "muke");
        data.putBoolean("balefire", balefire);
        IParticleCreator.sendPacket(world, x + 0.5, y + 0.5, z + 0.5, 250, data);
    }
    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<CrashedBombBlock> CODEC = com.hbm_m.platform.BlockCodecs.unsupported(CrashedBombBlock.class);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
