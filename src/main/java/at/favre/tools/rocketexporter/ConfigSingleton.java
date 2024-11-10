package at.favre.tools.rocketexporter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@Getter
@Setter
public class ConfigSingleton {
    // Singleton instance
    private static ConfigSingleton instance;

    // Config values
    private String host;
    private String userId;
    private String password;
    private File file;

    // Private constructor to prevent instantiation
    private ConfigSingleton(String config) {
        // Load the config during initialization
        try {
            loadConfig(config);
        } catch (IOException e) {
            e.printStackTrace();
            // Handle exception as needed (maybe throw a RuntimeException)
        }
    }

    // Method to get the singleton instance
    public static ConfigSingleton getInstance(String config) {
        if (instance == null) {
            synchronized (ConfigSingleton.class) {
                if (instance == null) {
                    instance = new ConfigSingleton(config);
                }
            }
        }
        return instance;
    }

    // Method to load the config from a file
    private void loadConfig(String config) throws IOException {
        Gson gson = new GsonBuilder().create();
        FileReader fileReader = new FileReader(config);  // Assuming the config file is named config.json
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        JsonObject myConf = gson.fromJson(bufferedReader, JsonObject.class);

        if (host == null) {
            host = myConf.get("host").getAsString();
        }
        if (userId == null) {
            userId = myConf.get("user_id").getAsString();
        }
        password = myConf.get("token").getAsString();
        if (file == null) {
            file = new File(myConf.get("output_dir").getAsString());
        }
    }

    // Getter methods for the config values
    public String getHost() {
        return host;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public File getFile() {
        return file;
    }

    // Optionally, you can add setter methods if you need to modify these values after loading
}
