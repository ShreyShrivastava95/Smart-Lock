package com.example.cj.lockscreen;

import android.content.Context;
import android.util.Log;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.auth.RegionAwareSigner;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.config.AWSConfigurable;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobile.auth.userpools.CognitoUserPoolsSignInProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;
import com.amazonaws.services.s3.model.Region;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.Scanner;
import java.util.UUID;

/**
 * Created by saleh on 5/1/18.
 */

public class AWSProvider {
    private static AWSProvider instance = null;
    private Context context;
    private AWSConfiguration awsConfiguration;
    private JSONObject awsIotConfiguration;
    private AmazonDynamoDBClient dbClient = null;
    private DynamoDBMapper dbMapper = null;
    private AWSIotMqttManager mqttManager = null;
    private KeyStore clientKeyStore = null;
    private AWSIotClient mIotAndroidClient;


    private static final String LOG_TAG = "AWSProvider Class";


    public static AWSProvider getInstance() {
        return instance;
    }
    public static void init(Context context){
        if (instance == null){
            instance = new AWSProvider(context);
        }
    }
    private AWSProvider(Context context) {
        Integer resourceID;
        resourceID = context.getResources().getIdentifier("awsconfiguration", "raw", context.getPackageName());
        this.context = context;
        this.awsConfiguration = new AWSConfiguration(context, resourceID);
        IdentityManager identityManager = new IdentityManager(context, awsConfiguration );
        IdentityManager.setDefaultIdentityManager(identityManager);
        identityManager.addSignInProvider(CognitoUserPoolsSignInProvider.class);
        //identityManager.addSignInProvider(CognitoCachingCredentialsProvider.class);
        //identityManager.getCachedUserID();




        //Reading form JSON file that contains AWS IoT endpoind and policy name
        InputStream is = context.getResources().openRawResource(R.raw.iotconfiguration);
        Scanner sc = new Scanner(is);
        StringBuilder strBuiler = new StringBuilder();
        while(sc.hasNextLine())
            strBuiler.append(sc.nextLine());
        try {
            awsIotConfiguration = new JSONObject(strBuiler.toString());
            initIot();
        } catch (JSONException e) {
            Log.e(LOG_TAG, "An error occurred while creating iot config json");
        }

    }
    private void initIot() throws JSONException {
        final String END_POINT = awsIotConfiguration.getString("endpoint");
        final String POLICY_NAME = awsIotConfiguration.getString("policy");

        //final Regions MY_REGION = Regions.US_EAST_2;
        Regions MY_REGION = Regions.fromName(awsIotConfiguration.getString("region"));


        final String keystoreName = "iot_keystore";
        final String keystorePassword = "password";
        final String certificateId = "default";
        final String keystorePath = context.getFilesDir().getPath();

        //Log.d("HERE",  POLICY_NAME + "\n" + getIdentityManager().isUserSignedIn());

        //String clientId = UUID.randomUUID().toString();


        //mqttManager = new AWSIotMqttManager(clientId, END_POINT);
        //mqttManager.setKeepAlive(10);
        // Set Last Will and Testament for MQTT.  On an unclean disconnect (loss of connection)
        // AWS IoT will publish this message to alert other clients.
        //AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my_lock",//                             "Android client lost connection", AWSIotMqttQos.QOS0);
        //getMqttManager();
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(context.getApplicationContext(), awsConfiguration);

        //mqttManager.setMqttLastWillAndTestament(lwt);






        //getMqttManager();
        //CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(context, awsConfiguration);

       // AWSIotClient iot = new AWSIotClient(getIdentityManager().getCachedUserID());
        mIotAndroidClient = new AWSIotClient(credentialsProvider);
        mIotAndroidClient.setRegion(com.amazonaws.regions.Region.getRegion(MY_REGION));
        // Create certificates if not exist
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
                        policyAttachRequest.setPolicyName(POLICY_NAME);
                        policyAttachRequest.setPrincipal(createKeysAndCertificateResult
                                .getCertificateArn());
                        mIotAndroidClient.attachPrincipalPolicy(policyAttachRequest);

                 /*       runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //btnConnect.setEnabled(true);
                            }
                        });*/
                    } catch (Exception e) {
                        Log.e(LOG_TAG,
                                "Exception occurred when generating new private key and certificate.",
                                e);
                    }
                }
            }).start();
        }
    }
    public KeyStore getClientKeyStore(){
        return clientKeyStore;
    }
    public AWSIotMqttManager getMqttManager() throws JSONException {
        if (mqttManager == null){
            final String END_POINT = awsIotConfiguration.getString("endpoint");
            String clientId = UUID.randomUUID().toString();
            mqttManager = new AWSIotMqttManager(clientId, END_POINT);
            mqttManager.setKeepAlive(10);
            // Set Last Will and Testament for MQTT.  On an unclean disconnect (loss of connection)
            // AWS IoT will publish this message to alert other clients.
            AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my_lock",
                    "Android client lost connection", AWSIotMqttQos.QOS0);
            mqttManager.setMqttLastWillAndTestament(lwt);
        }
        return mqttManager;
    }

    public IdentityManager getIdentityManager(){
        return IdentityManager.getDefaultIdentityManager();

    }
    public AWSConfiguration getAwsConfiguration(){
        return awsConfiguration;
    }
    public AWSConfiguration getConfiguration() {
        return this.awsConfiguration;
    }
    public DynamoDBMapper getDyanomoDBMapper(){
        if (dbMapper == null) {
            final AWSCredentialsProvider cp = getIdentityManager().getCredentialsProvider();
            dbClient = new AmazonDynamoDBClient(cp);
            dbMapper = DynamoDBMapper.builder()
                    .awsConfiguration(getConfiguration())
                    .dynamoDBClient(dbClient)
                    .build();
        }
        return dbMapper;
    }


}
