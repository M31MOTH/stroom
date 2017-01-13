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

package stroom.explorer.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;
import stroom.explorer.client.presenter.MultiSelectionModel;
import stroom.explorer.shared.ExplorerNode;

public class ExplorerTreeSelectEvent extends GwtEvent<ExplorerTreeSelectEvent.Handler> {
    private static Type<Handler> TYPE;
    private final MultiSelectionModel<ExplorerNode> selectionModel;
    private final SelectionType selectionType;

    private ExplorerTreeSelectEvent(final MultiSelectionModel<ExplorerNode> selectionModel, final SelectionType selectionType) {
        this.selectionModel = selectionModel;
        this.selectionType = selectionType;
    }

    public static void fire(final HasHandlers source, final MultiSelectionModel<ExplorerNode> selectionModel, final SelectionType selectionType) {
        source.fireEvent(new ExplorerTreeSelectEvent(selectionModel, selectionType));
    }

    public static Type<Handler> getType() {
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    @Override
    public Type<Handler> getAssociatedType() {
        return getType();
    }

    @Override
    protected void dispatch(final Handler handler) {
        handler.onSelect(this);
    }

    public MultiSelectionModel<ExplorerNode> getSelectionModel() {
        return selectionModel;
    }

    public SelectionType getSelectionType() {
        return selectionType;
    }

    public interface Handler extends EventHandler {
        void onSelect(ExplorerTreeSelectEvent event);
    }
}
