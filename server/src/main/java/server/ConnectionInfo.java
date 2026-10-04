package server;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

/** Browser connection URLs for the server's active local interfaces. */
public final class ConnectionInfo {
    private ConnectionInfo() {}

    public static void print(int port) {
        try {
            System.out.println(describe(port, localAddresses()));
        } catch (SocketException | SecurityException e) {
            System.out.println(describe(port, List.of()));
            System.out.println("Could not inspect network interfaces. Use this computer's LAN IP and port " + port + ".");
        }
    }

    private static List<Address> localAddresses() throws SocketException {
        List<Address> addresses = new ArrayList<>();
        var interfaces = NetworkInterface.getNetworkInterfaces();
        if (interfaces == null) {
            return addresses;
        }
        for (var network : Collections.list(interfaces)) {
            if (!network.isUp() || network.isLoopback()) {
                continue;
            }
            for (var address : Collections.list(network.getInetAddresses())) {
                addresses.add(new Address(network.getName(), address));
            }
        }
        return addresses;
    }

    static String describe(int port, List<Address> addresses) {
        // URLs are sorted and deduplicated, with IPv4 before bracketed IPv6.
        var urls = new TreeMap<String, String>();
        for (var candidate : addresses) {
            InetAddress address = candidate.ip();
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isMulticastAddress()) {
                continue;
            }
            // IPv6 link-local URLs need the connecting device's interface scope,
            // which cannot be inferred from this server's interface name.
            if (address instanceof Inet6Address && address.isLinkLocalAddress()) {
                continue;
            }
            String host = address.getHostAddress();
            if (address instanceof Inet6Address) {
                host = "[" + host.split("%", 2)[0] + "]";
            }
            urls.putIfAbsent("http://" + host + ":" + port + "/", candidate.network());
        }
        var message = new StringBuilder("\nChess server is ready on port ").append(port)
                .append(".\n\nOn this computer:\n  http://localhost:").append(port).append("/\n")
                .append("\nFrom a phone or another device on your local network:\n");
        if (urls.isEmpty()) {
            message.append("  No usable LAN addresses detected. Connect to Wi-Fi or Ethernet, then restart the server.\n");
        } else {
            urls.forEach((url, network) -> message.append("  ").append(url).append("  (").append(network).append(")\n"));
            message.append("\nUse the address for the network your device is connected to.\n");
        }
        return message.append("If a connection fails, check that the server firewall allows TCP port ")
                .append(port).append(".\n").toString();
    }

    record Address(String network, InetAddress ip) {}
}
