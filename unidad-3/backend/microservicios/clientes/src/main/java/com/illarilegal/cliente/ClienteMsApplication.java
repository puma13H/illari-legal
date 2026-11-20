package com.illarilegal.cliente;

import com.illarilegal.compartido.observabilidad.TraceIdFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import(TraceIdFilter.class)
@SpringBootApplication
public class ClienteMsApplication {
  public static void main(String[] args) {
    SpringApplication.run(ClienteMsApplication.class, args);
  }
}
