package com.spring.batch.job;

import java.util.Date;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class JobConfiguration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  @Bean
  public Job simpleJob() {
    return jobBuilderFactory.get("빌링")
        .start(step1(null))
        .next(step2(null))
        .build();
  }

  @Bean
  @JobScope
  public Step step1(@Value("#{jobParameters[requestDate]}") String requestDate) {
    return stepBuilderFactory.get("step1")
        .tasklet((contribution, chunkContext) -> {
//          throw  new IllegalArgumentException("Step1 failed");
          log.info(">>>>> This is Step1");
          log.info(">>>>> requestDate = {}", requestDate);
          return RepeatStatus.FINISHED;
        })
        .build();
  }

  @Bean
  @JobScope
  public Step step2(@Value("#{jobParameters[requestDate]}") String requestDate) {
    return stepBuilderFactory.get("simpleStep2")
        .tasklet((contribution, chunkContext) -> {
          log.info(">>>>> This is Step2");
          log.info(">>>>> requestDate = {}", requestDate);
          return RepeatStatus.FINISHED;
        })
        .build();
  }
}
