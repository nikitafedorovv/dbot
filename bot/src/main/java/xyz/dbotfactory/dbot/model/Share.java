package xyz.dbotfactory.dbot.model;

import lombok.*;

import javax.persistence.*;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Share {
    @Id
    @GeneratedValue
    private int id;

    private long telegramUserId;

    private Double share;
}
