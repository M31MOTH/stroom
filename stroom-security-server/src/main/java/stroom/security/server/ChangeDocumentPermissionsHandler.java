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

import stroom.entity.shared.DocRef;
import stroom.entity.shared.EntityServiceException;
import stroom.entity.shared.Folder;
import stroom.explorer.server.ExplorerService;
import stroom.security.Insecure;
import stroom.security.SecurityContext;
import stroom.security.shared.ChangeDocumentPermissionsAction;
import stroom.security.shared.ChangeSet;
import stroom.security.shared.DocumentPermissionNames;
import stroom.security.shared.DocumentPermissions;
import stroom.security.shared.UserPermission;
import stroom.security.shared.UserRef;
import stroom.security.shared.UserService;
import stroom.task.server.AbstractTaskHandler;
import stroom.task.server.TaskHandlerBean;
import stroom.util.logging.StroomLogger;
import stroom.util.shared.VoidResult;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@TaskHandlerBean(task = ChangeDocumentPermissionsAction.class)
@Insecure
public class ChangeDocumentPermissionsHandler
        extends AbstractTaskHandler<ChangeDocumentPermissionsAction, VoidResult> {
    private static final StroomLogger LOGGER = StroomLogger.getLogger(ChangeDocumentPermissionsHandler.class);

    private final DocumentPermissionService documentPermissionService;
    private final DocumentPermissionsCache documentPermissionsCache;
    private final UserService userService;
    private final UserPermissionsCache userPermissionsCache;
    private final SecurityContext securityContext;
    private final ExplorerService explorerService;

    @Inject
    public ChangeDocumentPermissionsHandler(final DocumentPermissionService documentPermissionService, final DocumentPermissionsCache documentPermissionsCache, final UserService userService, final UserPermissionsCache userPermissionsCache, final SecurityContext securityContext, final ExplorerService explorerService) {
        this.documentPermissionService = documentPermissionService;
        this.documentPermissionsCache = documentPermissionsCache;
        this.userService = userService;
        this.userPermissionsCache = userPermissionsCache;
        this.securityContext = securityContext;
        this.explorerService = explorerService;
    }

    @Override
    public VoidResult exec(final ChangeDocumentPermissionsAction action) {
        final DocRef docRef = action.getDocRef();

        // Check that the current user has permission to change the permissions of the document.
        if (securityContext.hasDocumentPermission(docRef.getType(), docRef.getUuid(), DocumentPermissionNames.OWNER)) {
            // Record what documents and what users are affected by these changes so we can clear the relevant caches.
            final Set<DocRef> affectedDocRefs = new HashSet<>();
            final Set<UserRef> affectedUserRefs = new HashSet<>();

            // Change the permissions of the document.
            final ChangeSet<UserPermission> changeSet = action.getChangeSet();
            changeDocPermissions(docRef, changeSet, affectedDocRefs, affectedUserRefs, false);

            // Cascade changes if this is a folder and we have been asked to do so.
            if (action.getCascade() != null) {
                cascadeChanges(docRef, changeSet, affectedDocRefs, affectedUserRefs, action.getCascade());
            }

            // Find out which actual users are affected by changes to user groups.
            final Set<UserRef> affectedUsers = new HashSet<>();
            for (final UserRef affectedUserRef : affectedUserRefs) {
                if (affectedUserRef.isGroup()) {
                    final List<UserRef> users = userService.findUsersInGroup(affectedUserRef);
                    affectedUsers.addAll(users);
                } else {
                    affectedUsers.add(affectedUserRef);
                }
            }

            // Force refresh of cached permissions.
            affectedDocRefs.forEach(documentPermissionsCache::remove);
            affectedUsers.forEach(userPermissionsCache::remove);

            return VoidResult.INSTANCE;
        }

        throw new EntityServiceException("You do not have sufficient privileges to change permissions for this document");
    }

    private void changeDocPermissions(final DocRef docRef, final ChangeSet<UserPermission> changeSet, final Set<DocRef> affectedDocRefs, final Set<UserRef> affectedUserRefs, final boolean clear) {
        if (clear) {
            // If we are asked to clear all permissions then get them for this document and then remove them.
            final DocumentPermissions documentPermissions = documentPermissionService.getPermissionsForDocument(docRef);
            for (final Map.Entry<UserRef, Set<String>> entry : documentPermissions.getUserPermissions().entrySet()) {
                final UserRef userRef = entry.getKey();
                for (final String permission : entry.getValue()) {
                    try {
                        documentPermissionService.removePermission(userRef, docRef, permission);
                        // Remember the affected documents and users so we can clear the relevant caches.
                        affectedDocRefs.add(docRef);
                        affectedUserRefs.add(userRef);
                    } catch (final RuntimeException e) {
                        // Expected.
                        LOGGER.debug(e.getMessage());
                    }
                }
            }

        } else {
            // Otherwise remove permissions specified by the change set.
            for (final UserPermission userPermission : changeSet.getRemoveSet()) {
                final UserRef userRef = userPermission.getUserRef();
                try {
                    documentPermissionService.removePermission(userRef, docRef, userPermission.getPermission());
                    // Remember the affected documents and users so we can clear the relevant caches.
                    affectedDocRefs.add(docRef);
                    affectedUserRefs.add(userRef);
                } catch (final RuntimeException e) {
                    // Expected.
                    LOGGER.debug(e.getMessage());
                }
            }
        }

        // Add permissions from the change set.
        for (final UserPermission userPermission : changeSet.getAddSet()) {
            // Don't add create permissions to items that aren't folders as it makes no sense.
            if (Folder.ENTITY_TYPE.equals(docRef.getType()) || !userPermission.getPermission().startsWith(DocumentPermissionNames.CREATE)) {
                final UserRef userRef = userPermission.getUserRef();
                try {
                    documentPermissionService.addPermission(userRef, docRef, userPermission.getPermission());
                    // Remember the affected documents and users so we can clear the relevant caches.
                    affectedDocRefs.add(docRef);
                    affectedUserRefs.add(userRef);
                } catch (final RuntimeException e) {
                    // Expected.
                    LOGGER.debug(e.getMessage());
                }
            }
        }
    }

    private void cascadeChanges(final DocRef docRef, final ChangeSet<UserPermission> changeSet, final Set<DocRef> affectedDocRefs, final Set<UserRef> affectedUserRefs, final ChangeDocumentPermissionsAction.Cascade cascade) {
        if ("System".equals(docRef.getType()) || "Folder".equals(docRef.getType())) {
            switch (cascade) {
                case CHANGES_ONLY:
                    // We are only cascading changes so just pass on the change set.
                    changeDescendantPermissions(docRef, changeSet, affectedDocRefs, affectedUserRefs, false);
                    break;

                case ALL:
                    // We are replicating the permissions of the parent folder on all children so create a change set from the parent folder.
                    final DocumentPermissions parentPermissions = documentPermissionService.getPermissionsForDocument(docRef);
                    final ChangeSet<UserPermission> fullChangeSet = new ChangeSet<>();
                    for (final Map.Entry<UserRef, Set<String>> entry : parentPermissions.getUserPermissions().entrySet()) {
                        final UserRef userRef = entry.getKey();
                        for (final String permission : entry.getValue()) {
                            fullChangeSet.add(new UserPermission(userRef, permission));
                        }
                    }

                    // Set child permissions to that of the parent folder after clearing all permissions from child documents.
                    changeDescendantPermissions(docRef, fullChangeSet, affectedDocRefs, affectedUserRefs, true);

                    break;

                case NO:
                    // Do nothing.
                    break;
            }
        }
    }

    private void changeDescendantPermissions(final DocRef folder, final ChangeSet<UserPermission> changeSet, final Set<DocRef> affectedDocRefs, final Set<UserRef> affectedUserRefs, final boolean clear) {
        final List<DocRef> descendants = explorerService.getDescendants(folder);
        if (descendants != null && descendants.size() > 0) {
            for (final DocRef descendant : descendants) {
                // Ensure that the user has permission to change the permissions of this child.
                if (securityContext.hasDocumentPermission(descendant.getType(), descendant.getUuid(), DocumentPermissionNames.OWNER)) {
                    changeDocPermissions(descendant, changeSet, affectedDocRefs, affectedUserRefs, clear);
                } else {
                    LOGGER.debug("User does not have permission to change permissions on " + descendant.toString());
                }
            }
        }
    }
}
