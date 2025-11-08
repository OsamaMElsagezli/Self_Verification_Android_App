package com.example.myapp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    public static UploadApi create(String baseUrl) {
        HttpLoggingInterceptor log = new HttpLoggingInterceptor();
        log.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient ok = new OkHttpClient.Builder()
                .addInterceptor(log)
                .build();

        Gson gson = new GsonBuilder().create();

        Retrofit r = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(ok)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        return r.create(UploadApi.class);
    }
}
