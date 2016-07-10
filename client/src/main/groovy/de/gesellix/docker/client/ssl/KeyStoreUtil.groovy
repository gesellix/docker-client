package de.gesellix.docker.client.ssl

import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser

import java.security.*
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec

/**
 * A slightly modified copy from https://github.com/rhuss/docker-maven-plugin
 * with kind permission of Roland Huß (https://twitter.com/ro14nd).
 */
public class KeyStoreUtil {

    def static KEY_STORE_PASSWORD = "docker".toCharArray()

    def static KeyStore createDockerKeyStore(String certPath) throws IOException, GeneralSecurityException {
        PrivateKey privKey = loadPrivateKey(new File(certPath, "key.pem").absolutePath)
        Certificate[] certs = loadCertificates(new File(certPath, "cert.pem").absolutePath)

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load((KeyStore.LoadStoreParameter) null)

        keyStore.setKeyEntry("docker", privKey, KEY_STORE_PASSWORD, certs)
        addCA(keyStore, new File(certPath, "ca.pem").absolutePath)
        return keyStore
    }

    static PrivateKey loadPrivateKey(String keyPath) throws IOException, GeneralSecurityException {
        PEMKeyPair keyPair = loadPEM(keyPath)
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyPair.getPrivateKeyInfo().getEncoded())
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec)
    }

    static <T> T loadPEM(String keyPath) throws IOException {
        PEMParser parser = new PEMParser(new BufferedReader(new FileReader(keyPath)))
        return (T) parser.readObject()
    }

    static void addCA(KeyStore keyStore, String caPath) throws KeyStoreException, FileNotFoundException, CertificateException {
        loadCertificates(caPath).each { cert ->
            X509Certificate crt = (X509Certificate) cert
            String alias = crt.subjectX500Principal.name
            keyStore.setCertificateEntry(alias, crt)
        }
    }

    static Collection<Certificate> loadCertificates(String certPath) throws FileNotFoundException, CertificateException {
        InputStream is = new FileInputStream(certPath)
        return CertificateFactory.getInstance("X509").generateCertificates(is)
    }
}
