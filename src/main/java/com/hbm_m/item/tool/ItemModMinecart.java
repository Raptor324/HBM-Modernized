package com.hbm_m.item.tool;

import com.hbm_m.platform.StackNbt;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.cart.EntityMinecartCrate;
import com.hbm_m.entity.cart.EntityMinecartDestroyer;
import com.hbm_m.entity.cart.EntityMinecartOre;
import com.hbm_m.entity.cart.EntityMinecartPowder;
import com.hbm_m.entity.cart.EntityMinecartSemtex;

import net.minecraft.core.BlockPos;
//? if < 1.21.1 {
import net.minecraft.core.BlockSource;
//?} else {
/*import net.minecraft.core.dispenser.BlockSource;
*///?}
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code ItemModMinecart}: NTM-Loren. Das Original kodiert die Art als Meta und die Basis (Vanilla, Holz, Stahl,
 * lackiert) als NBT; im Port ist jede gueltige Kombination ein eigener Gegenstand ({@code cart_<art>_<basis>}). Setzt
 * die Lore auf Schienen, Werfer stellen sie ebenfalls auf.
 */
public class ItemModMinecart extends Item {

    public enum EnumCartBase {
        VANILLA,
        WOOD,
        STEEL,
        PAINTED
    }

    public enum EnumMinecart {
        EMPTY(EnumCartBase.WOOD, EnumCartBase.STEEL, EnumCartBase.PAINTED),
        CRATE(EnumCartBase.VANILLA),
        DESTROYER(EnumCartBase.STEEL, EnumCartBase.PAINTED),
        POWDER(EnumCartBase.WOOD, EnumCartBase.STEEL, EnumCartBase.PAINTED),
        SEMTEX(EnumCartBase.WOOD, EnumCartBase.STEEL, EnumCartBase.PAINTED);

        public int types;

        EnumMinecart(EnumCartBase... types) {
            this.types = 0;
            for (EnumCartBase type : types) {
                this.types |= (1 << type.ordinal());
            }
        }

        public boolean supportsBase(EnumCartBase type) {
            return (this.types & (1 << type.ordinal())) > 0;
        }
    }

    public final EnumMinecart type;
    public final EnumCartBase base;

    public ItemModMinecart(Properties properties, EnumMinecart type, EnumCartBase base) {
        super(properties.stacksTo(4));
        this.type = type;
        this.base = base;
        DispenserBlock.registerBehavior(this, DISPENSE_BEHAVIOR);
    }

    /** Port-Gegenstueck zu {@code createCartItem(base, cart)}. */
    public static ItemStack createCartItem(EnumCartBase base, EnumMinecart cart) {
        return new ItemStack(com.hbm_m.item.ModItems.cart(cart, base));
    }

    @Override
    public @NotNull String getDescriptionId() {
        return "item.hbm_m.cart." + type.name().toLowerCase(java.util.Locale.US);
    }

    private static final DispenseItemBehavior DISPENSE_BEHAVIOR = new DefaultDispenseItemBehavior() {
        private final DefaultDispenseItemBehavior behaviourDefaultDispenseItem = new DefaultDispenseItemBehavior();

        @Override
        public @NotNull ItemStack execute(BlockSource source, ItemStack stack) {
            Direction enumfacing = com.hbm_m.platform.DispenseHooks.state(source).getValue(DispenserBlock.FACING);
            Level world = com.hbm_m.platform.DispenseHooks.level(source);
            double x = com.hbm_m.platform.DispenseHooks.x(source) + enumfacing.getStepX() * 1.125D;
            double y = com.hbm_m.platform.DispenseHooks.y(source) + enumfacing.getStepY() * 1.125D;
            double z = com.hbm_m.platform.DispenseHooks.z(source) + enumfacing.getStepZ() * 1.125D;
            BlockPos ipos = com.hbm_m.platform.DispenseHooks.pos(source).relative(enumfacing);
            BlockState block = world.getBlockState(ipos);
            double yOffset;

            if (block.is(net.minecraft.tags.BlockTags.RAILS)) {
                yOffset = 0.0D;
            } else {
                if (!block.isAir() || !world.getBlockState(ipos.below()).is(net.minecraft.tags.BlockTags.RAILS)) {
                    return this.behaviourDefaultDispenseItem.dispense(source, stack);
                }

                yOffset = -1.0D;
            }

            AbstractMinecart entityminecart = createMinecart(world, x, y + yOffset, z, stack);

            if (StackNbt.hasCustomName(stack)) {
                entityminecart.setCustomName(stack.getHoverName());
            }

            world.addFreshEntity(entityminecart);
            stack.shrink(1);
            return stack;
        }

        @Override
        protected void playSound(BlockSource source) {
            com.hbm_m.platform.DispenseHooks.level(source).levelEvent(1000, com.hbm_m.platform.DispenseHooks.pos(source), 0);
        }
    };

    @Override
    public @NotNull InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        if (world.getBlockState(pos).getBlock() instanceof BaseRailBlock) {
            ItemStack stack = ctx.getItemInHand();
            if (!world.isClientSide) {
                Vec3 hit = ctx.getClickLocation();
                AbstractMinecart entityminecart = createMinecart(world, hit.x, hit.y, hit.z, stack);

                if (StackNbt.hasCustomName(stack)) {
                    entityminecart.setCustomName(stack.getHoverName());
                }

                world.addFreshEntity(entityminecart);
            }

            stack.shrink(1);
            return InteractionResult.sidedSuccess(world.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }

    public static AbstractMinecart createMinecart(Level world, double x, double y, double z, ItemStack stack) {
        ItemModMinecart item = (ItemModMinecart) stack.getItem();
        EnumCartBase base = item.base;
        return switch (item.type) {
            case CRATE -> new EntityMinecartCrate(ModEntities.CART_CRATE.get(), world, x, y, z, base, stack);
            case DESTROYER -> new EntityMinecartDestroyer(ModEntities.CART_DESTROYER.get(), world, x, y, z, base);
            case EMPTY -> new EntityMinecartOre(ModEntities.CART_ORE.get(), world, x, y, z, base);
            case POWDER -> new EntityMinecartPowder(ModEntities.CART_POWDER.get(), world, x, y, z, base);
            case SEMTEX -> new EntityMinecartSemtex(ModEntities.CART_SEMTEX.get(), world, x, y, z, base);
        };
    }
}
