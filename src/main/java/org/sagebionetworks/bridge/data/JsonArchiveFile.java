package org.sagebionetworks.bridge.data;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.io.ByteSource;
import org.joda.time.DateTime;

import org.sagebionetworks.bridge.rest.RestUtils;

public class JsonArchiveFile implements ArchiveFile {
    private final String filename;
    private final DateTime endDate;
    private final String json;

    public JsonArchiveFile(String filename, DateTime endDate, String json) {
        this.filename = filename;
        this.endDate = endDate;
        this.json = json;
    }

    public JsonArchiveFile(String filename, DateTime endDate, Object object) {
        this(filename, endDate, RestUtils.GSON.toJson(object));
    }

    public JsonArchiveFile(String filename, DateTime endDate, Object object, Type objectTYpe) {
        this(filename, endDate, RestUtils.GSON.toJson(object, objectTYpe));
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public DateTime getEndDate() {
        return endDate;
    }

    @Override
    public ByteSource getByteSource() {
        return ByteSource.wrap(json.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JsonArchiveFile that = (JsonArchiveFile) o;
        return Objects.equal(filename, that.filename) &&
                Objects.equal(endDate, that.endDate) &&
                Objects.equal(json, that.json);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(filename, endDate, json);
    }

    @Override public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("filename", filename)
                .add("endDate", endDate)
                .add("json", json)
                .toString();
    }
}