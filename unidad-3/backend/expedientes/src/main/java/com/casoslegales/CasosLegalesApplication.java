package com.casoslegales;

import com.illarilegal.compartido.observabilidad.TraceIdFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import(TraceIdFilter.class)
@SpringBootApplication
public class CasosLegalesApplication {

  public static void main(String[] args) {
    SpringApplication.run(CasosLegalesApplication.class, args);
  }
}
