package com.hktv.ars.job;

import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
@Slf4j
public abstract class ArsJob implements Job {
    @Override
    @Observed
    public void execute(JobExecutionContext jobExecutionContext)  throws JobExecutionException {
        try {
            long startTime = System.currentTimeMillis();
            String jobName = jobExecutionContext.getJobDetail().getKey().getName();
            log.info("Start job: [{}]", jobName);
            executeJob(jobExecutionContext);
            log.info("Finish job: [{}], used [{}] milliseconds.", jobName, (System.currentTimeMillis() - startTime));
        } catch (Exception e) {
            log.error("Error job: [{}]", jobExecutionContext.getJobDetail().getKey().getName(), e);
            throw new JobExecutionException(e);
        }
    }

    protected abstract void executeJob(JobExecutionContext jobExecutionContext);
}
