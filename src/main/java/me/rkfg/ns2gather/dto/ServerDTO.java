package me.rkfg.ns2gather.dto;

public class ServerDTO extends CheckedDTO {
    String ip;
    String password;

    public ServerDTO(Long id, String name, String ip, String password) {
        super();
        this.id = id;
        this.name = name;
        this.ip = ip;
        this.password = password;
    }

    public ServerDTO() {
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
