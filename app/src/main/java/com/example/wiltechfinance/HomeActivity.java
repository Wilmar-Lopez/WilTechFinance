package com.example.wiltechfinance;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private TextView tvMontoTotal, tvNombreUsuario;
    private LinearLayout layoutSaldoOriginal;
    private View contenedorSubpantallas;

    private Button menuHome, menuHistorial, menuGrafica;

    private HistorialFragment fragHistorial;
    private GraficaFragment fragGrafica;

    private static final int REQ_FORMULARIO = 301;

    private FirebaseFirestore db;
    private ListenerRegistration saldoListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Inicialización de Firebase Firestore
        db = FirebaseFirestore.getInstance();

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

        // Escucha en tiempo real el saldo calculado desde la nube
        escucharSaldoEnTiempoReal();

        btnEditarPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, EditarPerfilActivity.class);
            startActivity(intent);
        });

        btnActividad.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, FormularioActivity.class);
            startActivityForResult(intent, REQ_FORMULARIO);
        });

        btnSalir.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        menuHome.setOnClickListener(v -> {
            limpiarContenedorFragmentos();
            layoutSaldoOriginal.setVisibility(View.VISIBLE);
            contenedorSubpantallas.setVisibility(View.GONE);

            menuHome.setTextColor(Color.parseColor("#E50914"));
            menuHistorial.setTextColor(Color.WHITE);
            menuGrafica.setTextColor(Color.WHITE);
        });

        menuHistorial.setOnClickListener(v -> {
            layoutSaldoOriginal.setVisibility(View.GONE);
            contenedorSubpantallas.setVisibility(View.VISIBLE);

            fragHistorial = new HistorialFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.contenedorHome, fragHistorial)
                    .commitAllowingStateLoss();

            menuHome.setTextColor(Color.WHITE);
            menuHistorial.setTextColor(Color.parseColor("#E50914"));
            menuGrafica.setTextColor(Color.WHITE);
        });

        menuGrafica.setOnClickListener(v -> {
            layoutSaldoOriginal.setVisibility(View.GONE);
            contenedorSubpantallas.setVisibility(View.VISIBLE);

            fragGrafica = new GraficaFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.contenedorHome, fragGrafica)
                    .commitAllowingStateLoss();

            menuHome.setTextColor(Color.WHITE);
            menuHistorial.setTextColor(Color.WHITE);
            menuGrafica.setTextColor(Color.parseColor("#E50914"));
        });
    }

    private void escucharSaldoEnTiempoReal() {
        saldoListener = db.collection("transacciones")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    double total = 0;
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Double monto = doc.getDouble("monto");
                        String tipo = doc.getString("tipo");

                        if (monto != null && tipo != null) {
                            if ("Ingreso".equalsIgnoreCase(tipo)) {
                                total += monto;
                            } else {
                                total -= monto;
                            }
                        }
                    }
                    tvMontoTotal.setText(String.format(Locale.US, "$%,.2f", total));
                });
    }

    private void limpiarContenedorFragmentos() {
        Fragment fragmentoActual = getSupportFragmentManager().findFragmentById(R.id.contenedorHome);
        if (fragmentoActual != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(fragmentoActual)
                    .commitAllowingStateLoss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (saldoListener != null) {
            saldoListener.remove();
        }
    }
}