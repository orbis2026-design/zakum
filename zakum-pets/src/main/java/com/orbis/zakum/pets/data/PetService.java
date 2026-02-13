package com.orbis.zakum.pets.data;

import com.orbis.zakum.pets.model.PetData;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PetService {
    private final Map<UUID, PetData> activePetCache = new ConcurrentHashMap<>();

    public PetData loadActivePet(UUID uuid) {
        // SELECT * FROM orbis_pets_data ...
        return null; 
    }

    public void cachePet(UUID uuid, PetData pet) {
        activePetCache.put(uuid, pet);
    }

    public PetData getCachedPet(UUID uuid) {
        return activePetCache.get(uuid);
    }

    public void removeCache(UUID uuid) {
        activePetCache.remove(uuid);
    }
}
