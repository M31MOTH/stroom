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

package stroom.widget.htree.client.treelayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyTreeLayout<TreeNode> implements TreeLayout<TreeNode> {
    private TreeForTreeLayout<TreeNode> tree;
    private final NodeExtentProvider<TreeNode> nodeExtentProvider;
    private final Configuration<TreeNode> configuration;

    private Map<TreeNode, Bounds> boundsMap;
    private double boundsX;
    private double boundsY;
    private double boundsWidth;
    private double boundsHeight;

    public MyTreeLayout(final NodeExtentProvider<TreeNode> nodeExtentProvider,
            final Configuration<TreeNode> configuration) {
        this.nodeExtentProvider = nodeExtentProvider;
        this.configuration = configuration;
    }

    @Override
    public void layout() {
        boundsMap = new HashMap<>();
        boundsX = 0;
        boundsY = 0;
        boundsWidth = 0;
        boundsHeight = 0;
        if (tree != null) {
            calculate(tree.getRoot(), 0, 0, 0);
        }
    }

    private double calculate(final TreeNode node, final double x, final double y, final int depth) {
        double totalHeight = 0;

        if (node != null) {
            final Dimension dimension = nodeExtentProvider.getExtents(node);
            final double width = dimension.getWidth();
            final double height = dimension.getHeight();
            totalHeight = height;

            final List<TreeNode> children = tree.getChildren(node);
            if (children != null && children.size() > 0) {
                final double xOffset = width + configuration.getGapBetweenLevels(depth);
                double yOffset = 0;
                final double childX = x + xOffset;
                double childY = y + yOffset;

                TreeNode lastChild = null;
                for (final TreeNode child : children) {
                    if (lastChild != null) {
                        final double space = configuration.getGapBetweenNodes(child, lastChild);
                        yOffset += space;
                        childY = y + yOffset;
                    }

                    final double childHeight = calculate(child, childX, childY, depth + 1);

                    yOffset += childHeight;
                    lastChild = child;
                }

                if (yOffset > totalHeight) {
                    totalHeight = yOffset;
                }
            }

            final Bounds bounds = new Bounds(x, y, width, totalHeight);
            boundsMap.put(node, bounds);

            // Increase outer bounds for whole tree if necessary.
            final double maxX = bounds.getMaxX();
            if (boundsWidth < maxX) {
                boundsWidth = maxX;
            }
            final double maxY = bounds.getMaxY();
            if (boundsHeight < maxY) {
                boundsHeight = maxY;
            }
        }

        return totalHeight;
    }

    @Override
    public Bounds getBounds() {
        return new Bounds(boundsX, boundsY, boundsWidth, boundsHeight);
    }

    @Override
    public Map<TreeNode, Bounds> getNodeBounds() {
        return boundsMap;
    }

    @Override
    public TreeForTreeLayout<TreeNode> getTree() {
        return tree;
    }

    @Override
    public void setTree(final TreeForTreeLayout<TreeNode> tree) {
        this.tree = tree;
    }
}
