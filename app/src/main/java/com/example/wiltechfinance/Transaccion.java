package com.example.wiltechfinance;

import com.google.firebase.firestore.Exclude;

public class Transaccion {

    @Exclude
    private String idDocumento;
    private String concepto;
    private double monto;
    private String tipo;
    private String fecha;

    // Campos para la rúbrica (Foto/Factura y GPS)
    private String urlFoto;
    private String urlFactura;
    private double latitud;
    private double longitud;

    // Constructor vacío OBLIGATORIO para Firebase Firestore
    public Transaccion() {
    }

    public Transaccion(String concepto, double monto, String tipo, String fecha, String urlFoto, String urlFactura, double latitud, double longitud) {
        this.concepto = concepto;
        this.monto = monto;
        this.tipo = tipo;
        this.fecha = fecha;
        this.urlFoto = urlFoto;
        this.urlFactura = urlFactura;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    @Exclude
    public String getIdDocumento() { return idDocumento; }
    @Exclude
    public void setIdDocumento(String idDocumento) { this.idDocumento = idDocumento; }

    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getUrlFoto() { return urlFoto; }
    public void setUrlFoto(String urlFoto) { this.urlFoto = urlFoto; }

    public String getUrlFactura() { return urlFactura; }
    public void setUrlFactura(String urlFactura) { this.urlFactura = urlFactura; }

    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }
}