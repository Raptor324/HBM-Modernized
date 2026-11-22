// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 mlbv <51232730+mlbv@users.noreply.github.com>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from the NTM Next project (MK5 crater generation system).
package com.hbm_m.util;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

/**
 * Точка доступа к sun.misc.Unsafe для неблокирующих структур движка взрыва MK5
 * (CAS-битсеты, off-heap маски, публикация секций чанков).
 */
public final class UnsafeHolder {

    public static final Unsafe U = bootstrap();

    private UnsafeHolder() {}

    private static Unsafe bootstrap() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final long RA_BASE = U.arrayBaseOffset(Object[].class);
    private static final int RA_SHIFT = Integer.numberOfTrailingZeros(U.arrayIndexScale(Object[].class));
    private static final long JA_BASE = U.arrayBaseOffset(long[].class);
    private static final int JA_SHIFT = Integer.numberOfTrailingZeros(U.arrayIndexScale(long[].class));

    public static long offLong(int i) {
        return ((long) i << JA_SHIFT) + JA_BASE;
    }

    public static long offReference(int i) {
        return ((long) i << RA_SHIFT) + RA_BASE;
    }

    public static long fieldOffset(Class<?> clz, String fieldName) {
        try {
            return U.objectFieldOffset(clz.getDeclaredField(fieldName));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
