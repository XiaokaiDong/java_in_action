/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.geektimes.projects.user.mybatis.annotation;

import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

/**
 * TODO Comment
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since TODO
 * Date : 2021-05-06
 */
public class MyBatisBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private Environment environment;

    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder beanDefinitionBuilder = genericBeanDefinition(SqlSessionFactoryBean.class);

        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(EnableMyBatis.class.getName());
        /**
         *  <bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
         *   <property name="dataSource" ref="dataSource" />
         *   <property name="mapperLocations" value="classpath*:" />
         *  </bean >
         */
        // Spring String 类型可以自动转化 Spring Resource
        beanDefinitionBuilder.addPropertyValue("configLocation", attributes.get("configLocation"));
        beanDefinitionBuilder.addPropertyValue("mapperLocations", attributes.get("mapperLocations"));
        beanDefinitionBuilder.addPropertyReference("dataSource", (String) attributes.get("dataSource"));

        // 自行添加其他属性
        beanDefinitionBuilder.addPropertyReference("transactionFactory", (String) attributes.get("transactionFactory"));

        //下面的代码利用BeanWrapperImpl -> TypeConverterSupport
        //                            -> TypeConverterDelegate -> ConversionService实现从String到Properties的转换
        beanDefinitionBuilder.addPropertyValue("configurationProperties", (String) attributes.get("configurationProperties"));

        beanDefinitionBuilder.addPropertyReference("sqlSessionFactory", (String) attributes.get("sqlSessionFactory"));

        beanDefinitionBuilder.addPropertyValue("environment", resolvePlaceholder(attributes.get("environment")));

        beanDefinitionBuilder.addPropertyValue("failFast", attributes.get("failFast"));

        beanDefinitionBuilder.addPropertyValue("plugins", attributes.get("plugins"));

        beanDefinitionBuilder.addPropertyValue("typeHandlers", attributes.get("typeHandlers"));

        beanDefinitionBuilder.addPropertyValue("typeHandlersPackage", attributes.get("typeHandlersPackage"));

        //attributes返回Object， Class is Object，所以可以这么传递
        beanDefinitionBuilder.addPropertyValue("defaultEnumTypeHandler", attributes.get("defaultEnumTypeHandler"));
        beanDefinitionBuilder.addPropertyValue("typeAliases", attributes.get("typeAliases"));

        beanDefinitionBuilder.addPropertyValue("typeAliasesPackage", attributes.get("typeAliasesPackage"));

        //attributes返回Object， Class is Object，所以可以这么传递
        beanDefinitionBuilder.addPropertyValue("typeAliasesSuperType", attributes.get("typeAliasesSuperType"));

        beanDefinitionBuilder.addPropertyReference("scriptingLanguageDrivers", (String) attributes.get("scriptingLanguageDrivers"));

        beanDefinitionBuilder.addPropertyValue("defaultScriptingLanguageDriver", attributes.get("defaultScriptingLanguageDriver"));

        beanDefinitionBuilder.addPropertyReference("databaseIdProvider", (String) attributes.get("databaseIdProvider"));

        beanDefinitionBuilder.addPropertyValue("vfs", attributes.get("vfs"));

        beanDefinitionBuilder.addPropertyValue("cache", attributes.get("cache"));

        beanDefinitionBuilder.addPropertyReference("objectFactory", (String) attributes.get("objectFactory"));

        beanDefinitionBuilder.addPropertyReference("objectWrapperFactory", (String) attributes.get("objectWrapperFactory"));

        // SqlSessionFactoryBean 的 BeanDefinition
        BeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();

        String beanName = (String) attributes.get("value");
        registry.registerBeanDefinition(beanName, beanDefinition);
    }

    private Object resolvePlaceholder(Object value) {
        if (value instanceof String) {
            return environment.resolvePlaceholders((String) value);
        }
        return value;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
