package com.chess.chessapi.entities;

import com.fasterxml.jackson.annotation.*;

import javax.persistence.*;

@Entity
@Table(name = "certificates")
@JsonIdentityInfo(generator= ObjectIdGenerators.PropertyGenerator.class, property="id",scope = Certificates.class)
public class Certificates {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String certificate_link;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCetificateLink() {
        return certificate_link;
    }

    public void setCetificateLink(String cetificateLink) {
        this.certificate_link = cetificateLink;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}