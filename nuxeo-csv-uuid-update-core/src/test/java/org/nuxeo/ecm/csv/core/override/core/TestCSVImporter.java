package org.nuxeo.ecm.csv.core.override.core;

import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.csv.core.CSVImporter;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import javax.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class })
@Deploy("org.nuxeo.ecm.csv.core.override.core.nuxeo-csv-uuid-update-core")
public class TestCSVImporter {

    @Inject
    protected CSVImporter csvimporter;

    @Ignore @Test
    public void testService() {
        assertNotNull(csvimporter);
    }
}
