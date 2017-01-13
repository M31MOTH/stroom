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

package stroom.explorer.client.presenter;

import stroom.data.client.event.DataSelectionEvent;
import stroom.data.client.event.DataSelectionEvent.DataSelectionHandler;
import stroom.data.client.event.HasDataSelectionHandlers;
import stroom.dispatch.client.ClientDispatchAsync;
import stroom.explorer.shared.ExplorerNode;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import com.gwtplatform.mvp.client.View;

import java.util.Set;

public class EntityCheckTreePresenter extends MyPresenterWidget<EntityCheckTreePresenter.EntityCheckTreeView>
        implements HasDataSelectionHandlers<Set<ExplorerNode>> {
    public interface EntityCheckTreeView extends View {
        void setCellTree(Widget cellTree);
    }

//    private final TickBoxSelectionModel<ExplorerNode> selectionModel;
    private final ExplorerTickBoxTree explorerTree;

    @Inject
    public EntityCheckTreePresenter(final EntityCheckTreeView view, final ClientDispatchAsync dispatcher) {
        super(new SimpleEventBus(), view);

//        selectionModel = new TickBoxSelectionModel<ExplorerNode>() {
//            @Override
//            public ExplorerNode getParent(final ExplorerNode object) {
//                if (object instanceof EntityCheckTreeData) {
//                    return ((EntityCheckTreeData) object).getParent();
//                }
//
//                return null;
//            }
//
//            @Override
//            public List<ExplorerNode> getChildren(final ExplorerNode object) {
//                if (object instanceof EntityCheckTreeData) {
//                    return ((EntityCheckTreeData) object).getChildren();
//                }
//
//                return null;
//            }
//        };
//
//        final TickBoxTreeCell<ExplorerNode> cell = new TickBoxTreeCell<ExplorerNode>(selectionModel) {
//            @Override
//            protected Image getIcon(final ExplorerNode item) {
//                return new Image(ImageUtil.getImageURL() + item.getIconUrl());
//            }
//        };

//        treeModel = new ExplorerTreeModel(selectionModel, new TickBoxSelectionManager<ExplorerNode>(), cell,
//                dispatcher);
//        final MyCellTree cellTree = new MyCellTree(treeModel);
//        cellTree.addOpenHandler(treeModel);
//        cellTree.addCloseHandler(treeModel);
//        treeModel.setRootTreeNode(cellTree.getRootTreeNode());
//
//        final UpdateHandler<ExplorerNode> updateHandler = new UpdateHandler<ExplorerNode>() {
//            @Override
//            public List<ExplorerNode> onUpdate(final ExplorerNode parent, final List<ExplorerNode> result) {
//                List<ExplorerNode> children = result;
//                if (result != null && selectionModel.isAffectRelatives()) {
//                    children = new ArrayList<ExplorerNode>();
//                    for (final ExplorerNode child : result) {
//                        // Select if the parent is selected. If is is then set
//                        // this item to be selected.
//                        if (TickBoxState.TICK.equals(selectionModel.getState(parent))) {
//                            selectionModel.setState(child, TickBoxState.TICK, true);
//
//                        } else {
//                            final TickBoxState childTickBoxState = selectionModel.getState(child);
//                            if (TickBoxState.TICK.equals(childTickBoxState)) {
//                                selectionModel.modifyState(child, TickBoxState.UNTICK);
//                                selectionModel.setState(child, TickBoxState.TICK, true);
//                            }
//                        }
//
//                        // Convert children to tree nodes so we can add parents
//                        // and children.
//                        final EntityCheckTreeData entityCheckTreeData = new EntityCheckTreeData(child);
//                        entityCheckTreeData.setParent(parent);
//                        if (parent != null && parent instanceof EntityCheckTreeData) {
//                            ((EntityCheckTreeData) parent).addChild(entityCheckTreeData);
//                        }
//                        children.add(entityCheckTreeData);
//                    }
//                }
//
//                return children;
//            }
//        };
//        treeModel.setUpdateHandler(updateHandler);
//
//        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
//            @Override
//            public void onSelectionChange(final SelectionChangeEvent event) {
//                if (!selectionModel.isIgnorable()) {
//                    DataSelectionEvent.fire(EntityCheckTreePresenter.this, selectionModel.getSelectedSet(), false);
//                }
//            }
//        });

        explorerTree = new ExplorerTickBoxTree(dispatcher);

        view.setCellTree(explorerTree);
    }

    public void setIncludedTypes(final String... includedTypes) {
        explorerTree.getTreeModel().setIncludedTypes(includedTypes);
    }

    public void setTags(final String... tags) {
        explorerTree.getTreeModel().setTags(tags);
    }

    public void setRequiredPermissions(final String... requiredPermissions) {
        explorerTree.getTreeModel().setRequiredPermissions(requiredPermissions);
    }

//    public void setRemoveOrphans(final boolean removeOrphans) {
//        treeModel.setRemoveOrphans(removeOrphans);
//    }
//
//    public void refresh(final Set<ExplorerNode> openItems, final Integer depth) {
//        treeModel.refresh(openItems, depth);
//    }

    public void refresh() {
        explorerTree.getTreeModel().refresh();
    }

    public TickBoxSelectionModel getSelectionModel() {
        return explorerTree.getSelectionModel();
    }

    @Override
    public HandlerRegistration addDataSelectionHandler(final DataSelectionHandler<Set<ExplorerNode>> handler) {
        return addHandlerToSource(DataSelectionEvent.getType(), handler);
    }
}
