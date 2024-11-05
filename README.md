# pgi-les-hendelse-skatt
Les hendelser om pensjonsgivende inntekter fra SKE, og publiser hendelser til kafka topicen ```privat-pgi-hendelse```.
Tilstanden til sekvensnummeret blir lagret på ```privat-pgi-nextSekvensnummer```.

Dokumentasjon REST tjenester til SKE: [Skatteetatens API-er - Hendelser](https://skatteetaten.github.io/api-dokumentasjon/api/hendelser)

Pensjonsgivende inntekt for folketrygden hendelser API: [SwaggerHub](https://app.swaggerhub.com/apis/skatteetaten/pensjonsgivende-inntekt-for-folketrygden-hendelser-api) 


### Hente hendelser på nytt
Ved behov for å hente hendelser må man unngå at sekvensnummer leses fra tilstanden på topic
`privat-pgi-nextSekvensnummer`. Dette kan gjøres ved å sette miljøvariabelen `TILBAKESTILL_SEKVENSNUMMER` til `"true"`.
Her kan man alternativt også sette miljvariabelen `TILBAKESTILL_SEKVENSNUMMER_TIL` til `"yyyy-MM-dd"` man ønsker å hente
hendelser fra (første sekvensnummer fra og med angitt dato). Dersom sistnevnte variabel ikke er spesifisert vil
det første mulige sekvensnummer hentes (siden tidenes morgen).

**NB** For å unngå at man alltid tilbakestiller ved oppstart, må man oppdatere `TILBAKESTILL_SEKVENSNUMMER` 
til `"false"` så fort applikasjonen har startet å lese inn hendelser fra ønsket tidspunkt.

#### Bygge lokalt
For å bygge lokalt, så må man ha satt environment variablene GITHUB_ACTOR og GITHUB_TOKEN.
Generer nytt token her: https://github.com/settings/tokens. Husk å KUN gi den følgende tilgangen:

```read:packages Download packages from github package registry```.

Med tokenet generert så har jeg satt det opp slik i .zshrc/.bashrc
```
export GITHUB_ACTOR="username"
# Read only token for downloading github packages
export GITHUB_TOKEN="token"
```

#### Metrikker
Grafana dashboards brukes for å f.eks. monitorere minne, cpu-bruk og andre metrikker.
Se [pgi-les-hendelse-skatt grafana dasboard](https://grafana.adeo.no/d/qdcTS8JGz/pgi-les-hendelse-skatt)

#### Logging
[Kibana](https://logs.adeo.no/app/kibana) benyttes til logging. Søk på f.eks. ```application:pgi-les-hendelse-skatt AND envclass:q``` for logginnslag fra preprod.

#### Kontakt
Kontakt Team Pensjon Opptjening dersom du har noen spørsmål. Vi finnes blant annet på Slack, i kanalen [#pensjon-opptjening](https://nav-it.slack.com/archives/C01JWB604DP)
