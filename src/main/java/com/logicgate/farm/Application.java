package com.logicgate.farm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

  public static void main(String[] args) {
    System.out.print("Testing 123");
    SpringApplication.run(Application.class, args);
  }

}
