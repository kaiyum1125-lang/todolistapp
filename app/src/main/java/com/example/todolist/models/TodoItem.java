package com.example.todolist.models;
public class TodoItem {
    private int id;
    private String task;
    private boolean isCompleted;
    private String createdAt;

    // Constructors
    public TodoItem() {}

    public TodoItem(String task) {
        this.task = task;
        this.isCompleted = false;
        this.createdAt = String.valueOf(System.currentTimeMillis());
    }

    public TodoItem(int id, String task, boolean isCompleted, String createdAt) {
        this.id = id;
        this.task = task;
        this.isCompleted = isCompleted;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTask() { return task; }
    public void setTask(String task) { this.task = task; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}