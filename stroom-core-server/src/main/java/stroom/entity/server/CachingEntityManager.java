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

package stroom.entity.server;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import stroom.entity.server.util.SQLBuilder;
import stroom.entity.server.util.StroomEntityManager;
import stroom.entity.shared.BaseCriteria;
import stroom.entity.shared.BaseResultList;
import stroom.entity.shared.Clearable;
import stroom.entity.shared.Entity;
import stroom.entity.shared.SummaryDataRow;

import javax.inject.Inject;
import javax.persistence.FlushModeType;
import java.util.Arrays;
import java.util.List;

@Component
public class CachingEntityManager implements StroomEntityManager, InitializingBean, Clearable {
    private final StroomEntityManager stroomEntityManager;
    private final CacheManager cacheManager;

    private Ehcache cache;

    @Inject
    public CachingEntityManager(final StroomEntityManager stroomEntityManager, final CacheManager cacheManager) {
        this.stroomEntityManager = stroomEntityManager;
        this.cacheManager = cacheManager;
    }

    @Override
    public void flush() {
        stroomEntityManager.flush();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Entity> T loadEntity(final Class<?> clazz, final T entity) {
        T result;
        if (entity != null && entity.isPersistent() && entity.getPrimaryKey() != null) {
            final List<Object> key = Arrays.asList("loadEntity", clazz, entity.getPrimaryKey());

            // Try and get a cached method result from the cache.
            final Element element = cache.get(key);
            if (element == null) {
                // We didn't find a cached result so get one and put it in the
                // cache.
                result = stroomEntityManager.loadEntity(clazz, entity);
                cache.put(new Element(key, result));
            } else {
                result = (T) element.getObjectValue();
            }

        } else {
            result = stroomEntityManager.loadEntity(clazz, entity);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Entity> T loadEntityById(final Class<?> clazz, final long id) {
        T result;
        final List<Object> key = Arrays.asList("loadEntityById", clazz, id);

        // Try and get a cached method result from the cache.
        final Element element = cache.get(key);
        if (element == null) {
            // We didn't find a cached result so get one and put it in the
            // cache.
            result = stroomEntityManager.loadEntityById(clazz, id);
            cache.put(new Element(key, result));
        } else {
            result = (T) element.getObjectValue();
        }

        return result;
    }

    @Override
    public <T extends Entity> T saveEntity(final T entity) {
        return stroomEntityManager.saveEntity(entity);
    }

    @Override
    public <T extends Entity> Boolean deleteEntity(final T entity) {
        return stroomEntityManager.deleteEntity(entity);
    }

    @Override
    public Long executeNativeUpdate(final SQLBuilder sql) {
        return stroomEntityManager.executeNativeUpdate(sql);
    }

    @Override
    public long executeNativeQueryLongResult(final SQLBuilder sql) {
        return stroomEntityManager.executeNativeQueryLongResult(sql);
    }

    @Override
    public BaseResultList<SummaryDataRow> executeNativeQuerySummaryDataResult(final SQLBuilder sql, final int numberKeys) {
        return stroomEntityManager.executeNativeQuerySummaryDataResult(sql, numberKeys);
    }

    @Override
    public List executeNativeQueryResultList(final SQLBuilder sql) {
        return stroomEntityManager.executeNativeQueryResultList(sql);
    }

    @Override
    public <T> List<T> executeNativeQueryResultList(final SQLBuilder sql, final Class<?> clazz) {
        return stroomEntityManager.executeNativeQueryResultList(sql, clazz);
    }

    @Override
    public List executeQueryResultList(final SQLBuilder sql) {
        List result;
        final List<Object> key = Arrays.asList("executeQueryResultList", sql.toString(), sql.getArgs());

        // Try and get a cached method result from the cache.
        final Element element = cache.get(key);
        if (element == null) {
            // We didn't find a cached result so get one and put it in the
            // cache.
            result = stroomEntityManager.executeQueryResultList(sql);
            cache.put(new Element(key, result));
        } else {
            result = (List) element.getObjectValue();
        }

        return result;
    }

    @Override
    public List executeQueryResultList(final SQLBuilder sql, final BaseCriteria criteria) {
        List result;
        final List<Object> key = Arrays.asList("executeQueryResultList", sql.toString(), sql.getArgs(), criteria);

        // Try and get a cached method result from the cache.
        final Element element = cache.get(key);
        if (element == null) {
            // We didn't find a cached result so get one and put it in the
            // cache.
            result = stroomEntityManager.executeQueryResultList(sql, criteria);
            cache.put(new Element(key, result));
        } else {
            result = (List) element.getObjectValue();
        }

        return result;
    }

    @Override
    public long executeQueryLongResult(final SQLBuilder sql) {
        long result;
        final List<Object> key = Arrays.asList("executeQueryLongResult", sql.toString(), sql.getArgs());

        // Try and get a cached method result from the cache.
        final Element element = cache.get(key);
        if (element == null) {
            // We didn't find a cached result so get one and put it in the
            // cache.
            result = stroomEntityManager.executeQueryLongResult(sql);
            cache.put(new Element(key, result));
        } else {
            result = (long) element.getObjectValue();
        }

        return result;
    }

    @Override
    public String runSubSelectQuery(final SQLBuilder sql, final boolean handleNull) {
        return stroomEntityManager.runSubSelectQuery(sql, handleNull);
    }

    @Override
    public boolean hasNativeColumn(final String nativeTable, final String nativeColumn) {
        return stroomEntityManager.hasNativeColumn(nativeTable, nativeColumn);
    }

    @Override
    public void shutdown() {
        stroomEntityManager.shutdown();
    }

    @Override
    public void setFlushMode(final FlushModeType mode) {
        stroomEntityManager.setFlushMode(mode);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        cache = cacheManager.getEhcache("serviceCache");
    }

    @Override
    public void clear() {
        cache.removeAll();
    }
}