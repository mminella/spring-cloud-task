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

import org.springframework.cloud.task.repository.LoggerTaskRepository;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.stereotype.Component;

/**
 * If no TaskConfigurer is present this configuration will be used.
 * @author Glenn Renfro
 */
@Component
public class DefaultTaskConfigurer implements TaskConfigurer{

	private DataSource dataSource;

	public DefaultTaskConfigurer(){
	}

	public DefaultTaskConfigurer(DataSource dataSource){
		this.dataSource = dataSource;
	}

	public TaskRepository getTaskRepository() {
		return new LoggerTaskRepository();
	}

}
