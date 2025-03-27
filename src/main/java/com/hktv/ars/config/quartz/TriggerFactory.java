package com.hktv.ars.config.quartz;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TriggerFactory {

    public static TriggerBuilder customTriggerBuild() {
        return new TriggerBuilder();
    }

    public static class TriggerBuilder {
        private JobDetail jobDetail;
        private String identity;
        private CronScheduleBuilder scheduleBuilder;

        public TriggerBuilder forJob(JobDetail jobDetail) {
            this.jobDetail = jobDetail;
            return this;
        }

        public TriggerBuilder withIdentity(String identity) {
            this.identity = identity;
            return this;
        }

        public TriggerBuilder withSchedule(CronScheduleBuilder scheduleBuilder) {
            this.scheduleBuilder = scheduleBuilder;
            return this;
        }

        public Trigger build() {
            if (jobDetail == null || identity == null || scheduleBuilder == null) {
                throw new IllegalStateException("JobDetail, identity, and scheduleBuilder must be set");
            }
            CronTrigger cronTrigger = org.quartz.TriggerBuilder.newTrigger()
                    .forJob(jobDetail)
                    .withIdentity(identity)
                    .withSchedule(scheduleBuilder)
                    .build();
            log.info("{}:{}", jobDetail.getKey().getName(), cronTrigger.getCronExpression());
            return cronTrigger;
        }
    }
}
