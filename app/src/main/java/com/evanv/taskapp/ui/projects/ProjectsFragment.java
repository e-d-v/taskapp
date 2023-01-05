package com.evanv.taskapp.ui.projects;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.evanv.taskapp.R;
import com.evanv.taskapp.databinding.FragmentProjectsBinding;
import com.evanv.taskapp.ui.main.MainActivity;
import com.evanv.taskapp.ui.projects.recycler.ProjectItem;
import com.evanv.taskapp.ui.projects.recycler.ProjectItemAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProjectsFragment extends Fragment {

    private FragmentProjectsBinding binding;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentProjectsBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ArrayList<String> projectNames = requireActivity().getIntent().getStringArrayListExtra(MainActivity.EXTRA_PROJECTS);
        ArrayList<String> projectGoals = requireActivity().getIntent().getStringArrayListExtra(MainActivity.EXTRA_GOALS);
        ArrayList<Integer> projectColors = requireActivity().getIntent().getIntegerArrayListExtra(MainActivity.EXTRA_PROJECT_COLORS);

        List<ProjectItem> mProjectItemList = new ArrayList<>();

        for (int i = 0; i < projectNames.size(); i++) {
            mProjectItemList.add(
                    new ProjectItem(projectNames.get(i), projectGoals.get(i), projectColors.get(i)));
        }

        RecyclerView recycler = view.findViewById(R.id.projects_recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler.setAdapter(new ProjectItemAdapter(mProjectItemList));
        recycler.setLayoutManager(layoutManager);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}