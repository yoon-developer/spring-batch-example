Spring Batch
==========

# 1. 설명
## 1.1. Batch ?
ETL 과정을 일정한 시간과 순서, 조건에 따라 수행하는 작업

## 1.2. ETL ?
- 추출(Extract)
- 변환(Transformation)
- 적재(Load)

## 1.3. 구성요소
> JobLauncher
```text
- Job을 실행시키는 역할
- Job과 Parameter를 받아서 실행
- JobExecution 반환
```

> Job
```text
- 하나의 배치 실행 단위
```

> JobParameter
```text
- Job 을 식별하거나 참조하는 데이터로 사용
```

> JobInstance
```text
- JobInstance = Job + JobParameter
```

> JobExecution
```text
- JobInstance가 실행되는 것을 의미
- JobInstance는 JobExecution 1:1 관계
- 시작시간, 종료시간, 상태(시작됨, 완료, 실패) 종료상태의 속성을 포함
```

> JobRepository
```text
- 수행되는 Job에 대한 정보를 담고 있는 저장소
- Batch 수행과 광련된 모든 Meta Data 저장 (시작시간, 종료시간, 상태)
```

> Step
```text
- Batch Job을 구성하는 독립적인 하나의 단계
- 모든 Job은 1개 이상의 Step을 가짐
- 실제 배치 처리 과정을 정의하고, 제어하는 필요한 모든 정보를 포함
```

> Step Execution
```text
- 하나의 Step을 실행하는 한번의 시도
- 시작시간, 종료시간, 상태, 종료 상태, Commit 개수, Item 개수 의 속성을 포함
```

> Item
```text
- 처리할 데이터의 가장 작은 구성 요소
```

> ItemReader
```text
- Step안에서 File 또는 DB등으로 Item을 읽음
```

> ItemWriter
```text
- Step안에서 File 또는 DB등으로 Item을 저장
```

> ItemProcessor
```text
- ItemReader에서 읽어 들인 Item에 대하여 필요한 로직처리 작업을 수행
```

> Chunk
```text
- 하나의 Transaction안에서 처리 할 Item의 모임
```

# 2. Batch Job
## 2.1. 환경 구성

> 버전  
- spring-boot-starter-batch: 2.4.4 
- lombok: 1.18.18
- spring-boot-devtools: 2.4.4
- h2: 1.4.200

> dependencies 추가
- org.springframework.boot:spring-boot-starter-batch
- org.projectlombok:lombok
- org.springframework.boot:spring-boot-devtools
- com.h2database:h2

```text
dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-batch'
	compileOnly 'org.projectlombok:lombok'
	developmentOnly 'org.springframework.boot:spring-boot-devtools'
	runtimeOnly 'com.h2database:h2'
	annotationProcessor 'org.projectlombok:lombok'
	testImplementation 'org.springframework.boot:spring-boot-starter-test'
	testImplementation 'org.springframework.batch:spring-batch-test'
}
```

## 2.2. Job 생성

> Batch Process 활성화
- @EnableBatchProcessing

```java
@EnableBatchProcessing
@SpringBootApplication
public class BatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(BatchApplication.class, args);
	}

}
```

> Job 설정
- @Configuration: Spring Batch의 모든 Job은 @Configuration으로 등록해서 사용
- @JobScope: Job 실행시점에 Bean 생성
- JobBuilderFactory, StepBuilderFactory DI
- jobBuilderFactory.get("job"): job 생성
- stepBuilderFactory.get("step1"): step 생성
- .tasklet: 기능 정의

```java
@Slf4j
@RequiredArgsConstructor
@Configuration
public class SimpleJobConfiguration {
  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  @Bean
  public Job simpleJob() {
    return jobBuilderFactory.get("simpleJob")
        .start(simpleStep1(null))
        .build();
  }

  @Bean
  @JobScope
  public Step simpleStep1(@Value("#{jobParameters[requestDate]}") String requestDate) {
    return stepBuilderFactory.get("simpleStep1")
        .tasklet((contribution, chunkContext) -> {
          log.info(">>>>> This is Step1");
          log.info(">>>>> requestDate = {}", requestDate);
          return RepeatStatus.FINISHED;
        })
        .build();
  }
}
```

> KEY 설정

Progran arguments 설정 (ex: requestDate=20210331)

## 2.3. DB 연동
> DB Table 설정 (파일 참고)
- schema-h2.sql 
- schema-mysql.sql
- schema-postgresql.sql


> application.yml 설정
- 실행 Job 지정
  - spring.batch.job:names: ${job.name:secondJob}

```yaml
spring:
  datasource:
    url: jdbc:h2:tcp://localhost/~/batch
    username: sa
    password:
    driver-class-name: org.h2.Driver

  jpa:
    show-sql: true
    hibernate:
      ddl-auto: create
    generate-ddl: true
    properties:
      hibernate:
        format_sql: true


  batch:
    job:
      names: ${job.name:secondJob}
    initialize-schema: always
```

# 3. DB Table 설명
> BATCH_JOB_INSTANCE

- JOB_INSTANCE_ID: 인스턴스를 식별하는 고유 ID. 기본 키
- VERSION: 해당 레코드에 update 될때마다 1씩 증가
- JOB_NAME: Job 이름
- JOB_KEY: 동일한 이름의 Job 을 실행시점에 부여되는 고유한 JobParameter의 값을 통해 식별

|JOB_INSTANCE_ID|VERSION|JOB_NAME|JOB_KEY                         |
|---------------|-------|--------|--------------------------------|
|1              |0      |job     |d41d8cd98f00b204e9800998ecf8427e|

> BATCH_JOB_EXECUTION

- JOB_EXECUTION_ID: JOB EXECUTION 기본 키
- VERSION: 해당 레코드에 update 될때마다 1씩 증가
- JOB_INSTANCE_ID: BATCH_JOB_INSTANCE 테이블의 외래 키
- CREATE_TIME: 실행이 생성 된 시간을 나타내는 타임 스탬프
- START_TIME: 실행이 시작된 시간을 나타내는 타임 스탬프
- END_TIME: 성공 여부에 관계없이 실행이 완료된 시간을 나타내는 타임 스탬프
- STATUS: 실행 상태를 나타내는 문자열
- EXIT_CODE: 실행 종료 코드를 나타내는 문자열
- EXIT_MESSAGE: 작업이 종료 된 방법에 대한 자세한 설명을 나타내는 문자열
- LAST_UPDATED: 실행이 마지막으로 지속 된 시간을 나타내는 타임 스탬프

|JOB_EXECUTION_ID|VERSION|JOB_INSTANCE_ID|CREATE_TIME                     |START_TIME             |END_TIME               |STATUS   |EXIT_CODE|EXIT_MESSAGE|LAST_UPDATED           |JOB_CONFIGURATION_LOCATION|
|----------------|-------|---------------|--------------------------------|-----------------------|-----------------------|---------|---------|------------|-----------------------|--------------------------|
|1               |2      |1              |2021-03-30 10:26:12.632         |2021-03-30 10:26:12.653|2021-03-30 10:26:12.677|COMPLETED|COMPLETED|            |2021-03-30 10:26:12.677|null                      |

> BATCH_JOB_EXECUTION_PARAMS

- JOB_EXECUTION_ID: BATCH_JOB_EXECUTION 테이블의 외래키
- TYPE_CD: 저장된 값 유형의 문자열 표현
- KEY_NAME: 매개 변수 키
- STRING_VAL: 유형이 문자열 인 경우 매개 변수 값
- DATE_VAL: 유형이 날짜 인 경우 매개 변수 값
- LONG_VAL: 유형이 Long 인 경우 매개 변수 값
- DOUBLE_VAL: 유형이 double 인 경우 매개 변수 값
- IDENTIFYING: 매개 변수가 관련 .NET의 ID에 기여했는지 여부를 나타내는 플래그

|JOB_EXECUTION_ID|TYPE_CD|KEY_NAME|STRING_VAL                      |DATE_VAL               |LONG_VAL               |DOUBLE_VAL|IDENTIFYING|
|----------------|-------|--------|--------------------------------|-----------------------|-----------------------|----------|-----------|
|2               |STRING |requestDate|20210330                        |1970-01-01 09:00:00    |0                      |0.0       |Y          |

> BATCH_JOB_EXECUTION_CONTEXT

- JOB_EXECUTION_ID: BATCH_JOB_EXECUTION 테이블의 외래키
- SHORT_CONTEXT: 문자열 버전의 SERIALIZED_CONTEXT
- SERIALIZED_CONTEXT: 직렬화(serialized)된 전체 컨테스트

|JOB_EXECUTION_ID|SHORT_CONTEXT|SERIALIZED_CONTEXT|
|------------------|-------------|------------------|
|1                 |{"@class":"java.util.HashMap"}|null              |

> BATCH_STEP_EXECUTION

- STEP_EXECUTION_ID: STEP EXECUTION 기본 키
- VERSION: 해당 레코드에 update 될때마다 1씩 증가
- STEP_NAME: step 이름
- JOB_EXECUTION_ID: BATCH_JOB_EXECUTION 테이블의 외래 키
- START_TIME: 실행이 시작된 시간을 나타내는 타임 스탬프
- END_TIME: 성공 또는 실패 여부에 관계없이 실행이 완료된 시간을 나타내는 타임 스탬프
- STATUS: 실행 상태를 나타내는 문자열
- COMMIT_COUNT: 트랜잭션을 커밋 한 횟수
- READ_COUNT: 읽은 항목 수
- FILTER_COUNT: 필터링 된 항목 수
- WRITE_COUNT: 작성 및 커밋 된 항목 수
- EAD_SKIP_COUNT: 읽기에서 건너 뛴 항목 수
- WRITE_SKIP_COUNT: 쓰기에서 건너 뛴 항목 수
- PROCESS_SKIP_COUNT: 처리 중에 건너 뛴 항목 수
- ROLLBACK_COUNT: 롤백 수. 재시도 및 건너 뛰기 복구 절차의 롤백을 포함하여 롤백이 발생할 때마다 포함
- EXIT_CODE: 종료 코드를 나타내는 문자열
- EXIT_MESSAGE: 작업이 종료 된 방법에 대한 자세한 설명을 나타내는 문자열
- LAST_UPDATED: 실행이 마지막으로 지속 된 시간을 나타내는 타임 스탬프

|STEP_EXECUTION_ID|VERSION|STEP_NAME|JOB_EXECUTION_ID|START_TIME             |END_TIME               |STATUS   |COMMIT_COUNT|READ_COUNT|FILTER_COUNT|WRITE_COUNT|READ_SKIP_COUNT|WRITE_SKIP_COUNT|PROCESS_SKIP_COUNT|ROLLBACK_COUNT|EXIT_CODE|EXIT_MESSAGE|LAST_UPDATED           |
|-----------------|-------|---------|----------------|-----------------------|-----------------------|---------|------------|----------|------------|-----------|---------------|----------------|------------------|--------------|---------|------------|-----------------------|
|1                |3      |step1    |1               |2021-03-30 10:26:12.664|2021-03-30 10:26:12.674|COMPLETED|1           |0         |0           |0          |0              |0               |0                 |0             |COMPLETED|            |2021-03-30 10:26:12.674|



> BATCH_STEP_EXECUTION_CONTEXT

- STEP_EXECUTION_ID: BATCH_STEP_EXECUTION 테이블의 외래키
- SHORT_CONTEXT: 문자열 버전의 SERIALIZED_CONTEXT
- SERIALIZED_CONTEXT: 직렬화(serialized)된 전체 컨테스트

|STEP_EXECUTION_ID|SHORT_CONTEXT|SERIALIZED_CONTEXT|
|-----------------|-------------|------------------|
|1                |{"@class":"java.util.HashMap","batch.taskletType":"com.spring.batch.job.JobConfig$$Lambda$406/0x000000084028f040","batch.stepType":"org.springframework.batch.core.step.tasklet.TaskletStep"}|null              |

# 4. 조건별 흐름 제어 

Step A  ->   (YES)  - > STEP B  
Step A  ->   (NO)   - > STEP C 

```java
@RequiredArgsConstructor
@Configuration
public class StepNextConditionalJobConfiguration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  @Bean
  public Job stepNextConditionalJob() {
    return jobBuilderFactory.get("stepNextConditionalJob")
        .start(conditionalJobStep1()) // ~ STEP 실행 
          .on("FAILED") // ~ STEP ExitStatus 결과가 ~ 일 경우
          .to(conditionalJobStep3()) // ~ STEP 실행
          .on("*")  // ~ STEP ExitStatus 결과가 ~ 일 경우
          .end() // STEP 종료
        .from(conditionalJobStep1()) // ~ STEP 실행
          .on("*") // ~ STEP ExitStatus 결과가 ~ 일 경우
          .to(conditionalJobStep2()) // ~ STEP 실행
          .next(conditionalJobStep3()) // 정상 종료될 경우 ~ STEP 실행
          .on("*") // ~ STEP ExitStatus 결과가 ~ 일 경우
          .end() // STEP 종료
        .end() // JOB 종료
        .build();
  }

  @Bean
  public Step conditionalJobStep1() {
    return stepBuilderFactory.get("step1")
        .tasklet((contribution, chunkContext) -> {
          log.info(">>>>> This is stepNextConditionalJob Step1");

          contribution.setExitStatus(ExitStatus.FAILED);

          return RepeatStatus.FINISHED;
        })
        .build();
  }

  @Bean
  public Step conditionalJobStep2() {
    return stepBuilderFactory.get("conditionalJobStep2")
        .tasklet((contribution, chunkContext) -> {
          log.info(">>>>> This is stepNextConditionalJob Step2");
          return RepeatStatus.FINISHED;
        })
        .build();
  }

  @Bean
  public Step conditionalJobStep3() {
    return stepBuilderFactory.get("conditionalJobStep3")
        .tasklet((contribution, chunkContext) -> {
          log.info(">>>>> This is stepNextConditionalJob Step3");
          return RepeatStatus.FINISHED;
        })
        .build();
  }
}
```

## 4.1. Batch Status

Job 또는 Step 의 실행 결과를 Spring에서 기록할 때 사용하는 Enum
- COMPLETED
- STARTING
- STARTED
- STOPPING
- STOPPED
- FAILED
- ABANDONED
- UNKNOWN

## 4.2. Exit Status
 
Step의 실행 후 상태
- EXECUTING
- COMPLETED
- NOOP
- FAILED
- STOPPED

## 4.3. Decide

- FlowExecutionStatus: 상태 관리

```java
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DeciderJobConfiguration {
  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  @Bean
  public Job deciderJob() {
    return jobBuilderFactory.get("deciderJob")
        .start(startStep())
        .next(decider()) // ~ METHOD 실행
          .from(decider()) 
          .on("ODD") // ~ METHOD 실행 상태가 ~~ 일 경우
          .to(oddStep()) // STEP 실행
        .from(decider())
          .on("EVEN") // ~ METHOD 실행 상태가 ~~ 일 경우
          .to(evenStep()) // STEP 실행 
        .end()
        .build();
  }

  @Bean
  public Step startStep() {
    return stepBuilderFactory.get("startStep")
        .tasklet((contribution, chunkContext) -> {
          log.info(">>>>> Start!");
          return RepeatStatus.FINISHED;
        })
        .build();
  }

  @Bean
  public Step evenStep() {
    return stepBuilderFactory.get("evenStep")
        .tasklet((contribution, chunkContext) -> {
          log.info(">>>>> 짝수입니다.");
          return RepeatStatus.FINISHED;
        })
        .build();
  }

  @Bean
  public Step oddStep() {
    return stepBuilderFactory.get("oddStep")
        .tasklet((contribution, chunkContext) -> {
          log.info(">>>>> 홀수입니다.");
          return RepeatStatus.FINISHED;
        })
        .build();
  }

  @Bean
  public JobExecutionDecider decider() {
    return new OddDecider();
  }

  public static class OddDecider implements JobExecutionDecider {

    @Override
    public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
      Random rand = new Random();

      int randomNumber = rand.nextInt(50) + 1;
      log.info("랜덤숫자: {}", randomNumber);

      if(randomNumber % 2 == 0) {
        return new FlowExecutionStatus("EVEN");
      } else {
        return new FlowExecutionStatus("ODD");
      }
    }
  }
}
```

