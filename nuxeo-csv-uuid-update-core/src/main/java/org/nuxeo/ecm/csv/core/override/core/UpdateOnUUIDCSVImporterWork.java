package org.nuxeo.ecm.csv.core.override.core;

import static org.nuxeo.ecm.csv.core.Constants.CSV_NAME_COL;
import static org.nuxeo.ecm.csv.core.Constants.CSV_TYPE_COL;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.csv.core.CSVImportLog;
import org.nuxeo.ecm.csv.core.CSVImportStatus;
import org.nuxeo.ecm.csv.core.CSVImporterOptions;
import org.nuxeo.ecm.csv.core.CSVImporterWork;
import org.nuxeo.runtime.api.Framework;

public class UpdateOnUUIDCSVImporterWork extends CSVImporterWork {

    static public final Serializable EMPTY_LOGS = new ArrayList<CSVImportLog>();

    private static final long serialVersionUID = 1L;

    private static final Logger log = LogManager.getLogger(UpdateOnUUIDCSVImporterWork.class);

    public UpdateOnUUIDCSVImporterWork(String id) {
        super(id);
    }

    public UpdateOnUUIDCSVImporterWork(String repositoryName, String parentPath, String username, Blob csvBlob,
            CSVImporterOptions options) {
        super(repositoryName, parentPath, username, csvBlob, options);
    }

    public String launch() {
        WorkManager works = Framework.getService(WorkManager.class);

        TransientStore store = getStore();
        store.putParameter(id, "logs", EMPTY_LOGS);
        store.putParameter(id, "status", new CSVImportStatus(CSVImportStatus.State.SCHEDULED));
        works.schedule(this);
        return id;
    }

    @Override
    protected boolean importRecord(CSVRecord record, Map<String, Integer> header) {
        String uuid = record.get(NXQL.ECM_UUID);
        Path targetPath;
        String name;
        // if a document UUID is provided, it is an update of an existing document
        if (!StringUtils.isBlank(uuid)) {
            if (log.isDebugEnabled()) {
                log.debug("Document UUID found!");
            }
            if (!session.exists(new IdRef(uuid))) {
                logError(getLineNumber(record), "Unable to update document, document %s does not exist", LABEL_CSV_IMPORTER_UNABLE_TO_UPDATE, uuid);
                return false;
            }
            targetPath = session.getDocument(new IdRef(uuid)).getPath();
        } else {
            name = record.get(CSV_NAME_COL);
            if (StringUtils.isBlank(name)) {
                log.debug("record.isSet={}", () -> record.isSet(CSV_NAME_COL));
                logError(getLineNumber(record), "Missing 'name' value", LABEL_CSV_IMPORTER_MISSING_NAME_VALUE);
                return false;
            }
            targetPath = new Path(parentPath).append(name);
        }
        name = targetPath.lastSegment();
        String newParentPath = targetPath.removeLastSegments(1).toString();
        boolean exists = options.getCSVImporterDocumentFactory().exists(session, newParentPath, name, null);

        DocumentRef docRef = null;
        String type = null;
        if (exists) {
            docRef = new PathRef(targetPath.toString());
            type = session.getDocument(docRef).getType();
        } else {
            if (hasTypeColumn) {
                type = record.get(CSV_TYPE_COL);
            }
            if (StringUtils.isBlank(type)) {
                log.debug("record.isSet={}", () -> record.isSet(CSV_TYPE_COL));
                logError(getLineNumber(record), "Missing 'type' value", LABEL_CSV_IMPORTER_MISSING_TYPE_VALUE);
                return false;
            }
        }

        DocumentType docType = Framework.getService(SchemaManager.class).getDocumentType(type);
        if (docType == null) {
            logError(getLineNumber(record), "The type '%s' does not exist", LABEL_CSV_IMPORTER_NOT_EXISTING_TYPE, type);
            return false;
        }
        Map<String, Serializable> properties = computePropertiesMap(record, docType, header);
        if (properties == null) {
            // skip this line
            return false;
        }
        if (!StringUtils.isBlank(uuid)) {
            // We don't want to update the UUID
            properties.remove(NXQL.ECM_UUID);
        }
        long lineNumber = getLineNumber(record);
        if (exists) {
            return updateDocument(lineNumber, docRef, properties);
        } else {
            return createDocument(lineNumber, newParentPath, name, type, properties);
        }
    }

}
