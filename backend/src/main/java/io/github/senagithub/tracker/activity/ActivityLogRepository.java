package io.github.senagithub.tracker.activity;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    /** Newest first; id breaks ties between entries written in the same instant. */
    @EntityGraph(attributePaths = {"content", "content.client"})
    List<ActivityLog> findTop10ByOrderByCreatedAtDescIdDesc();
}
