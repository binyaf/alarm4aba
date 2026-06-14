package com.banjos.dosalarm.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.banjos.dosalarm.R;
import com.banjos.dosalarm.databinding.ItemAlarmBinding;
import com.banjos.dosalarm.tools.DateTimesFormats;
import com.banjos.dosalarm.tools.ZmanimService;
import com.banjos.dosalarm.types.Alarm;
import java.util.Date;

public class AlarmAdapter extends ListAdapter<Alarm, AlarmAdapter.AlarmViewHolder> {

    private final OnAlarmLongClickListener longClickListener;

    public interface OnAlarmLongClickListener {
        void onAlarmLongClick(Alarm alarm, View view);
    }

    public AlarmAdapter(OnAlarmLongClickListener longClickListener) {
        super(new AlarmDiffCallback());
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAlarmBinding binding = ItemAlarmBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new AlarmViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class AlarmViewHolder extends RecyclerView.ViewHolder {
        private final ItemAlarmBinding binding;

        AlarmViewHolder(ItemAlarmBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Alarm alarm) {
            binding.getRoot().setCardBackgroundColor(alarm.isSelected() ?
                    ContextCompat.getColor(itemView.getContext(), R.color.light_blue) :
                    ContextCompat.getColor(itemView.getContext(), R.color.very_very_light_grey));

            Date alarmDate = alarm.getDateAndTime().getTime();
            binding.alarmDate.setText(DateTimesFormats.dateFormat.format(alarmDate));
            binding.alarmTime.setText(DateTimesFormats.timeFormat.format(alarmDate));
            binding.hebrewDate.setText(ZmanimService.getHebrewDateStringFromDate(alarmDate));

            if (alarm.getLabel() != null && !alarm.getLabel().isEmpty()) {
                binding.alarmLabel.setText(alarm.getLabel());
                binding.alarmLabel.setVisibility(View.VISIBLE);
            } else {
                binding.alarmLabel.setVisibility(View.GONE);
            }

            binding.getRoot().setOnLongClickListener(v -> {
                longClickListener.onAlarmLongClick(alarm, v);
                return true;
            });
        }
    }

    private static class AlarmDiffCallback extends DiffUtil.ItemCallback<Alarm> {
        @Override
        public boolean areItemsTheSame(@NonNull Alarm oldItem, @NonNull Alarm newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Alarm oldItem, @NonNull Alarm newItem) {
            return oldItem.equals(newItem);
        }
    }
}