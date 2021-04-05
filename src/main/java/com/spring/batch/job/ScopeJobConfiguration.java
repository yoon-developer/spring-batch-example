package com.spring.batch.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class ScopeJobConfiguration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  @Bean
  public Job scopeJob() {
    return jobBuilderFactory.get("simpleJob")
        .start(scopeStep1(null))
        .build();
  }

  @Bean
  @JobScope
  public Step scopeStep1(@Value("#{jobParameters[requestDate]}") String requestDate) {
    return stepBuilderFactory.get("simpleStep1")
        .tasklet(scopeStep1Tasklet(null))
        .build();
  }

  @Bean
  @StepScope
  public Tasklet scopeStep1Tasklet(@Value("#{jobParameters[requestDate]}") String requestDate) {
    return ((contribution, chunkContext) -> {
      log.info(">>>>> This is ScopeStep1Tasklet");
      log.info(">>>>> requestDate = {}", requestDate);
      return RepeatStatus.FINISHED;
    });
  }
}
