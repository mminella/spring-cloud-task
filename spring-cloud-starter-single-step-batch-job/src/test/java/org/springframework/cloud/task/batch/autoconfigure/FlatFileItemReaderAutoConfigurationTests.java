/*
 * Copyright 2020-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.task.batch.autoconfigure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineCallbackHandler;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.separator.RecordSeparatorPolicy;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.support.ListItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael Minella
 */
public class FlatFileItemReaderAutoConfigurationTests {

	/**
	 * Contents of the file to be read (included here because it's UTF-16):
	 *
	 * <pre>
	 * 1@2@3@4@5@six
	 * # This should be ignored
	 * 7@8@9@10@11@twelve
	 * $ So should this
	 * 13@14@15@16@17@eighteen
	 * 19@20@21@22@23@%twenty four%
	 * 15@26@27@28@29@thirty
	 * 31@32@33@34@35@thirty six
	 * 37@38@39@40@41@forty two
	 * 43@44@45@46@47@forty eight
	 * 49@50@51@52@53@fifty four
	 * 55@56@57@58@59@sixty
	 * </pre>
	 */
	@Test
	public void testFullDelimitedConfiguration() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(JobConfiguration.class)
				.withConfiguration(
						AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
								BatchAutoConfiguration.class,
								SingleStepJobAutoConfiguration.class,
								FlatFileItemReaderAutoConfiguration.class))
				.withPropertyValues("spring.batch.job.jobName=job",
						"spring.batch.job.stepName=step1", "spring.batch.job.chunkSize=5",
						"spring.batch.job.flatfilereader.savestate=true",
						"spring.batch.job.flatfilereader.name=fullDelimitedConfiguration",
						"spring.batch.job.flatfilereader.maxItemCount=5",
						"spring.batch.job.flatfilereader.currentItemCount=2",
						"spring.batch.job.flatfilereader.comments=#,$",
						"spring.batch.job.flatfilereader.resource=/testUTF16.csv",
						"spring.batch.job.flatfilereader.strict=true",
						"spring.batch.job.flatfilereader.encoding=UTF-16",
						"spring.batch.job.flatfilereader.linesToSkip=1",
						"spring.batch.job.flatfilereader.delimited=true",
						"spring.batch.job.flatfilereader.delimiter=@",
						"spring.batch.job.flatfilereader.quoteCharacter=%",
						"spring.batch.job.flatfilereader.includedFields=1,3,5",
						"spring.batch.job.flatfilereader.names=foo,bar,baz",
						"spring.batch.job.flatfilereader.parsingStrict=false");

		applicationContextRunner.run((context) -> {
			JobLauncher jobLauncher = context.getBean(JobLauncher.class);

			Job job = context.getBean(Job.class);

			ListItemWriter itemWriter = context.getBean(ListItemWriter.class);

			JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

			JobExplorer jobExplorer = context.getBean(JobExplorer.class);

			while (jobExplorer.getJobExecution(jobExecution.getJobId()).isRunning()) {
				Thread.sleep(1000);
			}

			List writtenItems = itemWriter.getWrittenItems();

			assertThat(writtenItems.size()).isEqualTo(3);
			assertThat(((Map) writtenItems.get(0)).get("foo")).isEqualTo("20");
			assertThat(((Map) writtenItems.get(0)).get("bar")).isEqualTo("22");
			assertThat(((Map) writtenItems.get(0)).get("baz")).isEqualTo("twenty four");
			assertThat(((Map) writtenItems.get(1)).get("foo")).isEqualTo("26");
			assertThat(((Map) writtenItems.get(1)).get("bar")).isEqualTo("28");
			assertThat(((Map) writtenItems.get(1)).get("baz")).isEqualTo("thirty");
			assertThat(((Map) writtenItems.get(2)).get("foo")).isEqualTo("32");
			assertThat(((Map) writtenItems.get(2)).get("bar")).isEqualTo("34");
			assertThat(((Map) writtenItems.get(2)).get("baz")).isEqualTo("thirty six");
		});
	}

	@Test
	public void testFixedWidthConfiguration() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(JobConfiguration.class)
				.withConfiguration(AutoConfigurations.of(
						PropertyPlaceholderAutoConfiguration.class,
						BatchAutoConfiguration.class,
						SingleStepJobAutoConfiguration.class,
						FlatFileItemReaderAutoConfiguration.class, RangeConverter.class))
				.withPropertyValues("spring.batch.job.jobName=job",
						"spring.batch.job.stepName=step1", "spring.batch.job.chunkSize=5",
						"spring.batch.job.flatfilereader.savestate=true",
						"spring.batch.job.flatfilereader.name=fixedWidthConfiguration",
						"spring.batch.job.flatfilereader.comments=#,$",
						"spring.batch.job.flatfilereader.resource=/test.txt",
						"spring.batch.job.flatfilereader.strict=true",
						"spring.batch.job.flatfilereader.fixedLength=true",
						"spring.batch.job.flatfilereader.ranges=3-4,7-8,11",
						"spring.batch.job.flatfilereader.names=foo,bar,baz",
						"spring.batch.job.flatfilereader.parsingStrict=false");

		applicationContextRunner.run((context) -> {
			JobLauncher jobLauncher = context.getBean(JobLauncher.class);

			Job job = context.getBean(Job.class);

			ListItemWriter itemWriter = context.getBean(ListItemWriter.class);

			JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

			JobExplorer jobExplorer = context.getBean(JobExplorer.class);

			while (jobExplorer.getJobExecution(jobExecution.getJobId()).isRunning()) {
				Thread.sleep(1000);
			}

			List writtenItems = itemWriter.getWrittenItems();

			assertThat(writtenItems.size()).isEqualTo(6);
			assertThat(((Map) writtenItems.get(0)).get("foo")).isEqualTo("2");
			assertThat(((Map) writtenItems.get(0)).get("bar")).isEqualTo("4");
			assertThat(((Map) writtenItems.get(0)).get("baz")).isEqualTo("six");
			assertThat(((Map) writtenItems.get(1)).get("foo")).isEqualTo("8");
			assertThat(((Map) writtenItems.get(1)).get("bar")).isEqualTo("10");
			assertThat(((Map) writtenItems.get(1)).get("baz")).isEqualTo("twelve");
			assertThat(((Map) writtenItems.get(2)).get("foo")).isEqualTo("14");
			assertThat(((Map) writtenItems.get(2)).get("bar")).isEqualTo("16");
			assertThat(((Map) writtenItems.get(2)).get("baz")).isEqualTo("eighteen");
			assertThat(((Map) writtenItems.get(3)).get("foo")).isEqualTo("20");
			assertThat(((Map) writtenItems.get(3)).get("bar")).isEqualTo("22");
			assertThat(((Map) writtenItems.get(3)).get("baz")).isEqualTo("twenty four");
			assertThat(((Map) writtenItems.get(4)).get("foo")).isEqualTo("26");
			assertThat(((Map) writtenItems.get(4)).get("bar")).isEqualTo("28");
			assertThat(((Map) writtenItems.get(4)).get("baz")).isEqualTo("thirty");
			assertThat(((Map) writtenItems.get(5)).get("foo")).isEqualTo("32");
			assertThat(((Map) writtenItems.get(5)).get("bar")).isEqualTo("34");
			assertThat(((Map) writtenItems.get(5)).get("baz")).isEqualTo("thirty six");
		});
	}

	@Test
	public void testCustomLineMapper() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(CustomLineMapperConfiguration.class)
				.withConfiguration(
						AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
								BatchAutoConfiguration.class,
								SingleStepJobAutoConfiguration.class,
								FlatFileItemReaderAutoConfiguration.class))
				.withPropertyValues("spring.batch.job.jobName=job",
						"spring.batch.job.stepName=step1", "spring.batch.job.chunkSize=5",
						"spring.batch.job.flatfilereader.name=fixedWidthConfiguration",
						"spring.batch.job.flatfilereader.resource=/test.txt",
						"spring.batch.job.flatfilereader.strict=true");

		applicationContextRunner.run((context) -> {
			JobLauncher jobLauncher = context.getBean(JobLauncher.class);

			Job job = context.getBean(Job.class);

			ListItemWriter itemWriter = context.getBean(ListItemWriter.class);

			JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

			JobExplorer jobExplorer = context.getBean(JobExplorer.class);

			while (jobExplorer.getJobExecution(jobExecution.getJobId()).isRunning()) {
				Thread.sleep(1000);
			}

			List writtenItems = itemWriter.getWrittenItems();

			assertThat(writtenItems.size()).isEqualTo(8);
		});
	}

	/**
	 * This test requires an input file with an even number of records
	 */
	@Test
	public void testCustomRecordSeparatorAndSkippedLines() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(
						RecordSeparatorAndSkippedLinesJobConfiguration.class)
				.withConfiguration(AutoConfigurations.of(
						PropertyPlaceholderAutoConfiguration.class,
						BatchAutoConfiguration.class,
						SingleStepJobAutoConfiguration.class,
						FlatFileItemReaderAutoConfiguration.class, RangeConverter.class))
				.withPropertyValues("spring.batch.job.jobName=job",
						"spring.batch.job.stepName=step1", "spring.batch.job.chunkSize=5",
						"spring.batch.job.flatfilereader.name=fixedWidthConfiguration",
						"spring.batch.job.flatfilereader.resource=/test.txt",
						"spring.batch.job.flatfilereader.linesToSkip=2",
						"spring.batch.job.flatfilereader.fixedLength=true",
						"spring.batch.job.flatfilereader.ranges=3-4,7-8,11",
						"spring.batch.job.flatfilereader.names=foo,bar,baz",
						"spring.batch.job.flatfilereader.strict=true");

		applicationContextRunner.run((context) -> {
			JobLauncher jobLauncher = context.getBean(JobLauncher.class);

			Job job = context.getBean(Job.class);

			ListItemWriter itemWriter = context.getBean(ListItemWriter.class);

			JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

			JobExplorer jobExplorer = context.getBean(JobExplorer.class);

			while (jobExplorer.getJobExecution(jobExecution.getJobId()).isRunning()) {
				Thread.sleep(1000);
			}

			ListLineCallbackHandler callbackHandler = context
					.getBean(ListLineCallbackHandler.class);

			assertThat(callbackHandler.getLines().size()).isEqualTo(2);

			List writtenItems = itemWriter.getWrittenItems();

			assertThat(writtenItems.size()).isEqualTo(2);
		});
	}

	@Test
	public void testCustomMapping() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(CustomMappingConfiguration.class)
				.withConfiguration(AutoConfigurations.of(
						PropertyPlaceholderAutoConfiguration.class,
						BatchAutoConfiguration.class,
						SingleStepJobAutoConfiguration.class,
						FlatFileItemReaderAutoConfiguration.class, RangeConverter.class))
				.withPropertyValues("spring.batch.job.jobName=job",
						"spring.batch.job.stepName=step1", "spring.batch.job.chunkSize=5",
						"spring.batch.job.flatfilereader.name=fixedWidthConfiguration",
						"spring.batch.job.flatfilereader.resource=/test.txt",
						"spring.batch.job.flatfilereader.maxItemCount=1",
						"spring.batch.job.flatfilereader.strict=true");

		applicationContextRunner.run((context) -> {
			JobLauncher jobLauncher = context.getBean(JobLauncher.class);

			Job job = context.getBean(Job.class);

			ListItemWriter itemWriter = context.getBean(ListItemWriter.class);

			JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

			JobExplorer jobExplorer = context.getBean(JobExplorer.class);

			while (jobExplorer.getJobExecution(jobExecution.getJobId()).isRunning()) {
				Thread.sleep(1000);
			}

			List<Map<Object, Object>> writtenItems = itemWriter.getWrittenItems();

			assertThat(writtenItems.size()).isEqualTo(1);
			assertThat(writtenItems.get(0).get("one")).isEqualTo("1 2 3");
			assertThat(writtenItems.get(0).get("two")).isEqualTo("4 5 six");
		});
	}

	@EnableBatchProcessing
	@Configuration
	public static class CustomMappingConfiguration {

		@Autowired
		private JobBuilderFactory jobBuilderFactory;

		@Autowired
		private StepBuilderFactory stepBuilderFactory;

		@Autowired
		private FlatFileItemReader itemReader;

		@Bean
		public ListItemWriter<Map> itemWriter() {
			return new ListItemWriter<>();
		}

		@Bean
		public LineTokenizer lineTokenizer() {
			return line -> new DefaultFieldSet(
					new String[] { line.substring(0, 5), line.substring(6) },
					new String[] { "one", "two" });
		}

		@Bean
		public FieldSetMapper<Map<Object, Object>> fieldSetMapper() {
			return fieldSet -> fieldSet.getProperties();
		}

	}

	@EnableBatchProcessing
	@Configuration
	public static class JobConfiguration {

		@Autowired
		private JobBuilderFactory jobBuilderFactory;

		@Autowired
		private StepBuilderFactory stepBuilderFactory;

		@Autowired
		private FlatFileItemReader itemReader;

		@Bean
		public ListItemWriter<Map> itemWriter() {
			return new ListItemWriter<>();
		}

	}

	@EnableBatchProcessing
	@Configuration
	public static class RecordSeparatorAndSkippedLinesJobConfiguration {

		@Autowired
		private JobBuilderFactory jobBuilderFactory;

		@Autowired
		private StepBuilderFactory stepBuilderFactory;

		@Autowired
		private FlatFileItemReader itemReader;

		@Bean
		public RecordSeparatorPolicy recordSeparatorPolicy() {
			return new RecordSeparatorPolicy() {
				@Override
				public boolean isEndOfRecord(String record) {

					boolean endOfRecord = false;

					int index = record.indexOf('\n');

					if (index > 0 && record.length() > index + 1) {
						endOfRecord = true;
					}

					return endOfRecord;
				}

				@Override
				public String postProcess(String record) {
					return record;
				}

				@Override
				public String preProcess(String record) {
					return record + '\n';
				}
			};
		}

		@Bean
		public LineCallbackHandler lineCallbackHandler() {
			return new ListLineCallbackHandler();
		}

		@Bean
		public ListItemWriter<Map> itemWriter() {
			return new ListItemWriter<>();
		}

	}

	@EnableBatchProcessing
	@Configuration
	public static class CustomLineMapperConfiguration {

		@Autowired
		private JobBuilderFactory jobBuilderFactory;

		@Autowired
		private StepBuilderFactory stepBuilderFactory;

		@Autowired
		private FlatFileItemReader itemReader;

		@Bean
		public LineMapper<Map<Object, Object>> lineMapper() {
			return (line, lineNumber) -> {
				Map<Object, Object> item = new HashMap<>(1);

				item.put("line", line);
				item.put("lineNumber", lineNumber);

				return item;
			};
		}

		@Bean
		public ListItemWriter<Map> itemWriter() {
			return new ListItemWriter<>();
		}

	}

	public static class ListLineCallbackHandler implements LineCallbackHandler {

		private List<String> lines = new ArrayList<>();

		@Override
		public void handleLine(String line) {
			lines.add(line);
		}

		public List<String> getLines() {
			return lines;
		}

	}

}
