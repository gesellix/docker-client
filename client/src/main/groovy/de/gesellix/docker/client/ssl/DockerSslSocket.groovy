package de.gesellix.docker.client.ssl

import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

class DockerSslSocket {

    SSLSocketFactory sslSocketFactory
    X509TrustManager trustManager
}
