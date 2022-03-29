package com.springbatchexample.config;

import com.springbatchexample.entity.Student;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowJobBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.xstream.XStreamMarshaller;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@EnableBatchProcessing
@Configuration
public class SpringBatchConfig {


    @Autowired
    private DataSource dataSource;

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    public StaxEventItemReader xmlItemReader() {
        return new StaxEventItemReaderBuilder<Student>()
                .name("itemReader")
                .resource(new ClassPathResource("data.xml"))
                .addFragmentRootElements("student")
                .unmarshaller(studentMarshaller())
                .build();

    }

    public XStreamMarshaller studentMarshaller() {
        Map<String, Class> aliases = new HashMap<>();
        aliases.put("student", Student.class);
        aliases.put("id", Long.class);
        aliases.put("rollNumber", String.class);
        aliases.put("name", String.class);
        XStreamMarshaller marshaller = new XStreamMarshaller();
        marshaller.setAliases(aliases);
        return marshaller;
    }


    @Bean
    public StudentItemProcessor processor() {
        return new StudentItemProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<Student> writer() {
        JdbcBatchItemWriter<Student> writer = new JdbcBatchItemWriter<>();
        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
        writer.setSql("insert into student(id,roll_number,name) values (:id,:rollNumber,:name)");
        writer.setDataSource(dataSource);
        return writer;
    }


    @Bean
    public Job writeStudentDataIntoSqlDb() {
        JobBuilder jobBuilder = jobBuilderFactory.get("STUDENT_JOB");
        jobBuilder.incrementer(new RunIdIncrementer());
        FlowJobBuilder flowJobBuilder = jobBuilder.flow(getFirstStep()).end();
        Job job = flowJobBuilder.build();
        return job;
    }

    @Bean
    public Step getFirstStep() {
        StepBuilder stepBuilder = stepBuilderFactory.get("getFirstStep");
        SimpleStepBuilder<Student, Student> simpleStepBuilder = stepBuilder.chunk(1);
        return simpleStepBuilder.reader(xmlItemReader()).processor(processor()).writer(writer()).build();
    }

}
