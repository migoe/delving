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

package eu.europeana.sip.gui;

import eu.delving.sip.DataSetInfo;
import eu.delving.sip.FileStore;
import eu.delving.sip.FileStoreException;

import javax.swing.AbstractListModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generate a description in HTML of a Data Set
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class DataSetListModel extends AbstractListModel {

    private List<Entry> entries = new ArrayList<Entry>();

    public void setDataSetStore(FileStore.DataSetStore dataSetStore) {
        Entry entry = getEntry(dataSetStore.getSpec());
        entry.setDataSetStore(dataSetStore);
    }

    public void setDataSetInfo(DataSetInfo dataSetInfo) {
        Entry entry = getEntry(dataSetInfo.spec);
        entry.setDataSetInfo(dataSetInfo);
    }

    public Entry getEntry(int i) {
        return entries.get(i);
    }

    @Override
    public int getSize() {
        return entries.size();
    }

    @Override
    public Object getElementAt(int i) {
        return entries.get(i);
    }

    public class Entry implements Comparable<Entry> {
        int index = -1;
        private String spec;
        private FileStore.DataSetStore dataSetStore;
        private DataSetInfo dataSetInfo;

        public Entry(String spec) {
            this.spec = spec;
        }

        public void setDataSetStore(FileStore.DataSetStore dataSetStore) {
            this.dataSetStore = dataSetStore;
            fireContentsChanged(DataSetListModel.this, index, index);
        }

        public String getSpec() {
            return spec;
        }

        public FileStore.DataSetStore getDataSetStore() {
            return dataSetStore;
        }

        public DataSetInfo getDataSetInfo() {
            return dataSetInfo;
        }

        public void setDataSetInfo(DataSetInfo dataSetInfo) {
            this.dataSetInfo = dataSetInfo;
            fireContentsChanged(DataSetListModel.this, index, index);
        }

        public String toHtml() throws FileStoreException {
            StringBuilder out = new StringBuilder(String.format("<html><h1>%s</h1>", spec));
            if (dataSetStore != null) {
                // todo: it appears locally!
                if (dataSetStore.hasSource()) {
                    for (File mappingDir : dataSetStore.getMappingDirectories()) {
                        // todo: it has a mapping
                        // todo: show its status
                    }
                }
            }
            if (dataSetInfo != null) {
                // todo: it appears on the server
                // todo: show strings according to state
            }
            return out.toString();
        }

        public String toString() {
            try {
                return toHtml();
            }
            catch (FileStoreException e) {
                return e.toString();
            }
        }

        @Override
        public int compareTo(Entry entry) {
            return spec.compareTo(entry.spec);
        }
    }

    private Entry getEntry(String spec) {
        for (Entry entry : entries) {
            if (entry.spec.equals(spec)) {
                return entry;
            }
        }
        Entry fresh = new Entry(spec);
        int added = entries.size();
        entries.add(fresh);
        fireIntervalAdded(this, added, added);
        Collections.sort(entries);
        int index = 0;
        for (Entry entry : entries) {
            if (entry.index != index) {
                entry.index = index;
                fireContentsChanged(this, index, index);
            }
            index++;
        }
        return fresh;
    }
}
