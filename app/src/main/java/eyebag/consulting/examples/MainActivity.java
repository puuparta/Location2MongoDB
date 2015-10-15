package eyebag.consulting.examples;

import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import java.io.IOException;

/*
 build.gradleen lisätty:
    android {
        useLibrary 'org.apache.http.legacy'
    }
 ottaa mukaan http kirjaston
*/
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    /* Paikkatiedolle oleelliset */
    private LocationManager locationManager;
    private TinyLocationListener tinyLocationListener;
    private String provider;
    private Criteria criteria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /* asetetaan paikkatiedon kuuntelija */
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setCostAllowed(false);
        provider = locationManager.getBestProvider(criteria, false);
        Location location = locationManager.getLastKnownLocation(provider);
        tinyLocationListener = new TinyLocationListener();

        if (location != null) {
            tinyLocationListener.onLocationChanged(location);
        } else {
            // ei paikkatietoa, näytetään paikannusasetukset luurista.
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
        // asetetaan minimi etäisyysmuutos ja aika joka laukaisee kuuntelijamme.
        // tässä 200ms ja 1 metri.
        locationManager.requestLocationUpdates(provider, 200, 1, tinyLocationListener);


        /*  ANDROID STUDION GENEROIMAA KOODIA, EI LIITY PAIKKATIETOON  ------------------*/
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        /*  ANDROID STUDION GENEROIMA KOODI PÄÄTTYY  -----------------------------------*/
    }

    /*  ANDROID STUDION GENEROIMAA KOODIA, EI LIITY PAIKKATIETOON  ------------------*/
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camara) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    /*  ANDROID STUDION GENEROIMA KOODI PÄÄTTYY  -----------------------------------*/


    /**
     * Oma paikkatietokuuntelija joka lähettää paikkatiedon
     * omassa säikeessä http yli MongoDBlle.
     */
    private class TinyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            // postDatan lähetys oltava omassa säikeessä. Android ei salli pääsäikeen blokkaamista.
            // Muuten applikaatio kaatuu ja heittää logiin: "Caused by: android.os.NetworkOnMainThreadException"
            postData(String.valueOf(location.getLatitude()),String.valueOf(location.getLongitude()));

            Toast.makeText(MainActivity.this,  "Paikka muuttui!",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Toast.makeText(MainActivity.this, provider + "' tila muuttui: " + status + "!",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderEnabled(String provider) {
            Toast.makeText(MainActivity.this, "Provider " + provider + " käytössä.",
                    Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onProviderDisabled(String provider) {
            Toast.makeText(MainActivity.this, "Provider " + provider + " pois käytöstä!",
                    Toast.LENGTH_SHORT).show();
        }

        /**
         * Datan lähetysfunktio.
         */
        @SuppressWarnings("deprecation") /* valittaa http kirjastosta muuten */
        public void postData(String lat, String lon) {

          

            /**
             * Http lähetys asynkkina omassa säikeessä ettei blokata Mainia.
             */
            class SendToMongoDbAsyncTask extends AsyncTask<String, Void, String> {

                @Override
                protected String doInBackground(String... params) {

                    String paramLatitude = params[0];
                    String paramLongitude = params[1];
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost("https://api.mongolab.com/api/1/databases/burris/collections/locations?apiKey=GGo7JekGU6E5AArDW9dG6Z0iMuxzUZOI");
                    httpPost.setHeader("Accept", "application/json");
                    httpPost.setHeader("Content-type", "application/json");
                    String json = "{'latitude': '" + paramLatitude + "','longitude' : '" + paramLongitude + "'}";

                    try {
                        httpPost.setEntity(new StringEntity(json));
                        HttpResponse response = httpclient.execute(httpPost);
                        return "ok";

                    } catch (ClientProtocolException e) {
                        // TODO Auto-generated catch block
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                    }

                    return null;
                }

                @Override
                protected void onPostExecute(String result) {
                    super.onPostExecute(result);

                    if (result.equals("ok")) {
                        Toast.makeText(getApplicationContext(), "Paikkatieto lähetetty.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Virhe lähetyksessä.", Toast.LENGTH_LONG).show();
                    }
                }
            }

            /* luodaan instanssi asynkkikäsittelijästä ja suoritetaan */
            SendToMongoDbAsyncTask sendPostReqAsyncTask = new SendToMongoDbAsyncTask();
            sendPostReqAsyncTask.execute(lat, lon);
        }
    }
}

