package com.example.todolist.models;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TodoItem {
    private int id;
    private String task;
    private boolean isCompleted;
    private String createdAt;
    private String dueDate;
    private String category;
    private int priority; // 1: High, 2: Medium, 3: Low
    private String description;

    // Constructors
    public TodoItem() {
        this.createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        this.priority = 2; // Default medium priority
        this.category = "General";
    }

    public TodoItem(String task) {
        this();
        this.task = task;
    }

    public TodoItem(String task, String dueDate, String category, int priority, String description) {
        this();
        this.task = task;
        this.dueDate = dueDate;
        this.category = category;
        this.priority = priority;
        this.description = description;
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

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // Helper methods
    public String getPriorityText() {
        switch (priority) {
            case 1: return "High";
            case 2: return "Medium";
            case 3: return "Low";
            default: return "Medium";
        }
    }

    public int getPriorityColor() {
        switch (priority) {
            case 1: return 0xFFFF5252; // Red
            case 2: return 0xFFFF9800; // Orange
            case 3: return 0xFF4CAF50; // Green
            default: return 0xFF757575; // Gray
        }
    }

    public boolean hasDueDate() {
        return dueDate != null && !dueDate.isEmpty();
    }

    public boolean isOverdue() {
        if (!hasDueDate()) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date due = sdf.parse(dueDate);
            Date today = new Date();
            return due != null && due.before(today) && !isCompleted;
        } catch (Exception e) {
            return false;
        }
    }
}