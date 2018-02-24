package zapr.com.zapr;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    RecyclerView recyclerView;
    LinearLayoutManager layoutManager;
    Myadapter myadapter;
    ArrayList<Model> model=new ArrayList<Model>();
    ProgressBar progressBar;
    String search="arijit+singh";  //default value for search for the first time
    private static final String URL_TO_READ="https://itunes.apple.com/search?term=";

    //
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView=(RecyclerView)findViewById(R.id.recyclerview);
        progressBar=(ProgressBar)findViewById(R.id.progress);
        layoutManager= new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        if(!isNetworkAvailable())
        {
            Toast.makeText(this,"network not available Kindly connect to network",Toast.LENGTH_LONG).show();
        }
        else {
           // getData();
            lockScreenOrientation();
            new AsyncHttpTask().execute(URL_TO_READ+search);
            progressBar.setVisibility(View.VISIBLE);

        }

        //pagination
      /*  recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener()
        {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy)
            {
                if(dy > 0) //check for scroll down
                {
                    visibleItemCount = layoutManager.getChildCount();
                    totalItemCount = layoutManager.getItemCount();
                    pastVisiblesItems = layoutManager.findFirstVisibleItemPosition();

                    if (loading)
                    {
                        if ( (visibleItemCount + pastVisiblesItems) >= totalItemCount)
                        {
                            loading = false;

                            getData();
                        }
                    }
                }
            }
        });*/

    }



    // parsing the response

    public void parse(JSONArray response) {

        for (int i = 0; i < response.length(); i++) {

            //creating data for adapter
            Model add=new Model();
            JSONObject json=null;
            try {
                json=response.getJSONObject(i);
                add.setTrackName(json.getString("trackName"));
                add.setPreviewUrl(json.getString("previewUrl"));
                add.setArtistName(json.getString("artistName"));
                add.setArtworkUrl100(json.getString("artworkUrl100"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            model.add(add);
        }
    }

    //option menu for search bar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_item,menu);
        MenuItem menuItem =menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        searchView.setOnQueryTextListener(this);


        return true;
    }


    //search functionality for custom recycler view
    @Override
    public boolean onQueryTextSubmit(String query) {
        model.clear();
        search=query.trim();
        search=search.replace(" ","+");
        progressBar.setVisibility(View.VISIBLE);
        lockScreenOrientation();
        new AsyncHttpTask().execute(URL_TO_READ+search);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        search="";
        return false;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public class AsyncHttpTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... params) {
            InputStream inputStream = null;
            HttpURLConnection urlConnection = null;

            Integer result = 0;
            try {
                /* forming th java.net.URL object */
                URL url = new URL(params[0]);

                urlConnection = (HttpURLConnection) url.openConnection();

                 /* optional request header */
                urlConnection.setRequestProperty("Content-Type", "application/json");

                /* optional request header */
                urlConnection.setRequestProperty("Accept", "application/json");

                /* for Get request */
                urlConnection.setRequestMethod("GET");

                int statusCode = urlConnection.getResponseCode();

                /* 200 represents HTTP OK */
                if (statusCode ==  200) {

                    inputStream = new BufferedInputStream(urlConnection.getInputStream());

                    String response = convertInputStreamToString(inputStream);
                    JSONObject jsonObject=null;
                    if(!TextUtils.isEmpty(response)) {
                        jsonObject = new JSONObject(response);
                        JSONArray jsonArray = jsonObject.getJSONArray("results");
                        if(jsonArray!=null && jsonArray.length()>0) {
                            parse(jsonArray);
                        }
                        else
                        {
                            Toast.makeText(MainActivity.this,"No data found",Toast.LENGTH_LONG).show();
                        }
                    }
                    else {
                        Toast.makeText(MainActivity.this,"No data found",Toast.LENGTH_LONG).show();
                    }

                    result = 1; // Successful

                }else{
                    result = 0; //"Failed to fetch data!";
                }

            } catch (Exception e) {
                Log.d("inoigni", e.getLocalizedMessage());
            }

            return result; //"Failed to fetch data!";
        }


        @Override
        protected void onPostExecute(Integer result) {
            progressBar.setVisibility(View.GONE);
            if(result == 1){
                if(myadapter==null) {
                    myadapter = new Myadapter(model, MainActivity.this);
                    recyclerView.setAdapter(myadapter);
                }
                else{
                    myadapter.notifyDataSetChanged();
                }
            }else{
                Log.e("bvneribneo", "Failed to fetch data!");
            }

            unlockScreenOrientation();
        }
    }


    private String convertInputStreamToString(InputStream inputStream) throws IOException {

        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));

        String line = "";
        String result = "";

        while((line = bufferedReader.readLine()) != null){
            result += line;
        }

            /* Close Stream */
        if(null!=inputStream){
            inputStream.close();
        }

        return result;
    }

    private void lockScreenOrientation() {
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    private void unlockScreenOrientation() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }
}