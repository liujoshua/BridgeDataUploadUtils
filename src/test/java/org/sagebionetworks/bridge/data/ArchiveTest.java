package org.sagebionetworks.bridge.data;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.internal.junit.ArrayAsserts.assertArrayEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.common.io.ByteSource;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.joda.time.DateTime;
import org.testng.annotations.Test;
import org.testng.collections.Maps;

import org.sagebionetworks.bridge.rest.RestUtils;

public class ArchiveTest {
    private static final String APP_VERSION_NAME = "version 1.0, build 9";
    private static final String DEVICE_NAME = "device";
    private static final String TEST_ITEM_NAME = "legacy-survey";
    private static final int TEST_SCHEMA_REVISION = 1;
    private static final String TEST_PHONE_INFO = "test-phone-info";

    private static final byte[] BYTES = new byte[]{1, 2, 3, 4};

    @Test
    public void testBuildActivity() throws IOException {

        String filename1 = "file1";
        DateTime endTime1 = DateTime.now().minusHours(1);
        String json = "{'key' : 'value'}";

        String filename2 = "file2";
        DateTime endTime2 = DateTime.now().minusHours(2);
        ByteSource byteSource = ByteSource.wrap(BYTES);

        ArchiveFile file1 = new JsonArchiveFile(filename1, endTime1, json);
        ArchiveFile file2 = new ByteSourceArchiveFile(filename2, endTime2, byteSource);

        Archive archive = Archive.Builder.forActivity(TEST_ITEM_NAME, TEST_SCHEMA_REVISION)
                .withAppVersionName(APP_VERSION_NAME)
                .withPhoneInfo(TEST_PHONE_INFO)
                .addDataFile(file1)
                .addDataFile(file2)
                .build();

        ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
        archive.writeTo(zipOutput);

        Map<String, ByteArrayOutputStream> map = Maps.newHashMap();
        byte[] buffer = new byte[1024];

        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipOutput.toByteArray()));
        ZipEntry zipEntry = zis.getNextEntry();
        try {
            while (zipEntry != null) {
                String fileName = zipEntry.getName();

                ByteArrayOutputStream fileBytes = new ByteArrayOutputStream();
                int len = 0;
                while ((len = zis.read(buffer)) > 0) {
                    fileBytes.write(buffer, 0, len);
                }

                map.put(fileName, fileBytes);
                zis.closeEntry();
                zipEntry = zis.getNextEntry();
            }
        } finally {
            zis.close();
        }

        ByteArrayOutputStream bytes1 = map.get(filename1);
        assertNotNull(bytes1);
        assertEquals(json, new String(bytes1.toByteArray()));

        ByteArrayOutputStream bytes2 = map.get(filename2);
        assertNotNull(bytes2);
        assertArrayEquals(BYTES, bytes2.toByteArray());

        ByteArrayOutputStream bytesInfo = map.get("info.json");
        assertNotNull(bytesInfo);

        ArchiveInfo info = RestUtils.GSON.fromJson(bytesInfo.toString(), ArchiveInfo.class);

        assertTrue(info.isValid());
        assertTrue(info.isSchema());
        assertFalse(info.isSurvey());

        assertEquals(APP_VERSION_NAME, info.appVersion);
        assertEquals(TEST_PHONE_INFO, info.phoneInfo);

        assertEquals(TEST_ITEM_NAME, info.item);
        assertEquals(TEST_SCHEMA_REVISION, info.schemaRevision);

        List<ArchiveInfo.FileInfo> files = info.files;

        ArchiveInfo.FileInfo info1 = files.get(0);
        assertFileInfoForFile(file1, info1, bytes1.toByteArray());

        ArchiveInfo.FileInfo info2 = files.get(1);
        assertFileInfoForFile(file2, info2, bytes2.toByteArray());
    }

    @Test
    public void testBuildSurvey() throws IOException {
        String surveyGuid = "survey";
        DateTime surveyCreatedOn = DateTime.now();


        String filename1 = "file1";
        DateTime endTime1 = DateTime.now().minusHours(1);
        String json = "{'key' : 'value'}";

        String filename2 = "file2";
        DateTime endTime2 = DateTime.now().minusHours(2);
        ByteSource byteSource = ByteSource.wrap(BYTES);

        ArchiveFile file1 = new JsonArchiveFile(filename1, endTime1, json);
        ArchiveFile file2 = new ByteSourceArchiveFile(filename2, endTime2, byteSource);

        Archive archive = Archive.Builder.forSurvey(surveyGuid, surveyCreatedOn)
                .withAppVersionName(APP_VERSION_NAME)
                .withPhoneInfo(TEST_PHONE_INFO)
                .addDataFile(file1)
                .addDataFile(file2)
                .build();

        ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
        archive.writeTo(zipOutput);

        Map<String, ByteArrayOutputStream> map = Maps.newHashMap();
        byte[] buffer = new byte[1024];

        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipOutput.toByteArray()));
        ZipEntry zipEntry = zis.getNextEntry();
        try {
            while (zipEntry != null) {
                String fileName = zipEntry.getName();

                ByteArrayOutputStream fileBytes = new ByteArrayOutputStream();
                int len = 0;
                while ((len = zis.read(buffer)) > 0) {
                    fileBytes.write(buffer, 0, len);
                }

                map.put(fileName, fileBytes);
                zis.closeEntry();
                zipEntry = zis.getNextEntry();
            }
        } finally {
            zis.close();
        }

        ByteArrayOutputStream bytes1 = map.get(filename1);
        assertNotNull(bytes1);
        assertEquals(json, new String(bytes1.toByteArray()));

        ByteArrayOutputStream bytes2 = map.get(filename2);
        assertNotNull(bytes2);
        assertArrayEquals(BYTES, bytes2.toByteArray());

        ByteArrayOutputStream bytesInfo = map.get("info.json");
        assertNotNull(bytesInfo);

        ArchiveInfo info = RestUtils.GSON.fromJson(bytesInfo.toString(), ArchiveInfo.class);

        assertTrue(info.isValid());
        assertTrue(info.isSurvey());
        assertFalse(info.isSchema());

        assertEquals(APP_VERSION_NAME, info.appVersion);
        assertEquals(TEST_PHONE_INFO, info.phoneInfo);

        assertEquals(surveyGuid, info.surveyGuid);
        assertEquals(surveyCreatedOn.getMillis(), info.surveyCreatedOn.getMillis());

        List<ArchiveInfo.FileInfo> files = info.files;

        ArchiveInfo.FileInfo info1 = files.get(0);
        assertFileInfoForFile(file1, info1, bytes1.toByteArray());

        ArchiveInfo.FileInfo info2 = files.get(1);
        assertFileInfoForFile(file2, info2, bytes2.toByteArray());
    }

    @Test
    public void equalsVerifier() {
        EqualsVerifier.forClass(Archive.class).allFieldsShouldBeUsed().verify();
    }

    private void assertFileInfoForFile(ArchiveFile file, ArchiveInfo.FileInfo info, byte[] contents) throws IOException {
        assertEquals(file.getFilename(), info.filename);
        assertTrue(file.getEndDate().isEqual(info.timestamp));
        assertArrayEquals(file.getByteSource().read(), contents);
    }
}
