package com.illarilegal.ordenlegal;

import com.illarilegal.compartido.observabilidad.Correlation;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignTraceConfiguration {
  @Bean
  RequestInterceptor traceIdToCatalogo() {
    return template -> {
      String traceId = Correlation.current();
      if (Correlation.valid(traceId)) template.header("X-Trace-ID", traceId);
    };
  }
}
