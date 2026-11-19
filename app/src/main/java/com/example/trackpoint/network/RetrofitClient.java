package com.example.trackpoint.network;

import android.content.Context;
import com.example.trackpoint.S3Config;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit = null;
    private static TrackPointApiService apiService = null;

    public static synchronized TrackPointApiService getApiService(Context context) {
        if (context != null) {
            S3Config.init(context.getApplicationContext());
        }

        String baseUrl = S3Config.API_BASE_URL;
        if (baseUrl == null || baseUrl.isEmpty() || !baseUrl.startsWith("http")) {
            baseUrl = "https://clear-mirrors-live.loca.lt/";
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        if (apiService == null || retrofit == null || !retrofit.baseUrl().toString().equalsIgnoreCase(baseUrl)) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request request = original.newBuilder()
                                .header("Bypass-Tunnel-Reminder", "true")
                                .header("User-Agent", "TrackPointAndroidApp")
                                .method(original.method(), original.body())
                                .build();
                        return chain.proceed(request);
                    })
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .build();

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
