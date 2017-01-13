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

import stroom.cell.dropdowntree.client.DropDownCell;
import stroom.data.client.event.DataSelectionEvent;
import stroom.data.client.event.DataSelectionEvent.DataSelectionHandler;
import stroom.entity.shared.DocRef;
import stroom.explorer.shared.ExplorerNode;
import com.google.inject.Inject;

public class EntityDropDownCell extends DropDownCell<DocRef> {
    private final ExplorerDropDownTreePresenter explorerDropDownTreePresenter;

    private String unselectedText;

    @Inject
    public EntityDropDownCell(final ExplorerDropDownTreePresenter explorerDropDownTreePresenter) {
        this.explorerDropDownTreePresenter = explorerDropDownTreePresenter;
        setUnselectedText("None");

        explorerDropDownTreePresenter.addDataSelectionHandler(new DataSelectionHandler<ExplorerNode>() {
            @Override
            public void onSelection(final DataSelectionEvent<ExplorerNode> event) {
                changeSelection(event.getSelectedItem());
            }
        });
    }

    public void setUnselectedText(final String unselectedText) {
        this.unselectedText = unselectedText;
        explorerDropDownTreePresenter.setUnselectedText(unselectedText);
        changeSelection(null);
    }

    @Override
    protected String getUnselectedText() {
        return unselectedText;
    }

    public void setIncludedTypes(final String... includedTypes) {
        explorerDropDownTreePresenter.setIncludedTypes(includedTypes);
    }

    public void setTags(final String... tags) {
        explorerDropDownTreePresenter.setTags(tags);
    }

    public void setRequiredPermissions(final String... requiredPermissions) {
        explorerDropDownTreePresenter.setRequiredPermissions(requiredPermissions);
    }

    public void setAllowFolderSelection(final boolean allowFolderSelection) {
        explorerDropDownTreePresenter.setAllowFolderSelection(allowFolderSelection);
    }

    @Override
    public void showPopup(final DocRef value) {
        explorerDropDownTreePresenter.setSelectedEntityReference(value);
        explorerDropDownTreePresenter.show();
    }

    private void changeSelection(final ExplorerNode selection) {
        if (selection == null) {
            setValue(null);
        } else {
            setValue(selection.getDocRef());
        }
    }
}
