package com.example.wiltechfinance;

import com.google.firebase.firestore.Exclude;

public class Transaccion {

    @Exclude
    private String idDocumento; // ID del documento generado por Firebase
    private String concepto;
    private double monto;
    private String tipo; // "Ingreso" o "Gasto"
    private String fecha; // Formato YYYY-MM-DD

    // Constructor vacío OBLIGATORIO para el mapeo automático de Firestore
    public Transaccion() {
    }

    public Transaccion(String concepto, double monto, String tipo, String fecha) {
        this.concepto = concepto;
        this.monto = monto;
        this.tipo = tipo;
        this.fecha = fecha;
    }

    // Getter y Setter especial para el Document ID de Firestore
    @Exclude
    public String getIdDocumento() {
        return idDocumento;
    }

    @Exclude
    public void setIdDocumento(String idDocumento) {
        this.idDocumento = idDocumento;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }
}