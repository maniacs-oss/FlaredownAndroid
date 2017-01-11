package com.flaredown.flaredownApp.Dagger2;

import android.app.Application;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.SharedPreferences;

import com.flaredown.flaredownApp.Helpers.PreferenceKeys;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Dagger2 Module for the application.
 */

@Module
public class AppModule {
    Application application;

    /**
     * Dagger2 Module for injection of the application.
     * @param application
     */
    public AppModule(Application application) {
        this.application = application;
    }

    /**
     * Provides the application object.
     * @return the application object.
     */
    @Provides
    @Singleton
    Application providesApplication() {
        return this.application;
    }

    /**
     * Provides the default SharedPreferences.
     * @return The default SharedPreferences for the application.
     */
    @Provides
    @Singleton
    SharedPreferences providesSharedPreferences() {
        return PreferenceKeys.getSharedPreferences(this.application);
    }
}