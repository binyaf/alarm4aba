package com.banjos.dosalarm.preference;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.banjos.dosalarm.R;
import java.util.ArrayList;
import java.util.List;

public class RemindersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public static class ReminderItem {
        public String key;
        public String text;
        public boolean isChecked;
        public boolean isCustom;

        public ReminderItem(String key, String text, boolean isChecked, boolean isCustom) {
            this.key = key;
            this.text = text;
            this.isChecked = isChecked;
            this.isCustom = isCustom;
        }
    }

    private static class DisplayItem {
        int type;
        String headerTitle;
        boolean isPersonalHeader;
        ReminderItem reminder;

        DisplayItem(String headerTitle, boolean isPersonalHeader) {
            this.type = TYPE_HEADER;
            this.headerTitle = headerTitle;
            this.isPersonalHeader = isPersonalHeader;
        }

        DisplayItem(ReminderItem reminder) {
            this.type = TYPE_ITEM;
            this.reminder = reminder;
        }
    }

    private final List<DisplayItem> displayItems = new ArrayList<>();
    private final OnReminderActionListener listener;

    public interface OnReminderActionListener {
        void onReminderChecked(ReminderItem item, boolean isChecked);
        void onEditReminder(ReminderItem item);
        void onDeleteReminder(ReminderItem item);
        void onAddReminder();
    }

    public RemindersAdapter(OnReminderActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<ReminderItem> personal, List<ReminderItem> general, String personalHeader, String generalHeader) {
        displayItems.clear();
        
        // Always show personal header
        displayItems.add(new DisplayItem(personalHeader, true));
        for (ReminderItem item : personal) {
            displayItems.add(new DisplayItem(item));
        }

        if (!general.isEmpty()) {
            displayItems.add(new DisplayItem(generalHeader, false));
            for (ReminderItem item : general) {
                displayItems.add(new DisplayItem(item));
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return displayItems.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DisplayItem displayItem = displayItems.get(position);
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.textView.setText(displayItem.headerTitle);

            // Set top margin: 0 for first item, 8dp for others
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) headerHolder.itemView.getLayoutParams();
            int topMargin = position == 0 ? 0 : (int) (8 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            if (params.topMargin != topMargin) {
                params.topMargin = topMargin;
                headerHolder.itemView.setLayoutParams(params);
            }

            if (displayItem.isPersonalHeader) {
                headerHolder.addButton.setVisibility(View.VISIBLE);
                headerHolder.addButton.setOnClickListener(v -> listener.onAddReminder());
            } else {
                headerHolder.addButton.setVisibility(View.GONE);
            }
        } else if (holder instanceof ItemViewHolder) {
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            ReminderItem reminder = displayItem.reminder;
            itemHolder.textView.setText(reminder.text);
            itemHolder.checkBox.setOnCheckedChangeListener(null);
            itemHolder.checkBox.setChecked(reminder.isChecked);
            itemHolder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> listener.onReminderChecked(reminder, isChecked));

            if (reminder.isCustom) {
                itemHolder.editButton.setVisibility(View.VISIBLE);
                itemHolder.deleteButton.setVisibility(View.VISIBLE);
                itemHolder.editButton.setOnClickListener(v -> listener.onEditReminder(reminder));
                itemHolder.deleteButton.setOnClickListener(v -> listener.onDeleteReminder(reminder));
            } else {
                itemHolder.editButton.setVisibility(View.GONE);
                itemHolder.deleteButton.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageButton addButton;

        HeaderViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.header_text);
            addButton = itemView.findViewById(R.id.add_reminder_button_header);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView textView;
        ImageButton editButton;
        ImageButton deleteButton;

        ItemViewHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.reminder_checkbox);
            textView = itemView.findViewById(R.id.reminder_text);
            editButton = itemView.findViewById(R.id.edit_reminder_button);
            deleteButton = itemView.findViewById(R.id.delete_reminder_button);
        }
    }
}
