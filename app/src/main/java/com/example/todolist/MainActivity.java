package com.example.todolist;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.example.todolist.utils.AlarmHelper;
import com.example.todolist.utils.PermissionHelper;
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

        // Check alarm permission for Android 12+
        checkAlarmPermission();
    }

    private void checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!PermissionHelper.canScheduleExactAlarms(this)) {
                // Show info about exact alarms (optional)
                Toast.makeText(this,
                        "For best results, enable 'Alarms & reminders' in app settings",
                        Toast.LENGTH_LONG).show();
            }
        }
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
                            // Cancel alarm before deleting
                            if (item.hasReminder()) {
                                AlarmHelper.cancelAlarm(MainActivity.this, item.getId());
                            }
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
        TextView dueTimeText = dialogView.findViewById(R.id.dialog_due_time_text);
        Button setDueDateButton = dialogView.findViewById(R.id.dialog_set_due_date);
        Button setDueTimeButton = dialogView.findViewById(R.id.dialog_set_due_time);
        CheckBox reminderCheckbox = dialogView.findViewById(R.id.dialog_reminder_checkbox);

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
        final String[] selectedDueTime = {""};
        final boolean[] hasReminder = {false};

        setDueDateButton.setOnClickListener(v -> showDatePickerDialog(dueDateText, selectedDueDate));

        setDueTimeButton.setOnClickListener(v -> showTimePickerDialog(dueTimeText, selectedDueTime));

        reminderCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hasReminder[0] = isChecked;
            // Show warning if time is not set but reminder is checked
            if (isChecked && selectedDueTime[0].isEmpty()) {
                Toast.makeText(MainActivity.this, "Please set a time for the reminder", Toast.LENGTH_SHORT).show();
            }
        });

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
                        newItem.setDueTime(selectedDueTime[0]);
                        newItem.setHasReminder(hasReminder[0]);

                        // Calculate and set alarm time if reminder is enabled
                        if (hasReminder[0] && !selectedDueDate[0].isEmpty() && !selectedDueTime[0].isEmpty()) {
                            long alarmTime = newItem.calculateAlarmTime();
                            newItem.setAlarmTime(alarmTime);
                        }

                        databaseHelper.addTodoItem(newItem);

                        // Set alarm if reminder is enabled
                        if (hasReminder[0] && !selectedDueDate[0].isEmpty() && !selectedDueTime[0].isEmpty()) {
                            AlarmHelper.setAlarm(MainActivity.this, newItem);
                        }

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
                    dueDateText.setText("Due: " + formatDateForDisplay(dueDate));
                    dueDateText.setVisibility(View.VISIBLE);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        // Optional: Set minimum date to today
        datePicker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePicker.setTitle("Select Due Date");
        datePicker.show();
    }

    private void showTimePickerDialog(TextView dueTimeText, String[] selectedDueTime) {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePicker = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    String dueTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    selectedDueTime[0] = dueTime;
                    String displayTime = formatTimeForDisplay(hourOfDay, minute);
                    dueTimeText.setText("Time: " + displayTime);
                    dueTimeText.setVisibility(View.VISIBLE);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true); // 24-hour format

        timePicker.setTitle("Select Due Time");
        timePicker.show();
    }

    private String formatDateForDisplay(String date) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return outputFormat.format(inputFormat.parse(date));
        } catch (Exception e) {
            return date;
        }
    }

    private String formatTimeForDisplay(int hour, int minute) {
        String period = "AM";
        int displayHour = hour;

        if (hour >= 12) {
            period = "PM";
            if (hour > 12) {
                displayHour = hour - 12;
            }
        }
        if (hour == 0) {
            displayHour = 12;
        }

        return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, period);
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
                // Sort by due date and time
                newList.sort((item1, item2) -> {
                    boolean hasDate1 = item1.hasDueDate();
                    boolean hasDate2 = item2.hasDueDate();

                    if (hasDate1 && hasDate2) {
                        int dateCompare = item1.getDueDate().compareTo(item2.getDueDate());
                        if (dateCompare == 0) {
                            // Same date, compare time
                            boolean hasTime1 = item1.hasDueTime();
                            boolean hasTime2 = item2.hasDueTime();
                            if (hasTime1 && hasTime2) {
                                return item1.getDueTime().compareTo(item2.getDueTime());
                            } else if (hasTime1) {
                                return -1;
                            } else if (hasTime2) {
                                return 1;
                            }
                            return 0;
                        }
                        return dateCompare;
                    } else if (hasDate1) {
                        return -1;
                    } else if (hasDate2) {
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
                // Sort by priority, then date, then time
                newList.sort((item1, item2) -> {
                    int priorityCompare = Integer.compare(item1.getPriority(), item2.getPriority());
                    if (priorityCompare != 0) {
                        return priorityCompare;
                    }

                    // Same priority, compare dates
                    boolean hasDate1 = item1.hasDueDate();
                    boolean hasDate2 = item2.hasDueDate();

                    if (hasDate1 && hasDate2) {
                        int dateCompare = item1.getDueDate().compareTo(item2.getDueDate());
                        if (dateCompare == 0 && item1.hasDueTime() && item2.hasDueTime()) {
                            return item1.getDueTime().compareTo(item2.getDueTime());
                        }
                        return dateCompare;
                    } else if (hasDate1) {
                        return -1;
                    } else if (hasDate2) {
                        return 1;
                    }
                    return 0;
                });
                break;
        }

        adapter.updateList(newList);
        updateStats();
    }

    private void updateStats() {
        int total = databaseHelper.getTotalCount();
        int completed = databaseHelper.getCompletedCount();
        int pending = databaseHelper.getPendingCount();
        int overdue = databaseHelper.getOverdueCount();
        int dueToday = databaseHelper.getDueTodayCount();
        int withReminders = databaseHelper.getReminderCount();

        String stats = String.format(Locale.getDefault(),
                "Total: %d • Completed: %d • Pending: %d\nOverdue: %d • Due Today: %d • Reminders: %d",
                total, completed, pending, overdue, dueToday, withReminders);
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

                // Handle alarms when task completion status changes
                if (isChecked && item.hasReminder()) {
                    // Cancel alarm when task is completed
                    AlarmHelper.cancelAlarm(MainActivity.this, item.getId());
                } else if (!isChecked && item.hasReminder() && !item.isOverdue()) {
                    // Restore alarm if task is marked incomplete and not overdue
                    AlarmHelper.setAlarm(MainActivity.this, item);
                }

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
        TextView dueTimeText = dialogView.findViewById(R.id.dialog_due_time_text);
        Button setDueDateButton = dialogView.findViewById(R.id.dialog_set_due_date);
        Button setDueTimeButton = dialogView.findViewById(R.id.dialog_set_due_time);
        CheckBox reminderCheckbox = dialogView.findViewById(R.id.dialog_reminder_checkbox);

        // Populate fields
        taskInput.setText(item.getTask());
        descriptionInput.setText(item.getDescription() != null ? item.getDescription() : "");

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
        } else {
            categorySpinner.setSelection(0);
        }

        // Setup priority spinner
        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(this,
                R.array.priority_levels, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);
        prioritySpinner.setSelection(item.getPriority() - 1);

        // Setup due date and time
        final String[] selectedDueDate = {item.getDueDate() != null ? item.getDueDate() : ""};
        final String[] selectedDueTime = {item.getDueTime() != null ? item.getDueTime() : ""};
        final boolean[] hasReminder = {item.hasReminder()};

        if (item.hasDueDate()) {
            dueDateText.setText("Due: " + formatDateForDisplay(item.getDueDate()));
            dueDateText.setVisibility(View.VISIBLE);
        }

        if (item.hasDueTime()) {
            try {
                String[] timeParts = item.getDueTime().split(":");
                int hour = Integer.parseInt(timeParts[0]);
                int minute = Integer.parseInt(timeParts[1]);
                String displayTime = formatTimeForDisplay(hour, minute);
                dueTimeText.setText("Time: " + displayTime);
                dueTimeText.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                dueTimeText.setText("Time: " + item.getDueTime());
                dueTimeText.setVisibility(View.VISIBLE);
            }
        }

        reminderCheckbox.setChecked(hasReminder[0]);

        setDueDateButton.setOnClickListener(v -> showDatePickerDialog(dueDateText, selectedDueDate));
        setDueTimeButton.setOnClickListener(v -> showTimePickerDialog(dueTimeText, selectedDueTime));

        reminderCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hasReminder[0] = isChecked;
            if (isChecked && selectedDueTime[0].isEmpty()) {
                Toast.makeText(MainActivity.this, "Please set a time for the reminder", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setTitle("Edit Task")
                .setPositiveButton("Save", (dialog, which) -> {
                    String task = taskInput.getText().toString().trim();
                    if (!TextUtils.isEmpty(task)) {
                        // Cancel existing alarm if any
                        if (item.hasReminder()) {
                            AlarmHelper.cancelAlarm(MainActivity.this, item.getId());
                        }

                        item.setTask(task);
                        item.setDescription(descriptionInput.getText().toString());
                        item.setCategory(categorySpinner.getSelectedItem().toString());
                        item.setPriority(prioritySpinner.getSelectedItemPosition() + 1);
                        item.setDueDate(selectedDueDate[0]);
                        item.setDueTime(selectedDueTime[0]);
                        item.setHasReminder(hasReminder[0]);

                        // Calculate and set new alarm time if reminder is enabled
                        if (hasReminder[0] && !selectedDueDate[0].isEmpty() && !selectedDueTime[0].isEmpty()) {
                            long alarmTime = item.calculateAlarmTime();
                            item.setAlarmTime(alarmTime);
                            AlarmHelper.setAlarm(MainActivity.this, item);
                        }

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

        String dueInfo = "Not set";
        if (item.hasDueDate() && item.hasDueTime()) {
            try {
                String[] timeParts = item.getDueTime().split(":");
                int hour = Integer.parseInt(timeParts[0]);
                int minute = Integer.parseInt(timeParts[1]);
                String displayTime = formatTimeForDisplay(hour, minute);
                dueInfo = formatDateForDisplay(item.getDueDate()) + " at " + displayTime;
            } catch (Exception e) {
                dueInfo = item.getDueDate() + " at " + item.getDueTime();
            }
        } else if (item.hasDueDate()) {
            dueInfo = formatDateForDisplay(item.getDueDate());
        }

        String details = "Task: " + item.getTask() + "\n\n" +
                "Description: " + (item.getDescription() != null ? item.getDescription() : "No description") + "\n\n" +
                "Category: " + item.getCategory() + "\n" +
                "Priority: " + item.getPriorityText() + "\n" +
                "Due: " + dueInfo + "\n" +
                "Reminder: " + (item.hasReminder() ? "Yes" : "No") + "\n" +
                "Status: " + (item.isCompleted() ? "Completed" : "Pending") + "\n" +
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
                    // Cancel alarm before deleting
                    if (item.hasReminder()) {
                        AlarmHelper.cancelAlarm(this, item.getId());
                    }
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
        } else if (id == R.id.menu_upcoming_tasks) {
            showUpcomingTasks();
            return true;
        } else if (id == R.id.menu_tasks_with_reminders) {
            showTasksWithReminders();
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
                    // Cancel alarms for all completed tasks
                    for (TodoItem item : completedItems) {
                        if (item.hasReminder()) {
                            AlarmHelper.cancelAlarm(this, item.getId());
                        }
                        databaseHelper.deleteTodoItem(item.getId());
                    }
                    loadTodoItems();
                    updateStats();
                    Toast.makeText(MainActivity.this, "Completed tasks cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showUpcomingTasks() {
        List<TodoItem> upcomingTasks = databaseHelper.getUpcomingTasks();
        if (upcomingTasks.isEmpty()) {
            Toast.makeText(this, "No upcoming tasks", Toast.LENGTH_SHORT).show();
            return;
        }

        currentFilter = "ALL";
        adapter.updateList(upcomingTasks);
        Toast.makeText(this, "Showing upcoming tasks", Toast.LENGTH_SHORT).show();
    }

    private void showTasksWithReminders() {
        List<TodoItem> tasksWithReminders = databaseHelper.getTasksWithReminders();
        if (tasksWithReminders.isEmpty()) {
            Toast.makeText(this, "No tasks with reminders", Toast.LENGTH_SHORT).show();
            return;
        }

        currentFilter = "ALL";
        adapter.updateList(tasksWithReminders);
        Toast.makeText(this, "Showing tasks with reminders", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        databaseHelper.close();
        super.onDestroy();
    }
}