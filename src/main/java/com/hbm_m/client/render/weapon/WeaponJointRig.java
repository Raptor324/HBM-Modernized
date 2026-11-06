package com.hbm_m.client.render.weapon;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.hbm_m.client.render.NucleusJointSpecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Declarative kinematic rig for a weapon model, registering joint specs into {@link NucleusJointSpecs}.
 */
@OnlyIn(Dist.CLIENT)
public final class WeaponJointRig {

    public record JointDef(int jointId, int kind, float axisX, float axisY, float axisZ,
                           float pivotX, float pivotY, float pivotZ) { }

    public record ChainDef(int baseJointId, int nodeCount, int[] kinds, float[] axes, float[] pivots) { }

    private static final java.util.concurrent.atomic.AtomicInteger NEXT_LOCAL_JOINT_ID =
            new java.util.concurrent.atomic.AtomicInteger(0);

    private final ResourceLocation modelLocation;
    private final Map<String, JointDef> singleJoints = new ConcurrentHashMap<>();
    private final Map<String, ChainDef> jointChains = new ConcurrentHashMap<>();
    private String leftPartName = null;
    private String[] restParts = new String[0];

    public WeaponJointRig(ResourceLocation modelLocation) {
        this.modelLocation = modelLocation;
    }

    public ResourceLocation getModelLocation() {
        return modelLocation;
    }

    public WeaponJointRig registerRotate(String partName, float axisX, float axisY, float axisZ,
                                         float pivotX, float pivotY, float pivotZ) {
        int id = NEXT_LOCAL_JOINT_ID.getAndIncrement();
        singleJoints.put(partName, new JointDef(id, NucleusJointSpecs.KIND_ROTATE, axisX, axisY, axisZ, pivotX, pivotY, pivotZ));
        return this;
    }

    public WeaponJointRig registerTranslate(String partName, float axisX, float axisY, float axisZ) {
        int id = NEXT_LOCAL_JOINT_ID.getAndIncrement();
        singleJoints.put(partName, new JointDef(id, NucleusJointSpecs.KIND_TRANSLATE, axisX, axisY, axisZ, 0f, 0f, 0f));
        return this;
    }

    public WeaponJointRig registerTranslateAxis(String partName, float axisX, float axisY, float axisZ) {
        int id = NEXT_LOCAL_JOINT_ID.getAndIncrement();
        singleJoints.put(partName, new JointDef(id, NucleusJointSpecs.KIND_TRANSLATE_AXIS, axisX, axisY, axisZ, 0f, 0f, 0f));
        return this;
    }

    public WeaponJointRig registerChain(String partName, int[] kinds, float[] axes, float[] pivots) {
        int baseId = NEXT_LOCAL_JOINT_ID.getAndAdd(kinds.length);
        jointChains.put(partName, new ChainDef(baseId, kinds.length, kinds, axes, pivots));
        return this;
    }

    public WeaponJointRig setLeftPartName(String leftPartName) {
        this.leftPartName = leftPartName;
        return this;
    }

    public String getLeftPartName() {
        return leftPartName != null ? leftPartName : "Gun";
    }

    public boolean hasLeftPart() {
        return leftPartName != null;
    }

    /**
     * Declares the list of non-receiver parts drawn in static rest pose
     * (inventory, third-person, entity, mod table), e.g. "StockBack", "StockFront", "Slide", "Magazine".
     */
    public WeaponJointRig registerRestParts(String... parts) {
        this.restParts = parts != null ? parts.clone() : new String[0];
        return this;
    }

    public String[] getRestParts() {
        return restParts;
    }

    public JointDef getJoint(String partName) {
        return singleJoints.get(partName);
    }

    public ChainDef getChain(String partName) {
        return jointChains.get(partName);
    }

    public Map<String, JointDef> getSingleJoints() {
        return Collections.unmodifiableMap(singleJoints);
    }

    public Map<String, ChainDef> getJointChains() {
        return Collections.unmodifiableMap(jointChains);
    }
}
