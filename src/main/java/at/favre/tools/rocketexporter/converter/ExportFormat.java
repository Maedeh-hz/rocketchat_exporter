package at.favre.tools.rocketexporter.converter;

import at.favre.tools.rocketexporter.model.Message;

import java.io.File;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface ExportFormat {
    /**
     * Export given messages to provided stream
     *
     * @param messages     to export
     * @param outputStream to write to
     */
    void export(List<Message> messages, OutputStream outputStream);

    void export(List<Message> messages, OutputStream outputStream, File file, Map<String, String> headers);

    /**
     * Type of file extension, e.g. 'csv' or 'json'
     *
     * @return extension
     */
    String fileExtension();
}
