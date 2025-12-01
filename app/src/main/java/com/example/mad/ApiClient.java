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

    // Base URL for your local PHP backend
    // For Emulator: 10.0.2.2 replaces localhost
    public static final String BASE_URL = "http://10.0.2.2/mad/";
}

