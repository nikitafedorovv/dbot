package xyz.dbotfactory.dbot.model;

import lombok.*;
import xyz.dbotfactory.dbot.model.meta.ChatMetaInfo;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.EnumType.ORDINAL;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chat {

    @Id
    @GeneratedValue
    private int id;

    @Column(unique = true)
    private long telegramChatId;

    @OneToMany(cascade = ALL, fetch = FetchType.EAGER)
    private List<Receipt> receipts = new ArrayList<>();

    @Enumerated(ORDINAL)
    ChatState chatState;

    @OneToOne(cascade = ALL, fetch = FetchType.EAGER)
    private ChatMetaInfo chatMetaInfo;
}
