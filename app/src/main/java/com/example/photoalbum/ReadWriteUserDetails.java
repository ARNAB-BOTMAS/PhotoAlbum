package com.example.photoalbum;

public class ReadWriteUserDetails {
    public String username, phone, email, password;
    public ReadWriteUserDetails(String stUser, String stPhone, String stEmail, String stPass){
        this.username = stUser;
        this.phone = stPhone;
        this.email = stEmail;
        this.password = stPass;
    }
}