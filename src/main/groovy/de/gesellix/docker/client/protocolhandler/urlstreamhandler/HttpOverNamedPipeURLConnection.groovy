package de.gesellix.docker.client.protocolhandler.urlstreamhandler

import sun.net.www.http.HttpClient
import sun.net.www.protocol.http.HttpURLConnection

// this class only exists to override the sun internal HttpClient with
// our own named pipe aware implementation
class HttpOverNamedPipeURLConnection extends HttpURLConnection {

    HttpOverNamedPipeURLConnection(URL url) {
        super(url, (Proxy) null)
    }

    protected void setNewClient(URL var1) throws IOException {
        this.setNewClient(var1, false)
    }

    protected void setNewClient(URL var1, boolean var2) throws IOException {
        this.http = new HttpOverNamedPipeClient(var1)
        this.http.setConnectTimeout(this.connectTimeout)
        this.http.setReadTimeout(this.readTimeout)
    }

    protected HttpClient getNewHttpClient(URL var1, Proxy var2, int connectTimeout) throws IOException {
        def client = new HttpOverNamedPipeClient(var1)
        client.setConnectTimeout(connectTimeout)
        return client
    }

    protected HttpClient getNewHttpClient(URL var1, Proxy var2, int connectTimeout, boolean var4) throws IOException {
        return new HttpOverNamedPipeClient(var1)
    }
}
