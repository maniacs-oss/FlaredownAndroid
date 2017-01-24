package com.flaredown.flaredownApp.Activities.Login;

import android.os.Parcel;
import android.os.Parcelable;

import com.flaredown.flaredownApp.Helpers.Wrappers.Mosby.ViewStateWrapper;
import com.hannesdorfmann.parcelableplease.annotation.ParcelablePlease;

/**
 * Created by thunter on 12/01/2017.
 */

@ParcelablePlease(allFields = false)
public class LoginViewState extends ViewStateWrapper<LoginModel, LoginView> implements Parcelable {

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        LoginViewStateParcelablePlease.writeToParcel(this, dest, flags);
    }

    public static final Creator<LoginViewState> CREATOR = new Creator<LoginViewState>() {
        public LoginViewState createFromParcel(Parcel source) {
            LoginViewState target = new LoginViewState();
            LoginViewStateParcelablePlease.readFromParcel(target, source);
            return target;
        }

        public LoginViewState[] newArray(int size) {
            return new LoginViewState[size];
        }
    };
}