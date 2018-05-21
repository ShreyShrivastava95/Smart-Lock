package com.example.cj.lockscreen;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;

import java.security.KeyStore;
import java.util.UUID;

public class TestActivity extends AppCompatActivity {

    String keystorePath;
    String keystoreName;
    String keystorePassword;
    KeyStore clientKeyStore = null;
    String certificateId;
    DynamoDBMapper dbMapper;
    AWSIotClient mIotAndroidClient;
    AWSIotMqttManager mqttManager;
    String clientId;
    private static final String AWS_IOT_POLICY_NAME = "XXX_PRIVATE_XXX";
    private static final String LOG_TAG = "TestActivity";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final EditText usrName = (EditText) findViewById(R.id.usrName);
        final EditText usrPass = (EditText) findViewById(R.id.pass);
        Button submitBtn = (Button) findViewById(R.id.sumbitBtn);


        

        //mIotAndroidClient = new AWSIotClient(AWSProvider.getInstance().getIdentityManager().getCredentialsProvider());

        //AWSProvider.init(getApplicationContext());

        dbMapper = AWSProvider.getInstance().getDyanomoDBMapper();

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Users newUser = new Users();
                //TODO change this method to reflect new values
               // newUser.setUserId(new Integer(usrName.getText().toString()));
               // newUser.setOwner(true);
              //  newUser.setPassword(usrPass.getText().toString());

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        dbMapper.save(newUser);
                    }
                }).start();


            }
        });

        Log.d(LOG_TAG + "Config: ", AWSProvider.getInstance().getAwsConfiguration().toString());


        mIotAndroidClient = new AWSIotClient(AWSProvider.getInstance().getIdentityManager().getCredentialsProvider());


        clientId = UUID.randomUUID().toString();

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, "XXX_PRIVATE_XXX");
        //MQTT pings every 10 seconds
        mqttManager.setKeepAlive(10);
        // Set Last Will and Testament for MQTT.  On an unclean disconnect (loss of connection)
        // AWS IoT will publish this message to alert other clients.
        AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my_lock",
                "Android client lost connection", AWSIotMqttQos.QOS0);
        mqttManager.setMqttLastWillAndTestament(lwt);

        final String KEYSTORE_NAME = "iot_keystore";
        final String KEYSTORE_PASSWORD = "password";
        final String CERTIFICATE_ID = "default";

        keystorePath = getFilesDir().getPath();
        keystoreName = KEYSTORE_NAME;
        keystorePassword = KEYSTORE_PASSWORD;
        certificateId = CERTIFICATE_ID;


        try {
            if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
                if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath, keystoreName, keystorePassword)) {
                    Log.i(LOG_TAG, "Certificate " + certificateId
                            + " found in keystore - using for MQTT.");
                    // load keystore from file into memory to pass on connection
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                            keystorePath, keystoreName, keystorePassword);
                    //.setEnabled(true);
                } else {
                    Log.i(LOG_TAG, "Key/cert " + certificateId + " not found in keystore.");
                }
            } else {
                Log.i(LOG_TAG, "Keystore " + keystorePath + "/" + keystoreName + " not found.");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "An error occurred retrieving cert/key from keystore.", e);
        }
        if (clientKeyStore == null) {
            Log.i(LOG_TAG, "Cert/key was not found in keystore - creating new key and certificate.");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Create a new private key and certificate. This call
                        // creates both on the server and returns them to the device.
                        CreateKeysAndCertificateRequest createKeysAndCertificateRequest =
                                new CreateKeysAndCertificateRequest();
                        createKeysAndCertificateRequest.setSetAsActive(true);
                        final CreateKeysAndCertificateResult createKeysAndCertificateResult;
                        createKeysAndCertificateResult =
                                mIotAndroidClient.createKeysAndCertificate(createKeysAndCertificateRequest);
                        Log.i(LOG_TAG,
                                "Cert ID: " +
                                        createKeysAndCertificateResult.getCertificateId() +
                                        " created.");

                        // store in keystore for use in MQTT client
                        // saved as alias "default" so a new certificate isn't
                        // generated each run of this application
                        AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
                                createKeysAndCertificateResult.getCertificatePem(),
                                createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
                                keystorePath, keystoreName, keystorePassword);

                        // load keystore from file into memory to pass on
                        // connection
                        clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                                keystorePath, keystoreName, keystorePassword);

                        // Attach a policy to the newly created certificate.
                        // This flow assumes the policy was already created in
                        // AWS IoT and we are now just attaching it to the
                        // certificate.
                        AttachPrincipalPolicyRequest policyAttachRequest =
                                new AttachPrincipalPolicyRequest();
                        policyAttachRequest.setPolicyName(AWS_IOT_POLICY_NAME);
                        policyAttachRequest.setPrincipal(createKeysAndCertificateResult
                                .getCertificateArn());
                        mIotAndroidClient.attachPrincipalPolicy(policyAttachRequest);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //btnConnect.setEnabled(true);
                            }
                        });
                    } catch (Exception e) {
                        Log.e(LOG_TAG,
                                "Exception occurred when generating new private key and certificate.",
                                e);
                    }
                }
            }).start();
        }


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

}
