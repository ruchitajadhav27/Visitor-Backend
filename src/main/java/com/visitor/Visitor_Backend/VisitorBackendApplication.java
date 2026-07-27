package com.visitor.Visitor_Backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VisitorBackendApplication {

	public static void main(String[] args) {
	    // This line forces Spring to use Atlas and ignores the properties file entirely
	    System.setProperty("spring.data.mongodb.uri", "mongodb+srv://jadhavruchita27_db_user:ruchi@cluster0.ajdye1r.mongodb.net/visitorsdb?retryWrites=true&w=majority&appName=Cluster0");
	    
	    SpringApplication.run(VisitorBackendApplication.class, args);
	}

}
