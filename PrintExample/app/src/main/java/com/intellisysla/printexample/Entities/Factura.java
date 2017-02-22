package com.intellisysla.printexample.Entities;

import java.util.List;

/**
 * Created by alienware on 2/22/2017.
 */

public class Factura {
    private String empresa;
    private String emp_telefono;
    private String emp_rtn;
    private String emp_direccion;
    private String fecha;
    //private String folio;
    private String codigoCliente;
    private String nombreCliente;
    private String direccion;
    private String telefono;
    private String vendedor;
    private Double subtotal;
    private Double descuento;
    private Double isv;
    private Double total;

    private List<Item> items;

    public Factura(){
    }

    public Factura(String empresa, String emp_telefono, String emp_rtn, String fecha, String codigoCliente, String direccion, String telefono, String vendedor, Double subtotal, Double descuento, Double isv, Double total) {
        this.empresa = empresa;
        this.emp_telefono = emp_telefono;
        this.emp_rtn = emp_rtn;
        this.fecha = fecha;
        this.codigoCliente = codigoCliente;
        this.direccion = direccion;
        this.telefono = telefono;
        this.vendedor = vendedor;
        this.subtotal = subtotal;
        this.descuento = descuento;
        this.isv = isv;
        this.total = total;
    }

    public String getEmp_direccion() {
        return emp_direccion;
    }

    public void setEmp_direccion(String emp_direccion) {
        this.emp_direccion = emp_direccion;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public String getEmp_telefono() {
        return emp_telefono;
    }

    public void setEmp_telefono(String emp_telefono) {
        this.emp_telefono = emp_telefono;
    }

    public String getEmp_rtn() {
        return emp_rtn;
    }

    public void setEmp_rtn(String emp_rtn) {
        this.emp_rtn = emp_rtn;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getCodigoCliente() {
        return codigoCliente;
    }

    public void setCodigoCliente(String codigoCliente) {
        this.codigoCliente = codigoCliente;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getVendedor() {
        return vendedor;
    }

    public void setVendedor(String vendedor) {
        this.vendedor = vendedor;
    }

    public Double getDescuento() {
        return descuento;
    }

    public void setDescuento(Double descuento) {
        this.descuento = descuento;
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal;
    }

    public Double getIsv() {
        return isv;
    }

    public void setIsv(Double isv) {
        this.isv = isv;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}
