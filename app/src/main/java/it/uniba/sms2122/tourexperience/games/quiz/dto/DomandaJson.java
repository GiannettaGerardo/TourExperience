package it.uniba.sms2122.tourexperience.games.quiz.dto;

import java.util.List;

/**
 * Rappresenta un oggetto Domanda ottenuto dal parsing di un json.
 */
public class DomandaJson {
    private String id;
    private String domanda;
    private Double valore;
    private List<RispostaJson> risposte;

    public String getId() {
        return id;
    }

    public String getDomanda() {
        return domanda;
    }

    public List<RispostaJson> getRisposte() {
        return risposte;
    }

    public Double getValore() {
        return valore;
    }

}
