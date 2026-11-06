package com.hbm_m.explosion;

import java.util.UUID;

/**
 * Заглушка для MK5: без разрушения блоков, мгновенно «завершён».
 */
public final class NoOpExplosionRay implements IExplosionRay {

    public static final NoOpExplosionRay INSTANCE = new NoOpExplosionRay();

    private NoOpExplosionRay() {}

    @Override
    public void update(long msBudget) {
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

    @Override
    public boolean isContained() {
        return true;
    }

    @Override
    public void setDetonator(UUID detonator) {
        // no-op
    }
}
