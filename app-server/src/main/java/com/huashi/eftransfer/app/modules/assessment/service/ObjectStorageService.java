package com.huashi.eftransfer.app.modules.assessment.service;

import java.io.InputStream;

public interface ObjectStorageService {
    String put(String objectKey, InputStream content, long sizeBytes, String contentType);

    InputStream open(String objectKey);

    void delete(String objectKey);
}
