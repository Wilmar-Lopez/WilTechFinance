package com.example.wiltechfinance;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private TextView tvMontoTotal, tvNombreUsuario;
    private LinearLayout layoutSaldoOriginal;
    private View contenedorSubpantallas;

    private Button menuHome, menuHistorial, menuGrafica, menuMapa;

    private HistorialFragment fragHistorial;
    private GraficaFragment fragGrafica;
    private Fragment fragAdmin;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration saldoListener;
    private SharedPreferences prefs;

    private String rolUsuario;

    // Variables para el Modo Auditoría
    private String correoAuditado = null;
    private String nombreAuditado = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("SesionUsuario", Context.MODE_PRIVATE);

        rolUsuario = prefs.getString("rol", "Estándar").trim();

        tvMontoTotal = findViewById(R.id.tvMontoTotal);
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario);
        ImageView btnSalir = findViewById(R.id.btnCerrarSesionTop);
        Button btnEditarPerfil = findViewById(R.id.btnEditarPerfil);
        Button btnActividad = findViewById(R.id.btnActividad);

        layoutSaldoOriginal = findViewById(R.id.layoutSaldo);
        contenedorSubpantallas = findViewById(R.id.contenedorHome);
        menuHome = findViewById(R.id.menuHome);
        menuHistorial = findViewById(R.id.menuHistorial);
        menuGrafica = findViewById(R.id.menuGrafica);
        menuMapa = findViewById(R.id.menuMapa);

        if (esAdmin()) {
            menuGrafica.setText("USUARIOS");
        }

        cargarYRefrescarNombreUsuario();
        escucharSaldoEnTiempoReal();

        btnEditarPerfil.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, EditarPerfilActivity.class));
        });

        btnActividad.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, FormularioActivity.class);
            if (correoAuditado != null) {
                intent.putExtra("correoAuditado", correoAuditado);
            }
            startActivity(intent);
        });

        btnSalir.setOnClickListener(v -> {
            // Si está auditando, el botón superior permite salir del modo auditoría
            if (correoAuditado != null) {
                salirDeAuditoria();
                return;
            }

            mAuth.signOut();
            prefs.edit().clear().apply();

            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        menuHome.setOnClickListener(v -> {
            limpiarContenedorFragmentos();
            layoutSaldoOriginal.setVisibility(View.VISIBLE);
            contenedorSubpantallas.setVisibility(View.GONE);
            actualizarColoresMenu(menuHome);
        });

        menuHistorial.setOnClickListener(v -> {
            layoutSaldoOriginal.setVisibility(View.GONE);
            contenedorSubpantallas.setVisibility(View.VISIBLE);

            fragHistorial = new HistorialFragment();
            if (correoAuditado != null) {
                Bundle bundle = new Bundle();
                bundle.putString("correoAuditado", correoAuditado);
                fragHistorial.setArguments(bundle);
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.contenedorHome, fragHistorial).commitAllowingStateLoss();
            actualizarColoresMenu(menuHistorial);
        });

        menuGrafica.setOnClickListener(v -> {
            layoutSaldoOriginal.setVisibility(View.GONE);
            contenedorSubpantallas.setVisibility(View.VISIBLE);

            if (esAdmin() && correoAuditado == null) {
                fragAdmin = new AdminUsuariosFragment();
                getSupportFragmentManager().beginTransaction().replace(R.id.contenedorHome, fragAdmin).commitAllowingStateLoss();
            } else {
                fragGrafica = new GraficaFragment();
                if (correoAuditado != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("correoAuditado", correoAuditado);
                    fragGrafica.setArguments(bundle);
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.contenedorHome, fragGrafica).commitAllowingStateLoss();
            }
            actualizarColoresMenu(menuGrafica);
        });

        menuMapa.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MapaActivity.class);
            if (correoAuditado != null) {
                intent.putExtra("correoAuditado", correoAuditado);
            }
            startActivity(intent);
        });
    }

    public boolean esAdmin() {
        return "Administrador".equalsIgnoreCase(rolUsuario) || "Admin".equalsIgnoreCase(rolUsuario);
    }

    // Método para ingresar a la cuenta de un usuario seleccionado
    public void iniciarAuditoria(String correo, String nombre) {
        this.correoAuditado = correo.toLowerCase().trim();
        this.nombreAuditado = nombre;

        Toast.makeText(this, "Auditando a: " + nombre, Toast.LENGTH_SHORT).show();
        tvNombreUsuario.setText("Auditando: " + nombre);

        if (esAdmin()) {
            menuGrafica.setText("GRÁFICA");
        }

        // Ir al Home con el saldo del usuario auditado
        limpiarContenedorFragmentos();
        layoutSaldoOriginal.setVisibility(View.VISIBLE);
        contenedorSubpantallas.setVisibility(View.GONE);
        actualizarColoresMenu(menuHome);

        escucharSaldoEnTiempoReal();
    }

    // Método para regresar a la vista normal del Administrador
    public void salirDeAuditoria() {
        this.correoAuditado = null;
        this.nombreAuditado = null;

        Toast.makeText(this, "Modo auditoría finalizado", Toast.LENGTH_SHORT).show();
        cargarYRefrescarNombreUsuario();

        if (esAdmin()) {
            menuGrafica.setText("USUARIOS");
        }

        limpiarContenedorFragmentos();
        layoutSaldoOriginal.setVisibility(View.VISIBLE);
        contenedorSubpantallas.setVisibility(View.GONE);
        actualizarColoresMenu(menuHome);

        escucharSaldoEnTiempoReal();
    }

    public String getCorreoActivo() {
        if (correoAuditado != null) {
            return correoAuditado;
        }
        FirebaseUser user = mAuth.getCurrentUser();
        return (user != null && user.getEmail() != null) ? user.getEmail().toLowerCase().trim() : "";
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (correoAuditado == null) {
            cargarYRefrescarNombreUsuario();
        }
    }

    private void cargarYRefrescarNombreUsuario() {
        String nombreGuardado = prefs.getString("nombre", null);
        if (nombreGuardado != null && !nombreGuardado.isEmpty()) {
            tvNombreUsuario.setText("Hola, " + nombreGuardado);
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            String correoUser = user.getEmail().toLowerCase().trim();

            db.collection("usuarios").document(correoUser).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && doc.getString("nombre") != null) {
                            aplicarNombre(doc.getString("nombre"));
                            String rol = doc.getString("rol");
                            if (rol != null) {
                                rolUsuario = rol.trim();
                                prefs.edit().putString("rol", rolUsuario).apply();
                                if (esAdmin() && correoAuditado == null) {
                                    menuGrafica.setText("USUARIOS");
                                }
                            }
                        } else {
                            db.collection("usuarios").whereEqualTo("correo", correoUser).limit(1).get()
                                    .addOnSuccessListener(querySnapshots -> {
                                        if (!querySnapshots.isEmpty()) {
                                            DocumentSnapshot d = querySnapshots.getDocuments().get(0);
                                            String nombre = d.getString("nombre");
                                            if (nombre != null) aplicarNombre(nombre);
                                            String rol = d.getString("rol");
                                            if (rol != null) {
                                                rolUsuario = rol.trim();
                                                prefs.edit().putString("rol", rolUsuario).apply();
                                                if (esAdmin() && correoAuditado == null) {
                                                    menuGrafica.setText("USUARIOS");
                                                }
                                            }
                                        }
                                    });
                        }
                    });
        }
    }

    private void aplicarNombre(String nombre) {
        tvNombreUsuario.setText("Hola, " + nombre);
        prefs.edit().putString("nombre", nombre).apply();
    }

    private void actualizarColoresMenu(Button activo) {
        menuHome.setTextColor(Color.WHITE);
        menuHistorial.setTextColor(Color.WHITE);
        menuGrafica.setTextColor(Color.WHITE);
        menuMapa.setTextColor(Color.WHITE);
        activo.setTextColor(Color.parseColor("#E50914"));
    }

    private void escucharSaldoEnTiempoReal() {
        if (saldoListener != null) {
            saldoListener.remove();
        }

        String correoFiltro = getCorreoActivo();
        if (correoFiltro.isEmpty()) return;

        Query query = db.collection("transacciones").whereEqualTo("correoUsuario", correoFiltro);

        saldoListener = query.addSnapshotListener((snapshots, error) -> {
            if (error != null || snapshots == null) return;
            double total = 0;
            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                Double monto = doc.getDouble("monto");
                String tipo = doc.getString("tipo");
                if (monto != null && tipo != null) {
                    if ("Ingreso".equalsIgnoreCase(tipo)) total += monto;
                    else total -= monto;
                }
            }
            tvMontoTotal.setText(String.format(Locale.US, "$%,.2f", total));
        });
    }

    private void limpiarContenedorFragmentos() {
        Fragment fragmentoActual = getSupportFragmentManager().findFragmentById(R.id.contenedorHome);
        if (fragmentoActual != null) {
            getSupportFragmentManager().beginTransaction().remove(fragmentoActual).commitAllowingStateLoss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (saldoListener != null) saldoListener.remove();
    }
}