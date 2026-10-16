package com.hbm_m.multiblock;

import com.hbm_m.main.MainRegistry;

/**
 * Verbose multiblock teardown tracing. The user cannot pass JVM -D properties,
 * so diagnostics live as a dumb in-code boolean (same pattern as
 * {@code NucleusDebug.DEBUG_JOINTS}).
 *
 * <p>While {@link #TRACE} is on, every teardown-channel event is logged with a
 * distinctive {@code [HBM-M Teardown]} tag so the full chain can be followed:
 * server cascade → S2C packet → client channels B/B2 → presence eviction →
 * retained-graph slots → the stale-render-list draw suppressions.
 *
 * <p><b>Keep {@link #TRACE} = false in normal play</b>: a big multiblock (Hydraulic
 * Frac Tower) fires hundreds of channel events per break, each with a stack trace,
 * and that alone stalled the client for seconds (30k+ lines per session on
 * 2026-10-13). The placement/demolition player logs in MultiblockStructureHelper
 * are NOT gated by this flag - they are one line per event and stay at INFO.
 */
public final class TeardownTrace {

    /** Master switch - hunt mode. Default off; flip to true only to chase a teardown bug. */
    public static boolean TRACE = false;

    private TeardownTrace() {}

    /** Event line without stack trace: high-frequency or self-explanatory events. */
    public static void log(String format, Object... args) {
        if (TRACE) {
            MainRegistry.LOGGER.info("[HBM-M Teardown] " + format, args);
        }
    }

    /**
     * Event line + current stack trace (who called this channel). Call only from
     * cold paths (block break, packet arrival) - never from per-frame code.
     */
    public static void trace(String format, Object... args) {
        if (TRACE) {
            // Append the throwable to the args array explicitly: passing
            // (args, exception) to a varargs parameter would nest the array
            // (SLF4J then prints the Object[] into the first placeholder and
            // stringifies the exception into the next one).
            Object[] all = java.util.Arrays.copyOf(args, args.length + 1);
            all[args.length] = new Exception("teardown trace");
            MainRegistry.LOGGER.info("[HBM-M Teardown] " + format, all);
        }
    }
}
