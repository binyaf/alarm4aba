package com.banjos.dosalarm.preference;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.banjos.dosalarm.R;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RemindersDialogFragment extends PreferenceDialogFragmentCompat implements RemindersAdapter.OnReminderActionListener {

    private RemindersAdapter adapter;
    private List<RemindersAdapter.ReminderItem> personalItems = new ArrayList<>();
    private List<RemindersAdapter.ReminderItem> generalItems = new ArrayList<>();
    private Set<String> checkedKeys = new HashSet<>();
    private static final String PREF_CUSTOM_REMINDERS = "pref_custom_reminders";

    public static RemindersDialogFragment newInstance(String key) {
        final RemindersDialogFragment fragment = new RemindersDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);

        RecyclerView recyclerView = view.findViewById(R.id.reminders_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RemindersAdapter(this);
        recyclerView.setAdapter(adapter);

        loadData();
    }

    private void loadData() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        
        // Load checked keys
        Set<String> savedChecked = prefs.getStringSet(getPreference().getKey(), null);
        if (savedChecked == null) {
            // Load default values from array resource
            String[] defaultValues = getResources().getStringArray(R.array.pref_pre_shabbat_notification_values);
            checkedKeys = new HashSet<>(Arrays.asList(defaultValues));
        } else {
            checkedKeys = new HashSet<>(savedChecked);
        }

        // Load personal items
        Set<String> customReminders = prefs.getStringSet(PREF_CUSTOM_REMINDERS, new HashSet<>());
        personalItems.clear();
        for (String text : customReminders) {
            personalItems.add(new RemindersAdapter.ReminderItem(text, text, checkedKeys.contains(text), true));
        }
        personalItems.sort((o1, o2) -> o1.text.compareToIgnoreCase(o2.text));

        // Load general items
        generalItems.clear();
        String[] entries = getResources().getStringArray(R.array.pref_pre_shabbat_notification_entries);
        String[] values = getResources().getStringArray(R.array.pref_pre_shabbat_notification_values);
        for (int i = 0; i < entries.length; i++) {
            generalItems.add(new RemindersAdapter.ReminderItem(values[i], entries[i], checkedKeys.contains(values[i]), false));
        }
        generalItems.sort((o1, o2) -> o1.text.compareToIgnoreCase(o2.text));

        updateList();
    }

    private void updateList() {
        String personalHeader = personalItems.isEmpty() 
                ? getString(R.string.personal_reminders_header_empty)
                : getString(R.string.personal_reminders_header);
                
        adapter.setData(personalItems, generalItems, 
                personalHeader, 
                getString(R.string.general_reminders_header));
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor = prefs.edit();

            // Save custom reminders list
            Set<String> customSet = new HashSet<>();
            for (RemindersAdapter.ReminderItem item : personalItems) {
                customSet.add(item.text);
            }
            editor.putStringSet(PREF_CUSTOM_REMINDERS, customSet);

            // Save checked items (keys)
            editor.putStringSet(getPreference().getKey(), checkedKeys);
            editor.apply();
        }
    }

    @Override
    public void onReminderChecked(RemindersAdapter.ReminderItem item, boolean isChecked) {
        item.isChecked = isChecked;
        if (isChecked) {
            checkedKeys.add(item.key);
        } else {
            checkedKeys.remove(item.key);
        }
    }

    @Override
    public void onEditReminder(RemindersAdapter.ReminderItem item) {
        showAddEditDialog(item);
    }

    @Override
    public void onDeleteReminder(RemindersAdapter.ReminderItem item) {
        personalItems.remove(item);
        checkedKeys.remove(item.key);
        updateList();
    }

    @Override
    public void onAddReminder() {
        showAddEditDialog(null);
    }

    private void showAddEditDialog(RemindersAdapter.ReminderItem itemToEdit) {
        if (itemToEdit == null && personalItems.size() >= 10) {
            Toast.makeText(getContext(), R.string.error_max_reminders, Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(itemToEdit == null ? R.string.add_reminder : R.string.edit_reminder);

        final EditText input = new EditText(getContext());
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(25)});
        if (itemToEdit != null) {
            input.setText(itemToEdit.text);
        }
        input.setHint(R.string.hint_new_reminder);
        builder.setView(input);

        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            String newText = input.getText().toString().trim();
            if (newText.isEmpty()) return;

            if (itemToEdit == null) {
                // Check for duplicates
                for (RemindersAdapter.ReminderItem item : personalItems) {
                    if (item.text.equalsIgnoreCase(newText)) {
                        Toast.makeText(getContext(), "Item already exists", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                RemindersAdapter.ReminderItem newItem = new RemindersAdapter.ReminderItem(newText, newText, true, true);
                personalItems.add(newItem);
                checkedKeys.add(newItem.key);
            } else {
                // If text changed, update key in checkedKeys if it was checked
                if (!itemToEdit.text.equals(newText)) {
                    if (checkedKeys.contains(itemToEdit.key)) {
                        checkedKeys.remove(itemToEdit.key);
                        checkedKeys.add(newText);
                    }
                    itemToEdit.text = newText;
                    itemToEdit.key = newText;
                }
            }
            personalItems.sort((o1, o2) -> o1.text.compareToIgnoreCase(o2.text));
            updateList();
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }
}
