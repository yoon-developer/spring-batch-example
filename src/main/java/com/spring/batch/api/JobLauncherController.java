package com.spring.batch.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class JobLauncherController {

  private final JobLauncher jobLauncher;
  private final Job job;

  @GetMapping("/launchjob")
  public String handle(@RequestParam("fileName") String fileName) throws Exception {

    try {
      JobParameters jobParameter = new JobParametersBuilder()
          .addString("input.file.name", fileName)
          .addLong("time", System.currentTimeMillis())
          .toJobParameters();

      jobLauncher.run(job, jobParameter);

    } catch (Exception e) {
      log.error(e.getMessage());
    }

    return "Done";
  }

}
