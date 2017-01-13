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

import org.springframework.stereotype.Component;
import stroom.entity.server.event.EntityEventBus;
import stroom.entity.shared.DocumentServiceLocator;
import stroom.entity.shared.DocumentType;
import stroom.explorer.shared.ExplorerNode;
import stroom.security.Insecure;
import stroom.util.task.TaskScopeRunnable;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Component
class ExplorerTreeModel {
    private static final long TEN_MINUTES = 1000 * 60 * 10;

    private final ExplorerTreeDao explorerTreeDao;
    private final DocumentServiceLocator documentServiceLocator;
    private final EntityEventBus eventBus;
    private final ReentrantLock treeBuildLock = new ReentrantLock();

    private volatile TreeModel treeModel;
    private volatile long lastBuildTime;
    private volatile boolean rebuildRequired;
    private volatile Map<String, DocumentType> typeMap;

    @Inject
    ExplorerTreeModel(final ExplorerTreeDao explorerTreeDao, final DocumentServiceLocator documentServiceLocator, final EntityEventBus eventBus) {
        this.explorerTreeDao = explorerTreeDao;
        this.documentServiceLocator = documentServiceLocator;
        this.eventBus = eventBus;
    }

    @Insecure
    public TreeModel getModel() {
        // If the tree is more than 10 minutes old then rebuild it.
        if (!rebuildRequired && lastBuildTime < System.currentTimeMillis() - TEN_MINUTES) {
            rebuildRequired = true;
            treeModel = null;
        }

        TreeModel model = treeModel;
        if (model == null || rebuildRequired) {
            // Try and get the map under lock.
            treeBuildLock.lock();
            try {
                model = treeModel;
                while (model == null || rebuildRequired) {
                    // Record the last time we built the full tree.
                    lastBuildTime = System.currentTimeMillis();
                    rebuildRequired = false;
                    model = createModel();
                }

                // Record the last time we built the full tree.
                lastBuildTime = System.currentTimeMillis();
                treeModel = model;
            } finally {
                treeBuildLock.unlock();
            }
        }

        return model;
    }

    private TreeModel createModel() {
        final TreeModel treeModel = new TreeModelImpl();
        final TaskScopeRunnable runnable = new TaskScopeRunnable(null) {
            @Override
            protected void exec() {
                final List<ExplorerTreeNode> roots = explorerTreeDao.getRoots();
                addChildren(treeModel, sort(roots), null);
            }
        };

        runnable.run();
        return treeModel;
    }

    private void addChildren(final TreeModel treeModel, final List<ExplorerTreeNode> children, final ExplorerNode parentNode) {
        for (final ExplorerTreeNode child : children) {
            final ExplorerNode explorerNode = new ExplorerNode(child.getId(), child.getType(), child.getUuid(), child.getName(), child.getTags());
            explorerNode.setIconUrl(getIconUrl(child.getType()));
            treeModel.add(parentNode, explorerNode);

            final List<ExplorerTreeNode> subChildren = explorerTreeDao.getChildren(child);
            if (subChildren != null && subChildren.size() > 0) {
                addChildren(treeModel, sort(subChildren), explorerNode);
            }
        }
    }

    private List<ExplorerTreeNode> sort(final List<ExplorerTreeNode> list) {
        list.sort((o1, o2) -> {
            if (!o1.getType().equals(o2.getType())) {
                final int p1 = getPriority(o1.getType());
                final int p2 = getPriority(o2.getType());
                return Integer.compare(p1, p2);
            }

            return o1.getName().compareTo(o2.getName());
        });
        return list;
    }

    private String getIconUrl(final String type) {
        final DocumentType documentType = getType(type);
        if (documentType == null) {
            return null;
        }

        return documentType.getIconUrl();
    }

    private int getPriority(final String type) {
        final DocumentType documentType = getType(type);
        if (documentType == null) {
            return Integer.MAX_VALUE;
        }

        return documentType.getPriority();
    }

    private DocumentType getType(final String type) {
        if (typeMap == null) {
            final Map<String, DocumentType> map = new HashMap<>();
            for (final DocumentType documentType : documentServiceLocator.getTypes()) {
                map.put(documentType.getType(), documentType);
            }
            this.typeMap = map;
        }
        return this.typeMap.get(type);
    }
}
