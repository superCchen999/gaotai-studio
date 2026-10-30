package com.gaotai.studio.task;

import com.gaotai.studio.common.ApiException;
import com.gaotai.studio.config.StudioProperties;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class SimulatorSettings {

    public static final long MAX_DELAY_MS = 120_000L;

    private final AtomicReference<Snapshot> snapshot = new AtomicReference<>();

    public SimulatorSettings(StudioProperties properties) {
        StudioProperties.Simulator configured = properties.getSimulator();
        this.snapshot.set(new Snapshot(
                validateDelay(configured.getStepDelayMs()),
                normalize(configured.getFailStage())));
    }

    public Snapshot get() {
        return snapshot.get();
    }

    public synchronized SimulatorView update(Long stepDelayMs, String failStage) {
        if (stepDelayMs == null || failStage == null || failStage.isBlank()) {
            throw new ApiException(400, "INVALID_SIMULATOR", "请同时提供 stepDelayMs 和 failStage");
        }
        Snapshot next = new Snapshot(validateDelay(stepDelayMs), normalize(failStage));
        snapshot.set(next);
        return next.toView();
    }

    public SimulatorView view() {
        return snapshot.get().toView();
    }

    public static long validateDelay(long stepDelayMs) {
        if (stepDelayMs < 0 || stepDelayMs > MAX_DELAY_MS) {
            throw new ApiException(400, "INVALID_SIMULATOR", "步骤耗时需在 0 到 120000 毫秒之间");
        }
        return stepDelayMs;
    }

    public static SimulatedFailure normalize(String failStage) {
        if (failStage == null || failStage.isBlank()) {
            throw new ApiException(400, "INVALID_SIMULATOR", "失败阶段不能为空");
        }
        try {
            return SimulatedFailure.valueOf(failStage.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(400, "INVALID_SIMULATOR", "失败阶段必须是 NONE、ASSET_CHECK、SIMULATE_GENERATE 或 RESULT_PACKAGING");
        }
    }

    public record Snapshot(long stepDelayMs, SimulatedFailure failStage) {
        public SimulatorView toView() {
            return new SimulatorView(stepDelayMs, failStage.name());
        }
    }
}
