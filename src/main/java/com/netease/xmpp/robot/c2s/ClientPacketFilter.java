package com.netease.xmpp.robot.c2s;

import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

public class ClientPacketFilter implements PacketFilter {

    @Override
    public boolean accept(Packet packet) {
        return packet instanceof Message;
    }
}
