package com.example.wiltechfinance;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.wiltechfinance.R;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class FormularioActivity extends AppCompatActivity {

    private EditText etConcepto, etMonto;
    private RadioGroup rgTipo;
    private RadioButton rbIngreso;
    private ConexionSQLite dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_formulario);

        dbHelper = new ConexionSQLite(this);
        etConcepto = findViewById(R.id.etConcepto);
        etMonto = findViewById(R.id.etMonto);
        rgTipo = findViewById(R.id.rgTipo);
        rbIngreso = findViewById(R.id.rbIngreso);

        Button btnRegistrar = findViewById(R.id.btnRegistrarActividad);
        Button btnCancelar = findViewById(R.id.btnCancelarActividad);

        // 🔥 ESCUCHADOR EN VIVO: Agrega los puntos de miles automáticamente en pantalla
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

                    // Limpia cualquier punto o coma previo para reformatear desde cero
                    String limpio = s.toString().replaceAll("[.,]", "");

                    if (!limpio.isEmpty()) {
                        try {
                            double numero = Double.parseDouble(limpio);

                            // Configura el formato con punto (.) para separación de miles colombiano
                            DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.US);
                            simbolos.setGroupingSeparator('.');
                            DecimalFormat formateador = new DecimalFormat("#,###", simbolos);

                            actual = formateador.format(numero);
                            etMonto.setText(actual);
                            etMonto.setSelection(actual.length()); // Mueve el cursor al final
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

        btnRegistrar.setOnClickListener(v -> guardarEnBaseDeDatos());
        btnCancelar.setOnClickListener(v -> finish());
    }

    private void guardarEnBaseDeDatos() {
        String concepto = etConcepto.getText().toString().trim();
        String montoStr = etMonto.getText().toString().trim();

        if (concepto.isEmpty() || montoStr.isEmpty()) {
            Toast.makeText(this, "Completa todos los datos", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔥 MATEMÁTICA SEGURA: Quitamos los puntos visuales para que SQLite pueda operar el número real
        String montoLimpio = montoStr.replaceAll("\\.", "");
        double montoIngresado = Double.parseDouble(montoLimpio);
        String tipo = rbIngreso.isChecked() ? "Ingreso" : "Gasto";

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // 1. Obtener saldo actual
        Cursor cursor = db.rawQuery("SELECT saldo FROM usuarios WHERE credencial = 'Prueba@tech.com'", null);
        double saldoActual = 0;
        if (cursor.moveToFirst()) {
            saldoActual = cursor.getDouble(0);
        }
        cursor.close();

        // 2. Calcular nuevo saldo (Suma o Resta)
        double nuevoSaldo = (tipo.equals("Ingreso")) ? saldoActual + montoIngresado : saldoActual - montoIngresado;

        // 3. Guardar el movimiento en la tabla de transacciones
        ContentValues trans = new ContentValues();
        trans.put("concepto", concepto);
        trans.put("monto", montoIngresado);
        trans.put("tipo", tipo);
        // 🔥 FIJADO AQUÍ: Le obligamos a guardar la fecha real de la vida usando el método centralizado de tu Base de Datos
        trans.put("fecha", ConexionSQLite.obtenerFechaActual());
        db.insert("transacciones", null, trans);

        // 4. Actualizar el saldo definitivo del usuario
        ContentValues usu = new ContentValues();
        usu.put("saldo", nuevoSaldo);
        db.update("usuarios", usu, "credencial = 'Prueba@tech.com'", null);

        Toast.makeText(this, "¡Movimiento guardado con éxito!", Toast.LENGTH_SHORT).show();

        setResult(RESULT_OK); // Avisa al Home para refrescar las pantallas
        finish();
    }
}