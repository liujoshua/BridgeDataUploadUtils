package org.sagebionetworks.bridge.dataUploadUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;

import org.sagebionetworks.bridge.rest.RestUtils;

public class Archive {
    private static final String ARCHIVE_INFO_FILE_NAME = "info.json";

    private final List<ArchiveFile> dataFiles;
    private final ArchiveInfo archiveInfo;

    private Archive(List<ArchiveFile> dataFiles, ArchiveInfo archiveInfo) {
        this.dataFiles = dataFiles;
        this.archiveInfo = archiveInfo;
    }

    public ZipOutputStream writeTo(OutputStream os) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(os);
        try {
            for (ArchiveFile dataFile : dataFiles) {
                ZipEntry entry = new ZipEntry(dataFile.getFilename());

                zos.putNextEntry(entry);
                zos.write(dataFile.getByteSource().read());
                zos.closeEntry();
            }

            ZipEntry infoFileEntry = new ZipEntry(ARCHIVE_INFO_FILE_NAME);
            zos.putNextEntry(infoFileEntry);
            zos.write(RestUtils.GSON.toJson(archiveInfo).getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        } finally {
            zos.close();
        }
        return zos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Archive archive = (Archive) o;
        return Objects.equal(dataFiles, archive.dataFiles)
                && Objects.equal(archiveInfo, archive.archiveInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dataFiles, archiveInfo);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("dataFiles", dataFiles)
                .add("archiveInfo", archiveInfo)
                .toString();
    }

    public static class Builder {
        private List<ArchiveFile> files = Lists.newArrayList();
        private ArchiveInfo archiveInfo = new ArchiveInfo();

        public Builder withAppVersionName(String appVersionName) {
            checkNotNull(appVersionName);

            archiveInfo.appVersion = appVersionName;
            return this;
        }

        public Builder withPhoneInfo(String phoneInfo) {
            checkNotNull(phoneInfo);

            archiveInfo.phoneInfo = phoneInfo;
            return this;
        }

        public Builder addDataFile(ArchiveFile entry) {
            checkNotNull(entry);

            files.add(entry);
            return this;
        }

        public Archive build() {
            checkState(archiveInfo.isValid(), "archive info is invalid");

            archiveInfo.files = Lists.newArrayList();
            for (ArchiveFile file : files) {
                archiveInfo.files.add(new ArchiveInfo.FileInfo(file.getFilename(), file.getEndDate()));
            }

            return new Archive(files, archiveInfo);
        }

        private Builder() {
        }

        public static Builder forActivity(String item) {
            checkNotNull(item);

            Builder builder = new Builder();
            builder.archiveInfo.item = item;
            return builder;
        }

        public static Builder forActivity(String item, int schemaRevision) {
            checkNotNull(item);

            Builder builder = new Builder();
            builder.archiveInfo.item = item;
            builder.archiveInfo.schemaRevision = schemaRevision;
            return builder;
        }

        public static Builder forSurvey(String surveyGuid, DateTime surveyCreatedOn) {
            checkNotNull(surveyGuid);
            checkNotNull(surveyCreatedOn);

            Builder builder = new Builder();
            builder.archiveInfo.surveyGuid = surveyGuid;
            builder.archiveInfo.surveyCreatedOn = surveyCreatedOn;
            return builder;
        }

    }

}