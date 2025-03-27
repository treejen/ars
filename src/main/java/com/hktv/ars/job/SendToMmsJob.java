package com.hktv.ars.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SendToMmsJob extends ArsJob {

    public SendToMmsJob() {
    }

    @Override
    protected void executeJob(JobExecutionContext jobExecutionContext) {

    }
}
