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

package stroom.explorer.shared;

import stroom.entity.shared.DocRef;
import stroom.entity.shared.HasUuid;
import stroom.util.shared.HasDisplayValue;
import stroom.util.shared.HasNodeState;
import stroom.util.shared.HasType;
import stroom.util.shared.SharedObject;

public class ExplorerNode implements HasType, HasUuid, HasNodeState, HasDisplayValue, SharedObject {
    private static final long serialVersionUID = -5216736591679930246L;

    private Long id;
    private String type;
    private String uuid;
    private String name;
    private String tags;

    private int depth;
    private String iconUrl;
    private NodeState nodeState;


//    private DocRef docRef;
//    private NodeState nodeState;
//    private Set<String> tags;

    public ExplorerNode() {
        // Default constructor necessary for GWT serialisation.
    }

    public ExplorerNode(final Long id, final String type, String uuid, final String name, final String tags) {
        this.id = id;
        this.type = type;
        this.uuid = uuid;
        this.name = name;
        this.tags = tags;
    }

//    public static ExplorerNode create(final String iconUrl, final DocRef docRef, final NodeState nodeState) {
//        if (docRef == null) {
//            return null;
//        }
//        return new ExplorerNode(iconUrl, docRef, nodeState);
//    }
//
//    public static ExplorerNode create(final long id, final String type, String uuid, final String name, final String tags) {
//        if (docRef == null) {
//            return null;
//        }
//        return create(iconUrl, docRef, NodeState.LEAF);
//    }
//
//    public static ExplorerNode create(final DocRef docRef) {
//        if (docRef == null) {
//            return null;
//        }
//        return create(null, docRef, NodeState.LEAF);
//    }

    public Long getId() {
        return id;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getTags() {
        return tags;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(final int depth) {
        this.depth = depth;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(final String iconUrl) {
        this.iconUrl = iconUrl;
    }

//    public DocRef getDocRef() {
//        return docRef;
//    }
//

    public DocRef getDocRef() {
        return new DocRef(type, uuid, name);
    }

    @Override
    public NodeState getNodeState() {
        return nodeState;
    }

    public void setNodeState(final NodeState nodeState) {
        this.nodeState = nodeState;
    }

    @Override
    public String getDisplayValue() {
        return name;
    }

//    @Override
//    public Set<String> getTags() {
//        return tags;
//    }
//
//    public void setTags(final Set<String> tags) {
//        this.tags = tags;
//    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(obj instanceof ExplorerNode)) {
            return false;
        }

        final ExplorerNode explorerNode = (ExplorerNode) obj;
//        EqualsBuilder builder = new EqualsBuilder();

        if (id == null || explorerNode.id == null) {
            return uuid.equals(explorerNode.uuid);
        }

//        builder.append(id, explorerNode.id);
//        builder.append(type, item.name);

        return id.equals(explorerNode.id);
    }

    @Override
    public String toString() {
        return getDisplayValue();
    }
}