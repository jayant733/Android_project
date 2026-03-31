package com.example.tripsync.data.model;

public class FeedPost {
    public final String docId;
    public final String tripName;
    public final String location;
    public final String details;
    public final String email;
    public final String userId;
    public final String imageUri;
    public final float averageRating;
    public final int ratingCount;
    public final boolean hasUserRated;
    public final long timestamp;

    public FeedPost(String docId,
                    String tripName,
                    String location,
                    String details,
                    String email,
                    String userId,
                    String imageUri,
                    float averageRating,
                    int ratingCount,
                    boolean hasUserRated,
                    long timestamp) {
        this.docId = docId;
        this.tripName = tripName;
        this.location = location;
        this.details = details;
        this.email = email;
        this.userId = userId;
        this.imageUri = imageUri;
        this.averageRating = averageRating;
        this.ratingCount = ratingCount;
        this.hasUserRated = hasUserRated;
        this.timestamp = timestamp;
    }
}
