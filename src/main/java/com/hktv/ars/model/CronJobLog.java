package com.hktv.ars.model;

import com.hktv.ars.enums.CronJobStatusEnum;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@Entity
@Table(name = "cron_job_log")
@AllArgsConstructor
@NoArgsConstructor
public class CronJobLog extends BaseModel {
    private String jobName;
    private String jobGroup;
    @Enumerated(EnumType.STRING)
    private CronJobStatusEnum status;
    private String errorMessage;
}
