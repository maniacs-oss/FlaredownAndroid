package com.flaredown.flaredownApp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.flaredown.com.flaredown.R;
import android.os.Bundle;
import android.text.InputType;
import android.text.Spanned;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.flaredown.flaredownApp.FlareDown.Locales;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class Checkin_catalogQ_fragment extends ViewPagerFragmentBase {
    private static final String DEBUG_KEY = "checkin_catalogQ_fragment";
    JSONArray questions;
    String catalogue;
    int section;
    public Activity mContext;
    private View fragmentRoot;
    private TextView tv_catalogName;
    private TextView tv_sectionTitle;
    private static final String QUESTION_ANS = "Question answers";


    private List<BlankQuestion> questionViews = new ArrayList<>();

    private View focusedView;
    private LinearLayout ll_questionHolder;
    public Checkin_catalogQ_fragment() {
    }

    public Checkin_catalogQ_fragment setQuestion(JSONArray question, int section, String catalogue) {
        this.questions = question;
        this.catalogue = catalogue;
        this.section = section;
        this.focusedView = null;
        return this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        if(savedInstanceState == null || mContext == null) createFragment(inflater, container, savedInstanceState);
        if(savedInstanceState != null) {
            double[] questAns = savedInstanceState.getDoubleArray(QUESTION_ANS);
            for(int i = 0; i < questAns.length; i++) {
                questionViews.get(i).setValue(questAns[i]);
            }
        }

        return fragmentRoot;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        double[] questAns = new double[questionViews.size()];

        for(int i = 0; i < questAns.length; i++) {
            questAns[i] = questionViews.get(i).getValue();
        }

        outState.putDoubleArray(QUESTION_ANS, questAns);
    }

    private void createFragment (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();
        fragmentRoot = inflater.inflate(R.layout.fragment_checkin_catalog_q, container, false);

        ll_questionHolder = (LinearLayout) fragmentRoot.findViewById(R.id.ll_questionHolder);

        tv_catalogName = (TextView) fragmentRoot.findViewById(R.id.tv_catalog);
        tv_sectionTitle = (TextView) fragmentRoot.findViewById(R.id.tv_question);


        String sectionTitle;
        try {
            sectionTitle = questions.getJSONObject(0).getString("name");
            if (catalogue.equals("symptoms"))
                sectionTitle = "How active is the symptom: " + sectionTitle + "?";
            if (catalogue.equals("conditions"))
                sectionTitle = "How active is the condition: " + sectionTitle + "?";
        } catch (JSONException e) {
            sectionTitle = "--";
        }
        sectionTitle = Locales.read(getActivity(), "catalogs." + catalogue + ".section_" + section + "_prompt").resultIfUnsuccessful(sectionTitle).create();

        tv_sectionTitle.setText(sectionTitle);

        if(catalogue.equals("conditions")) {
            tv_catalogName.setText(Locales.read(getActivity(), "onboarding.edit_conditions").resultIfUnsuccessful("Edit conditions.").capitalize1Char().createAT());
        } else if(catalogue.equals("symptoms")) {
            tv_catalogName.setText(Locales.read(getActivity(), "onboarding.edit_symptoms").resultIfUnsuccessful("Edit symptoms.").capitalize1Char().createAT());
        } else {
            tv_catalogName.setText(Locales.read(getActivity(), "catalogs." + catalogue + ".catalog_description").resultIfUnsuccessful(catalogue).capitalize1Char().createAT());
        }

        tv_catalogName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(catalogue.equals("symptoms")) {
                    AddADialogActivity.startActivity(mContext, Locales.read(mContext, "onboarding.add_a_symptom_title").capitalize1Char().create(), "/symptoms/search");
                } else if(catalogue.equals("conditions")) {
                    AddADialogActivity.startActivity(mContext, Locales.read(mContext, "onboarding.add_condition").capitalize1Char().create(), "/conditions/search");
                }
            }
        });

        try {
            for (int i = 0; i < questions.length(); i++) {
                JSONObject question = questions.getJSONObject(i);
                appendQuestion(question);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void appendQuestion(JSONObject question) throws JSONException{
        String kind = question.getString("kind");
        if(kind.equals("select")) {
            SelectQuestionInflate selectQuestionInflate = new SelectQuestionInflate(question, catalogue, section);
            ll_questionHolder.addView(selectQuestionInflate.ll_root);
        } else if(kind.equals("number")) {
            NumberQuestionInflate numberQuestionInflate = new NumberQuestionInflate(question, catalogue, section);
            ll_questionHolder.addView(numberQuestionInflate.ll_root);
        } else if(kind.equals("checkbox")){
            CheckBoxQuestionInflate checkBoxQuestionInflate = new CheckBoxQuestionInflate(question, catalogue, section);
            ll_questionHolder.addView(checkBoxQuestionInflate.ll_root);
        } else {
            BlankQuestion blankQuestion = new BlankQuestion(question, catalogue, section);
            ll_questionHolder.addView(blankQuestion.ll_root);
        }
    }

    private class NumberQuestionInflate extends BlankQuestion {
        EditText editText;
        public NumberQuestionInflate(JSONObject question, String catalogue, int section) throws JSONException {
            super(question, catalogue, section);
            JSONObject inputs = question.getJSONArray("inputs").getJSONObject(0);

            editText = new EditText(mContext);
            if(inputs.has("step") && inputs.getString("step").contains("."))
                editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            else
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);

            editText.setGravity(Gravity.CENTER_HORIZONTAL);

            if(inputs.has("value")) {
                editText.setText(inputs.getString("value"));
            }

            this.ll_root.addView(editText);
            //if(focusedView == null) focusedView = editText;

            // Make sure it is the first quesiton which is focused
            if(!hasFocusEditText()) setEditTextFocus(editText);
        }

        @Override
        public double getValue() {
            return Double.valueOf(editText.getText().toString());
        }

        @Override
        public void setValue(double value) {
            editText.setText(String.valueOf(value));
        }
    }
    private class SelectQuestionInflate extends BlankQuestion {
        Checkin_Selector_View checkin_selector_view;
        public SelectQuestionInflate(JSONObject question, String catalogue, int section) throws JSONException {
            super(question, catalogue, section);

            JSONArray inputs = question.getJSONArray("inputs");
            checkin_selector_view = new Checkin_Selector_View(getActivity()).setInputs(inputs);
            this.ll_root.addView(checkin_selector_view);
        }

        @Override
        public double getValue() {
            return checkin_selector_view.getValue();
        }

        @Override
        public void setValue(double value) {
            checkin_selector_view.setValue(value);
        }
    }
    private class CheckBoxQuestionInflate extends BlankQuestion {
        Button button;
        public CheckBoxQuestionInflate(JSONObject question, String catalogue, int section) throws JSONException {
            super(question, catalogue, section);

            button = new Button(new ContextThemeWrapper(mContext, R.style.AppTheme_Checkin_Selector_Button), null, R.style.AppTheme_Checkin_Selector_Button);

            Spanned label = Locales.read(mContext, "catalogs." + catalogue + "." + question.getString("name")).resultIfUnsuccessful(question.getString("name")).createAT();

            button.setText(label);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.setSelected(!v.isSelected());
                }
            });

            this.ll_root.addView(button);
            int margins  = (int) Styling.getInDP(mContext, 5);
            ((ViewGroup.MarginLayoutParams) button.getLayoutParams()).setMargins(margins, margins, margins, margins);
        }

        @Override
        public double getValue() {
            return (button.isSelected())? 1 : 0;
        }

        @Override
        public void setValue(double value) {
            button.setSelected(value == 1.0);
        }
    }


    private class BlankQuestion {
        public LinearLayout ll_root;
        public double getValue() {
            return 0;
        }
        public void setValue(double value) {

        }

        BlankQuestion(JSONObject question, String catalogue, int section) throws JSONException{
            questionViews.add(this); // Add to the list of elements for easy restoration
            // Create root elements
            ll_root = (LinearLayout) ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.checkin_question_blank, null);
        }
    }
}