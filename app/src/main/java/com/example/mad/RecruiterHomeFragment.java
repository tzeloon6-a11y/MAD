package com.example.mad;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class RecruiterHomeFragment extends Fragment {

    RecyclerView recyclerView;
    RecruiterAdapter adapter;
    List<StudentModel> studentList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 1. Inflate the layout (This loads the XML you created in Step 4)
        View view = inflater.inflate(R.layout.fragment_recruiter_home, container, false);

        // 2. Find the RecyclerView by ID
        recyclerView = view.findViewById(R.id.rvCandidates);

        // 3. Set how the items are arranged (Vertical list)
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 4. Create Dummy Data (We will replace this with Supabase later)
        studentList = new ArrayList<>();
        studentList.add(new StudentModel("Alice Tan", "Software Engineering"));
        studentList.add(new StudentModel("Brandon Lee", "Data Science"));
        studentList.add(new StudentModel("Charlie Ng", "Graphic Design"));
        studentList.add(new StudentModel("Diana Lim", "Cyber Security"));
        studentList.add(new StudentModel("Ethan Teoh", "Mobile Development"));

        // 5. Connect the Adapter (The code you wrote in Step 3)
        adapter = new RecruiterAdapter(studentList);
        recyclerView.setAdapter(adapter);

        return view;
    }
}