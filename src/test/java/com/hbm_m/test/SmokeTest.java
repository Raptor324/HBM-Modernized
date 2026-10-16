package com.hbm_m.test;

import com.hbm_m.client.render.retained.upload.SparseUpdateQueue;
import com.hbm_m.client.render.retained.alloc.InstanceSlotAllocator;

public class SmokeTest {
    public static void main(String[] args) {
        InstanceSlotAllocator allocator = new InstanceSlotAllocator(65536);
        SparseUpdateQueue queue = new SparseUpdateQueue(allocator);
        System.out.println("Allocated queue pending=" + queue.getPendingCount());
        queue.destroy();
        System.out.println("Destroyed queue ok");
    }
}
