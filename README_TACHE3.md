# IFT3913 - Tâche 3 : automatisation des tests de sécurité (Semgrep)

Binôme : David STANESCU (`DavidStanescu13`) et Aness Lakhdar AZZOUZ (`ans1008`).

Cas d'étude : cinq modules de Tika (`tika-core`, `tika-serialization`, `tika-parser-ocr-module`, `tika-parser-pdf-module`, `tika-parser-microsoft-module`). Tout le travail est sur la branche `tache3`.

## Table des matières

1. [Workflow Semgrep](#1-workflow-semgrep)
2. [Rapport de base](#2-rapport-de-base)
3. [Les 7 vulnérabilités introduites](#3-les-7-vulnérabilités-introduites)
4. [Comparaison des rapports avant et après](#4-comparaison-des-rapports-avant-et-après)
5. [Humour : rickroll dans le CI](#5-humour--rickroll-dans-le-ci)
6. [Déclaration d'usage de l'intelligence artificielle](#6-déclaration-dusage-de-lintelligence-artificielle)
7. [Reproduire les résultats](#7-reproduire-les-résultats)

## 1. Workflow Semgrep

Fichier : [`.github/workflows/semgrep.yml`](.github/workflows/semgrep.yml).

### 1.1 Conception

| Choix | Justification |
|---|---|
| Mode diff : `semgrep scan --baseline-commit <réf> --error` | Le build doit échouer seulement si un commit **introduit** une nouvelle détection. Tika en contient déjà 3 (section 2) ; sans mode diff, elles feraient échouer tous les builds. |
| Commit de référence : base de la PR, ou commit précédent le push, ou `HEAD^` | C'est l'état du code juste avant le changement analysé. |
| `fetch-depth: 0` au checkout | Le commit de référence doit exister dans le runner. |
| Périmètre : les cinq modules du cas d'étude | Les consignes limitent le cas d'étude à ces modules. L'analyse de tout le dépôt avait donné 85 détections hors périmètre (`.github`, `tika-pipes`, etc.). |
| Règles : `p/java` | Tika est en Java. Ce sont les mêmes règles que pour le rapport de base, sans quoi le diff serait faussé. |
| Déclencheurs : PR vers `main`/`tache3`, push sur `tache3` et `tache3-**` | La PR bloque la fusion d'une nouvelle vulnérabilité ; le push donne un retour rapide sur nos branches de travail. |
| Métriques désactivées (`SEMGREP_SEND_METRICS=off`) | Pas d'envoi de données à Semgrep. |
| Rapport JSON en artefact | Permet de consulter les nouvelles détections après coup. |

### 1.2 Validation

| Cas | Attendu | Résultat | Preuve |
|---|---|---|---|
| Commit sans changement Java | build vert | vert (0 fichier analysé, 0 détection) | run `43754809935` |
| Commit qui ajoute une vulnérabilité | build rouge | TODO | TODO |
| PR propre vers `tache3` | build vert | TODO | TODO |
| PR avec vulnérabilité vers `tache3` | build rouge | TODO | TODO |

## 2. Rapport de base

Rapport : [`semgrep/baseline.json`](semgrep/baseline.json), généré sur le commit `be5779de7` (avant toute vulnérabilité).

Il contient 3 détections, toutes déjà présentes dans Tika :

| Fichier | Ligne | Règle |
|---|---|---|
| `tika-core/.../mime/MimeTypesReader.java` | 223 | XXE : `SAXParserFactory` sans `disallow-doctype-decl` |
| `tika-core/.../parser/NetworkParser.java` | 68 | socket non chiffré |
| `tika-core/.../utils/XMLReaderUtils.java` | 307 | XXE possible avec `XMLInputFactory` |

## 3. Les 7 vulnérabilités introduites

TODO : une sous-section par vulnérabilité, avec le fichier choisi et pourquoi, le type (CWE), pourquoi le code est vulnérable, la règle Semgrep et le résultat.

## 4. Comparaison des rapports avant et après

TODO : `semgrep/baseline.json` (3 détections) contre `semgrep/after.json` (10 attendues), avec la liste des 7 nouvelles.

## 5. Humour : rickroll dans le CI

TODO.

## 6. Déclaration d'usage de l'intelligence artificielle

TODO : outil utilisé, pour quelles parties, comment le résultat a été vérifié.

## 7. Reproduire les résultats

Rapport de base (depuis la racine du dépôt) :

```bash
pip install semgrep
semgrep scan --config p/java --json -o semgrep/baseline.json \
  tika-core tika-serialization \
  tika-parsers/tika-parsers-standard/tika-parsers-standard-modules/tika-parser-ocr-module \
  tika-parsers/tika-parsers-standard/tika-parsers-standard-modules/tika-parser-pdf-module \
  tika-parsers/tika-parsers-standard/tika-parsers-standard-modules/tika-parser-microsoft-module
```

Le rapport de base a été généré avec Semgrep 1.136.0. Le workflow installe la dernière version disponible.
