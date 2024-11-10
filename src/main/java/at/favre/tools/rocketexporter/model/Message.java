package at.favre.tools.rocketexporter.model;

import at.favre.tools.rocketexporter.dto.RocketChatFileMessage;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Data
@RequiredArgsConstructor
public class Message {
    private  String message;
    private  String username;
    private  String channel;
    private  Instant timestamp;

    public Message(String message, String username, String channel, Instant timestamp) {
        this.message = message;
        this.username = username;
        this.channel = channel;
        this.timestamp = timestamp;
    }

    private RocketChatFileMessage fileMessage;
}

