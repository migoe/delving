/*
 * Copyright 2010 DELVING BV
 *
 * Licensed under the EUPL, Version 1.1 or as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * you may not use this work except in compliance with the
 * Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.delving.services.core.impl;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import eu.delving.metadata.Facts;
import eu.delving.metadata.MetadataNamespace;
import eu.delving.metadata.Path;
import eu.delving.metadata.RecordMapping;
import eu.delving.services.core.MetaRepo;
import eu.delving.services.core.MongoObjectParser;
import eu.delving.services.exceptions.AccessKeyException;
import eu.delving.services.exceptions.MappingNotFoundException;
import eu.delving.services.exceptions.MetaRepoSystemException;
import eu.delving.services.exceptions.RecordParseException;
import eu.delving.sip.DataSetState;
import org.bson.types.ObjectId;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static eu.delving.core.util.MongoObject.mob;

/**
 * Implementing the data set interface
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

class DataSetImpl implements MetaRepo.DataSet {
    private ImplFactory implFactory;
    private DBObject object;
    private DBCollection recColl;

    DataSetImpl(ImplFactory implFactory, DBObject object) {
        this.implFactory = implFactory;
        this.object = object;
    }

    private DBCollection records() {
        if (recColl == null) {
            recColl = implFactory.records(getSpec());
        }
        return recColl;
    }

    @Override
    public String getSpec() {
        return (String) object.get(SPEC);
    }

    @Override
    public DBObject getNamespaces() {
        return (DBObject) object.get(NAMESPACES);
    }

    @Override
    public DataSetState getState(boolean fresh) {
        if (fresh) {
            object = implFactory.dataSets().findOne(mob("_id", object.get("_id")));
        }
        return DataSetState.get((String) object.get(DATA_SET_STATE));
    }

    @Override
    public String getErrorMessage() {
        return (String) object.get(ERROR_MESSAGE);
    }

    @Override
    public void setState(DataSetState dataSetState) {
        object.put(DATA_SET_STATE, dataSetState.toString());
        object.removeField(ERROR_MESSAGE);
    }

    @Override
    public void setErrorState(String message) {
        setState(DataSetState.ERROR);
        object.put(ERROR_MESSAGE, message);
    }

    @Override
    public void parseRecords(InputStream inputStream) throws RecordParseException {
        implFactory.removeFirstHarvestSteps(getSpec());
        object.put(SOURCE_HASH, "");
        save();
        try {
            MetaRepo.Details details = getDetails();
            Facts facts = Facts.read(new ByteArrayInputStream(details.getFacts()));
            MongoObjectParser parser = new MongoObjectParser(
                    inputStream,
                    new Path(facts.getRecordRootPath()),
                    new Path(facts.getUniqueElementPath()),
                    details.getMetadataFormat().getPrefix(),
                    details.getMetadataFormat().getNamespace()
            );
            MongoObjectParser.Record record;
            Date modified = new Date();
            object.put(NAMESPACES, parser.getNamespaces());
            while ((record = parser.nextRecord()) != null) {
                record.getMob().put(MetaRepo.Record.MODIFIED, modified);
                record.getMob().put(MetaRepo.Record.DELETED, false);
                records().update( // just match unique values for now
                        mob(
                                MetaRepo.Record.UNIQUE,
                                record.getMob().get(MetaRepo.Record.UNIQUE)
                        ),
                        record.getMob(),
                        true,
                        false
                );
            }
            parser.close();
            // mark records unmodified by above loop as deleted
            records().update(
                    mob(
                            MetaRepo.Record.MODIFIED,
                            mob("$lt", modified)
                    ),
                    mob(
                            "$set",
                            mob(MetaRepo.Record.DELETED, true)
                    ),
                    false,
                    true
            );
        }
        catch (Exception e) {
            throw new RecordParseException("Unable to parse records", e);
        }
        object.put(MetaRepo.DataSet.DATA_SET_STATE, DataSetState.UPLOADED.toString());
        save();
    }

    @Override
    public void setMapping(RecordMapping recordMapping, boolean accessKeyRequired) {
        DBObject mappings = (DBObject) object.get(MAPPINGS);
        if (mappings == null) {
            object.put(MAPPINGS, mappings = mob());
        }
        MetadataNamespace mappedNamespace = null;
        for (MetadataNamespace namespace : MetadataNamespace.values()) {
            if (namespace.getPrefix().equals(recordMapping.getPrefix())) {
                mappedNamespace = namespace;
                break;
            }
        }
        if (mappedNamespace == null) {
            throw new MetaRepoSystemException(String.format("Namespace prefix %s not recognized", recordMapping.getPrefix()));
        }
        implFactory.removeFirstHarvestSteps(getSpec());
        String xml = RecordMapping.toXml(recordMapping);
        mappings.put(
                mappedNamespace.getPrefix(),
                mob(
                        MetaRepo.Mapping.FORMAT,
                        mob(
                                MetaRepo.MetadataFormat.PREFIX, mappedNamespace.getPrefix(),
                                MetaRepo.MetadataFormat.NAMESPACE, mappedNamespace.getUri(),
                                MetaRepo.MetadataFormat.SCHEMA, mappedNamespace.getSchema(),
                                MetaRepo.MetadataFormat.ACCESS_KEY_REQUIRED, accessKeyRequired
                        ),
                        MetaRepo.Mapping.RECORD_MAPPING, xml
                )
        );
        save();
    }

    @Override
    public void setFactsHash(String sourceHash) {
        object.put(FACTS_HASH, sourceHash);
    }

    @Override
    public void setSourceHash(String sourceHash, boolean downloaded) {
        object.put(downloaded ? DOWNLOADED_SOURCE_HASH : SOURCE_HASH, sourceHash);
    }

    @Override
    public void setMappingHash(String metadataPrefix, String hash) {
        object.put(MAPPING_HASH_PREFIX + metadataPrefix, hash);
    }

    @Override
    public int getRecordsIndexed() {
        Object obj = object.get(RECORDS_INDEXED);
        if (obj == null) {
            return 0;
        }
        return (Integer) obj;
    }

    @Override
    public void setRecordsIndexed(int count) {
        object.put(RECORDS_INDEXED, count);
    }

    @Override
    public void incrementRecordsIndexed(int increment) {
        implFactory.dataSets().update(
                mob(
                        MetaRepo.DataSet.SPEC,
                        getSpec()
                ),
                mob(
                        "$inc",
                        mob(
                                RECORDS_INDEXED,
                                increment
                        )
                )
        );
        setRecordsIndexed(getRecordsIndexed() + increment);
    }

    @Override
    public boolean hasDetails() {
        return object.get(DETAILS) != null;
    }

    @Override
    public MetaRepo.Details createDetails() {
        DBObject details = mob();
        object.put(DETAILS, details);
        return new DetailsImpl(details);
    }

    @Override
    public MetaRepo.Details getDetails() {
        Object detailsObject = object.get(DETAILS);
        if (detailsObject == null) {
            throw new MetaRepoSystemException("No Details found");
        }
        return new DetailsImpl((DBObject) detailsObject);
    }

    @Override
    public Map<String, MetaRepo.Mapping> mappings() {
        Map<String, MetaRepo.Mapping> mappingMap = new TreeMap<String, MetaRepo.Mapping>();
        DBObject mappingsObject = (DBObject) object.get(MAPPINGS);
        if (mappingsObject != null) {
            for (String prefix : mappingsObject.keySet()) {
                mappingMap.put(prefix, implFactory.createMapping(this, (DBObject) mappingsObject.get(prefix)));
            }
        }
        return mappingMap;
    }

    @Override
    public int getRecordCount() {
        return records().find().count();
    }

    @Override
    public MetaRepo.Record getRecord(ObjectId id, String prefix, String accessKey) throws MappingNotFoundException, AccessKeyException { // if prefix is passed in, mapping can be done
        DBObject rawRecord = records().findOne(mob(MetaRepo.MONGO_ID, id));
        if (rawRecord != null) {
            MetaRepo.Mapping mapping = getMapping(prefix, accessKey);
            List<RecordImpl> list = new ArrayList<RecordImpl>();
            RecordImpl rec = new RecordImpl(records(), rawRecord, getDetails().getMetadataFormat().getPrefix(), getNamespaces());
            System.out.println("Fingerprint {");
            for (Map.Entry<String, Integer> entry : rec.getFingerprint().entrySet()) {
                System.out.println("   " + entry.getKey() + " => " + entry.getValue());
            }
            System.out.println("}");
            list.add(rec);
            if (mapping != null) {
                Map<String, String> namespaces = new TreeMap<String, String>();
                DBObject namespacesObject = getNamespaces();
                for (String nsPrefix : namespacesObject.keySet()) {
                    namespaces.put(nsPrefix, (String) namespacesObject.get(nsPrefix));
                }
                ((MappingInternal) mapping).executeMapping(list, namespaces);
            }
            return list.isEmpty() ? null : list.get(0);
        }
        return null;
    }

    @Override
    public RecordFetch getRecords(String prefix, int count, Date from, ObjectId afterId, Date until, String accessKey) throws MappingNotFoundException, AccessKeyException {
        MetaRepo.Mapping mapping = getMapping(prefix, accessKey);
        final List<RecordImpl> list = new ArrayList<RecordImpl>();
        DBCursor cursor = createCursor(from, afterId, until).limit(count).sort(mob(MetaRepo.MONGO_ID, 1));
        while (cursor.hasNext()) {
            DBObject object = cursor.next();
            list.add(new RecordImpl(records(), object, getDetails().getMetadataFormat().getPrefix(), getNamespaces()));
        }
        if (list.isEmpty()) {
            return null;
        }
        else {
            final ObjectId nextAfterId = list.get(list.size() - 1).getId();
            if (mapping != null) {
                Map<String, String> namespaces = new TreeMap<String, String>();
                DBObject namespacesObject = (DBObject) object.get(NAMESPACES);
                for (String nsPrefix : namespacesObject.keySet()) {
                    namespaces.put(nsPrefix, (String) namespacesObject.get(nsPrefix));
                }
                ((MappingInternal) mapping).executeMapping(list, namespaces); // can remove members when records don't validate
            }
            return new RecordFetch() {

                @Override
                public List<? extends MetaRepo.Record> getRecords() {
                    return list;
                }

                @Override
                public ObjectId getAfterId() {
                    return nextAfterId;
                }
            };
        }
    }

    @Override
    public List<String> getHashes() {
        List<String> hashes = new ArrayList<String>();
        addHash(FACTS_HASH, hashes);
        addHash(SOURCE_HASH, hashes);
        addHash(DOWNLOADED_SOURCE_HASH, hashes);
        for (String metadataPrefix : implFactory.getMetadataModel().getPrefixes()) {
            addHash(MAPPING_HASH_PREFIX + metadataPrefix, hashes);
        }
        return hashes;
    }

    @Override
    public void save() {
        implFactory.dataSets().save(object);
    }

    @Override
    public void delete() {
        records().drop();
        implFactory.dataSets().remove(object);
    }

    @Override
    public String toString() {
        return String.format("DataSet(%s)", getSpec());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSetImpl dataSet = (DataSetImpl) o;
        return getSpec().equals(dataSet.getSpec());
    }

    @Override
    public int hashCode() {
        return getSpec().hashCode();
    }

    private void addHash(String hashAttribute, List<String> hashes) {
        String hash = (String) object.get(hashAttribute);
        if (hash != null) {
            hashes.add(hash);
        }
    }

    private MetaRepo.Mapping getMapping(String prefix, String key) throws AccessKeyException, MappingNotFoundException {
        MetaRepo.Mapping mapping;
        if (getDetails().getMetadataFormat().getPrefix().equals(prefix)) {
            mapping = null;
            if (getDetails().getMetadataFormat().isAccessKeyRequired() && !implFactory.getAccessKey().checkKey(key)) {
                throw new AccessKeyException(String.format("Raw metadata format requires access key, but %s is not valid", key));
            }
        }
        else {
            mapping = mappings().get(prefix);
            if (mapping == null) {
                throw new MappingNotFoundException(String.format("No mapping found to prefix %s", prefix));
            }
            if (mapping.getMetadataFormat().isAccessKeyRequired() && !implFactory.getAccessKey().checkKey(key)) {
                throw new AccessKeyException(String.format("Mapping to metadata format requires access key, but %s is not valid", key));
            }
        }
        return mapping;
    }

    private DBCursor createCursor(Date from, ObjectId afterId, Date until) {
        DBObject query = mob();
        if (from != null) {
            query.put(MetaRepo.Record.MODIFIED, mob("$gte", from));
        }
        if (afterId != null) {
            query.put(MetaRepo.MONGO_ID, mob("$gt", afterId));
        }
        if (until != null) {
            query.put(MetaRepo.Record.MODIFIED, mob("$lte", until));
        }
        return records().find(query);
    }
}