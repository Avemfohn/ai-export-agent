# TargetOut AI — Kullanıcı Arayüzü Rehberi

Bu belge, panelin (dashboard) sol menüsündeki her ekranı ve o ekranda
gördüğünüz bilgilerin ne anlama geldiğini açıklar. Durum (status) etiketleri
arayüzde İngilizce göründüğü için orijinal haliyle bırakılmış, yanına Türkçe
açıklaması eklenmiştir.

## Genel Bakış (Overview)

Panele girince ilk gördüğünüz ekran. Dört özet kart bulunur:

- **Total Leads (Toplam Aday)** — sisteme kayıtlı tüm adayların sayısı.
- **Warm Replies (Sıcak Yanıtlar)** — "ilgileniyorum" olarak sınıflandırılan
  yanıt sayısı.
- **Active Campaigns (Aktif Kampanyalar)** — durumu `ACTIVE` olan kampanya
  sayısı.
- **Scraping Jobs (Tarama İşleri)** — toplam tarama işi sayısı.

Rakamlar her sayfa yüklendiğinde canlı olarak sunucudan çekilir. Sunucuya o
anda ulaşılamazsa kartta rakam yerine tire (—) görünür; bu bir hata değil,
bağlantı sorununun göstergesidir.

## Adaylar (Leads)

Şirketinizin alım kriterlerine uyduğu için sistem tarafından eşleştirilmiş
tüm şirketlerin listesi. Tablo sütunları:

- **Company (Şirket)** — şirket adı.
- **Domain (Alan Adı)** — şirketin web sitesi alan adı.
- **Country (Ülke)** — şirketin bulunduğu ülke.
- **Sector (Sektör)** — faaliyet gösterdiği sektör.
- **Status (Durum)** — aşağıda açıklanan yaşam döngüsü durumlarından biri.
- **Score (Puan)** — yapay zekânın bu eşleşmeye verdiği güven puanı.

Bir satıra tıklayınca o adayın **detay sayfası** açılır: neden bu şekilde
puanlandığını açıklayan notlar, o adaya gönderilen e-postalar ve alınan
yanıtlar için ayrı bölümler görürsünüz.

**Durum (Status) değerleri ve anlamları:**

| Durum | Anlamı |
|---|---|
| `PENDING_APPROVAL` | Eşleşti, onayınız bekleniyor |
| `APPROVED` | Onaylandı, e-posta gönderimi için sırada |
| `REJECTED` | Reddedildi (kriterlere uymuyor) |
| `EMAIL_SENT` | İlk e-posta gönderildi, yanıt bekleniyor |
| `NO_RESPONSE` | Yanıt gelmedi |
| `INTERESTED` | İlgileniyor — sıcak aday |
| `NOT_INTERESTED` | İlgilenmiyor |
| `BOUNCED` | E-posta ulaşmadı (adres geçersiz vb.) |
| `CONVERTED` | Müşteriye dönüştü — hedefe ulaşıldı |

## Kampanyalar (Campaigns)

Kampanyalar, adayları ve tarama işlerini isimlendirilmiş bir hedef altında
gruplar (örn. "Ev Tekstili — Kuzey Amerika"). Her kart şunları gösterir:

- **İsim** ve **açıklama**.
- **Status (Durum)**: `DRAFT` (taslak), `ACTIVE` (aktif), `PAUSED`
  (duraklatıldı), `COMPLETED` (tamamlandı), `ARCHIVED` (arşivlendi).

Bir kampanyaya tıklayınca, o kampanya kapsamında gönderilen e-postaların
listelendiği detay sayfası açılır.

## Gönderimler (Outreach)

Sistemin gönderdiği (veya taslak olarak hazırladığı) her soğuk e-postanın
kaydı. Sütunlar:

- **To (Alıcı)** — e-posta adresi.
- **Subject (Konu)** — e-posta konusu.
- **Status (Durum)**: `DRAFT` (taslak) → `QUEUED` (sırada) → `SENT`
  (gönderildi), ya da `FAILED` (başarısız) / `BOUNCED` (geri döndü).
- **Sent At (Gönderim Zamanı)**.

## Yanıtlar (Responses)

"Sıcak yanıt" gelen kutusu. Bir gönderime gelen her yanıt burada listelenir:
gönderen kişi, renkli bir **niyet (intent) etiketi**, konu başlığı, mesaj
metninin bir önizlemesi ve alınma zamanı. Günlük olarak en çok bu sayfaya
bakacaksınız — gerçekten ilgilenen alıcılar önce burada görünür.

**Niyet (Classified Intent) etiketleri:**

| Etiket | Renk | Anlamı |
|---|---|---|
| `INTERESTED` | Yeşil | İlgileniyor |
| `NOT_INTERESTED` | Kırmızı | İlgilenmiyor |
| `NEEDS_INFO` | Sarı | Daha fazla bilgi istiyor |
| `OUT_OF_OFFICE` | Gri | Otomatik "ofis dışı" yanıtı |
| `UNSUBSCRIBE` | Kırmızı | Listeden çıkmak istiyor |
| `SPAM` | Kırmızı | Spam olarak işaretlendi |
| `UNKNOWN` | Gri | Sınıflandırılamadı |

## Bildirimler (Notifications)

Sistem uyarıları: sıcak yanıt bildirimleri (aynı zamanda WhatsApp'a da
gönderilir), yeni aday bildirimleri, tarama işi tamamlanma bildirimleri ve
geri dönen (bounce) e-posta uyarıları. Her bildirimde bir **kanal (channel)**
etiketi (Dashboard, WhatsApp veya Email), mesaj metni, okunup okunmadığı ve
gönderim zamanı görünür. Okunmamış bildirimler görsel olarak vurgulanır.

## Tarama İşleri (Scraping Jobs)

Her tarama çalışmasının kaydı:

- **Source (Kaynak)**: Google Maps, bir B2B dizini, bir fuar listesi yüklemesi
  veya manuel giriş.
- **Status (Durum)**: `PENDING` (bekliyor), `RUNNING` (çalışıyor),
  `COMPLETED` (tamamlandı), `FAILED` (başarısız), `CANCELLED` (iptal
  edildi).
- Bulunan şirket sayısı.
- Başarısız işler için hata mesajı.

## Ayarlar (Settings)

Şirketinizin **alım kriterlerinin** tutulduğu sayfa: hedef sektörler, hedef
ülkeler, minimum şirket büyüklüğü, hariç tutulacak anahtar kelimeler, tercih
edilen diller. Yapay zekâ, taranan her şirketi işte bu kriterlere göre
değerlendirir.

**Not:** Bu sayfa şu an yalnızca görüntüleme amaçlıdır (salt okunur); kriterleri
arayüzden düzenleme özelliği henüz eklenmemiştir.
