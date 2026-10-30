package com.gaotai.studio.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gaotai.studio.common.ApiException;
import com.gaotai.studio.config.StudioProperties;
import org.junit.jupiter.api.Test;

class SimulatorSettingsTest {

    @Test
    void rejectsOutOfRangeDelayAndUnknownStage() {
        SimulatorSettings settings = settings();
        assertEquals(400, assertThrows(ApiException.class, () -> settings.update(-1L, "NONE")).getStatus());
        assertEquals(400, assertThrows(ApiException.class, () -> settings.update(120_001L, "NONE")).getStatus());
        assertEquals(400, assertThrows(ApiException.class, () -> settings.update(10L, "BOOM")).getStatus());
        SimulatorView updated = settings.update(1500L, "asset_check");
        assertEquals(1500L, updated.stepDelayMs());
        assertEquals("ASSET_CHECK", updated.failStage());
    }

    private static SimulatorSettings settings() {
        StudioProperties properties = new StudioProperties();
        properties.getSimulator().setStepDelayMs(0);
        properties.getSimulator().setFailStage("NONE");
        return new SimulatorSettings(properties);
    }
}
