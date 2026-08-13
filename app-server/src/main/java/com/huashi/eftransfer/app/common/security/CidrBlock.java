package com.huashi.eftransfer.app.common.security;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

record CidrBlock(byte[] network, int prefixLength) {

    static CidrBlock parse(String cidr) {
        if (cidr == null || cidr.isBlank()) {
            throw new IllegalArgumentException("CIDR must not be blank");
        }
        String trimmed = cidr.trim();
        int slash = trimmed.indexOf('/');
        String addressPart = slash < 0 ? trimmed : trimmed.substring(0, slash);
        InetAddress address;
        try {
            address = InetAddress.getByName(addressPart);
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("Invalid CIDR address: " + cidr, exception);
        }
        byte[] network = address.getAddress();
        int maxPrefix = network.length * 8;
        int prefixLength = slash < 0 ? maxPrefix : Integer.parseInt(trimmed.substring(slash + 1).trim());
        if (prefixLength < 0 || prefixLength > maxPrefix) {
            throw new IllegalArgumentException("Invalid CIDR prefix: " + cidr);
        }
        return new CidrBlock(network, prefixLength);
    }

    boolean contains(InetAddress address) {
        byte[] candidate = unwrapMappedIpv4(address.getAddress());
        byte[] networkBytes = unwrapMappedIpv4(network);
        if (candidate.length != networkBytes.length) {
            return false;
        }
        int fullBytes = prefixLength / 8;
        int remainBits = prefixLength % 8;
        for (int index = 0; index < fullBytes; index++) {
            if (candidate[index] != networkBytes[index]) {
                return false;
            }
        }
        if (remainBits == 0) {
            return true;
        }
        int mask = 0xFF << (8 - remainBits);
        return (candidate[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
    }

    private static byte[] unwrapMappedIpv4(byte[] address) {
        if (address.length == 16
                && address[0] == 0 && address[1] == 0 && address[2] == 0 && address[3] == 0
                && address[4] == 0 && address[5] == 0 && address[6] == 0 && address[7] == 0
                && address[8] == 0 && address[9] == 0 && address[10] == (byte) 0xFF && address[11] == (byte) 0xFF) {
            return Arrays.copyOfRange(address, 12, 16);
        }
        return address;
    }
}
