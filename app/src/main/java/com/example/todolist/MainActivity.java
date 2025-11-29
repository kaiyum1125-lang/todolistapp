package com.example.todolist;


import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.adapter.TodoAdapter;
import com.example.todolist.database.DatabaseHelper;

import com.example.todolist.models.TodoItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TodoAdapter.OnItemClickListener {

    private EditText taskInput;
    private Button addButton;
    private RecyclerView todoRecyclerView;
    private TodoAdapter adapter;
    private DatabaseHelper databaseHelper;
    private List<TodoItem> todoList;
    private SearchView searchView;
    private TextView statsText;
    private FloatingActionButton fabAddTask;

    // Filter states
    private String currentFilter = "ALL"; // ALL, COMPLETED, PENDING, CATEGORY
    private String currentCategoryFilter = "";
    private String currentSort = "PRIORITY"; // PRIORITY, DATE, NAME

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_enhanced);

        initializeViews();
        setupRecyclerView();
        setupSwipeToDelete();
        setupClickListeners();
        loadTodoItems();
        updateStats();
    }

    private void initializeViews() {
        taskInput = findViewById(R.id.task_input);
        addButton = findViewById(R.id.add_button);
        todoRecyclerView = findViewById(R.id.todo_recycler_view);
        searchView = findViewById(R.id.search_view);
        statsText = findViewById(R.id.stats_text);
        fabAddTask = findViewById(R.id.fab_add_task);
        databaseHelper = new DatabaseHelper(this);
        todoList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        todoRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TodoAdapter(todoList, this);
        todoRecyclerView.setAdapter(adapter);
    }

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                TodoItem item = todoList.get(position);

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Task")
                        .setMessage("Delete \"" + item.getTask() + "\"?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            databaseHelper.deleteTodoItem(item.getId());
                            loadTodoItems();
                            Toast.makeText(MainActivity.this, "Task deleted", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            loadTodoItems(); // Reload to reset swipe
                        })
                        .setOnCancelListener(dialog -> loadTodoItems())
                        .show();
            }
        }).attachToRecyclerView(todoRecyclerView);
    }

    private void setupClickListeners() {
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewTask();
            }
        });

        fabAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTaskDialog();
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    loadTodoItems();
                } else {
                    List<TodoItem> searchResults = databaseHelper.searchTodoItems(newText);
                    adapter.updateList(searchResults);
                }
                return true;
            }
        });
    }

    private void addNewTask() {
        String task = taskInput.getText().toString().trim();

        if (TextUtils.isEmpty(task)) {
            Toast.makeText(this, "Please enter a task", Toast.LENGTH_SHORT).show();
            return;
        }

        TodoItem newItem = new TodoItem(task);
        databaseHelper.addTodoItem(newItem);
        taskInput.setText("");

        loadTodoItems();
        updateStats();
        Toast.makeText(this, "Task added", Toast.LENGTH_SHORT).show();
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        EditText taskInput = dialogView.findViewById(R.id.dialog_task_input);
        EditText descriptionInput = dialogView.findViewById(R.id.dialog_description_input);
        Spinner categorySpinner = dialogView.findViewById(R.id.dialog_category_spinner);
        Spinner prioritySpinner = dialogView.findViewById(R.id.dialog_priority_spinner);
        TextView dueDateText = dialogView.findViewById(R.id.dialog_due_date_text);
        Button setDueDateButton = dialogView.findViewById(R.id.dialog_set_due_date);

        // Setup category spinner
        List<String> categories = databaseHelper.getAllCategories();
        categories.add(0, "General");
        categories.add("Work");
        categories.add("Personal");
        categories.add("Shopping");
        categories.add("Health");

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        // Setup priority spinner
        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(this,
                R.array.priority_levels, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);

        final String[] selectedDueDate = {""};

        setDueDateButton.setOnClickListener(v -> showDatePickerDialog(dueDateText, selectedDueDate));

        builder.setTitle("Add New Task")
                .setPositiveButton("Add", (dialog, which) -> {
                    String task = taskInput.getText().toString().trim();
                    if (!TextUtils.isEmpty(task)) {
                        TodoItem newItem = new TodoItem();
                        newItem.setTask(task);
                        newItem.setDescription(descriptionInput.getText().toString());
                        newItem.setCategory(categorySpinner.getSelectedItem().toString());
                        newItem.setPriority(prioritySpinner.getSelectedItemPosition() + 1);
                        newItem.setDueDate(selectedDueDate[0]);

                        databaseHelper.addTodoItem(newItem);
                        loadTodoItems();
                        updateStats();
                        Toast.makeText(MainActivity.this, "Task added", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null);

        builder.create().show();
    }

    private void showDatePickerDialog(TextView dueDateText, String[] selectedDueDate) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String dueDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    selectedDueDate[0] = dueDate;
                    dueDateText.setText("Due: " + dueDate);
                    dueDateText.setVisibility(View.VISIBLE);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    private void loadTodoItems() {
        List<TodoItem> newList;

        switch (currentFilter) {
            case "COMPLETED":
                newList = databaseHelper.getCompletedItems();
                break;
            case "PENDING":
                newList = databaseHelper.getPendingItems();
                break;
            case "CATEGORY":
                newList = databaseHelper.getTodoItemsByCategory(currentCategoryFilter);
                break;
            default:
                newList = databaseHelper.getAllTodoItems();
        }

        // Apply sorting
        switch (currentSort) {
            case "DATE":
                // Sort by due date (items without due date go last)
                newList.sort((item1, item2) -> {
                    if (item1.hasDueDate() && item2.hasDueDate()) {
                        return item1.getDueDate().compareTo(item2.getDueDate());
                    } else if (item1.hasDueDate()) {
                        return -1;
                    } else if (item2.hasDueDate()) {
                        return 1;
                    }
                    return 0;
                });
                break;
            case "NAME":
                newList.sort((item1, item2) -> item1.getTask().compareToIgnoreCase(item2.getTask()));
                break;
            case "PRIORITY":
            default:
                // Already sorted by priority from database
                break;
        }

        adapter.updateList(newList);
        updateStats();
    }

    private void updateStats() {
        int total = databaseHelper.getTotalCount();
        int completed = databaseHelper.getCompletedCount();
        int pending = total - completed;

        String stats = String.format(Locale.getDefault(),
                "Total: %d • Completed: %d • Pending: %d", total, completed, pending);
        statsText.setText(stats);
    }

    @Override
    public void onItemClick(int position) {
        TodoItem item = todoList.get(position);
        showEditDialog(item);
    }

    @Override
    public void onCheckBoxClick(int position, boolean isChecked) {
        todoRecyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                TodoItem item = todoList.get(position);
                item.setCompleted(isChecked);
                databaseHelper.updateTodoItem(item);

                adapter.updateItem(position, item);
                updateStats();

                String message = isChecked ? "Task completed! ✅" : "Task marked incomplete";
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        }, 100);
    }

    @Override
    public void onItemLongClick(int position) {
        showTaskDetailsDialog(position);
    }

    private void showEditDialog(final TodoItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        EditText taskInput = dialogView.findViewById(R.id.dialog_task_input);
        EditText descriptionInput = dialogView.findViewById(R.id.dialog_description_input);
        Spinner categorySpinner = dialogView.findViewById(R.id.dialog_category_spinner);
        Spinner prioritySpinner = dialogView.findViewById(R.id.dialog_priority_spinner);
        TextView dueDateText = dialogView.findViewById(R.id.dialog_due_date_text);
        Button setDueDateButton = dialogView.findViewById(R.id.dialog_set_due_date);

        // Populate fields
        taskInput.setText(item.getTask());
        descriptionInput.setText(item.getDescription());

        // Setup category spinner
        List<String> categories = databaseHelper.getAllCategories();
        categories.add(0, "General");
        categories.add("Work");
        categories.add("Personal");
        categories.add("Shopping");
        categories.add("Health");

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        // Set selected category
        int categoryPosition = categories.indexOf(item.getCategory());
        if (categoryPosition != -1) {
            categorySpinner.setSelection(categoryPosition);
        }

        // Setup priority spinner
        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(this,
                R.array.priority_levels, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);
        prioritySpinner.setSelection(item.getPriority() - 1);

        // Setup due date
        final String[] selectedDueDate = {item.getDueDate()};
        if (item.hasDueDate()) {
            dueDateText.setText("Due: " + item.getDueDate());
            dueDateText.setVisibility(View.VISIBLE);
        }

        setDueDateButton.setOnClickListener(v -> showDatePickerDialog(dueDateText, selectedDueDate));

        builder.setTitle("Edit Task")
                .setPositiveButton("Save", (dialog, which) -> {
                    String task = taskInput.getText().toString().trim();
                    if (!TextUtils.isEmpty(task)) {
                        item.setTask(task);
                        item.setDescription(descriptionInput.getText().toString());
                        item.setCategory(categorySpinner.getSelectedItem().toString());
                        item.setPriority(prioritySpinner.getSelectedItemPosition() + 1);
                        item.setDueDate(selectedDueDate[0]);

                        databaseHelper.updateTodoItem(item);
                        loadTodoItems();
                        Toast.makeText(MainActivity.this, "Task updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null);

        builder.create().show();
    }

    private void showTaskDetailsDialog(int position) {
        TodoItem item = todoList.get(position);

        String details = "Task: " + item.getTask() + "\n\n" +
                "Description: " + (item.getDescription() != null ? item.getDescription() : "No description") + "\n\n" +
                "Category: " + item.getCategory() + "\n" +
                "Priority: " + item.getPriorityText() + "\n" +
                "Status: " + (item.isCompleted() ? "Completed" : "Pending") + "\n" +
                "Due Date: " + (item.hasDueDate() ? item.getDueDate() : "Not set") + "\n" +
                "Created: " + item.getCreatedAt();

        new AlertDialog.Builder(this)
                .setTitle("Task Details")
                .setMessage(details)
                .setPositiveButton("Edit", (dialog, which) -> showEditDialog(item))
                .setNegativeButton("Delete", (dialog, which) -> showDeleteDialog(position))
                .setNeutralButton("Close", null)
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
                    updateStats();
                    Toast.makeText(MainActivity.this, "Task deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_filter_all) {
            currentFilter = "ALL";
            loadTodoItems();
            return true;
        } else if (id == R.id.menu_filter_completed) {
            currentFilter = "COMPLETED";
            loadTodoItems();
            return true;
        } else if (id == R.id.menu_filter_pending) {
            currentFilter = "PENDING";
            loadTodoItems();
            return true;
        } else if (id == R.id.menu_sort_priority) {
            currentSort = "PRIORITY";
            loadTodoItems();
            return true;
        } else if (id == R.id.menu_sort_date) {
            currentSort = "DATE";
            loadTodoItems();
            return true;
        } else if (id == R.id.menu_sort_name) {
            currentSort = "NAME";
            loadTodoItems();
            return true;
        } else if (id == R.id.menu_filter_category) {
            showCategoryFilterDialog();
            return true;
        } else if (id == R.id.menu_clear_completed) {
            clearCompletedTasks();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showCategoryFilterDialog() {
        List<String> categories = databaseHelper.getAllCategories();
        categories.add(0, "All Categories");

        String[] categoryArray = categories.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Filter by Category")
                .setItems(categoryArray, (dialog, which) -> {
                    if (which == 0) {
                        currentFilter = "ALL";
                        currentCategoryFilter = "";
                    } else {
                        currentFilter = "CATEGORY";
                        currentCategoryFilter = categories.get(which);
                    }
                    loadTodoItems();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearCompletedTasks() {
        List<TodoItem> completedItems = databaseHelper.getCompletedItems();
        if (completedItems.isEmpty()) {
            Toast.makeText(this, "No completed tasks to clear", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Clear Completed Tasks")
                .setMessage("Delete all " + completedItems.size() + " completed tasks?")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    for (TodoItem item : completedItems) {
                        databaseHelper.deleteTodoItem(item.getId());
                    }
                    loadTodoItems();
                    updateStats();
                    Toast.makeText(MainActivity.this, "Completed tasks cleared", Toast.LENGTH_SHORT).show();
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