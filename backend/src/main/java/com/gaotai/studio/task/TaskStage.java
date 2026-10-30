package com.gaotai.studio.task;

import java.util.List;

public enum TaskStage {
    ASSET_CHECK,
    SIMULATE_GENERATE,
    RESULT_PACKAGING;

    public static List<TaskStage> ordered() {
        return List.of(ASSET_CHECK, SIMULATE_GENERATE, RESULT_PACKAGING);
    }
}
