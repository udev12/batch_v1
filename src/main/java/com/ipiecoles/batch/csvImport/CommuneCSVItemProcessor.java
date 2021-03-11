package com.ipiecoles.batch.csvImport;

import com.ipiecoles.batch.dto.CommuneCSV;
import com.ipiecoles.batch.exception.CommuneCSVException;
import com.ipiecoles.batch.model.Commune;
import org.apache.commons.text.WordUtils;
import org.springframework.batch.item.ItemProcessor;

import java.util.regex.Pattern;

public class CommuneCSVItemProcessor implements ItemProcessor<CommuneCSV, Commune> {
    @Override
    public Commune process(CommuneCSV item) throws Exception {
        Commune commune = new Commune();
        validateCommuneCSV(item);
        commune.setCodeInsee(item.getCodeInsee());
//        validateCommuneCSV(item);
//        commune.setCodeInsee(item.getCodeInsee());
        commune.setCodePostal(item.getCodePostal());
        //Majuscule première lettre de chaque terme
        String nomCommune = WordUtils.capitalizeFully(item.getNom());
        //Proprification du nom
        nomCommune = nomCommune.replaceAll("^L ", "L'");
        nomCommune = nomCommune.replaceAll(" L ", " L'");
        nomCommune = nomCommune.replaceAll("^D ", "D'");
        nomCommune = nomCommune.replaceAll(" D ", " D'");
        nomCommune = nomCommune.replaceAll("^St ", "Saint ");
        nomCommune = nomCommune.replaceAll(" St ", " Saint ");
        nomCommune = nomCommune.replaceAll("^Ste ", "Sainte ");
        nomCommune = nomCommune.replaceAll(" Sainte ", " Sainte ");
        commune.setNom(nomCommune);
        //Latitude/Longitude
        String[] coordonnees = item.getCoordonneesGps().split(",");
        if (coordonnees.length == 2) {
            commune.setLatitude(Double.valueOf(coordonnees[0]));
            commune.setLongitude(Double.valueOf(coordonnees[1]));
        }
        return commune;
    }

//    private void validateCommuneCSV(CommuneCSV item) throws Exception{
//
////        try{
////            Pattern.matches("^[0-9]{5}$", item.getCodeInsee());
////        }catch (Exception e){
////            System.out.println(e.getMessage());
////        }
//
//        //Contrôler Code INSEE 5 chiffres
//          if (!Pattern.matches("^[0-9]{5}$", item.getCodeInsee())){
//            throw new Exception("Le code INSEE n'est pas au bon format!");
//        }
//        //Contrôler Code postal 5 chiffres
//        if (!Pattern.matches("^[0-9]{5}$", item.getCodePostal())){
//            throw new Exception("Le code postal n'est pas au bon format!");
//        }
//        //Contrôler nom de la communes lettres en majuscules, espaces, tirets, et apostrophes
//        if (!Pattern.matches("^[A-Z-' ]+$", item.getNom())){
//            throw new Exception("Le nom de la commune contient un ou plusieurs chiffres!");
//        }
//        //Contrôler les coordonnées GPS
//        if (!Pattern.matches("^[-+]?([1-8]?\\\\d(\\\\.\\\\d+)?|90(\\\\.0+)?),\\\\s*[-+]?(180(\\\\.0+)?|((1[0-7]\\\\d)|([1-9]?\\\\d))(\\\\.\\\\d+)?)$", item.getCoordonneesGps())){
//            throw new Exception("Les coordonnées GPS ne sont pas au bon format!");
//        }
//
//    }

    private void validateCommuneCSV(CommuneCSV item) throws CommuneCSVException {
        //Contrôler Code INSEE 5 chiffres
        if(item.getCodeInsee() != null && !item.getCodeInsee().matches("^[0-9]{5}$")){
            throw new CommuneCSVException("Le code Insee ne contient pas 5 chiffres");
        }
        //Contrôler Code postal 5 chiffres
        if(item.getCodePostal() != null && !item.getCodePostal().matches("^[0-9]{5}$")){
            throw new CommuneCSVException("Le code Postal ne contient pas 5 chiffres");
        }
        //Contrôler nom de la communes lettres en majuscules, espaces, tirets, et apostrophes
        if(item.getNom() != null && !item.getNom().matches("^[A-Z-' ]+$")){
            throw new CommuneCSVException("Le nom de la commune n'est pas composé uniquement de lettres, espaces et tirets");
        }
        //Contrôler les coordonnées GPS
        if(item.getCoordonneesGps() != null && !item.getCoordonneesGps().matches("^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?),\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)$")){
            throw new CommuneCSVException("Le nom de la commune n'est pas composé uniquement de lettres, espaces et tirets");
        }
    }


}
