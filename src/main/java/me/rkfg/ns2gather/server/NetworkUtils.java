package me.rkfg.ns2gather.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

public class NetworkUtils {

    public static HttpClient getHTTPClient(String host, int port, String username, String password) {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(host, port), new UsernamePasswordCredentials(username, password));
        return getHTTPClientBuilder().setDefaultCredentialsProvider(credsProvider).build();
    }

    public static HttpClient getHTTPClient() {
        return getHTTPClientBuilder().build();
    }

    public static HttpClientBuilder getHTTPClientBuilder() {
        RequestConfig config = RequestConfig.custom().setConnectionRequestTimeout(Settings.TIMEOUT).setConnectTimeout(Settings.TIMEOUT)
                .setSocketTimeout(Settings.TIMEOUT).build();
        HttpClientBuilder builder = HttpClientBuilder.create().setDefaultRequestConfig(config)
                .setUserAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.63 Safari/537.36");
        return builder;
    }

    public static String readStream(InputStream stream) throws IOException {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }

}
