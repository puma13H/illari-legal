package com.illarilegal.serviciolegal;

import com.illarilegal.compartido.observabilidad.TraceIdFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import(TraceIdFilter.class)
@SpringBootApplication
public class ServicioLegalMsApplication {
  public static void main(String[] args) {
    SpringApplication.run(ServicioLegalMsApplication.class, args);
  }
}
