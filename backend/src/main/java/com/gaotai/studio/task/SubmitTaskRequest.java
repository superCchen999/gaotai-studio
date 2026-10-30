package com.gaotai.studio.task;

import java.util.List;

public record SubmitTaskRequest(String templateId, List<String> assetIds) {
}
