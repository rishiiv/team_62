package com.team62.model;

/**
 * Employee model used by the manager UI.
 *
 * {@code employeeId} is a simple UI-friendly integer id.
 * {@code dbId} holds the real UUID primary key from the database.
 */
public class Employee {
    private int employeeId;
    private String dbId;
    private String name;
    private String role;
    private boolean active;

    public Employee() {
    }

    public Employee(int employeeId, String name, String role, boolean active) {
        this.employeeId = employeeId;
        this.name = name;
        this.role = role;
        this.active = active;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }
    
    public String getDbId() {
        return dbId;
    }
    
    public void setDbId(String dbId) {
        this.dbId = dbId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return name + " (" + role + ")" + (active ? "" : " - inactive");
    }
}

