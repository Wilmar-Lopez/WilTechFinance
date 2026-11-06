package com.example.wiltechfinance;

public class Usuario {
    private String nombre;
    private String correo;
    private String telefono;
    private String rol;

    public Usuario() {
        // Constructor vacío requerido por Firebase
    }

    public Usuario(String nombre, String correo, String telefono, String rol) {
        this.nombre = nombre;
        this.correo = correo;
        this.telefono = telefono;
        this.rol = rol;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}