package com.phuc.studentmanagement.model;

public class Student {
    private int id;
    private String studentCode;
    private String name;
    private int age;

    public Student(String studentCode, String name, int age) {
        this.studentCode = studentCode;
        this.name = name;
        this.age = age;
    }

    public Student(int id, String studentCode, String name, int age) {
        this.id = id;
        this.studentCode = studentCode;
        this.name = name;
        this.age = age;
    }

    public int getId() {
        return id;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }
}