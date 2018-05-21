package com.example.cj.lockscreen;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

import java.util.List;

/**
 * Created by saleh on 5/2/18.
 */

@DynamoDBTable(tableName = "USER_PERMISSIONS")
public class Users {
    private String _username;
    private Integer _permID;
    private String _password;

    //Hash key means it is the primary key
    @DynamoDBHashKey(attributeName = "USERNAME")
    //Is the column name
    @DynamoDBAttribute(attributeName = "USERNAME")
    public String get_username(){
        return _username;
    }
    public void set_username(final String id){
        this._username = id;

    }
    @DynamoDBAttribute(attributeName = "PermID")
    public Integer get_permID(){
        return _permID;
    }
    public void set_permID(final Integer permID){
        this._permID = permID;

    }
    @DynamoDBAttribute(attributeName = "password")
    public String get_password(){
        return _password;
    }
    public void set_password(final String password){
        this._password = password;
    }
}
