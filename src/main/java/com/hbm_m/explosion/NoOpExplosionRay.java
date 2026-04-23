package com.hbm_m.explosion;

/**
 * Заглушка для MK5: без разрушения блоков, мгновенно «завершён».
 */
public final class NoOpExplosionRay implements IExplosionRay {

    public static final NoOpExplosionRay INSTANCE = new NoOpExplosionRay();

    private NoOpExplosionRay() {}

    @Override
    public void cacheChunksTick(int processTimeMs) {
        // no-op
    }

    @Override
    public void destructionTick(int processTimeMs) {
        // no-op
    }

    @Override
    public void cancel() {
        // no-op
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
