package br.com.ecofy.ms_categorization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "br.com.ecofy.ms_categorization.adapters.out.persistence.repository")
@EntityScan(basePackages = "br.com.ecofy.ms_categorization.adapters.out.persistence.entity")
public class MsCategorizationApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsCategorizationApplication.class, args);
	}

}
