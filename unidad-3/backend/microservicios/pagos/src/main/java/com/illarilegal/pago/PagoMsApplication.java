package com.illarilegal.pago;

import com.illarilegal.compartido.observabilidad.TraceIdFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@org.springframework.scheduling.annotation.EnableScheduling
@Import(TraceIdFilter.class)
@SpringBootApplication
public class PagoMsApplication {
  public static void main(String[] args) {
    SpringApplication.run(PagoMsApplication.class, args);
  }
}
