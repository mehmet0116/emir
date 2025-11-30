# 🍬 Şeker Patlatma Oyunu

Profesyonel seviye sistemi ve akıcı oyun mekaniği ile hazırlanmış eğlenceli bir şeker patlatma oyunu!

## 🎮 Özellikler

- **50 Farklı Seviye**: Her biri benzersiz zorluk ve hedeflerle
- **Akıcı Animasyonlar**: Profesyonel görsel efektler ve geçişler
- **Yıldız Sistemi**: Her seviyede 3 yıldıza kadar kazanın
- **Kombo Sistemi**: Ardışık eşleşmelerle puan çarpanı
- **Özel Şekerler**: Çizgili, paketli ve renk bombası şekerleri
- **İpucu Sistemi**: Sıkıştığınızda yardım alın
- **Karıştırma**: Geçerli hamle kalmadığında tahtayı karıştırın
- **Mobil Uyumlu**: Dokunmatik ekranlar için optimize edilmiş

## 🚀 Nasıl Oynanır

1. Oyunu başlatmak için **Oyna** butonuna tıklayın
2. İki komşu şekeri değiştirmek için sırayla tıklayın
3. 3 veya daha fazla aynı şekeri eşleştirin
4. Hedef puana ulaşarak seviyeyi tamamlayın
5. Daha fazla yıldız kazanmak için daha yüksek puanlar yapın

## 📱 Platformlar

### Web Versiyonu
- HTML5
- CSS3 (Animasyonlar ve Flexbox/Grid)
- Vanilla JavaScript (ES6+)
- Web Audio API (Ses efektleri)
- LocalStorage (İlerleme kaydetme)

### Android Versiyonu (Android Studio)
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Kotlin** ile geliştirilmiş
- WebView tabanlı hibrit uygulama
- Tam ekran oyun deneyimi

## 📁 Android Studio Kurulumu

1. Android Studio'yu açın
2. `File > Open` seçeneğini kullanın
3. `android` klasörünü seçin
4. Gradle senkronizasyonunu bekleyin
5. Bir emülatör veya fiziksel cihaz seçin
6. `Run` butonuna tıklayın

## 🎯 Seviye Sistemi

- **Hedef Puan**: Her seviyede belirli bir puana ulaşmanız gerekir
- **Hamle Limiti**: Sınırlı hamle ile hedefe ulaşın
- **Yıldız Eşikleri**: 
  - ⭐ 1 Yıldız: Minimum hedef puan
  - ⭐⭐ 2 Yıldız: Orta seviye puan
  - ⭐⭐⭐ 3 Yıldız: Maksimum puan

## 💡 İpuçları

- Kombo yapmak için birden fazla eşleşme planlayın
- 4'lü ve 5'li eşleşmeler bonus puan verir
- Kalan hamlelerin her biri bonus puana dönüşür
- 3 yıldız için maksimum skoru hedefleyin

## 📂 Dosya Yapısı

```
├── index.html          # Ana HTML dosyası (Web)
├── css/
│   └── style.css       # Tüm stiller
├── js/
│   └── game.js         # Oyun mantığı
├── android/            # Android Studio Projesi
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/sekerpatlatma/game/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   └── WebAppInterface.kt
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── values/
│   │   │   │   └── drawable/
│   │   │   ├── assets/     # Web dosyaları
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle
│   ├── build.gradle
│   ├── settings.gradle
│   └── gradle.properties
└── README.md           # Bu dosya
```

## 🎨 Ekran Görüntüleri

Oyun, mobil ve masaüstü cihazlarda mükemmel görünüm için tasarlanmıştır.

## 📝 Lisans

Bu proje açık kaynaklıdır ve serbestçe kullanılabilir.

---

🍬 İyi eğlenceler! 🍬