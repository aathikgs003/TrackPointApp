package com.example.trackpoint.network;

import android.content.Context;
import com.example.trackpoint.S3Config;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit = null;
    private static TrackPointApiService apiService = null;

    public static synchronized TrackPointApiService getApiService(Context context) {
        if (apiService == null) {
            if (context != null) {
                S3Config.init(context.getApplicationContext());
            }

            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            String baseUrl = S3Config.API_BASE_URL;
            if (baseUrl == null || baseUrl.isEmpty() || !baseUrl.startsWith("http")) {
                baseUrl = "https://api.trackpoint.app/";
            }
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(TrackPointApiService.class);
        }
        return apiService;
    }

    public static synchronized void resetInstance() {
        retrofit = null;
        apiService = null;
    }
}
