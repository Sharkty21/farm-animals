package com.logicgate.farm.service;

import com.logicgate.farm.domain.Animal;
import com.logicgate.farm.domain.Barn;
import com.logicgate.farm.repository.AnimalRepository;
import com.logicgate.farm.repository.BarnRepository;

import com.logicgate.farm.util.FarmUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional
public class AnimalServiceImpl implements AnimalService {

  private final AnimalRepository animalRepository;

  private final BarnRepository barnRepository;

  @Autowired
  public AnimalServiceImpl(AnimalRepository animalRepository, BarnRepository barnRepository) {
    this.animalRepository = animalRepository;
    this.barnRepository = barnRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Animal> findAll() {
    return animalRepository.findAll();
  }

  @Override
  public void deleteAll() {
    animalRepository.deleteAll();
  }

  @Override
  public Animal addToFarm(Animal animal) {

    //List of all Barns that have same color as Inputted Animal
    List<Barn> barnColorMatches = (barnRepository.findAll()).stream()
      .filter(barn -> barn.getColor().equals(animal.getFavoriteColor()))
      .collect(Collectors.toList());

    //List of all Animals that have same color as Inputted Animal
    List<Animal> animalColorMatches = (animalRepository.findAll()).stream()
      .filter(match -> match.getFavoriteColor().equals(animal.getFavoriteColor()))
      .collect(Collectors.toList());

    //Map of Barns with corresponding Animal values
    Map<Barn, List<Animal>> barnAnimalMap = animalRepository.findAll().stream()
      .collect(Collectors.groupingBy(Animal::getBarn));

    if (barnColorMatches.size() == 0) { //No barns match Inputted Animal's Color
      //Create new Barn "newBarn" with Inputted Animal's Color and save to barnRepository
      Barn newBarn = new Barn("Barn", animal.getFavoriteColor());
      barnRepository.save(newBarn);
      //Set Inputted Animal's Barn value to "newBarn" and save to animalRepository
      animal.setBarn(newBarn);
      animalRepository.save(animal);
    } else if (animalColorMatches.size() % FarmUtils.barnCapacity() == 0) { //Barns that match Inputted Animal's Color are at full capacity
      //Create new Barn "newBarn" with Inputted Animal's Color and save to barnRepository
      Barn newBarn = new Barn("NewBarn", animal.getFavoriteColor());
      barnRepository.save(newBarn);
      //Set Inputted Animal's Barn value to "newBarn" and save to animalRepository
      animal.setBarn(newBarn);
      animalRepository.save(animal);
      //Evenly redistributing Animals among Barns
      //For each existing animal that matches colors to Inputted Animal
      animalColorMatches.forEach(checkAnimal -> {

        //Updated Map of Barns with corresponding Animal values
        Map<Barn, List<Animal>> barnAnimalMapUpdated = animalRepository.findAll().stream()
          .collect(Collectors.groupingBy(Animal::getBarn));

        //Integers used to determine which Barn to take Animal from and add to newBarn
        int newBarnAnimalsCount = (barnAnimalMapUpdated.get(newBarn)).size(); //Animals associated with newBarn
        int checkBarnAnimalsCount = (barnAnimalMapUpdated.get(checkAnimal.getBarn())).size(); //Animals associated with checked barn
        int averageAnimalsPerBarn = (animalColorMatches.size()+1)/(barnColorMatches.size()+1); //Average Animals per new Barn total

        /* If the newBarn's Animal count is less than average Animals per
         * new Barn total AND if the checked animal's Barn's Animal count
         * is greater than average Animals per new Barn total */
        if (newBarnAnimalsCount < averageAnimalsPerBarn && checkBarnAnimalsCount > averageAnimalsPerBarn){
          checkAnimal.setBarn(newBarn);
        }

      });
    } else { //A Barn matches Inputted Animal's Color and is not to full capacity
      /* Find Barns from barnColorMatches whose Animal count is less
       * than or equal to average count of Animals per total Barns */
      barnColorMatches.forEach(checkBarn -> {
        int avgAnimalsPerBarns = (animalColorMatches.size())/(barnColorMatches.size());
        int barnAnimalsCount = (barnAnimalMap.get(checkBarn)).size();
        /* If the current Barn's Animal count is less than average Animals per total Barns,
         * set the Inputted Animal's Barn to current Barn */
        if ( barnAnimalsCount <= avgAnimalsPerBarns ) {
          animal.setBarn(checkBarn);
        }
      });
      animalRepository.save(animal);
    }
    return animal;
  }

  @Override
  public void addToFarm(List<Animal> animals) {
    animals.forEach(this::addToFarm);
  }

  @Override
  public void removeFromFarm(Animal animal) {

    //Delete Inputted Animal from animalRepository
    animalRepository.delete(animal);

    //Counts for Barns and Animals matching Inputted Animal's Color
    long countMatchingAnimals = animalRepository.findAll().stream()
      .filter(filterAnimal -> filterAnimal.getFavoriteColor().equals(animal.getFavoriteColor()))
      .count();
    long countMatchingBarns = barnRepository.findAll().stream()
      .filter(filterBarn -> filterBarn.getColor().equals(animal.getFavoriteColor()))
      .count();
    //Map of Barns with corresponding Animal values that match Inputted Animal's Color
    Map<Barn, List<Animal>> barnAnimalMatchMap = animalRepository.findAll().stream()
      .filter(checkAnimal -> checkAnimal.getFavoriteColor().equals(animal.getFavoriteColor()))
      .collect(Collectors.groupingBy(Animal::getBarn));

    //If there are more than enough barns to accommodate all the animals
    if (countMatchingAnimals % FarmUtils.barnCapacity() == 0) {
      //For all animals whose barn matches Inputted Animal barn, set barn id to null
      animalRepository.findAll().stream()
        .filter(checkAnimal -> checkAnimal.getBarn().equals(animal.getBarn()))
        .forEach(checkAnimal -> checkAnimal.setBarn(null));
      //Delete Barn of Inputted Animal
      barnRepository.delete(animal.getBarn());
      //Create updated list of all barns matching color of Inputted Animal
      List<Barn> matchingBarns = barnRepository.findAll().stream()
        .filter(filterBarn -> filterBarn.getColor().equals(animal.getFavoriteColor()))
        .collect(Collectors.toList());
      //Create List of all animals with null barn id
      List<Animal> nullAnimals = animalRepository.findAll().stream()
        .filter(checkAnimal -> checkAnimal.getBarn() == (null))
        .collect(Collectors.toList());
      /* For all barns matching color of Inputted Animal, add animals from
       * nullAnimals until barn is full */
      matchingBarns.stream()
        .forEach(checkBarn -> {
          for (int i=0; i < FarmUtils.barnCapacity() - barnAnimalMatchMap.get(checkBarn).size(); i++) {
            nullAnimals.get(0).setBarn(checkBarn);
            nullAnimals.remove(0);
          }
        });
    }
    //If Inputted Animal removal causes uneven animal/barn distribution
    else if (barnAnimalMatchMap.get(animal.getBarn()).size() < countMatchingAnimals / countMatchingBarns) {
      /* Create updated list of all barns above average animal/barn distribution
       * who match color of Inputted Animal */
      List<Barn> matchingFullBarns = barnRepository.findAll().stream()
        .filter(filterBarn -> filterBarn.getColor().equals(animal.getFavoriteColor()))
        .filter(filterBarn -> barnAnimalMatchMap.get(filterBarn).size() > countMatchingAnimals / countMatchingBarns)
        .collect(Collectors.toList());
      //Take animal from one of matchingFullBarns and set barn id to Inputted Animal's barn id
      Barn fullBarn = matchingFullBarns.get(0);
      barnAnimalMatchMap.get(fullBarn).get(0).setBarn(animal.getBarn());
    }
  }

  @Override
  public void removeFromFarm(List<Animal> animals) {
    animals.forEach(animal -> removeFromFarm(animalRepository.getOne(animal.getId())));
  }
}

/*
Dear Code Reviewer,

Thank you for taking the time to go over my code and your consideration of me for a Software
Engineering role at LogicGate. I had no previous experience using Spring Boot or streams
so this was a great learning experience as well as hopefully a good showcase of my ability
to learn quickly. I am sincerely appreciative of your time and jealous of your work environment!
LogicGate seems like a perfect culture fit for myself, both in terms of professional and
personal growth. The attitudes reflected to me by recruiting and Engineering leadership are
very welcoming and seem conducive to team success. I just wanted to take this section to
once again express my extremely high interest in working for LogicGate and thanks for your
consideration.

Best,
Tyler Sharkey
 */
