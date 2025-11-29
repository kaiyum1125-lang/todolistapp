package com.example.todolist;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.adapter.TodoAdapter;
import com.example.todolist.database.DatabaseHelper;
import com.example.todolist.models.TodoItem;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TodoAdapter.OnItemClickListener {

    private EditText taskInput;
    private Button addButton;
    private RecyclerView todoRecyclerView;
    private TodoAdapter adapter;
    private DatabaseHelper databaseHelper;
    private List<TodoItem> todoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadTodoItems();
    }

    private void initializeViews() {
        taskInput = findViewById(R.id.task_input);
        addButton = findViewById(R.id.add_button);
        todoRecyclerView = findViewById(R.id.todo_recycler_view);
        databaseHelper = new DatabaseHelper(this);
        todoList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        todoRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TodoAdapter(todoList, this);
        todoRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewTask();
            }
        });
    }

    private void addNewTask() {
        String task = taskInput.getText().toString().trim();

        System.out.println("----------------------------------------------------" + task);
        if (TextUtils.isEmpty(task)) {
            Toast.makeText(this, "Please enter a task", Toast.LENGTH_SHORT).show();
            return;
        }

        TodoItem newItem = new TodoItem(task);
        databaseHelper.addTodoItem(newItem);
        taskInput.setText("");
        loadTodoItems();
        Toast.makeText(this, "Task added", Toast.LENGTH_SHORT).show();
    }

    private void loadTodoItems() {
        todoList.clear();
        todoList.addAll(databaseHelper.getAllTodoItems());
        adapter.updateList(todoList);
    }

    @Override
    public void onItemClick(int position) {
        // Handle item click for editing
        TodoItem item = todoList.get(position);
        showEditDialog(item);
    }

    @Override
    public void onCheckBoxClick(int position, boolean isChecked) {
        TodoItem item = todoList.get(position);
        item.setCompleted(isChecked);
        databaseHelper.updateTodoItem(item);
        loadTodoItems();

        String message = isChecked ? "Task completed!" : "Task marked incomplete";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemLongClick(int position) {
        showDeleteDialog(position);
    }

    private void showEditDialog(final TodoItem item) {
        final EditText input = new EditText(this);
        input.setText(item.getTask());

        new AlertDialog.Builder(this)
                .setTitle("Edit Task")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newTask = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(newTask)) {
                        item.setTask(newTask);
                        databaseHelper.updateTodoItem(item);
                        loadTodoItems();
                        Toast.makeText(MainActivity.this, "Task updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteDialog(final int position) {
        TodoItem item = todoList.get(position);

        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete: \"" + item.getTask() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    databaseHelper.deleteTodoItem(item.getId());
                    loadTodoItems();
                    Toast.makeText(MainActivity.this, "Task deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        databaseHelper.close();
        super.onDestroy();
    }
}