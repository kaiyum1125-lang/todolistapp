package com.example.todolist.models;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TodoItem {
    private int id;
    private String task;
    private boolean isCompleted;
    private String createdAt;
    private String dueDate;
    private String dueTime;
    private String category;
    private int priority;
    private String description;
    private boolean hasReminder;
    private long alarmTime; // NEW: Store alarm time in milliseconds

    // Constructors
    public TodoItem() {
        this.createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        this.priority = 2;
        this.category = "General";
        this.hasReminder = false;
        this.alarmTime = 0;
    }

    public TodoItem(String task) {
        this();
        this.task = task;
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

    public String getDueTime() { return dueTime; }
    public void setDueTime(String dueTime) { this.dueTime = dueTime; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean hasReminder() { return hasReminder; }
    public void setHasReminder(boolean hasReminder) { this.hasReminder = hasReminder; }

    // NEW: Alarm time getter and setter
    public long getAlarmTime() { return alarmTime; }
    public void setAlarmTime(long alarmTime) { this.alarmTime = alarmTime; }

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
            case 1: return 0xFFFF5252;
            case 2: return 0xFFFF9800;
            case 3: return 0xFF4CAF50;
            default: return 0xFF757575;
        }
    }

    public boolean hasDueDate() {
        return dueDate != null && !dueDate.isEmpty();
    }

    public boolean hasDueTime() {
        return dueTime != null && !dueTime.isEmpty();
    }

    public String getFullDueDateTime() {
        if (hasDueDate() && hasDueTime()) {
            return dueDate + " " + dueTime;
        }
        return dueDate;
    }

    public boolean isOverdue() {
        if (!hasDueDate()) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String dateTimeString = hasDueTime() ? dueDate + " " + dueTime : dueDate + " 23:59";
            Date due = sdf.parse(dateTimeString);
            Date now = new Date();
            return due != null && due.before(now) && !isCompleted;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isDueToday() {
        if (!hasDueDate()) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = sdf.format(new Date());
            return dueDate.equals(today);
        } catch (Exception e) {
            return false;
        }
    }

    // NEW: Calculate alarm time from due date and time
//    public long calculateAlarmTime() {
//        if (!hasDueDate() || !hasDueTime()) return 0;
//
//        try {
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
//            String dateTimeString = dueDate + " " + dueTime;
//            Date dueDateTime = sdf.parse(dateTimeString);
//
//            if (dueDateTime != null) {
//                // Set alarm 1 minute before due time for testing
//                // Change to 5 minutes for production: (5 * 60 * 1000)
//                long alarmTime = dueDateTime.getTime() - (1 * 60 * 1000); // 1 minute before
//
//                // Check if alarm is in the future
//                if (alarmTime > System.currentTimeMillis()) {
//                    return alarmTime;
//                } else {
//                    Log.w("TodoItem", "Alarm time is in the past: " + new Date(alarmTime));
//                    return 0;
//                }
//            }
//        } catch (Exception e) {
//            Log.e("TodoItem", "Error calculating alarm time: " + e.getMessage());
//            e.printStackTrace();
//        }
//        return 0;
//    }


    public long calculateAlarmTime() {
        if (!hasDueDate() || !hasDueTime()) {
            Log.d("TodoItem", "Missing date or time - Date: " + dueDate + ", Time: " + dueTime);
            return 0;
        }

        try {
            // Parse the date and time
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String dateTimeString = dueDate + " " + dueTime;
            Date dueDateTime = sdf.parse(dateTimeString);

            if (dueDateTime == null) {
                Log.e("TodoItem", "Failed to parse date/time: " + dateTimeString);
                return 0;
            }

            // Set alarm 1 minute before due time (for testing)
            long alarmTime = dueDateTime.getTime() - (1 * 60 * 1000);

            // For production, use 5 minutes before:
            // long alarmTime = dueDateTime.getTime() - (5 * 60 * 1000);

            long currentTime = System.currentTimeMillis();

            Log.d("TodoItem", "Current time: " + new Date(currentTime));
            Log.d("TodoItem", "Due time: " + dueDateTime);
            Log.d("TodoItem", "Alarm time: " + new Date(alarmTime));
            Log.d("TodoItem", "Time difference: " + (alarmTime - currentTime) + "ms");

            // Check if alarm is in the future
            if (alarmTime > currentTime) {
                return alarmTime;
            } else {
                Log.w("TodoItem", "Alarm time is in the past: " + new Date(alarmTime));
                // REMOVED: Toast.makeText(context, "Cannot set alarm for past time", Toast.LENGTH_SHORT).show();
                return 0;
            }

        } catch (ParseException e) {
            Log.e("TodoItem", "Parse error for date/time: " + dueDate + " " + dueTime + " - " + e.getMessage());
            return 0;
        } catch (Exception e) {
            Log.e("TodoItem", "Unexpected error: " + e.getMessage());
            return 0;
        }
    }
}