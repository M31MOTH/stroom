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

package stroom.db.migration.mysql;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import stroom.entity.shared.DocRef;
import stroom.util.logging.StroomLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class V6_0_0_1__Explorer implements JdbcMigration {
    private static final StroomLogger LOGGER = StroomLogger.getLogger(V6_0_0_1__Explorer.class);

    private Map<Long, List<Long>> folderIdToAncestorIDMap = new HashMap<>();

    @Override
    public void migrate(final Connection connection) throws Exception {
        // Create explorer tree data from existing child/parent node relationships.
        createExplorerTree(connection);
    }

    private void createExplorerTree(final Connection connection) throws Exception {
        // Create a map of document references for all folders.
        final Map<Long, Set<DocRef>> docRefMap = createDocRefMap(connection, "FOLDER", "Folder");

        // Insert System root node.
        final DocRef root = new DocRef("System", "00000000", "System");

        Long nodeId = getExplorerTreeNodeId(connection, root);
        List<Long> ancestorIdList;

        if (nodeId == null) {
            createExplorerTreeNode(connection, root);
            nodeId = getExplorerTreeNodeId(connection, root);
            ancestorIdList = Collections.singletonList(nodeId);

            // Insert paths.
            insertPaths(connection, nodeId, ancestorIdList);
        } else {
            ancestorIdList = Collections.singletonList(nodeId);
        }

        // Store the mapping of folder id to explorer node ancestors.
        folderIdToAncestorIDMap.put(0L, ancestorIdList);

        // Add child nodes
        addFolderNodes(connection, 0L, ancestorIdList, docRefMap);


        // Migrate other document types.
        addOtherNodes(connection, "STAT_DAT_SRC", "StatisticStore");
        addOtherNodes(connection, "IDX", "Index");
        addOtherNodes(connection, "FD", "Feed");
        addOtherNodes(connection, "XML_SCHEMA", "XMLSchema");
        addOtherNodes(connection, "VIS", "Visualisation");
        addOtherNodes(connection, "TXT_CONV", "TextConverter");
        addOtherNodes(connection, "SCRIPT", "Script");
        addOtherNodes(connection, "PIPE", "Pipeline");
        addOtherNodes(connection, "DASH", "Dashboard");
        addOtherNodes(connection, "DICT", "Dictionary");
        addOtherNodes(connection, "XSLT", "XSLT");
    }

    private void addOtherNodes(final Connection connection, final String tableName, final String type) throws SQLException {
        final Map<Long, Set<DocRef>> feedMap = createDocRefMap(connection, tableName, type);
        for (final Map.Entry<Long, Set<DocRef>> entry : feedMap.entrySet()) {
            final long folderId = entry.getKey();
            final Set<DocRef> docRefs = entry.getValue();
            final List<Long> parentAncestorIdList = folderIdToAncestorIDMap.get(folderId);

            for (final DocRef docRef : docRefs) {
                Long nodeId = getExplorerTreeNodeId(connection, docRef);

                if (nodeId == null) {
                    createExplorerTreeNode(connection, docRef);
                    nodeId = getExplorerTreeNodeId(connection, docRef);

                    final List<Long> ancestorIdList = new ArrayList<>(parentAncestorIdList);
                    ancestorIdList.add(0, nodeId);

                    // Insert paths.
                    insertPaths(connection, nodeId, ancestorIdList);
                }
            }
        }
    }

    private void addFolderNodes(final Connection connection, final Long parentId, final List<Long> parentAncestorIdList, final Map<Long, Set<DocRef>> docRefMap) throws SQLException {
        final Set<DocRef> childNodes = docRefMap.get(parentId);

        if (childNodes != null && childNodes.size() > 0) {
            // Add nodes
            for (final DocRef docRef : childNodes) {
                final List<Long> ancestorIdList = new ArrayList<>(parentAncestorIdList);

                Long nodeId = getExplorerTreeNodeId(connection, docRef);
                if (nodeId == null) {
                    createExplorerTreeNode(connection, docRef);
                    nodeId = getExplorerTreeNodeId(connection, docRef);
                    ancestorIdList.add(0, nodeId);

                    // Insert paths.
                    insertPaths(connection, nodeId, ancestorIdList);

                } else {
                    ancestorIdList.add(0, nodeId);
                }

                // Store the mapping of folder id to explorer node ancestors.
                folderIdToAncestorIDMap.put(docRef.getId(), ancestorIdList);

                // Recurse to insert child nodes.
                addFolderNodes(connection, docRef.getId(), ancestorIdList, docRefMap);
            }
        }
    }

    private Map<Long, Set<DocRef>> createDocRefMap(final Connection connection, final String tableName, final String type) throws SQLException {
        final Map<Long, Set<DocRef>> docRefMap = new HashMap<>();

        try (final Statement statement = connection.createStatement()) {
            try (final ResultSet resultSet = statement.executeQuery("SELECT ID, UUID, NAME, FK_FOLDER_ID FROM " + tableName)) {
                while (resultSet.next()) {
                    final long id = resultSet.getLong(1);
                    final String uuid = resultSet.getString(2);
                    final String name = resultSet.getString(3);
                    Long parentId = resultSet.getLong(4);

                    final DocRef docRef = new DocRef(type, uuid, name);
                    docRef.setId(id);

                    if (parentId == null) {
                        parentId = 0L;
                    }

                    docRefMap.computeIfAbsent(parentId, k -> new HashSet<>()).add(docRef);
                }
            }
        }

        return docRefMap;
    }

    private void createExplorerTreeNode(final Connection connection, final DocRef docRef) throws SQLException {
        // Insert node entry.
        try (final PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO EXPLORER_TREE_NODE (TYPE, UUID, NAME) VALUES (?, ?, ?)")) {
            preparedStatement.setString(1, docRef.getType());
            preparedStatement.setString(2, docRef.getUuid());
            preparedStatement.setString(3, docRef.getName());
            preparedStatement.executeUpdate();
        }
    }

    private Long getExplorerTreeNodeId(final Connection connection, final DocRef docRef) throws SQLException {
        Long nodeId = null;

        // Fetch id for newly inserted node entry.
        try (final PreparedStatement preparedStatement = connection.prepareStatement("SELECT ID FROM EXPLORER_TREE_NODE WHERE TYPE = ? AND UUID = ?;")) {
            preparedStatement.setString(1, docRef.getType());
            preparedStatement.setString(2, docRef.getUuid());

            try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    nodeId = resultSet.getLong(1);
                }
            }
        }

        return nodeId;
    }

    private void insertPaths(final Connection connection, final Long id, final List<Long> ancestorIdList) throws SQLException {
        // Insert ancestor references.
        for (int i = 0; i < ancestorIdList.size(); i++) {
            final Long ancestorId = ancestorIdList.get(i);
            insertReference(connection, ancestorId, id, (long) i);
        }
    }

    private void insertReference(final Connection connection, final Long ancestor, final Long descendant, final Long depth) throws SQLException {
        // Insert self reference.
        try (final PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO EXPLORER_TREE_PATH (ANCESTOR, DESCENDANT, DEPTH, ORDER_INDEX) VALUES (?, ?, ?, ?)")) {
            preparedStatement.setLong(1, ancestor);
            preparedStatement.setLong(2, descendant);
            preparedStatement.setLong(3, depth);
            preparedStatement.setLong(4, -1);
            preparedStatement.executeUpdate();
        }
    }
}
