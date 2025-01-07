package com.example.nightguard;

import com.example.nightguard.Report;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public interface ApiService {

    // GET all reports
    @GET("/api/reports")
    Call<List<Report>> getAllReports();

    //POST to create report
    @POST("/api/reports")
    Call<Report> createReport(@Body Report report);

}
