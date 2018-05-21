package com.example.cj.lockscreen;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class access_history_Adapter extends RecyclerView.Adapter<access_history_Adapter.access_historyViewHolder> {
    //Used to inflate view
    private Context mCtx;
    //This is to be a query list from the DB
    private List<Access_History> accessList;

    public access_history_Adapter(Context mCtx, List<Access_History> accessList) {
        this.mCtx = mCtx;
        this.accessList = accessList;
    }

    //Creates view holder  (The UI returns)
    @Override
    public access_history_Adapter.access_historyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mCtx);
        //Inflate the list
        View view = inflater.inflate(R.layout.access_history_layout, null);
        final access_history_Adapter.access_historyViewHolder holder = new access_history_Adapter.access_historyViewHolder(view);
        return holder;
    }

    //Binds the data to it
    @Override
    public void onBindViewHolder(final access_history_Adapter.access_historyViewHolder holder, final int position) {
        //Position is the specific item inside of it
        final Access_History access  = accessList.get(position);
        //Set any variables to the data
        //TODO link this to the class methods

        holder.userName.setText(access.get_username());
        holder.access_action.setText(access.get_action());
        holder.dateTime.setText(access.get_time());
    }

    //Returns the size of the list
    @Override
    public int getItemCount() {
        try {
            return accessList.size();
        }catch (Exception e){
            return 0;
        }
    }

    class access_historyViewHolder extends RecyclerView.ViewHolder{

        TextView userName;
        TextView dateTime;
        TextView access_action;
        LinearLayout parentLayout;
        TextView emptyHistory;

        //Constructor
        public access_historyViewHolder(final View itemView) {
            super(itemView);
            emptyHistory = itemView.findViewById(R.id.access_empty);
            if(getItemCount()==0){
                emptyHistory.setVisibility(View.VISIBLE);
            }
            userName = itemView.findViewById(R.id.access_user);
            dateTime = itemView.findViewById(R.id.access_time);
            access_action = itemView.findViewById(R.id.access_action);
            parentLayout = itemView.findViewById(R.id.access_parent_Layout_list);
        }
    }
}
