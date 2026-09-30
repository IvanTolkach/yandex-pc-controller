package dev.tolkach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class GatewayApplication {

    static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        SpringApplication.run(GatewayApplication.class, args);
    }
}
