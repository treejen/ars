package com.hktv.ars.config.quartz;

import com.hktv.ars.enums.CronJobStatusEnum;
import com.hktv.ars.model.CronJobLog;
import com.hktv.ars.repository.CronJobLogDao;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.listeners.JobListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;

public class CustomJobListener extends JobListenerSupport {

    @Autowired
    private CronJobLogDao cronJobLogDao;

    @Override
    public String getName() {
        return "CustomJobListener";
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        String jobName = context.getJobDetail().getKey().getName();
        String jobGroup = context.getJobDetail().getKey().getGroup();


        if (jobException == null) {
            cronJobLogDao.save(CronJobLog.builder()
                    .jobName(jobName)
                    .jobGroup(jobGroup)
                    .status(CronJobStatusEnum.SUCCESS)
                    .build());
        } else {
            cronJobLogDao.save(CronJobLog.builder()
                    .jobName(jobName)
                    .jobGroup(jobGroup)
                    .status(CronJobStatusEnum.FAILED)
                    .errorMessage(jobException.getMessage())
                    .build());
        }
    }
}

