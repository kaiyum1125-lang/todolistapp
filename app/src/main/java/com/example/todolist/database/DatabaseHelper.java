package com.example.todolist.database;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.todolist.models.TodoItem;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "todo_enhanced.db";
    private static final int DATABASE_VERSION = 3; // Updated version

    private static final String TABLE_TODO = "todo_items";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TASK = "task";
    private static final String COLUMN_COMPLETED = "completed";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_DUE_DATE = "due_date";
    private static final String COLUMN_DUE_TIME = "due_time";
    private static final String COLUMN_HAS_REMINDER = "has_reminder";
    private static final String COLUMN_ALARM_TIME = "alarm_time";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_PRIORITY = "priority";
    private static final String COLUMN_DESCRIPTION = "description";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TODO_TABLE = "CREATE TABLE " + TABLE_TODO + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TASK + " TEXT,"
                + COLUMN_COMPLETED + " INTEGER,"
                + COLUMN_CREATED_AT + " TEXT,"
                + COLUMN_DUE_DATE + " TEXT,"
                + COLUMN_DUE_TIME + " TEXT,"
                + COLUMN_CATEGORY + " TEXT,"
                + COLUMN_PRIORITY + " INTEGER,"
                + COLUMN_DESCRIPTION + " TEXT,"
                + COLUMN_HAS_REMINDER + " INTEGER DEFAULT 0,"
                + COLUMN_ALARM_TIME + " INTEGER DEFAULT 0" + ")";
        db.execSQL(CREATE_TODO_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Upgrade from version 1: Drop and recreate
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_TODO);
            onCreate(db);
        } else if (oldVersion == 2) {
            // Upgrade from version 2 to 3: Add new columns for time and alarms
            db.execSQL("ALTER TABLE " + TABLE_TODO + " ADD COLUMN " + COLUMN_DUE_TIME + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_TODO + " ADD COLUMN " + COLUMN_HAS_REMINDER + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_TODO + " ADD COLUMN " + COLUMN_ALARM_TIME + " INTEGER DEFAULT 0");
        }
        // If you have future upgrades, add more conditions here
    }

    // Updated addTodoItem with all fields
    public void addTodoItem(TodoItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TASK, item.getTask());
        values.put(COLUMN_COMPLETED, item.isCompleted() ? 1 : 0);
        values.put(COLUMN_CREATED_AT, item.getCreatedAt());
        values.put(COLUMN_DUE_DATE, item.getDueDate());
        values.put(COLUMN_DUE_TIME, item.getDueTime()); // NEW: Due time
        values.put(COLUMN_CATEGORY, item.getCategory());
        values.put(COLUMN_PRIORITY, item.getPriority());
        values.put(COLUMN_DESCRIPTION, item.getDescription());
        values.put(COLUMN_HAS_REMINDER, item.hasReminder() ? 1 : 0); // NEW: Reminder flag
        values.put(COLUMN_ALARM_TIME, item.getAlarmTime()); // NEW: Alarm time

        db.insert(TABLE_TODO, null, values);
        db.close();
    }

    public List<TodoItem> getAllTodoItems() {
        return getTodoItemsWithQuery("SELECT * FROM " + TABLE_TODO + " ORDER BY " + COLUMN_PRIORITY + " ASC, " + COLUMN_DUE_DATE + " ASC, " + COLUMN_DUE_TIME + " ASC");
    }

    public List<TodoItem> getTodoItemsByCategory(String category) {
        return getTodoItemsWithQuery("SELECT * FROM " + TABLE_TODO + " WHERE " + COLUMN_CATEGORY + " = '" + category + "' ORDER BY " + COLUMN_PRIORITY + " ASC, " + COLUMN_DUE_DATE + " ASC");
    }

    public List<TodoItem> searchTodoItems(String query) {
        return getTodoItemsWithQuery("SELECT * FROM " + TABLE_TODO + " WHERE " + COLUMN_TASK + " LIKE '%" + query + "%' OR " + COLUMN_DESCRIPTION + " LIKE '%" + query + "%'");
    }

    public List<TodoItem> getCompletedItems() {
        return getTodoItemsWithQuery("SELECT * FROM " + TABLE_TODO + " WHERE " + COLUMN_COMPLETED + " = 1");
    }

    public List<TodoItem> getPendingItems() {
        return getTodoItemsWithQuery("SELECT * FROM " + TABLE_TODO + " WHERE " + COLUMN_COMPLETED + " = 0");
    }

    // NEW: Get tasks with active reminders
    public List<TodoItem> getTasksWithReminders() {
        return getTodoItemsWithQuery("SELECT * FROM " + TABLE_TODO + " WHERE " + COLUMN_HAS_REMINDER + " = 1 AND " + COLUMN_COMPLETED + " = 0");
    }

    // NEW: Get tasks due today
    public List<TodoItem> getTasksDueToday() {
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        return getTodoItemsWithQuery("SELECT * FROM " + TABLE_TODO + " WHERE " + COLUMN_DUE_DATE + " = '" + today + "' AND " + COLUMN_COMPLETED + " = 0");
    }

    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT " + COLUMN_CATEGORY + " FROM " + TABLE_TODO, null);

        if (cursor.moveToFirst()) {
            do {
                categories.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return categories;
    }

    public int getCompletedCount() {
        return getCountWithQuery("SELECT COUNT(*) FROM " + TABLE_TODO + " WHERE " + COLUMN_COMPLETED + " = 1");
    }

    public int getTotalCount() {
        return getCountWithQuery("SELECT COUNT(*) FROM " + TABLE_TODO);
    }

    public int getPendingCount() {
        return getCountWithQuery("SELECT COUNT(*) FROM " + TABLE_TODO + " WHERE " + COLUMN_COMPLETED + " = 0");
    }

    // NEW: Get count of tasks with reminders
    public int getReminderCount() {
        return getCountWithQuery("SELECT COUNT(*) FROM " + TABLE_TODO + " WHERE " + COLUMN_HAS_REMINDER + " = 1 AND " + COLUMN_COMPLETED + " = 0");
    }

    // NEW: Get count of tasks due today
    public int getDueTodayCount() {
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        return getCountWithQuery("SELECT COUNT(*) FROM " + TABLE_TODO + " WHERE " + COLUMN_DUE_DATE + " = '" + today + "' AND " + COLUMN_COMPLETED + " = 0");
    }

    public int getOverdueCount() {
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        return getCountWithQuery("SELECT COUNT(*) FROM " + TABLE_TODO + " WHERE " + COLUMN_COMPLETED + " = 0 AND " + COLUMN_DUE_DATE + " IS NOT NULL AND " + COLUMN_DUE_DATE + " < '" + today + "'");
    }

    // UPDATED: Complete cursor handling with all fields
    private List<TodoItem> getTodoItemsWithQuery(String query) {
        List<TodoItem> todoList = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                TodoItem item = new TodoItem();
                item.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                item.setTask(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TASK)));
                item.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COMPLETED)) == 1);
                item.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));

                // Handle due date (might be null)
                int dueDateIndex = cursor.getColumnIndex(COLUMN_DUE_DATE);
                if (!cursor.isNull(dueDateIndex)) {
                    item.setDueDate(cursor.getString(dueDateIndex));
                }

                // Handle due time (might be null)
                int dueTimeIndex = cursor.getColumnIndex(COLUMN_DUE_TIME);
                if (!cursor.isNull(dueTimeIndex)) {
                    item.setDueTime(cursor.getString(dueTimeIndex));
                }

                item.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)));
                item.setPriority(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PRIORITY)));

                // Handle description (might be null)
                int descIndex = cursor.getColumnIndex(COLUMN_DESCRIPTION);
                if (!cursor.isNull(descIndex)) {
                    item.setDescription(cursor.getString(descIndex));
                }

                // NEW: Handle reminder and alarm time
                int reminderIndex = cursor.getColumnIndex(COLUMN_HAS_REMINDER);
                if (!cursor.isNull(reminderIndex)) {
                    item.setHasReminder(cursor.getInt(reminderIndex) == 1);
                }

                int alarmTimeIndex = cursor.getColumnIndex(COLUMN_ALARM_TIME);
                if (!cursor.isNull(alarmTimeIndex)) {
                    item.setAlarmTime(cursor.getLong(alarmTimeIndex));
                }

                todoList.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return todoList;
    }

    private int getCountWithQuery(String query) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    // UPDATED: Complete update method with all fields
    public void updateTodoItem(TodoItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TASK, item.getTask());
        values.put(COLUMN_COMPLETED, item.isCompleted() ? 1 : 0);
        values.put(COLUMN_DUE_DATE, item.getDueDate());
        values.put(COLUMN_DUE_TIME, item.getDueTime()); // NEW: Due time
        values.put(COLUMN_CATEGORY, item.getCategory());
        values.put(COLUMN_PRIORITY, item.getPriority());
        values.put(COLUMN_DESCRIPTION, item.getDescription());
        values.put(COLUMN_HAS_REMINDER, item.hasReminder() ? 1 : 0); // NEW: Reminder flag
        values.put(COLUMN_ALARM_TIME, item.getAlarmTime()); // NEW: Alarm time

        db.update(TABLE_TODO, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(item.getId())});
        db.close();
    }

    // NEW: Update specific fields only (for performance)
    public void updateTodoItemCompletion(int id, boolean isCompleted) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_COMPLETED, isCompleted ? 1 : 0);
        db.update(TABLE_TODO, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // NEW: Update reminder status
    public void updateTodoItemReminder(int id, boolean hasReminder, long alarmTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_HAS_REMINDER, hasReminder ? 1 : 0);
        values.put(COLUMN_ALARM_TIME, alarmTime);
        db.update(TABLE_TODO, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteTodoItem(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TODO, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // NEW: Get single todo item by ID
    public TodoItem getTodoItemById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TODO, null, COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)}, null, null, null);

        TodoItem item = null;
        if (cursor != null && cursor.moveToFirst()) {
            item = new TodoItem();
            item.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            item.setTask(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TASK)));
            item.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COMPLETED)) == 1);
            item.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));

            int dueDateIndex = cursor.getColumnIndex(COLUMN_DUE_DATE);
            if (!cursor.isNull(dueDateIndex)) {
                item.setDueDate(cursor.getString(dueDateIndex));
            }

            int dueTimeIndex = cursor.getColumnIndex(COLUMN_DUE_TIME);
            if (!cursor.isNull(dueTimeIndex)) {
                item.setDueTime(cursor.getString(dueTimeIndex));
            }

            item.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)));
            item.setPriority(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PRIORITY)));

            int descIndex = cursor.getColumnIndex(COLUMN_DESCRIPTION);
            if (!cursor.isNull(descIndex)) {
                item.setDescription(cursor.getString(descIndex));
            }

            int reminderIndex = cursor.getColumnIndex(COLUMN_HAS_REMINDER);
            if (!cursor.isNull(reminderIndex)) {
                item.setHasReminder(cursor.getInt(reminderIndex) == 1);
            }

            int alarmTimeIndex = cursor.getColumnIndex(COLUMN_ALARM_TIME);
            if (!cursor.isNull(alarmTimeIndex)) {
                item.setAlarmTime(cursor.getLong(alarmTimeIndex));
            }
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return item;
    }

    // NEW: Clear all completed tasks
    public void clearCompletedTasks() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TODO, COLUMN_COMPLETED + " = ?", new String[]{"1"});
        db.close();
    }

    // NEW: Get tasks sorted by due date and time
    public List<TodoItem> getTasksSortedByDateTime() {
        return getTodoItemsWithQuery("SELECT * FROM " + TABLE_TODO + " WHERE " + COLUMN_COMPLETED + " = 0 ORDER BY " +
                COLUMN_DUE_DATE + " ASC, " + COLUMN_DUE_TIME + " ASC, " + COLUMN_PRIORITY + " ASC");
    }

    // NEW: Get upcoming tasks (next 7 days)
    public List<TodoItem> getUpcomingTasks() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(calendar.getTime());

        calendar.add(java.util.Calendar.DAY_OF_YEAR, 7);
        String nextWeek = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(calendar.getTime());

        return getTodoItemsWithQuery("SELECT * FROM " + TABLE_TODO + " WHERE " + COLUMN_COMPLETED + " = 0 AND " +
                COLUMN_DUE_DATE + " BETWEEN '" + today + "' AND '" + nextWeek + "' ORDER BY " +
                COLUMN_DUE_DATE + " ASC, " + COLUMN_DUE_TIME + " ASC");
    }
}