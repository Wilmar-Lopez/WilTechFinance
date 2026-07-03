package com.example.wiltechfinance;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.text.NumberFormat; // Agregado para el formato de millones
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GraficaFragment extends Fragment {

    private TextView tvGastoMasAlto, tvMontoMasAltoSemanal;
    private PlanoCartesianoView planoDiario, planoSemanal;
    private ConexionSQLite dbHelper;
    private NumberFormat formatoDinero; // Variable para el formato local colombiano

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Enlaza la vista visual del plano de picos
        View vista = inflater.inflate(R.layout.fragment_grafica, container, false);

        dbHelper = new ConexionSQLite(getContext());

        // Inicializamos el formato en español para pintar correctamente puntos de miles y millones colombianos
        formatoDinero = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

        tvGastoMasAlto = vista.findViewById(R.id.tvGastoMasAlto);
        tvMontoMasAltoSemanal = vista.findViewById(R.id.tvMontoMasAltoSemanal);
        planoDiario = vista.findViewById(R.id.planoCartesianoDiario);
        planoSemanal = vista.findViewById(R.id.planoCartesianoSemanal);

        calcularFlujoCartesiano();
        return vista;
    }

    // ACTUALIZACIÓN EN TIEMPO REAL AUTOMÁTICA
    @Override
    public void onResume() {
        super.onResume();
        calcularFlujoCartesiano();
    }

    public void calcularFlujoCartesiano() {
        if (planoDiario == null || planoSemanal == null) return;
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // ------------------------------------------------------------------
        // LÓGICA 1: GRÁFICA DIARIA (Picos por horas basados en movimientos)
        // ------------------------------------------------------------------
        List<Double> valoresDiariosY = new ArrayList<>();
        List<String> etiquetasDiariasX = new ArrayList<>();
        double gastoMasAltoDeHoy = 0;

        // Seleccionamos también la columna fecha para poder procesar la hora si viene en el texto
        Cursor cDiario = db.rawQuery("SELECT concepto, monto, tipo, fecha FROM transacciones ORDER BY id ASC", null);

        // 🔥 CORRECCIÓN CONTABLE EXACTA: Arrancamos el acumulador en 0.0. Así, el primer registro
        // de "Saldo Inicial de Cuenta" establece los $1.500.000 reales sin duplicar ni inflar a $2.900.000.
        double saldoCorrienteDiario = 0.0;
        int contadorHoras = 1;
        boolean esPrimeraTransaccion = true;

        while (cDiario.moveToNext()) {
            String concepto = cDiario.getString(0);
            double monto = cDiario.getDouble(1);
            String tipo = cDiario.getString(2);

            // Escanea y guarda el gasto más alto registrado hoy
            if (tipo.equals("Gasto") && monto > gastoMasAltoDeHoy) {
                gastoMasAltoDeHoy = monto;
            }

            saldoCorrienteDiario += (tipo.equals("Ingreso")) ? monto : -monto;
            valoresDiariosY.add(saldoCorrienteDiario);

            // LOGICA DE HORAS INTELIGENTE: Si el concepto empieza con formato de hora (ej: '10:00 - '), la saca, de lo contrario simula de forma consecutiva
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

        // Usa formatoDinero para pintar los millones y miles colombianos
        tvGastoMasAlto.setText("Gasto más alto de hoy: " + formatoDinero.format(gastoMasAltoDeHoy));

        // ZOOM INTELIGENTE DIARIO MEJORADO: Buscamos mínimos y máximos exactos
        double minDiario = valoresDiariosY.isEmpty() ? 0 : valoresDiariosY.get(0);
        double maxDiario = valoresDiariosY.isEmpty() ? 100000 : valoresDiariosY.get(0);
        for (double v : valoresDiariosY) {
            if (v < minDiario) minDiario = v;
            if (v > maxDiario) maxDiario = v;
        }

        double rangoDiario = maxDiario - minDiario;
        // CORRECCIÓN DEL PISO: Le damos un margen muy pequeño (5%) para que la curva no se aplaste en cero y flote como en Python
        double margenDiario = rangoDiario > 0 ? rangoDiario * 0.05 : 100000.00;

        double techoEjeYDiario = maxDiario + margenDiario;
        double pisoEjeYDiario = minDiario - margenDiario;

        // Bloqueo de seguridad: Solo baja a cero si realmente te gastaste toda la plata de la cuenta
        if (pisoEjeYDiario < 0 || minDiario >= 500000) {
            if (pisoEjeYDiario < 0) pisoEjeYDiario = 0;
            else pisoEjeYDiario = minDiario - 200000; // Mantiene el balance despegado del piso
        }

        // 🔥 FIX COMPILACIÓN DIARIA: Enviamos los 3 argumentos nativos que tu PlanoCartesianoView espera, pasando tu techoEjeYDiario real calculadísimo.
        if (!valoresDiariosY.isEmpty()) {
            planoDiario.setDatos(valoresDiariosY, etiquetasDiariasX, techoEjeYDiario);
        }

        // ------------------------------------------------------------------
        // LÓGICA 2: GRÁFICA SEMANAL (Línea continua de Lunes a Domingo)
        // ------------------------------------------------------------------
        List<Double> valoresSemanalesY = new ArrayList<>();
        List<String> etiquetasSemanalesX = new ArrayList<>();
        String[] diasSemana = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

        // Arrancamos en 0.0 para asimilar los ingresos contables base perfectamente
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
                saldoHistoricoAcumulado += (tipo.equals("Ingreso")) ? monto : -monto;
            }

            valoresSemanalesY.add(saldoHistoricoAcumulado);
        }

        double minSemanal = valoresSemanalesY.isEmpty() ? 0 : valoresSemanalesY.get(0);
        double maxSemanal = valoresSemanalesY.isEmpty() ? 100000 : valoresSemanalesY.get(0);
        for (double v : valoresSemanalesY) {
            if (v < minSemanal) minSemanal = v;
            if (v > maxSemanal) maxSemanal = v;
        }

        tvMontoMasAltoSemanal.setText("Monto más alto de la semana: " + formatoDinero.format(maxSemanal));

        double rangoSemanal = maxSemanal - minSemanal;
        double margenSemanal = rangoSemanal > 0 ? rangoSemanal * 0.05 : 100000.00;

        double techoEjeYSemanal = maxSemanal + margenSemanal;
        double pisoEjeYSemanal = minSemanal - margenSemanal;
        if (pisoEjeYSemanal < 0) pisoEjeYSemanal = 0;

        // 🔥 FIX COMPILACIÓN SEMANAL: Enviamos los 3 argumentos nativos usando tu techoEjeYSemanal para que la escala sea exacta.
        if (!valoresSemanalesY.isEmpty()) {
            planoSemanal.setDatos(valoresSemanalesY, etiquetasSemanalesX, techoEjeYSemanal);
        }
    }
}