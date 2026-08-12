# Zeus EQ Pro18

**Ecualizador paramétrico real** para Android.

## Características

- 10 bandas paramétricas completamente ajustables
- **Tipos de filtro por banda**:
  - **Peak** (campana)
  - **Low Shelf** / **High Shelf**
  - **Low Pass** / **High Pass**
  - **Notch**
  - **Band Pass**
- Frecuencia: **1 Hz → 30 000 Hz**
- Gain: **±30 dB**
- Q: **0.1 → 40**
- Clic en cualquier valor (FREQ / GAIN / Q) para editarlo manualmente
- Arrastra las bandas en el gráfico
- 3 secciones con flechas: `< Equalizer Pro18 >` → Crossover → Limiter
- Procesamiento real con **DynamicsProcessing** + **AudioEffect**
- Analizador de espectro de fondo
- Tile de Ajustes Rápidos
- Servicio en primer plano

## Compilar desde el celular (GitHub Actions)

Como no tienes PC, puedes compilar el APK directamente en GitHub:

### Pasos (todo desde el teléfono):

1. Entra a [github.com](https://github.com) e inicia sesión (o crea cuenta).
2. Toca el botón **+** → **New repository**.
3. Nombre: `ZeusEQPro18` (o el que quieras) → Create repository.
4. En el repositorio nuevo, toca **uploading an existing file** (o "Add file" → Upload files).
5. Sube **todo el contenido** de la carpeta del proyecto (o el ZIP descomprimido).
6. Escribe un mensaje de commit (ej: "Zeus EQ Pro18") y toca **Commit changes**.
7. Ve a la pestaña **Actions**.
8. Selecciona el workflow **Build Zeus EQ Pro18 APK**.
9. Toca **Run workflow** → **Run workflow**.
10. Espera 3-6 minutos.
11. Cuando termine en verde, entra al workflow → **Artifacts** → descarga **ZeusEQPro18-debug**.
12. Descomprime el ZIP y tendrás el archivo `.apk` listo para instalar.

> Si el workflow no aparece, asegúrate de haber subido la carpeta `.github/workflows/build_apk.yml`.

## Requisitos del dispositivo

- Android 9.0 (API 28) o superior
- Permisos de micrófono / audio (para el motor)

## Nota sobre el procesamiento real

`DynamicsProcessing` es la API oficial de Google. En muchos teléfonos (Samsung, Xiaomi, etc.) los efectos globales están limitados por el fabricante. Funciona mejor en ROMs cercanas a AOSP o cuando se adjunta a la sesión de una app de música.

---

**Zeus EQ Pro18**
