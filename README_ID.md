# Glass Clock — widget jam transparan untuk Samsung Galaxy A16

Proyek Android native Kotlin yang membuat widget jam digital transparan bergaya tipografi iPhone. Widget dapat di-resize horizontal dan vertikal dari launcher Samsung One UI. Ukuran jam menyesuaikan area yang diberikan launcher.

## Fitur
- Jam mengikuti format waktu sistem (12/24 jam dan locale perangkat).
- Hari, tanggal, dan bulan mengikuti locale perangkat.
- Latar widget transparan dengan bayangan halus agar teks tetap terbaca.
- Resize horizontal dan vertikal melalui Home Screen.
- Ukuran angka jam menyesuaikan ukuran widget.
- Meminta launcher menambahkan widget dari tombol di aplikasi.
- Refresh waktu menggunakan alarm inexact setiap menit; Android dapat menunda alarm demi penghematan baterai.
- Tidak meminta izin internet, lokasi, atau akses data pribadi.

## Kompatibilitas dan build
- minSdk 26 (Android 8.0+)
- compileSdk 35
- Android Gradle Plugin 8.7.3, Gradle 8.9, JDK 17

## Build melalui GitHub Actions
1. Buat repository GitHub bernama `GlassClockWidget` (Private boleh).
2. Upload seluruh isi folder proyek ini ke root repository, termasuk folder `.github` dan `app`.
3. Commit ke branch `main`.
4. Buka tab **Actions** → **Build Glass Clock APK** → **Run workflow**.
5. Setelah selesai, buka run yang berhasil dan unduh artifact **Glass-Clock-debug-APK**.
6. Ekstrak artifact ZIP dan pasang `app-debug.apk` pada Samsung A16.

## Pasang widget di Samsung
1. Buka aplikasi Glass Clock sekali.
2. Tekan tombol **Tambah widget ke Home Screen**, atau tekan lama area kosong Home Screen.
3. Pilih **Widget** → **Glass Clock** → tambahkan.
4. Tekan lama widget → **Resize/Ubah ukuran**.
5. Tarik pegangan bawah untuk memperpanjang widget. Angka jam akan menyesuaikan ruang yang tersedia.

## Catatan teknis
- Widget Android di-resize oleh launcher. Aplikasi meminta dukungan resize horizontal dan vertikal melalui AppWidgetProviderInfo.
- Transparansi berarti tidak ada panel latar yang digambar aplikasi. Keterbacaan teks dapat berbeda menurut wallpaper; bayangan teks disediakan untuk membantu kontras.
- Penyegaran menit menggunakan alarm inexact yang hemat baterai. Waktu refresh dapat sedikit tertunda oleh pengelolaan daya Android.
- Ini adalah proyek sumber. APK belum dikompilasi atau diuji pada perangkat fisik dalam sesi pembuatan ini.
