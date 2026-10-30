package com.hbm_m.client.render;

/**
 * Per-pass cache of one {@link BakedMesh}: everything needed to draw without re-baking.
 */
public final class ModeCache {
    public int filled = -1;               // CPU EBO fill level (no-cull path)
    public int eboCapacity = 0;
    public int recordSsbo = -1;
    public final long[] recordSize = { -1L };
    public float[] recordCopy;            // heap snapshot of the records at last bake
    public int recordCount = -1;
    public long boneKey = -1;             // bone epoch folded into validity
    // Span-level re-expansion state: dirty-index list
    public int dirtySsbo = -1;
    public long dirtyBytes = -1;
    public int[] dirtyIdx = null;
    public int dirtyCount = 0;
    // Cull state
    public int ctlSsbo = -1;
    public long ctlBytes = -1;
    public long frustumStamp = Long.MIN_VALUE;
    public boolean cullValid = false;
    public boolean outputValid = false;
    public long lastUseFrame = 0L;
    // Instance-granularity cull result of the last cull run for THIS cache
    public long lastVisibleCount = 0L;
    public boolean countsDirty = false;
    public int stagingSlot = -1;
    public int cmdGenStamp = -1;
    public int cmdSlotWritten = -1;
    public long lastOccludedCount = 0L;
    public boolean lastCullHiZ = false;
    // Global-batch state
    public int outInstances = 0;
    public int eboInstances = 0;
    public long admissionRefusedFrame = -1L;
    public long sourceMutationEpoch = -1L;
}
