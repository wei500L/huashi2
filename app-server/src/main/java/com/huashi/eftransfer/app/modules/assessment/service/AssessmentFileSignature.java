package com.huashi.eftransfer.app.modules.assessment.service;

import java.util.Locale;
import java.util.Set;

public final class AssessmentFileSignature {

    public static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "xlsx", "csv", "txt", "png", "jpg", "jpeg");

    private AssessmentFileSignature() {
    }

    public static String normalizeExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isAllowedExtension(String extension) {
        return ALLOWED_EXTENSIONS.contains(extension == null ? "" : extension.toLowerCase(Locale.ROOT));
    }

    public static boolean isExecutableOrScript(byte[] header) {
        if (header == null || header.length < 2) {
            return false;
        }
        return header[0] == 'M' && header[1] == 'Z'
                || header[0] == 0x7F && header.length > 3 && header[1] == 'E' && header[2] == 'L' && header[3] == 'F'
                || header[0] == '#' && header[1] == '!';
    }

    public static String detectMime(String extension, byte[] header, String declaredType) {
        String ext = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "pdf" -> matches(header, new byte[]{0x25, 0x50, 0x44, 0x46}) ? "application/pdf" : null;
            case "png" -> matches(header, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}) ? "image/png" : null;
            case "jpg", "jpeg" -> matches(header, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}) ? "image/jpeg" : null;
            case "docx", "xlsx" -> isZip(header)
                    ? ("docx".equals(ext)
                    ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    : null;
            case "csv" -> "text/csv";
            case "txt" -> "text/plain";
            default -> declaredType;
        };
    }

    private static boolean isZip(byte[] header) {
        return matches(header, new byte[]{0x50, 0x4B, 0x03, 0x04})
                || matches(header, new byte[]{0x50, 0x4B, 0x05, 0x06})
                || matches(header, new byte[]{0x50, 0x4B, 0x07, 0x08});
    }

    private static boolean matches(byte[] header, byte[] magic) {
        if (header == null || header.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (header[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }
}
