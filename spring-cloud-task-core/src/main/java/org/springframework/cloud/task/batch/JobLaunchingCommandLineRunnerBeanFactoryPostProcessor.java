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
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.batch.JobLauncherCommandLineRunner;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Creates a task for each job found in the context.
 * TODO: Allow which jobs get wrapped to be configurable (required for nested jobs)
 *
 * @author Michael Minella
 */
public class JobLaunchingCommandLineRunnerBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor {

	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		String[] bootCommandLineRunnerNames = ((GenericApplicationContext) registry).getBeanNamesForType(JobLauncherCommandLineRunner.class);

		System.out.println(">>> THERE ARE " + bootCommandLineRunnerNames.length + " BOOT COMMAND LINE RUNNERS!!!!!");

		String[] jobBeanNames = ((GenericApplicationContext) registry).getBeanNamesForType(Job.class);

		String[] jobLauncherBeanNames = ((GenericApplicationContext) registry).getBeanNamesForType(JobLauncher.class);

		if(jobBeanNames.length != 1) {
			throw new RuntimeException("Context must contain exactly one JobLauncher");
		}

		BeanDefinition jobLauncherBeanDefinition = registry.getBeanDefinition(jobLauncherBeanNames[0]);

		for (String jobBeanName : jobBeanNames) {
			GenericBeanDefinition beanDefinition = new GenericBeanDefinition();

			beanDefinition.setBeanClass(JobLaunchingTask.class);
			MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
			propertyValues.add("job", registry.getBeanDefinition(jobBeanName));
			propertyValues.add("jobLauncher", jobLauncherBeanDefinition);
		}
	}

	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
	}
}
