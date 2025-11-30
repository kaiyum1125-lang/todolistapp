package com.example.todolist.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.todolist.database.DatabaseHelper;
import com.example.todolist.models.TodoItem;
import com.example.todolist.utils.AlarmHelper;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            restoreAlarms(context);
        }
    }

    private void restoreAlarms(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Query all tasks with reminders that are not completed
        String query = "SELECT * FROM todo_items WHERE completed = 0 AND due_date IS NOT NULL AND due_time IS NOT NULL";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                TodoItem item = new TodoItem();
                item.setId(cursor.getInt(0));
                item.setTask(cursor.getString(1));
                item.setCompleted(cursor.getInt(2) == 1);
                item.setCreatedAt(cursor.getString(3));
                item.setDueDate(cursor.getString(4));
                item.setDueTime(cursor.getString(5)); // Assuming this column exists
                item.setCategory(cursor.getString(6));
                item.setPriority(cursor.getInt(7));
                item.setDescription(cursor.getString(8));
                item.setHasReminder(true); // Assume all have reminders for restoration

                // Only restore if the task is not overdue
                if (!item.isOverdue()) {
                    AlarmHelper.setAlarm(context, item);
                }

            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        dbHelper.close();
    }
}