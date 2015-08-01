package com.meg_codes.android.popularmoviesstage1;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;


/**
 * Fragment (with corresponding Activity) launched by the Gridview in the MovieGridFragment.
 * Displays the title of the film in the action bar, a poster of the selected movie,
 * as well as several TextViews with details about the movie.
 *
 */
public class MovieDetailFragment extends Fragment {
    private static final String LOG_TAG = MovieDetailFragment.class.getSimpleName();
    private Movie mMovie;

    public MovieDetailFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_movie_detail, container, false);

        // Gets the "parent" intent from the MovieGridFragment with Movie object
        Intent intent = getActivity().getIntent();
        if (intent != null && intent.hasExtra("movie")) {
            mMovie = intent.getParcelableExtra("movie");
        }

        // Populates the ImageView using Picasso and the Movie object's poster URL
        ImageView imageView = (ImageView) rootView.findViewById(R.id.fragment_movie_detail_poster);
        Picasso
                .with(getActivity())
                .load(mMovie.getUrl())
                .into(imageView);

        // Sets the title of the Activity page to the film's title
        getActivity().setTitle(mMovie.getTitle());

        // Populates the detail TextViews (will hopefully find a cleaner way to do this in Stage 2)
        TextView releaseDate = (TextView) rootView.findViewById(R.id.fragment_movie_detail_release_date);
        releaseDate.setText(mMovie.getTextReleaseDate());
        TextView voteAverage = (TextView) rootView.findViewById(R.id.fragment_movie_detail_vote_average);
        voteAverage.setText(mMovie.getVoteAverage());
        TextView overview = (TextView) rootView.findViewById(R.id.fragment_movie_detail_overview);
        overview.setText(mMovie.getOverview());

        return rootView;
    }
}
