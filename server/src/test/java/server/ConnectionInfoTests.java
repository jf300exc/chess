package server;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionInfoTests {
    private ConnectionInfo.Address address(String network, String ip) throws Exception {
        return new ConnectionInfo.Address(network, InetAddress.getByName(ip));
    }

    @Test
    void listsEveryLocalNetworkWithTheListeningPort() throws Exception {
        String message = ConnectionInfo.describe(9090, List.of(
                address("wifi", "192.168.1.20"), address("ethernet", "10.10.0.8")));
        assertTrue(message.contains("http://localhost:9090/"));
        assertTrue(message.contains("http://192.168.1.20:9090/  (wifi)"));
        assertTrue(message.contains("http://10.10.0.8:9090/  (ethernet)"));
        assertFalse(message.contains(":8080/"));
    }

    @Test
    void excludesAddressesThatAnotherDeviceCannotUse() throws Exception {
        String message = ConnectionInfo.describe(8080, List.of(
                address("loopback", "127.0.0.1"), address("loopback", "::1"),
                address("wildcard", "0.0.0.0"), address("wildcard", "::"),
                address("wifi", "fe80::1234"), address("multicast", "224.0.0.1")));
        assertTrue(message.contains("No usable LAN addresses detected"));
        assertFalse(message.contains("http://127.0.0.1"));
        assertFalse(message.contains("http://["));
        assertFalse(message.contains("http://0.0.0.0"));
        assertFalse(message.contains("224.0.0.1"));
    }

    @Test
    void formatsIpv6AndDeduplicatesUrls() throws Exception {
        var ip = address("wifi", "fd12:3456::20");
        String message = ConnectionInfo.describe(9090, List.of(ip, ip));
        String url = "http://[fd12:3456:0:0:0:0:0:20]:9090/";
        assertTrue(message.contains(url));
        assertEquals(message.indexOf(url), message.lastIndexOf(url));
    }
}
