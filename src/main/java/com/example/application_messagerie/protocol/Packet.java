package com.example.application_messagerie.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Packet {

    private PacketType type;
    private String sender;
    private String receiver;
    private String content;
    private String extra;

    private static final ObjectMapper mapper = new ObjectMapper();

    // Constructeurs
    public Packet() {}

    public Packet(PacketType type) {
        this.type = type;
    }

    public Packet(PacketType type, String content) {
        this.type = type;
        this.content = content;
    }

    public Packet(PacketType type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
    }

    public Packet(PacketType type, String sender, String receiver, String content) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
    }

    // Sérialisation JSON → String
    public String toJson() throws Exception {
        return mapper.writeValueAsString(this);
    }

    // Désérialisation String → Packet
    public static Packet fromJson(String json) throws Exception {
        return mapper.readValue(json, Packet.class);
    }

    // Getters et Setters
    public PacketType getType() { return type; }
    public void setType(PacketType type) { this.type = type; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getExtra() { return extra; }
    public void setExtra(String extra) { this.extra = extra; }

    @Override
    public String toString() {
        return "Packet{type=" + type + ", sender=" + sender +
                ", receiver=" + receiver + ", content=" + content + "}";
    }
}
