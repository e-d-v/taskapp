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
 * Form for creating new labels.
 *
 * @author Evan Voogd
 */
public class LabelEntry extends DialogFragment {
    public int color;                       // User-selected label color
    private EditText mNameET;               // The EditText representing label name
    private TextView mColorLabel;           // The TextView representing label color
    private long mEditedID = -1;            // The ID of the label to edit
    private View.OnClickListener mOnSubmit; // The listener for the submit button

    /**
     * Called upon the LabelEntry's creation
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return the root view of the fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_label_entry, container, false);
        view.findViewById(R.id.colorSelectTextView).setOnClickListener(view1 -> handleColorPress());
        view.findViewById(R.id.submitButton).setOnClickListener(view1 -> submit());
        mNameET = view.findViewById(R.id.editTextLabelName);
        mColorLabel = view.findViewById(R.id.colorSelectTextView);
        color = 11;
        view.findViewById(R.id.helpButton).setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.label_url)));
            startActivity(browserIntent);
        });

        if (mEditedID != -1) {
            mNameET.setText(LogicSubsystem.getInstance().getLabelName(mEditedID));

            color = LogicSubsystem.getInstance().getLabelColor(mEditedID);

            // Change the color of the label to the chosen color.
            String labelStart = getString(R.string.colorLabel);
            SpannableString colorText = new SpannableString(labelStart +
                    getResources().getStringArray(R.array.colors)[color]);

            int[] colors = {ContextCompat.getColor(requireContext(), R.color.pale_blue),
                    ContextCompat.getColor(requireContext(), R.color.blue),
                    ContextCompat.getColor(requireContext(), R.color.pale_green),
                    ContextCompat.getColor(requireContext(), R.color.green),
                    ContextCompat.getColor(requireContext(), R.color.pink),
                    ContextCompat.getColor(requireContext(), R.color.red),
                    ContextCompat.getColor(requireContext(), R.color.pale_orange),
                    ContextCompat.getColor(requireContext(), R.color.orange),
                    ContextCompat.getColor(requireContext(), R.color.lavender),
                    ContextCompat.getColor(requireContext(), R.color.purple),
                    ContextCompat.getColor(requireContext(), R.color.yellow),
                    ContextCompat.getColor(requireContext(), R.color.gray)};

            int colorResource = colors[color];

            colorText.setSpan(
                    new ForegroundColorSpan(colorResource), labelStart.length(), colorText.length(), 0);

            mColorLabel.setText(colorText);
        }

        return view;
    }

    @SuppressWarnings("unused")
    /**
     * Set the ID of the label to edit, if necessary
     */
    public void setID(long id) {
        mEditedID = id;
    }

    /**
     * Add the submit listener that is called when the submit button is pressed
     *
     * @param callback listener that is called when the submit button is pressed
     */
    @SuppressWarnings("unused")
    public void setOnSubmit(View.OnClickListener callback) {
        mOnSubmit = callback;
    }

    /**
     * Creates dialog to choose color of the label.
     */
    public void handleColorPress() {
        final Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.color_picker);
        dialog.setTitle(R.string.pick_label_color);

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
                            ContextCompat.getDrawable(requireContext(),R.drawable.ic_baseline_circle_24));
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

            int[] colors = {ContextCompat.getColor(requireContext(), R.color.pale_blue),
                    ContextCompat.getColor(requireContext(), R.color.blue),
                    ContextCompat.getColor(requireContext(), R.color.pale_green),
                    ContextCompat.getColor(requireContext(), R.color.green),
                    ContextCompat.getColor(requireContext(), R.color.pink),
                    ContextCompat.getColor(requireContext(), R.color.red),
                    ContextCompat.getColor(requireContext(), R.color.pale_orange),
                    ContextCompat.getColor(requireContext(), R.color.orange),
                    ContextCompat.getColor(requireContext(), R.color.lavender),
                    ContextCompat.getColor(requireContext(), R.color.purple),
                    ContextCompat.getColor(requireContext(), R.color.yellow),
                    ContextCompat.getColor(requireContext(), R.color.gray)};

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
     */
    public void submit() {
        String name = String.valueOf(mNameET.getText());

        if (name.equals("")) {
            Toast.makeText(requireContext(), R.string.name_reminder, Toast.LENGTH_LONG).show();
            return;
        }

        if (mEditedID != -1) {
            LogicSubsystem.getInstance().editLabel(name, color, mEditedID);
        }
        else {
            LogicSubsystem.getInstance().addLabel(name, color);
        }

        if (mOnSubmit != null) {
            mOnSubmit.onClick(null);
        }
        dismiss();
    }
}