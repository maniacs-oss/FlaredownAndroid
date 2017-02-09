package com.flaredown.flaredownApp.Activities.Checkin.InputViews;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.flaredown.flaredownApp.Helpers.APIv2_old.APIResponse;
import com.flaredown.flaredownApp.Helpers.APIv2_old.Communicate;
import com.flaredown.flaredownApp.Helpers.APIv2_old.EndPoints.CheckIns.TreatmentTrackable;
import com.flaredown.flaredownApp.Helpers.APIv2_old.EndPoints.Searches.Search;
import com.flaredown.flaredownApp.Helpers.APIv2_old.EndPoints.Searches.Searchable;
import com.flaredown.flaredownApp.Helpers.APIv2_old.Error;
import com.flaredown.flaredownApp.R;

import java.util.ArrayList;

import rx.Subscriber;

/**
 * Input view for a treatments used for tracking treatments.
 */
public class TreatmentDetails extends LinearLayout implements View.OnClickListener{

    private TreatmentTrackable mTrackable;
    private Switch mTreatmentSwitch;
    private TextView mTreatmentDose;
    private TextView mTreatmentName;
    private TextView mRemove;
    private TextView mDone;
    private AutoCompleteTextView mTreatmentDoseAutoComplete;

    private Communicate api;

    public TreatmentDetails(final Context context, TreatmentTrackable trackable) {
        super(context);
        mTrackable = trackable;
        api = new Communicate(context);

        // Set parameters.
        this.setOrientation(VERTICAL);
        this.setGravity(Gravity.CENTER_HORIZONTAL);

        //Inflate TreatmentItem View
        View treatmentItemLayout = inflate(getContext(),R.layout.treatment_view,null);
        this.addView(treatmentItemLayout);

        mTreatmentSwitch = (Switch) treatmentItemLayout.findViewById(R.id.treatmentSwitch);
        mTreatmentSwitch.setOnClickListener(this);
        mTreatmentDose = (TextView) treatmentItemLayout.findViewById(R.id.treatmentDose);
        mTreatmentDose.setOnClickListener(this);
        mTreatmentName = (TextView) treatmentItemLayout.findViewById(R.id.treatmentName);
        mRemove = (TextView) treatmentItemLayout.findViewById(R.id.treatmentRemove);
        mRemove.setOnClickListener(this);
        mDone = (TextView) treatmentItemLayout.findViewById(R.id.doseDone);
        mDone.setOnClickListener(this);

        mTreatmentDoseAutoComplete = (AutoCompleteTextView) findViewById(R.id.treatmentDoseAutoComplete);
        api.getSuggestedDoses(trackable.getTrackableId().toString(), new APIResponse<Search, Error>() {
            @Override
            public void onSuccess(Search result) {
                ArrayList<String> doses = new ArrayList<>();
                for (Searchable searchable : result.getSearchables()){
                    doses.add(searchable.getName());
                }
                mTreatmentDoseAutoComplete.setAdapter(new ArrayAdapter<String>(getContext(),android.R.layout.simple_dropdown_item_1line,doses));
            }

            @Override
            public void onFailure(Error result) {
                Log.d("Treatment Error",result.toString());
            }
        });
        mTreatmentDoseAutoComplete.setDropDownAnchor(this.getId());

        Subscriber<String> trackableValueSubscriber = new Subscriber<String>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(String s) {
                mTreatmentName.setText(mTrackable.getMetaTrackable().getName());
                if (null == mTrackable.getValue() || mTrackable.getValue().isEmpty()){
                    mTreatmentDose.setText(R.string.locales_no_dose);
                } else {
                    mTreatmentDose.setText(mTrackable.getValue());
                }

                if (mTrackable.getIsTaken()){
                    mTreatmentSwitch.setChecked(true);
                    toggleSwitchAndVisibility();
                } else {
                    mTreatmentSwitch.setChecked(false);
                    toggleSwitchAndVisibility();
                }
            }
        };

        trackableValueSubscriber.onNext(trackable.getValue());
        trackable.subscribeValueObservable(trackableValueSubscriber);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.treatmentRemove){
            // TODO remove treatment dose... Need to have access to the check in object/treatments collection.
        } else if (view.getId() == R.id.treatmentDose){
            toggleDoseEdit();
        } else if (view.getId() == R.id.treatmentSwitch){
            mTrackable.setIsTaken(mTreatmentSwitch.isChecked());
            toggleSwitchAndVisibility();
        } else if (view.getId() == R.id.doseDone){
            mTreatmentDose.setText(mTreatmentDoseAutoComplete.getText());
            toggleDoseEdit();
            mTrackable.setValue(mTreatmentDoseAutoComplete.getText().toString());
        }

    }

    private void toggleDoseEdit() {
        if (mTreatmentDose.getVisibility() == View.GONE){
            mTreatmentDose.setVisibility(View.VISIBLE);
            mTreatmentDoseAutoComplete.setVisibility(View.GONE);
            mDone.setVisibility(View.GONE);
            //Hide Keyboard
            View v = getRootView();
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        } else {
            mTreatmentDose.setVisibility(View.GONE);
            mTreatmentDoseAutoComplete.setVisibility(View.VISIBLE);
            mDone.setVisibility(View.VISIBLE);
        }
    }

    private void toggleSwitchAndVisibility() {
        if (mTreatmentSwitch.isChecked()){
            mTreatmentDose.setVisibility(View.VISIBLE);
            mTreatmentName.setAlpha((float) 1);
        } else {
            mTreatmentDose.setVisibility(View.GONE);
            mTreatmentName.setAlpha((float) .5);
        }
    }
}
