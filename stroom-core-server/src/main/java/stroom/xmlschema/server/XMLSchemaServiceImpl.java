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

package stroom.xmlschema.server;

import stroom.entity.server.DocumentEntityServiceImpl;
import stroom.entity.server.QueryAppender;
import stroom.entity.server.util.StroomEntityManager;
import stroom.entity.server.util.SQLBuilder;
import stroom.entity.server.util.SQLUtil;
import stroom.entity.shared.DocumentType;
import stroom.logging.EntityEventLog;
import stroom.security.SecurityContext;
import stroom.xmlschema.shared.FindXMLSchemaCriteria;
import stroom.xmlschema.shared.XMLSchema;
import stroom.xmlschema.shared.XMLSchemaService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;

@Component
@Transactional
public class XMLSchemaServiceImpl extends DocumentEntityServiceImpl<XMLSchema, FindXMLSchemaCriteria>
        implements XMLSchemaService {
    @Inject
    XMLSchemaServiceImpl(final StroomEntityManager entityManager, final SecurityContext securityContext, final EntityEventLog entityEventLog) {
        super(entityManager, securityContext, entityEventLog);
    }

    @Override
    public DocumentType getDocumentType() {
        return getDocumentType(13, "XMLSchema", "XML Schema");
    }

    @Override
    public Class<XMLSchema> getEntityClass() {
        return XMLSchema.class;
    }

    @Override
    public FindXMLSchemaCriteria createCriteria() {
        return new FindXMLSchemaCriteria();
    }

    @Override
    protected QueryAppender<XMLSchema, FindXMLSchemaCriteria> createQueryAppender(final StroomEntityManager entityManager) {
        return new XMLSchemaQueryAppender(entityManager);
    }

    private static class XMLSchemaQueryAppender extends QueryAppender<XMLSchema, FindXMLSchemaCriteria> {
        public XMLSchemaQueryAppender(final StroomEntityManager entityManager) {
            super(entityManager);
        }

        @Override
        protected void appendBasicCriteria(final SQLBuilder sql, final String alias, final FindXMLSchemaCriteria criteria) {
            super.appendBasicCriteria(sql, alias, criteria);

            SQLUtil.appendValueQuery(sql, alias + ".namespaceURI", criteria.getNamespaceURI());
            SQLUtil.appendValueQuery(sql, alias + ".systemId", criteria.getSystemId());
            SQLUtil.appendValueQuery(sql, alias + ".schemaGroup", criteria.getSchemaGroup());
        }
    }
}
