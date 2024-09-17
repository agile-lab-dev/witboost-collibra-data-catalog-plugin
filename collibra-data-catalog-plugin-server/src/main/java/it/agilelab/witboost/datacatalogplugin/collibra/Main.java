package it.agilelab.witboost.datacatalogplugin.collibra;

import it.agilelab.witboost.datacatalogplugin.collibra.config.JaywayJsonPathConfigurer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** This is the Main class. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class Main {

    /** This is the main method which acts as the entry point inside the application. */
    public static void main(String[] args) {
        JaywayJsonPathConfigurer.configureJaywayJsonPath();
        SpringApplication.run(Main.class, args);
    }
}
