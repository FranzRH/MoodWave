# MoodWave 🌊
### Diario emocional y análisis de hábitos

MoodWave es una aplicación móvil Android que permite registrar diariamente el estado de ánimo, nivel de energía, actividades y reflexiones personales. A partir de estos registros, la aplicación genera estadísticas y descubre patrones que ayudan al usuario a comprender mejor sus hábitos y experiencias.

El proyecto busca ofrecer una herramienta de seguimiento personal, sin realizar diagnósticos médicos ni afirmar relaciones de causalidad.

## 👥 Equipo de desarrollo

- Franz (GitHub: [@FranzRH](https://github.com/FranzRH))
- Diego Antonio Hernández Tzompantzi

## ✨ Funcionalidades principales

### 1. Registro emocional
- Registro diario del estado de ánimo.
- Nivel de energía.
- Selección de actividades.
- Diario de reflexiones personales.
- Actividades personalizadas.
- Edición y eliminación de registros.

### 2. Seguimiento y calendario
- Consulta de registros anteriores.
- Organización por fechas.
- Visualización del historial emocional.

### 3. Estadísticas y análisis
- Estadísticas de estado de ánimo y energía.
- Tendencias de 7, 30 y 90 días.
- Representaciones gráficas.
- Comparación de actividades con el estado de ánimo.
- Correlaciones y diferencias respecto al promedio general.
- Interpretaciones automáticas de resultados.

### 4. Inteligencia artificial: descubrimiento de patrones
Se implementó el algoritmo K-means para agrupar registros con características similares.

El análisis utiliza:
- Estado de ánimo.
- Nivel de energía.
- Actividades realizadas.

También incorpora:
- Mínimo de 14 días para iniciar el análisis.
- Mínimo de 5 registros por patrón.
- Evaluación mediante Silhouette Score.
- Comparación contra una línea base (baseline).
- Explicabilidad de los patrones encontrados.
- Modo de demostración con 90 días de datos sintéticos.

### 5. Reportes PDF
- Generación de reportes mensuales.
- Estadísticas y gráficas.
- Interpretaciones y patrones detectados.

### 6. Recordatorios
- Configuración de recordatorios diarios.
- Selección de horario.
- Notificaciones locales.

### 7. Respaldo de información
- Exportación de registros en formato JSON.
- Importación de respaldos.
- Validación del archivo antes de restaurar.
- Respaldo versionado.
- Exclusión de información de seguridad como PIN y configuración biométrica.

### 8. Seguridad
- Protección mediante PIN.
- Autenticación biométrica.
- Persistencia local de información.

## 🛠️ Tecnologías utilizadas

- Kotlin
- Android Studio
- Jetpack Compose
- Material 3
- Room
- DataStore
- WorkManager
- BiometricPrompt
- Gradle

## 🏗️ Arquitectura general

El proyecto organiza sus responsabilidades en diferentes módulos:

- `analytics`: cálculos estadísticos, correlaciones y descubrimiento de patrones.
- `data`: acceso y almacenamiento de información local.
- `screens`: interfaces de usuario.
- `notifications`: programación y ejecución de recordatorios.
- `reports`: generación de reportes PDF.
- `backup`: exportación e importación de respaldos.

## 📱 Instalación y ejecución

1. Descargar o clonar este repositorio.
2. Abrir el proyecto desde Android Studio.
3. Esperar la sincronización de Gradle.
4. Conectar un dispositivo Android o iniciar un emulador.
5. Ejecutar el proyecto mediante Run.

Para generar el APK:

`Build → Build Bundle(s) / APK(s) → Build APK(s)`

## 🔒 Privacidad y funcionamiento

MoodWave utiliza almacenamiento local para conservar los registros del usuario.

Las funciones principales pueden utilizarse sin conexión a internet. La aplicación no incluye actualmente sincronización en la nube ni integración con Health Connect.

Los datos sintéticos del modo de demostración se utilizan para evaluar y mostrar el funcionamiento del análisis de patrones, sin confundirse con los registros personales.

## 🎥 Demostración

Video de funcionamiento de la aplicación:

[Agregar aquí el enlace al video de demostración]

## 📦 APK

[Agregar aquí el enlace de descarga del APK]

## 📌 Consideraciones

MoodWave es una herramienta de seguimiento y análisis personal. Sus resultados muestran patrones y asociaciones presentes en los registros, pero no representan diagnósticos médicos ni relaciones causales.

---

**MoodWave — Conoce tus emociones, descubre tus patrones.**
