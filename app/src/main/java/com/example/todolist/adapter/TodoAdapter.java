package com.example.todolist.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todolist.R;
import com.example.todolist.models.TodoItem;

import java.util.List;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.ViewHolder> {
    private List<TodoItem> todoList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
        void onCheckBoxClick(int position, boolean isChecked);
        void onItemLongClick(int position);
    }

    public TodoAdapter(List<TodoItem> todoList, OnItemClickListener listener) {
        this.todoList = todoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.todo_item_enhanced, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TodoItem item = todoList.get(position);
        holder.bind(item, position, listener);
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    public void updateList(List<TodoItem> newList) {
        this.todoList.clear();
        this.todoList.addAll(newList);
        notifyDataSetChanged();
    }

    public void updateItem(int position, TodoItem item) {
        if (position >= 0 && position < todoList.size()) {
            todoList.set(position, item);
            notifyItemChanged(position);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView taskText, dueDateText, categoryText, priorityText;
        private CheckBox completedCheckbox;
        private View priorityIndicator, categoryIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            taskText = itemView.findViewById(R.id.task_text);
            dueDateText = itemView.findViewById(R.id.due_date_text);
            categoryText = itemView.findViewById(R.id.category_text);
            priorityText = itemView.findViewById(R.id.priority_text);
            completedCheckbox = itemView.findViewById(R.id.completed_checkbox);
            priorityIndicator = itemView.findViewById(R.id.priority_indicator);
            categoryIndicator = itemView.findViewById(R.id.category_indicator);
        }

        public void bind(final TodoItem item, final int position, final OnItemClickListener listener) {
            taskText.setText(item.getTask());

            // Remove previous listener to avoid recursive calls
            completedCheckbox.setOnCheckedChangeListener(null);
            completedCheckbox.setChecked(item.isCompleted());

            // Set due date
            if (item.hasDueDate()) {
                dueDateText.setVisibility(View.VISIBLE);
                dueDateText.setText("Due: " + item.getDueDate());
                if (item.isOverdue()) {
                    dueDateText.setTextColor(0xFFFF5252); // Red for overdue
                } else {
                    dueDateText.setTextColor(0xFF757575); // Gray for normal
                }
            } else {
                dueDateText.setVisibility(View.GONE);
            }

            // Set category
            categoryText.setText(item.getCategory());
            categoryIndicator.setBackgroundColor(getCategoryColor(item.getCategory()));

            // Set priority
            priorityText.setText(item.getPriorityText());
            priorityIndicator.setBackgroundColor(item.getPriorityColor());

            // Update text appearance based on completion
            updateTextAppearance(item.isCompleted());

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(position);
                }
            });

            completedCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // Update visual immediately
                    updateTextAppearance(isChecked);
                    // Notify the activity
                    listener.onCheckBoxClick(position, isChecked);
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    listener.onItemLongClick(position);
                    return true;
                }
            });
        }

        private void updateTextAppearance(boolean isCompleted) {
            if (isCompleted) {
                taskText.setPaintFlags(taskText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                taskText.setAlpha(0.6f);
                dueDateText.setAlpha(0.6f);
                categoryText.setAlpha(0.6f);
                priorityText.setAlpha(0.6f);
            } else {
                taskText.setPaintFlags(taskText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                taskText.setAlpha(1.0f);
                dueDateText.setAlpha(1.0f);
                categoryText.setAlpha(1.0f);
                priorityText.setAlpha(1.0f);
            }
        }

        private int getCategoryColor(String category) {
            // Generate consistent color based on category name
            int hash = category.hashCode();
            int[] colors = {
                    0xFFFF9800, 0xFF2196F3, 0xFF4CAF50, 0xFF9C27B0,
                    0xFFFF5722, 0xFF607D8B, 0xFF795548, 0xFF009688
            };
            return colors[Math.abs(hash) % colors.length];
        }
    }
}