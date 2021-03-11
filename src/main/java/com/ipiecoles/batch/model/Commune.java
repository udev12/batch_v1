package com.ipiecoles.batch.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Commune {

    @Id
    @Column(length = 5)
    private String codeInsee;
    private String nom;

    @Column(length = 5)
    private String codePostal;
    private Double latitude;
    private Double longitude;

    public String getCodeInsee() {
        return codeInsee;
    }

    public void setCodeInsee(String codeInsee) {
        this.codeInsee = codeInsee;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setCodePostal(String codePostal) {
        this.codePostal = codePostal;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getNom() {
        return nom;
    }

    public String getCodePostal() {
        return codePostal;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Commune() {



    }


}
