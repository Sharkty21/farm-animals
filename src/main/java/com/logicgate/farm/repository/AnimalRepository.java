package com.logicgate.farm.repository;

import com.logicgate.farm.domain.Animal;

import com.logicgate.farm.domain.Barn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {

  // additional methods can be defined here

}
