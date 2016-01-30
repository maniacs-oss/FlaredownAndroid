package com.flaredown.flaredownApp.Checkin;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.flaredown.flaredownApp.FlareDown.API;
import com.flaredown.flaredownApp.FlareDown.API_Error;
import com.flaredown.flaredownApp.FlareDown.DefaultErrors;
import com.flaredown.flaredownApp.FlareDown.ForceLogin;
import com.flaredown.flaredownApp.MainToolbarView;
import com.flaredown.flaredownApp.R;
import com.flaredown.flaredownApp.SettingsActivity;
import com.flaredown.flaredownApp.Styling;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class CheckinActivity extends AppCompatActivity {
    API flareDownAPI;

    private Date checkinDate = null;
    private JSONObject entriesJSONObject = null;
    private JSONObject responseJSONObject = null;
    private List<EntryParsers.CollectionCatalogDefinition> collectionCatalogDefinitions;

    /*
        View Variables.
     */
    private Button bt_nextQuestion;
    private Button bt_prevQuestion;
    private Button bt_submitCheckin;
    private Button bt_not_checked_in_checkin;

    private TextView tv_not_checked_in_checkin;

    private MainToolbarView mainToolbarView;
    private Toolbar toolbar;
    private TextView toolbarTitle;
    private LinearLayout ll_not_checked_in;
    private LinearLayout ll_splashScreen;
    private RelativeLayout rl_checkin;
    private FrameLayout fl_checkin_summary;
    private Fragment f_checkin_sumary;

    private ViewPager vp_questions;
    private ViewPagerAdapter vpa_questions;

    private OnActivityResultListener onActivityResultListener;

    /*
        Instance constant arguments.
     */
    private static final String SI_ENTRIES_JSON = "entries endpoint";
    private static final String SI_RESPONSE_JSON = "response json";
    private static final String SI_CURRENT_VIEW = "current view";
    private static final String SI_CHECKIN_DATE = "checkin date";
    private static final String SI_CHECKIN_PAGE_NUMBER = "checkin page number";

    private enum Views {
        SPLASH_SCREEN, CHECKIN, NOT_CHECKED_IN_YET, SUMMARY
    }

    private Views currentView = null;
    private Integer currentQuestionPage = 0;
    private static final int ANIMATION_DURATION = 250;
    private void setView(Views showView) { setView(showView, true); }
    private void setView(Views showView, boolean animate) {
        if(showView != currentView) {
            if (currentView != null) {
                // Hide animations
                switch (currentView) {
                    case CHECKIN:
                        if(animate) {
                            rl_checkin.setAlpha(1);
                            rl_checkin.setTranslationY(0);
                            rl_checkin.setVisibility(View.VISIBLE);
                            rl_checkin.animate()
                                    .alpha(0)
                                    .translationY(Styling.getInDP(this, 100))
                                    .setDuration(ANIMATION_DURATION)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            super.onAnimationEnd(animation);
                                            rl_checkin.setVisibility(View.GONE);
                                            rl_checkin.setTranslationY(0);
                                            rl_checkin.setAlpha(1);
                                        }
                                    });
                        } else
                            rl_checkin.setVisibility(View.GONE);
                        break;
                    case SPLASH_SCREEN:
                        if(animate) {
                            ll_splashScreen.setAlpha(1);
                            ll_splashScreen.setVisibility(View.VISIBLE);
                            ll_splashScreen.animate()
                                    .alpha(0)
                                    .translationY(-Styling.getInDP(this, 100))
                                    .setDuration(ANIMATION_DURATION)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            super.onAnimationEnd(animation);
                                            ll_splashScreen.setVisibility(View.GONE);
                                            ll_splashScreen.setTranslationY(0);
                                            ll_splashScreen.setAlpha(1);
                                        }
                                    });
                        } else
                            ll_splashScreen.setVisibility(View.GONE);
                        break;
                    case NOT_CHECKED_IN_YET:
                        if(animate) {
                            ll_not_checked_in.setAlpha(1);
                            ll_not_checked_in.setTranslationY(0);
                            ll_not_checked_in.setVisibility(View.VISIBLE);
                            ll_not_checked_in.animate()
                                    .alpha(0)
                                    .translationY(Styling.getInDP(this, 100))
                                    .setDuration(ANIMATION_DURATION)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            super.onAnimationEnd(animation);
                                            ll_not_checked_in.setVisibility(View.GONE);
                                            ll_not_checked_in.setTranslationY(0);
                                            ll_not_checked_in.setAlpha(1);
                                        }
                                    });
                        } else {
                            ll_not_checked_in.setVisibility(View.GONE);
                        }
                        break;
                    case SUMMARY:
                        if(animate) {
                            fl_checkin_summary.setAlpha(1);
                            fl_checkin_summary.setTranslationY(0);
                            fl_checkin_summary.setVisibility(View.VISIBLE);
                            fl_checkin_summary.animate()
                                    .alpha(0)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            super.onAnimationEnd(animation);
                                            fl_checkin_summary.setVisibility(View.GONE);
                                            fl_checkin_summary.setAlpha(1);
                                        }
                                    });
                        } else
                            fl_checkin_summary.setVisibility(View.GONE);
                        break;
                }
            }
            // Show animations
            switch (showView) {
                case CHECKIN:
                    if(animate) {
                        rl_checkin.setAlpha(0);
                        rl_checkin.setTranslationY(Styling.getInDP(this, 100));
                        rl_checkin.setVisibility(View.VISIBLE);
                        rl_checkin.animate()
                                .alpha(1)
                                .translationY(0)
                                .setDuration(ANIMATION_DURATION);
                    } else
                        rl_checkin.setVisibility(View.VISIBLE);
                    break;
                case SPLASH_SCREEN:
                    if(animate) {
                        ll_splashScreen.setAlpha(0);
                        ll_splashScreen.setVisibility(View.VISIBLE);
                        ll_splashScreen.animate()
                                .alpha(1)
                                .translationY(Styling.getInDP(this, 100));
                    } else
                        ll_splashScreen.setVisibility(View.VISIBLE);
                    break;
                case NOT_CHECKED_IN_YET:
                    if(animate) {
                        ll_not_checked_in.setAlpha(0);
                        ll_not_checked_in.setTranslationY(Styling.getInDP(this, 100));
                        ll_not_checked_in.setVisibility(View.VISIBLE);
                        ll_not_checked_in.animate()
                                .alpha(1)
                                .translationY(0)
                                .setDuration(ANIMATION_DURATION);
                    } else {
                        ll_not_checked_in.setVisibility(View.VISIBLE);
                    }
                    break;
                case SUMMARY:
                    if(animate) {
                        fl_checkin_summary.setAlpha(0);
                        fl_checkin_summary.setVisibility(View.VISIBLE);
                        fl_checkin_summary.setTranslationY(Styling.getInDP(this, 100));
                        fl_checkin_summary.animate()
                                .translationY(0)
                                .alpha(1);
                    } else {
                        fl_checkin_summary.setVisibility(View.VISIBLE);
                    }
                    break;
            }
        }
        currentView = showView;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Styling.forcePortraitOnSmallDevices(this);
        setContentView(R.layout.activity_home);
        flareDownAPI = new API(getApplicationContext());
        if(!flareDownAPI.isLoggedIn()) { // Ensure the user is signed in.
            new ForceLogin(this, flareDownAPI);
            return;
        }
        Styling.setFont(); // Uses the Calligraphy library inject the font.
        assignViews();
        setLocales();
        initialise();

        if(savedInstanceState != null && savedInstanceState.containsKey(SI_CURRENT_VIEW) && savedInstanceState.containsKey(SI_CHECKIN_DATE)) { // Restore previous activity.
            Views savedViewState = (Views) savedInstanceState.getSerializable(SI_CURRENT_VIEW);
            Date savedCheckinDate = new Date(savedInstanceState.getLong(SI_CHECKIN_DATE));
            setView(savedViewState, false);
            if(savedInstanceState.containsKey(SI_CHECKIN_PAGE_NUMBER) && savedInstanceState.containsKey(SI_ENTRIES_JSON) && savedInstanceState.containsKey(SI_RESPONSE_JSON)) {
                try {
                    JSONObject entriesJObject = new JSONObject(savedInstanceState.getString(SI_ENTRIES_JSON));
                    JSONArray responseJArray = new JSONArray(savedInstanceState.getString(SI_RESPONSE_JSON));
                    List<EntryParsers.CollectionCatalogDefinition> collectionCatalogDefinitions = EntryParsers.getCatalogDefinitions(entriesJObject, responseJArray);
                    displayCheckin(savedCheckinDate, collectionCatalogDefinitions);
                    if(savedViewState == Views.SUMMARY) {
                        //displaySummary(collectionCatalogDefinitions, savedCheckinDate );
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            setView(Views.SPLASH_SCREEN);
            displayCheckin(new Date());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(collectionCatalogDefinitions != null) {
            outState.putString(SI_ENTRIES_JSON, EntryParsers.getCatalogDefinitionsJSON(collectionCatalogDefinitions).toString());
            outState.putString(SI_RESPONSE_JSON, EntryParsers.getResponsesJSONCatalogDefinitionList(collectionCatalogDefinitions).toString());
            outState.putLong(SI_CHECKIN_DATE, checkinDate.getTime());
            outState.putInt(SI_CHECKIN_PAGE_NUMBER, currentQuestionPage);
        }
        outState.putSerializable(SI_CURRENT_VIEW, currentView);
    }

    /**
     * Finds the views of relevant views and assigns it's corresponding variable.
     */
    private void assignViews() {
        bt_nextQuestion = (Button) findViewById(R.id.bt_nextQuestion);
        bt_prevQuestion = (Button) findViewById(R.id.bt_prevQuestion);
        bt_submitCheckin = (Button) findViewById(R.id.bt_submitCheckin);
        bt_not_checked_in_checkin = (Button) findViewById(R.id.bt_not_checked_in_checkin);
        vp_questions = (ViewPager) findViewById(R.id.vp_questionPager);

        tv_not_checked_in_checkin = (TextView) findViewById(R.id.tv_not_checked_in_checkin);

        mainToolbarView = (MainToolbarView) findViewById(R.id.main_toolbar_view);
        toolbar = (Toolbar) findViewById(R.id.toolbar_top);
        toolbarTitle = (TextView) findViewById(R.id.toolbar_title);
        ll_not_checked_in = (LinearLayout) findViewById(R.id.ll_not_checked_in);
        ll_splashScreen = (LinearLayout) findViewById(R.id.ll_splashScreen);
        rl_checkin = (RelativeLayout) findViewById(R.id.rl_checkin);
        fl_checkin_summary = (FrameLayout) findViewById(R.id.fl_checkin_summary);
    }

    private void initialise() {
        //Set up the toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        bt_not_checked_in_checkin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setView(Views.CHECKIN);
            }
        });

        vp_questions.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position <= 0) {
                    bt_prevQuestion.setVisibility(View.INVISIBLE);
                    bt_submitCheckin.setVisibility(View.GONE);
                    bt_nextQuestion.setVisibility(View.VISIBLE);
                } else if (position >= vpa_questions.getFragments().size() - 1) {
                    bt_nextQuestion.setVisibility(View.GONE);
                    bt_prevQuestion.setVisibility(View.VISIBLE);
                    bt_submitCheckin.setVisibility(View.VISIBLE);
                } else {
                    bt_submitCheckin.setVisibility(View.GONE);
                    bt_nextQuestion.setVisibility(View.VISIBLE);
                    bt_prevQuestion.setVisibility(View.VISIBLE);
                }

                if (currentQuestionPage != null)
                    vpa_questions.getFragments().get(currentQuestionPage).onPageExit();
                vpa_questions.getFragments().get(position).onPageEnter();
                currentQuestionPage = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        bt_nextQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vp_questions.setCurrentItem(vp_questions.getCurrentItem() + 1, true);
            }
        });
        bt_prevQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vp_questions.setCurrentItem(vp_questions.getCurrentItem() - 1, true);
            }
        });

        bt_submitCheckin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitCheckin();
                displaySummary(collectionCatalogDefinitions, checkinDate);
                setView(Views.SUMMARY);
            }
        });
    }

    /**
     * Submits the checkin to flaredown and displays the summary page.
     */
    private void submitCheckin() {
        flareDownAPI.submitEntry(checkinDate, EntryParsers.getResponsesJSONCatalogDefinitionList(collectionCatalogDefinitions), new API.OnApiResponse<JSONObject>() {
            @Override
            public void onFailure(API_Error error) {
                new DefaultErrors(getApplicationContext(), error);
            }

            @Override
            public void onSuccess(JSONObject result) {
                Toast.makeText(getApplicationContext(), "Submission was a success", Toast.LENGTH_LONG).show(); //TODO show summary instead.
            }
        });
    }

    private void displaySummary(List<EntryParsers.CollectionCatalogDefinition> collectionCatalogDefinitions, Date date) {
        f_checkin_sumary = Checkin_summary_fragment.newInstance(EntryParsers.getCatalogDefinitionsJSON(collectionCatalogDefinitions), EntryParsers.getResponsesJSONCatalogDefinitionList(collectionCatalogDefinitions), date);
        fl_checkin_summary.removeAllViews();
        getSupportFragmentManager().beginTransaction().add(fl_checkin_summary.getId(), f_checkin_sumary, "sumary").commit();
    }

    /**
     * Set all the text locales inside the activity
     */
    private void setLocales() { // TODO set locales for the activity
        //bt_not_checked_in_checkin.setText(Locales.read(this, "onboarding.checkin").createAT());
        //tv_not_checked_in_checkin.setText(Locales.read(this, "you_havent_checked_in_yet").createAT());
    }

    /**
     * Display the check in for a specific date.
     * @param date The date for the check in.
     */
    private void displayCheckin(final Date date) { // TODO implement loading screen.
        toolbarTitle.setText(Styling.displayDateLong(date));
        checkinDate = date;
        flareDownAPI.entries(date, new API.OnApiResponse<JSONObject>() {
            @Override
            public void onFailure(API_Error error) {
                new DefaultErrors(getApplicationContext(), error);
            }

            @Override
            public void onSuccess(JSONObject result) {
                try {
                    JSONObject entryJObject = result.getJSONObject("entry");
                    if (!entryJObject.has("responses"))
                        entryJObject.put("responses", new JSONArray());
                    List<EntryParsers.CollectionCatalogDefinition> ccds = EntryParsers.getCatalogDefinitions(entryJObject.getJSONObject("catalog_definitions"), entryJObject.getJSONArray("responses"));
                    displayCheckin(date, ccds);
                } catch (JSONException e) {
                    e.printStackTrace();
                    new DefaultErrors(getApplicationContext(), new API_Error().setStatusCode(500).setDebugString("CheckinActivity.displayCheckin(Entry JSON has no entry object)"));
                }
            }
        });
    }

    /**
     * Display the check in for a specific date, passing the entries json object.
     * @param date The date for the check in.
     * @param collectionCatalogDefinitions Prefetched entries json object, including response.
     */
    private void displayCheckin(final Date date, List<EntryParsers.CollectionCatalogDefinition> collectionCatalogDefinitions) {
        checkinDate = date;
        toolbarTitle.setText(Styling.displayDateLong(date));
        if(currentView == Views.SPLASH_SCREEN) setView(Views.NOT_CHECKED_IN_YET);
        this.collectionCatalogDefinitions = collectionCatalogDefinitions;
        List<ViewPagerFragmentBase> fragments = createFragments(collectionCatalogDefinitions);
        for (ViewPagerFragmentBase fragment : fragments) {
            fragment.addOnUpdateListener(new ViewPagerFragmentBase.OnResposneUpdate() {
                @Override
                public void onUpdate(EntryParsers.CatalogDefinition catalogDefinition) {
                    try {
                        if (catalogDefinition.getResponse() != null) {
                            EntryParsers.findCatalogDefinition(CheckinActivity.this.collectionCatalogDefinitions, catalogDefinition.getCatalog(), catalogDefinition.getName()).setResponse(catalogDefinition.getResponse());
                        }
                    } catch (NullPointerException e) {e.printStackTrace();}
                }
            });
        }
        vpa_questions = new ViewPagerAdapter(getSupportFragmentManager(), fragments);
        vp_questions.setAdapter(vpa_questions);
    }

    /**
     * Creates a list array of fragment objects for each question specified in the JSON object
     * @param collectionCatalogDefinitions Specification for each question.
     * @return A list array of Fragments extending the View Pager Fragment.
     * @throws JSONException
     */
    public static List<ViewPagerFragmentBase> createFragments(List<EntryParsers.CollectionCatalogDefinition> collectionCatalogDefinitions) {
        List<ViewPagerFragmentBase> fragments = new ArrayList<>();
        String currentCatalog = null;
        Integer section = 1;
        for (EntryParsers.CollectionCatalogDefinition collectionCatalogDefinition : collectionCatalogDefinitions) {
            if(EntryParsers.CATALOG_NAMES.indexOf(collectionCatalogDefinition.getCatalog()) == -1) { // Check that it isn't a grouped catalog.
                if(!collectionCatalogDefinition.getCatalog().equals(currentCatalog)) {
                    section = 1;
                    currentCatalog = collectionCatalogDefinition.getCatalog(); // Add a new class to contain groups of catalog definitions.
                }
                CheckinCatalogQFragment checkinCatalogQFragment = new CheckinCatalogQFragment();
                checkinCatalogQFragment.setQuestions(new ArrayList<EntryParsers.CollectionCatalogDefinition>(Arrays.asList(collectionCatalogDefinition)), section);
                fragments.add(checkinCatalogQFragment);
                section++;
            }
        }
        for (String catalogName : EntryParsers.CATALOG_NAMES) { // Handling the grouped catalogs.
            if(!catalogName.equals("treatments")) {
                CheckinCatalogQFragment checkinCatalogQFragment = new CheckinCatalogQFragment();
                checkinCatalogQFragment.setQuestions(EntryParsers.getCatalogDefinitions(catalogName, collectionCatalogDefinitions), 0);
                fragments.add(checkinCatalogQFragment);
            }
        }
        return fragments;
    }

    public List<ViewPagerFragmentBase> getFragmentQuestions() {
        return vpa_questions.getFragments();
    }
    public ViewPagerAdapter getScreenSlidePagerAdapter() {
        return vpa_questions;
    }

    public void setOnActivityResultListener(OnActivityResultListener onActivityResultListener){
        this.onActivityResultListener = onActivityResultListener;
    }

    public interface OnActivityResultListener {
        void onActivityResult(int requestCode, int resultCode, Intent data);
    }

    /**
     * The adapter for the view pager, used to display the questions.
     */
    public static class ViewPagerAdapter extends FragmentStatePagerAdapter {
        private List<ViewPagerFragmentBase> fragments;
        private OnPageViewCountListener onPageViewCountListener;

        /**
         * Constructs the custom view pager adapte with fragments that extent the ViewPagerFragmentBase class.
         * @param fragmentManager
         * @param fragments The fragments to display in view pager.
         */
        public ViewPagerAdapter(FragmentManager fragmentManager, List<ViewPagerFragmentBase> fragments) {
            super(fragmentManager);
            this.fragments = fragments;
        }

        /**
         * Add a fragment to the view pager.
         * @param viewPagerFragmentBase The fragment to add to the view pager.
         * @param index the location of the index.
         */
        public void addView(ViewPagerFragmentBase viewPagerFragmentBase, int index) {
            fragments.add(index, viewPagerFragmentBase);
            notifyDataSetChanged();
            triggerOnViewPageCountChange();
        }

        /**
         * Remove a fragment at a specific location.
         * @param index The index of the fragment to remove.
         */
        public void removeFragment(int index) {
            fragments.remove(index);
            notifyDataSetChanged();
            triggerOnViewPageCountChange();
        }

        /**
         * Remove all fragments from the view pager.
         */
        public void removeAllFragments() {
            fragments = new ArrayList<>();
            notifyDataSetChanged();
            triggerOnViewPageCountChange();
        }

        private void triggerOnViewPageCountChange() {
            if(this.onPageViewCountListener != null)
                this.onPageViewCountListener.onPageViewCountChange(fragments.size());
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        /*
                                    Getters and setters.
                                 */
        public List<ViewPagerFragmentBase> getFragments() {
            return fragments;
        }

        public void setFragments(List<ViewPagerFragmentBase> viewPagerFragmentBase) {
            this.fragments = viewPagerFragmentBase;
            notifyDataSetChanged();
            triggerOnViewPageCountChange();
        }

        public void setOnPageViewCountListener(OnPageViewCountListener onPageViewCountListener) {
            this.onPageViewCountListener = onPageViewCountListener;
        }
    }

    /**
     * An event listener which is triggered when the page size changes for a view pager.
     */
    public interface OnPageViewCountListener {
        void onPageViewCountChange(int size);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Used to inject a custom font with Calligraphy library to the activity.
     * @param newBase
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(onActivityResultListener != null) {
            onActivityResultListener.onActivityResult(requestCode, resultCode, data);
            onActivityResultListener = null;
        }
    }

}
