package com.orbis.zakum.pets.model;

public class PetData {
    private final String petId;
    private String customName;
    private int level;
    private double xp;
    private boolean isActive;

    public PetData(String petId, String customName, int level, double xp, boolean isActive) {
        this.petId = petId;
        this.customName = customName;
        this.level = level;
        this.xp = xp;
        this.isActive = isActive;
    }

    public String getPetId() { return petId; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
}
