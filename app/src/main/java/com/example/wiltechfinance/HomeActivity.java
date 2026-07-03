package com.example.wiltechfinance;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import java.util.Locale;

import com.example.wiltechfinance.R;

public class HomeActivity extends AppCompatActivity {

    // Tus variables globales de interfaz originales
    private TextView tvMontoTotal, tvNombreUsuario;
    private ConexionSQLite dbHelper;
    private LinearLayout layoutSaldoOriginal;
    private View contenedorSubpantallas;

    // Tus botones de navegación inferiores originales
    private Button menuHome, menuHistorial, menuGrafica;

    // Instancias de los fragmentos para el intercambio de vistas
    private HistorialFragment fragHistorial;
    private GraficaFragment fragGrafica;

    // Tu ID único e indispensable para el retorno del formulario
    private static final int REQ_FORMULARIO = 301;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Inicialización de tu manejador de base de datos SQLite
        dbHelper = new ConexionSQLite(this);

        // Enlace de tus elementos originales de la cabecera y cuerpo
        tvMontoTotal = findViewById(R.id.tvMontoTotal);
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario);
        ImageView btnSalir = findViewById(R.id.btnCerrarSesionTop);
        Button btnEditarPerfil = findViewById(R.id.btnEditarPerfil);
        Button btnActividad = findViewById(R.id.btnActividad);

        // Enlace de los contenedores de intercambio y la barra inferior
        layoutSaldoOriginal = findViewById(R.id.layoutSaldo);
        contenedorSubpantallas = findViewById(R.id.contenedorHome);
        menuHome = findViewById(R.id.menuHome);
        menuHistorial = findViewById(R.id.menuHistorial);
        menuGrafica = findViewById(R.id.menuGrafica);

        // Tu carga inicial obligatoria del saldo real del usuario Prueba
        actualizarSaldoPantalla();

        // CONTROLADORES DE CLIC ORIGINALES DE TU NEGOCIO

        // Abre tu subpantalla para modificar los datos del perfil de Prueba
        btnEditarPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, EditarPerfilActivity.class);
            startActivity(intent);
        });

        // Abre tu formulario esperando el código de éxito RESULT_OK (REQ_FORMULARIO = 301)
        btnActividad.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, FormularioActivity.class);
            startActivityForResult(intent, REQ_FORMULARIO);
        });

        // Tu botón de salida segura: limpia la pila de actividades y regresa al MainActivity
        btnSalir.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // CONTROLADORES DE NAVEGACIÓN ENTRE SUBPANTALLAS (INTERCAMBIO SEGURO)

        // Botón HOME: Apaga los fragmentos de la RAM y enciende tu layoutSaldo original
        menuHome.setOnClickListener(v -> {
            limpiarContenedorFragmentos();
            layoutSaldoOriginal.setVisibility(View.VISIBLE);
            contenedorSubpantallas.setVisibility(View.GONE);
            actualizarSaldoPantalla(); // Refresca tu saldo original inmediatamente

            // Cambios de color estéticos exigidos
            menuHome.setTextColor(android.graphics.Color.parseColor("#E50914"));
            menuHistorial.setTextColor(android.graphics.Color.WHITE);
            menuGrafica.setTextColor(android.graphics.Color.WHITE);
        });

        // Botón HISTORIAL: Oculta el saldo original e inyecta la lista de transacciones sin pisar vistas
        menuHistorial.setOnClickListener(v -> {
            layoutSaldoOriginal.setVisibility(View.GONE);
            contenedorSubpantallas.setVisibility(View.VISIBLE);

            fragHistorial = new HistorialFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.contenedorHome, fragHistorial)
                    .commitAllowingStateLoss(); // Blindaje total contra cierres imprevistos del sistema

            // Espera que el hilo visual renderice el fragmento para inyectar los datos de SQLite
            contenedorSubpantallas.post(() -> {
                if (fragHistorial != null && fragHistorial.isAdded()) {
                    fragHistorial.cargarTransaccionesDesdeBD();
                }
            });

            menuHome.setTextColor(android.graphics.Color.WHITE);
            menuHistorial.setTextColor(android.graphics.Color.parseColor("#E50914"));
            menuGrafica.setTextColor(android.graphics.Color.WHITE);
        });

        // Botón GRÁFICA: Oculta el saldo original e inyecta el plano cartesiano de picos en vivo
        menuGrafica.setOnClickListener(v -> {
            layoutSaldoOriginal.setVisibility(View.GONE);
            contenedorSubpantallas.setVisibility(View.VISIBLE);

            fragGrafica = new GraficaFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.contenedorHome, fragGrafica)
                    .commitAllowingStateLoss(); // Blindaje contra pérdidas de estado en el Honor

            contenedorSubpantallas.post(() -> {
                if (fragGrafica != null && fragGrafica.isAdded()) {
                    fragGrafica.calcularFlujoCartesiano();
                }
            });

            menuHome.setTextColor(android.graphics.Color.WHITE);
            menuHistorial.setTextColor(android.graphics.Color.WHITE);
            menuGrafica.setTextColor(android.graphics.Color.parseColor("#E50914"));
        });
    }

    // Método auxiliar que remueve fragmentos residuales de la memoria para que no se trabe la app
    private void limpiarContenedorFragmentos() {
        Fragment fragmentoActual = getSupportFragmentManager().findFragmentById(R.id.contenedorHome);
        if (fragmentoActual != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(fragmentoActual)
                    .commitAllowingStateLoss();
        }
    }

    // Tu método original matemático que lee de SQLite y aplica formato regional estadounidense ($%,.2f)
    private void actualizarSaldoPantalla() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT saldo FROM usuarios WHERE credencial = 'Prueba@tech.com'", null);
        if (cursor.moveToFirst()) {
            double saldoDb = cursor.getDouble(0);
            tvMontoTotal.setText(String.format(Locale.US, "$%,.2f", saldoDb));
        }
        cursor.close();
    }

    // Tu método original onActivityResult: captura el cierre del Formulario y actualiza el saldo de inmediato
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FORMULARIO && resultCode == RESULT_OK) {
            actualizarSaldoPantalla(); // Refresco instantáneo del balance general
        }
    }
}