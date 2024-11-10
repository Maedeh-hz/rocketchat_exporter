package at.favre.tools.rocketexporter.dto;

import at.favre.tools.rocketexporter.ConfigSingleton;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

    // Mime type to file extension mapping (a small subset, can be expanded as needed)
    private static final Map<String, String> mimeToExtensionMap = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "application/pdf", "pdf",
            "text/plain", "txt",
            "application/zip", "zip",
            "application/msword", "doc",
            "application/vnd.ms-excel", "xls",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"
    );

    // Download method with MIME type handling and unique file naming
    public synchronized void download(String path, Map<String, String> headers) throws IOException, InterruptedException {
        // Construct the file URL and ensure encoding of the file name part
        String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
        String furl = String.format("%s/file-upload/%s/%s", ConfigSingleton.getInstance(null).getHost(), _id, encodedName);

        // Create the HttpClient with automatic redirect handling
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS) // Ensure redirects are followed
                .build();

        // Create the request builder
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(furl));

        // Add headers to the request
        for (Map.Entry<String, String> header : headers.entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }

        // Send the request and get the response
        HttpRequest request = requestBuilder.build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        // Check the status code and log if needed
        if (response.statusCode() != 200) {
            // If a redirect happens (status code 301 or 302), log the final URL and status code
            throw new IOException("Failed to download file, HTTP status code: " + response.statusCode());
        }

        // Determine the file extension
        String fileExtension = null;
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        if (!contentType.isEmpty()) {
            fileExtension = mimeToExtensionMap.get(contentType.split(";")[0].trim()); // Get the MIME type (ignore any charset)
        }

        // If the file name does not have an extension, add the MIME type extension
        if (fileExtension != null && !name.contains(".")) {
            name = name + "." + fileExtension;
        }

        // Sanitize the filename by replacing non-alphanumeric characters (except period) with hyphens
        String sanitizedFileName = name.replaceAll("[^a-zA-Z0-9.]", "-");

        // Define the file path where the file will be saved
        Path destinationDir = Path.of(path, "files");
        Path destinationPath = destinationDir.resolve(sanitizedFileName);

        // Create the directory if it doesn't exist
        if (Files.notExists(destinationDir)) {
            Files.createDirectories(destinationDir);
        }

        // Ensure the file name is unique by checking if the file exists and renaming it
        Path uniqueFilePath = ensureUniqueFilePath(destinationPath);

        // Copy the content to the destination file
        try (InputStream in = response.body()) {
            Files.copy(in, uniqueFilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IOException("Error writing the file to disk", e);
        }
    }

    // Helper method to ensure the file path is unique
    private Path ensureUniqueFilePath(Path destinationPath) throws IOException {
        Path uniqueFilePath = destinationPath;
        int counter = 1;

        // Check if the file exists and modify the name if necessary
        while (Files.exists(uniqueFilePath)) {
            String fileNameWithoutExtension = uniqueFilePath.getFileName().toString().replaceFirst("[.][^.]+$", "");
            String extension = getFileExtension(uniqueFilePath);

            // Create a new file name with a counter
            String newFileName = fileNameWithoutExtension + "_" + counter + extension;
            uniqueFilePath = destinationPath.getParent().resolve(newFileName);
            counter++;
        }

        return uniqueFilePath;
    }

    // Helper method to get the file extension
    private String getFileExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf(".");
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex); // Return the extension, or an empty string if none exists
    }
}
