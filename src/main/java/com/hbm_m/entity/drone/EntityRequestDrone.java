package com.hbm_m.entity.drone;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.blockentity.network.MachineDroneDockBlockEntity;
import com.hbm_m.blockentity.network.MachineDroneProviderBlockEntity;
import com.hbm_m.blockentity.network.MachineDroneRequesterBlockEntity;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Port von {@code EntityRequestDrone} (1.7.10 Original, Pipeline B). Fuehrt ein bei Spawn erstelltes
 * "Programm" (Liste aus Wegpunkten, einem Abhol-Muster, {@link ProgramStep#UNLOAD}, {@link ProgramStep#DOCK})
 * Schritt fuer Schritt aus - sobald die Geschwindigkeit ~0 ist (Ziel erreicht) und ein kurzer Cooldown
 * abgelaufen ist, wird der naechste Programmschritt ausgefuehrt. Waypoint-Schritte setzen nur das
 * naechste Ziel; Abhol-/Abgabe-/Dock-Schritte raytracen 4 Bloecke senkrecht nach unten, um die
 * darunterliegende TileEntity zu finden.
 */
public class EntityRequestDrone extends EntityDroneBase {

    public enum ProgramStep { UNLOAD, DOCK }

    private ItemStack heldItem = ItemStack.EMPTY;
    private final List<Object> program = new ArrayList<>();
    private int nextActionTimer = 0;

    public EntityRequestDrone(EntityType<? extends EntityRequestDrone> type, Level level) {
        super(type, level);
    }

    public static EntityRequestDrone create(Level level, double x, double y, double z, List<Object> program) {
        EntityRequestDrone drone = new EntityRequestDrone(ModEntities.REQUEST_DRONE.get(), level);
        drone.setPos(x, y, z);
        drone.program.addAll(program);
        return drone;
    }

    @Override
    public void setTarget(double x, double y, double z) {
        super.setTarget(x, y + 1, z);
    }

    @Override
    public double getSpeed() {
        return 0.625D;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) return;
        if (getDeltaMovement().lengthSqr() >= 0.0001) return;

        if (nextActionTimer > 0) {
            nextActionTimer--;
            return;
        }

        if (program.isEmpty()) {
            selfDestructAndReturnDrone();
            return;
        }

        Object next = program.remove(0);

        if (next instanceof BlockPos pos) {
            setTarget(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        } else if (next instanceof ItemStack pattern && heldItem.isEmpty()) {
            tryPickup(pattern);
            nextActionTimer = 5;
        } else if (next == ProgramStep.UNLOAD && !heldItem.isEmpty()) {
            tryUnload();
            nextActionTimer = 5;
        } else if (next == ProgramStep.DOCK) {
            tryDock();
        }
    }

    private BlockEntity raytraceBelow() {
        Vec3 from = position();
        Vec3 to = from.subtract(0, 4, 0);
        BlockHitResult hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        return level().getBlockEntity(hit.getBlockPos());
    }

    private void tryPickup(ItemStack pattern) {
        if (!(raytraceBelow() instanceof MachineDroneProviderBlockEntity provider)) return;

        ItemStack taken = provider.extractMatching(pattern, pattern.getMaxStackSize());
        if (!taken.isEmpty()) {
            heldItem = taken;
            setAppearance(APPEARANCE_CRATE);
            level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP, net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 0.75F);
        }
    }

    private void tryUnload() {
        if (!(raytraceBelow() instanceof MachineDroneRequesterBlockEntity requester)) return;

        heldItem = requester.depositStock(heldItem);
        if (heldItem.isEmpty()) {
            setAppearance(APPEARANCE_EMPTY);
            level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP, net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 0.75F);
        }
    }

    private void tryDock() {
        if (raytraceBelow() instanceof MachineDroneDockBlockEntity dock) {
            if (dock.dockDrone(heldItem)) {
                level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.BARREL_CLOSE, net.minecraft.sounds.SoundSource.BLOCKS, 2.0F, 1.0F);
                this.discard();
                return;
            }
        }
        selfDestructAndReturnDrone();
    }

    private void selfDestructAndReturnDrone() {
        this.discard();
        if (!heldItem.isEmpty()) {
            level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), heldItem));
        }
        level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), new ItemStack(ModItems.DRONE_REQUEST.get())));
    }

    @Override
    public net.minecraft.world.InteractionResult interact(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        if (!level().isClientSide) {
            selfDestructAndReturnDrone();
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!level().isClientSide && !this.isRemoved()) {
            selfDestructAndReturnDrone();
        }
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        heldItem = tag.contains("held") ? ItemStack.of(tag.getCompound("held")) : ItemStack.EMPTY;
        nextActionTimer = 5;

        program.clear();
        ListTag list = tag.getList("program", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            switch (entry.getString("type")) {
                case "pos" -> {
                    int[] p = entry.getIntArray("pos");
                    program.add(new BlockPos(p[0], p[1], p[2]));
                }
                case "unload" -> program.add(ProgramStep.UNLOAD);
                case "dock" -> program.add(ProgramStep.DOCK);
                case "pattern" -> program.add(ItemStack.of(entry.getCompound("stack")));
                default -> {}
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!heldItem.isEmpty()) tag.put("held", heldItem.save(new CompoundTag()));

        ListTag list = new ListTag();
        for (Object step : program) {
            CompoundTag entry = new CompoundTag();
            if (step instanceof BlockPos pos) {
                entry.putString("type", "pos");
                entry.putIntArray("pos", new int[]{pos.getX(), pos.getY(), pos.getZ()});
            } else if (step == ProgramStep.UNLOAD) {
                entry.putString("type", "unload");
            } else if (step == ProgramStep.DOCK) {
                entry.putString("type", "dock");
            } else if (step instanceof ItemStack pattern) {
                entry.putString("type", "pattern");
                entry.put("stack", pattern.save(new CompoundTag()));
            }
            list.add(entry);
        }
        tag.put("program", list);
    }
}
