package com.example.mad;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PostStorage {
    private static final String PREFS = "PostPrefs";
    private static final String KEY_EXPERIENCE_POSTS = "experience_posts";

    public static void savePost(Context context, ExperiencePost post) {
        List<ExperiencePost> list = loadPosts(context);
        list.add(0, post); // add to top
        saveList(context, list);
    }

    public static List<ExperiencePost> loadPosts(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_EXPERIENCE_POSTS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<ExperiencePost>>(){}.getType();
        return new Gson().fromJson(json, type);
    }

    private static void saveList(Context context, List<ExperiencePost> list) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = new Gson().toJson(list);
        prefs.edit().putString(KEY_EXPERIENCE_POSTS, json).apply();
    }
}
