package org.sagebionetworks.bridge.dataUploadUtils;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.testng.annotations.Test;

public class ArchiveInfoTest {
    @Test
    public void equalsVerifier() {
        EqualsVerifier.forClass(Archive.class).allFieldsShouldBeUsed().verify();
    }
}
