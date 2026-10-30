package com.gaotai.studio.asset;

public interface UploadStore {

    boolean exists(String storageName);

    void write(String storageName, byte[] bytes);

    void delete(String storageName);
}
