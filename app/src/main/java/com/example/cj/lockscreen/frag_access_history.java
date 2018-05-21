package com.example.cj.lockscreen;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBScanExpression;

import java.util.ArrayList;
import java.util.List;

import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;


public class frag_access_history extends Fragment {
    RecyclerView recyclerView;
    access_history_Adapter adapter;
    //when we query only select the user and id
    List<Access_History> accessList;
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
        return inflater.inflate(R.layout.fragment_frag_access_history, container,false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle("Access History");
        dbMapper = AWSProvider.getInstance().getDyanomoDBMapper();
        //This is the equivaelent to the oncreate methods
        recyclerView = (RecyclerView) getView().findViewById(R.id.access_recycler_View);
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
                final List<Access_History> accessList = dbMapper.scan(Access_History.class, scanExpression);
                //result = dbMapper.query(Users.class, queryExpression);
                System.out.println(accessList.size());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.hide();
                        adapter = new access_history_Adapter(getActivity(),accessList);
                        recyclerView.setAdapter(adapter);
                    }
                });
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }

}




