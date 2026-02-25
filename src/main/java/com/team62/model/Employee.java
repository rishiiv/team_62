package com.team62.model;

/**
 * Simple in-memory representation of an employee for manager demos.
 */
public class Employee {
    private int employeeId;
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

