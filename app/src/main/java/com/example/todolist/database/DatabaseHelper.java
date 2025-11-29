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
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_TODO = "todo_items";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TASK = "task";
    private static final String COLUMN_COMPLETED = "completed";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_DUE_DATE = "due_date";
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
                + COLUMN_CATEGORY + " TEXT,"
                + COLUMN_PRIORITY + " INTEGER,"
                + COLUMN_DESCRIPTION + " TEXT" + ")";
        db.execSQL(CREATE_TODO_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_TODO);
            onCreate(db);
        }
    }

    public void addTodoItem(TodoItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TASK, item.getTask());
        values.put(COLUMN_COMPLETED, item.isCompleted() ? 1 : 0);
        values.put(COLUMN_CREATED_AT, item.getCreatedAt());
        values.put(COLUMN_DUE_DATE, item.getDueDate());
        values.put(COLUMN_CATEGORY, item.getCategory());
        values.put(COLUMN_PRIORITY, item.getPriority());
        values.put(COLUMN_DESCRIPTION, item.getDescription());

        db.insert(TABLE_TODO, null, values);
        db.close();
    }

    public List<TodoItem> getAllTodoItems() {
        return getTodoItemsWithQuery("SELECT * FROM " + TABLE_TODO + " ORDER BY " + COLUMN_PRIORITY + " ASC, " + COLUMN_DUE_DATE + " ASC");
    }

    public List<TodoItem> getTodoItemsByCategory(String category) {
        return getTodoItemsWithQuery("SELECT * FROM " + TABLE_TODO + " WHERE " + COLUMN_CATEGORY + " = '" + category + "' ORDER BY " + COLUMN_PRIORITY + " ASC");
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

    public int getOverdueCount() {
        // This would need a more complex query for actual overdue calculation
        return getCountWithQuery("SELECT COUNT(*) FROM " + TABLE_TODO + " WHERE " + COLUMN_COMPLETED + " = 0 AND " + COLUMN_DUE_DATE + " IS NOT NULL");
    }

    private List<TodoItem> getTodoItemsWithQuery(String query) {
        List<TodoItem> todoList = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                TodoItem item = new TodoItem();
                item.setId(cursor.getInt(0));
                item.setTask(cursor.getString(1));
                item.setCompleted(cursor.getInt(2) == 1);
                item.setCreatedAt(cursor.getString(3));
                item.setDueDate(cursor.getString(4));
                item.setCategory(cursor.getString(5));
                item.setPriority(cursor.getInt(6));
                item.setDescription(cursor.getString(7));
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

    // Update and Delete methods remain the same but with new fields
    public void updateTodoItem(TodoItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TASK, item.getTask());
        values.put(COLUMN_COMPLETED, item.isCompleted() ? 1 : 0);
        values.put(COLUMN_DUE_DATE, item.getDueDate());
        values.put(COLUMN_CATEGORY, item.getCategory());
        values.put(COLUMN_PRIORITY, item.getPriority());
        values.put(COLUMN_DESCRIPTION, item.getDescription());

        db.update(TABLE_TODO, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(item.getId())});
        db.close();
    }

    public void deleteTodoItem(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TODO, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}