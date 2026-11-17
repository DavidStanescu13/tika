# IFT3913 - Tâche 3 : automatisation des tests de sécurité (Semgrep)

Binôme : David STANESCU (`DavidStanescu13`) et Aness Lakhdar AZZOUZ (`ans1008`).

Cas d'étude : cinq modules de Tika (`tika-core`, `tika-serialization`, `tika-parser-ocr-module`, `tika-parser-pdf-module`, `tika-parser-microsoft-module`).

Le workflow, le rapport de base et ce README sont sur la branche `tache3`. Les 7 vulnérabilités sont sur la branche `tache3-vulns` (une par-dessus l'autre à partir de `tache3`), proposées à la fusion par la [PR #2](https://github.com/DavidStanescu13/tika/pull/2).

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
| Commit qui ajoute une vulnérabilité | build rouge | rouge (3 nouvelles détections) | run `44004471064` (push des vulnérabilités 5 à 7 sur `tache3-vulns`) |
| PR propre vers `tache3` | build vert | vert | run `43766511781` (PR #1 : ajout d'un test, aucune détection Semgrep) |
| PR avec vulnérabilité vers `tache3` | build rouge | rouge (7 détections) | run `44006559348` (PR #2) |

## 2. Rapport de base

Rapport : [`semgrep/baseline.json`](semgrep/baseline.json), généré sur le commit `be5779de7` (avant toute vulnérabilité).

Il contient 3 détections, toutes déjà présentes dans Tika :

| Fichier | Ligne | Règle |
|---|---|---|
| `tika-core/.../mime/MimeTypesReader.java` | 223 | XXE : `SAXParserFactory` sans `disallow-doctype-decl` |
| `tika-core/.../parser/NetworkParser.java` | 68 | socket non chiffré |
| `tika-core/.../utils/XMLReaderUtils.java` | 307 | XXE possible avec `XMLInputFactory` |

## 3. Les 7 vulnérabilités introduites

Chaque vulnérabilité est ajoutée dans une méthode nouvelle ou un chemin de repli, pour ne pas casser le code existant (les tests du cas d'étude passent toujours, section 1.2). Les lignes citées sont celles de `tache3-vulns`.

### 3.1 Désérialisation Jackson — CWE-502
Fichier : `tika-serialization/.../serialization/JsonMetadata.java`, nouvelle méthode `fromJsonAsMap`. Elle lit des métadonnées libres dans une `Map` et, pour conserver le type Java des valeurs, appelle `enableDefaultTyping()` avant `readValue`. Avec le *default typing*, c'est le JSON qui nomme la classe à instancier : un document piégé fait instancier une classe arbitraire. Règle : `jackson-unsafe-deserialization`.

### 3.2 Injection SQL — CWE-89
Fichier : `tika-core/.../io/TikaInputStream.java`, nouvelle surcharge `get(Connection, table, documentId, metadata)`. La requête est bâtie par concaténation (`"... WHERE id = '" + documentId + "'"`). Un identifiant comme `x' OR '1'='1` change le sens de la requête. Règle : `formatted-sql-string`.

### 3.3 IV statique en CBC — CWE-329
Fichier : `tika-core/.../parser/CryptoParser.java`, constante `DEFAULT_IV`. Quand le contexte ne fournit pas de paramètres, le déchiffrement CBC retombe sur cet IV fixe codé en dur. Un IV constant rend le chiffrement déterministe (même clair → même chiffré). Règle : `no-static-initialization-vector`.

### 3.4 Contexte TLS faible — CWE-326
Fichier : `tika-core/.../parser/NetworkParser.java`, schéma `telnets` (méthode `openSecureSocket`). Le socket est ouvert avec `SSLContext.getInstance("TLSv1")`. TLS 1.0 est obsolète et considéré comme faible. Règle : `weak-ssl-context`.

### 3.5 Repli sur MD5 — CWE-328
Fichier : `tika-core/.../digest/InputStreamDigester.java`. Si l'algorithme demandé est absent de la JVM, le code retombe sur `MessageDigest.getInstance("MD5")`. MD5 est cassé (collisions) et ne convient pas pour l'intégrité. Règle : `use-of-md5`.

### 3.6 XXE — CWE-611
Fichier : `tika-core/.../utils/XMLReaderUtils.java`, nouvelle méthode `buildDOMWithEntities`. Contrairement aux fabriques durcies du reste du fichier, elle crée une `DocumentBuilderFactory` qui développe les entités et ne désactive pas le DOCTYPE. Une entité externe permet alors la lecture de fichiers locaux / SSRF. Règle : `documentbuilderfactory-disallow-doctype-decl-missing`.

### 3.7 Mode de chiffrement ECB — CWE-327
Fichier : `tika-core/.../parser/CryptoParser.java`, constante `DEFAULT_TRANSFORMATION = "AES/ECB/PKCS5Padding"`. Quand aucune transformation n'est fournie, le parseur utilise AES en mode ECB, qui chiffre chaque bloc indépendamment et laisse apparaître les motifs du clair. Règle : `use-of-aes-ecb`.

## 4. Comparaison des rapports avant et après

Le rapport de base ([`semgrep/baseline.json`](semgrep/baseline.json), commit `b3d595c`) contient 3 détections, toutes préexistantes (section 2).

En analysant `tache3-vulns` en mode diff contre cette base (`--baseline-commit b3d595c`), Semgrep signale exactement 7 nouvelles détections, une par vulnérabilité :

| Fichier | Règle | CWE |
|---|---|---|
| `JsonMetadata.java` | `jackson-unsafe-deserialization` | 502 |
| `TikaInputStream.java` | `formatted-sql-string` | 89 |
| `CryptoParser.java` | `no-static-initialization-vector` | 329 |
| `CryptoParser.java` | `use-of-aes-ecb` | 327 |
| `NetworkParser.java` | `weak-ssl-context` | 326 |
| `InputStreamDigester.java` | `use-of-md5` | 328 |
| `XMLReaderUtils.java` | `documentbuilderfactory-disallow-doctype-decl-missing` | 611 |

C'est ce que confirme le run Semgrep de la [PR #2](https://github.com/DavidStanescu13/tika/pull/2) : « 7 findings (7 blocking) », build rouge (run `44006559348`). Les 3 détections de base ne sont pas recomptées, car le mode diff ne retient que ce que les commits ajoutent.

## 5. Humour : rickroll dans le CI

Quand un test du cas d'étude échoue, le workflow [`tache3-tests.yml`](.github/workflows/tache3-tests.yml) invoque l'action locale [`.github/actions/test-failure-report`](.github/actions/test-failure-report/action.yml), qui publie dans le résumé du run (et en commentaire de PR) une vignette YouTube cliquable menant à un rickroll. La branche `tache3-test-failure` ajoute un test qui échoue exprès pour le démontrer.

## 6. Déclaration d'usage de l'intelligence artificielle

_À compléter._

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
