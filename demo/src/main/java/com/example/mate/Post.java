package com.example.mate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Post {
    private int postId;
    private String userId;
    private String title;
    private String img;
    private String createdAt;
    private String updatedAt;
    private String content;
}