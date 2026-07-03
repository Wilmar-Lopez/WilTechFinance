package com.example.wiltechfinance;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface; // Agregado para resaltar los títulos de los días
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.text.NumberFormat; // 🔥 Agregado para coordinar el formato de moneda oficial
import java.text.SimpleDateFormat; // Agregado para formatear fechas
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HistorialFragment extends Fragment {

    private LinearLayout contenedorTransacciones;
    private ConexionSQLite dbHelper;
    private int limiteActual = 10; // Carga inicial fija obligatoria
    private NumberFormat formatoDinero; // 🔥 Formateador coordinado

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View vista = inflater.inflate(R.layout.fragment_historial, container, false);

        dbHelper = new ConexionSQLite(getContext());
        // 🔥 Inicializado con la configuración local de Colombia para asegurar puntos y comas correctos
        formatoDinero = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

        contenedorTransacciones = vista.findViewById(R.id.listaTransaccionesContenedor);
        Button btnVerMas = vista.findViewById(R.id.btnVerMasTransacciones);

        cargarTransaccionesDesdeBD();

        // Tu lógica de paginación original para el botón de ver más
        btnVerMas.setOnClickListener(v -> {
            limiteActual += 10;
            cargarTransaccionesDesdeBD();
        });

        return vista;
    }

    // ACTUALIZACIÓN EN TIEMPO REAL AUTOMÁTICA
    @Override
    public void onResume() {
        super.onResume();
        // Fuerza el refresco completo de la lista apenas entras a la pestaña
        cargarTransaccionesDesdeBD();
    }

    public void cargarTransaccionesDesdeBD() {
        if (contenedorTransacciones == null) return;
        contenedorTransacciones.removeAllViews(); // Limpieza anti-duplicados

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Seleccionamos también la 'fecha' de la base de datos de manera descendente para ver lo más nuevo primero
        Cursor cursor = db.rawQuery("SELECT concepto, monto, tipo, fecha FROM transacciones ORDER BY id DESC LIMIT " + limiteActual, null);

        // Variable de control para saber cuándo insertar el separador de día
        String ultimaFechaAgrupada = "";

        while (cursor.moveToNext()) {
            String concepto = cursor.getString(0);
            double monto = cursor.getDouble(1);
            String tipo = cursor.getString(2);
            String fechaBD = cursor.getString(3); // Captura la fecha YYYY-MM-DD

            // LOGICA DE FECHAS: Compara y genera el título del día
            if (fechaBD != null && !fechaBD.equals(ultimaFechaAgrupada)) {
                ultimaFechaAgrupada = fechaBD;
                String encabezadoDia = calcularTextoDia(fechaBD);

                // Creamos un TextView elegante para que actúe de divisor en el Historial
                TextView tvSeparador = new TextView(getContext());
                tvSeparador.setText(encabezadoDia.toUpperCase());
                tvSeparador.setTextColor(Color.parseColor("#8C8C8C")); // Gris ejecutivo
                tvSeparador.setTextSize(14f);
                tvSeparador.setTypeface(null, Typeface.BOLD);
                tvSeparador.setPadding(10, 30, 10, 10); // Espacio superior para separar bloques
                contenedorTransacciones.addView(tvSeparador);
            }

            // Tus filas de transacciones originales con sus fuentes y colores intactos
            TextView textoFila = new TextView(getContext());
            textoFila.setTextSize(16f);
            textoFila.setPadding(25, 15, 10, 15); // Un poco de sangría a la derecha del separador

            // 🔥 MEJORADO: Ahora usa el formato de dinero oficial sincronizado para que se vea premium
            if (tipo.equals("Ingreso")) {
                textoFila.setText("➕ " + concepto + " : +" + formatoDinero.format(monto));
                textoFila.setTextColor(Color.GREEN);
            } else {
                textoFila.setText("➖ " + concepto + " : -" + formatoDinero.format(monto));
                textoFila.setTextColor(Color.parseColor("#E50914"));
            }
            contenedorTransacciones.addView(textoFila);
        }
        cursor.close();
    }

    /**
     * Compara las fechas dinámicamente obteniendo el día de hoy directo del sistema de tu Samsung.
     * Limpia horas y milisegundos para garantizar un cálculo exacto de la diferencia de días.
     */
    private String calcularTextoDia(String fechaString) {
        try {
            SimpleDateFormat formateadorEntrada = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date fechaTransaccion = formateadorEntrada.parse(fechaString);

            // Obtenemos el tiempo dinámico y real del celular
            Calendar calHoy = Calendar.getInstance();

            // Forzamos a limpiar horas, minutos y segundos para comparar días calendarios puros
            calHoy.set(Calendar.HOUR_OF_DAY, 0);
            calHoy.set(Calendar.MINUTE, 0);
            calHoy.set(Calendar.SECOND, 0);
            calHoy.set(Calendar.MILLISECOND, 0);

            Calendar calTransaccion = Calendar.getInstance();
            calTransaccion.setTime(fechaTransaccion);
            calTransaccion.set(Calendar.HOUR_OF_DAY, 0);
            calTransaccion.set(Calendar.MINUTE, 0);
            calTransaccion.set(Calendar.SECOND, 0);
            calTransaccion.set(Calendar.MILLISECOND, 0);

            // Calculamos la diferencia exacta en días naturales
            long diferenciaMilisegundos = calHoy.getTimeInMillis() - calTransaccion.getTimeInMillis();
            long diasDiferencia = diferenciaMilisegundos / (1000 * 60 * 60 * 24);

            if (diasDiferencia == 0) {
                return "Hoy";
            } else if (diasDiferencia == 1) {
                return "Ayer";
            } else if (diasDiferencia > 1 && diasDiferencia < 7) {
                // Muestra el nombre del día de la semana de forma automática
                SimpleDateFormat formatoDiaSemana = new SimpleDateFormat("EEEE", new Locale("es", "CO"));
                return formatoDiaSemana.format(fechaTransaccion);
            } else {
                // Si pasaron más de 7 días, muestra el formato de día y mes
                SimpleDateFormat formatoDiaMes = new SimpleDateFormat("d 'de' MMMM", new Locale("es", "CO"));
                return formatoDiaMes.format(fechaTransaccion);
            }
        } catch (Exception e) {
            return fechaString; // Si algo inesperado ocurre, muestra la fecha original
        }
    }
}