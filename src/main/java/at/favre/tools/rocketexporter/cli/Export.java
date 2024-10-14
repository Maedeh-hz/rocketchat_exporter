package at.favre.tools.rocketexporter.cli;

import at.favre.tools.rocketexporter.Config;
import at.favre.tools.rocketexporter.RocketExporter;
import at.favre.tools.rocketexporter.TooManyRequestException;
import at.favre.tools.rocketexporter.converter.ExportFormat;
import at.favre.tools.rocketexporter.converter.SlackCsvFormat;
import at.favre.tools.rocketexporter.dto.Conversation;
import at.favre.tools.rocketexporter.dto.LoginDto;
import at.favre.tools.rocketexporter.dto.TokenDto;
import at.favre.tools.rocketexporter.model.Message;
import lombok.SneakyThrows;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@CommandLine.Command(description = "Exports rocket chat messages from a specific group/channel.",
        name = "export", mixinStandardHelpOptions = true, version = "1.0")
class Export implements Runnable {

    @CommandLine.Option(names = {"-o", "--outFile"}, description = "The file or directory to write the export data to. Will write to current directory with auto generated filename if this arg is omitted. If you want to export multiple conversations you must pass a directory not a file.")
    private File file;

    @CommandLine.Option(names = {"-t", "--host"}, required = false, description = "The rocket chat server. E.g. 'https://myserver.com'")
    private String host;
    @CommandLine.Option(names = {"-c", "--config"}, required = true, description = "The rocket chat exporter config file. E.g. './config.cnf'")
    private File configFile;
    @CommandLine.Option(names = {"-u", "--user"}, required = false, description = "RocketChat username for authentication.")
    private String username;

    @CommandLine.Option(names = {"-k", "--user-id"}, required = false, description = "RocketChat Personal Access Token user ID.")
    private String userId;

    @CommandLine.Option(names = {"--debug"}, description = "Add debug log output to STDOUT.")
    private boolean debug;

    @CommandLine.Option(names = {"-m", "--maxMsg"}, description = "How many messages should be exported.")
    private int maxMessages = 50000;

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.out.println("Text in UTF-8");
        int exitCode = new CommandLine(new Export())
                .setCaseInsensitiveEnumValuesAllowed(true).execute(args);
        System.exit(exitCode);
    }

    @SneakyThrows
    @Override
    public void run() {
        PrintStream out = System.out;

        FileReader fileReader = new FileReader(configFile);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        host = bufferedReader.readLine();
        userId = bufferedReader.readLine();
        String password = bufferedReader.readLine();
        file = new File(bufferedReader.readLine());

        System.out.println("host = " + host);
        System.out.println("userId = " + userId);
        System.out.println("password or token = " + password);
        System.out.println("store dir = " + file.getAbsolutePath());
        if (username == null && userId == null) {
            out.println("You have to use a username or a token user ID to continue.");
            System.exit(-1);
        }
        try {
            RocketExporter exporter = RocketExporter.newInstance(
                    Config.builder()
                            .host(URI.create(host))
                            .httpDebugOutput(debug)
                            .build());

            if (username != null && !username.isEmpty()) {
                exporter.login(new LoginDto(username, password));
            } else {
                exporter.tokenAuth(new TokenDto(userId, password));
            }

            out.println("Authentication successful (" + username + " or " + userId + ").");

            CliOptionChooser typeChooser =
                    new CliOptionChooser(System.in, out,
                            List.of(
                                    RocketExporter.ConversationType.GROUP.name,
                                    RocketExporter.ConversationType.CHANNEL.name,
                                    RocketExporter.ConversationType.DIRECT_MESSAGES.name),
                            "\nWhat type do you want to export:");

            ArrayList<Conversation> conversations = new ArrayList<>();
            RocketExporter.ConversationType type = RocketExporter.ConversationType.of(typeChooser.prompt());

            switch (type) {
                case GROUP:
                    conversations.addAll(exporter.listGroups());
                    break;
                case CHANNEL:
                    conversations.addAll(exporter.listChannels());
                    break;
                case DIRECT_MESSAGES:
                    conversations.addAll(exporter.listDirectMessageChannels());
                    break;
                default:
                    throw new IllegalStateException();
            }

            List<Conversation> conversationSelection = new ArrayList<>();
            conversationSelection.add(new Conversation.AllConversations());

            List<Conversation> allConversations = conversations.stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Conversation::getName))
                    .collect(Collectors.toList());

            conversationSelection.addAll(allConversations);

            if (allConversations.isEmpty()) {
                out.println("Nothing found to export.");
                return;
            }

            CliOptionChooser cliOptionChooser =
                    new CliOptionChooser(System.in, out,
                            conversationSelection.stream().map(Conversation::getName).collect(Collectors.toList()),
                            "\nPlease choose the " + type.name + " you want to export:");

            int selection = cliOptionChooser.prompt();
            List<Conversation> toExport = new ArrayList<>();

            if (selection == 0) {
                toExport.addAll(allConversations);
            } else {
                toExport.add(allConversations.get(selection - 1));
            }

            for (int i = 0; i < toExport.size(); i++) {
                Conversation selectedGroup = toExport.get(i);

                final List<Message> messages;
                final ExportFormat format = new SlackCsvFormat();
                final int offset = 0;
                final int maxMsg = maxMessages;

                String directoryName = selectedGroup.getName().replaceAll("[^a-zA-Z0-9]", "-");

                final File outFile = generateOutputFile(file, directoryName, type, format);

                try {
                    switch (type) {
                        case GROUP:
                            messages = exporter.exportPrivateGroupMessages(selectedGroup.getName(), selectedGroup.get_id(), offset, maxMsg, outFile, format);
                            break;
                        case CHANNEL:
                            messages = exporter.exportChannelMessages(selectedGroup.getName(), selectedGroup.get_id(), offset, maxMsg, outFile, format);
                            break;
                        case DIRECT_MESSAGES:
                            messages = exporter.exportDirectMessages(selectedGroup.getName(), selectedGroup.get_id(), offset, maxMsg, outFile, format);
                            break;
                        default:
                            throw new IllegalStateException();
                    }

                    out.println("Successfully exported " + messages.size() + " " + type.name + " messages to '" + outFile + "'");
                } catch (TooManyRequestException e) {
                    out.println("Too many requests. Slowing down...");
                    Thread.sleep(5000);
                    i--;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private File generateOutputFile(File provided, String contextName, RocketExporter.ConversationType type, ExportFormat format) {
        if (provided == null) {
            provided = new File("./");
        }

        if (!provided.exists()) {
            provided.mkdirs();
        }
        File out = new File(provided, contextName);
        if (!out.exists()) {
            out.mkdirs();
        }
        return out;
    }
}
