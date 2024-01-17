package org.churk.telegrampibot.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "daily_message")
@ToString(exclude = "sentences")
public class DailyMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID dailyMessageId;

    @Column(length = 1000)
    private String keyName;

    @OneToMany(mappedBy = "dailyMessage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Sentence> sentences = new ArrayList<>();
}
