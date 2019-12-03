package de.gesellix.docker.testutil

// https://docs.oracle.com/javase/tutorial/networking/nifs/listing.html
class NetworkInterfaces {

    static void main(String[] args) {
        println new NetworkInterfaces().getFirstInet4Address()
    }

    String getFirstInet4Address() {
        def interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        return interfaces.collect { getInet4Addresses(it) }.flatten().grep().first()
    }

    static def getInet4Addresses(NetworkInterface netint) throws SocketException {
        def addresses = Collections.list(netint.getInetAddresses())
        return addresses.findAll {
            it instanceof Inet4Address && !it.isLoopbackAddress()
        }.collect {
            it.hostAddress
        }
    }
}
