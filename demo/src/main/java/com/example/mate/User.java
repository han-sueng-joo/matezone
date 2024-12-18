package com.example.mate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class User {
    private String userId;
    private String password;
    private String userName;
    private int grade;
    private String sex;
    private String createdAt;
    private String updatedAt;
}