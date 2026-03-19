package com.huashi.eftransfer.shared.enums;

public enum UserRole {
    STUDENT("student", "Student"),
    TEACHER("teacher", "Teacher"),
    ADMIN("admin", "Administrator");

    private final String code;
    private final String label;

    UserRole(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }
}
