package io.github.senagithub.tracker.activity;

import io.github.senagithub.tracker.content.Content;
import io.github.senagithub.tracker.content.ContentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    /** Joins the caller's transaction: if the content change rolls back, so does its log entry. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Content content, ActivityAction action, ContentStatus from, ContentStatus to) {
        activityLogRepository.save(new ActivityLog(content, action, from, to));
    }
}
