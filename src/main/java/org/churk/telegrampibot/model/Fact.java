package org.churk.telegrampibot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Entity(name = "facts")
@NoArgsConstructor
public class Fact {
    @Id
    private UUID factId;
    private String comment;
    private Double isHate;
}
