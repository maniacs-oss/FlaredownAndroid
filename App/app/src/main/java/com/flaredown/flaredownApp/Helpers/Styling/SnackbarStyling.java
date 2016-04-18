package com.flaredown.flaredownApp.Helpers.Styling;

import android.view.View;

/**
 * Created by thunter on 03/04/16.
 */
public class SnackbarStyling {
    private static final int DEFAULT_BACKGROUND = 0xffffffff;

    private static View getSnackBarLayout(android.support.design.widget.Snackbar snackbar) {
        if(snackbar != null)
            return snackbar.getView();
        return null;
    }

    public static android.support.design.widget.Snackbar colorSnackBar(android.support.design.widget.Snackbar snackbar, int colorId) {
        View snackbarView = getSnackBarLayout(snackbar);
        if(snackbarView != null)
            snackbarView.setBackgroundColor(colorId);
        return snackbar;
    }

    public static android.support.design.widget.Snackbar defaultColor(android.support.design.widget.Snackbar snackbar) {
        return colorSnackBar(snackbar, DEFAULT_BACKGROUND);
    }
}