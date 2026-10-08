package com.hbm_m.blockentity.machines;

import java.util.List;

import com.hbm_m.api.fluids.IFillableItem;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.block.machines.RefuelerBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityRefueler}: Wandtank (100 mB, Vorgabe Kerosin) mit Anschluss hinten. Spieler direkt davor
 * bekommen jedes befuellbare Ausruestungsteil (auch eingebaute Ruestungsmods) aufgefuellt; dabei zischt es (1/s)
 * und Fluessigkeit spritzt. Die Fuellstandsanzeige gleitet clientseitig nach.
 */
public class RefuelerBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements com.hbm_m.api.fluids.IFluidStandardReceiverMK2 {

    public double fillLevel;
    public double prevFillLevel;

    private boolean isOperating = false;
    private int operatingTime;

    public FluidTank tank;

    public RefuelerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REFUELER_BE.get(), pos, state);
        tank = new FluidTank(ModFluids.KEROSENE.getSource(), 100);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RefuelerBlockEntity be) {
        be.update(level, pos, state);
    }

    private void update(Level world, BlockPos pos, BlockState state) {
        Direction dir = state.getValue(RefuelerBlock.FACING).getOpposite();
        Direction rot = dir.getClockWise();

        if (!world.isClientSide) {
            trySubscribe(tank.getTankType(), world, pos.relative(dir), dir);

            boolean wasOperating = isOperating;
            isOperating = false;

            List<Player> players = world.getEntitiesOfClass(Player.class, new AABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5).inflate(0.5, 0.0, 0.5));

            int prevFill = tank.getFill();
            for (Player player : players) {
                for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.MAINHAND, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD }) {

                    ItemStack stack = player.getItemBySlot(slot);
                    if (stack.isEmpty()) continue;

                    if (fillFillable(stack)) {
                        isOperating = true;
                    }

                    if (stack.getItem() instanceof ArmorItem && ArmorModificationHelper.hasMods(stack)) {
                        for (ItemStack mod : ArmorModificationHelper.pryMods(stack)) {
                            if (mod == null || mod.isEmpty()) continue;

                            if (fillFillable(mod)) {
                                ArmorModificationHelper.applyMod(stack, mod);
                                isOperating = true;
                            }
                        }
                    }
                }
            }

            if (isOperating) {
                if (operatingTime % 20 == 0)
                    world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2F, 0.5F);

                operatingTime++;
            } else {
                operatingTime = 0;
            }

            // networkPackNT(150)
            if (isOperating != wasOperating || tank.getFill() != prevFill || world.getGameTime() % 20 == 0) {
                setChanged();
                world.sendBlockUpdated(pos, state, state, 3);
            }
        } else {
            if (isOperating) {
                RandomSource rand = world.random;

                CompoundTag data = new CompoundTag();
                data.putString("type", "fluidfill");
                data.putInt("color", FluidType.forFluid(tank.getTankType()).getColor());
                data.putDouble("posX", pos.getX() + 0.5 + rand.nextDouble() * 0.0625 + dir.getStepX() * 0.5 + rot.getStepX() * 0.25);
                data.putDouble("posZ", pos.getZ() + 0.5 + rand.nextDouble() * 0.0625 + dir.getStepZ() * 0.5 + rot.getStepZ() * 0.25);
                data.putDouble("posY", pos.getY() + 0.375);
                data.putDouble("mX", -dir.getStepX() + rand.nextGaussian() * 0.1);
                data.putDouble("mZ", -dir.getStepZ() + rand.nextGaussian() * 0.1);
                data.putDouble("mY", 0D);

                com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
            }

            prevFillLevel = fillLevel;

            double targetFill = (double) tank.getFill() / (double) tank.getMaxFill();
            float f = targetFill > fillLevel || !isOperating ? 0.1F : 0.01F;
            fillLevel = fillLevel + (targetFill - fillLevel) * f;
        }
    }

    private boolean fillFillable(ItemStack stack) {
        if (stack.getItem() instanceof IFillableItem fillable) {
            if (fillable.acceptsFluid(tank.getTankType(), stack)) {
                int prevFill = tank.getFill();
                tank.setFill(fillable.tryFill(tank.getTankType(), tank.getFill(), stack));
                return tank.getFill() < prevFill;
            }
        }

        return false;
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        tank.writeToNBT(nbt, "t");
        nbt.putBoolean("isOperating", isOperating);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        tank.readFromNBT(nbt, "t");
        isOperating = nbt.getBoolean("isOperating");
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { tank };
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { tank };
    }
}
