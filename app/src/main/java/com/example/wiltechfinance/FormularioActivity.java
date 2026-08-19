package com.example.wiltechfinance;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FormularioActivity extends AppCompatActivity {

    private EditText etConcepto, etMonto;
    private RadioGroup rgTipo;
    private RadioButton rbIngreso;
    private Button btnRegistrar;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_formulario);

        // Inicialización de Firestore Nube
        db = FirebaseFirestore.getInstance();

        etConcepto = findViewById(R.id.etConcepto);
        etMonto = findViewById(R.id.etMonto);
        rgTipo = findViewById(R.id.rgTipo);
        rbIngreso = findViewById(R.id.rbIngreso);

        btnRegistrar = findViewById(R.id.btnRegistrarActividad);
        Button btnCancelar = findViewById(R.id.btnCancelarActividad);

        // 🔥 ESCUCHADOR EN VIVO: Mantiene tus puntos de miles automáticamente
        etMonto.addTextChangedListener(new TextWatcher() {
            private String actual = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(actual)) {
                    etMonto.removeTextChangedListener(this);

                    String limpio = s.toString().replaceAll("[.,]", "");

                    if (!limpio.isEmpty()) {
                        try {
                            double numero = Double.parseDouble(limpio);

                            DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.US);
                            simbolos.setGroupingSeparator('.');
                            DecimalFormat formateador = new DecimalFormat("#,###", simbolos);

                            actual = formateador.format(numero);
                            etMonto.setText(actual);
                            etMonto.setSelection(actual.length());
                        } catch (NumberFormatException e) {
                            // Ignora errores si la cifra es inválida
                        }
                    } else {
                        actual = "";
                    }

                    etMonto.addTextChangedListener(this);
                }
            }
        });

        btnRegistrar.setOnClickListener(v -> guardarEnFirestore());
        btnCancelar.setOnClickListener(v -> finish());
    }

    private void guardarEnFirestore() {
        String concepto = etConcepto.getText().toString().trim();
        String montoStr = etMonto.getText().toString().trim();

        if (concepto.isEmpty() || montoStr.isEmpty()) {
            Toast.makeText(this, "Completa todos los datos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Limpieza de puntos para operar el número real
        String montoLimpio = montoStr.replaceAll("\\.", "");
        double montoIngresado = Double.parseDouble(montoLimpio);
        String tipo = rbIngreso.isChecked() ? "Ingreso" : "Gasto";

        // Bloquea el botón mientras guarda para evitar duplicados
        btnRegistrar.setEnabled(false);

        // Obtenemos fecha actual
        String fechaActual = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Objeto Transaccion listo para Firestore
        Transaccion transaccion = new Transaccion(concepto, montoIngresado, tipo, fechaActual);

        // Guardar en la colección "transacciones" de la nube
        db.collection("transacciones")
                .add(transaccion)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "¡Movimiento guardado en Firestore!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnRegistrar.setEnabled(true);
                    Toast.makeText(this, "Error al conectar con la nube", Toast.LENGTH_SHORT).show();
                });
    }
}