package com.hktv.ars.config.quartz;


import com.hktv.ars.enums.CronJobTriggerAndName;
import com.hktv.ars.job.SendToMmsJob;
import com.hktv.ars.repository.CronJobLogDao;
import jakarta.annotation.PostConstruct;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Value("${ars.job.schedule.sendToMms}")
    private String sendToMmsJobSchedule;

    @Autowired
    private Scheduler scheduler;

    @Bean
    public JobListener customJobListener() {
        return new CustomJobListener();
    }

    @PostConstruct
    public void init() throws SchedulerException {
        scheduler.getListenerManager().addJobListener(customJobListener());
    }

    @Bean
    public JobDetail sendToMmsJobDetail() {
        return JobBuilder.newJob(SendToMmsJob.class)
                .withIdentity(CronJobTriggerAndName.SEND_TO_MMS.getDetailIdentity())
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger sendToMmsJobTrigger(JobDetail sendToMmsJobDetail) {
        return TriggerFactory.customTriggerBuild()
                .forJob(sendToMmsJobDetail)
                .withIdentity(CronJobTriggerAndName.SEND_TO_MMS.getTriggerIdentity())
                .withSchedule(CronScheduleBuilder.cronSchedule(sendToMmsJobSchedule))
                .build();
    }

}
