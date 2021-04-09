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

# 5. Spring Batch Scope & Job Parameter

## 5.1. JobParameter와 Scope

> JobScope

Step 선언문에서 사용

- .start(scopeStep1(null))
- @JobScope 
- scopeStep1

```java
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
```

> StepScope

Tasklet, ItemReader, ItemWriter, ItemProcessor 에서 사용

- .tasklet(scopeStep1Tasklet(null))
- scopeStep1Tasklet

```java
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
```

## 5.2. @StepScope & @JobScope 소개

@JobScope, @StepScope 사용시 지연로딩

> Spring Batch Scope 장점
- JobParameter 의 Late Binding
- 동일한 컴포넌트를 병렬 혹은 동시에 사용할 경우 (각각의 Step에서 별도의 Tasklet을 생성하고 관리)

## 5.3. JobParameter vs 시스템 변수
> 시스템 변수를 사용할 경우
- Spring Batch에서 자동으로 관리해주는 Parameter 관련 메타 테이블을 관리하지 않음
- 전역 상태 (시스템 변수 혹은 환경 변수)를 동적으로 계속해서 변경시킬 수 있도록 Spring Batch를 구성 해야함
- Late Binding을 사용 하지 못함

## 5.4. JobParameter Controller Example

예제코드: Controller 에서 Spring Batch 사용을 권장하지 않음

```java
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
```

# 6. Chunk 지향 처리
- Chunk: 데이터 덩어리로 작업 할 때 각 커밋 사이에 처리되는 row 수.
- Chunk 지향 처리: 한 번에 하나씩 데이터를 읽어 Chunk라는 덩어리를 만든 뒤, Chunk 단위로 트랜잭션을 다루는 것
- 실패할 경우엔 해당 Chunk 만큼만 롤백

> Chunk 지향 처리를 Java 코드로 표현

모든 Item 에서 Read, Process 작업이 이루어진후, 마지막에 모든 Item 을 Write 

```java
for(int i=0; i<totalSize; i+=chunkSize){
    List items = new Arraylist();
    for(int j = 0; j < chunkSize; j++){
        Object item = itemReader.read()
        Object processedItem = itemProcessor.process(item);
        items.add(processedItem);
    }
    itemWriter.write(items);
}
```

## 6.1. ChunkOrientedTasklet

> ChunkOrientedTasklet Class
- chunkProvider.provide(): Reader 에서 Chunk size만큼 데이터 읽음(Read)
- chunkProcessor.process(): Reader로 받은 데이터를 가공(Processor)하고 저장(Writer)

```java
@Nullable
@Override
public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

	@SuppressWarnings("unchecked")
	Chunk<I> inputs = (Chunk<I>) chunkContext.getAttribute(INPUTS_KEY);
	if (inputs == null) {
		inputs = chunkProvider.provide(contribution);
		if (buffering) {
			chunkContext.setAttribute(INPUTS_KEY, inputs);
		}
	}

	chunkProcessor.process(contribution, inputs);
	chunkProvider.postProcess(contribution, inputs);

	// Allow a message coming back from the processor to say that we
	// are not done yet
	if (inputs.isBusy()) {
		logger.debug("Inputs still busy");
		return RepeatStatus.CONTINUABLE;
	}

	chunkContext.removeAttribute(INPUTS_KEY);
	chunkContext.setComplete();

	if (logger.isDebugEnabled()) {
		logger.debug("Inputs not busy, ended: " + inputs.isEnd());
	}
	return RepeatStatus.continueIf(!inputs.isEnd());

}
```

> SimpleChunkProvider
- repeatOperations.iterate: 반복 작업 수행
- item = read(contribution, inputs): Reader.read()
- inputs.add(item): 데이터 추기
- Read 호출 순서: provide > read > doRead 

```java
	@Override
public Chunk<I> provide(final StepContribution contribution) throws Exception {

	final Chunk<I> inputs = new Chunk<>();
	repeatOperations.iterate(new RepeatCallback() {

		@Override
		public RepeatStatus doInIteration(final RepeatContext context) throws Exception {
			I item = null;
			Timer.Sample sample = Timer.start(Metrics.globalRegistry);
			String status = BatchMetrics.STATUS_SUCCESS;
			try {
				item = read(contribution, inputs);
			}
			catch (SkipOverflowException e) {
				// read() tells us about an excess of skips by throwing an
				// exception
				status = BatchMetrics.STATUS_FAILURE;
				return RepeatStatus.FINISHED;
			}
			finally {
				stopTimer(sample, contribution.getStepExecution(), status);
			}
			if (item == null) {
				inputs.setEnd();
				return RepeatStatus.FINISHED;
			}
			inputs.add(item);
			contribution.incrementReadCount();
			return RepeatStatus.CONTINUABLE;
		}

	});

	return inputs;

}

protected I read(StepContribution contribution, Chunk<I> chunk) throws SkipOverflowException, Exception {
	return doRead();
}

@Nullable
protected final I doRead() throws Exception {
	try {
		listener.beforeRead();
		I item = itemReader.read();
		if(item != null) {
			listener.afterRead(item);
		}
		return item;
	}
	catch (Exception e) {
		if (logger.isDebugEnabled()) {
			logger.debug(e.getMessage() + " : " + e.getClass().getName());
		}
		listener.onReadError(e);
		throw e;
	}
}
```

> 
```java

```

## 6.2. SimpleChunkProcessor

- Chunk<O> outputs = transform(contribution, inputs)
  - inputs: chunkProvider.provide() 에서 받은 ChunkSize 만큼 쌓인 item
- transform(): 전달받은 inputs을 doProcess()로 전달
- transform()을 통해 가공된 대량의 데이터는 write()를 통해 일괄 저장

```java
@Override
public final void process(StepContribution contribution, Chunk<I> inputs) throws Exception {

	// Allow temporary state to be stored in the user data field
	initializeUserData(inputs);

	// If there is no input we don't have to do anything more
	if (isComplete(inputs)) {
		return;
	}

	// Make the transformation, calling remove() on the inputs iterator if
	// any items are filtered. Might throw exception and cause rollback.
	Chunk<O> outputs = transform(contribution, inputs);

	// Adjust the filter count based on available data
	contribution.incrementFilterCount(getFilterCount(inputs, outputs));

	// Adjust the outputs if necessary for housekeeping purposes, and then
	// write them out...
	write(contribution, inputs, getAdjustedOutputs(inputs, outputs));

}
```

> Chunk<O> outputs = transform(contribution, inputs)

- doProcess(): 만약 ItemProcessor가 없다면 item을 그대로 반환하고 있다면 ItemProcessor의 process()로 가공하여 반환

```java
protected Chunk<O> transform(StepContribution contribution, Chunk<I> inputs) throws Exception {
	Chunk<O> outputs = new Chunk<>();
	for (Chunk<I>.ChunkIterator iterator = inputs.iterator(); iterator.hasNext();) {
		final I item = iterator.next();
		O output;
		Timer.Sample sample = BatchMetrics.createTimerSample();
		String status = BatchMetrics.STATUS_SUCCESS;
		try {
			output = doProcess(item);
		}
		catch (Exception e) {
			/*
			 * For a simple chunk processor (no fault tolerance) we are done
			 * here, so prevent any more processing of these inputs.
			 */
			inputs.clear();
			status = BatchMetrics.STATUS_FAILURE;
			throw e;
		}
		finally {
			stopTimer(sample, contribution.getStepExecution(), "item.process", status, "Item processing");
		}
		if (output != null) {
			outputs.add(output);
		}
		else {
			iterator.remove();
		}
	}
	return outputs;
}

protected final O doProcess(I item) throws Exception {

	if (itemProcessor == null) {
		@SuppressWarnings("unchecked")
		O result = (O) item;
		return result;
	}

	try {
		listener.beforeProcess(item);
		O result = itemProcessor.process(item);
		listener.afterProcess(item, result);
		return result;
	}
	catch (Exception e) {
		listener.onProcessError(item, e);
		throw e;
	}

}
```

> write(contribution, inputs, getAdjustedOutputs(inputs, outputs))

```java
protected void write(StepContribution contribution, Chunk<I> inputs, Chunk<O> outputs) throws Exception {
	Timer.Sample sample = BatchMetrics.createTimerSample();
	String status = BatchMetrics.STATUS_SUCCESS;
	try {
		doWrite(outputs.getItems());
	}
	catch (Exception e) {
		/*
		 * For a simple chunk processor (no fault tolerance) we are done
		 * here, so prevent any more processing of these inputs.
		 */
		inputs.clear();
		status = BatchMetrics.STATUS_FAILURE;
		throw e;
	}
	finally {
		stopTimer(sample, contribution.getStepExecution(), "chunk.write", status, "Chunk writing");
	}
	contribution.incrementWriteCount(outputs.size());
}

protected final void doWrite(List<O> items) throws Exception {

	if (itemWriter == null) {
		return;
	}

	try {
		listener.beforeWrite(items);
		writeItems(items);
		doAfterWrite(items);
	}
	catch (Exception e) {
		doOnWriteError(e, items);
		throw e;
	}

}

```

## 6.3. Page Size vs Chunk Size

- Chunk Size: 한번에 처리될 트랜잭션 단위를
- Page Size: 한번에 조회할 Item의 양

[ AbstractItemCountingItemStreamItemReader Class ]

```java
@Nullable
@Override
public T read() throws Exception, UnexpectedInputException, ParseException {
	if (currentItemCount >= maxItemCount) {
		return null;
	}
	currentItemCount++;
	T item = doRead();
	if(item instanceof ItemCountAware) {
		((ItemCountAware) item).setItemCount(currentItemCount);
	}
	return item;
}
```

[ AbstractPagingItemReader Class ] 

- doRead()에서는 현재 읽어올 데이터가 없거나, Page Size를 초과한 경우 doReadPage()를 호출 (Page 단위로 끊어서 조회)

```java
@Override
protected T doRead() throws Exception {

	synchronized (lock) {

		if (results == null || current >= pageSize) {

			if (logger.isDebugEnabled()) {
				logger.debug("Reading page " + getPage());
			}

			doReadPage();
			page++;
			if (current >= pageSize) {
				current = 0;
			}

		}

		int next = current++;
		if (next < results.size()) {
			return results.get(next);
		}
		else {
			return null;
		}

	}

}
```

[ JpaPagingItemReader Class ]

- .setFirstResult(getPage() * getPageSize()).setMaxResults(getPageSize()): Page만큼 추가 조회
- results.addAll(query.getResultList()): 조회결과 results에 저장

```java
@Override
@SuppressWarnings("unchecked")
protected void doReadPage() {

	EntityTransaction tx = null;
		
	if (transacted) {
		tx = entityManager.getTransaction();
		tx.begin();
			
		entityManager.flush();
		entityManager.clear();
	}//end if

	Query query = createQuery().setFirstResult(getPage() * getPageSize()).setMaxResults(getPageSize());

	if (parameterValues != null) {
		for (Map.Entry<String, Object> me : parameterValues.entrySet()) {
			query.setParameter(me.getKey(), me.getValue());
		}
	}

	if (results == null) {
		results = new CopyOnWriteArrayList<>();
	}
	else {
		results.clear();
	}
		
	if (!transacted) {
		List<T> queryResult = query.getResultList();
		for (T entity : queryResult) {
			entityManager.detach(entity);
			results.add(entity);
		}//end if
	} else {
		results.addAll(query.getResultList());
		tx.commit();
	}//end if
}
```

ex) PageSize가 10이고, ChunkSize가 50이라면 ItemReader에서 Page 조회가 5번 일어나면 1번 의 트랜잭션이 발생하여 Chunk가 처리
- 한번의 트랜잭션 처리를 위해 5번의 쿼리 조회가 발생하기 때문에 성능상 이슈가 발생

# 7. ItemReader

## 7.1. ItemReader 설명
> Spring Batch의 Chunk Tasklet 과정

데이터(읽기) -> ItemReader -> ItemProcessor -> ItemWriter -> 데이터(쓰기)

> Readerd에서 읽어올 수 있는 데이터 유형
- 입력 데이터
- 파일
- Database
- Java Message Service
- Custom Reader

> ItemReader 구현체

[ ItemReader Interface ]

- read(): 데이터를 읽어오는 메소드

 ```java
public interface ItemReader<T> {

	/**
	 * Reads a piece of input data and advance to the next one. Implementations
	 * <strong>must</strong> return <code>null</code> at the end of the input
	 * data set. In a transactional setting, caller might get the same item
	 * twice from successive calls (or otherwise), if the first call was in a
	 * transaction that rolled back.
	 * 
	 * @throws ParseException if there is a problem parsing the current record
	 * (but the next one may still be valid)
	 * @throws NonTransientResourceException if there is a fatal exception in
	 * the underlying resource. After throwing this exception implementations
	 * should endeavour to return null from subsequent calls to read.
	 * @throws UnexpectedInputException if there is an uncategorised problem
	 * with the input data. Assume potentially transient, so subsequent calls to
	 * read might succeed.
	 * @throws Exception if an there is a non-specific error.
	 * @return T the item to be processed or {@code null} if the data source is
	 * exhausted
	 */
	@Nullable
	T read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException;

}
```
[ ItemStream Interface ]
- 주기적으로 상태를 저장하고 오류가 발생하면 해당 상태에서 복원
- ItemReader의 상태를 저장하고 실패한 곳에서 다시 실행할 수 있게 해주는 역할
- open(), close(): 스트림을 열기, 닫기
- update(): Batch 처리의 상태를 업데이트

```java
public interface ItemStream {

	/**
	 * Open the stream for the provided {@link ExecutionContext}.
	 *
	 * @param executionContext current step's {@link org.springframework.batch.item.ExecutionContext}.  Will be the
	 *                            executionContext from the last run of the step on a restart.
	 * @throws IllegalArgumentException if context is null
	 */
	void open(ExecutionContext executionContext) throws ItemStreamException;

	/**
	 * Indicates that the execution context provided during open is about to be saved. If any state is remaining, but
	 * has not been put in the context, it should be added here.
	 * 
	 * @param executionContext to be updated
	 * @throws IllegalArgumentException if executionContext is null.
	 */
	void update(ExecutionContext executionContext) throws ItemStreamException;

	/**
	 * If any resources are needed for the stream to operate they need to be destroyed here. Once this method has been
	 * called all other methods (except open) may throw an exception.
	 */
	void close() throws ItemStreamException;
}
```

ItemReader와 ItemStream 인터페이스를 직접 구현해서 원하는 형태의 ItemReader 구현 가능

## 7.2. Database Reader
- Cursor는 실제로 JDBC ResultSet의 기본 기능
- ResultSet이 open 될 때마다 next() 메소드가 호출 되어 Database의 데이터가 반환
- Paging: 페이지 단위로 한번에 데이터를 조회해오는 방식

> Cursor와 Paging 비교

- Database -> CursorItemReader(1Row)
  - Cursor: Database와 커넥션을 맺은 후, Cursor를 한칸씩 옮기면서 지속적으로 데이터 Read

- Database -> PagingItemReader(10Row)
  - Paging: Database와 커넥션을 맺은 후, PageSize 만큼 데이터 Read

> 구현체

- Cursor 기반 ItemReader 구현체
  - JdbcCursorItemReader
  - HibernateCursorItemReader
  - StoredProcedureItemReader
  - JpaCursorItemReader
  
- Paging 기반 ItemReader 구현체
  - JdbcPagingItemReader
  - HibernatePagingItemReader
  - JpaPagingItemReader
  
## 7.3. CursorItemReader
- Streaming 으로 데이터를 처리

### 7.3.1. JdbcCursorItemReader

- chunk
  - <Pay, Pay> 에서 첫번째 Pay는 Reader에서 반환할 타입이며, 두번째 Pay는 Writer에 파라미터로 넘어올 타입
  - chunkSize로 인자값을 넣은 경우는 Reader & Writer가 묶일 Chunk 트랜잭션 범위

- fetchSize
  - Database에서 한번에 가져올 데이터 양
  - Paging은 실제 쿼리를 limit, offset을 이용해서 분할 처리하는 반면, Cursor는 쿼리는 분할 처리 없이 실행되나 내부적으로 가져오는 데이터는 FetchSize만큼 가져와 read()를 통해서 하나씩 가져옴

- dataSource
  - Database에 접근하기 위해 사용할 Datasource 객체를 할당
  
- rowMapper
  - 쿼리 결과를 Java 인스턴스로 매핑하기 위한 Mapper
  - 커스텀하게 생성해서 사용할 수 도 있지만, 이렇게 될 경우 매번 Mapper 클래스를 생성해야 되서 보편적으로는 Spring에서 공식적으로 지원하는 BeanPropertyRowMapper.class를 사용
  
- sql
  - Reader로 사용할 쿼리문
  
- name
  - reader의 이름을 지정
  - Bean의 이름이 아니며 Spring Batch의 ExecutionContext에서 저장되어질 이름

```java
@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Pay {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private Long amount;
  private String txName;
  private LocalDateTime txDateTime;

  public Pay(Long amount, String txName, String txDateTime) {
    this.amount = amount;
    this.txName = txName;
    this.txDateTime = LocalDateTime.parse(txDateTime, FORMATTER);
  }
}

@Slf4j
@RequiredArgsConstructor
@Configuration
public class JdbcCursorItemReaderJobConfiguration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final DataSource dataSource; // DataSource DI

  private static final int chunkSize = 10;

  @Bean
  public Job jdbcCursorItemReaderJob() {
    return jobBuilderFactory.get("jdbcCursorItemReaderJob")
        .start(jdbcCursorItemReaderStep())
        .build();
  }

  @Bean
  public Step jdbcCursorItemReaderStep() {
    return stepBuilderFactory.get("jdbcCursorItemReaderStep")
        .<Pay, Pay>chunk(chunkSize)
        .reader(jdbcCursorItemReader())
        .writer(jdbcCursorItemWriter())
        .build();
  }

  @Bean
  public JdbcCursorItemReader<Pay> jdbcCursorItemReader() {
    return new JdbcCursorItemReaderBuilder<Pay>()
        .fetchSize(chunkSize)
        .dataSource(dataSource)
        .rowMapper(new BeanPropertyRowMapper<>(Pay.class))
        .sql("SELECT id, amount, tx_name, tx_date_time FROM pay")
        .name("jdbcCursorItemReader")
        .build();
  }

  private ItemWriter<Pay> jdbcCursorItemWriter() {
    return list -> {
      for (Pay pay: list) {
        log.info("Current Pay={}", pay);
      }
    };
  }
}
```

> 결과
```text
Current Pay=Pay(id=1, amount=1000, txName=trade1, txDateTime=2018-09-10T00:00)
Current Pay=Pay(id=2, amount=2000, txName=trade2, txDateTime=2018-09-10T00:00)
Current Pay=Pay(id=3, amount=3000, txName=trade3, txDateTime=2018-09-10T00:00)
Current Pay=Pay(id=4, amount=4000, txName=trade4, txDateTime=2018-09-10T00:00)
```

> 주의사항
- CursorItemReader를 사용하실때는 Database와 SocketTimeout을 충분히 큰 값으로 설정
- Cursor는 하나의 Connection으로 Batch가 끝날때까지 사용되기 때문에 Batch가 끝나기전에 Database와 어플리케이션의 Connection이 먼저 끊어질수 있음

## 7.4. PagingItemReader
- Paging: Database Cursor를 사용하는 대신 여러 쿼리를 실행하여 각 쿼리가 결과의 일부를 가져 오는 방법
- Spring Batch에서는 offset과 limit을 PageSize에 맞게 자동으로 생성

### 7.4.1. JdbcPagingItemReader

```java
@Slf4j
@RequiredArgsConstructor
@Configuration
public class JdbcPagingItemReaderJobConfiguration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final DataSource dataSource; // DataSource DI

  private static final int chunkSize = 10;

  @Bean
  public Job jdbcPagingItemReaderJob() throws Exception {
    return jobBuilderFactory.get("jdbcPagingItemReaderJob")
        .start(jdbcPagingItemReaderStep())
        .build();
  }

  @Bean
  public Step jdbcPagingItemReaderStep() throws Exception {
    return stepBuilderFactory.get("jdbcPagingItemReaderStep")
        .<Pay, Pay>chunk(chunkSize)
        .reader(jdbcPagingItemReader())
        .writer(jdbcPagingItemWriter())
        .build();
  }

  @Bean
  public JdbcPagingItemReader<Pay> jdbcPagingItemReader() throws Exception {
    Map<String, Object> parameterValues = new HashMap<>();
    parameterValues.put("amount", 2000);

    return new JdbcPagingItemReaderBuilder<Pay>()
        .pageSize(chunkSize)
        .fetchSize(chunkSize)
        .dataSource(dataSource)
        .rowMapper(new BeanPropertyRowMapper<>(Pay.class))
        .queryProvider(createQueryProvider())
        .parameterValues(parameterValues)
        .name("jdbcPagingItemReader")
        .build();
  }

  private ItemWriter<Pay> jdbcPagingItemWriter() {
    return list -> {
      for (Pay pay: list) {
        log.info("Current Pay={}", pay);
      }
    };
  }

  @Bean
  public PagingQueryProvider createQueryProvider() throws Exception {
    SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
    queryProvider.setDataSource(dataSource); // Database에 맞는 PagingQueryProvider를 선택하기 위해
    queryProvider.setSelectClause("id, amount, tx_name, tx_date_time");
    queryProvider.setFromClause("from pay");
    queryProvider.setWhereClause("where amount >= :amount");

    Map<String, Order> sortKeys = new HashMap<>(1);
    sortKeys.put("id", Order.ASCENDING);

    queryProvider.setSortKeys(sortKeys);

    return queryProvider.getObject();
  }
}
```

> Database Paging Provider
[ SqlPagingQueryProviderFactoryBean Class ]
- Db2PagingQueryProvider
- Db2PagingQueryProvider
- Db2PagingQueryProvider
- Db2PagingQueryProvider
- DerbyPagingQueryProvider
- HsqlPagingQueryProvider
- H2PagingQueryProvider
- MySqlPagingQueryProvider
- OraclePagingQueryProvider
- PostgresPagingQueryProvider
- SqlitePagingQueryProvider
- SqlServerPagingQueryProvider
- SybasePagingQueryProvider

> Parameter 사용
- 쿼리에 대한 매개 변수 값의 Map을 지정
- queryProvider.setWhereClause을 사용하여 Parameter 사용

```java
@Bean
public JdbcPagingItemReader<Pay> jdbcPagingItemReader() throws Exception {
  Map<String, Object> parameterValues = new HashMap<>();
  parameterValues.put("amount", 2000);
  .....
}

@Bean
public PagingQueryProvider createQueryProvider() throws Exception {
  .....
  queryProvider.setWhereClause("where amount >= :amount");
  .....
}
```

### 7.4.2. JpaPagingItemReader
- JPA에는 Cursor 기반 Database 접근을 지원하지 않음

```java
@Slf4j
@RequiredArgsConstructor
@Configuration
public class JpaPagingItemReaderJobConfiguration {
  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final EntityManagerFactory entityManagerFactory;

  private int chunkSize = 10;

  @Bean
  public Job jpaPagingItemReaderJob() {
    return jobBuilderFactory.get("jpaPagingItemReaderJob")
        .start(jpaPagingItemReaderStep())
        .build();
  }

  @Bean
  public Step jpaPagingItemReaderStep() {
    return stepBuilderFactory.get("jpaPagingItemReaderStep")
        .<Pay, Pay>chunk(chunkSize)
        .reader(jpaPagingItemReader())
        .writer(jpaPagingItemWriter())
        .build();
  }

  @Bean
  public JpaPagingItemReader<Pay> jpaPagingItemReader() {
    return new JpaPagingItemReaderBuilder<Pay>()
        .name("jpaPagingItemReader")
        .entityManagerFactory(entityManagerFactory)
        .pageSize(chunkSize)
        .queryString("SELECT p FROM Pay p WHERE amount >= 2000")
        .build();
  }

  private ItemWriter<Pay> jpaPagingItemWriter() {
    return list -> {
      for (Pay pay: list) {
        log.info("Current Pay={}", pay);
      }
    };
  }
}
```

### 7.4.3. PagingItemReader 주의 사항
- 정렬 (Order) 가 무조건 포함되어 있어야함

## 7.5. ItemReader 주의 사항
- JpaRepository를 ListItemReader, QueueItemReader에 사용하면 안됨
  - Spring Batch의 장점인 페이징 & Cursor 구현이 없어 대규모 데이터 처리가 불가능 ( hunk 단위 트랜잭션은 가능)
  - JpaRepository를 사용할 경우 RepositoryItemReader를 사용
- Hibernate, JPA 등 영속성 컨텍스트가 필요한 Reader 사용시 fetchSize와 ChunkSize는 같은 값을 유지

# 8. ItemWriter

## 8.1. ItemWriter 설명
- Spring Batch에서 사용하는 출력 기능
- Item 을 List 형태로 받음

```java
public interface ItemWriter<T> {

	/**
	 * Process the supplied data element. Will not be called with any null items
	 * in normal operation.
	 *
	 * @param items items to be written
	 * @throws Exception if there are errors. The framework will catch the
	 * exception and convert or rethrow it as appropriate.
	 */
	void write(List<? extends T> items) throws Exception;

}
```

> ItemReader -> ItemProcessor -> ItemWriter
- ItemReader를 통해 각 항목을 개별적으로 읽고 이를 처리하기 위해 ItemProcessor에 전달 (Chunk 의 Item 개수 만큼 처리)
- ItemProcessor 에서 처리가 완료되면 Chunk 단위만큼 ItemWriter 에서 일괄 처리.

## 8.2. Database Writer

- JPA 사용시 wirte 부분의 영속성 관리를 위하여 flush 사용됨

[ JpaItemWriter ]
```java
@Override
public void write(List<? extends T> items) {
	EntityManager entityManager = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
	if (entityManager == null) {
		throw new DataAccessResourceFailureException("Unable to obtain a transactional EntityManager");
	}
	doWrite(entityManager, items);
	entityManager.flush();
}
```

> Writer
- JdbcBatchItemWriter
- HibernateItemWriter
- JpaItemWriter

## 8.3. JdbcBatchItemWriter

- JDBC의 Batch 기능을 사용하여 한번에 Database로 전달하여 Database 내부에서 쿼리들이 실행 (Database Connection 최적화)
  - .batchUpdate(sql, items.toArray(new Map[items.size()]));
  - .batchUpdate(sql, batchArgs);
  - ps.addBatch();

```java
public void write(final List<? extends T> items) throws Exception {

	if (!items.isEmpty()) {

		if (logger.isDebugEnabled()) {
			logger.debug("Executing batch with " + items.size() + " items.");
		}

		int[] updateCounts;

		if (usingNamedParameters) {
			if(items.get(0) instanceof Map && this.itemSqlParameterSourceProvider == null) {
				updateCounts = namedParameterJdbcTemplate.batchUpdate(sql, items.toArray(new Map[items.size()]));
			} else {
				SqlParameterSource[] batchArgs = new SqlParameterSource[items.size()];
				int i = 0;
				for (T item : items) {
					batchArgs[i++] = itemSqlParameterSourceProvider.createSqlParameterSource(item);
				}
				updateCounts = namedParameterJdbcTemplate.batchUpdate(sql, batchArgs);
			}
		}
		else {
			updateCounts = namedParameterJdbcTemplate.getJdbcOperations().execute(sql, new PreparedStatementCallback<int[]>() {
				@Override
				public int[] doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
					for (T item : items) {
						itemPreparedStatementSetter.setValues(item, ps);
						ps.addBatch();
					}
					return ps.executeBatch();
				}
			});
		}

		if (assertUpdates) {
			for (int i = 0; i < updateCounts.length; i++) {
				int value = updateCounts[i];
				if (value == 0) {
					throw new EmptyResultDataAccessException("Item " + i + " of " + updateCounts.length
							+ " did not update any rows: [" + items.get(i) + "]", 1);
				}
			}
		}
	}
}
```

### 8.3.1. JdbcBatchItemWriter 예제코드

- JdbcBatchItemWriter의 제네릭 타입은 Reader에서 넘겨주는 값의 타입

```java
@Slf4j
@RequiredArgsConstructor
@Configuration
public class JdbcBatchItemWriterJobConfiguration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final DataSource dataSource;

  private static final int chunkSize = 10;

  @Bean
  public Job jdbcBatchItemWriterJob() {

    return jobBuilderFactory.get("jdbcBatchItemWriterJob")
        .start(jdbcBatchItemWriterStep())
        .build();

  }

  private Step jdbcBatchItemWriterStep() {
    return stepBuilderFactory.get("jdbcBatchItemWriterStep")
        .<Pay, Pay>chunk(chunkSize)
        .reader(jdbcBatchItemWriterReader())
        .writer(jdbcBatchItemWriter())
        .build();
  }

  private JdbcBatchItemWriter<Pay> jdbcBatchItemWriter() {

    JdbcBatchItemWriter<Pay> jdbcBatchItemWriterBuilder = new JdbcBatchItemWriterBuilder<Pay>()
        .dataSource(dataSource)
        .sql("INSERT INTO pay2(amount, tx_name, tx_date_time) VALUES(:amount, :txName, :txDateTime)")
        .beanMapped()
        .build();
    
    jdbcBatchItemWriterBuilder.afterPropertiesSet();;

    return jdbcBatchItemWriterBuilder;
  }

  private JdbcCursorItemReader<Pay> jdbcBatchItemWriterReader() {
    return new JdbcCursorItemReaderBuilder<Pay>()
        .dataSource(dataSource)
        .sql("SELECT id, amount, tx_name, tx_date_time FROM pay")
        .name("jdbcBatchItemReader")
        .build();
  }

}
```

|Property     |Parameter Type|설명                                                                                                             |
|-------------|--------------|---------------------------------------------------------------------------------------------------------------|
|assertUpdates|boolean       |적어도 하나의 항목이 행을 업데이트하거나 삭제하지 않을 경우 예외를 throw할지 여부를 설정. 기본값은 true. Exception:EmptyResultDataAccessException|
|columnMapped |없음            |Key,Value 기반으로 Insert SQL의 Values를 매핑 (ex: Map<String, Object>)                                             |
|beanMapped   |없음            |Pojo 기반으로 Insert SQL의 Values를 매핑  

> columnMapped vs beanMapped
- Reader에서 Writer로 넘겨주는 타입
  - columnMapped: Map<String, Object>
  - beanMapped: Pojo 타입

### 8.3.2. afterPropertiesSet

- Writer들이 실행되기 위해 필요한 필수값 확인 Method

[ JdbcBatchItemWriter Class ]

```java
	@Override
	public void afterPropertiesSet() {
		Assert.notNull(namedParameterJdbcTemplate, "A DataSource or a NamedParameterJdbcTemplate is required.");
		Assert.notNull(sql, "An SQL statement is required.");
		List<String> namedParameters = new ArrayList<>();
		parameterCount = JdbcParameterUtils.countParameterPlaceholders(sql, namedParameters);
		if (namedParameters.size() > 0) {
			if (parameterCount != namedParameters.size()) {
				throw new InvalidDataAccessApiUsageException("You can't use both named parameters and classic \"?\" placeholders: " + sql);
			}
			usingNamedParameters = true;
		}
		if (!usingNamedParameters) {
			Assert.notNull(itemPreparedStatementSetter, "Using SQL statement with '?' placeholders requires an ItemPreparedStatementSetter");
		}
	}
```
## 8.4. JpaItemWriter

- JpaItemWriter는 JPA를 사용하기 때문에 영속성 관리를 위해 EntityManager를 할당
- JpaItemWriter는 넘어온 Entity를 데이터베이스에 반영

```java
@Slf4j
@RequiredArgsConstructor
@Configuration
public class JpaItemWriterJobConfiguration {

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final EntityManagerFactory entityManagerFactory;

  private static final int chunkSize = 10;

  @Bean
  public Job jpaItemWriterJob() {
    return jobBuilderFactory.get("jpaItemWriterJob")
        .start(jpaItemWriterStep())
        .build();
  }

  @Bean
  public Step jpaItemWriterStep() {
    return stepBuilderFactory.get("jpaItemWriterStep")
        .<Pay, Pay2>chunk(chunkSize)
        .reader(jpaItemWriterReader())
        .processor(jpaItemProcessor())
        .writer(jpaItemWriter())
        .build();
  }

  @Bean
  public JpaItemWriter<Pay2> jpaItemWriter() {
    JpaItemWriter<Pay2> jpaItemWriter = new JpaItemWriter<>();
    jpaItemWriter.setEntityManagerFactory(entityManagerFactory);
    return jpaItemWriter;
  }

  @Bean
  public ItemProcessor<Pay, Pay2> jpaItemProcessor() {
    return pay -> new Pay2(pay.getAmount(), pay.getTxName(), pay.getTxDateTime());
  }

  @Bean
  public JpaPagingItemReader<Pay> jpaItemWriterReader() {
    return new JpaPagingItemReaderBuilder<Pay>()
        .name("jpaItemWriterReader")
        .entityManagerFactory(entityManagerFactory)
        .pageSize(chunkSize)
        .queryString("SELECT p FROM pay p")
        .build();
  }

}
```

[ JpaItemWriter ]

- EntityManager는 필수 설정

```java
@Override
public void afterPropertiesSet() throws Exception {
	Assert.notNull(entityManagerFactory, "An EntityManagerFactory is required");
}
```

```text
Hibernate: insert into pay2(id, amount, tx_date_time, tx_name) values(null, ?, ?, ?)
Hibernate: insert into pay2(id, amount, tx_date_time, tx_name) values(null, ?, ?, ?)
Hibernate: insert into pay2(id, amount, tx_date_time, tx_name) values(null, ?, ?, ?)
Hibernate: insert into pay2(id, amount, tx_date_time, tx_name) values(null, ?, ?, ?)
```



[ JpaItemWriter Class ]

- JpaItemWriter 는 넘어온 Item을 그대로 entityManger.merge()로 테이블에 반영

```java
@Override
public void write(List<? extends T> items) {
	EntityManager entityManager = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
	if (entityManager == null) {
		throw new DataAccessResourceFailureException("Unable to obtain a transactional EntityManager");
	}
	doWrite(entityManager, items);
	entityManager.flush();
}

protected void doWrite(EntityManager entityManager, List<? extends T> items) {

	if (logger.isDebugEnabled()) {
		logger.debug("Writing to JPA with " + items.size() + " items.");
	}

	if (!items.isEmpty()) {
		long addedToContextCount = 0;
		for (T item : items) {
			if (!entityManager.contains(item)) {
				if(usePersist) {
					entityManager.persist(item);
				}
				else {
					entityManager.merge(item);
				}					
				addedToContextCount++;
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug(addedToContextCount + " entities " + (usePersist ? " persisted." : "merged."));
			logger.debug((items.size() - addedToContextCount) + " entities found in persistence context.");
		}
	}
}
```
## 8.5. Custom ItemWriter

- Spring Batch에서 공식적으로 지원하지 않는 Writer를 사용하고 싶을때 ItemWriter인터페이스를 구현
- ItemWriter 에서 write() override 하여 구현

```java
@Slf4j
@RequiredArgsConstructor
@Configuration
public class CustomItemWriterJobConfiguration {
  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final EntityManagerFactory entityManagerFactory;

  private static final int chunkSize = 10;

  @Bean
  public Job customItemWriterJob() {
    return jobBuilderFactory.get("customItemWriterJob")
        .start(customItemWriterStep())
        .build();
  }

  @Bean
  public Step customItemWriterStep() {
    return stepBuilderFactory.get("customItemWriterStep")
        .<Pay, Pay2>chunk(chunkSize)
        .reader(customItemWriterReader())
        .processor(customItemWriterProcessor())
        .writer(customItemWriter())
        .build();
  }

  @Bean
  public JpaPagingItemReader<Pay> customItemWriterReader() {
    return new JpaPagingItemReaderBuilder<Pay>()
        .name("customItemWriterReader")
        .entityManagerFactory(entityManagerFactory)
        .pageSize(chunkSize)
        .queryString("SELECT p FROM Pay p")
        .build();
  }

  @Bean
  public ItemProcessor<Pay, Pay2> customItemWriterProcessor() {
    return pay -> new Pay2(pay.getAmount(), pay.getTxName(), pay.getTxDateTime());
  }

  @Bean
  public ItemWriter<Pay2> customItemWriter() {
    return new ItemWriter<Pay2>() {
      @Override
      public void write(List<? extends Pay2> items) throws Exception {
        for (Pay2 item : items) {
          System.out.println(item);
        }
      }
    };
  }
}
```

# 9. ItemProcessor

- ItemProcessor는 필수 X
- ItemProcessor 사용이유: Reader, Writer에 비지니스 코드가 섞이는 것을 방지

## 9.1. ItemProcessor 설명

- 변환
  - Reader에서 읽은 데이터를 원하는 타입으로 변환해서 Writer에 전달
- 필터
  - Reader에서 넘겨준 데이터를 Writer로 넘겨줄 것인지를 결정
  - null을 반환하면 Writer에 전달되지 않음
  
```java
public interface ItemProcessor<I, O> {

	@Nullable
	O process(@NonNull I item) throws Exception;
}
```

> Generic Type
- I: ItemReader에서 받은 데이터 타입
- O: IteamWriter에 보낼 데이터 타입

> Lambda
- 장점
  - 불필요한 코드 가 없어 구현 코드 양이 적음
  - 고정된 형태가 없어 원하는 형태의 처리도 가능
- 단점
  - Batch Config 클래스 안에 포함되어 있어야만 해서 Batch Config의 코드 양이 많아질 수 있음
  
## 9.2. 변환

> 예제 코드
```java
@Slf4j
@RequiredArgsConstructor
@Configuration
public class ProcessorConvertJobConfiguration {

  public static final String JOB_NAME = "ProcessorConvertBatch";
  public static final String BEAN_PREFIX = JOB_NAME + "_";

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final EntityManagerFactory emf;

  @Value("${chunkSize:1000}")
  private int chunkSize;

  @Bean(JOB_NAME)
  public Job job() {

    return jobBuilderFactory.get(JOB_NAME)
        .preventRestart()
        .start(step())
        .build();

  }

  @Bean(BEAN_PREFIX + "step")
  public Step step() {
    return stepBuilderFactory.get(BEAN_PREFIX + "step")
        .<Pay, String>chunk(chunkSize)
        .reader(reader())
        .processor(processor())
        .writer(writer())
        .build();
  }

  private ItemWriter<String> writer() {
    return items -> {
      for (String item : items) {
        log.info("Pay Tx Name: " + item);
      }
    };
  }

  private ItemProcessor<Pay, String> processor() {
    return pay -> pay.getTxName();
  }

  private JpaPagingItemReader<Pay> reader() {
    return new JpaPagingItemReaderBuilder<Pay>()
        .name(BEAN_PREFIX + "reader")
        .entityManagerFactory(emf)
        .pageSize(chunkSize)
        .queryString("SELECT p FROM Pay p")
        .build();
  }

}
```

- <Pay, String>
  - Pay: Reader에서 읽어올 타입
  - String: Writer에 넘겨줄 타입
  
- <Pay, String>chunk(chunkSize)
  - Pay: Reader 타입
  - String: Writer 타입 
  
```text
@Bean
public ItemProcessor<Pay, String> processor() {
    return teacher -> {
        return teacher.getName();
    };
}

@Bean(BEAN_PREFIX + "step")
public Step step() {
  return stepBuilderFactory.get(BEAN_PREFIX + "step")
      .<Pay, String>chunk(chunkSize)
      .reader(reader())
      .processor(processor())
      .writer(writer())
      .build();
}
```

```text
INFO 15711 --- [  restartedMain] c.s.b.j.ProcessorConvertJobConfiguration : Pay Tx Name: trade1
INFO 15711 --- [  restartedMain] c.s.b.j.ProcessorConvertJobConfiguration : Pay Tx Name: trade2
INFO 15711 --- [  restartedMain] c.s.b.j.ProcessorConvertJobConfiguration : Pay Tx Name: trade3
INFO 15711 --- [  restartedMain] c.s.b.j.ProcessorConvertJobConfiguration : Pay Tx Name: trade4
```
## 9.3. 필터
 Writer에 값을 넘길지 말지를 Processor에서 판단하는 것

```java
@Slf4j
@RequiredArgsConstructor
@Configuration
public class ProcessorNullJobConfiguration {

  public static final String JOB_NAME = "processorNullBatch";
  public static final String BEAN_PREFIX = JOB_NAME + "_";

  private final JobBuilderFactory jobBuilderFactory;
  private final StepBuilderFactory stepBuilderFactory;
  private final EntityManagerFactory emf;


  @Value("${chunkSize:1000}")
  private int chunkSize;


  @Bean(JOB_NAME)
  public Job job() {
    return jobBuilderFactory.get(JOB_NAME)
        .preventRestart()
        .start(step())
        .build();
  }

  @Bean(BEAN_PREFIX + "step")
  public Step step() {
    return stepBuilderFactory.get(BEAN_PREFIX + "step")
        .<Pay, Pay>chunk(chunkSize)
        .reader(reader())
        .processor(processor())
        .writer(writer())
        .build();
  }

  private ItemWriter<Pay> writer() {

    return items -> {
      for (Pay item : items) {
        log.info("Pay Tx Name={}", item.getTxName());
      }
    };
  }

  private ItemProcessor<Pay, Pay> processor() {
    return pay -> {

      boolean isIgnoreTarget = pay.getId() % 2 == 0L;
      if (isIgnoreTarget) {
        log.info(">>>>> Pay Tx Name={},  isIgnoreTarget={}", pay.getTxName(), isIgnoreTarget);
        return null;
      }
      return pay;
    };
  }

  private JpaPagingItemReader<Pay> reader() {
    return new JpaPagingItemReaderBuilder<Pay>()
        .name(BEAN_PREFIX + "reader")
        .pageSize(chunkSize)
        .entityManagerFactory(emf)
        .queryString("SELECT p FROM Pay p")
        .build();
  }

}
```

```text
INFO 29642 --- [  restartedMain] c.s.b.job.ProcessorNullJobConfiguration  : >>>>> Pay Tx Name=trade2,  isIgnoreTarget=true
INFO 29642 --- [  restartedMain] c.s.b.job.ProcessorNullJobConfiguration  : >>>>> Pay Tx Name=trade4,  isIgnoreTarget=true
INFO 29642 --- [  restartedMain] c.s.b.job.ProcessorNullJobConfiguration  : Pay Tx Name=trade1
INFO 29642 --- [  restartedMain] c.s.b.job.ProcessorNullJobConfiguration  : Pay Tx Name=trade3
```