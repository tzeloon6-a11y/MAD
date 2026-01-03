package com.example.mad;

import java.util.ArrayList;
import java.util.List;
public class SavedJobsManager {
    private static final SavedJobsManager instance = new SavedJobsManager();
    private final List<Job> savedJobs = new ArrayList<>();

    private SavedJobsManager() {}

    public static SavedJobsManager getInstance() {
        return instance;
    }

    public void saveJob(Job job) {
        boolean exists = false;
        for (Job savedJob : savedJobs) {
            if (savedJob.getJobId().equals(job.getJobId())) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            savedJobs.add(job);
        }
    }

    public void removeJob(Job job) {
        savedJobs.remove(job);
    }

    public List<Job> getSavedJobs() {
        return new ArrayList<>(savedJobs);
    }
}

