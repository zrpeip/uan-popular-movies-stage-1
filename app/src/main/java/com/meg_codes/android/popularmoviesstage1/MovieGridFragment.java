package com.meg_codes.android.popularmoviesstage1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;


/**
 * Main fragment for user interaction.
 * This serves as the starting screen with a grid view of the top 20
 * most popular movies from The Movie Database API. These top 20 most
 * popular can be sorted by popularity or highest average vote, descending.
 */
public class MovieGridFragment extends Fragment {
    private final String LOG_TAG = MovieGridFragment.class.getSimpleName();
    private ImageAdapter mPosterAdapter;
    private ArrayList<Movie> mMovieList;

    public MovieGridFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_movie_grid, container, false);

        // Grid view that is linked to mPosterAdapter. Displays movie posters.
        GridView movieView = (GridView) rootView.findViewById(R.id.fragment_moviegrid);
        movieView.setAdapter(mPosterAdapter);

        // Makes each poster interactive, takes user to a fragment with details.
        // Movie object implements Parcelable, is sent as an intent extra here.
        movieView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                Intent detailIntent = new Intent(v.getContext(), MovieDetailActivity.class);
                Movie movie = mPosterAdapter.getItem(position);
                detailIntent.putExtra("movie", movie);
                startActivity(detailIntent);
            }
        });
        return rootView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize global ImageAdapter variable.
        mPosterAdapter = new ImageAdapter(getActivity(), 0, new ArrayList<Movie>());
        if (savedInstanceState == null || !savedInstanceState.containsKey("movies")) {
            // When there is no saved instance state, query TMDb API.
            mMovieList = new ArrayList<Movie>();
            FetchMovieDataTask fetch = new FetchMovieDataTask();
            fetch.execute();
        } else {
            // If there is a saved instance state, use that instead of API query.
            mMovieList = savedInstanceState.getParcelableArrayList("movies");
            if (mMovieList != null) {

                for (Movie m : mMovieList) {
                    mPosterAdapter.add(m);
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("movies", mMovieList);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPosterAdapter != null) {
            sortMoviesByPref(mPosterAdapter, mMovieList);
        }
    }

    /**
     * Custom Adapter for loading images into the movie grid view in the fragment_movie_grid file.
     */
    public class ImageAdapter extends ArrayAdapter<Movie> {
        private Context context;
        private ArrayList<Movie> movies;

        public ImageAdapter(Context c, int resource, ArrayList<Movie> m) {
            super(c, resource, m);
            context = c;
            movies = m;
        }

        public int getCount() {
            return movies.size();
        }

        public Movie getItem(int position) {
            return movies.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public ArrayList<Movie> getMovies() {
            return movies;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                // If it isn't recycled, initialize some attributes
                imageView = new ImageView(parent.getContext());
                imageView.setLayoutParams(new GridView.LayoutParams(185, 278));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(16, 4, 16, 4);
            } else {
                imageView = (ImageView) convertView;
            }

            // Picasso library, populates GridView with poster URLs
            // (full URL saved into Movie object)
            Picasso
                    .with(context)
                    .load(movies.get(position).getUrl())
                    .into(imageView);
            return imageView;
        }
    }

    /**
     * Sorts the ImageAdapter's list of movies to reflect the user preference,
     * either "sort by most popular (default)" or "sort by highest vote average."
     *
     * @param movies the ImageAdapter of Movie objects to be sorted, must have at least 1 object.
     *
     * @param movieList also needs to be sorted, is used to repopulate the image adapter (in the
     *                  method "onSaveInstanceState" above) if the activity is re-created, for
     *                  example on screen rotation.
     */
    public void sortMoviesByPref(ImageAdapter movies, ArrayList movieList) {
        // Get user preferences from the Preference Manager.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // prefs.getString will take care of assigning a default if none has been assigned yet.
        String sortCriteria = prefs.getString(
                getString(R.string.pref_sort_order_key),
                getString(R.string.pref_sort_order_default));

        // If the ImageAdapter has at least one object, sort results by either vote or popularity.
        // Update the Movie ArrayList to make sure state is stored
        if (movies != null && sortCriteria != null) {
            if (sortCriteria.equals("vote")) {
                movies.sort(new Comparator<Movie>() {
                    @Override
                    public int compare(Movie lhs, Movie rhs) {
                        return rhs.getVoteAverage().compareTo(lhs.getVoteAverage());
                    }
                });
            } else if (sortCriteria.equals("popularity")) {
                movies.sort(new Comparator<Movie>() {
                    @Override
                    public int compare(Movie lhs, Movie rhs) {
                        return rhs.getPopularity().compareTo(lhs.getPopularity());
                    }
                });
            }
            movieList.clear();
            movieList.addAll(movies.getMovies());
        }
    }

    /**
     * AsyncTask that handles getting data from The Movie Database.
     * <p/>
     * doInBackground - gets JSON string from TMDb API, calls a method that
     * turns the string into an array of Movie objects with details filled in, and then
     * returns it to onPostExecute.
     * <p/>
     * onPostExecute - adds the Movie objects to the class variable mPosterAdapter, which
     * is set on the opening GridView.
     */
    public class FetchMovieDataTask extends AsyncTask<Void, Void, ArrayList<Movie>> {

        private final String LOG_TAG = FetchMovieDataTask.class.getSimpleName();

        @Override
        protected ArrayList<Movie> doInBackground(Void... params) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String movieJsonStr = null;

            try {
                // Construct the URL for the Movie Database query
                String BASE_URL = "http://api.themoviedb.org/3/discover/movie?";
                String POPULARITY_URL = "sort_by=popularity.desc";

                // API key removed, please put yours between the empty quotation marks below.
                String API_KEY = "&api_key=" + "";
                URL url = new URL(BASE_URL + POPULARITY_URL + API_KEY);

                // Create the request to The Movie Db, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }

                movieJsonStr = buffer.toString();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error! No data from the API. Is the API key for TMDb missing? ", e);
                // If the code didn't successfully get the  data, there's no point in attempting
                // to parse it.
                return null;
            } finally {
                // End connection and close the reader.
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("MovieGridFragment", "Error closing stream", e);
                    }
                }
            }

            // Try to parse the JSON string
            try {
                return getMovieDetails(movieJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }

        /**
         * Parses the JSON string from the API query, populates an ArrayList with Movie objects
         * constructed with the JSON data, and returns the ArrayList.
         *
         * @param movieJsonString holds the JSON string taken from TMDb query.
         * @return a string array with the full poster URLs
         * @throws JSONException
         */
        private ArrayList<Movie> getMovieDetails(String movieJsonString) throws JSONException {
            // Keys to get values from JSON arrays/objects to create Movie objects.
            final String RESULTS_KEY = "results";
            final String ORIGINAL_TITLE_KEY = "original_title";
            final String BASE_URL = "http://image.tmdb.org/t/p/w185/";
            final String POSTER_PATH_KEY = "poster_path";
            final String RELEASE_DATE_KEY = "release_date";
            final String VOTE_AVERAGE_KEY = "vote_average";
            final String POPULARITY_KEY = "popularity";
            final String OVERVIEW_KEY = "overview";
            ArrayList<Movie> movies = new ArrayList<>();

            // JSON libraries
            JSONObject movieJson = new JSONObject(movieJsonString);
            JSONArray resultsArray = movieJson.getJSONArray(RESULTS_KEY);

            // Takes JSON key:value pairs and moves information to each Movie object in turn.
            // Movie constructor takes 6 args: title, poster URL, release date, vote average,
            // popularity, and overview.
            for (int i = 0; i < resultsArray.length(); i++) {
                movies.add(i, new Movie(
                                resultsArray.getJSONObject(i).getString(ORIGINAL_TITLE_KEY),
                                BASE_URL + resultsArray.getJSONObject(i).getString(POSTER_PATH_KEY),
                                resultsArray.getJSONObject(i).getString(RELEASE_DATE_KEY),
                                resultsArray.getJSONObject(i).getString(VOTE_AVERAGE_KEY),
                                resultsArray.getJSONObject(i).getString(POPULARITY_KEY),
                                resultsArray.getJSONObject(i).getString(OVERVIEW_KEY))
                );
            }
            return movies;
        }

        /**
         * Populates the GridView's mPosterAdapter with the movies to be displayed,
         * then sorts them by the saved user preference. If this is the initial run,
         * the Preferences Manager sets the default to "popularity."
         *
         * @param movies the ArrayList of Movie objects returned by doInBackground.
         */
        @Override
        protected void onPostExecute(ArrayList<Movie> movies) {

            if (movies != null && mPosterAdapter != null) {
                if (movies.size() > 0) {
                    for (Movie m : movies) {
                        mPosterAdapter.add(m);
                        mMovieList.add(m);
                    }
                    sortMoviesByPref(mPosterAdapter, mMovieList);
                }
            }
        }
    }
}
