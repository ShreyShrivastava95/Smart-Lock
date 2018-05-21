package com.example.cj.lockscreen;

/**
 * Created by saleh on 5/5/18.
 */

public class User {
    private static User instance = null;
    private Users user = null;

    public static User getInstance() {
        return instance;
    }
    public static void init(String username, Integer permID){
        if(instance == null){
            instance = new User(username, permID);
        }
    }

    private User(String username, Integer permID) {
        user = new Users();
        user.set_permID(permID);
        user.set_username(username);
    }
    public Users getUser(){
        return user;
    }
    public Integer getPermID(){
        return user.get_permID();
    }
    public String getUsername(){
        return user.get_username();
    }
}
