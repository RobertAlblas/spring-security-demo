package com.bol.securitydemo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/pets")
public class PetController {

    private final Map<Long, Pet> pets = new HashMap<>();

    @GetMapping
    public Collection<Pet> getPets() {
        return pets.values();
    }

    @GetMapping("/{id}")
    public Pet getPet(String id) {
        return pets.get(id);
    }

    @PutMapping("/{id}")
    public Pet updatePet(Long id, @RequestBody Pet pet) {
        pets.put(id, pet);
        return pets.get(id);
    }

    @PostMapping
    public Pet insertPet(@RequestBody Pet pet) {
        Long highestId = pets.keySet().stream().max(Comparator.naturalOrder()).orElse(-1L);
        pet.id = highestId + 1;
        pets.put(pet.id, pet);
        return pet;
    }
}
