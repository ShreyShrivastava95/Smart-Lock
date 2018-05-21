package com.example.cj.lockscreen;

import android.app.ProgressDialog;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBScanExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.Select;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;

import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;
import static com.amazonaws.mobile.auth.userpools.CognitoUserPoolsSignInProvider.AttributeKeys.USERNAME;

public class PermissionTable extends android.support.v4.app.Fragment {

    RecyclerView recyclerView;
    UserAdapter adapter;
    //when we query only select the user and id
    //List<Users> usersList;
    List<Users> result;
    DynamoDBMapper dbMapper;
    ProgressDialog dialog;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Loading message while we grab the data
        dialog=new ProgressDialog(getActivity());
        dialog.setMessage("Loading...");
        dialog.setCancelable(false);
        dialog.setInverseBackgroundForced(false);
        dialog.show();
        return inflater.inflate(R.layout.fragment_permission_table, container,false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle("Permissions");

        dbMapper = AWSProvider.getInstance().getDyanomoDBMapper();
        //This is the equivaelent to the oncreate methods
        recyclerView = (RecyclerView) getView().findViewById(R.id.permission_recycler_View);
        //Sets the recycler view to a fixed size
        recyclerView.setHasFixedSize(true);
        //sets it to a vertical layout, card with stack on top each other
        RecyclerView.LayoutManager layouter = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layouter);

        //Grab the items from the database
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
                final List<Users> usersList = dbMapper.scan(Users.class, scanExpression);
                //result = dbMapper.query(Users.class, queryExpression);
                System.out.println(usersList.size());
                for(Users using : usersList){
                    System.out.println("Username: "+using.get_username()+" Password: "+using.get_password());
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.hide();
                        adapter = new UserAdapter(getActivity(),usersList);
                        recyclerView.setAdapter(adapter);
                    }
                });
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }

}
