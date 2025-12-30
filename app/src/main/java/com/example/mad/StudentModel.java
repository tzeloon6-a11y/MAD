package com.example.mad;

public class StudentModel {
    String name;
    String major;

    public StudentModel(String name, String major) {
        this.name = name;
        this.major = major;
    }

    public String getName() { return name; }
    public String getMajor() { return major; }
}
