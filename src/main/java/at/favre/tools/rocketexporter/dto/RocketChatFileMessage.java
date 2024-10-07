package at.favre.tools.rocketexporter.dto;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.Map;

public class RocketChatFileMessage {
    public String _id;
    public String name;
    public String type;
    public String rid;
    public String userId;
    public String uploadedAt;
    public String url;
    public RocketChatMessageWrapperDto.Message.User user;
    public String description;

    public void download(String path, Map<String, String> headers) throws IOException {
        String furl = MessageFormat.format("https://chat.peykasa.ir/file-upload/{0}/{1}", _id, name);
        URLConnection urlConnection = new URL(furl).openConnection();
        for (Map.Entry<String, String> kv : headers.entrySet()) {
            urlConnection.addRequestProperty(kv.getKey(), kv.getValue());
        }
        InputStream in = urlConnection.getInputStream();
        Files.copy(in, Paths.get(path, name), StandardCopyOption.REPLACE_EXISTING);
    }
}
