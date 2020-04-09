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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.item.support.ListItemWriter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Michael Minella
 */
public class SingleStepJobAutoConfigurationTests {

	@Test
	public void testInvalidProperties() {
		SingleStepJobProperties properties = new SingleStepJobProperties();

		try {
			new SingleStepJobAutoConfiguration(null, null, properties, null);
		}
		catch (IllegalArgumentException iae) {
			assertThat(iae.getMessage()).isEqualTo("A job name is required");
		}
		catch (Throwable t) {
			fail("wrong exception was thrown", t);
		}

		properties.setJobName("job");

		try {
			new SingleStepJobAutoConfiguration(null, null, properties, null);
		}
		catch (IllegalArgumentException iae) {
			assertThat(iae.getMessage()).isEqualTo("A step name is required");
		}
		catch (Throwable t) {
			fail("wrong exception was thrown", t);
		}

		properties.setStepName("step");

		try {
			new SingleStepJobAutoConfiguration(null, null, properties, null);
		}
		catch (IllegalArgumentException iae) {
			assertThat(iae.getMessage()).isEqualTo("A chunk size is required");
		}
		catch (Throwable t) {
			fail("wrong exception was thrown", t);
		}

		properties.setChunkSize(-5);

		try {
			new SingleStepJobAutoConfiguration(null, null, properties, null);
		}
		catch (IllegalArgumentException iae) {
			assertThat(iae.getMessage())
					.isEqualTo("A chunk size greater than zero is required");
		}
		catch (Throwable t) {
			fail("wrong exception was thrown", t);
		}

		properties.setChunkSize(5);
	}

	@Test
	public void testSimpleConfiguration() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(SimpleConfiguration.class)
				.withConfiguration(
						AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
								BatchAutoConfiguration.class,
								SingleStepJobAutoConfiguration.class))
				.withPropertyValues("spring.batch.job.jobName=job",
						"spring.batch.job.stepName=step1",
						"spring.batch.job.chunkSize=5");

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

			assertThat(writtenItems.size()).isEqualTo(3);

			assertThat(writtenItems.get(0).get("item")).isEqualTo("foo");
			assertThat(writtenItems.get(1).get("item")).isEqualTo("bar");
			assertThat(writtenItems.get(2).get("item")).isEqualTo("baz");
		});
	}

	@EnableBatchProcessing
	@Configuration
	public static class SimpleConfiguration {

		@Bean
		public ListItemReader<Map<Object, Object>> itemReader() {
			List<Map<Object, Object>> items = new ArrayList<>(3);

			items.add(Collections.singletonMap("item", "foo"));
			items.add(Collections.singletonMap("item", "bar"));
			items.add(Collections.singletonMap("item", "baz"));

			return new ListItemReader<>(items);
		}

		@Bean
		public ListItemWriter<Map<Object, Object>> itemWriter() {
			return new ListItemWriter<>();
		}

	}

}
