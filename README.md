# ChatL — Fabric 1.21.1 istemcisi

Java 21 · Fabric Loader 0.16.5 · Yarn `1.21.1+build.3` · Fabric API `0.105.0+1.21.1`

## Özellikler
- **Yalnızca 5 modül:** AimAssist, TriggerBot, Velocity, ElytraFix, FireworkFix. Başka modül yok.
- **O tuşu:** ClickGUI (Modules / Config / Friends / HUD sekmeleri, Search, kategori filtresi).
  - Sol tık: modülü aç/kapat · **Sağ tık (veya ⋮): modülün ayarları** · ESC: geri/kapat.
  - Ayar satırları: anahtar, kaydırıcı (sürükle), mod seçici (sol/sağ tık), tuş ataması.
  - Altta oyuncunun gerçek skin'i + kullanıcı adı; tıklayınca hesap/sürüm bilgisi.
- **P tuşu:** bağlantı ve uyumluluk paneli (sunucu, sürümler, canlı testler, modül durumu, günlük).
- **Friend Config:** ekle/kaldır, A-Z sıralı, büyük-küçük harf duyarsız arama, boş durum ve hata mesajları.
  Arkadaşlar hedef gösteriminden hariç tutulur.
- Tuşlar Seçenekler → Tuş Atamaları → ChatL altında değiştirilebilir.

## Modüllerin gerçek kapsamı (dürüst tanım)
| Modül | Ne yapar | Ne YAPMAZ |
|---|---|---|
| AimAssist | En yakın oyuncunun mesafe/yaw/pitch farkını HUD'da gösterir | Kamerayı çevirmez |
| TriggerBot | Saldırı cooldown çubuğu, "hazır" göstergesi, test modu | Saldırı göndermez |
| Velocity | Hasar anındaki yatay/dikey hızı zaman damgalı kaydeder (varsayılan: yalnızca yerel dünya) | Knockback'i iptal/azaltmaz |
| ElytraFix | Elytra/uçuş durumu, geçersiz durum logu, dayanıklılık uyarısı | Hareketi değiştirmez |
| FireworkFix | Hotbar fişek durumu, yanlış slot uyarısı, ElytraFix olayını dinler | Eşya kullanımını değiştirmez |

Anti-cheat atlatma, tespitten gizlenme ve knockback iptali **yoktur**.

## Kurulum / derleme
```bash
./gradlew build        # ilk calistirmada gradle-wrapper.jar otomatik indirilir
./gradlew runClient    # gelistirme istemcisi
```
Windows: `gradlew.bat build`. Cikti: `build/libs/chatl-1.0.0.jar`

### GitHub
Repoya yukle (`git add . && git commit && git push`). **Actions** sekmesi `build` is akisini calistirir,
jar `chatl-jar` artifact'i olarak indirilir. Wrapper jar'i repoda yoksa is akisi kendisi uretir.

Jar'ı `.minecraft/mods` içine, Fabric API ile birlikte koy.

Ayarlar `config/chatl/` altında: `modules.json`, `gui.json`, `friends.json`.
Bozuk dosya `.bad` olarak yedeklenir, varsayılanlara dönülür.

## Test
`./gradlew test` — ayar/JSON, arama, arkadaş listesi, olay sistemi ve bozuk dosya testleri.
GitHub Actions: `.github/workflows/build.yml`.
