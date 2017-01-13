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

package stroom.entity.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;
import com.gwtplatform.mvp.client.PresenterWidget;
import stroom.explorer.shared.ExplorerNode;

public class CopyEntityEvent extends GwtEvent<CopyEntityEvent.Handler> {
    private static Type<Handler> TYPE;
    private final PresenterWidget<?> presenter;
    private final ExplorerNode document;
    private final ExplorerNode folder;
    private final String name;

    private CopyEntityEvent(final PresenterWidget<?> presenter, final ExplorerNode document,
            final ExplorerNode folder, final String name) {
        this.presenter = presenter;
        this.document = document;
        this.folder = folder;
        this.name = name;
    }

    public static void fire(final HasHandlers handlers, final PresenterWidget<?> presenter,
                            final ExplorerNode document, final ExplorerNode folder, final String name) {
        handlers.fireEvent(new CopyEntityEvent(presenter, document, folder, name));
    }

    public static Type<Handler> getType() {
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    @Override
    public final Type<Handler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final Handler handler) {
        handler.onCopy(this);
    }

    public PresenterWidget<?> getPresenter() {
        return presenter;
    }

    public ExplorerNode getDocument() {
        return document;
    }

    public ExplorerNode getFolder() {
        return folder;
    }

    public String getName() {
        return name;
    }

    public interface Handler extends EventHandler {
        void onCopy(final CopyEntityEvent event);
    }
}
