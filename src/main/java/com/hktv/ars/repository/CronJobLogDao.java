package com.hktv.ars.repository;


import com.hktv.ars.model.CronJobLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CronJobLogDao extends JpaRepository<CronJobLog, Long> {

    @Query(value = "select * from cron_job_log " +
            "where job_name = :jobName and job_group = :jobGroup " +
            "order by creation_date desc limit 1",
            nativeQuery = true)
    Optional<CronJobLog> findCurrentByJobNameAndGroup(String jobName, String jobGroup);
}
