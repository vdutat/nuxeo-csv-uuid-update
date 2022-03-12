package org.nuxeo.ecm.csv.core.override.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.csv.core.CSVImporter;
import org.nuxeo.ecm.csv.core.CSVImporterOptions;

public class CSVImporterImpl extends org.nuxeo.ecm.csv.core.CSVImporterImpl implements CSVImporter {

    private static final Log log = LogFactory.getLog(CSVImporterImpl.class);

    @Override
    public String launchImport(CoreSession session, String parentPath, Blob blob, CSVImporterOptions options) {
        log.warn("<launchImport> ");
        return new UpdateOnUUIDCSVImporterWork(session.getRepositoryName(), parentPath, session.getPrincipal().getName(), blob,
                options).launch();
    }

}
