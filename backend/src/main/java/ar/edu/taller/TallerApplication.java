package ar.edu.taller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de entrada del backend "Taller de reparaciones".
 *
 * El taller es el único usuario del sistema: no hay autenticación, usuarios ni roles.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class TallerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TallerApplication.class, args);
    }
}
