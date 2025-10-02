package ru.practicum.service.server;

import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hits")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String app;
    @Column(nullable = false)
    private String uri;
    @Column(nullable = false)
    private String ip;
    @Column(nullable = false)
    private LocalDateTime timestamp;
}
