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
    private static final String DATABASE_NAME = "todo.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_TODO = "todo_items";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TASK = "task";
    private static final String COLUMN_COMPLETED = "completed";
    private static final String COLUMN_CREATED_AT = "created_at";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TODO_TABLE = "CREATE TABLE " + TABLE_TODO + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TASK + " TEXT,"
                + COLUMN_COMPLETED + " INTEGER,"
                + COLUMN_CREATED_AT + " TEXT" + ")";
        db.execSQL(CREATE_TODO_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TODO);
        onCreate(db);
    }

    // Add new todo item
    public void addTodoItem(TodoItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TASK, item.getTask());
        values.put(COLUMN_COMPLETED, item.isCompleted() ? 1 : 0);
        values.put(COLUMN_CREATED_AT, item.getCreatedAt());

        db.insert(TABLE_TODO, null, values);
        db.close();
    }

    // Get all todo items
    public List<TodoItem> getAllTodoItems() {
        List<TodoItem> todoList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TODO + " ORDER BY " + COLUMN_CREATED_AT + " DESC";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                TodoItem item = new TodoItem();
                item.setId(cursor.getInt(0));
                item.setTask(cursor.getString(1));
                item.setCompleted(cursor.getInt(2) == 1);
                item.setCreatedAt(cursor.getString(3));
                todoList.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return todoList;
    }

    // Update todo item
    public void updateTodoItem(TodoItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TASK, item.getTask());
        values.put(COLUMN_COMPLETED, item.isCompleted() ? 1 : 0);

        db.update(TABLE_TODO, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(item.getId())});
        db.close();
    }

    // Delete todo item
    public void deleteTodoItem(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TODO, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}