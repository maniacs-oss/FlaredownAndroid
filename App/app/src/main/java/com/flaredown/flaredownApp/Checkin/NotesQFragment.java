package com.flaredown.flaredownApp.Checkin;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.flaredown.flaredownApp.Helpers.APIv2.EndPoints.CheckIns.CheckIn;
import com.flaredown.flaredownApp.Helpers.PreferenceKeys;
import com.flaredown.flaredownApp.R;

import java.util.concurrent.TimeUnit;

/**
 * Allows users to write a note for the day in the check in summary view.
 */
public class NotesQFragment extends ViewPagerFragmentBase {
    private static String DEBUG_KEY = "CI_NotesQFrag";


    private FrameLayout fl_root;
    private EditText et_noteText;
    private Thread textChangeWaitingThread;
    private CheckIn checkIn;

    /**
     * Creates a new instance of the notes fragment card for the check in summary.
     * @return
     */
    public static NotesQFragment newInstance() {
        NotesQFragment fragment = new NotesQFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        assignViews(inflater, container);
        this.checkIn = getCheckInActivity().getCheckIn();

        // Display note if available
        if(this.checkIn.getNote() != null && !"".equals(this.checkIn.getNote()) && !"null".equals(this.checkIn.getNote())) {
            et_noteText.setText(this.checkIn.getNote());
        }


        // Attach note text change listener
        et_noteText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(textChangeWaitingThread != null) {
                    textChangeWaitingThread.interrupt();
                }
                textChangeWaitingThread = new Thread(new TextChangedWaiting());
                textChangeWaitingThread.start();
            }
        });

        // Save if the application is paused.
        getCheckInActivity().addActivityPauseListener(new ActivityPauseEventListener() {
            @Override
            public void onPause() {
                updateCheckIn();
                if(NotesQFragment.this.textChangeWaitingThread != null) {
                    NotesQFragment.this.textChangeWaitingThread.interrupt();
                    NotesQFragment.this.textChangeWaitingThread = null;
                }

            }
        });

        return fl_root;
    }

    /**
     * Assigns the views for the fragment.
     */
    private void assignViews(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        fl_root = (FrameLayout) layoutInflater.inflate(R.layout.checkin_fragment_notes_q, viewGroup, false);
        et_noteText = (EditText) fl_root.findViewById(R.id.et_note_text);
    }

    /**
     * Updates the check in note with the edit text value.
     */
    private void updateCheckIn() {
        getCheckInActivity().getCheckIn().setNote(et_noteText.getText().toString());
        getCheckInActivity().checkInUpdate();
    }

    private class TextChangedWaiting implements Runnable {
        @Override
        public void run() {
            try {
                TimeUnit.SECONDS.sleep(5);
                updateCheckIn();
            } catch (InterruptedException e) {
                PreferenceKeys.log(PreferenceKeys.LOG_D, "THREAD", "TextChangedWaiting interrupted");
            }
        }
    }
}
