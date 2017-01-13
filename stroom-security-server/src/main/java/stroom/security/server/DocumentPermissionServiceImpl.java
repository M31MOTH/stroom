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

package stroom.security.server;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stroom.entity.server.util.SQLBuilder;
import stroom.entity.server.util.StroomEntityManager;
import stroom.entity.shared.DocRef;
import stroom.entity.shared.DocumentService;
import stroom.entity.shared.DocumentServiceLocator;
import stroom.security.shared.DocumentPermissionKey;
import stroom.security.shared.DocumentPermissionKeySet;
import stroom.security.shared.DocumentPermissions;
import stroom.security.shared.User;
import stroom.security.shared.UserRef;
import stroom.util.logging.StroomLogger;

import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Transactional
@Component
public class DocumentPermissionServiceImpl implements DocumentPermissionService {
    private static final StroomLogger LOGGER = StroomLogger.getLogger(DocumentPermissionServiceImpl.class);

    private static final String SQL_INSERT_USER_PERMISSIONS;
    private static final String SQL_DELETE_USER_PERMISSIONS;
    private static final String SQL_GET_PERMISSION_FOR_DOCUMENT;
    private static final String SQL_GET_PERMISSION_KEYSET_FOR_USER;

    static {
        final StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ");
        sql.append(DocumentPermission.TABLE_NAME);
        sql.append(" (");
        sql.append(DocumentPermission.VERSION);
        sql.append(" ,");
        sql.append(DocumentPermission.USER_UUID);
        sql.append(" ,");
        sql.append(DocumentPermission.DOC_TYPE);
        sql.append(" ,");
        sql.append(DocumentPermission.DOC_UUID);
        sql.append(" ,");
        sql.append(DocumentPermission.PERMISSION);
        sql.append(")");
        sql.append(" VALUES (?,?,?,?,?)");
        SQL_INSERT_USER_PERMISSIONS = sql.toString();
    }

    static {
        final StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ");
        sql.append(DocumentPermission.TABLE_NAME);
        sql.append(" WHERE ");
        sql.append(DocumentPermission.USER_UUID);
        sql.append(" = ?");
        sql.append(" AND ");
        sql.append(DocumentPermission.DOC_TYPE);
        sql.append(" = ?");
        sql.append(" AND ");
        sql.append(DocumentPermission.DOC_UUID);
        sql.append(" = ?");
        sql.append(" AND ");
        sql.append(DocumentPermission.PERMISSION);
        sql.append(" = ?");
        SQL_DELETE_USER_PERMISSIONS = sql.toString();
    }

    static {
        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT");
        sql.append(" user.");
        sql.append(User.UUID);
        sql.append(", user.");
        sql.append(User.NAME);
        sql.append(", user.");
        sql.append(User.GROUP);
        sql.append(", doc.");
        sql.append(DocumentPermission.PERMISSION);

        sql.append(" FROM ");
        sql.append(DocumentPermission.TABLE_NAME);
        sql.append(" AS ");
        sql.append("doc");

        sql.append(" JOIN ");
        sql.append(User.TABLE_NAME);
        sql.append(" AS ");
        sql.append("user");
        sql.append(" ON (");
        sql.append("user." + User.UUID + " = doc." + DocumentPermission.USER_UUID);
        sql.append(")");

        sql.append(" WHERE ");
        sql.append(DocumentPermission.DOC_TYPE);
        sql.append(" = ?");
        sql.append(" AND ");
        sql.append(DocumentPermission.DOC_UUID);
        sql.append(" = ?");
        SQL_GET_PERMISSION_FOR_DOCUMENT = sql.toString();
    }

    static {
        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("doc.");
        sql.append(DocumentPermission.DOC_TYPE);
        sql.append(", doc.");
        sql.append(DocumentPermission.DOC_UUID);
        sql.append(", doc.");
        sql.append(DocumentPermission.PERMISSION);

        sql.append(" FROM ");
        sql.append(DocumentPermission.TABLE_NAME);
        sql.append(" AS ");
        sql.append("doc");

        sql.append(" LEFT OUTER JOIN ");
        sql.append(UserGroupUser.TABLE_NAME);
        sql.append(" AS ");
        sql.append("userGroupUser");
        sql.append(" ON (");
        sql.append("userGroupUser." + UserGroupUser.GROUP_UUID + " = doc." + DocumentPermission.USER_UUID);
        sql.append(")");

        sql.append(" WHERE");
        sql.append(" doc.");
        sql.append(DocumentPermission.USER_UUID);
        sql.append(" = ?");
        sql.append(" OR userGroupUser.");
        sql.append(UserGroupUser.USER_UUID);
        sql.append(" = ?");

        sql.append(" GROUP BY");
        sql.append(" doc.");
        sql.append(DocumentPermission.DOC_TYPE);
        sql.append(", ");
        sql.append(" doc.");
        sql.append(DocumentPermission.DOC_UUID);
        sql.append(", ");
        sql.append(" doc.");
        sql.append(DocumentPermission.PERMISSION);
        SQL_GET_PERMISSION_KEYSET_FOR_USER = sql.toString();
    }

    protected final StroomEntityManager entityManager;
    private final DocumentServiceLocator documentServiceLocator;

    @Inject
    DocumentPermissionServiceImpl(final StroomEntityManager entityManager,
                                  final DocumentServiceLocator documentServiceLocator) {
        this.entityManager = entityManager;
        this.documentServiceLocator = documentServiceLocator;
    }

    @Override
    public DocumentPermissions getPermissionsForDocument(final DocRef document) {
        final Map<UserRef, Set<String>> userPermissions = new HashMap<>();

        try {
            final SQLBuilder sqlBuilder = new SQLBuilder(SQL_GET_PERMISSION_FOR_DOCUMENT, document.getType(), document.getUuid());
            final List list = entityManager.executeNativeQueryResultList(sqlBuilder);
            list.stream().forEach(o -> {
                final Object[] arr = (Object[]) o;
                final String uuid = (String) arr[0];
                final String name = (String) arr[1];
                final boolean group = (Boolean) arr[2];
                final String permission = (String) arr[3];

                final UserRef userRef = new UserRef(User.ENTITY_TYPE, uuid, name, group);

                Set<String> permissions = userPermissions.get(userRef);
                if (permissions == null) {
                    permissions = new HashSet<>();
                    userPermissions.put(userRef, permissions);
                }
                permissions.add(permission);
            });

        } catch (final RuntimeException e) {
            LOGGER.error("getPermissionsForDocument()", e);
            throw e;
        }

        final DocumentService documentService = documentServiceLocator
                .locate(document.getType());
        final String[] permissions = documentService.getPermissions();
        return new DocumentPermissions(document, permissions, userPermissions);
    }

    @Override
    public DocumentPermissionKeySet getPermissionKeySetForUser(final UserRef userRef) {
        final DocumentPermissionKeySet permissions = new DocumentPermissionKeySet();

        try {
            final SQLBuilder sqlBuilder = new SQLBuilder(SQL_GET_PERMISSION_KEYSET_FOR_USER, userRef.getUuid(), userRef.getUuid());
            final List list = entityManager.executeNativeQueryResultList(sqlBuilder);
            list.stream().forEach(o -> {
                final Object[] arr = (Object[]) o;
                final String docType = (String) arr[0];
                final String docUuid = (String) arr[1];
                final String permission = (String) arr[2];

                final DocumentPermissionKey documentPermissionKey = new DocumentPermissionKey(docType, docUuid,
                        permission);
                permissions.addPermission(documentPermissionKey);
            });

        } catch (final RuntimeException e) {
            LOGGER.error("getPermissionKeySetForUser()", e);
            throw e;
        }

        return permissions;
    }

    @Override
    public void addPermission(final UserRef userRef, final DocRef document, final String permission) {
        try {
            final SQLBuilder sqlBuilder = new SQLBuilder(SQL_INSERT_USER_PERMISSIONS, 1, userRef.getUuid(), document.getType(), document.getUuid(), permission);
            entityManager.executeNativeUpdate(sqlBuilder);
        } catch (final PersistenceException e) {
            // Expected exception.
            LOGGER.debug("addPermission()", e);
            throw e;
        } catch (final RuntimeException e) {
            LOGGER.error("addPermission()", e);
            throw e;
        }
    }

    @Override
    public void removePermission(final UserRef userRef, final DocRef document, final String permission) {
        try {
            final SQLBuilder sqlBuilder = new SQLBuilder(SQL_DELETE_USER_PERMISSIONS, userRef.getUuid(), document.getType(), document.getUuid(), permission);
            entityManager.executeNativeUpdate(sqlBuilder);
        } catch (final RuntimeException e) {
            LOGGER.error("removePermission()", e);
            throw e;
        }
    }
}
