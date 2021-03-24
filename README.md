# pgi-les-hendelse-skatt
Les hendelser om pensjonsgivende inntekter fra SKE, og publiser hendelser til kafka topicen ```privat-pgi-hendelse```.
Tilstanden til sekvensnummeret blir lagret på ```privat-pgi-nextSekvensnummer```.

Dokumentasjon REST tjenester til SKE: [HendelseListe API](https://skatteetaten.github.io/datasamarbeid-api-dokumentasjon/reference_feed.html)



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
