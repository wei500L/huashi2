package com.huashi.eftransfer.shared.enums;

import java.util.Arrays;

public enum AssessmentFileStorageProvider {
    LOCAL("LOCAL", "Local filesystem"),
    S3("S3", "Amazon S3"),
    OSS("OSS", "Aliyun OSS"),
    MINIO("MINIO", "MinIO");

    private final String code;
    private final String label;

    AssessmentFileStorageProvider(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static AssessmentFileStorageProvider fromCode(String value) {
        String normalized = normalize(value);
        return Arrays.stream(values())
                .filter(item -> normalize(item.code).equals(normalized)
                        || normalize(item.name()).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported assessmentFileStorageProvider: " + value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }
}
