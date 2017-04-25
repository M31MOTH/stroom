/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.dashboard.shared;

import stroom.util.shared.SharedObject;

public abstract class ComponentResultRequest implements SharedObject {
    private static final long serialVersionUID = -7455554742243923562L;
    private boolean wantsData;

    public abstract ComponentType getComponentType();

    public boolean wantsData() {
        return wantsData;
    }

    public void setWantsData(final boolean wantsData) {
        this.wantsData = wantsData;
    }

    public enum ComponentType {
        TABLE, VIS
    }
}