/**
 * Clasa principală a aplicației Spring Boot pentru gestionarea spectacolelor de stand-up.
 *
 * @author Necula Mihai
 * @version 12 ianuarie 2026
 */
package com.standup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CompanieStandUpApplication {
    public static void main(String[] args) {
        SpringApplication.run(CompanieStandUpApplication.class, args);
    }
}
