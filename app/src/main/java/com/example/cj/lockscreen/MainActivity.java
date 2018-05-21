package com.example.cj.lockscreen;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    static final String LOG_TAG = "Main";
    final static String USR_PREFERENCE = "user_name";
    final static String PERM_PREFERENCE = "user_prem";


    //AWSIotClient mIotAndroidClient;
    AWSIotMqttManager mqttManager;

    KeyStore clientKeyStore;
    private String currentLockStatus;
    CognitoCachingCredentialsProvider credentialsProvider;

    //Button But_Lock;
    ImageButton But_Connection;
    ImageButton But_Lock;
    TextView CurrentStat;
    DynamoDBMapper dbMapper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        try {
            mqttManager = AWSProvider.getInstance().getMqttManager();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        clientKeyStore = AWSProvider.getInstance().getClientKeyStore();


        //ELEMENTS INITS
        //Button But_Lock = (Button) findViewById(R.id.But)
        But_Connection = (ImageButton) findViewById(R.id.But_Connection);
        But_Connection.setOnClickListener(connectClick);
        But_Lock = (ImageButton) findViewById(R.id.But_ChangeLockStatus);
        But_Lock.setOnClickListener(pushToLock);
        CurrentStat = (TextView) findViewById(R.id.Text_Status);
        But_Connection.callOnClick();
    }

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
        if (id == R.id.action_remove) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void displaySelectedScreen(int itemId){

        Fragment fragment = null;
        if(User.getInstance().getPermID()!=0){
            Toast.makeText(this.getApplicationContext(),"Invalid Permissions",Toast.LENGTH_LONG).show();
        }else{
            switch (itemId){
                case R.id.nav_m:
                    fragment = null;
                    startActivity(new Intent(getApplicationContext(),MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    break;
                case R.id.inventory:
                    fragment = new inventory();
                    break;
                case R.id.accessHistory:
                    fragment = new frag_access_history();
                    break;
                case R.id.nav_permission:
                    fragment = new PermissionTable();
                    break;
            }
            if (fragment != null) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.content_frame, fragment);
                ft.commit();
            }
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        displaySelectedScreen(id);

        return true;
    }

    //More IOT stuff
    //Still in the onCreate here
    View.OnClickListener connectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                clientKeyStore = AWSProvider.getInstance().getClientKeyStore();
                mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                    @Override
                    public void onStatusChanged(final AWSIotMqttClientStatus status, final Throwable throwable) {
                        Log.d(LOG_TAG, "Status = " + String.valueOf(status));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (status == AWSIotMqttClientStatus.Connecting) {
                                    But_Connection.setColorFilter(Color.parseColor("#e83535"));
                                } else if (status == AWSIotMqttClientStatus.Connected) {
                                    But_Connection.setColorFilter(Color.parseColor("#099904"));


                                    //also try to subscribe now since we are connecting
                                    final String topic = "LockStatus";
                                    Log.d(LOG_TAG, "topic = " + topic);
                                    try {
                                        mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                                                new AWSIotMqttNewMessageCallback() {
                                                    @Override
                                                    public void onMessageArrived(final String topic, final byte[] data) {
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                try {
                                                                    String message = new String(data, "UTF-8");
                                                                    CurrentStat.setText(message);
                                                                    currentLockStatus = message;
                                                                    Log.e(LOG_TAG, message);
                                                                    if(message.equals("Unlocked")){
                                                                        But_Lock.setImageResource(R.drawable.unlocked);
                                                                    }else if(message.equals("Locked")){
                                                                        But_Lock.setImageResource(R.drawable.locked);
                                                                    }
                                                                } catch (UnsupportedEncodingException e) {
                                                                    Log.e(LOG_TAG, "Message encoding error.", e);
                                                                }
                                                            }
                                                        });
                                                    }
                                                });
                                        //Since we dont have the data yet send the pi that we dont know it yet who will send it back
                                        mqttManager.publishString("newSubscriber", "LockStatus", AWSIotMqttQos.QOS0);
                                    } catch (Exception e) {
                                        Log.e(LOG_TAG, "Subscription error.", e);
                                    }
                                } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                    if (throwable != null) {
                                        Log.e(LOG_TAG, "Connection error.", throwable);
                                    }
                                    But_Connection.setColorFilter(Color.parseColor("#e83535"));
                                } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                    if (throwable != null) {
                                        Log.e(LOG_TAG, "Connection error.", throwable);
                                    }
                                    But_Connection.setColorFilter(Color.parseColor("#e83535"));
                                } else {
                                    But_Connection.setColorFilter(Color.parseColor("#e83535"));
                                }
                            }
                        });
                    }
                });
            } catch (final Exception e) {
                Log.e(LOG_TAG, "Connection error.", e);
                But_Connection.setColorFilter(Color.parseColor("#e83535"));
            }
        }
    };
    //We only need to first connect here and then publish the message, a subscription is not necessary
    View.OnClickListener pushToLock = new View.OnClickListener(){
        @Override
        public void onClick(View v){

            final String topic = "LockStatus";
            dbMapper = AWSProvider.getInstance().getDyanomoDBMapper();
            try {
                //TODO Check if their status is correct on the table
                //TODO Put in access history all

                //QUERY for this user to see what permission levels they are on
                //thisUser.set_username(); set to current
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final Access_History newAction = new Access_History();

                        final Users thisUser = new Users();
                        thisUser.set_username(User.getInstance().getUsername());
                        thisUser.set_permID(User.getInstance().getPermID());
                        DynamoDBQueryExpression<Users> queryExpression= new DynamoDBQueryExpression<Users>()
                                .withHashKeyValues(thisUser);
                        final List<Users> result = dbMapper.query(Users.class, queryExpression);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String msg; //What to tell topic
                                System.out.println(result.get(0).get_username()+"   \n\n\n"+result.get(0).get_permID());
                                if(result.get(0).get_permID() < 2){
                                    if(currentLockStatus==null){
                                        msg = "Unlocked";
                                    }
                                    else if(currentLockStatus.equals("Locked")){
                                        msg = "Unlocked";
                                        But_Lock.setImageResource(R.drawable.unlocked);

                                    }else{
                                        msg = "Locked";
                                        But_Lock.setImageResource(R.drawable.locked);
                                    }
                                    mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);

                                }else{
                                    Toast.makeText(getApplicationContext(),"Invalid Permissions",Toast.LENGTH_LONG).show();
                                    msg = "Unauthorized Attempt";
                                }
                            }
                        });
                        //send the action off
                        System.out.println("We are sending now");
                        //Tell the DB Access History that someone did something
                        String messager;
                        newAction.set_username(thisUser.get_username());
                        //Have to repeat above since on different thread
                        if(result.get(0).get_permID() < 2){
                            if(currentLockStatus==null){
                                messager = "Unlocked";
                            }
                            else if(currentLockStatus.equals("Locked")){
                                messager = "Unlocked";

                            }else{
                                messager = "Locked";
                            }
                        }else{
                            messager = "Unauthorized";
                        }
                        newAction.set_action(messager);
                        SimpleDateFormat formatted = new SimpleDateFormat("MM/dd HH:mm a");
                        Date now = new Date();
                        String currentTime = formatted.format(now);
                        System.out.println("Current time is " + currentTime);
                        newAction.set_time(currentTime);
                        dbMapper.save(newAction);
                    }
                }).start();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Publish error.", e);
            }
        }
    };
}
