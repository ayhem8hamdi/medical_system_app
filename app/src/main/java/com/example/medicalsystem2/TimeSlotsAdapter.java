package com.example.medicalsystem2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TimeSlotsAdapter extends RecyclerView.Adapter<TimeSlotsAdapter.TimeSlotViewHolder> {

    private List<String> timeSlots;
    private String selectedTimeSlot = "";
    private OnTimeSlotClickListener listener;

    public interface OnTimeSlotClickListener {
        void onTimeSlotClick(String timeSlot);
    }

    public TimeSlotsAdapter(List<String> timeSlots, OnTimeSlotClickListener listener) {
        this.timeSlots = timeSlots;
        this.listener = listener;
    }

    public void setTimeSlots(List<String> timeSlots) {
        this.timeSlots = timeSlots;
        notifyDataSetChanged();
    }

    public void setSelectedTimeSlot(String timeSlot) {
        this.selectedTimeSlot = timeSlot;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TimeSlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.time_slot_item, parent, false);
        return new TimeSlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeSlotViewHolder holder, int position) {
        String timeSlot = timeSlots.get(position);
        holder.timeTextView.setText(timeSlot);

        // Set background based on selection
        if (timeSlot.equals(selectedTimeSlot)) {
            holder.timeTextView.setBackgroundResource(R.drawable.selected_time_slot);
            holder.timeTextView.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(android.R.color.white));
        } else {
            holder.timeTextView.setBackgroundResource(R.drawable.time_slot_border);
            holder.timeTextView.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(android.R.color.black));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTimeSlotClick(timeSlot);
            }
        });
    }

    @Override
    public int getItemCount() {
        return timeSlots != null ? timeSlots.size() : 0;
    }

    static class TimeSlotViewHolder extends RecyclerView.ViewHolder {
        TextView timeTextView;

        TimeSlotViewHolder(@NonNull View itemView) {
            super(itemView);
            // Fix this line - use R.id.timeTextView instead of android.R.id.text1
            timeTextView = itemView.findViewById(R.id.timeTextView);
        }
    }
}