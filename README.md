# LS Manager API

## Prerequisiti

- JDK 21
- PostgreSQL

Il progetto usa il Maven Wrapper, quindi non richiede un'installazione globale
di Maven.

## Configurazione e avvio locale

Creare una configurazione locale partendo dal file privo di segreti reali:

```sh
cp .env.example .env
```

Impostare almeno `DB_PASSWORD` e un `JWT_SECRET` casuale. Spring carica
automaticamente `.env` sia quando l'applicazione viene avviata dalla directory
`ls-manager-api`, sia quando viene avviata dalla root del progetto:

```sh
./mvnw spring-boot:run
```

Se la directory di lavoro dell'IDE è diversa, impostare `ENV_FILE` con il
percorso assoluto del file `.env`.

Lo schema di sviluppo viene aggiornato automaticamente tramite
`spring.jpa.hibernate.ddl-auto=update`.

## Test

```sh
./mvnw test
```

I test attivano il profilo `test` e usano un database H2 in memoria isolato;
non leggono né modificano il database PostgreSQL di sviluppo.

## Creazione dell'amministratore iniziale

Il bootstrap è intenzionalmente escluso dalla suite ordinaria perché scrive sul
database configurato da `DB_URL`. Impostare `ADMIN_USERNAME` e
`ADMIN_PASSWORD` nel file `.env` e lanciarlo in modo esplicito dalla directory
`ls-manager-api`:

```sh
./mvnw -Dtest=AdminUserBootstrap test
```

Il comando crea l'utente se non esiste; se esiste, aggiorna password, ruolo
`ADMIN_ROLE` e stato attivo. La password viene salvata esclusivamente come hash
BCrypt e non viene stampata nei log.
