package com.evanv.taskapp;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.evanv.taskapp.databinding.ActivityAddItemBinding;

public class AddItem extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityAddItemBinding binding;

    // Used by both
    public static final String EXTRA_ITEM = "com.evanv.taskapp.extra.ITEM";
    public static final String EXTRA_TYPE = "com.evanv.taskapp.extra.TYPE";
    public static final String EXTRA_NAME = "com.evanv.taskapp.extra.NAME";
    public static final String EXTRA_TTC = "com.evanv.taskapp.extra.TTC";

    // Used by Task
    public static final String EXTRA_VAL_TASK = "com.evanv.taskapp.extra.val.TASK";
    public static final String EXTRA_ECD = "com.evanv.taskapp.extra.ECD";
    public static final String EXTRA_DUE = "com.evanv.taskapp.extra.DUE";
    public static final String EXTRA_PARENTS = "com.evanv.taskapp.extra.PARENTS";

    // Used by Event
    public static final String EXTRA_VAL_EVENT = "com.evanv.taskapp.extra.val.EVENT";
    public static final String EXTRA_START = "com.evanv.taskapp.extra.START";
    public static final String EXTRA_RECUR = "com.evanv.taskapp.extra.RECUR";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAddItemBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_add_item);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submit();
            }
        });

        RadioGroup rGroup = (RadioGroup)findViewById(R.id.radioGroupTaskEvent);

        rGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                RadioButton checkedRadioButton = (RadioButton)group.findViewById(checkedId);
                boolean isChecked = checkedRadioButton.isChecked();
                if (isChecked)
                {
                    if (checkedRadioButton.getId() == R.id.radioButtonEvent) {
                        navController.navigate(R.id.action_taskEntry_to_eventEntry);
                    }
                    else {
                        navController.navigate(R.id.action_eventEntry_to_taskEntry);
                    }
                }
            }
        });

    }

    protected void submit() {
        NavHostFragment navFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_add_item);
        ItemEntry current = (ItemEntry) navFragment.getChildFragmentManager().getFragments().get(0);

        assert current != null;
        Bundle toReturn = current.getItem();

        if (toReturn != null) {
            Intent replyIntent = new Intent();
            replyIntent.putExtra(EXTRA_ITEM, toReturn);
            setResult(RESULT_OK, replyIntent);
            finish();
        }
        else {
            Toast.makeText(this, "Please complete missing fields.", Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_add_item);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}