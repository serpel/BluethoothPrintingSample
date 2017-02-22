package com.intellisysla.printexample.Entities;

/**
 * Created by alienware on 2/22/2017.
 */

public class Item {
    private String SKU;
    private int cantidad;
    private Double precio;

    public Item(String SKU, int cantidad, Double precio) {
        this.SKU = SKU;
        this.cantidad = cantidad;
        this.precio = precio;
    }

    public String getSKU() {
        return SKU;
    }

    public void setSKU(String SKU) {
        this.SKU = SKU;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }
}
