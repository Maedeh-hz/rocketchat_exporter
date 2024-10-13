package at.favre.tools.rocketexporter.converter;

import at.favre.tools.rocketexporter.model.Message;
import at.favre.tools.rocketexporter.util.DateUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class SlackCsvFormat implements ExportFormat {
    @Override
    public void export(List<Message> messages, OutputStream outputStream) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

            for (Message normalizedMessage : messages) {

                String message = normalizedMessage.getMessage();
                message = message == null ? "" : message.replaceAll("\"", "\\\\\"");
                writer.write("\"" + normalizedMessage.getTimestamp().getEpochSecond() + "\"," +
                        "\"" + normalizedMessage.getChannel() + "\"," +
                        "\"" + normalizedMessage.getUsername() + "\"," +
                        "\"" + message + "\"" +
                        "\n");
            }
            writer.flush();
            writer.close();

        } catch (IOException e) {
            throw new IllegalStateException("could not write to stream", e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ex) {
                throw new IllegalStateException("could not close", ex);
            }
        }
    }

    @Override
    public void export(List<Message> messages, OutputStream outputStream, File file, Map<String, String> headers) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            for (Message normalizedMessage : messages) {
                if (normalizedMessage.getFileMessage() != null) {
                    normalizedMessage.getFileMessage().download(file.getPath(), headers);
                }
                String message = normalizedMessage.getMessage();
                String persianDate = DateUtil.toPersianDate(new Date(normalizedMessage.getTimestamp().toEpochMilli()), "yyyy-MM-dd HH:mm:ss");
                message = message == null ? "" : message.replaceAll("\"", "\\\\\"");
                writer.write("\"" + persianDate + "\"," +
                        "\"" + normalizedMessage.getChannel() + "\"," +
                        "\"" + normalizedMessage.getUsername() + "\"," +
                        "\"" + message + "\"" +
                        "\n");
            }
            writer.flush();
            writer.close();

        } catch (IOException e) {
            throw new IllegalStateException("could not write to stream", e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ex) {
                throw new IllegalStateException("could not close", ex);
            }
        }
    }

    @Override
    public String fileExtension() {
        return "csv";
    }
}
