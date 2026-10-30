package com.gaotai.studio.task;

public class StageFailure extends RuntimeException {

    public StageFailure(String message) {
        super(message);
    }
}
