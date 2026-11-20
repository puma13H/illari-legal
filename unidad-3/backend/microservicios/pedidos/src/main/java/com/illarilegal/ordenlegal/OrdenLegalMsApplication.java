package com.illarilegal.ordenlegal;

import com.illarilegal.compartido.observabilidad.TraceIdFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@org.springframework.cloud.openfeign.EnableFeignClients
@org.springframework.scheduling.annotation.EnableScheduling
@Import(TraceIdFilter.class)
@SpringBootApplication
public class OrdenLegalMsApplication {
  public static void main(String[] args) {
    SpringApplication.run(OrdenLegalMsApplication.class, args);
  }
}
