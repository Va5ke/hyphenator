package com.ftn.sbnz.model.models;

public class EventRequest {
    private int number;

    public EventRequest() {};

    public EventRequest(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }
}