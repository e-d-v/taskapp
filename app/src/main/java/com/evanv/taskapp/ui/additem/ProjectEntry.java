package com.evanv.taskapp.ui.additem;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.evanv.taskapp.R;
import com.evanv.taskapp.logic.LogicSubsystem;

/**
 * Form for project creation
 *
 * @author Evan Voogd
 */
public class ProjectEntry extends DialogFragment {
    public int color; // User-selected project color
    private TextView mColorLabel;
    private EditText mNameET;
    private EditText mGoalET;
    private long mEditedID = -1;
    private View.OnClickListener mOnSubmit;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_project_entry, container, false);
        mColorLabel = view.findViewById(R.id.colorSelectTextView);
        mNameET = view.findViewById(R.id.editTextProjectName);
        mGoalET = view.findViewById(R.id.editTextGoal);
        view.findViewById(R.id.colorSelectTextView).setOnClickListener(this::handleColorPress);
        view.findViewById(R.id.submitButton).setOnClickListener(this::submit);
        color = 11;
        view.findViewById(R.id.helpButton).setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.project_url)));
            startActivity(browserIntent);
        });

        if (mEditedID != -1) {
            mNameET.setText(LogicSubsystem.getInstance().getProjectName(mEditedID));
            color = LogicSubsystem.getInstance().getProjectColor(mEditedID);
            mGoalET.setText(LogicSubsystem.getInstance().getProjectGoal(mEditedID));

            // Change the color of the label to the chosen color.
            String labelStart = getString(R.string.colorLabel);
            SpannableString colorText = new SpannableString(labelStart +
                    getResources().getStringArray(R.array.colors)[color]);

            int[] colors = {getResources().getColor(R.color.pale_blue),
                    getResources().getColor(R.color.blue),
                    getResources().getColor(R.color.pale_green),
                    getResources().getColor(R.color.green),
                    getResources().getColor(R.color.pink),
                    getResources().getColor(R.color.red),
                    getResources().getColor(R.color.pale_orange),
                    getResources().getColor(R.color.orange),
                    getResources().getColor(R.color.lavender),
                    getResources().getColor(R.color.purple),
                    getResources().getColor(R.color.yellow),
                    getResources().getColor(R.color.gray)};

            int colorResource = colors[color];

            colorText.setSpan(
                    new ForegroundColorSpan(colorResource), labelStart.length(), colorText.length(), 0);

            mColorLabel.setText(colorText);
        }
        return view;
    }

    public void setID(long id) {
        mEditedID = id;
    }

    public void setOnSubmit(View.OnClickListener callback) {
        mOnSubmit = callback;
    }

    /**
     * Creates dialog to choose color of the project.
     *
     * @param view not used
     */
    public void handleColorPress(View view) {
        final Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.color_picker);
        dialog.setTitle("Pick Project Color");

        ImageButton[] buttons = new ImageButton[12];
        buttons[0] = dialog.findViewById(R.id.paleBlueButton);
        buttons[1] = dialog.findViewById(R.id.blueButton);
        buttons[2] = dialog.findViewById(R.id.paleGreenButton);
        buttons[3] = dialog.findViewById(R.id.greenButton);
        buttons[4] = dialog.findViewById(R.id.pinkButton);
        buttons[5] = dialog.findViewById(R.id.redButton);
        buttons[6] = dialog.findViewById(R.id.paleOrangeButton);
        buttons[7] = dialog.findViewById(R.id.orangeButton);
        buttons[8] = dialog.findViewById(R.id.lavenderButton);
        buttons[9] = dialog.findViewById(R.id.purpleButton);
        buttons[10] = dialog.findViewById(R.id.yellowButton);
        buttons[11] = dialog.findViewById(R.id.grayButton);

        for (ImageButton b : buttons) {
            b.setSelected(false);

            b.setOnClickListener(v -> {
                for (ImageButton button : buttons) {
                    button.setSelected(false);
                    button.setImageDrawable(
                            ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_circle_24));
                }

                v.setSelected(true);
                ((ImageButton) v).setImageDrawable(
                        ContextCompat.getDrawable(requireContext(), R.drawable.ic_select_color_24));
            });
        }

        buttons[11].setSelected(true);

        dialog.findViewById(R.id.selectButton).setOnClickListener(view1 -> {
            // Set color to selected color.
            for (color = 0; color < 12; color++) {
                if (buttons[color].isSelected()) {
                    break;
                }
            }

            // Change the color of the label to the chosen color.
            String labelStart = getString(R.string.colorLabel);
            SpannableString colorText = new SpannableString(labelStart +
                    getResources().getStringArray(R.array.colors)[color]);

            int[] colors = {getResources().getColor(R.color.pale_blue),
                    getResources().getColor(R.color.blue),
                    getResources().getColor(R.color.pale_green),
                    getResources().getColor(R.color.green),
                    getResources().getColor(R.color.pink),
                    getResources().getColor(R.color.red),
                    getResources().getColor(R.color.pale_orange),
                    getResources().getColor(R.color.orange),
                    getResources().getColor(R.color.lavender),
                    getResources().getColor(R.color.purple),
                    getResources().getColor(R.color.yellow),
                    getResources().getColor(R.color.gray)};

            int colorResource = colors[color];

            colorText.setSpan(
                    new ForegroundColorSpan(colorResource), labelStart.length(), colorText.length(), 0);

            mColorLabel.setText(colorText);

            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * If no required fields are empty, pack user input into a bundle and send it.
     *
     * @param view not used
     */
    public void submit(View view) {
        String name = String.valueOf((mNameET).getText());
        String goal = String.valueOf((mGoalET).getText());

        if (name.equals("")) {
            Toast.makeText(requireContext(), R.string.name_reminder, Toast.LENGTH_LONG).show();
            return;
        }
        if (goal.equals("")) {
            Toast.makeText(requireContext(), R.string.goal_reminder, Toast.LENGTH_LONG).show();
            return;
        }

        // Add Project to LogicSubsystem
        if (mEditedID != -1) {
            LogicSubsystem.getInstance().editProject(name, color, goal, mEditedID);
        }
        else {
            LogicSubsystem.getInstance().addProject(name, color, goal);
        }

        if (mOnSubmit != null) {
            mOnSubmit.onClick(null);
        }

        // Return control
        dismiss();
    }
}