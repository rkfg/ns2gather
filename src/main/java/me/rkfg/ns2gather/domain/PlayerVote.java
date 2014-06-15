package me.rkfg.ns2gather.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import ru.ppsrk.gwt.domain.BasicDomain;

@Entity
public class PlayerVote extends BasicDomain {
    Long steamId;
    @OneToMany(mappedBy = "player")
    @Cascade(CascadeType.ALL)
    Set<Vote> votes = new HashSet<>();

    public PlayerVote(Long steamId) {
        super();
        this.steamId = steamId;
    }

    public PlayerVote() {
    }

    public Long getSteamId() {
        return steamId;
    }

    public void setSteamId(Long steamId) {
        this.steamId = steamId;
    }

    public Set<Vote> getVotes() {
        return votes;
    }

    public void setVotes(Set<Vote> votes) {
        this.votes = votes;
    }

}
