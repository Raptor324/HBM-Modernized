package com.hbm_m.mixin.compat.sable;

//? if >= 1.21.1 {
/*import com.hbm_m.block.machines.MachineTurbofanBlock;
import com.hbm_m.blockentity.machines.MachineTurbofanBlockEntity;
import com.hbm_m.compat.sable.TurbofanVehiclePhysics;
import com.hbm_m.interfaces.TurbofanAirflowFrame;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.block.propeller.BlockEntitySubLevelPropellerActor;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/^*
 * Делает работающий турбовентилятор "родным" движителем для движущихся конструкций.
 * Применяется только при наличии мода "sable" (фильтр в HbmMixinConfigPlugin);
 * на 1.20.1 файл вырезан препроцессором - там остаётся пустой стаб ниже.
 ^/

@Mixin(MachineTurbofanBlockEntity.class)
public abstract class TurbofanSableMixin extends BlockEntity
        implements BlockEntitySubLevelPropellerActor, BlockEntityPropeller, TurbofanAirflowFrame {
    protected TurbofanSableMixin(BlockEntityType<?> type, BlockPos position, BlockState state) {
        super(type, position, state);
    }

    private MachineTurbofanBlockEntity hbm$turbofan() {
        return (MachineTurbofanBlockEntity) (Object) this;
    }

    private SubLevel hbm$subLevel() {
        return level == null ? null : Sable.HELPER.getContaining(level, worldPosition);
    }

    @Override
    public Vec3 hbm$localVectorToWorld(Vec3 localVector) {
        SubLevel subLevel = hbm$subLevel();
        return subLevel == null ? localVector : subLevel.logicalPose().transformNormal(localVector);
    }

    @Override
    public Vec3 hbm$localPosToWorld(Vec3 localPos) {
        SubLevel subLevel = hbm$subLevel();
        return subLevel == null ? localPos : subLevel.logicalPose().transformPosition(localPos);
    }

    @Override
    public AABB hbm$worldBoundsToLocal(AABB worldBounds) {
        SubLevel subLevel = hbm$subLevel();
        return subLevel == null ? worldBounds : new BoundingBox3d(worldBounds)
                .transformInverse(subLevel.logicalPose(), new BoundingBox3d())
                .toMojang();
    }

    @Override
    public double hbm$distanceSquaredToLocalPosition(Vec3 observerPosition, Vec3 localPosition) {
        return level == null
                ? observerPosition.distanceToSqr(localPosition)
                : Sable.HELPER.distanceSquaredWithSubLevels(
                        level, observerPosition, localPosition.x, localPosition.y, localPosition.z);
    }

    @Override
    public BlockEntityPropeller getPropeller() {
        return this;
    }

    @Override
    public Direction getBlockDirection() {
        BlockState state = getBlockState();
        Direction facing = state.hasProperty(MachineTurbofanBlock.FACING)
                ? state.getValue(MachineTurbofanBlock.FACING) : Direction.NORTH;
        return TurbofanVehiclePhysics.exhaustDirection(facing);
    }

    @Override
    public double getAirflow() {
        return TurbofanVehiclePhysics.airflow(hbm$turbofan().getOutput());
    }

    @Override
    public double getThrust() {
        return TurbofanVehiclePhysics.thrust(hbm$turbofan().getOutput());
    }

    /^*
     * BE занимает нижний центральный блок, а ось ротора на блок выше - сила в точке оси
     * не изобретает фантомный тангаж у центрированного двигателя.
     ^/
    @Override
    public boolean isActive() {
        MachineTurbofanBlockEntity turbofan = hbm$turbofan();
        return TurbofanVehiclePhysics.isActive(
                turbofan.wasOn(), turbofan.getOutput(), turbofan.getConsumption());
    }

    /^*
     * BE занимает нижний центральный блок, а ось ротора на блок выше - сила в точке оси
     * не изобретает фантомный тангаж у центрированного двигателя.
     ^/
    @Override
    public void applyForces(ServerSubLevel subLevel, Vec3 thrustDirection, double timeStep) {
        double scaledThrust = getScaledThrust() * timeStep;
        Vector3d thrust = new Vector3d(thrustDirection.x, thrustDirection.y, thrustDirection.z)
                .mul(scaledThrust);
        BlockPos position = getBlockPos();
        Vector3d rotorCenter = new Vector3d(
                position.getX() + 0.5D, position.getY() + 1.5D, position.getZ() + 0.5D);
        // RegistryObject.get() тянет за собой Veil в classpath - берём группу из реестра напрямую.
        QueuedForceGroup forceGroup = subLevel.getOrCreateQueuedForceGroup(ForceGroups.REGISTRY.get(
                ResourceLocation.fromNamespaceAndPath("sable", "propulsion")));
        if (forceGroup == null) return;
        forceGroup.applyAndRecordPointForce(rotorCenter, thrust);
    }
}
*///?} else {
// Sable существует только на 1.21.1; стаб указывает на класс, который есть везде, и ничего не делает.
@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.inventory.AbstractContainerMenu.class)
public abstract class TurbofanSableMixin { }
//?}
