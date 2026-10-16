package com.hbm_m.test;

import com.hbm_m.client.render.retained.alloc.InstanceSlotAllocator;

public class Milestone1SlotAllocatorTest {

    public static void main(String[] args) {
        testSlotAllocationAndPersistence();
        testSlotFreeAndReuse();
        testHighWaterMark();
        System.out.println("All Milestone1SlotAllocatorTest tests passed successfully!");
    }

    private static void testSlotAllocationAndPersistence() {
        InstanceSlotAllocator allocator = new InstanceSlotAllocator(128);

        long posKey1 = 123456789L;
        long posKey2 = 987654321L;

        assert !allocator.hasSlot(posKey1) : "Should not have posKey1 initially";
        assert allocator.getSlot(posKey1) == -1 : "getSlot should return -1 when unallocated";

        int slot1 = allocator.allocateSlot(posKey1);
        assert slot1 >= 0 : "Allocated slot must be non-negative";
        assert allocator.hasSlot(posKey1) : "Should have posKey1 after allocation";
        assert allocator.getSlot(posKey1) == slot1 : "getSlot must return allocated slot";

        // Idempotent allocation for same posKey
        int slot1Again = allocator.allocateSlot(posKey1);
        assert slot1Again == slot1 : "Allocating existing posKey must return same slot";

        int slot2 = allocator.allocateSlot(posKey2);
        assert slot2 != slot1 : "Distinct posKeys must receive distinct slots";
        assert allocator.hasSlot(posKey2) : "Should have posKey2 after allocation";
        assert allocator.getSlot(posKey2) == slot2 : "getSlot must return slot2";
    }

    private static void testSlotFreeAndReuse() {
        InstanceSlotAllocator allocator = new InstanceSlotAllocator(128);

        long posKey1 = 1111L;
        int slot1 = allocator.allocateSlot(posKey1);

        allocator.freeSlot(posKey1);
        assert !allocator.hasSlot(posKey1) : "posKey1 must not be present after free";
        assert allocator.getSlot(posKey1) == -1 : "getSlot must return -1 after free";

        // FIFO queue: slot1 was enqueued at the back of the queue
        assert !allocator.isSlotAllocated(slot1) : "Slot must not be allocated in bitmap after free";
        long posKey2 = 2222L;
        int slot2 = allocator.allocateSlot(posKey2);
        assert slot2 != -1 : "Next allocation must succeed";
        assert allocator.hasSlot(posKey2) : "posKey2 must be registered";
    }

    private static void testHighWaterMark() {
        InstanceSlotAllocator allocator = new InstanceSlotAllocator(128);

        assert allocator.getHighWaterMark() == 0 : "Initial highWaterMark must be 0";

        long posKey1 = 100L;
        long posKey2 = 200L;
        long posKey3 = 300L;

        allocator.allocateSlot(posKey1);
        allocator.allocateSlot(posKey2);
        int slot3 = allocator.allocateSlot(posKey3);

        assert allocator.getHighWaterMark() == slot3 + 1 : "highWaterMark must be max slot + 1";

        allocator.reset();
        assert allocator.getHighWaterMark() == 0 : "reset must reset highWaterMark to 0";
        assert !allocator.hasSlot(posKey1) : "reset must clear all slot mappings";
    }
}
