package com.rajesh.aashraynet.scheduler;

import com.rajesh.aashraynet.entity.OccupancySnapshot;
import com.rajesh.aashraynet.repository.DisasterEventRepository;
import com.rajesh.aashraynet.repository.OccupancySnapshotRepository;
import com.rajesh.aashraynet.repository.ShelterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Hourly snapshot per shelter for post-cyclone reports. Skips rows that already exist for this hour (idempotent). */
@Component
@ConditionalOnProperty(prefix = "app.jobs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OccupancySnapshotJob {
    private static final Logger log = LoggerFactory.getLogger(OccupancySnapshotJob.class);
    private final DisasterEventRepository events;
    private final ShelterRepository shelters;
    private final OccupancySnapshotRepository snapshots;

    public OccupancySnapshotJob(DisasterEventRepository events, ShelterRepository shelters,
                                OccupancySnapshotRepository snapshots) {
        this.events = events;
        this.shelters = shelters;
        this.snapshots = snapshots;
    }

    @Scheduled(cron = "${app.jobs.snapshot-cron}")
    @Transactional
    public void run() {
        events.findFirstByActiveTrueOrderByStartDateDesc().ifPresent(event -> {
            LocalDateTime hour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
            int created = 0;
            for (var s : shelters.findByActiveTrue()) {
                if (!snapshots.existsByEventIdAndShelterIdAndTakenAt(event.getId(), s.getId(), hour)) {
                    snapshots.save(new OccupancySnapshot(event.getId(), s.getId(), s.getOccupancy(), s.getCapacity(), hour));
                    created++;
                }
            }
            log.info("Occupancy snapshot {}: {} rows", hour, created);
        });
    }
}
