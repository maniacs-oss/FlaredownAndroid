package com.flaredown.flaredownApp.Activities.Checkin;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.flaredown.flaredownApp.Activities.Checkin.tags.TagFragment;
import com.flaredown.flaredownApp.Helpers.APIv2.APIResponse;
import com.flaredown.flaredownApp.Helpers.APIv2.Communicate;
import com.flaredown.flaredownApp.Helpers.APIv2.EndPoints.CheckIns.CheckIn;
import com.flaredown.flaredownApp.Helpers.APIv2.EndPoints.CheckIns.Tag;
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
import com.flaredown.flaredownApp.Activities.Login.ForceLogin;
import com.flaredown.flaredownApp.Activities.Main.MainActivity;
import com.flaredown.flaredownApp.R;
import com.flaredown.flaredownApp.Toolbars.MainToolbarView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import rx.Subscriber;
import rx.functions.Action1;

public class CheckinFragment extends Fragment {
    private Communicate API;
    private Context mContext;
    private static final String DEBUG_KEY = "CHECK_IN";
    public static final int ANIMATION_DURATION = 250;
    private boolean isActivityDestroyed = false;

    // Save instance state static keys.
    private final static String IS_CHECK_IN = "check in";
    private final static String IS_CURRENT_VIEW = "current view";


    // Application state variables.... (Static so persistent across instances, should also be valid at ALL times).
    private static ImmutableObserver<CheckIn> checkIn = new ImmutableObserver<>(null);
    private static List<Trackable> checkInIdUpdate = new ArrayList<>(); // List of trackables which need there individual id updating on check in submission.
    private static ImmutableObserver<Boolean> isLoadingCheckIn = new ImmutableObserver<>(false);

    // Intent keys.
    public static final String I_CHECK_IN_ID = "launch_intent_check_in_id";

    // View variables.
    private ImageButton bt_nextQuestion;
    private ImageButton bt_prevQuestion;
    private Button bt_submitCheckin;


    private MainToolbarView mainToolbarView;
    private Toolbar toolbar;
    private TextView toolbarTitle;
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

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-graphical fragments can return null (which
     * is the default implementation).  This will be called between
     * {@link #onCreate(Bundle)} and {@link #onActivityCreated(Bundle)}.
     * <p/>
     * <p>If you return a View from here, you will later be called in
     * {@link #onDestroyView} when the view is being released.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to.  The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.checkin_fragment,container,false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * This gives subclasses a chance to initialize themselves once
     * they know their view hierarchy has been completely created.  The fragment's
     * view hierarchy is not however attached to its parent at this point.
     *
     * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     */
    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mContext = getActivity().getApplicationContext();
        Styling.forcePortraitOnSmallDevices(getActivity());

        API = new Communicate(mContext);
        if (!API.isCredentialsSaved()) {
            new ForceLogin(getActivity());
            return;
        }
        Styling.setFont(); // Uses the Calligraphy library to inject the font.
        assignViews(view); // Assign all the variables which are associated with view.

/*        // Set up the tool bar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(false);*/

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
            }
        });
        mainToolbarView.setNextOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isLoadingCheckIn.getValue()) {
                    Calendar c = (Calendar) checkIn.getValue().getDate().clone();
                    c.add(Calendar.DATE, 1);
                    displayCheckin(c);
                }
            }
        });
        mainToolbarView.setPrevOnClickListner(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isLoadingCheckIn.getValue()) {
                    Calendar c = (Calendar) checkIn.getValue().getDate().clone();
                    c.add(Calendar.DATE, -1);
                    displayCheckin(c);
                }
            }
        });

        toolbarTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckInDatePickerDialogFragment datepickerFragment = new CheckInDatePickerDialogFragment();
                datepickerFragment.show(getChildFragmentManager(), "check-in-date-picker");
            }
        });

        // Animation Sequences
        vsAnimationHelper.addState(VIEW_STATES.CHECK_IN, new ViewAnimationHelper.AnimationEvents(new ViewAnimationHelper.Animation() {
            @Override
            public void start(boolean animate, final ViewAnimationHelper.AnimationEndListener animationEndListener) {
                // Run to SHOW the check in.
                if (animate) { // Animate.
                    rl_checkin.setAlpha(0);
                    rl_checkin.setTranslationY(Styling.getInDP(getActivity(), 100));
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
                } else // No animation is required.
                    rl_checkin.setVisibility(View.VISIBLE);
            }
        }, new ViewAnimationHelper.Animation() {
            @Override
            public void start(boolean animate, final ViewAnimationHelper.AnimationEndListener animationEndListener) {
                // Run to HIDE the check in.
                if (animate) { // Animate.
                    rl_checkin.setAlpha(1);
                    rl_checkin.setTranslationY(0);
                    rl_checkin.setVisibility(View.VISIBLE);
                    rl_checkin.animate()
                            .alpha(0)
                            .translationY(Styling.getInDP(getActivity(), 100))
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
                } else { // No need to animate.
                    rl_checkin.setVisibility(View.GONE);
                }
            }
        }, true));

        vsAnimationHelper.addState(VIEW_STATES.SUMMARY, new ViewAnimationHelper.AnimationEvents(new ViewAnimationHelper.Animation() {
            @Override
            public void start(boolean animate, final ViewAnimationHelper.AnimationEndListener animationEndListener) {
                // To SHOW the summary view.
                if (animate) { // Animate.
                    fl_checkin_summary.setAlpha(0);
                    fl_checkin_summary.setVisibility(View.VISIBLE);
                    fl_checkin_summary.setTranslationY(Styling.getInDP(getActivity(), 100));
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
                } else { // No need to animate.
                    fl_checkin_summary.setVisibility(View.VISIBLE);
                }
            }
        }, new ViewAnimationHelper.Animation() {
            @Override
            public void start(boolean animate, final ViewAnimationHelper.AnimationEndListener animationEndListener) {
                // To HIDE the summary view.
                if (animate) { // Animate.
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
                } else // No need to animate.
                    fl_checkin_summary.setVisibility(View.GONE);
            }
        }, true));

        vsAnimationHelper.addState(VIEW_STATES.SPLASH_SCREEN, new ViewAnimationHelper.AnimationEvents(new ViewAnimationHelper.Animation() {
            @Override
            public void start(boolean animate, final ViewAnimationHelper.AnimationEndListener animationEndListener) {
                // To SHOW the splash screen.
                if (animate) { // Animate.
                    mainToolbarView.setAlpha(1);
                    mainToolbarView.animate()
                            .alpha(0)
                            .setDuration(100)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    animationEndListener.addProgress(2);
                                }
                            });
                    ll_splashScreen.setAlpha(0);
                    ll_splashScreen.setVisibility(View.VISIBLE);
                    ll_splashScreen.setTranslationY(0);
                    ll_splashScreen.animate()
                            .alpha(1)
                            .translationY(Styling.getInDP(getActivity(), 0))
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    animationEndListener.addProgress(2);
                                }
                            });
                } else { //  No need to animate.
                    ll_splashScreen.setVisibility(View.VISIBLE);
                    mainToolbarView.setAlpha(0);
                }
            }
        }, new ViewAnimationHelper.Animation() {
            @Override
            public void start(boolean animate, final ViewAnimationHelper.AnimationEndListener animationEndListener) {
                // To HIDE the splash screen.
                if (animate) { // Animate.
                    mainToolbarView.setAlpha(0);
                    mainToolbarView.animate()
                            .alpha(1)
                            .setDuration(100)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    animationEndListener.addProgress(2);
                                }
                            });
                    ll_splashScreen.setAlpha(1);
                    ll_splashScreen.setVisibility(View.VISIBLE);
                    ll_splashScreen.animate()
                            .alpha(0)
                            .translationY(-Styling.getInDP(getActivity(), 100))
                            .setDuration(ANIMATION_DURATION)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    ll_splashScreen.setVisibility(View.GONE);
                                    ll_splashScreen.setTranslationY(0);
                                    ll_splashScreen.setAlpha(1);
                                    animationEndListener.addProgress(2);
                                }
                            });
                } else { // No need to animate.
                    ll_splashScreen.setVisibility(View.GONE);
                    mainToolbarView.setAlpha(1);
                }
            }
        }, false));

        /*
         * Observers the check in object for when it is replaced with another. When this occurs the
         * code will update the view, updating the title bar and re creating the check in fragments.
         *
         * It is done with observers as it allows previous activities which have been destroyed to
         * change the check in object and for it to be reflected in the view.
         *
         * For example. If the user rotates the phone straight after a web request is sent and the
         * response is triggered after the activity is destroyed this would cause the app to crash.
         * As it would be modifying an activity which has been destroyed. With an observer it makes
         * changes to the current running activity.
         */
        checkIn.getObservable().subscribe(new Action1<CheckIn>() {
            @Override
            public void call(CheckIn checkIn) {
                Log.d("Check-in", "Loaded");
                // Providing the activity hasn't been destroyed then display the check in.
                if(!isActivityDestroyed) {
                    removeSummary();
                    updateDateButtons(checkIn.getDate());
                    checkIn.getCheckInChangeObservable().subscribe(new Subscriber<Void>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onNext(Void aVoid) {
                            checkInUpdate(view);
                        }
                    });
                    if (checkIn.hasResponse()) {
                        Log.d("Check-in", "Displaying summary");
                        displaySummary();
                        return;
                    } else {
                        Log.d("Check-in", "Displaying check in");
                        vsAnimationHelper.changeState(VIEW_STATES.CHECK_IN, true);
                    }
                    List<ViewPagerFragmentBase> fragments = createFragments(false);
                    if (vpa_questions == null) {
                        vpa_questions = new ViewPagerAdapter(getChildFragmentManager(), fragments);
                        vp_questions.setAdapter(vpa_questions);
                    } else {
                        vpa_questions.removeAllFragments();
                        // Fixing an issue where the application would crash when the user changes date
                        // and changes pages quickly (often have to do repeatedly) The crash would
                        // state fragment already active.... https://github.com/Flaredown/FlaredownAndroid/issues/61
                        // To resolve I recreate the adapter as well as removing the fragments.
                        vpa_questions = new ViewPagerAdapter(getChildFragmentManager(), fragments);
                        vp_questions.setAdapter(vpa_questions);
                        vp_questions.setCurrentItem(0, false);
                    }
                    bt_nextQuestion.setVisibility(View.INVISIBLE);
                }
            }
        });

        if(savedInstanceState != null && savedInstanceState.containsKey(IS_CURRENT_VIEW)) {
            vsAnimationHelper.changeState((VIEW_STATES) savedInstanceState.getSerializable(IS_CURRENT_VIEW), false);
        }


        if(checkIn.getValue() != null) {
            // The check in object is static, and is the same across all activity instances.
            // Meaning if the phone is rotated (app is restarted) the checkIn object should have a
            // value.

            // This will notify the subscriber causing it to update the view... As the subscriber
            // does not run on first instance.
            displayCheckin(checkIn.getValue());

        } else if(savedInstanceState != null && savedInstanceState.containsKey(IS_CHECK_IN)) {
            // This is more precautionary, I assume it could be possible for android to have a reason
            // to clear all static variables... i.e. a app has been paused to long etc.
            // So to be safe also give the option to load the model from savedInstanceState
            displayCheckin((CheckIn) savedInstanceState.getSerializable(IS_CHECK_IN));
        } else {
            // This is run when a fresh activity is run... or there is no previous evidence of the
            // check in object so it creates one from scratch.
            displayCheckin(Calendar.getInstance());
        }
    }

    /**
     * If not in summary view turn to the next page.
     * @param animate if it should animate.
     */
    public void nextPage(boolean animate) {
        try {
            vp_questions.setCurrentItem(vp_questions.getCurrentItem() + 1, animate);
        } catch (Exception e) {} // Just in case the method is triggered before load or in summary view.
    }

    /**
     * Assign all the variables which are associated with the view.
     */
    private void assignViews(View view) {
        bt_nextQuestion = (ImageButton) view.findViewById(R.id.bt_nextQuestion);
        bt_prevQuestion = (ImageButton) view.findViewById(R.id.bt_prevQuestion);
        bt_submitCheckin = (Button) view.findViewById(R.id.bt_submitCheckin);
        //bt_not_checked_in_checkin = (Button) view.findViewById(R.id.bt_not_checked_in_checkin);
        vp_questions = (ViewPager) view.findViewById(R.id.vp_questionPager);

        //tv_not_checked_in_checkin = (TextView) view.findViewById(R.id.tv_not_checked_in_checkin);

        mainToolbarView = (MainToolbarView) view.findViewById(R.id.main_toolbar_view);
        toolbar = (Toolbar) view.findViewById(R.id.toolbar_top);
        toolbarTitle = (TextView) view.findViewById(R.id.toolbar_title);
        //ll_not_checked_in = (LinearLayout) view.findViewById(R.id.ll_not_checked_in);
        ll_splashScreen = (LinearLayout) view.findViewById(R.id.ll_splashScreen);
        rl_checkin = (RelativeLayout) view.findViewById(R.id.rl_checkin);
        fl_checkin_summary = (FrameLayout) view.findViewById(R.id.fl_checkin_summary);
    }

    private class ViewPagerAdapter extends FragmentStatePagerAdapter {
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
        return checkIn.getValue();
    }

    public void checkInUpdate(final View view) { // TODO reduce update frequencies.
        final Calendar updateTime = lastUpdate = Calendar.getInstance();
        if(!isActivityDestroyed)
            API.submitCheckin(checkIn.getValue(), new APIResponse<CheckIn, Error>() {
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
                    Toast.makeText(mContext, getResources().getText(R.string.locales_summary_title), Toast.LENGTH_SHORT).show();
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(Calendar.getInstance().getTimeInMillis() - updateTime.getTimeInMillis() + 1000);
                                if (updateTime.equals(lastUpdate))
                                    SnackbarStyling.colorSnackBar(Snackbar.make(view.findViewById(R.id.cl_root_view), R.string.locales_summary_title, Snackbar.LENGTH_SHORT), getResources().getColor(R.color.background)).show();
                            } catch (InterruptedException e) {
                            }
                        }
                    }).start();
                }
            }

            @Override
            public void onFailure(Error result) {
                new ErrorDialog(getActivity(), result).setCancelable(false).show();
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
                new ErrorDialog(getActivity(), result).setCancelable(false).show();
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
                new ErrorDialog(getActivity(), result).setCancelable(false).show();
            }
        });
    }

    /**
     * Display the check in from a chack in object.
     *
     * @param checkIn The object for the check in to display.
     */
    private void displayCheckin(final CheckIn checkIn) {
        this.checkIn.setValue(checkIn);
    }

    private void displaySummary() {
        try {
            removeSummary();
            f_checkin_sumary = CheckInSummaryFragment.newInstance();
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();

            transaction.replace(fl_checkin_summary.getId(), f_checkin_sumary);
            transaction.commitAllowingStateLoss();

            if (vpa_questions != null) {
                vpa_questions.removeAllFragments();
            }
            vsAnimationHelper.changeState(VIEW_STATES.SUMMARY, true);
        } catch (Exception e) {
            new ErrorDialog(getActivity(), new Error().setExceptionThrown(e).setDebugString("CheckinFragment:displaySummary...DISPLAYSUMMARY")).setCancelable(false).show();
        }
    }

    private void updateDateButtons(Calendar date) {
        if(DateUtils.isToday(date.getTimeInMillis()) || date.after(Calendar.getInstance())) {
            // Date is today, hide the next button.
            mainToolbarView.setPrevButtonState(MainToolbarView.ButtonState.VISIBLE);
            mainToolbarView.setNextButtonState(MainToolbarView.ButtonState.HIDDEN);
        } else {
            mainToolbarView.setPrevButtonState(MainToolbarView.ButtonState.VISIBLE);
            mainToolbarView.setNextButtonState(MainToolbarView.ButtonState.VISIBLE);
        }
        toolbarTitle.setText(Styling.displayDateLong(date));
    }

    /**
     * Called when the view previously created by {@link #onCreateView} has
     * been detached from the fragment.  The next time the fragment needs
     * to be displayed, a new view will be created.  This is called
     * after {@link #onStop()} and before {@link #onDestroy()}.  It is called
     * <em>regardless</em> of whether {@link #onCreateView} returned a
     * non-null view.  Internally it is called after the view's state has
     * been saved but before it has been removed from its parent.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isActivityDestroyed = true;
    }

    /**
     * Called when the Fragment is no longer resumed.  This is generally
     * tied to {@link Fragment#onPause() Activity.onPause} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onPause() {
        super.onPause();
        isActivityPaused().setValue(true);
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to {@link Fragment#onResume() Activity.onResume} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onResume() {
        super.onResume();
        isActivityPaused().setValue(false);
    }

    /**
     * Remove the summary fragment (including fragment transaction removal and setting field to null).
     */
    private void removeSummary() {
        if (f_checkin_sumary != null) {
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.remove(f_checkin_sumary);
            transaction.commitAllowingStateLoss();
            f_checkin_sumary = null;
        }
    }

    private void updateLocalCheckInAndUI(final Trackable trackable) {
        // When the check in is updated because of the statement below it's id is updated from the returned check in.
        checkInIdUpdate.add(trackable);
        // Updates the model which automatically submits the check in.
        checkIn.getValue().getTrackables(trackable.getType()).add(trackable);
    }

    /**
     * Creates a list of fragment objects for each page (for example, conditions, treatments,
     * symptoms and tags.
     *
     * @return List of fragment objects for each page of the check in (not the extra pages inside
     * the summary fragment).
     */
    public static List<ViewPagerFragmentBase> createFragments(boolean isSummaryView) {
        List<ViewPagerFragmentBase> fragments = new ArrayList<>();
        FlaringQuestionFragment flaringQuestionFragment = FlaringQuestionFragment.newInstance(isSummaryView);
        fragments.add(flaringQuestionFragment);
        for (TrackableType trackableType : TrackableType.trackableValues()) {
            CheckinCatalogQFragment checkinCatalogQFragment = CheckinCatalogQFragment.newInstance(trackableType);
            fragments.add(checkinCatalogQFragment);
        }
        fragments.add(TagFragment.newInstance());
        return fragments;
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == getActivity().RESULT_OK) {
            if (requestCode == FlaredownConstants.ADD_TRACKABLE_REQUEST_CODE) {
                // Retrieve trackable from intent.
                if (data.hasExtra(FlaredownConstants.RETURN_TRACKABLE_KEY)) {
                    Bundle bundle = data.getExtras();
                    final Trackable trackable = (Trackable) bundle.get(FlaredownConstants.RETURN_TRACKABLE_KEY);
                    if (trackable != null) {
                        trackable.setCheckInId(checkIn.getValue().getId());
                        if (DateUtils.isToday(checkIn.getValue().getDate().getTimeInMillis())) {
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
                                    if (result.getStatusCode() == 422)
                                        updateLocalCheckInAndUI(trackable);
                                    else
                                        new ErrorDialog(getActivity(), result).setCancelable(false).show();
                                    new ErrorDialog(getActivity(), result).setCancelable(false).show();
                                }
                            });
                        } else {
                            try {
                                updateLocalCheckInAndUI(trackable);
                            } catch (IllegalStateException e) {
                                new ErrorDialog(getActivity(), new Error().setExceptionThrown(e).setDebugString("AddEditableActivity result exception catched.").setRetryRunnable(new Runnable() {
                                    @Override
                                    public void run() {
                                        onActivityResult(requestCode, resultCode, data);
                                    }
                                })).setCancelable(false).show();
                            }
                        }
                    }
                    // If a tag has been returned from the add editable activity.
                } else if (data.hasExtra(AddEditableActivity.RETURN_TAG_KEY)) {
                    Bundle bundle = data.getExtras();
                    Tag newTag = (Tag) bundle.get(AddEditableActivity.RETURN_TAG_KEY);
                    if (newTag != null) {
                        checkIn.getValue().getTags().add(newTag);
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

    public static class CheckInDatePickerDialogFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
        public CheckInDatePickerDialogFragment() {
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            CheckinFragment checkinActivity = ((MainActivity) getActivity()).getCheckinFragment();

            final Calendar c = checkinActivity.getCheckIn().getDate();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it.
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        @Override
        public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
            CheckinFragment checkinActivity = ((MainActivity) getActivity()).getCheckinFragment();
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, datePicker.getYear());
            c.set(Calendar.MONTH, datePicker.getMonth());
            c.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());

            checkinActivity.displayCheckin(c);
        }
    }
}