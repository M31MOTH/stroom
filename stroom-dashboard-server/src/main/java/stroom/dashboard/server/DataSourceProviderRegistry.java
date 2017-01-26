/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.dashboard.server;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import stroom.datasource.api.DataSource;
import stroom.query.api.DocRef;
import stroom.util.spring.StroomBeanStore;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Component
public class DataSourceProviderRegistry implements InitializingBean {
    private final Map<String, DataSourceProvider> providers = new HashMap<>();

    private final StroomBeanStore stroomBeanStore;

    @Inject
    public DataSourceProviderRegistry(final StroomBeanStore stroomBeanStore) {
        this.stroomBeanStore = stroomBeanStore;
    }

    public DataSourceProvider getDataSourceProvider(final DocRef dataSourceRef) {
        if (dataSourceRef != null && dataSourceRef.getType() != null) {
            final DataSourceProvider provider = providers.get(dataSourceRef.getType());
            return provider;
        }
        return null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (final String beanName : stroomBeanStore.getStroomBeanByType(DataSourceProvider.class)) {
            final Object bean = stroomBeanStore.getBean(beanName);
            final DataSourceProvider dataSourceProvider = (DataSourceProvider) bean;
            providers.put(dataSourceProvider.getType(), dataSourceProvider);
        }
    }
}
