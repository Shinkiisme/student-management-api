package com.phuc.studentmanagement.model;

public class Course {
    private int id;
    private String code;
    private String name;
    private int credits;

    public Course(String code, String name, int credits) {
        this.code = code;
        this.name = name;
        this.credits = credits;
    }

    public Course(int id, String code, String name, int credits) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.credits = credits;
    }

    public int getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getCredits() {
        return credits;
    }
}