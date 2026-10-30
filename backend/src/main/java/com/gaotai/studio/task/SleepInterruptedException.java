package com.gaotai.studio.task;

public class SleepInterruptedException extends RuntimeException {

    public SleepInterruptedException() {
        super("步骤等待被中断");
    }
}
