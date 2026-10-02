package com.hbm_m.item.special;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.effect.BlackHoleEntity;
import com.hbm_m.entity.effect.EntityCloudFleija;
import com.hbm_m.entity.effect.RagingVortexEntity;
import com.hbm_m.entity.effect.VortexEntity;
import com.hbm_m.entity.logic.EntityNukeExplosionMK3;
import com.hbm_m.explosion.ExplosionChaos;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.ModItems;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 1:1 {@code com.hbm.items.special.ItemDrop}: Gegenstaende, die fallengelassen etwas tun - beta
 * verschwindet, Totmann-Zuender zuenden, Antimaterie/Singularitaeten/Xen-Kristall wirken beim Aufprall.
 * Jeder am Boden liegende ItemDrop-Stapel verschwindet danach.
 */
public class ItemDrop extends Item implements ITooltipProvider {

    /** Original {@code setContainerItem}; lazy, weil cell_empty/nuclear_waste spaeter registriert werden. */
    @Nullable
    private final java.util.function.Supplier<Item> container;

    public ItemDrop(Properties properties) {
        this(properties, null);
    }

    public ItemDrop(Properties properties, @Nullable java.util.function.Supplier<Item> container) {
        super(properties);
        this.container = container;
    }

    //? if forge {
    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return container != null;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return container != null ? new ItemStack(container.get()) : ItemStack.EMPTY;
    }
    //?}

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entityItem) {
        Level world = entityItem.level();

        if (this == ModItems.BETA.get()) {
            entityItem.discard();
            return true;
        }

        ModClothConfig cfg = ModClothConfig.get();

        if (stack.is(ModItems.DETONATOR_DEADMAN.get())) {
            if (!world.isClientSide) {
                CompoundTag tag = stack.getTag();
                if (tag != null) {
                    BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
                    Block b = world.getBlockState(pos).getBlock();
                    if (b instanceof IBomb bomb) {
                        bomb.explode(world, pos);
                        MainRegistry.LOGGER.info("[DET] Tried to detonate block at {} / {} / {} by dead man's switch!", pos.getX(), pos.getY(), pos.getZ());
                    }
                }

                world.explode(entityItem, entityItem.getX(), entityItem.getY(), entityItem.getZ(), 0.0F, true, Level.ExplosionInteraction.TNT);
                entityItem.discard();
            }
        }

        if (stack.is(ModItems.DETONATOR_DE.get())) {
            if (!world.isClientSide && cfg.dropDead) {
                world.explode(entityItem, entityItem.getX(), entityItem.getY(), entityItem.getZ(), 15.0F, true, Level.ExplosionInteraction.TNT);
                MainRegistry.LOGGER.info("[DET] Detonated dead man's explosive at {} / {} / {}!", (int) entityItem.getX(), (int) entityItem.getY(), (int) entityItem.getZ());
            }
            entityItem.discard();
        }

        if (entityItem.onGround()) {

            if (!world.isClientSide) {
                double x = entityItem.getX();
                double y = entityItem.getY();
                double z = entityItem.getZ();

                if (stack.is(ModItems.CELL_ANTIMATTER.get()) && cfg.dropCell) {
                    new ExplosionVNT(world, x, y, z, 3F).makeAmat().explode();
                }
                if (stack.is(ModItems.PELLET_ANTIMATTER.get()) && cfg.dropCell) {
                    new ExplosionVNT(world, x, y, z, 20F).makeAmat().explode();
                }
                if (stack.is(ModItems.CELL_ANTI_SCHRABIDIUM.get()) && cfg.dropCell) {
                    EntityNukeExplosionMK3 ex = EntityNukeExplosionMK3.statFacFleija(world, x, y, z, cfg.aSchrabRadius);
                    if (!ex.isRemoved()) {
                        world.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 100.0F, world.random.nextFloat() * 0.1F + 0.9F);
                        world.addFreshEntity(ex);

                        EntityCloudFleija cloud = new EntityCloudFleija(ModEntities.CLOUD_FLEIJA.get(), world, cfg.aSchrabRadius);
                        cloud.setPos(x, y, z);
                        world.addFreshEntity(cloud);
                    }
                }
                if (stack.is(ModItems.SINGULARITY.get()) && cfg.dropSingularity) {
                    vortex(world, x, y, z, 1.5F);
                }
                if (stack.is(ModItems.SINGULARITY_COUNTER_RESONANT.get()) && cfg.dropSingularity) {
                    vortex(world, x, y, z, 2.5F);
                }
                if (stack.is(ModItems.SINGULARITY_SUPER_HEATED.get()) && cfg.dropSingularity) {
                    vortex(world, x, y, z, 2.5F);
                }
                if (stack.is(ModItems.BLACK_HOLE.get()) && cfg.dropSingularity) {
                    BlackHoleEntity bl = new BlackHoleEntity(ModEntities.BLACK_HOLE.get(), world);
                    bl.setSize(1.5F);
                    bl.setPos(x, y, z);
                    world.addFreshEntity(bl);
                }
                if (stack.is(ModItems.SINGULARITY_SPARK.get()) && cfg.dropSingularity) {
                    RagingVortexEntity bl = new RagingVortexEntity(ModEntities.RAGING_VORTEX.get(), world);
                    bl.setSize(3.5F);
                    bl.setPos(x, y, z);
                    world.addFreshEntity(bl);
                }
                if (stack.is(com.hbm_m.item.material.ModMaterialItems.item(com.hbm_m.item.material.ModMaterials.XEN, com.hbm_m.item.material.MaterialShape.CRYSTAL)) && cfg.dropCrys) {
                    ExplosionChaos.floater(world, (int) x, (int) y, (int) z, 25, 75);
                    ExplosionChaos.move(world, (int) x, (int) y, (int) z, 25, 0, 75, 0);
                }
            }

            entityItem.discard();
            return true;
        }
        return false;
    }

    private static void vortex(Level world, double x, double y, double z, float size) {
        VortexEntity bl = new VortexEntity(ModEntities.VORTEX.get(), world);
        bl.setSize(size);
        bl.setPos(x, y, z);
        world.addFreshEntity(bl);
    }

    @Override
    public void appendHbmTooltip(ItemStack itemstack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        if (this == ModItems.CELL_ANTIMATTER.get()) {
            list.add(Component.literal("Warning: Exposure to matter will"));
            list.add(Component.literal("lead to violent annihilation!"));
        }
        if (this == ModItems.PELLET_ANTIMATTER.get()) {
            list.add(Component.literal("Very heavy antimatter cluster."));
            list.add(Component.literal("Gets rid of black holes."));
        }
        if (this == ModItems.CELL_ANTI_SCHRABIDIUM.get()) {
            list.add(Component.literal("Warning: Exposure to matter will"));
            list.add(Component.literal("create a fólkvangr field!"));
        }
        if (this == ModItems.SINGULARITY.get()) {
            list.add(Component.literal("You may be asking:"));
            list.add(Component.literal("\"But HBM, a manifold with an undefined"));
            list.add(Component.literal("state of spacetime? How is this possible?\""));
            list.add(Component.literal("Long answer short:"));
            list.add(Component.literal("\"I have no idea!\""));
        }
        if (this == ModItems.SINGULARITY_COUNTER_RESONANT.get()) {
            list.add(Component.literal("Nullifies resonance of objects in"));
            list.add(Component.literal("non-euclidean space, creates variable"));
            list.add(Component.literal("gravity well. Spontaneously spawns"));
            list.add(Component.literal("tesseracts. If a tesseract happens to"));
            list.add(Component.literal("appear near you, do not look directly"));
            list.add(Component.literal("at it."));
        }
        if (this == ModItems.SINGULARITY_SUPER_HEATED.get()) {
            list.add(Component.literal("Continuously heats up matter by"));
            list.add(Component.literal("resonating every planck second."));
            list.add(Component.literal("Tends to catch fire or to create"));
            list.add(Component.literal("small plasma arcs. Not edible."));
        }
        if (this == ModItems.BLACK_HOLE.get()) {
            list.add(Component.literal("Contains a regular singularity"));
            list.add(Component.literal("in the center. Large enough to"));
            list.add(Component.literal("stay stable. It's not the end"));
            list.add(Component.literal("of the world as we know it,"));
            list.add(Component.literal("and I don't feel fine."));
        }
        if (this == ModItems.DETONATOR_DEADMAN.get()) {
            list.add(Component.literal("Shift right-click to set position,"));
            list.add(Component.literal("drop to detonate!"));
            CompoundTag tag = itemstack.getTag();
            if (tag == null) {
                list.add(Component.literal("No position set!"));
            } else {
                list.add(Component.literal("Set pos to " + tag.getInt("x") + ", " + tag.getInt("y") + ", " + tag.getInt("z")));
            }
        }
        if (this == ModItems.DETONATOR_DE.get()) {
            list.add(Component.literal("Explodes when dropped!"));
        }

        list.add(Component.literal("[").append(Component.translatable("trait.drop")).append("]").withStyle(ChatFormatting.RED));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (this != ModItems.DETONATOR_DEADMAN.get()) {
            return super.useOn(ctx);
        }

        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        Level world = ctx.getLevel();
        CompoundTag tag = stack.getOrCreateTag();

        if (player != null && player.isShiftKeyDown()) {
            BlockPos pos = ctx.getClickedPos();
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());

            if (world.isClientSide) {
                player.sendSystemMessage(Component.literal("Position set!"));
            }

            world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:item.techBoop"), SoundSource.PLAYERS, 2.0F, 1.0F);

            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        return InteractionResult.PASS;
    }
}
