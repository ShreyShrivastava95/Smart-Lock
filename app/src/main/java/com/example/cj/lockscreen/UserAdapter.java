package com.example.cj.lockscreen;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;

import java.util.List;

//Just the recycler view adapter for the user table
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    //Used to inflate view
    private Context mCtx;
    //This is to be a query list from the DB
    private List<Users> userList;
    private DynamoDBMapper dbMapper;
    public UserAdapter(Context mCtx, List<Users> userList) {
        this.mCtx = mCtx;
        this.userList = userList;
    }

    //Creates view holder  (The UI returns)
    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mCtx);
        //Inflate the list
        View view = inflater.inflate(R.layout.permission_layout, null);
        final UserViewHolder holder = new UserViewHolder(view);
        return holder;
    }

    //Binds the data to it
    @Override
    public void onBindViewHolder(final UserViewHolder holder, final int position) {
        //Position is the specific item inside of it
        final Users user  = userList.get(position);
        holder.userName.setText(user.get_username());
        holder.permID.setText(Integer.toString(user.get_permID()));
        if(user.get_permID() == 1 || user.get_permID() == 0){
            holder.permissionSwitch.setChecked(true);
        }else{
            holder.permissionSwitch.setChecked(false);
        }
        /*
        holder.itemView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                //Do stuff
                PopupMenu popupMenu = new PopupMenu(context,view);
            }
        });*/
    }

    //Returns the size of the list
    @Override
    public int getItemCount() {
        try {
            return userList.size();
        }catch (Exception e){
            return 0;
        }
    }

    class UserViewHolder extends RecyclerView.ViewHolder{

        TextView userName;
        TextView permID;
        LinearLayout parentLayout;
        TextView emptyText;
        Switch permissionSwitch;
        //Constructor
        public UserViewHolder(final View itemView) {
            super(itemView);

            userName = itemView.findViewById(R.id.userName);
            permID = itemView.findViewById(R.id.text_PermissionLevel);
            parentLayout = itemView.findViewById(R.id.perm_parent_Layout_list);
            emptyText = itemView.findViewById(R.id.permission_empty);
            permissionSwitch = itemView.findViewById(R.id.toggle_Permissions);

            //If their are no rows just tell them there is none
            if(getItemCount()==0){
                emptyText.setVisibility(View.VISIBLE);
            }

            //Permission level 0 is OWNER
            //1 is SubUser
            //2 is Restricted
            permissionSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isOn) {
                    int newPermissionLevel;
                    if(isOn == true){
                        //The switch is on update the user to level 1
                        newPermissionLevel = 1;
                    }else{
                        //The switch is off update the user to level 2
                        newPermissionLevel = 2;
                    }
                    final Users user = new Users();
                    System.out.println("Permissions level: "+isOn+"  "+ newPermissionLevel);
                    String username = userName.getText().toString();
                    user.set_username(username);
                    user.set_permID(newPermissionLevel);
                    dbMapper = AWSProvider.getInstance().getDyanomoDBMapper();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //Save the new user to the db with new permissions
                            dbMapper.save(user);
                            //TODO check if the password is getting deleted
                        }
                    }).start();
                    permID.setText(Integer.toString(newPermissionLevel));

                }
            });
        }
    }



}
