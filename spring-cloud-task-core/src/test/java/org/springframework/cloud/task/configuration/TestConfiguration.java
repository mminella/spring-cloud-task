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
package org.springframework.cloud.task.configuration;

import javax.sql.DataSource;

import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.dao.JdbcTaskExecutionDao;
import org.springframework.cloud.task.repository.dao.MapTaskExecutionDao;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskDatabaseInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Michael Minella
 */

@Configuration
public class TestConfiguration {

	@Autowired(required = false)
	private DataSource dataSource;

	@Autowired(required = false)
	private ResourceLoader resourceLoader;

	@Bean
	public TaskRepository taskRepository(TaskExecutionDao taskExecutionDao){
		return new SimpleTaskRepository(taskExecutionDao);
	}

	@Bean
	public PlatformTransactionManager transactionManager() {
		if(dataSource == null) {
			return new ResourcelessTransactionManager();
		}
		else {
			TaskDatabaseInitializer.initializeDatabase(dataSource, this.resourceLoader);

			return new DataSourceTransactionManager(dataSource);
		}
	}

	@Bean
	public TaskExplorer taskExplorer(TaskExecutionDao taskExecutionDao) {
		return new SimpleTaskExplorer(taskExecutionDao);
	}

	@Bean
	public TaskExecutionDao taskExecutionDao() {
		if(dataSource != null) {
			return new JdbcTaskExecutionDao(dataSource);
		}
		else {
			return new MapTaskExecutionDao();
		}
	}
}
