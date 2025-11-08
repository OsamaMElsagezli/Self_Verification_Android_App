package com.example.myapp;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface UploadApi {

    // TODO: change the path "upload/passport" to match your server route
    @Multipart
    @POST("upload/passport")
    Call<UploadResponse> uploadPassport(
            @Part MultipartBody.Part file,
            @Part("username") RequestBody username
    );

    Call<UploadResponse> upload(RequestBody usernameBody, MultipartBody.Part pFront, MultipartBody.Part pLeft, MultipartBody.Part pRight, MultipartBody.Part pBlink, MultipartBody.Part pSmile, MultipartBody.Part pPassport);
}
