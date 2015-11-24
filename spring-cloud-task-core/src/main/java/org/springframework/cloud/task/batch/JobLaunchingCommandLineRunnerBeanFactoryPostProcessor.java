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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.batch.JobLauncherCommandLineRunner;

/**
 * Creates a task for each job found in the context.
 * TODO: Allow which jobs get wrapped to be configurable (required for nested jobs)
 *
 * @author Michael Minella
 */
public class JobLaunchingCommandLineRunnerBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor {

	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		String[] bootCommandLineRunnerNames = ((DefaultListableBeanFactory) registry).getBeanNamesForType(JobLauncherCommandLineRunner.class);

		registry.removeBeanDefinition(bootCommandLineRunnerNames[0]);

		String[] jobBeanNames = ((DefaultListableBeanFactory) registry).getBeanNamesForType(Job.class);

		String[] jobLauncherBeanNames = ((DefaultListableBeanFactory) registry).getBeanNamesForType(JobLauncher.class);

		if(jobLauncherBeanNames.length != 1) {
			throw new RuntimeException("Context must contain exactly one JobLauncher");
		}

		BeanDefinition jobLauncherBeanDefinition = registry.getBeanDefinition(jobLauncherBeanNames[0]);

		for (String jobBeanName : jobBeanNames) {
			GenericBeanDefinition beanDefinition = new GenericBeanDefinition();

			beanDefinition.setBeanClass(JobLaunchingTask.class);
			ConstructorArgumentValues constructorArgumentValues = beanDefinition.getConstructorArgumentValues();
			constructorArgumentValues.addGenericArgumentValue(jobLauncherBeanDefinition);
			constructorArgumentValues.addGenericArgumentValue(registry.getBeanDefinition(jobBeanName));

			registry.registerBeanDefinition(jobBeanName + "Task", beanDefinition);
		}
	}

	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
	}
}
