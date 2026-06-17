package de.kopfzentrum.gam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAutoConfiguration;

@SpringBootApplication(exclude = GroovyTemplateAutoConfiguration.class)
public class GamStandaloneApplication {
  public static void main(String[] args) { SpringApplication.run(GamStandaloneApplication.class, args); }
}
