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

package stroom.entity.shared;

import stroom.util.shared.SharedObject;

public class DocumentType implements SharedObject {
    private static final long serialVersionUID = -7826692935161793565L;

    public static final String DOC_IMAGE_URL = "document/";

    private int priority;
    private String type;
    private String displayType;
    private String iconUrl;

    public DocumentType() {
        // Default constructor necessary for GWT serialisation.
    }

    public DocumentType(final int priority, final String type, final String displayType, final String iconUrl) {
        this.priority = priority;
        this.type = type;
        this.displayType = displayType;
        this.iconUrl = iconUrl;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(final int priority) {
        this.priority = priority;
    }

    public String getDisplayType() {
        return displayType;
    }

    public void setDisplayType(final String displayType) {
        this.displayType = displayType;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(final String iconUrl) {
        this.iconUrl = iconUrl;
    }
}
