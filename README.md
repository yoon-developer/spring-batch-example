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