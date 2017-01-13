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

import com.google.gwt.user.client.ui.Composite;
import stroom.explorer.shared.ExplorerNode;

import java.util.List;

public abstract class AbstractExporerTree extends Composite {
    /**
     * Called by the model after refresh to select the initial item.
     * @param selection
     */
    abstract void setInitialSelectedItem(ExplorerNode selection);

    /**
     * Called by the model on refresh to set data.
     * @param rows
     */
    abstract void setData(List<ExplorerNode> rows);
}
