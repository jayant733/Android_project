package com.example.tripsync.data.model;

public class FeedPost {
    public final String docId;
    public final String tripName;
    public final String location;
    public final String details;
    public final String email;
    public final String userId;
    public final float averageRating;
    public final int ratingCount;

    public FeedPost(String docId,
                    String tripName,
                    String location,
                    String details,
                    String email,
                    String userId,
                    float averageRating,
                    int ratingCount) {
        this.docId = docId;
        this.tripName = tripName;
        this.location = location;
        this.details = details;
        this.email = email;
        this.userId = userId;
        this.averageRating = averageRating;
        this.ratingCount = ratingCount;
    }
}
