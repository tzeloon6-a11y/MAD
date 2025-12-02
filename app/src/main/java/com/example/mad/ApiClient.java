package com.example.mad;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class ApiClient {

    private static RequestQueue requestQueue;

    public static RequestQueue getRequestQueue(Context context) {
        if (requestQueue == null) {
            // Use application context to avoid leaking Activity
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return requestQueue;
    }

    // Base URL for your backend
    // IF USING EMULATOR: Use "http://10.0.2.2/mad/" (assuming XAMPP/WAMP is running)
    // IF USING REAL PHONE: Use your PC's IP, e.g., "http://192.168.1.5/mad/"
    // Free hosting (InfinityFree) often blocks mobile apps, so Localhost is better for testing.
    public static final String BASE_URL = "http://10.0.2.2/mad/";

}
