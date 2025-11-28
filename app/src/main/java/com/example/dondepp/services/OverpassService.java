package com.example.dondepp.services;


import com.example.dondepp.model.OverpassResponse;
import retrofit2.Call;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface OverpassService {
    @GET("interpreter/")
    Call<OverpassResponse> searchPlaces(@Query("data") String query);


}
