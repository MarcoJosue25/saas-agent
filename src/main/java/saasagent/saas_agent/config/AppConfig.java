package saasagent.saas_agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class AppConfig {

    // Preparamos el http para llamar apis externas, timeouts para evitar hilos colgados
    @Bean
    public RestClient.Builder restClientBuilder() {
        HttpClient clienteJdk = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(clienteJdk);
        fabrica.setReadTimeout(Duration.ofSeconds(60));

        return RestClient.builder().requestFactory(fabrica);
    }
}
