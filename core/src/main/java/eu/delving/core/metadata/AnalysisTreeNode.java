/*
 * Copyright 2010 DELVING BV
 *
 *  Licensed under the EUPL, Version 1.0 or? as soon they
 *  will be approved by the European Commission - subsequent
 *  versions of the EUPL (the "Licence");
 *  you may not use this work except in compliance with the
 *  Licence.
 *  You may obtain a copy of the Licence at:
 *
 *  http://ec.europa.eu/idabc/eupl
 *
 *  Unless required by applicable law or agreed to in
 *  writing, software distributed under the Licence is
 *  distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied.
 *  See the Licence for the specific language governing
 *  permissions and limitations under the Licence.
 */

package eu.delving.core.metadata;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * A node of the analysis tree
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class AnalysisTreeNode implements AnalysisTree.Node, Serializable {
    private static final long serialVersionUID = -8362212829296408316L;
    private AnalysisTreeNode parent;
    private List<AnalysisTreeNode> children = new ArrayList<AnalysisTreeNode>();
    private String tag;
    private QName qName;
    private boolean recordRoot, uniqueElement;
    private AnalysisTreeStatistics statistics;

    AnalysisTreeNode(String tag) {
        this.tag = tag;
    }

    AnalysisTreeNode(AnalysisTreeNode parent, QName qName) {
        this.parent = parent;
        this.qName = qName;
    }

    AnalysisTreeNode(AnalysisTreeNode parent, AnalysisTreeStatistics statistics) {
        this.parent = parent;
        this.statistics = statistics;
        this.qName = statistics.getPath().peek();
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setStatistics(AnalysisTreeStatistics statistics) {
        this.statistics = statistics;
    }

    public List<AnalysisTreeNode> getChildren() {
        return children;
    }

    @Override
    public AnalysisTreeStatistics getStatistics() {
        return statistics;
    }

    @Override
    public TreePath getTreePath() {
        List<AnalysisTreeNode> list = new ArrayList<AnalysisTreeNode>();
        compilePathList(list);
        return new TreePath(list.toArray());
    }

    @Override
    public QName getQName() {
        return qName;
    }

    @Override
    public boolean setRecordRoot(QName recordRoot) {
        boolean oldValue = this.recordRoot;
        this.recordRoot = recordRoot != null && qName != null && qName.equals(recordRoot);
        return this.recordRoot != oldValue;
    }

    @Override
    public boolean setUniqueElement(QName uniqueElement) {
        boolean oldValue = this.uniqueElement;
        this.uniqueElement = uniqueElement != null && qName != null && qName.equals(uniqueElement);
        return this.uniqueElement != oldValue;
    }

    @Override
    public boolean isRecordRoot() {
        return recordRoot;
    }

    @Override
    public boolean isUniqueElement() {
        return uniqueElement;
    }

    @Override
    public Iterable<? extends AnalysisTree.Node> getChildNodes() {
        return children;
    }

    @Override
    public boolean couldBeRecordRoot() {
        return statistics != null && statistics.isEmpty();
    }

    @Override
    public String getVariableName() {
        List<AnalysisTreeNode> path = new ArrayList<AnalysisTreeNode>();
        AnalysisTreeNode node = this;
        while (node != null && !node.isRecordRoot()) {
            path.add(node);
            node = node.parent;
        }
        Collections.reverse(path);
        StringBuilder out = new StringBuilder("input.");
        Iterator<AnalysisTreeNode> nodeWalk = path.iterator();
        while (nodeWalk.hasNext()) {
            String nodeName = nodeWalk.next().toString();
            out.append(SourceVariable.Filter.tagToVariable(nodeName));
            if (nodeWalk.hasNext()) {
                out.append('.');
            }
        }
        return out.toString();
    }

    private void compilePathList(List<AnalysisTreeNode> list) {
        if (parent != null) {
            parent.compilePathList(list);
        }
        list.add(this);
    }

    public void add(AnalysisTreeNode child) {
        children.add(child);
    }

    @Override
    public TreeNode getChildAt(int index) {
        return children.get(index);
    }

    @Override
    public int getChildCount() {
        return children.size();
    }

    @Override
    public TreeNode getParent() {
        return parent;
    }

    @Override
    public int getIndex(TreeNode treeNode) {
        AnalysisTreeNode qNameNode = (AnalysisTreeNode) treeNode;
        return children.indexOf(qNameNode);
    }

    @Override
    public boolean getAllowsChildren() {
        return statistics != null && statistics.isEmpty();
//        return !children.isEmpty();
    }

    @Override
    public boolean isLeaf() {
        return statistics != null && !statistics.isEmpty();
    }

    @Override
    public Enumeration children() {
        return new Vector<AnalysisTreeNode>(children).elements();
    }

    public String toString() {
        if (qName == null) {
            return tag;
        }
        else if (!qName.getPrefix().isEmpty()) {
            return qName.getPrefix() + '_' + qName.getLocalPart();
        }
        else {
            return qName.getLocalPart();
        }
    }

    @Override
    public int compareTo(AnalysisTree.Node other) {
        return getVariableName().compareTo(other.getVariableName());
    }
}