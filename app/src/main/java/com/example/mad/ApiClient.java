
package com.example.mad;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class ApiClient {

    public static final String BASE_URL =
            SupabaseConfig.SUPABASE_URL + "/rest/v1/";

    private static RequestQueue requestQueue;

    public static RequestQueue getRequestQueue(Context context) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return requestQueue;
    }

    // ✅ REQUIRED HEADERS FOR SUPABASE
    public static Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("apikey", SupabaseConfig.SUPABASE_KEY);
        headers.put("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY);
        headers.put("Content-Type", "application/json");
        return headers;
    }
}

