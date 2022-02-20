package ch.so.agi.grundstuecksinformation;

import java.util.List;

public class Grundstueck {
    private String display;
    private String id;
    private String type;
    private String number;
    private String egrid;
    private String lawStatus;
    private List<Double> bbox;
    
    public String getDisplay() {
        return display;
    }
    public void setDisplay(String display) {
        this.display = display;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getNumber() {
        return number;
    }
    public void setNumber(String number) {
        this.number = number;
    }
    public String getEgrid() {
        return egrid;
    }
    public void setEgrid(String egrid) {
        this.egrid = egrid;
    }
    public String getLawStatus() {
        return lawStatus;
    }
    public void setLawStatus(String lawStatus) {
        this.lawStatus = lawStatus;
    }
    public List<Double> getBbox() {
        return bbox;
    }
    public void setBbox(List<Double> bbox) {
        this.bbox = bbox;
    }
}
