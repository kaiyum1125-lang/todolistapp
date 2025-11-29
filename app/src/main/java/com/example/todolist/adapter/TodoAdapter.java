package com.example.todolist.adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
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
                .inflate(R.layout.activity_todo_item, parent, false);
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
        todoList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView taskText;
        private CheckBox completedCheckbox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            taskText = itemView.findViewById(R.id.task_text);
            completedCheckbox = itemView.findViewById(R.id.completed_checkbox);
        }

        public void bind(final TodoItem item, final int position, final OnItemClickListener listener) {
            taskText.setText(item.getTask());
            completedCheckbox.setChecked(item.isCompleted());

            // Strike through text if completed
            if (item.isCompleted()) {
                taskText.setPaintFlags(taskText.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                taskText.setAlpha(0.6f);
            } else {
                taskText.setPaintFlags(taskText.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                taskText.setAlpha(1.0f);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(position);
                }
            });

            completedCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
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
    }
}
