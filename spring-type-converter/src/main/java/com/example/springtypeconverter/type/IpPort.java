package com.example.springtypeconverter.type;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class IpPort {

    private String ip;
    private int port;

    public IpPort(String id, int port) {
        this.ip = id;
        this.port = port;
    }
}
