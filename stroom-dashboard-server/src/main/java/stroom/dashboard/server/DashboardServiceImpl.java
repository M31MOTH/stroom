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

import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stroom.dashboard.shared.Dashboard;
import stroom.dashboard.shared.DashboardService;
import stroom.dashboard.shared.FindDashboardCriteria;
import stroom.dashboard.shared.Query;
import stroom.entity.server.AutoMarshal;
import stroom.entity.server.DocumentEntityServiceImpl;
import stroom.entity.server.util.SQLBuilder;
import stroom.entity.server.util.StroomEntityManager;
import stroom.entity.shared.DocRef;
import stroom.entity.shared.DocumentType;
import stroom.logging.EntityEventLog;
import stroom.security.SecurityContext;
import stroom.util.io.StreamUtil;
import stroom.util.logging.StroomLogger;

import javax.inject.Inject;
import java.util.Arrays;

@Component
@Transactional
@AutoMarshal
public class DashboardServiceImpl extends DocumentEntityServiceImpl<Dashboard, FindDashboardCriteria>
        implements DashboardService {
    private static final StroomLogger LOGGER = StroomLogger.getLogger(DashboardServiceImpl.class);

    private static final String[] PERMISSIONS = Arrays.copyOf(STANDARD_PERMISSIONS, STANDARD_PERMISSIONS.length + 1);

    static {
        PERMISSIONS[PERMISSIONS.length - 1] = "Download";
    }

    private final ResourceLoader resourceLoader;
    private String xmlTemplate;

    @Inject
    DashboardServiceImpl(final StroomEntityManager entityManager, final SecurityContext securityContext, final EntityEventLog entityEventLog, final ResourceLoader resourceLoader) {
        super(entityManager, securityContext, entityEventLog);
        this.resourceLoader = resourceLoader;
    }

    @Override
    public DocumentType getDocumentType() {
        return getDocumentType(7, "Dashboard", "Dashboard");
    }

    @Override
    public Class<Dashboard> getEntityClass() {
        return Dashboard.class;
    }

    @Override
    public FindDashboardCriteria createCriteria() {
        return new FindDashboardCriteria();
    }

    @Override
    public Dashboard create(final DocRef folder, final String name) {
        final Dashboard dashboard = super.create(folder, name);
        if (dashboard.getData() == null) {
            dashboard.setData(getTemplate());
        }
        return super.internalSave(dashboard);
    }

    @Override
    public Dashboard save(Dashboard entity) throws RuntimeException {
        if (entity.getData() == null) {
            entity.setData(getTemplate());
        }
        return super.save(entity);
    }

    @Override
    public Boolean delete(final Dashboard entity) throws RuntimeException {
        checkDeletePermission(DocRef.create(entity));

        // Delete associated queries first.
        final SQLBuilder sql = new SQLBuilder();
        sql.append("DELETE FROM ");
        sql.append(Query.TABLE_NAME);
        sql.append(" WHERE ");
        sql.append(Dashboard.FOREIGN_KEY);
        sql.append(" = ");
        sql.arg(entity.getId());
        getEntityManager().executeNativeUpdate(sql);

        return super.delete(entity);
    }

    private String getTemplate() {
        if (xmlTemplate == null) {
            try {
                final org.springframework.core.io.Resource resource = resourceLoader
                        .getResource("classpath:/stroom/dashboard/DashboardTemplate.data.xml");
                xmlTemplate = StreamUtil.streamToString(resource.getInputStream());
            } catch (final Exception e) {
                LOGGER.error(e.getMessage(), e);
            }

            // Don't try and load this template again if it fails.
            if (xmlTemplate == null) {
                xmlTemplate = "";
            }
        }

        return xmlTemplate;
    }

    @Override
    public String[] getPermissions() {
        return PERMISSIONS;
    }
}
