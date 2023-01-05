package com.evanv.taskapp.ui.projects.recycler;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.evanv.taskapp.R;
import com.google.android.material.chip.Chip;

import java.util.List;

public class ProjectItemAdapter extends RecyclerView.Adapter<ProjectItemAdapter.ProjectViewHolder> {
    public List<ProjectItem> mProjectsList;

    public ProjectItemAdapter(List<ProjectItem> list) {
        mProjectsList = list;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflates a new day_item, whose data will be filled by the DayViewHolder
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.project_item, parent, false);

        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        ProjectItem curr = mProjectsList.get(position);


        // Set project name
        holder.mProjectChip.setText(curr.getName());

        // Set project color
        int[] colors = {R.color.pale_blue,
                R.color.blue,
                R.color.pale_green,
                R.color.green,
                R.color.pink,
                R.color.red,
                R.color.pale_orange,
                R.color.orange,
                R.color.lavender,
                R.color.purple,
                R.color.yellow,
                R.color.gray};

        // What color is most readable on the background
        int[] textColors = { Color.BLACK,
                Color.WHITE,
                Color.BLACK,
                Color.BLACK,
                Color.BLACK,
                Color.WHITE,
                Color.BLACK,
                Color.BLACK,
                Color.BLACK,
                Color.WHITE,
                Color.BLACK,
                Color.BLACK};

        // Set colors
        holder.mProjectChip.setChipBackgroundColorResource(colors[curr.getColor()]);
        holder.mProjectChip.setTextColor(textColors[curr.getColor()]);

        // Set project goal
        holder.mGoalLabel.setText(curr.getGoal());
    }

    @Override
    public int getItemCount() {
        return mProjectsList.size();
    }

    public static class ProjectViewHolder extends RecyclerView.ViewHolder {
        public final Chip mProjectChip;
        public final TextView mGoalLabel;
        public final ImageButton mExpandButton;

        public ProjectViewHolder(View view) {
            super(view);
            mProjectChip = view.findViewById(R.id.projectChip);
            mGoalLabel = view.findViewById(R.id.projectGoalTextView);
            mExpandButton = view.findViewById(R.id.buttonExpandProject);
        }
    }
}
