# Mies - Energy Portfolio Backend ⚡

**Mies** è un backend robusto e scalabile sviluppato con **Quarkus** e **Java 21**, progettato per la gestione avanzata di portafogli energetici. Il sistema centralizza l'acquisizione, l'analisi e la gestione delle bollette (Elettricità e Gas), offrendo strumenti potenti per il parsing dei documenti e il calcolo dei costi.

## 🚀 Funzionalità Principali

### 📄 Parsing e Acquisizione Dati
Il sistema integra librerie avanzate per estrarre automaticamente dati strutturati da diverse fonti:
- **PDFBox**: Estrazione dati da bollette in formato PDF.
- **Apache POI**: Elaborazione di file Excel per report e importazioni massive.
- **JSoup**: Parsing di contenuti HTML.

### 📊 Gestione POD e Consumi
- Gestione completa dei **Punti di Prelievo (POD)**.
- Analisi dettagliata dei consumi per fasce orarie (**F1, F2, F3**).
- Monitoraggio di Energia Attiva, Reattiva (Induttiva/Capacitiva) e Potenza.

### 🔄 Motore di Calcolo e Rettifiche
- Logiche complesse per il **Ricalcolo** delle bollette.
- Gestione di conguagli, rettifiche trimestrali/annuali e calcolo penali (es. per energia reattiva).
- Storico letture e costi mensilizzati.

### 🛠️ Architettura Tecnica
- **Database**: MySQL con Hibernate ORM Panache per una persistenza dati efficiente.
- **Cloud Ready**: Integrazione nativa con **Azure** (Identity, Core).
- **Scheduling**: Job pianificati gestiti tramite **Quartz Scheduler** per automazioni periodiche.
- **Notifiche**: Servizio di invio email integrato (Quarkus Mailer).

---

## 🛠️ Stack Tecnologico

| Tecnologia | Descrizione |
|---|---|
| **Java 21** | Linguaggio Core (LTS) |
| **Quarkus** | Framework "Supersonic Subatomic Java" |
| **MySQL** | Database Relazionale |
| **Hibernate Panache** | ORM semplificato |
| **Apache POI / PDFBox** | Document Processing |
| **Maven** | Build Tool |

---

## 💻 Guida all'Avvio

### Prerequisiti
- JDK 21+ installato
- Docker (opzionale, per database locale)

### Modalità Sviluppo (Dev Mode)
Puoi avviare l'applicazione in modalità dev con live reload abilitato:

```shell script
./mvnw compile quarkus:dev
```

> **Nota:** La Dev UI di Quarkus è disponibile a [http://localhost:8080/q/dev/](http://localhost:8080/q/dev/).

### Preparazione Pacchetto
Per compilare l'applicazione:

```shell script
./mvnw package
```
L'artefatto verrà generato in `target/quarkus-app/`.

### Esecuzione Nativa
Per creare un eseguibile nativo (richiede GraalVM):

```shell script
./mvnw package -Dnative
```

---

## 📁 Struttura del Progetto

Il codice sorgente è organizzato secondo lo standard Maven:
- `src/main/java`: Sorgenti Java (Logica di business, Entity, Repository).
- `src/main/resources`: Configurazioni (`application.properties`) e risorse.

---

_Project Mies - Energy Management System_
