/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.task.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.task.annotation.Task;

/**
 * @author Michael Minella
 */
@Task
public class JobLaunchingTask implements CommandLineRunner {

	private Job job;

	private JobLauncher jobLauncher;

	public JobLaunchingTask(JobLauncher jobLauncher, Job job) {
		this.job = job;
		this.jobLauncher = jobLauncher;
	}

	public void run(String... args) throws JobExecutionException {
		jobLauncher.run(job, new JobParameters());
	}
}
