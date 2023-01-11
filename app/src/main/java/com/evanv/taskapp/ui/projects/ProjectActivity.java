package com.evanv.taskapp.ui.projects;

import android.content.Intent;
import android.os.Bundle;

import com.evanv.taskapp.logic.LogicSubsystem;
import com.evanv.taskapp.ui.TaskListActivity;
import com.evanv.taskapp.ui.main.ClickListener;
import com.evanv.taskapp.ui.projects.recycler.ProjectItem;
import com.evanv.taskapp.ui.projects.recycler.ProjectItemAdapter;

import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.evanv.taskapp.databinding.ActivityProjectBinding;

import com.evanv.taskapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Activity showing a list of Projects.
 *
 * @author Evan Voogd
 */
public class ProjectActivity extends AppCompatActivity implements ClickListener {
    /**
     * Create a project activity.
     *
     * @param savedInstanceState Not used.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityProjectBinding binding = ActivityProjectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        ArrayList<String> projectNames = LogicSubsystem.getInstance().getProjectNames();
        ArrayList<String> projectGoals = LogicSubsystem.getInstance().getProjectGoals();
        ArrayList<Integer> projectColors = LogicSubsystem.getInstance().getProjectColors();

        List<ProjectItem> mProjectItemList = new ArrayList<>();

        for (int i = 0; i < projectNames.size(); i++) {
            mProjectItemList.add(
                    new ProjectItem(projectNames.get(i), projectGoals.get(i), projectColors.get(i)));
        }

        RecyclerView recycler = findViewById(R.id.projects_recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recycler.setAdapter(new ProjectItemAdapter(mProjectItemList, this));
        recycler.setLayoutManager(layoutManager);
    }

    /**
     * Handles a button clicked.
     *
     * @param position The index in the project recycler that was clicked.
     * @param day Not used.
     * @param action Not used.
     */
    @Override
    public void onButtonClick(int position, int day, int action) {
        Intent intent = new Intent(this, TaskListActivity.class);
        intent.putExtra(TaskListActivity.EXTRA_PROJECT,
                LogicSubsystem.getInstance().getProjectID(position));
        startActivity(intent);
    }
}