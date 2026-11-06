package com.example.wiltechfinance;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapaActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int CODIGO_PERMISO_UBICACION = 1001;

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FusedLocationProviderClient clienteUbicacion;

    private ProgressBar pbCargandoMapa;
    private FloatingActionButton fabMiUbicacion;
    private String correoAuditado = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);

        if (getIntent() != null && getIntent().hasExtra("correoAuditado")) {
            correoAuditado = getIntent().getStringExtra("correoAuditado");
        }

        db = FirebaseFirestore.getInstance();
        clienteUbicacion = LocationServices.getFusedLocationProviderClient(this);

        pbCargandoMapa = findViewById(R.id.pbCargandoMapa);
        fabMiUbicacion = findViewById(R.id.fabMiUbicacion);
        Button btnVolverMapa = findViewById(R.id.btnVolverMapa);

        btnVolverMapa.setOnClickListener(v -> finish());
        fabMiUbicacion.setOnClickListener(v -> centrarEnUbicacionActual(true));

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapaFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private String obtenerCorreoObjetivo() {
        if (correoAuditado != null && !correoAuditado.isEmpty()) {
            return correoAuditado.toLowerCase().trim();
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return (user != null && user.getEmail() != null) ? user.getEmail().toLowerCase().trim() : "";
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        // Geocodificación inversa: Manten presionado para guardar un lugar
        mMap.setOnMapLongClickListener(this::mostrarDialogoConDireccion);

        solicitarPermisosYObtenerUbicacion();
        cargarLugaresFavoritos();
    }

    private String obtenerDireccionFisica(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> direcciones = geocoder.getFromLocation(lat, lng, 1);
            if (direcciones != null && !direcciones.isEmpty()) {
                Address direccion = direcciones.get(0);
                return direccion.getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Coordenadas: " + String.format(Locale.US, "%.4f, %.4f", lat, lng);
    }

    private void mostrarDialogoConDireccion(LatLng latLng) {
        String direccionTexto = obtenerDireccionFisica(latLng.latitude, latLng.longitude);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Guardar Lugar");
        builder.setMessage("Dirección detectada:\n" + direccionTexto + "\n\nIngresa un nombre para este punto:");

        final EditText input = new EditText(this);
        input.setHint("Ej: Puesto de perros / Cliente");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        builder.setView(input);

        builder.setPositiveButton("Guardar Pin", (dialog, which) -> {
            String nombreLugar = input.getText().toString().trim();
            if (!nombreLugar.isEmpty()) {
                guardarFavoritoEnFirestore(nombreLugar, direccionTexto, latLng);
            } else {
                Toast.makeText(MapaActivity.this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void guardarFavoritoEnFirestore(String nombre, String direccion, LatLng latLng) {
        Map<String, Object> favorito = new HashMap<>();
        favorito.put("nombre", nombre);
        favorito.put("direccion", direccion);
        favorito.put("latitud", latLng.latitude);
        favorito.put("longitud", latLng.longitude);
        favorito.put("correoUsuario", obtenerCorreoObjetivo());

        db.collection("lugares_favoritos").add(favorito)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Lugar guardado exitosamente", Toast.LENGTH_SHORT).show();
                    mMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title(nombre)
                            .snippet(direccion)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al guardar el lugar", Toast.LENGTH_SHORT).show());
    }

    private void cargarLugaresFavoritos() {
        String correoFiltro = obtenerCorreoObjetivo();
        if (correoFiltro.isEmpty()) return;

        db.collection("lugares_favoritos")
                .whereEqualTo("correoUsuario", correoFiltro)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String nombre = doc.getString("nombre");
                        String direccion = doc.getString("direccion");
                        Double lat = doc.getDouble("latitud");
                        Double lng = doc.getDouble("longitud");

                        if (nombre != null && lat != null && lng != null) {
                            LatLng ubicacionFavorito = new LatLng(lat, lng);
                            mMap.addMarker(new MarkerOptions()
                                    .position(ubicacionFavorito)
                                    .title(nombre)
                                    .snippet(direccion != null ? direccion : "")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                        }
                    }
                    if (pbCargandoMapa != null) pbCargandoMapa.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    if (pbCargandoMapa != null) pbCargandoMapa.setVisibility(View.GONE);
                    Toast.makeText(this, "Error al cargar pines", Toast.LENGTH_SHORT).show();
                });
    }

    private void solicitarPermisosYObtenerUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            centrarEnUbicacionActual(false);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    CODIGO_PERMISO_UBICACION);
        }
    }

    private void centrarEnUbicacionActual(boolean animar) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        clienteUbicacion.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng posicionActual = new LatLng(location.getLatitude(), location.getLongitude());
                if (animar) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(posicionActual, 15f));
                } else {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posicionActual, 14f));
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODIGO_PERMISO_UBICACION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    mMap.getUiSettings().setMyLocationButtonEnabled(false);
                    centrarEnUbicacionActual(true);
                }
            } else {
                if (pbCargandoMapa != null) pbCargandoMapa.setVisibility(View.GONE);
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}