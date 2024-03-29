package com.example.tripshare.adapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tripshare.R;
import com.example.tripshare.model.Location;
import com.example.tripshare.model.Schedule;
import com.example.tripshare.ui.interfaces.IOnClickListener;
import com.example.tripshare.utils.ColorUtil;
import com.google.android.material.divider.MaterialDivider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class ScheduleItemAdapter
        extends RecyclerView.Adapter<ScheduleItemAdapter.ScheduleItemViewHolder>
{
    private List<Schedule> scheduleList;
    private boolean isEditable = false;
    private IOnClickListener onClickListener;
    private boolean isColorLoaded = false;

    public List<Schedule> getScheduleList() {
        return scheduleList;
    }

    public void setEditable(boolean isEditable) {
        this.isEditable = isEditable;
        notifyDataSetChanged();
    }

    public ScheduleItemAdapter(IOnClickListener onClickListener) {
        setHasStableIds(true);
        isColorLoaded = false;
        this.onClickListener = onClickListener;
    }

    public void setScheduleList(List<Schedule> scheduleList) {
        this.scheduleList = scheduleList;
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            List<Integer> markerColors = ColorUtil.generateMapMarkerColors(this.scheduleList.size());
            IntStream.range(0, this.scheduleList.size()).forEach(idx -> {
                this.scheduleList.get(idx).setMarkerColor(markerColors.get(idx));
            });

            notifyDataSetChanged();
        });
        executorService.shutdown();
    }

    private void loadScheduleColors() {
        if (this.scheduleList == null) return;
    }

    @NonNull
    @Override
    public ScheduleItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.schedule_item_layout, parent, false);
        return new ScheduleItemViewHolder(v);
    }

    @Override
    public long getItemId(int position) {
        if (scheduleList != null) {
            return scheduleList.get(position).getId();
        }
        return super.getItemId(position);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleItemViewHolder holder, int position) {
        Schedule currSchedule = scheduleList.get(position);
        holder.title.setText(currSchedule.getTitle());
        holder.dateText.setText(formatDate(currSchedule.getDate()));
        holder.countPlaces.setText(String.join(" ",
                String.valueOf(currSchedule.getLocationCount()),
                holder.itemView.getResources().getString(R.string.count_places)
        ));

        int visibility = currSchedule.isExpandable() ? View.VISIBLE : View.GONE;
        holder.expandableLayout.setVisibility(visibility);

        int iconSrc = currSchedule.isExpandable() ?  R.drawable.ic_baseline_expand_more_24 : R.drawable.ic_baseline_chevron_left_24;
        holder.expandableIcon.setImageResource(iconSrc);

        int isEditVisibility = isEditable ? View.VISIBLE : View.GONE;
        holder.mapRecycleView.setVisibility(currSchedule.getLocationCount() == 0 ? View.GONE : View.VISIBLE);
        MapScheduleItemAdapter mapItemAdapter = new MapScheduleItemAdapter(
                currSchedule.getLocations(),
                currSchedule.getMarkerColor(),
                new IOnClickListener() {
                    @Override
                    public void onClick(String action, Bundle data) {
                        if (data != null) {
                            data.putInt("schedulePos", holder.getBindingAdapterPosition());
                        }
                        onClickListener.onClick(action, data);
                    }
                });
        mapItemAdapter.setEditable(isEditable);
        holder.mapRecycleView.setAdapter(mapItemAdapter);

        if (currSchedule.getLocations() == null || currSchedule.getLocations().isEmpty()) {
            holder.divider.setVisibility(View.GONE);
        }
        else {
            holder.divider.setVisibility(isEditVisibility);
        }
        holder.addPlace.setVisibility(isEditVisibility);
    }

    @Override
    public int getItemCount() {
        if (scheduleList != null) {
            return scheduleList.size();
        }
        return 0;
    }

    public String formatDate(Date date) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(date);
    }

    public void addLocation(int schedulePos, Location location) {
        Schedule currSchedule = scheduleList.get(schedulePos);
        currSchedule.getLocations().add(location);
        currSchedule.setLocationCount(currSchedule.getLocationCount() + 1);
        notifyItemChanged(schedulePos);
    }

    public void removeLocation(int schedulePos, int locationPos) {
        Schedule currSchedule = scheduleList.get(schedulePos);

        List<Location> locations = currSchedule.getLocations();
        locations.remove(locationPos);
        locations.stream().skip(locationPos).forEach(location -> {
            location.setPosition(location.getPosition() - 1);
        });

        currSchedule.setLocationCount(currSchedule.getLocationCount() - 1);
        notifyItemChanged(schedulePos);
    }
    public void editNoteLocation(int schedulePos, int locationPos, String newNote) {
        scheduleList.get(schedulePos).getLocations().get(locationPos).setNote(newNote);
        notifyItemChanged(schedulePos);
    }

    public class ScheduleItemViewHolder extends RecyclerView.ViewHolder {
        ImageView expandableIcon;
        MaterialDivider divider;
        TextView title, dateText, countPlaces, addPlace;
        RelativeLayout titleLayout;
        ConstraintLayout expandableLayout;
        RecyclerView mapRecycleView;

        public ScheduleItemViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.title);
            dateText = itemView.findViewById(R.id.dateText);
            countPlaces = itemView.findViewById(R.id.countPlaces);
            addPlace = itemView.findViewById(R.id.addPlace);
            titleLayout = itemView.findViewById(R.id.titleLayout);
            expandableLayout = itemView.findViewById(R.id.expandableLayout);
            expandableIcon = itemView.findViewById(R.id.expandableIcon);
            divider = itemView.findViewById(R.id.divider);
            mapRecycleView = itemView.findViewById(R.id.mapRecycleView);

            addPlace.setOnClickListener(view -> {
                Bundle bundle = new Bundle();
                bundle.putInt("schedulePos", getBindingAdapterPosition());
                onClickListener.onClick("open_add_place", bundle);
            });
            titleLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Schedule schedule =  scheduleList.get(getBindingAdapterPosition());
                    schedule.setExpandable(!schedule.isExpandable());
                    notifyItemChanged(getBindingAdapterPosition());
                }
            });
        }
    }
}
