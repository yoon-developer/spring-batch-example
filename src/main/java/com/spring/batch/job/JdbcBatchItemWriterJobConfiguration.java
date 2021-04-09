//package com.spring.batch.job;
//
//import com.spring.batch.domain.Pay;
//import javax.sql.DataSource;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.batch.core.Job;
//import org.springframework.batch.core.Step;
//import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
//import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
//import org.springframework.batch.item.database.JdbcBatchItemWriter;
//import org.springframework.batch.item.database.JdbcCursorItemReader;
//import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
//import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Slf4j
//@RequiredArgsConstructor
//@Configuration
//public class JdbcBatchItemWriterJobConfiguration {
//
//  private final JobBuilderFactory jobBuilderFactory;
//  private final StepBuilderFactory stepBuilderFactory;
//  private final DataSource dataSource;
//
//  private static final int chunkSize = 10;
//
//  @Bean
//  public Job jdbcBatchItemWriterJob() {
//    return jobBuilderFactory.get("jdbcBatchItemWriterJob")
//        .start(jdbcBatchItemWriterStep())
//        .build();
//
//  }
//
//  private Step jdbcBatchItemWriterStep() {
//    return stepBuilderFactory.get("jdbcBatchItemWriterStep")
//        .<Pay, Pay>chunk(chunkSize)
//        .reader(jdbcBatchItemWriterReader())
//        .writer(jdbcBatchItemWriter())
//        .build();
//  }
//
//  private JdbcBatchItemWriter<Pay> jdbcBatchItemWriter() {
//
//    JdbcBatchItemWriter<Pay> jdbcBatchItemWriterBuilder = new JdbcBatchItemWriterBuilder<Pay>()
//        .dataSource(dataSource)
//        .sql("INSERT INTO pay2(amount, tx_name, tx_date_time) VALUES(:amount, :txName, :txDateTime)")
//        .beanMapped()
//        .build();
//
//    jdbcBatchItemWriterBuilder.afterPropertiesSet();;
//
//    return jdbcBatchItemWriterBuilder;
//  }
//
//  private JdbcCursorItemReader<Pay> jdbcBatchItemWriterReader() {
//    return new JdbcCursorItemReaderBuilder<Pay>()
//        .dataSource(dataSource)
//        .sql("SELECT id, amount, tx_name, tx_date_time FROM pay")
//        .name("jdbcBatchItemReader")
//        .build();
//  }
//
//}
