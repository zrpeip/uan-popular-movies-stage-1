package com.meg_codes.android.popularmoviesstage1;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.Collections;
import java.util.Comparator;

/**
 * Movie class for storing Movie object characteristics.
 * Movie objects are created from the JSON data, retrieved from the query to the
 * TMDb API in "FetchMovieDataTask" in MovieGridFragment.java.
 *
 * Implements Parcelable since in the future, may want to move data around
 * in something like an ArrayList instead of just strings or a string array.
 *
 */
public class Movie implements Parcelable {
    // Movie details taken from the JSON string from The Movie Database.
    private String mTitle;
    private String mPosterUrl;
    private String mReleaseDate;
    private String mVoteAverage;
    private String mPopularity;
    private String mOverview;

    // Constructor requires all details (may make this an array or array list later).
    public Movie(String title, String posterUrl, String releaseDate,
                 String voteAverage, String popularity, String overview) {
        mTitle = title;
        mPosterUrl = posterUrl;
        mReleaseDate = releaseDate;
        mVoteAverage = voteAverage;
        mPopularity = popularity;
        mOverview = overview;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getUrl() {
        return mPosterUrl;
    }

    public String getTextReleaseDate() {
        // Change the form of the release date from "YYYY/MM/DD" to "Released in Month, Year"
        String[] months = {null, "January", "February", "March", "April", "May", "June", "July",
                "August", "September", "October", "November", "December"};
        int monthNumber = Integer.parseInt(mReleaseDate.substring(5, 7));
        return "Released in " + months[monthNumber] + ", " + mReleaseDate.substring(0, 4);
    }

    public String getReleaseDate() {
        return mReleaseDate;
    }

    public String getVoteAverage() {
        // Database form does not have the " / 10" included for the user, so it's added here.
        return "Average user score: " + mVoteAverage + " / 10";
    }

    public String getPopularity() {
        return mPopularity;
    }

    public String getOverview() {
        return mOverview;
    }

    public String toString() {
        return mTitle + "--" + mPosterUrl + "--" + mReleaseDate
                + "--" + mVoteAverage + "--" + mPopularity + "--" + mOverview;
    }

    // --------- Methods for Implementing Parcelable -----------

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mTitle);
        parcel.writeString(mPosterUrl);
        parcel.writeString(mReleaseDate);
        parcel.writeString(mVoteAverage);
        parcel.writeString(mPopularity);
        parcel.writeString(mOverview);
    }

    public static final Parcelable.Creator<Movie> CREATOR = new Parcelable.Creator<Movie>() {
        @Override
        public Movie createFromParcel(Parcel parcel) {
            return new Movie(parcel);
        }

        @Override
        public Movie[] newArray(int i) {
            return new Movie[i];
        }

    };

    private Movie(Parcel in) {
        mTitle = in.readString();
        mPosterUrl = in.readString();
        mReleaseDate = in.readString();
        mVoteAverage = in.readString();
        mPopularity = in.readString();
        mOverview = in.readString();
    }
}
