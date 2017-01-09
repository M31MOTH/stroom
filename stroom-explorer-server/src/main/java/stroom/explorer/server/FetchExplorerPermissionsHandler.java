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

package stroom.explorer.server;

import stroom.entity.shared.DocRef;
import stroom.entity.shared.Folder;
import stroom.explorer.shared.DocumentType;
import stroom.explorer.shared.DocumentTypes;
import stroom.explorer.shared.EntityData;
import stroom.explorer.shared.ExplorerData;
import stroom.explorer.shared.ExplorerPermissions;
import stroom.explorer.shared.FetchExplorerPermissionsAction;
import stroom.security.SecurityContext;
import stroom.security.shared.DocumentPermissionNames;
import stroom.task.server.AbstractTaskHandler;
import stroom.task.server.TaskHandlerBean;
import stroom.util.shared.SharedMap;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@TaskHandlerBean(task = FetchExplorerPermissionsAction.class)
class FetchExplorerPermissionsHandler
        extends AbstractTaskHandler<FetchExplorerPermissionsAction, SharedMap<ExplorerData, ExplorerPermissions>> {
    private static final DocRef ROOT = new DocRef("System", "00000000", "System");

    private final ExplorerService explorerService;
    private final SecurityContext securityContext;

    @Inject
    FetchExplorerPermissionsHandler(final ExplorerService explorerService, final SecurityContext securityContext) {
        this.explorerService = explorerService;
        this.securityContext = securityContext;
    }

    @Override
    public SharedMap<ExplorerData, ExplorerPermissions> exec(final FetchExplorerPermissionsAction action) {
        final List<ExplorerData> explorerDataList = action.getExplorerDataList();
        final Map<ExplorerData, ExplorerPermissions> resultMap = new HashMap<>();

        for (final ExplorerData explorerData : explorerDataList) {
            final Set<DocumentType> createPermissions = new HashSet<>();
            final Set<String> documentPermissions = new HashSet<>();
            DocRef docRef = null;

            if (explorerData instanceof EntityData) {
                final EntityData entityData = (EntityData) explorerData;
                docRef = entityData.getDocRef();
            }

            if (docRef != null) {
                for (final String permissionName : DocumentPermissionNames.DOCUMENT_PERMISSIONS) {
                    if (securityContext.hasDocumentPermission(docRef.getType(), docRef.getUuid(),
                            permissionName)) {
                        documentPermissions.add(permissionName);
                    }
                }
            }

            // If no entity reference has been passed then assume root folder.
            if (docRef == null) {
                docRef = ROOT;
            }

            // Add special permissions for folders to control creation of sub items.
            if (Folder.ENTITY_TYPE.equals(docRef.getType())) {
                final DocumentTypes documentTypes = explorerService.getAllDocumentTypes();
                for (final DocumentType documentType : documentTypes.getAllTypes()) {
                    final String permissionName = DocumentPermissionNames.getDocumentCreatePermission(documentType.getType());
                    if (securityContext.hasDocumentPermission(docRef.getType(), docRef.getUuid(),
                            permissionName)) {
                        createPermissions.add(documentType);
                    }
                }
            }

            resultMap.put(explorerData, new ExplorerPermissions(createPermissions, documentPermissions, securityContext.isAdmin()));
        }

        return new SharedMap<>(resultMap);
    }
}
