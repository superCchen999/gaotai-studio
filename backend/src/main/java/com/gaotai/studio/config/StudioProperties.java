package com.gaotai.studio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "studio")
public class StudioProperties {

    private final Worker worker = new Worker();
    private final Storage storage = new Storage();
    private final Simulator simulator = new Simulator();

    public Worker getWorker() {
        return worker;
    }

    public Storage getStorage() {
        return storage;
    }

    public Simulator getSimulator() {
        return simulator;
    }

    public static class Worker {
        private boolean enabled = true;
        private long idleWaitMs = 400;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getIdleWaitMs() {
            return idleWaitMs;
        }

        public void setIdleWaitMs(long idleWaitMs) {
            this.idleWaitMs = idleWaitMs;
        }
    }

    public static class Storage {
        private String root = "./data";

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }
    }

    public static class Simulator {
        private boolean apiEnabled = true;
        private long stepDelayMs = 2000;
        private String failStage = "NONE";

        public boolean isApiEnabled() {
            return apiEnabled;
        }

        public void setApiEnabled(boolean apiEnabled) {
            this.apiEnabled = apiEnabled;
        }

        public long getStepDelayMs() {
            return stepDelayMs;
        }

        public void setStepDelayMs(long stepDelayMs) {
            this.stepDelayMs = stepDelayMs;
        }

        public String getFailStage() {
            return failStage;
        }

        public void setFailStage(String failStage) {
            this.failStage = failStage;
        }
    }
}
