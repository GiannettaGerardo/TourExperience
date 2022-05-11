package it.uniba.sms2122.tourexperience.model;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import it.uniba.sms2122.tourexperience.R;

public class Museo {
    private String nome;
    private String citta;
    private String descrizione;
    private String tipologia;
    private String fileUri;

    public Museo(String nome) {
        this.nome = nome;
        this.citta = "";
        this.descrizione = "";
        this.tipologia = "";
        this.fileUri = "";
    }

    public Museo(String nome, String citta) {
        this.nome = nome;
        this.citta = citta;
        this.tipologia = "";
        this.fileUri = "";
    }

    public Museo(String nome, String citta, String descrizione, String tipologia, String fileUri) {
        this.nome = nome;
        this.citta = citta;
        this.descrizione = descrizione;
        this.tipologia = tipologia;
        this.fileUri = fileUri;
    }

    public String getNome() { return nome; }

    public void setNome(String nome) { this.nome = nome; }

    public String getCitta() { return citta; }

    public void setCitta(String citta) { this.citta = citta; }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getTipologia() { return tipologia; }

    public void setTipologia(String tipologia) { this.tipologia = tipologia; }

    public String getFileUri() {
        return fileUri;
    }

    public void setFileUri(String fileUri) {
        this.fileUri = fileUri;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        return builder.append("Nome: ").append(this.nome)
                .append(", Citta': ").append(this.citta)
                .append(", Tipologia: ").append(this.tipologia)
                .append(", URI: ").append(this.fileUri)
                .toString();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof Museo)) return false;
        Museo m = (Museo) obj;
        if (!m.getNome().equals(this.getNome())) return false;
        return m.getCitta().equals(this.getCitta());
    }

    /**
     * Ritorna un oggetto museo definito come Museo Vuoto.
     * @param resources per ottenere le stringhe che servono
     * @return un oggetto museo con solo nome e citta impostati,
     * ma impostati cone delle stringhe che definiscono questo museo
     * un museo vuoto.
     */
    public static Museo getMuseoVuoto(@NonNull Resources resources) {
        return new Museo(
            resources.getString(R.string.no_result_1),
            resources.getString(R.string.no_result_2)
        );
    }
}