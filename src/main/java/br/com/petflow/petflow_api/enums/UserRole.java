package br.com.petflow.petflow_api.enums;

public enum UserRole {
    ADMIN("admin"),
    TUTOR("tutor");

    private String role;

    UserRole(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }
}