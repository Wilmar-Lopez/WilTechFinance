package com.example.wiltechfinance;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ConexionSQLite extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "WilTechFinance.db";
    // 🔥 EDITADO: Subimos a versión 7 para forzar la actualización limpia con el mapeo de horas cronológicas
    private static final int DATABASE_VERSION = 7;

    public ConexionSQLite(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Tabla principal de usuarios (Mantiene tus credenciales y el saldo en tiempo real)
        db.execSQL("CREATE TABLE usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT, credencial TEXT, pin TEXT, saldo REAL)");

        // 2. Tabla de transacciones (Se mantiene la columna 'fecha' tipo TEXT para agrupar dinámicamente)
        db.execSQL("CREATE TABLE transacciones (id INTEGER PRIMARY KEY AUTOINCREMENT, concepto TEXT, monto REAL, tipo TEXT, fecha TEXT)");

        // 3. Registro inicial de tu usuario Prueba Tech
        db.execSQL("INSERT INTO usuarios (credencial, pin, saldo) VALUES ('Prueba@tech.com', '1234', 2350000.00)");

        // ======================================================================================
        // 🔥 CAPTURA DE FECHA VIVA EN TIEMPO REAL 🔥
        // ======================================================================================
        String hoy = obtenerFechaActual();

        // ======================================================================================
        // 🔥 SET DE PRUEBAS COMPLETO CRONOLÓGICO CON HORAS ASIGNADAS PARA EL PLANO 🔥
        // ======================================================================================

        // 🗓️ BALANCE DE APERTURA: Arranca el día flotando en $1.500.000 (No en cero)
        db.execSQL("INSERT INTO transacciones (concepto, monto, tipo, fecha) VALUES ('08:00 AM - Saldo Inicial de Cuenta', 1500000.00, 'Ingreso', '" + hoy + "')");

        // 🗓️ BLOQUE MAÑANA: Subidas y bajadas consecutivas
        db.execSQL("INSERT INTO transacciones (concepto, monto, tipo, fecha) VALUES ('09:00 AM - Pago de Nómina Freelance', 900000.00, 'Ingreso', '" + hoy + "')");
        db.execSQL("INSERT INTO transacciones (concepto, monto, tipo, fecha) VALUES ('11:00 AM - Compra herramientas de trabajo', 350000.00, 'Gasto', '" + hoy + "')");
        db.execSQL("INSERT INTO transacciones (concepto, monto, tipo, fecha) VALUES ('12:00 PM - Pago servicios PruebaTech', 120000.00, 'Gasto', '" + hoy + "')");

        // 🗓️ BLOQUE TARDE: Más movimientos distribuidos por horas
        db.execSQL("INSERT INTO transacciones (concepto, monto, tipo, fecha) VALUES ('01:00 PM - Soporte técnico servidores', 450000.00, 'Ingreso', '" + hoy + "')");
        db.execSQL("INSERT INTO transacciones (concepto, monto, tipo, fecha) VALUES ('02:00 PM - Venta Licencia Software', 200000.00, 'Ingreso', '" + hoy + "')");
        db.execSQL("INSERT INTO transacciones (concepto, monto, tipo, fecha) VALUES ('03:00 PM - Almuerzo de negocios', 45000.00, 'Gasto', '" + hoy + "')");

        db.execSQL("INSERT INTO transacciones (concepto, monto, tipo, fecha) VALUES ('04:00 PM - Pago suscripción AWS', 180000.00, 'Gasto', '" + hoy + "')");
        db.execSQL("INSERT INTO transacciones (concepto, monto, tipo, fecha) VALUES ('05:00 PM - Reparación de PC Cliente', 80000.00, 'Gasto', '" + hoy + "')");
        db.execSQL("INSERT INTO transacciones (concepto, monto, tipo, fecha) VALUES ('06:00 PM - Compra de papelería', 35000.00, 'Gasto', '" + hoy + "')");

        db.execSQL("INSERT INTO transacciones (concepto, monto, tipo, fecha) VALUES ('07:00 PM - Abono proyecto Web', 300000.00, 'Ingreso', '" + hoy + "')");
        db.execSQL("INSERT INTO transacciones (concepto, monto, tipo, fecha) VALUES ('08:00 PM - Tanqueo Gasolina Carro', 90000.00, 'Gasto', '" + hoy + "')");
        db.execSQL("INSERT INTO transacciones (concepto, monto, tipo, fecha) VALUES ('09:00 PM - Cena de celebración', 110000.00, 'Gasto', '" + hoy + "')");

        db.execSQL("INSERT INTO transacciones (concepto, monto, tipo, fecha) VALUES ('10:00 PM - Bono por cumplimiento', 250000.00, 'Ingreso', '" + hoy + "')");
        db.execSQL("INSERT INTO transacciones (concepto, monto, tipo, fecha) VALUES ('11:00 PM - Asesoría TI Express', 150000.00, 'Ingreso', '" + hoy + "')");
        db.execSQL("INSERT INTO transacciones (concepto, monto, tipo, fecha) VALUES ('11:59 PM - Gasto de prueba actual', 390000.00, 'Gasto', '" + hoy + "')");

        // ======================================================================================
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS usuarios");
        db.execSQL("DROP TABLE IF EXISTS transacciones");
        onCreate(db);
    }

    // 🔥 MÉTODO UTILITARIO CENTRAL: Extrae la fecha real del sistema del teléfono
    public static String obtenerFechaActual() {
        SimpleDateFormat formateador = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return formateador.format(new Date());
    }
}