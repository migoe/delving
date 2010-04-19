/*
 * Copyright 2007 EDL FOUNDATION
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

package eu.europeana.sip.gui;

import eu.europeana.sip.io.FileSet;
import eu.europeana.sip.mapping.MappingTree;
import eu.europeana.sip.mapping.Statistics;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.namespace.QName;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A Graphical interface for analysis
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 * @author Serkan Demirel <serkan@blackbuilt.nl>
 */

public class AnalyzerPanel extends JPanel {
    private static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(15, 15, 15, 15);
    private Logger log = Logger.getLogger(getClass());
    private JLabel title = new JLabel("Document Structure", JLabel.CENTER);
    private JTree statisticsJTree = new JTree(MappingTree.create("No Document Loaded").createTreeModel()); // todo: inform about available nodes
    private JLabel statsTitle = new JLabel("Statistics", JLabel.CENTER);

    private JTable statsTable;
    private DefaultTableColumnModel statisticsTableColumnModel;
    private FileMenu.Enablement fileMenuEnablement;
    private ProgressDialog progressDialog;
    private GroovyEditor groovyEditor = new GroovyEditor();
    private JButton nextRecordButton = new JButton("Next");
    private boolean abort = false;
    private MappingsPanel mappingsPanel = new MappingsPanel(new TreeMap<String, String>());

    private AnalyzerPanel instance;

    public AnalyzerPanel() {
        super(new BorderLayout());
        this.instance = this;
        createStatsTable();
        this.statsTitle.setFont(new Font("Serif", Font.BOLD, 20));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setLeftComponent(createAnalysisPanel());
        split.setRightComponent(createMappingPanel());
        split.setResizeWeight(0.5);
        split.setDividerLocation(0.5);
        add(split, BorderLayout.CENTER);
    }

    public void setFileSet(final FileSet fileSet) {
        try {
            final QName recordRoot = fileSet.getRecordRoot();
            List<Statistics> statistics = fileSet.getStatistics();
            if (statistics == null) {
                abort = false;
                fileMenuEnablement.enable(false);
                fileSet.analyze(new FileSet.AnalysisListener() {
                    @Override
                    public void success(List<Statistics> statistics) {
                        setMappingTree(MappingTree.create(statistics, fileSet.getName(), recordRoot));
                        fileMenuEnablement.enable(true);
                        if (progressDialog != null) {
                            progressDialog.setVisible(false);
                        }
                    }

                    @Override
                    public void failure(Exception exception) {
                        if (progressDialog != null) {
                            progressDialog.setVisible(false);
                        }
                        JOptionPane.showMessageDialog(AnalyzerPanel.this, "Error analyzing file : '" + exception.getMessage() + "'");
                        fileMenuEnablement.enable(true);
                    }

                    @Override
                    public void progress(final long recordNumber) {
                        if (progressDialog == null) {
                            try {
                                SwingUtilities.invokeAndWait(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressDialog = new ProgressDialog("Analyze", "Analyzed",
                                                new ProgressDialog.Listener() {

                                                    @Override
                                                    public void abort() {
                                                        abort = true;
                                                    }

                                                    @Override
                                                    public JFrame getFrame() {
                                                        return (JFrame) SwingUtilities.getWindowAncestor(instance);
                                                    }
                                                }
                                        );
                                        progressDialog.setVisible(true);
                                    }
                                });
                            }
                            catch (Exception e) {
                                e.printStackTrace();  // todo: something
                            }
                        }
                        else {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.setProgress(recordNumber);
                                }
                            });
                        }
                    }

                    @Override
                    public boolean abort() {
                        return abort;
                    }

                });
            }
            else {
                setMappingTree(MappingTree.create(statistics, fileSet.getName(), recordRoot));
            }
            groovyEditor.setFileSet(fileSet);
        }
        catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error analyzing file : '" + e.getMessage() + "'");
        }
    }

    private Component createMappingPanel() {
        JTabbedPane mappingPane = new JTabbedPane();
        mappingPane.addTab("Source", groovyEditor);
        Map<String, String> map = new TreeMap<String, String>();
        map.put("dc_date", "dc_date");
        map.put("dc_title", "dc_title");
        mappingPane.addTab("Mappings", mappingsPanel);
        return mappingPane;
    }

//    private Component createMappingPanel() {
//        JPanel p = new JPanel(new BorderLayout());
//        p.add(groovyEditor, BorderLayout.CENTER);
//        p.add(createNextButton(), BorderLayout.NORTH);
//        p.setPreferredSize(new Dimension(500, 800));
//        return p;
//    }

    private JComponent createNextButton() {
//        nextRecordButton.setEnabled(false);
        nextRecordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                groovyEditor.nextRecord();
            }
        });
        return nextRecordButton;
    }

    private Component createAnalysisPanel() {
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setTopComponent(createDocumentTreePanel());
        split.setBottomComponent(createStatisticsListPanel());
        split.setResizeWeight(0.5);
        split.setDividerLocation(0.5);
        return split;
    }

    private Component createDocumentTreePanel() {
        final AnalysisTreeCellRenderer analysisTreeCellRenderer = new AnalysisTreeCellRenderer();
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 0));
        title.setFont(new Font("Serif", Font.BOLD, 22));
        p.add(title, BorderLayout.NORTH);
        statisticsJTree.setCellRenderer(analysisTreeCellRenderer);
        statisticsJTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        statisticsJTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent event) {
                TreePath path = event.getPath();
                MappingTree.Node node = (MappingTree.Node) path.getLastPathComponent();
                setStatistics(node.getStatistics());
            }
        });
        statisticsJTree.addMouseListener(
                new MouseAdapter() {

                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (e.isPopupTrigger()) {
                            final TreePath path = statisticsJTree.getPathForLocation(e.getX(), e.getY());
                            statisticsJTree.setSelectionPath(path);
                            JPopupMenu delimiterPopup = new JPopupMenu();
                            JMenuItem delimiterMenuItem = new JMenuItem("Set as delimiter");
                            delimiterMenuItem.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    MappingTree.Node node = (MappingTree.Node) path.getLastPathComponent();
                                    QName recordRoot = node.getQName();
                                    DefaultTreeModel tm = (DefaultTreeModel) statisticsJTree.getModel();
                                    int count = MappingTree.setRecordRoot(tm, recordRoot);
                                    if (count != 1) {
                                        JOptionPane.showConfirmDialog(AnalyzerPanel.this, "Expected one record root, got " + count);
                                    }
                                    else {
                                        groovyEditor.setRecordRoot(recordRoot);
                                        tm.reload(node);
                                    }
                                }
                            });
                            delimiterPopup.add(delimiterMenuItem);
                            delimiterPopup.show(statisticsJTree, e.getX(), e.getY());
                        }
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                    }
                }
        );
        JScrollPane scroll = new JScrollPane(statisticsJTree);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private Component createStatisticsListPanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(statsTable.getTableHeader(), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(statsTable);
        tablePanel.add(scroll, BorderLayout.CENTER);
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(EMPTY_BORDER);
        p.add(statsTitle, BorderLayout.NORTH);
        p.add(tablePanel, BorderLayout.CENTER);
        return p;
    }

    private void createStatsTable() {
        statisticsTableColumnModel = new DefaultTableColumnModel();
        statisticsTableColumnModel.addColumn(new TableColumn(0, 70));
        statisticsTableColumnModel.getColumn(0).setHeaderValue("Percent");
        statisticsTableColumnModel.getColumn(0).setResizable(false);
        statisticsTableColumnModel.addColumn(new TableColumn(1, 90));
        statisticsTableColumnModel.getColumn(1).setHeaderValue("Count");
        statisticsTableColumnModel.getColumn(1).setResizable(false);
        statisticsTableColumnModel.addColumn(new TableColumn(2));
        statisticsTableColumnModel.getColumn(2).setHeaderValue("Value");
        statisticsTableColumnModel.getColumn(2).setPreferredWidth(300);
        statsTable = new JTable(new StatisticsCounterTableModel(null), statisticsTableColumnModel);
        statsTable.getTableHeader().setReorderingAllowed(false);
        statsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    private void setStatistics(Statistics statistics) {
        if (statistics == null) {
            statsTitle.setText("Statistics");
            statsTable.setModel(new StatisticsCounterTableModel(null));
            statsTable.setColumnModel(statisticsTableColumnModel);
        }
        else {
            statsTitle.setText("Statistics for \"" + statistics.getPath().getLastNodeString() + "\"");
            statsTable.setModel(new StatisticsCounterTableModel(statistics.getCounters()));
            statsTable.setColumnModel(statisticsTableColumnModel);
        }
    }

    /**
     * Listen to me if you want to get informed about the latest available nodes
     */
    interface Listener {
        void updateAvailableNodes(List<String> nodes);
    }

    private void setMappingTree(MappingTree mappingTree) {
        List<String> variables = new ArrayList<String>();
        mappingTree.getVariables(variables);
        if (null != groovyEditor) {
            groovyEditor.updateAvailableNodes(variables);
        }
        if (null != mappingsPanel) {
            mappingsPanel.updateAvailableNodes(variables);
        }
        for (String variable : variables) {
            log.info(variable);
        }
        TreeModel treeModel = mappingTree.createTreeModel();
        statisticsJTree.setModel(treeModel);
        expandEmptyNodes((MappingTree.Node) treeModel.getRoot());
    }

    private void expandEmptyNodes(MappingTree.Node node) {
        if (node.getStatistics() == null) {
            TreePath path = node.getTreePath();
            statisticsJTree.expandPath(path);
        }
        for (MappingTree.Node childNode : node.getChildNodes()) {
            expandEmptyNodes(childNode);
        }
    }

    public void setFileMenuEnablement(FileMenu.Enablement fileMenuEnablement) {
        this.fileMenuEnablement = fileMenuEnablement;
    }
}
