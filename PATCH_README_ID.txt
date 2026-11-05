GLASS CLOCK - CUSTOM GLASS DIGIT PATCH

Fitur:
- Custom vector digit 0-9 (bukan system font/Roboto/serif)
- Format 24 jam HH:mm saja
- Tanpa tanggal, cuaca, AM/PM, atau teks tambahan
- Efek kaca: translucent gradient, bright rim, soft glow dan shadow
- Render bitmap menyesuaikan lebar dan tinggi widget
- Resize horizontal dan vertical

Cara menerapkan di Termux:
1. Ekstrak ZIP ke root proyek ~/projects/widget:
   unzip -o ~/storage/downloads/GlassClock_Custom_Digit_Patch.zip -d ~/projects/widget
2. Commit dan push:
   cd ~/projects/widget
   git add -A
   git commit -m "Draw custom iOS-inspired glass clock digits"
   git push origin main
3. Tunggu GitHub Actions berhasil, unduh artifact APK terbaru.
4. Pasang APK, hapus widget lama, lalu tambahkan widget baru.

Catatan:
Desain digit dibuat khusus dengan vector path, bukan menggunakan font iOS proprietary.
Proporsi dan ukuran aktual tetap dibatasi oleh grid/launcher One UI. Perlu pengujian perangkat.
