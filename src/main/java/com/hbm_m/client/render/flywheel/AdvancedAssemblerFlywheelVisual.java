package com.hbm_m.client.render.flywheel;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.joml.Vector4fc;

import com.hbm_m.block.entity.machines.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.block.entity.machines.MachineAdvancedAssemblerBlockEntity.ClientTicker;
import com.hbm_m.block.machines.MachineAdvancedAssemblerBlock;
import com.hbm_m.client.model.MachineAdvancedAssemblerBakedModel;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.util.MultipartFacingTransforms;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBuilder;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class AdvancedAssemblerFlywheelVisual extends AbstractBlockEntityVisual<MachineAdvancedAssemblerBlockEntity>
        implements SimpleDynamicVisual {

    /**
     * Indirect-backend Flywheel делает Hi-Z occlusion cull по bounding sphere. Подмодели рук/головы
     * дают тесную сферу около начала координат части; после переноса в мир сфера ошибочно
     * оказывается «за» depth от корпуса и выкидывается. Небольшое наращивание радиуса устраняет это.
     */
    private static final float ARM_PART_BOUNDING_SPHERE_PADDING = 3.25f;
    private static final float MATRIX_EPSILON = 1.0E-5f;

    private final MachineAdvancedAssemblerBakedModel assemblerModel;
    private final List<TransformedInstance> allInstances = new ArrayList<>();
    private final Map<TransformedInstance, InstanceState> instanceStates = new IdentityHashMap<>();
    private final Matrix4f matRing = new Matrix4f();
    private final Matrix4f matLower = new Matrix4f();
    private final Matrix4f matUpper = new Matrix4f();
    private final Matrix4f matHead = new Matrix4f();
    private final Matrix4f matSpike = new Matrix4f();
    private final Matrix4f matBlockFacing = new Matrix4f();
    private final Matrix4f matScratch = new Matrix4f();
    private final Matrix4f matStaticParts = new Matrix4f();
    private final Matrix4f matArmRingBase = new Matrix4f();

    private int lastPackedLight = Integer.MIN_VALUE;

    private final @Nullable TransformedInstance instBase;
    private final @Nullable TransformedInstance instFrame;
    private final @Nullable TransformedInstance instRing;
    private final @Nullable TransformedInstance instArmLower1;
    private final @Nullable TransformedInstance instArmUpper1;
    private final @Nullable TransformedInstance instHead1;
    private final @Nullable TransformedInstance instSpike1;
    private final @Nullable TransformedInstance instArmLower2;
    private final @Nullable TransformedInstance instArmUpper2;
    private final @Nullable TransformedInstance instHead2;
    private final @Nullable TransformedInstance instSpike2;

    private static final class InstanceState {
        private final Matrix4f lastWorldMatrix = new Matrix4f();
        private int lastPackedLight = Integer.MIN_VALUE;
        private boolean hasWorldMatrix = false;
        private boolean visible = false;
    }

    public AdvancedAssemblerFlywheelVisual(VisualizationContext ctx, MachineAdvancedAssemblerBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);
        if (level == null) {
            throw new IllegalStateException("Advanced Assembler BE has null level");
        }
        BakedModel raw = Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState);
        if (!(raw instanceof MachineAdvancedAssemblerBakedModel m)) {
            throw new IllegalStateException("Expected MachineAdvancedAssemblerBakedModel, got " + raw.getClass().getName());
        }
        this.assemblerModel = m;

        instBase = createPartInstance(ctx, "Base");
        instFrame = createPartInstance(ctx, "Frame");
        instRing = createPartInstance(ctx, "Ring");
        instArmLower1 = createPartInstance(ctx, "ArmLower1");
        instArmUpper1 = createPartInstance(ctx, "ArmUpper1");
        instHead1 = createPartInstance(ctx, "Head1");
        instSpike1 = createPartInstance(ctx, "Spike1");
        instArmLower2 = createPartInstance(ctx, "ArmLower2");
        instArmUpper2 = createPartInstance(ctx, "ArmUpper2");
        instHead2 = createPartInstance(ctx, "Head2");
        instSpike2 = createPartInstance(ctx, "Spike2");

        updateBlockFacingMatrix();
        applyTransformsAndVisibility(partialTick);
    }

    private record PaddedBoundingModel(Model inner, float extraRadius) implements Model {
        @Override
        public List<Model.ConfiguredMesh> meshes() {
            return inner.meshes();
        }

        @Override
        public Vector4fc boundingSphere() {
            Vector4f s = new Vector4f(inner.boundingSphere());
            s.w += extraRadius;
            return s;
        }
    }

    private @Nullable TransformedInstance createPartInstance(VisualizationContext ctx, String partName) {
        BakedModel part = assemblerModel.getPart(partName);
        if (part == null) {
            return null;
        }
        Model flyModel = BakedModelBuilder.create(part)
                .level(level)
                .pos(pos)
                .build();
        if (needsBoundingSpherePadding(partName)) {
            flyModel = new PaddedBoundingModel(flyModel, ARM_PART_BOUNDING_SPHERE_PADDING);
        }
        TransformedInstance inst = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, flyModel)
                .createInstance();
        allInstances.add(inst);
        instanceStates.put(inst, new InstanceState());
        return inst;
    }

    private static boolean needsBoundingSpherePadding(String partName) {
        return partName.startsWith("Arm") || partName.startsWith("Head") || partName.startsWith("Spike");
    }

    private void updateBlockFacingMatrix() {
        Direction facing = blockState.getValue(MachineAdvancedAssemblerBlock.FACING);
        BlockPos vp = getVisualPosition();
        matBlockFacing.identity()
                .translate(vp.getX() + 0.5f, vp.getY(), vp.getZ() + 0.5f)
                .rotateY((float) Math.toRadians(90f))
                .rotateY((float) Math.toRadians(MultipartFacingTransforms.legacyFacingRotationYDegrees(facing)));
    }

    private static boolean shouldSkipAnimationForDistance(BlockPos blockPos) {
        var minecraft = Minecraft.getInstance();
        var camera = minecraft.gameRenderer.getMainCamera();
        var cameraPos = camera.getPosition();
        double dx = blockPos.getX() + 0.5 - cameraPos.x;
        double dy = blockPos.getY() + 0.5 - cameraPos.y;
        double dz = blockPos.getZ() + 0.5 - cameraPos.z;
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        int thresholdChunks = ModClothConfig.get().modelUpdateDistance;
        double thresholdBlocks = thresholdChunks * 16.0;
        double thresholdSquared = thresholdBlocks * thresholdBlocks;
        return distanceSquared > thresholdSquared;
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        AABB bounds = blockEntity.getRenderBoundingBox();
        var origin = visualizationContext.renderOrigin();

        float minX = (float) (bounds.minX - origin.getX());
        float minY = (float) (bounds.minY - origin.getY());
        float minZ = (float) (bounds.minZ - origin.getZ());
        float maxX = (float) (bounds.maxX - origin.getX());
        float maxY = (float) (bounds.maxY - origin.getY());
        float maxZ = (float) (bounds.maxZ - origin.getZ());

        float centerX = (minX + maxX) * 0.5f;
        float centerY = (minY + maxY) * 0.5f;
        float centerZ = (minZ + maxZ) * 0.5f;
        float dx = maxX - centerX;
        float dy = maxY - centerY;
        float dz = maxZ - centerZ;
        float radius = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        return frustum.testSphere(centerX, centerY, centerZ, radius);
    }

    @Override
    public void beginFrame(Context context) {
        if (!ModClothConfig.useFlywheelAdvancedAssemblerPath()) {
            hideAll();
            return;
        }
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            hideAll();
            return;
        }
        if (!isVisible(context.frustum())) {
            hideAll();
            return;
        }
        if (doDistanceLimitThisFrame(context)) {
            return;
        }

        updateBlockFacingMatrix();
        applyTransformsAndVisibility(context.partialTick());
    }

    private void applyTransformsAndVisibility(float partialTick) {
        boolean lod = shouldSkipAnimationForDistance(pos);
        int packedLight = computePackedLight();

        matScratch.identity().translate(-0.5f, 0f, -0.5f);
        matStaticParts.set(matBlockFacing).mul(matScratch);
        applyWorldTransform(instBase, matStaticParts, packedLight);

        boolean showFrame = blockState.hasProperty(MachineAdvancedAssemblerBlock.FRAME)
                && blockState.getValue(MachineAdvancedAssemblerBlock.FRAME);
        if (instFrame != null) {
            if (showFrame) {
                matScratch.identity().translate(-0.5f, 0f, -0.5f);
                matStaticParts.set(matBlockFacing).mul(matScratch);
                applyWorldTransform(instFrame, matStaticParts, packedLight);
            } else {
                setVisible(instFrame, false);
            }
        }

        float ringAngle = lod ? 0f : Mth.lerp(partialTick, blockEntity.getPrevRingAngle(), blockEntity.getRingAngle());
        matRing.identity()
                .rotateY((float) Math.toRadians(ringAngle))
                .translate(-0.5f, 0f, -0.5f);
        matStaticParts.set(matBlockFacing).mul(matRing);
        applyWorldTransform(instRing, matStaticParts, packedLight);

        matArmRingBase.set(matBlockFacing).mul(matRing);

        // LOD только замирает кольцо; руки всё равно рисуем (раньше setArmVisible(false) оставлял «обрубок» без рук).
        ClientTicker.AssemblerArm[] arms = blockEntity.getArms();
        if (arms.length >= 2 && arms[0] != null && arms[1] != null) {
            setArmVisible(true);
            renderArm(arms[0], false, partialTick, packedLight, matArmRingBase);
            renderArm(arms[1], true, partialTick, packedLight, matArmRingBase);
        } else {
            setArmVisible(false);
        }

        lastPackedLight = packedLight;
    }

    private void setArmVisible(boolean visible) {
        setVisible(instArmLower1, visible);
        setVisible(instArmUpper1, visible);
        setVisible(instHead1, visible);
        setVisible(instSpike1, visible);
        setVisible(instArmLower2, visible);
        setVisible(instArmUpper2, visible);
        setVisible(instHead2, visible);
        setVisible(instSpike2, visible);
    }

    private void setVisible(@Nullable TransformedInstance inst, boolean visible) {
        if (inst != null) {
            InstanceState state = instanceStates.get(inst);
            if (state == null) {
                inst.setVisible(visible);
                return;
            }
            if (state.visible != visible) {
                inst.setVisible(visible);
                state.visible = visible;
            }
        }
    }

    private void applyWorldTransform(@Nullable TransformedInstance inst, Matrix4f worldMatrix, int packedLight) {
        if (inst == null) {
            return;
        }
        InstanceState state = instanceStates.get(inst);
        if (state == null) {
            inst.setVisible(true);
            inst.setIdentityTransform()
                    .mul(worldMatrix)
                    .light(packedLight)
                    .setChanged();
            return;
        }

        boolean transformChanged = !state.hasWorldMatrix || !state.lastWorldMatrix.equals(worldMatrix, MATRIX_EPSILON);
        boolean lightChanged = state.lastPackedLight != packedLight;
        boolean visibilityChanged = !state.visible;

        if (visibilityChanged) {
            inst.setVisible(true);
            state.visible = true;
        }

        if (transformChanged || lightChanged || visibilityChanged) {
            inst.setIdentityTransform()
                    .mul(worldMatrix)
                    .light(packedLight)
                    .setChanged();
            state.lastWorldMatrix.set(worldMatrix);
            state.lastPackedLight = packedLight;
            state.hasWorldMatrix = true;
        }
    }

    private void renderArm(ClientTicker.AssemblerArm arm, boolean inverted, float pt, int packedLight, Matrix4f baseTransform) {
        if (arm == null) {
            return;
        }
        float a0 = Mth.lerp(pt, arm.prevAngles[0], arm.angles[0]);
        float a1 = Mth.lerp(pt, arm.prevAngles[1], arm.angles[1]);
        float a2 = Mth.lerp(pt, arm.prevAngles[2], arm.angles[2]);
        float a3 = Mth.lerp(pt, arm.prevAngles[3], arm.angles[3]);
        float angleSign = inverted ? -1f : 1f;
        float zBase = inverted ? -0.9375f : 0.9375f;

        matLower.set(baseTransform)
                .translate(0.5f, 1.625f, 0.5f + zBase)
                .rotateX((float) Math.toRadians(angleSign * a0))
                .translate(-0.5f, -1.625f, -(0.5f + zBase));
        applyWorldTransform(inverted ? instArmLower2 : instArmLower1, matLower, packedLight);

        matUpper.set(matLower)
                .translate(0.5f, 2.375f, 0.5f + zBase)
                .rotateX((float) Math.toRadians(angleSign * a1))
                .translate(-0.5f, -2.375f, -(0.5f + zBase));
        applyWorldTransform(inverted ? instArmUpper2 : instArmUpper1, matUpper, packedLight);

        matHead.set(matUpper)
                .translate(0.5f, 2.375f, 0.5f + (zBase * 0.4667f))
                .rotateX((float) Math.toRadians(angleSign * a2))
                .translate(-0.5f, -2.375f, -(0.5f + (zBase * 0.4667f)));
        applyWorldTransform(inverted ? instHead2 : instHead1, matHead, packedLight);

        matSpike.set(matHead)
                .translate(0f, a3, 0f);
        applyWorldTransform(inverted ? instSpike2 : instSpike1, matSpike, packedLight);
    }

    private void hideAll() {
        for (TransformedInstance inst : allInstances) {
            if (inst != null) {
                setVisible(inst, false);
            }
        }
    }

    private void relightAll() {
        int light = computePackedLight();
        for (TransformedInstance inst : allInstances) {
            if (inst != null) {
                InstanceState state = instanceStates.get(inst);
                if (state == null) {
                    inst.light(light).setChanged();
                    continue;
                }
                if (state.visible && state.lastPackedLight != light) {
                    inst.light(light).setChanged();
                    state.lastPackedLight = light;
                }
            }
        }
    }

    @Override
    public void updateLight(float partialTick) {
        int light = computePackedLight();
        if (light == lastPackedLight) {
            return;
        }
        lastPackedLight = light;
        relightAll();
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        for (TransformedInstance inst : allInstances) {
            if (inst != null && inst.handle().isVisible()) {
                consumer.accept(inst);
            }
        }
    }

    @Override
    protected void _delete() {
        for (TransformedInstance inst : allInstances) {
            if (inst != null) {
                inst.delete();
            }
        }
        allInstances.clear();
        instanceStates.clear();
    }
}
