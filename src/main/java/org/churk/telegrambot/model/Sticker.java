package org.churk.telegrambot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.UUID;

@Data
@Entity(name = "stickers")
public class Sticker {
    @Id
    private UUID stickerId;
    private String fileId;
    private String setName;
    private Boolean isAnimated;
    private Boolean isVideo;
    private String emoji;
    private Integer fileSize;
}
