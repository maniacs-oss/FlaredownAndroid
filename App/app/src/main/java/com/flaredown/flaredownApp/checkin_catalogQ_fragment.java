package com.flaredown.flaredownApp;

import android.content.Context;
import android.flaredown.com.flaredown.R;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.flaredown.flaredownApp.FlareDown.Locales;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A placeholder fragment containing a simple view.
 */
public class Checkin_catalogQ_fragment extends ViewPagerFragmentBase {
    private static final String DEBUG_KEY = "checkin_catalogQ_fragment";
    JSONArray questions;
    String catalogue;
    int section;
    public Context context;
    private View fragmentRoot;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getActivity();
        fragmentRoot = inflater.inflate(R.layout.fragment_checkin_catalog_q, container, false);

        ll_questionHolder = (LinearLayout) fragmentRoot.findViewById(R.id.ll_questionHolder);

        try {
            for(int i = 0; i < questions.length(); i++) {
                JSONObject question = questions.getJSONObject(i);
                appendQuestion(question);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }



        return fragmentRoot;
    }

    private void appendQuestion(JSONObject question) throws JSONException{
        String kind = question.getString("kind");
        if(kind.equals("select")) {
            SelectQuestionInflate selectQuestionInflate = new SelectQuestionInflate(question, catalogue, section);
            ll_questionHolder.addView(selectQuestionInflate.ll_root);
        } else if(kind.equals("number")) {
            NumberQuestionInflate numberQuestionInflate = new NumberQuestionInflate(question, catalogue, section);
            ll_questionHolder.addView(numberQuestionInflate.ll_root);
        } else {
            BlankQuestion blankQuestion = new BlankQuestion(question, catalogue, section);
            ll_questionHolder.addView(blankQuestion.ll_root);
        }
    }

    private class NumberQuestionInflate extends BlankQuestion {
        public NumberQuestionInflate(JSONObject question, String catalogue, int section) throws JSONException {
            super(question, catalogue, section);

            JSONObject inputs = question.getJSONArray("inputs").getJSONObject(0);

            final EditText editText = new EditText(context);
            editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            editText.setGravity(Gravity.CENTER_HORIZONTAL);

            if(inputs.has("value")) {
                editText.setText(inputs.getString("value"));
            }

            this.ll_root.addView(editText);
            //if(focusedView == null) focusedView = editText;

            // Make sure it is the first quesiton which is focused
            if(!hasFocusEditText()) setEditTextFocus(editText);
        }
    }
    private class SelectQuestionInflate extends BlankQuestion {
        public SelectQuestionInflate(JSONObject question, String catalogue, int section) throws JSONException {
            super(question, catalogue, section);

            JSONArray inputs = question.getJSONArray("inputs");
            Checkin_Selector_View checkin_selector_view = new Checkin_Selector_View(getActivity()).setInputs(inputs);
            checkin_selector_view.setButtonClickListener(new Checkin_Selector_View.OnButtonClickListener() {
                @Override
                public void onClick() {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // Adding a dealy... allowing confirmation of seleciton.
                                Thread.sleep(250);
                                ((HomeActivity) context).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((HomeActivity) context).nextQuestion();
                                    }
                                });

                            } catch (InterruptedException e) { e.printStackTrace(); }
                        }
                    }).start();

                }
            });
            //checkin_selector_view.setId(Styling.getUniqueId());
            //TODO: restore correctly
            checkin_selector_view.setId(R.id.bt_sign_in);

            this.ll_root.addView(checkin_selector_view);
        }
    }


    private class BlankQuestion {
        public LinearLayout ll_root;
        public TextView tv_questionTitle;
        public TextView tv_catalog;

        BlankQuestion(JSONObject question, String catalogue, int section) throws JSONException{
            // Create root elements
            ll_root = (LinearLayout) ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.checkin_question_blank, null);
            tv_questionTitle = (TextView) ll_root.findViewById(R.id.tv_question);
            tv_catalog = (TextView) ll_root.findViewById(R.id.tv_catalog);
            // Set the questions
            String questionKey = question.getString("name");
            tv_questionTitle.setText(Locales.read(getActivity(), "catalogs." + catalogue + ".section_" + section + "_prompt").resultIfUnsuccessful(questionKey).createAT());
            tv_catalog.setText(Locales.read(getActivity(), "catalogs." + catalogue + ".catalog_description").resultIfUnsuccessful(catalogue).createAT());
        }
    }




    private void changeQuestion () {
        if(catalogue == "symptoms") {
            //tv_question.setText();
        }
    }
}
