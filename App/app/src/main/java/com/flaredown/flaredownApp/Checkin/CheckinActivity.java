package com.flaredown.flaredownApp.Checkin;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.flaredown.flaredownApp.Checkin.tags.TagFragment;
import com.flaredown.flaredownApp.Helpers.APIv2.APIResponse;
import com.flaredown.flaredownApp.Helpers.APIv2.Communicate;
import com.flaredown.flaredownApp.Helpers.APIv2.EndPoints.CheckIns.CheckIn;
import com.flaredown.flaredownApp.Helpers.APIv2.EndPoints.CheckIns.Trackable;
import com.flaredown.flaredownApp.Helpers.APIv2.EndPoints.CheckIns.TrackableType;
import com.flaredown.flaredownApp.Helpers.APIv2.EndPoints.Trackings.Tracking;
import com.flaredown.flaredownApp.Helpers.APIv2.Error;
import com.flaredown.flaredownApp.Helpers.APIv2.ErrorDialog;
import com.flaredown.flaredownApp.Helpers.AndroidViewAnimation.ViewAnimationHelper;
import com.flaredown.flaredownApp.Helpers.FlaredownConstants;
import com.flaredown.flaredownApp.Helpers.Observers.ImmutableObserver;
import com.flaredown.flaredownApp.Helpers.PreferenceKeys;
import com.flaredown.flaredownApp.Helpers.Styling.SnackbarStyling;
import com.flaredown.flaredownApp.Helpers.Styling.Styling;
import com.flaredown.flaredownApp.Login.ForceLogin;
import com.flaredown.flaredownApp.R;
import com.flaredown.flaredownApp.Toolbars.MainToolbarView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import rx.Subscriber;

public class CheckinActivity extends AppCompatActivity {
    public Communicate API;
    private static final String DEBUG_KEY = "CHECK_IN";
    public static final int ANIMATION_DURATION = 250;


    // Application state variables.... (Static so persistent across instances, should also be valid at ALL times).
    private static CheckIn checkIn;
    private static List<Trackable> checkInIdUpdate = new ArrayList<>(); // List of trackables which need there individual id updating on check in submission.
    private static ImmutableObserver<Boolean> isLoadingCheckIn = new ImmutableObserver<>(false);

    // Intent keys.
    public static final String I_CHECK_IN_ID = "launch_intent_check_in_id";

    // View variables.
    private ImageButton bt_nextQuestion;
    private ImageButton bt_prevQuestion;
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


    private Integer currentQuestionPage;
    private ViewAnimationHelper<VIEW_STATES> vsAnimationHelper = new ViewAnimationHelper<>();
    private Calendar lastUpdate;
    private final ImmutableObserver<Boolean> isActivityPaused = new ImmutableObserver<>(false);

    private enum VIEW_STATES {
        SPLASH_SCREEN, CHECK_IN, SUMMARY
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Styling.forcePortraitOnSmallDevices(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.checkin_activity);
        Intent launchIntent = getIntent();
        API = new Communicate(this);
        if (!API.isCredentialsSaved()) {
            new ForceLogin(this);
            return;
        }
        Styling.setFont(); // Uses the Calligraphy library to inject the font.
        assignViews(); // Assign all the variables which are associated with view.

        // Set up the tool bar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Click listener for the not check in yet button.
        bt_not_checked_in_checkin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO make the "You haven't checked in yet" into a page and display that way.
                Answers.getInstance().logCustom(new CustomEvent("New Check In"));
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
                displaySummary();
                vsAnimationHelper.changeState(VIEW_STATES.SUMMARY, true);
            }
        });
        mainToolbarView.setNextOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isLoadingCheckIn.getValue()) {
                    Calendar c = (Calendar) checkIn.getDate().clone();
                    c.add(Calendar.DATE, 1);
                    displayCheckin(c);
                }
            }
        });
        mainToolbarView.setPrevOnClickListner(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isLoadingCheckIn.getValue()) {
                    Calendar c = (Calendar) checkIn.getDate().clone();
                    c.add(Calendar.DATE, -1);
                    displayCheckin(c);
                }
            }
        });

        // Animation Sequences
        vsAnimationHelper.addState(VIEW_STATES.CHECK_IN, new ViewAnimationHelper.AnimationEvents(new ViewAnimationHelper.Animation() {
            @Override
            public void start(boolean animate, final ViewAnimationHelper.AnimationEndListener animationEndListener) {
                if (animate) {
                    rl_checkin.setAlpha(0);
                    rl_checkin.setTranslationY(Styling.getInDP(CheckinActivity.this, 100));
                    rl_checkin.setVisibility(View.VISIBLE);
                    rl_checkin.animate()
                            .alpha(1)
                            .translationY(0)
                            .setDuration(ANIMATION_DURATION)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    animationEndListener.addProgress(1);
                                }
                            });
                } else
                    rl_checkin.setVisibility(View.VISIBLE);
            }
        }, new ViewAnimationHelper.Animation() {
            @Override
            public void start(boolean animate, final ViewAnimationHelper.AnimationEndListener animationEndListener) {
                if (animate) {
                    rl_checkin.setAlpha(1);
                    rl_checkin.setTranslationY(0);
                    rl_checkin.setVisibility(View.VISIBLE);
                    rl_checkin.animate()
                            .alpha(0)
                            .translationY(Styling.getInDP(CheckinActivity.this, 100))
                            .setDuration(ANIMATION_DURATION)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    rl_checkin.setVisibility(View.GONE);
                                    rl_checkin.setTranslationY(0);
                                    rl_checkin.setAlpha(1);
                                    animationEndListener.addProgress(1);
                                }
                            });
                } else {
                    rl_checkin.setVisibility(View.GONE);
                }
            }
        }, false));

        vsAnimationHelper.addState(VIEW_STATES.SUMMARY, new ViewAnimationHelper.AnimationEvents(new ViewAnimationHelper.Animation() {
            @Override
            public void start(boolean animate, final ViewAnimationHelper.AnimationEndListener animationEndListener) {
                if (animate) {
                    fl_checkin_summary.setAlpha(0);
                    fl_checkin_summary.setVisibility(View.VISIBLE);
                    fl_checkin_summary.setTranslationY(Styling.getInDP(CheckinActivity.this, 100));
                    fl_checkin_summary.animate()
                            .translationY(0)
                            .alpha(1)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    animationEndListener.addProgress(1);
                                    fl_checkin_summary.setAlpha(1);
                                    fl_checkin_summary.setVisibility(View.VISIBLE);
                                }
                            });
                } else {
                    fl_checkin_summary.setVisibility(View.VISIBLE);
                }
            }
        }, new ViewAnimationHelper.Animation() {
            @Override
            public void start(boolean animate, final ViewAnimationHelper.AnimationEndListener animationEndListener) {
                if (animate) {
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
                                    animationEndListener.addProgress(1);
                                }
                            });
                } else
                    fl_checkin_summary.setVisibility(View.GONE);
            }
        }, false));

        vsAnimationHelper.addState(VIEW_STATES.SPLASH_SCREEN, new ViewAnimationHelper.AnimationEvents(new ViewAnimationHelper.Animation() {
            @Override
            public void start(boolean animate, final ViewAnimationHelper.AnimationEndListener animationEndListener) {
                if (animate) {
                    ll_splashScreen.setAlpha(0);
                    ll_splashScreen.setVisibility(View.VISIBLE);
                    ll_splashScreen.setTranslationY(0);
                    ll_splashScreen.animate()
                            .alpha(1)
                            .translationY(Styling.getInDP(CheckinActivity.this, 0))
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    animationEndListener.addProgress(1);
                                }
                            });
                } else
                    ll_splashScreen.setVisibility(View.VISIBLE);
            }
        }, new ViewAnimationHelper.Animation() {
            @Override
            public void start(boolean animate, final ViewAnimationHelper.AnimationEndListener animationEndListener) {
                if (animate) {
                    ll_splashScreen.setAlpha(1);
                    ll_splashScreen.setVisibility(View.VISIBLE);
                    ll_splashScreen.animate()
                            .alpha(0)
                            .translationY(-Styling.getInDP(CheckinActivity.this, 100))
                            .setDuration(ANIMATION_DURATION)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    ll_splashScreen.setVisibility(View.GONE);
                                    ll_splashScreen.setTranslationY(0);
                                    ll_splashScreen.setAlpha(1);
                                    animationEndListener.addProgress(1);
                                }
                            });
                } else
                    ll_splashScreen.setVisibility(View.GONE);
            }
        }, false));


        // If no check in set, display one
        if (checkIn == null) { // TODO handle intent
            displayCheckin(Calendar.getInstance());
        } else {
            displayCheckin(checkIn);
        }
    }

    /**
     * Assign all the variables which are associated with the view.
     */
    private void assignViews() {
        bt_nextQuestion = (ImageButton) findViewById(R.id.bt_nextQuestion);
        bt_prevQuestion = (ImageButton) findViewById(R.id.bt_prevQuestion);
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

    private class ViewPagerAdapter extends FragmentPagerAdapter {
        private List<ViewPagerFragmentBase> fragments;
        private FragmentManager fragmentManager;

        /**
         * Constructs the custom view pager adapter with fragments that extend the ViewPagerFragment class.
         *
         * @param fragmentManger
         * @param fragments      The fragments to display in view pager.
         */
        public ViewPagerAdapter(FragmentManager fragmentManger, List<ViewPagerFragmentBase> fragments) {
            super(fragmentManger);
            this.fragmentManager = fragmentManger;
            this.fragments = fragments;
        }

        private void attachFragments() {
            final FragmentTransaction transaction = fragmentManager.beginTransaction();
            for (int i = 0; i < fragments.size(); i++) {
                transaction.show(fragments.get(i));
            }
            transaction.commitAllowingStateLoss();
        }

        /**
         * Remove all fragments from the view pager.
         */
        public void removeAllFragments() {
            final FragmentTransaction transaction = fragmentManager.beginTransaction();
            for (int i = 0; i < fragments.size(); i++) {
                transaction.remove(fragments.get(i));
            }
            transaction.commitAllowingStateLoss();
            fragments.clear();
            notifyDataSetChanged();
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

        public List<ViewPagerFragmentBase> getFragments() {
            return fragments;
        }

        public void setFragments(List<ViewPagerFragmentBase> fragments) {
            this.fragments = fragments;
            attachFragments();
            notifyDataSetChanged();
        }
    }

    /**
     * Get the check in object for the activity.
     *
     * @return Check in object for the activity.
     */
    public CheckIn getCheckIn() {
        return checkIn;
    }

    public void checkInUpdate() { // TODO reduce update frequencies.
        final Calendar updateTime = lastUpdate = Calendar.getInstance();
        API.submitCheckin(checkIn, new APIResponse<CheckIn, Error>() {
            @Override
            public void onSuccess(CheckIn result) {
                // For removing once the id has been found.
                List<Trackable> foundIdTrackables = new ArrayList<Trackable>();
                for (Trackable trackable : checkInIdUpdate) {
                    for (Trackable trackable1 : result.getTrackables(trackable.getType())) {
                        try {
                            if (trackable.getMetaTrackable().getId().equals(trackable1.getMetaTrackable().getId())) {
                                foundIdTrackables.add(trackable);
                                trackable.setId(trackable1.getId());
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                }
                checkInIdUpdate.removeAll(foundIdTrackables); // Remove found trackables... otherwise wait for the next check in (shouldn't ever happen just in case).


                PreferenceKeys.log(PreferenceKeys.LOG_D, DEBUG_KEY, "Check in saved successfully");
                if (isActivityPaused().getValue()) {
                    Toast.makeText(getApplicationContext(), getResources().getText(R.string.locales_summary_title), Toast.LENGTH_SHORT).show();
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(Calendar.getInstance().getTimeInMillis() - updateTime.getTimeInMillis() + 1000);
                                if (updateTime.equals(lastUpdate))
                                    SnackbarStyling.colorSnackBar(Snackbar.make(findViewById(R.id.cl_root_view), R.string.locales_summary_title, Snackbar.LENGTH_SHORT), getResources().getColor(R.color.background)).show();
                            } catch (InterruptedException e) {
                            }
                        }
                    }).start();
                }
            }

            @Override
            public void onFailure(Error result) {
                new ErrorDialog(CheckinActivity.this, result).setCancelable(false).show();
            }
        });
    }

    /**
     * Get the check in and display the check in for a specific date.
     *
     * @param date The date for the check in to load.
     */
    private void displayCheckin(final Calendar date) {
        removeSummary();
        vsAnimationHelper.changeState(VIEW_STATES.SPLASH_SCREEN, true);
        // TODO update date buttons via observer.
        isLoadingCheckIn.setValue(true);
        // Load the check in object and display.
        API.checkIn(date, new APIResponse<CheckIn, Error>() {
            @Override
            public void onSuccess(CheckIn result) {
                isLoadingCheckIn.setValue(false);
                displayCheckin(result);
            }

            @Override
            public void onFailure(Error result) {
                new ErrorDialog(CheckinActivity.this, result).setCancelable(false).show();
            }
        });
    }

    /**
     * Get the check in and display the check in for a specific id.
     *
     * @param id The id for the check in to load.
     */
    private void displayCheckin(final String id) {
        removeSummary();
        vsAnimationHelper.changeState(VIEW_STATES.SPLASH_SCREEN, true);
        isLoadingCheckIn.setValue(true);
        API.checkIn(id, new APIResponse<CheckIn, Error>() {
            @Override
            public void onSuccess(CheckIn result) {
                isLoadingCheckIn.setValue(false);
                displayCheckin(result);
            }

            @Override
            public void onFailure(Error result) {
                new ErrorDialog(CheckinActivity.this, result).setCancelable(false).show();
            }
        });
    }

    /**
     * Display the check in from a chack in object.
     *
     * @param checkIn The object for the check in to display.
     */
    private void displayCheckin(final CheckIn checkIn) {
        removeSummary();
        CheckinActivity.checkIn = checkIn;
        checkIn.getCheckInChangeObservable().subscribe(new Subscriber<Void>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Void aVoid) {
                checkInUpdate();
            }
        });
        if (checkIn.hasResponse()) {
            displaySummary();
            return;
        } else {
            vsAnimationHelper.changeState(VIEW_STATES.CHECK_IN, true);
        }
        List<ViewPagerFragmentBase> fragments = createFragments();
        if (vpa_questions == null) {
            vpa_questions = new ViewPagerAdapter(getSupportFragmentManager(), fragments);
            vp_questions.setAdapter(vpa_questions);
        } else {
            vpa_questions.removeAllFragments();
            vpa_questions.setFragments(fragments);
            vp_questions.setCurrentItem(0, false);
        }
    }

    private void displaySummary() {
        try {
            removeSummary();
            f_checkin_sumary = CheckInSummaryFragment.newInstance();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            transaction.replace(fl_checkin_summary.getId(), f_checkin_sumary);
            transaction.commitAllowingStateLoss();

            if (vpa_questions != null) {
                vpa_questions.removeAllFragments();
            }
            vsAnimationHelper.changeState(VIEW_STATES.SUMMARY, true);
        } catch (Exception e) {
            new ErrorDialog(CheckinActivity.this, new Error().setExceptionThrown(e).setDebugString("CheckinActivity:displaySummary...DISPLAYSUMMARY")).setCancelable(false).show();
        }
    }

    /**
     * Remove the summary fragment (including fragment transaction removal and setting field to null).
     */
    private void removeSummary() {
        if (f_checkin_sumary != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.remove(f_checkin_sumary);
            transaction.commitAllowingStateLoss();
            f_checkin_sumary = null;
        }
    }

    private void updateLocalCheckInAndUI(final Trackable trackable) {
        // When the check in is updated because of the statement below it's id is updated from the returned check in.
        checkInIdUpdate.add(trackable);
        // Updates the model which automatically submits the check in.
        checkIn.getTrackables(trackable.getType()).add(trackable);
    }

    /**
     * Creates a list of fragment objects for each page (for example, conditions, treatments,
     * symptoms and tags.
     *
     * @return List of fragment objects for each page of the check in (not the extra pages inside
     * the summary fragment).
     */
    public static List<ViewPagerFragmentBase> createFragments() {
        List<ViewPagerFragmentBase> fragments = new ArrayList<>();
        for (TrackableType trackableType : TrackableType.trackableValues()) {
            CheckinCatalogQFragment checkinCatalogQFragment = CheckinCatalogQFragment.newInstance(trackableType);
            fragments.add(checkinCatalogQFragment);
        }
        fragments.add(TagFragment.newInstance());
        return fragments;
    }


    @Override
    protected void onPause() {
        super.onPause();
        isActivityPaused.setValue(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityPaused.setValue(false);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == FlaredownConstants.ADD_TRACKABLE_REQUEST_CODE) {
                // Retrieve trackable from intent.
                if (data.hasExtra(FlaredownConstants.RETURN_TRACKABLE_KEY)) {
                    Bundle bundle = data.getExtras();
                    final Trackable trackable = (Trackable) bundle.get(FlaredownConstants.RETURN_TRACKABLE_KEY);
                    if (trackable != null) {
                        trackable.setCheckInId(checkIn.getId());
                        if (DateUtils.isToday(checkIn.getDate().getTimeInMillis())) {
                            // If check in is today, add trackable to /tracking
                            Tracking tracking = new Tracking();
                            tracking.setTrackable_id(trackable.getTrackableId());
//                            tracking.setId(trackable.getId());
                            tracking.setTrackable_type(trackable.getType());
                            API.submitTracking(tracking, new APIResponse<Tracking, Error>() {
                                @Override
                                public void onSuccess(Tracking result) {
                                    updateLocalCheckInAndUI(trackable);
                                }

                                @Override
                                public void onFailure(Error result) {
                                    new ErrorDialog(CheckinActivity.this, result).setCancelable(false).show();
                                }
                            });
                        } else {
                            try {
                                updateLocalCheckInAndUI(trackable);
                            } catch (IllegalStateException e) {
                                new ErrorDialog(this, new Error().setExceptionThrown(e).setDebugString("AddEditableActivity result exception catched.").setRetryRunnable(new Runnable() {
                                    @Override
                                    public void run() {
                                        onActivityResult(requestCode, resultCode, data);
                                    }
                                })).setCancelable(false).show();
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns true if the activity is paused.
     *
     * @return True if the activity is paused.
     */
    public ImmutableObserver<Boolean> isActivityPaused() {
        return isActivityPaused;
    }
}