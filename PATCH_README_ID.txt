Glass Clock - patch tampilan tinggi ala iPhone

Isi patch:
- Widget hanya menggambar HH:mm; tanpa tanggal, cuaca, AM/PM, atau teks lain.
- Digit digambar menjadi bitmap transparan dan mengikuti tinggi widget.
- Widget mendukung resize horizontal dan vertikal, dengan ukuran awal portrait 3x4 sel.

Cara menerapkan di Termux dari folder proyek:
1) unzip -o ~/storage/downloads/GlassClock_iOS_Reference_Patch.zip -d ~/projects/widget
2) cd ~/projects/widget
3) git add -A
4) git commit -m "Render tall iOS-style resizable clock"
5) git push origin main
6) Tunggu GitHub Actions selesai, unduh artifact APK terbaru, instal, hapus widget lama, lalu tambahkan ulang.

Catatan: Launcher Samsung menentukan ukuran grid aktual. Setelah dipasang, tahan widget dan gunakan pegangan resize untuk mengatur lebar/tinggi.
