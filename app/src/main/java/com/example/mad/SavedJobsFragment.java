package com.example.mad;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
public class SavedJobsFragment extends Fragment{
    private RecyclerView recyclerView;
    private SavedJobAdapter adapter;
    private List<Job> savedJobsList;
    private TextView emptyView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_jobs, container, false);

        recyclerView = view.findViewById(R.id.saved_jobs_recyclerview);
        emptyView = view.findViewById(R.id.empty_view_saved_jobs);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        savedJobsList = SavedJobsManager.getInstance().getSavedJobs();
        adapter = new SavedJobAdapter(savedJobsList);
        recyclerView.setAdapter(adapter);

        adapter.setOnButtonClickListener(new SavedJobAdapter.OnButtonClickListener() {
            @Override
            public void onInterestedClicked(int position) {
                if (position >= 0 && position < savedJobsList.size()) {
                    Job job = savedJobsList.get(position);
                    Toast.makeText(getContext(), "Interested in " + job.getTitle(), Toast.LENGTH_SHORT).show();
                    // In a real app, you would add logic to save this interest to a database here
                    SavedJobsManager.getInstance().removeJob(job);
                    savedJobsList.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, savedJobsList.size());
                    checkIfEmpty();
                }
            }

            @Override
            public void onNotNowClicked(int position) {
                if (position >= 0 && position < savedJobsList.size()) {
                    Job job = savedJobsList.get(position);
                    SavedJobsManager.getInstance().removeJob(job);
                    savedJobsList.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, savedJobsList.size());
                    checkIfEmpty();
                }
            }
        });

        checkIfEmpty();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            savedJobsList.clear();
            savedJobsList.addAll(SavedJobsManager.getInstance().getSavedJobs());
            adapter.notifyDataSetChanged();
            checkIfEmpty();
        }
    }

    private void checkIfEmpty() {
        if (savedJobsList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }
}
