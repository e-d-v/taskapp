package com.evanv.taskapp.ui.projects.recycler;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.evanv.taskapp.R;
import com.evanv.taskapp.logic.LogicSubsystem;
import com.evanv.taskapp.ui.additem.ProjectEntry;
import com.evanv.taskapp.ui.main.ClickListener;
import com.google.android.material.chip.Chip;

import java.util.List;

/**
 * Adapter for the project list recycler.
 *
 * @author Evan Voogd
 */
public class ProjectItemAdapter extends RecyclerView.Adapter<ProjectItemAdapter.ProjectViewHolder> {
    public final List<ProjectItem> mProjectsList; // List of project information
    private final ClickListener mListener;        // The listener to handle button presses.
    private final AppCompatActivity mActivity;    // The Context of the ProjectAdapter

    /**
     * Construct a new ProjectItemAdapter
     *
     * @param list The list of projects to display
     * @param clickListener The listener to handle button presses
     */
    @SuppressWarnings("unused")
    public ProjectItemAdapter(List<ProjectItem> list, ClickListener clickListener, AppCompatActivity activity) {
        mProjectsList = list;
        mListener = clickListener;
        mActivity = activity;
    }

    /**
     * Create a new ViewHolder
     *
     * @param parent The view group that holds the ViewHolders
     * @param viewType Not used
     * @return a new ProjectViewHolder
     */
    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflates a new day_item, whose data will be filled by the DayViewHolder
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.project_item, parent, false);

        return new ProjectViewHolder(view);
    }

    /**
     * Update the content in the holder
     *
     * @param holder The holder to update
     * @param position The position in the recycler of the item
     */
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

        holder.mExpandButton.setOnClickListener(v -> {
            AlertDialog.Builder diag = new AlertDialog.Builder(mActivity);
            diag.setItems(R.array.project_options, (dialogInterface, i) -> {
                switch (i) {
                    case 0:
                        ProjectEntry projectEntry = new ProjectEntry();
                        projectEntry.setID(curr.getID());
                        projectEntry.setOnSubmit(view1 -> {
                            mProjectsList.set(position, LogicSubsystem.getInstance()
                                    .getProjectItem(curr.getID(), mActivity));
                            notifyItemChanged(position);
                        });
                        projectEntry.show(mActivity.getSupportFragmentManager(), "PROJECT ENTRY");
                        break;
                    case 1:
                        LogicSubsystem.getInstance().deleteProject(curr.getID());
                        mProjectsList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, getItemCount());
                        break;
                }
            });
            diag.show();
        });

        holder.mParent.setOnClickListener(v -> mListener.onButtonClick(position, -1, 0, -1));
    }

    /**
     * Get the number of items in the recycler.
     *
     * @return the count of items in the recycler
     */
    @Override
    public int getItemCount() {
        return mProjectsList.size();
    }

    /**
     * A ViewHolder that displays information of the project.
     */
    public static class ProjectViewHolder extends RecyclerView.ViewHolder {
        public final Chip mProjectChip;         // The chip that displays the project name
        public final TextView mGoalLabel;       // The label that displays the goal of the project
        public final ImageButton mExpandButton; // The button to show the list of tasks in project

        public final LinearLayout mParent;

        /**
         * Construct a new ViewHolder
         *
         * @param view The view of the ViewHolder
         */
        @SuppressWarnings("unused")
        public ProjectViewHolder(View view) {
            super(view);
            mProjectChip = view.findViewById(R.id.projectChip);
            mGoalLabel = view.findViewById(R.id.projectGoalTextView);
            mExpandButton = view.findViewById(R.id.buttonExpandProject);
            mParent = view.findViewById(R.id.parent);
        }
    }
}
