package at.favre.tools.rocketexporter.dto;

import java.util.ArrayList;

public class RocketChatFileMessageWrapperDto {
    public ArrayList<RocketChatFileMessage> files;
    public int count;
    public int offset;
    public int total;
    public boolean success;
}
