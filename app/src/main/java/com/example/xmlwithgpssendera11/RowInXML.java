package com.example.xmlwithgpssendera11;//пакет

public class RowInXML//обычный класс для хранения времени и географических координат
{
    private String time;
    private double latitude;
    private double longitude;


    public RowInXML(String time, double latitude,double longitude)
    {
        this.time=time;
        this.latitude=latitude;
        this.longitude=longitude;
    }

    public String getTime()
    {
        return time;
    }

    public void setTime(String time)
    {
        this.time=time;
    }

    public double getLatitude()
    {
        return latitude;
    }

    public void setLatitude(double latitude)
    {
        this.latitude=latitude;
    }

    public double getLongitude()
    {
        return longitude;
    }

    public void setLongitude(double longitude)
    {
        this.longitude=longitude;
    }


}

