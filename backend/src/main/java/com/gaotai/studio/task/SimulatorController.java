package com.gaotai.studio.task;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/simulator")
@ConditionalOnProperty(prefix = "studio.simulator", name = "api-enabled", havingValue = "true", matchIfMissing = true)
public class SimulatorController {

    private final SimulatorSettings simulatorSettings;

    public SimulatorController(SimulatorSettings simulatorSettings) {
        this.simulatorSettings = simulatorSettings;
    }

    @GetMapping
    public SimulatorView get() {
        return simulatorSettings.view();
    }

    @PutMapping
    public SimulatorView update(@RequestBody SimulatorUpdate request) {
        if (request == null) {
            return simulatorSettings.update(null, null);
        }
        return simulatorSettings.update(request.stepDelayMs(), request.failStage());
    }

    public record SimulatorUpdate(Long stepDelayMs, String failStage) {
    }
}
