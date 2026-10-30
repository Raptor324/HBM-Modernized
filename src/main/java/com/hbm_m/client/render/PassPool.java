package com.hbm_m.client.render;

import java.nio.ByteBuffer;

/**
 * Global multi-draw pass pool (output SSBO + EBO + command buffer)
 * holding every cache-admitted mesh at per-mesh vertBase / eboWordBase offsets.
 */
public final class PassPool {
    public final boolean slim;
    public int outputSsbo = -1;
    public long outputAllocBytes = 0;
    public int eboSsbo = -1;
    public long eboAllocBytes = 0;
    /** GPU-count flush path (GL 4.6): one 20-byte indirect command per pending-entry slot. */
    public int cmdSsbo = -1;
    public long cmdSlots = 0;
    /** Bumped whenever the slot store is (re)specified. */
    public int cmdGen = 0;
    /** Per-slot owner: which cache last wrote each slot. */
    public ModeCache[] cmdSlotOwners = new ModeCache[64];
    /** CPU-path command upload store (compacted commands). */
    public int cmdCpuSsbo = -1;
    /** GL 4.6 parameter buffer: the 4-byte drawcount consumed by glMultiDrawElementsIndirectCount. */
    public int paramSsbo = -1;
    public int vao = -1;
    /** CPU-side scratch for command building. */
    public ByteBuffer cmdCpu = null;
    /** Counts pool store reallocs for F3 counters. */
    public long reallocs = 0;
    /** Session count of pool store shrinks. */
    public long shrinks = 0;
    /** frameStamp of the last relayoutPool run for this pass. */
    public long lastRelayoutFrame = -1L;

    public PassPool(boolean slim) {
        this.slim = slim;
    }
}
