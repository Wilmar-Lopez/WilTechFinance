package com.example.wiltechfinance;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetalleGraficaActivity extends AppCompatActivity {

    private TextView tvTitulo, tvMontoMaximo;
    private PlanoCartesianoView planoDetalle;
    private ConexionSQLite dbHelper;
    private NumberFormat formatoDinero;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_grafica);

        dbHelper = new ConexionSQLite(this);
        formatoDinero = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

        tvTitulo = findViewById(R.id.tvTituloDetalle);
        tvMontoMaximo = findViewById(R.id.tvMontoMaximoDetalle);
        planoDetalle = findViewById(R.id.planoCartesianoDetalle);
        Button btnVolver = findViewById(R.id.btnVolverGrafica);

        btnVolver.setOnClickListener(v -> finish());

        String tipoGrafica = getIntent().getStringExtra("TIPO_GRAFICA");
        if (tipoGrafica != null && tipoGrafica.equals("DIARIA")) {
            configurarGraficaDiaria();
        } else {
            configurarGraficaSemanal();
        }
    }

    private void configurarGraficaDiaria() {
        tvTitulo.setText("ANÁLISIS DIARIO (POR HORAS)");
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        List<Double> valoresDiariosY = new ArrayList<>();
        List<String> etiquetasDiariasX = new ArrayList<>();

        // 🔥 FIX MATEMÁTICO: Arrancamos el acumulador en 0.0 de forma limpia.
        // Así, el primer registro de la base de datos ("Saldo Inicial") establecerá el punto de partida real en $1.500.000 exactos sin duplicarse.
        double saldoCalculado = 0.0;
        int contadorHoras = 1;
        boolean esPrimeraTransaccion = true;

        // Traemos las transacciones en orden cronológico (id ASC) para construir la línea del tiempo
        Cursor cDiario = db.rawQuery("SELECT concepto, monto, tipo FROM transacciones ORDER BY id ASC", null);

        while (cDiario.moveToNext()) {
            String concepto = cDiario.getString(0);
            double monto = cDiario.getDouble(1);
            String tipo = cDiario.getString(2);

            // Suma y resta consecutiva según la acción real registrada en el historial
            if (tipo.equals("Ingreso")) {
                saldoCalculado += monto;
            } else {
                saldoCalculado -= monto;
            }
            valoresDiariosY.add(saldoCalculado);

            // Extrae la hora si viene formateada en el concepto, de lo contrario la simula de forma consecutiva
            if (concepto.length() >= 5 && concepto.substring(0, 2).matches("\\d+") && concepto.contains("-")) {
                String horaExtraida = concepto.split("-")[0].trim();
                etiquetasDiariasX.add(horaExtraida);
            } else {
                if (esPrimeraTransaccion) {
                    etiquetasDiariasX.add("08:00 AM");
                } else {
                    etiquetasDiariasX.add(String.format(Locale.US, "%02d:00 PM", 12 + contadorHoras));
                    contadorHoras++;
                }
            }
            esPrimeraTransaccion = false;
        }
        cDiario.close();

        // Detector de picos para la escala y etiquetas informativas
        double maxSaldo = valoresDiariosY.isEmpty() ? 100000 : valoresDiariosY.get(0);
        for (double v : valoresDiariosY) {
            if (v > maxSaldo) maxSaldo = v;
        }

        tvMontoMaximo.setText("Saldo máximo alcanzado hoy: " + formatoDinero.format(maxSaldo));

        // Le agregamos un margen superior del 15% para que los picos respiren visualmente en el plano
        double techoEjeYDiario = maxSaldo * 1.15;

        // 🔥 FIX COMPILACIÓN: Enviamos exactamente los 3 argumentos nativos que tu PlanoCartesianoView espera ahora
        if (!valoresDiariosY.isEmpty()) {
            planoDetalle.setDatos(valoresDiariosY, etiquetasDiariasX, techoEjeYDiario);
        }
    }

    private void configurarGraficaSemanal() {
        tvTitulo.setText("TENDENCIA SEMANAL (CIERRES)");
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        List<Double> valoresSemanalesY = new ArrayList<>();
        List<String> etiquetasSemanalesX = new ArrayList<>();
        String[] diasSemana = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

        // 🔥 FIX MATEMÁTICO: Iniciamos el acumulador histórico sobre la base limpia en 0.0
        double saldoHistoricoAcumulado = 0.0;

        Cursor cSemanal = db.rawQuery("SELECT monto, tipo FROM transacciones ORDER BY id ASC", null);
        List<Double> variacionesDeBD = new ArrayList<>();
        List<String> tiposDeBD = new ArrayList<>();

        while (cSemanal.moveToNext()) {
            variacionesDeBD.add(cSemanal.getDouble(0));
            tiposDeBD.add(cSemanal.getString(1));
        }
        cSemanal.close();

        for (int i = 0; i < diasSemana.length; i++) {
            etiquetasSemanalesX.add(diasSemana[i]);

            if (i < variacionesDeBD.size()) {
                double monto = variacionesDeBD.get(i);
                String tipo = tiposDeBD.get(i);

                if (tipo.equals("Ingreso")) {
                    saldoHistoricoAcumulado += monto;
                } else {
                    saldoHistoricoAcumulado -= monto;
                }
            }
            valoresSemanalesY.add(saldoHistoricoAcumulado);
        }

        // Calibrador de escala semanal buscando el pico de balance
        double maxSaldo = valoresSemanalesY.isEmpty() ? 100000 : valoresSemanalesY.get(0);
        for (double v : valoresSemanalesY) {
            if (v > maxSaldo) maxSaldo = v;
        }

        tvMontoMaximo.setText("Saldo máximo de la semana: " + formatoDinero.format(maxSaldo));

        double techoEjeYSemanal = maxSaldo * 1.15;

        // 🔥 FIX COMPILACIÓN: Enviamos exactamente los 3 argumentos nativos que tu PlanoCartesianoView espera ahora
        if (!valoresSemanalesY.isEmpty()) {
            planoDetalle.setDatos(valoresSemanalesY, etiquetasSemanalesX, techoEjeYSemanal);
        }
    }
}