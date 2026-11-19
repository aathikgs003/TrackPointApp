package com.example.trackpoint;

import android.content.Context;
import java.io.InputStream;
import java.util.Properties;

public class S3Config {

    public static String ENDPOINT = "https://s3.us-east-005.backblazeb2.com";
    public static String REGION = "us-east-005";
    public static String ACCESS_KEY_ID = "0055d972f4bbe320000000001";
    public static String SECRET_ACCESS_KEY = "K005L7F8nbdagudltmppBnYy0qm294s";
    public static String BUCKET_NAME = "trackpoint--data";
    public static String API_BASE_URL = "https://strong-eagle-44.loca.lt/";

    private static boolean isInitialized = false;

    public static synchronized void init(Context context) {
        if (context == null) return;

        try (InputStream inputStream = context.getAssets().open(".env")) {
            Properties properties = new Properties();
            properties.load(inputStream);

            String endpoint = properties.getProperty("AWS_S3_ENDPOINT");
            if (endpoint != null && !endpoint.isEmpty() && !endpoint.contains("<YOUR")) {
                ENDPOINT = endpoint.trim();
            }

            String region = properties.getProperty("AWS_S3_REGION");
            if (region != null && !region.isEmpty() && !region.contains("<YOUR")) {
                REGION = region.trim();
            }

            String keyId = properties.getProperty("AWS_S3_ACCESS_KEY_ID");
            if (keyId != null && !keyId.isEmpty() && !keyId.contains("<YOUR")) {
                ACCESS_KEY_ID = keyId.trim();
            }

            String secretKey = properties.getProperty("AWS_S3_SECRET_ACCESS_KEY");
            if (secretKey != null && !secretKey.isEmpty() && !secretKey.contains("<YOUR")) {
                SECRET_ACCESS_KEY = secretKey.trim();
            }

            String bucket = properties.getProperty("AWS_S3_BUCKET_NAME");
            if (bucket != null && !bucket.isEmpty() && !bucket.contains("<YOUR")) {
                BUCKET_NAME = bucket.trim();
            }

            String apiUrl = properties.getProperty("API_BASE_URL");
            if (apiUrl != null && !apiUrl.isEmpty() && !apiUrl.contains("<YOUR")) {
                API_BASE_URL = apiUrl.trim();
                if (!API_BASE_URL.endsWith("/")) {
                    API_BASE_URL += "/";
                }
            }

            isInitialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getFormattedEndpoint() {
        if (ENDPOINT == null || ENDPOINT.isEmpty()) {
            return "https://s3.us-east-005.backblazeb2.com";
        }
        if (!ENDPOINT.startsWith("http://") && !ENDPOINT.startsWith("https://")) {
            return "https://" + ENDPOINT;
        }
        return ENDPOINT;
    }
}
