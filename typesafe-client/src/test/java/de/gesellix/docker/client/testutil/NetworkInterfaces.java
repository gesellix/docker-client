package de.gesellix.docker.client.testutil;

import de.gesellix.docker.engine.client.infrastructure.LoggingExtensionsKt;
import org.slf4j.Logger;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// https://docs.oracle.com/javase/tutorial/networking/nifs/listing.html
public class NetworkInterfaces {

  private static final Logger log = LoggingExtensionsKt.logger(NetworkInterfaces.class.getName()).getValue();

  public static void main(String[] args) throws SocketException {
    log.info(new NetworkInterfaces().getFirstInet4Address());
  }

  public String getFirstInet4Address() throws SocketException {
    return getInet4Addresses().stream().findFirst().get();
  }

  public List<String> getInet4Addresses() throws SocketException {
    List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
    return interfaces.stream()
        .flatMap((i) -> getInet4Addresses(i))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public static Stream<String> getInet4Addresses(NetworkInterface netint) {
    List<InetAddress> addresses = Collections.list(netint.getInetAddresses());
    return addresses.stream()
        .filter((it) -> it instanceof Inet4Address && !it.isLoopbackAddress())
        .map((it) -> it.getHostAddress());
  }
}
