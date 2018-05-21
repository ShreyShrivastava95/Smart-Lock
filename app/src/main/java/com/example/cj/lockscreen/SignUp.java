package com.example.cj.lockscreen;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;

public class SignUp extends AppCompatActivity {
    final static String LOG_TAG = "Sign-up Page";
    final static String USR_PREFERENCE = "user_name";
    final static String PERM_PREFERENCE = "user_prem";
    final static Integer DEFAULT_PERMISSION = 3;
    final static String CODE = "XXXXX";
    EditText nameView, emailView, passwordView, lockCodeView;
    CheckBox ownerCheckBoxView;
    Button signUpView;
    SharedPreferences userAttributes;
    String name, email, password, code;
    Integer perm;
    ProgressDialog dialog;
    DynamoDBMapper dbMapper;
    Context context;
    Thread initUser;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        context = getApplicationContext();

        nameView = (EditText) findViewById(R.id.signup_input_name);
        emailView = (EditText) findViewById(R.id.signup_input_email);
        passwordView = (EditText) findViewById(R.id.signup_input_password);
        lockCodeView = (EditText) findViewById(R.id.signup_input_code);
        ownerCheckBoxView = (CheckBox) findViewById(R.id.owner_check_box);
        signUpView = (Button) findViewById(R.id.btn_signup);
        dialog = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);

        AWSProvider.init(context);



        initUser = new Thread(new Runnable() {
            @Override
            public void run() {
                User.init(name, perm);
            }
        });




        userAttributes = getSharedPreferences(getString(R.string.usr), Context.MODE_PRIVATE);
        name = userAttributes.getString(USR_PREFERENCE, null);
        perm = userAttributes.getInt(PERM_PREFERENCE, DEFAULT_PERMISSION);
        if (name != null){
            initUser.run();
            Intent intent = new Intent(context, MainActivity.class);
            startActivity(intent);
            finish();
        }





        signUpView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                name = nameView.getText().toString();
                email = emailView.getText().toString();
                password = passwordView.getText().toString();
                code = lockCodeView.getText().toString();
                perm = ownerCheckBoxView.isChecked() ? 0 : 2;
                if (perm != 0){
                    new signUpThread().execute(new Users());
                }
                if(perm == 0) {
                    if (code.equals(CODE)) {
                        new signUpThread().execute(new Users());
                        dialog.setMessage("Please Wait...");
                        dialog.show();

                    }
                    else{
                        Toast.makeText(context, "Lock code is not correct!", Toast.LENGTH_LONG).show();
                    }
                }

            }
        });



        lockCodeView.setVisibility(View.INVISIBLE);
        ownerCheckBoxView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ownerCheckBoxView.isChecked())
                    lockCodeView.setVisibility(View.VISIBLE);
                else
                    lockCodeView.setVisibility(View.INVISIBLE);
            }
        });

    }
    private class signUpThread extends AsyncTask<Users, Integer, Integer>{

        @Override
        protected void onPreExecute() {
//            dialog.setMessage("Please Wait...");
//            dialog.show();
        }

        @Override
        protected Integer doInBackground(Users... users) {
            dbMapper = AWSProvider.getInstance().getDyanomoDBMapper();
            final Users user = new Users();
            user.set_username(name);
            user.set_password(password);
            user.set_permID(perm);
            dbMapper.save(user);
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            SharedPreferences.Editor editor = userAttributes.edit();
            editor.putString(USR_PREFERENCE, name);
            editor.putInt(PERM_PREFERENCE, perm);
            editor.commit();
            initUser.run();
            Intent intent = new Intent(context, MainActivity.class);
            dialog.cancel();
            startActivity(intent);
            finish();
        }
    }
}
