package de.gesellix.docker.testutil

class SwarmUtil {

  String getAdvertiseAddr(){
    return "127.0.0.1"
//    return new NetworkInterfaces().getFirstInet4Address()
  }

  String getListenAddr(){
    return "0.0.0.0"
  }
}
