package de.gesellix.docker.client.ssl

import javax.net.ssl.*
import java.security.KeyStore

import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm

class SslSocketConfigFactory {

    DockerSslSocket createDockerSslSocket(String certPath) {
        def keyStore = createKeyStore(certPath)
        KeyManagerFactory keyManagerFactory = initKeyManagerFactory(keyStore)
        TrustManagerFactory tmf = initTrustManagerFactory(keyStore)
        X509TrustManager trustManager = getUniqueX509TrustManager(tmf)
        SSLContext sslContext = initSslContext(keyManagerFactory, trustManager)
        return new DockerSslSocket(sslSocketFactory: sslContext.socketFactory, trustManager: trustManager)
    }

    private SSLContext initSslContext(KeyManagerFactory keyManagerFactory, X509TrustManager trustManager) {
        def sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagerFactory.keyManagers, [trustManager] as TrustManager[], null)
        sslContext
    }

    private X509TrustManager getUniqueX509TrustManager(TrustManagerFactory trustManagerFactory) {
        def trustManagers = trustManagerFactory.trustManagers
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers: ${trustManagers}")
        }
        (X509TrustManager) trustManagers[0]
    }

    private TrustManagerFactory initTrustManagerFactory(KeyStore keyStore) {
        def trustManagerFactory = TrustManagerFactory.getInstance(getDefaultAlgorithm() as String)
        trustManagerFactory.init(keyStore)
        trustManagerFactory
    }

    private KeyManagerFactory initKeyManagerFactory(KeyStore keyStore) {
        def keyManagerFactory = KeyManagerFactory.getInstance(getDefaultAlgorithm() as String)
        keyManagerFactory.init(keyStore, KeyStoreUtil.KEY_STORE_PASSWORD as char[])
        keyManagerFactory
    }

    private KeyStore createKeyStore(dockerCertPath) {
        KeyStoreUtil.createDockerKeyStore(new File(dockerCertPath as String).absolutePath)
    }
}
