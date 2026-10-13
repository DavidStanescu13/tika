# IFT3913 – Tâche 2 : augmentation d'une suite de tests (Apache Tika)

| Nom complet | Identifiant GitHub |
|---|---|
| David STANESCU | DavidStanescu13 |
| Aness Lakhdar AZZOUZ | ans1008 |

- Dépôt de la tâche : https://github.com/DavidStanescu13/tika (fork de [umontreal-diro/tika](https://github.com/umontreal-diro/tika)), module **`tika-core`**
- Ce README : https://github.com/DavidStanescu13/tika/blob/main/README.md
- Workflow GitHub Actions : https://github.com/DavidStanescu13/tika/actions/workflows/tache2.yml
- Rapports complets (JaCoCo HTML, PIT HTML/XML/CSV, Surefire) : branches [`tache2-reports`](https://github.com/DavidStanescu13/tika/tree/tache2-reports) (Linux) et [`tache2-reports-windows`](https://github.com/DavidStanescu13/tika/tree/tache2-reports-windows), régénérées à chaque exécution du workflow

> Le README original de Tika a été déplacé dans [`README-tika.md`](README-tika.md).

## Résumé

Classes testées : **`org.apache.tika.utils.ProcessUtils`** (choisie et traitée principalement par Aness) et **`org.apache.tika.utils.CharsetUtils`** (David). Chacun a mené sa propre campagne ChatUniTest (macOS et Windows) ; le dépôt final contient les tests générés retenus, trois classes de tests manuels pour `ProcessUtils` et une pour `CharsetUtils`, et un pipeline GitHub Actions Linux + Windows qui recalcule JaCoCo et quatre scénarios PIT à chaque push.

| Classe | Scénario | Tests | Mutants tués (PIT, Linux) | Score (Linux) | Score (Windows) |
|---|---|---|---|---|---|
| ProcessUtils | A – tests originaux de `tika-core` | indirects (`FileCommandDetectorTest`) | 3 / 61 | **4,9 %** | 4,9 % |
| ProcessUtils | B – + tests générés par ChatUniTest (corrigés) | + 40 | 9 / 61 | **14,8 %** | 29,5 % |
| ProcessUtils | C – + tests écrits à la main | + 60 | 47 / 61 (dont 2 par timeout) | **77,0 %** | 91,8 % (56 / 61) |
| CharsetUtils | A – tests originaux (`CharsetUtilsTest`) | 4 | 15 / 25 | **60,0 %** | 60,0 % |
| CharsetUtils | B – + tests générés par ChatUniTest | + 21 (7 désactivés) | 15 / 25 | **60,0 %** | 60,0 % |
| CharsetUtils | C – + tests écrits à la main | + 10 | 16 / 25 | **64,0 %** | 64,0 % |

- ChatUniTest (modèle ouvert `qwen2.5-coder:7b` exécuté localement avec Ollama) **ne produit pas de tests utilisables sans intervention** : 0 test compilable sur 224 tentatives tant que Mockito manquait au classpath ; ensuite 30 % (macOS) à 43 % (Windows) des tentatives sont retenues par l'outil, mais 11 tests sur 40 (macOS) et 51 sur 88 (Windows) échouent réellement sous Maven, parce que l'outil accepte un test dont les assertions échouent (§4).
- Les mutants restants sont tous justifiés (§9) : mutants équivalents créés par PIT, code exécutable seulement sous Windows (tué par le job Windows), chemin ICU4J absent du classpath de `tika-core`.
- Tous les nouveaux tests s'exécutent avec succès dans la GitHub Action, sous Linux **et** sous Windows (§10).

## Sommaire

1. [Organisation du dépôt](#1-organisation-du-dépôt)
2. [Classes choisies et justification](#2-classes-choisies-et-justification)
3. [ChatUniTest dans le pipeline Maven](#3-chatunitest-dans-le-pipeline-maven)
4. [Génération des tests : deux campagnes](#4-génération-des-tests--deux-campagnes)
5. [Critique des tests générés et comparaison des oracles](#5-critique-des-tests-générés-et-comparaison-des-oracles)
6. [PIT : installation et scores de mutation](#6-pit--installation-et-scores-de-mutation)
7. [Mutants détectés par les tests générés](#7-mutants-détectés-par-les-tests-générés)
8. [Tests écrits à la main](#8-tests-écrits-à-la-main)
9. [Mutants détectés par les tests manuels, mutants restants](#9-mutants-détectés-par-les-tests-manuels-mutants-restants)
10. [Exécution dans GitHub Actions](#10-exécution-dans-github-actions)
11. [Observations sur le code de Tika](#11-observations-sur-le-code-de-tika)
12. [Déclaration d'usage de l'intelligence artificielle](#12-déclaration-dusage-de-lintelligence-artificielle)
13. [Reproduire les résultats](#13-reproduire-les-résultats)
14. [Annexe : les 61 mutants de ProcessUtils et les 25 de CharsetUtils](#14-annexe--les-61-mutants-de-processutils-et-les-25-de-charsetutils)

---

## 1. Organisation du dépôt

Tout le travail porte sur le module **`tika-core`**. Fichiers ajoutés ou modifiés par rapport au dépôt amont :

| Fichier | Rôle |
|---|---|
| [`tika-core/pom.xml`](tika-core/pom.xml) | ajout de deux profils Maven : `pitest` (plugin `pitest-maven` + `pitest-junit5-plugin`) et `chatunitest` (plugin `chatunitest-maven-plugin` configuré pour Ollama) |
| [`tika-core/pom.xml`](tika-core/pom.xml) (suite) | ajout de Mockito en dépendance de test (exigé par les tests générés) ; exclusion des fichiers générés de `checkstyle`/`forbiddenapis` |
| [`ProcessUtils_*_Test.java`](tika-core/src/test/java/org/apache/tika/utils/) (9 fichiers, 40 tests) | tests **générés** par ChatUniTest pour `ProcessUtils` – campagne macOS (Aness), corrigés pour passer (§4.2) |
| [`CharsetUtils_*_Test.java`](tika-core/src/test/java/org/apache/tika/utils/) (2 fichiers, 21 tests) | tests **générés** par ChatUniTest pour `CharsetUtils` – campagne Windows (David), copiés tels quels + `@Disabled` (§4.3) |
| [`ProcessUtilsManualTest.java`](tika-core/src/test/java/org/apache/tika/utils/ProcessUtilsManualTest.java) (16 tests) | tests **manuels** `ProcessUtils` avec de vrais processus `sh -c` (Aness, §8.1) |
| [`ProcessUtilsMockedManualTest.java`](tika-core/src/test/java/org/apache/tika/utils/ProcessUtilsMockedManualTest.java) (26 tests) | tests **manuels** `ProcessUtils` avec `Process`/`Runtime` simulés par Mockito (Aness, §8.2) |
| [`ProcessUtilsPortableManualTest.java`](tika-core/src/test/java/org/apache/tika/utils/ProcessUtilsPortableManualTest.java) (18 tests) | tests **manuels** `ProcessUtils` avec un sous-processus JVM contrôlé, portables Linux/Windows (David, §8.3) |
| [`CharsetUtilsManualTest.java`](tika-core/src/test/java/org/apache/tika/utils/CharsetUtilsManualTest.java) (10 tests) | tests **manuels** `CharsetUtils` (David, §8.4) |
| [`tika-core/chatunitest/`](tika-core/chatunitest/) | sortie brute de la campagne ChatUniTest **macOS** : 9 fichiers retenus, journal `chatunitest-run.log`, 105 rapports d'erreur `error-messages/` |
| [`tika-core/chatunitest-windows/`](tika-core/chatunitest-windows/tika-parent/tika-core/org/apache/tika/utils) | sortie brute de la campagne ChatUniTest **Windows** : 15 fichiers retenus (13 `ProcessUtils`, 2 `CharsetUtils`), non modifiés |
| [`tika-core/target/chatunitest-info/`](tika-core/target/chatunitest-info/tika-parent/tika-core) | traces complètes de la campagne Windows : prompts, réponses du modèle, erreurs de chaque tentative (versionné volontairement) |
| [`tache2/run-chatunitest.sh`](tache2/run-chatunitest.sh) | script qui lance ChatUniTest + Ollama (choix du JDK, alias du modèle, Maven Wrapper) et copie les tests retenus dans le dossier de test |
| [`tache2/chatunitest-*.log`](tache2/) | journaux Maven de la campagne Windows |
| [`tache2/README-aness-processutils.md`](tache2/README-aness-processutils.md) | rapport initial d'Aness sur `ProcessUtils` (branche `tache2-processutils`), intégré dans ce document |
| [`.github/workflows/tache2.yml`](.github/workflows/tache2.yml) | workflow GitHub Actions (Linux + Windows) : tests originaux + JaCoCo, nouveaux tests, PIT en quatre scénarios, publication des rapports |
| [`.github/scripts/tache2_summary.py`](.github/scripts/tache2_summary.py) | produit le résumé Markdown affiché dans l'onglet *Summary* de chaque exécution |
| `.github/workflows/main-*.yml`, `docker-snapshot.yml`, `split-packages.yml` | workflows amont de Tika passés en déclenchement manuel (`workflow_dispatch`) pour ne pas recompiler tout Tika à chaque push |

Commandes locales (Java 17+ ; Maven 3.9+ ou le wrapper `./mvnw` fourni) :

```bash
# compiler tika-parent + tika-core
mvn -pl tika-core -am install -DskipTests -Drat.skip=true

# tous les tests de tika-core + rapport JaCoCo (tika-core/target/site/jacoco/index.html)
mvn -pl tika-core test -Drat.skip=true

# PIT avec les tests originaux seulement (tika-core/target/pit-reports/index.html)
mvn -pl tika-core -Ppitest test-compile org.pitest:pitest-maven:mutationCoverage -Drat.skip=true

# PIT avec tests originaux + générés + manuels (C2) ; remplacer ProcessUtils*Test par ProcessUtils_*Test pour B,
# ou par ProcessUtilsManualTest pour C1
mvn -pl tika-core -Ppitest test-compile org.pitest:pitest-maven:mutationCoverage -Drat.skip=true \
  -DtargetTests='org.apache.tika.detect.FileCommandDetectorTest,org.apache.tika.utils.CharsetUtilsTest,org.apache.tika.utils.ProcessUtils*Test,org.apache.tika.utils.CharsetUtils*Test'

# génération ChatUniTest (Ollama doit tourner)
bash tache2/run-chatunitest.sh
```


## 2. Classes choisies et justification

Nous avons retenu deux classes du paquet `org.apache.tika.utils` de `tika-core` (la consigne en permet 1 à 3) :

| Classe | Rôle | Tests existants | Pourquoi elle convient |
|---|---|---|---|
| **`ProcessUtils`** (296 lignes) | lance des processus externes avec timeout (`execute`, deux surcharges), capture/redirige stdout et stderr, vérifie qu'une commande existe (`checkCommand`, deux surcharges), échappe les arguments pour Windows (`escapeCommandLine` / `unescapeCommandLine`) | aucun test dédié ; exercée **indirectement** par `FileCommandDetectorTest#testBasic` (via `FileCommandDetector`, qui appelle `checkCommand` et `execute(pb, timeout, int, int)`) | couverture partielle, beaucoup de branches (timeouts, troncature, codes d'erreur) jamais exécutées, 58 mutants vivants sur 61 |
| **`CharsetUtils`** (197 lignes) | normalise des noms de jeux de caractères (`"utf-8>"`, `cp-1251`, `win1251`, `8859-15`…), les résout via une table, des regex, ICU4J si présent, puis le JDK | `CharsetUtilsTest` (4 tests, 29 assertions), qui ne passe que par `clean()` et `isSupported()` | couverture élevée mais incomplète (branches ICU, exceptions de `forName`), 10 mutants vivants sur 25 ; classe « pure », idéale pour comparer les oracles d'un LLM à ceux d'un humain |

### ProcessUtils : détail de ce qui n'est pas couvert

Avec la suite originale complète de `tika-core` (374 tests examinés par PIT, dont 27 exécutés pour tuer des mutants) :

- 70 lignes sur 146 sont couvertes (48 %) ;
- sur **61 mutants**, seulement **3 sont tués** (5 %), **24 survivent** et **34 n'ont aucune couverture** ;
- aucun test original ne vise `ProcessUtils` : elle n'est exercée qu'indirectement, par `FileCommandDetectorTest#testBasic` (via `FileCommandDetector`), qui tue les 3 seuls mutants (`escapeCommandLine` ligne 71, `Thread::start` ligne 107 et valeur de retour de `execute` ligne 149) ;
- méthodes sans aucun test : `unescapeCommandLine`, `checkCommand` (les deux surcharges) et la surcharge `execute(ProcessBuilder, long, Path, int)` ; aucune branche d'erreur de `execute` n'est testée (délai dépassé, interruption, `IOException`).


### Couverture et mutants **avant** nos ajouts (mesurés par le workflow, exécution #1)

Couverture JaCoCo obtenue avec la suite de tests **complète** de `tika-core` (pas seulement les deux tests ci-dessus) :

| Classe | Instructions | Branches | Lignes | Méthodes |
|---|---|---|---|---|
| ProcessUtils | 49,7 % (261/525) | 22,2 % (12/54) | 48,6 % (69/142) | 54,5 % (6/11) |
| CharsetUtils | 83,4 % (363/435) | 78,1 % (25/32) | 76,5 % (62/81) | 83,3 % (5/6) |

Mutation PIT (mutateurs `DEFAULTS`) avec les tests originaux qui exercent ces classes (`FileCommandDetectorTest`, `CharsetUtilsTest`) :

| Classe | Mutants | Tués | Survivants | Sans couverture | **Score** |
|---|---|---|---|---|---|
| ProcessUtils | 61 | 3 | 24 | 34 | **4,9 %** |
| CharsetUtils | 25 | 15 | 5 | 5 | **60,0 %** |

Méthodes/branches non couvertes qui motivent le choix :

- `ProcessUtils.execute(ProcessBuilder, long, Path, int)` (redirection de stdout vers un fichier) : **0 ligne couverte**, 15 mutants `NO_COVERAGE`.
- `ProcessUtils.execute(ProcessBuilder, long, int, int)` : seule la branche « le processus se termine avant le timeout » est couverte ; la branche timeout (lignes 120-131) et le `catch InterruptedException` ne le sont pas.
- `ProcessUtils.checkCommand` : seul le chemin « commande présente, code de sortie 0 » est couvert ; `errorValue` explicite, commande absente (`IOException`), `TimeoutException`, surcharge `checkCommand(String, int...)` ne le sont pas.
- `ProcessUtils.unescapeCommandLine` : jamais appelée par un test ; `escapeCommandLine` n'est couverte que sur le chemin « pas d'espace ».
- `CharsetUtils.forName` : les exceptions (`null`, nom non analysable, `none`/`no`) ne sont observées qu'à travers `clean()` qui les transforme en `null` ; le chemin ICU4J (lignes 183-192) et le `catch (Exception)` de `isSupported` ne sont pas couverts.


## 3. ChatUniTest dans le pipeline Maven

[ChatUniTest](https://github.com/ZJU-ACES-ISE/ChatUniTest) est intégré comme **plugin Maven** dans `tika-core/pom.xml`, dans le profil `chatunitest` (`io.github.zju-aces-ise:chatunitest-maven-plugin:2.1.1`) ; PIT est dans le profil `pitest` (§6). Ces profils ne sont pas actifs dans un build normal.

```xml
<profile>
  <id>chatunitest</id>
  <dependencies>   <!-- Mockito en portée provided : voir obstacle 3 ci-dessous -->
    <dependency><groupId>org.mockito</groupId><artifactId>mockito-core</artifactId><scope>provided</scope></dependency>
    <dependency><groupId>org.mockito</groupId><artifactId>mockito-junit-jupiter</artifactId><version>${mockito-junit-jupiter.version}</version><scope>provided</scope></dependency>
  </dependencies>
  <properties>
    <chatunitest.url>http://localhost:11434/v1/chat/completions</chatunitest.url>   <!-- API OpenAI-compatible d'Ollama -->
    <chatunitest.model>codeqwen:v1.5-chat</chatunitest.model>                        <!-- alias Ollama de qwen2.5-coder:7b, voir obstacle 2 -->
  </properties>
  <build><plugins>
    <plugin>
      <groupId>io.github.zju-aces-ise</groupId>
      <artifactId>chatunitest-maven-plugin</artifactId>
      <version>2.1.1</version>
      <configuration>
        <apiKeys>ollama</apiKeys>          <!-- clé factice : Ollama n'en exige pas, le plugin oui -->
        <model>${chatunitest.model}</model>
        <url>${chatunitest.url}</url>
        <testNumber>5</testNumber>         <!-- 5 tests candidats par méthode -->
        <maxRounds>5</maxRounds>           <!-- jusqu'à 5 rondes de réparation automatique -->
        <temperature>0.2</temperature>
        <testOutput>${project.basedir}/chatunitest</testOutput>
        <thread>false</thread>
        <stopWhenSuccess>false</stopWhenSuccess>
        <merge>true</merge>
      </configuration>
    </plugin>
  </plugins></build>
</profile>
```

Le modèle de langage est **ouvert et exécuté localement** avec [Ollama](https://ollama.com) : `ollama pull qwen2.5-coder:7b` (4,7 Go) puis `ollama cp qwen2.5-coder:7b codeqwen:v1.5-chat`. Aucune donnée ne quitte la machine. Commande de génération pour une classe (JDK 17 ou 21 requis, voir ci-dessous) :

```bash
mvn -pl tika-core -Pchatunitest -Drat.skip=true \
    io.github.zju-aces-ise:chatunitest-maven-plugin:2.1.1:class \
    -DselectClass=org.apache.tika.utils.ProcessUtils
```

Le script [`tache2/run-chatunitest.sh`](tache2/run-chatunitest.sh) automatise tout cela (choix d'un JDK 17/21 si le JDK par défaut est plus récent, vérification qu'Ollama répond, `mvn install` de `tika-core`, génération, copie des tests retenus dans `src/test/java`).

Trois obstacles, rencontrés indépendamment sur les deux machines (macOS, Java 25 ; Windows, Java 26) :

| # | Symptôme | Cause | Correctif (dans le dépôt) |
|---|---|---|---|
| 1 | `Invalid Java version format: 25` / `: 26` dès le lancement du plugin | ChatUniTest 2.1.1 analyse `java.version` avec un format `17.0.x` et ne reconnaît pas les JDK récents | lancer Maven avec un JDK 17 ou 21 pour la génération seulement (`JAVA_HOME`) ; le script le fait automatiquement |
| 2 | `No Model with name qwen2.5-coder:7b. Support models: gpt-3.5-turbo, …, code-llama, codeqwen:v1.5-chat` | le plugin n'accepte qu'une liste fermée de noms (pour choisir le *tokenizer* et la taille de contexte) | alias Ollama `codeqwen:v1.5-chat` qui pointe vers `qwen2.5-coder:7b`. **Le modèle qui génère réellement les tests est donc `qwen2.5-coder:7b`**, pas CodeQwen |
| 3 | tous les tests générés qui importent Mockito échouent à la compilation (`package org.mockito does not exist`) et le plugin gaspille 5 rondes de réparation par tentative : **0 test compilable sur 224 tentatives** lors de la première campagne Windows (1 h 05), ~10 tentatives sur 30 terminées en 57 min sur macOS avant interruption | le *prompt* de ChatUniTest impose Mockito au modèle, mais `tika-core` n'avait pas Mockito ; de plus le plugin construit son classpath avec `getCompileClasspathElements()` (portées `compile`/`provided`/`system`), donc une dépendance en portée `test` peut lui rester invisible | `mockito-core` et `mockito-junit-jupiter` ajoutés en portée `test` (pour Surefire/PIT) **et** redéclarés en portée `provided` dans le profil `chatunitest` (pour le plugin). Analyse automatique des 224 rapports d'erreur de la campagne Windows : 100 % des erreurs étaient `org.mockito` ou des symboles `@Mock`/`MockitoExtension`/`when(…)` |

## 4. Génération des tests : deux campagnes

Chaque membre du binôme a fait tourner ChatUniTest sur sa machine. Les deux campagnes sont documentées parce qu'elles donnent des résultats différents pour la même classe (§4.3, tableau comparatif), et parce que chacune a laissé des traces exploitables.

### 4.1 Campagne macOS (ProcessUtils, Aness) : génération

Machine : MacBook Pro M3 Pro, 36 Go de RAM, Ollama sur GPU (mémoire unifiée). Commande (depuis la racine du dépôt) :

```bash
export JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.5-zulu   # JDK 21 requis par le plugin
mvn -pl tika-core -Pchatunitest -Drat.skip=true \
    io.github.zju-aces-ise:chatunitest-maven-plugin:2.1.1:class \
    -DselectClass=org.apache.tika.utils.ProcessUtils
```

Le plugin traite 6 méthodes (les deux surcharges de `execute` et de `checkCommand` sont comptées à part) avec 5 tentatives chacune, soit **30 tentatives** en **1 h 11**.

| Méthode | Tentatives réussies (compile + s'exécute) | Échecs de compilation | Échecs d'exécution |
|---|---|---|---|
| `escapeCommandLine` | 5 / 5 | 0 | 0 |
| `unescapeCommandLine` | 3 / 5 | 0 | 2 |
| `execute(pb, timeout, int, int)` | 0 / 5 | 5 | 0 |
| `execute(pb, timeout, Path, int)` | 1 / 5 (après 2 rondes de réparation) | 4 | 0 |
| `checkCommand(String, int...)` | 0 / 5 | 2 | 3 |
| `checkCommand(String[], int...)` | 0 / 5 | 2 | 3 |
| **Total** | **9 / 30 (30 %)** | **13** | **8** |

Causes des échecs de compilation (nombre d'occurrences dans les fichiers d'erreur du plugin) : symbole introuvable (116), `void` utilisé comme valeur (40), `IOException` / `InterruptedException` non déclarées (15 + 15), appel de méthodes **privées** `register` et `release` (14 + 14), tentative de simuler `Runtime.getRuntime().exec(...)` ou un `StreamGobbler` par un `thenReturn` de type incompatible (10 + 7). Les échecs d'exécution de `checkCommand` viennent du même motif : `Runtime.getRuntime()` est statique, et le modèle essaie de le simuler comme une instance.


### 4.2 Campagne macOS (ProcessUtils) : où sont les tests, compilent-ils, s'exécutent-ils ?

- **Sortie brute du plugin :** [`tika-core/chatunitest/tika-parent/tika-core/org/apache/tika/utils/`](tika-core/chatunitest/tika-parent/tika-core/org/apache/tika/utils/) (9 fichiers `ProcessUtils_<méthode>_<n°>_<tentative>_Test.java` et une `ProcessUtils_Suite.java` JUnit 4 que nous n'utilisons pas). Ces fichiers ne sont pas compilés par Maven. Le journal complet du plugin ([`chatunitest-run.log`](tika-core/chatunitest/chatunitest-run.log)) et les 105 messages d'erreur de compilation ou d'exécution ([`error-messages/`](tika-core/chatunitest/error-messages/)) qui ont servi aux statistiques de la §4.1 y sont conservés.
- **Version intégrée au projet (après corrections) :** [`tika-core/src/test/java/org/apache/tika/utils/`](tika-core/src/test/java/org/apache/tika/utils/), mêmes noms. Ce sont ces fichiers qui s'exécutent dans le build et dans la GitHub Action.

| Fichier | Tests | Compile tel quel ? | S'exécute tel quel sous Maven ? | Intervention manuelle |
|---|---|---|---|---|
| `ProcessUtils_escapeCommandLine_2_0_Test` | 6 | oui, mais Checkstyle refuse (en-tête, imports) | 1 échec | 6 lignes |
| `ProcessUtils_escapeCommandLine_2_1_Test` | 7 | idem | 2 échecs | 3 lignes |
| `ProcessUtils_escapeCommandLine_2_2_Test` | 1 | idem | oui | formatage seulement |
| `ProcessUtils_escapeCommandLine_2_3_Test` | 9 | idem | 1 échec | 1 ligne |
| `ProcessUtils_escapeCommandLine_2_4_Test` | 7 | idem | 1 échec | 1 ligne |
| `ProcessUtils_execute_5_4_Test` | 4 | idem | 3 échecs | 29 lignes |
| `ProcessUtils_unescapeCommandLine_3_1_Test` | 4 | idem | 1 échec | 2 lignes |
| `ProcessUtils_unescapeCommandLine_3_2_Test` | 1 | idem | 1 échec | 2 lignes |
| `ProcessUtils_unescapeCommandLine_3_4_Test` | 1 | idem | 1 échec | 4 lignes |
| **Total** | **40** | | **11 tests en échec sur 40** | 16 tests modifiés |

**Les tests générés ne s'exécutent donc pas sans intervention manuelle.** Corrections nécessaires, dans l'ordre :

1. **Mise en forme exigée par le build de Tika (9 fichiers sur 9).** Checkstyle (`checkstyle.xml` du projet) rejette les fichiers : 9 en-têtes de licence Apache manquants, 79 imports inutilisés, 36 imports avec `*`. Correction mécanique (script, aucune modification de la logique) : ajout de l'en-tête, imports explicites, suppression des imports inutilisés.
2. **Suppression du fichier `ProcessUtils_Suite.java`** (exécuteur JUnit 4 `@RunWith(JUnitPlatform.class)`, inutile avec JUnit 5 et Surefire).
3. **Tests dépendants du système d'exploitation (8 tests en échec + 5 rendus portables).** `escapeCommandLine` et `unescapeCommandLine` ne modifient leur argument que si `SystemUtils.IS_OS_WINDOWS` est vrai. Or ce booléen est une **constante `static final` calculée une seule fois** au chargement de la classe. Les tests nommés `..._Windows` supposent donc Windows sans le vérifier : ils échouent sur macOS et Linux. Correction : `@EnabledOnOs(OS.WINDOWS)` sur les tests qui décrivent le comportement Windows, `@DisabledOnOs(OS.WINDOWS)` sur ceux qui décrivent le comportement des autres systèmes. Deux tests faisaient `System.setProperty("os.name", "Linux")` : cet appel **ne peut pas fonctionner** (la constante est déjà figée) et modifie une propriété globale de la JVM, ce qui peut contaminer d'autres tests. Nous l'avons supprimé. Dans `unescapeCommandLine_3_2` et `_3_4`, un seul test mélange des cas Windows et non Windows : l'oracle devient `SystemUtils.IS_OS_WINDOWS ? "sans guillemets" : entrée`. Dans `_3_4`, le cas 4 (`"\"example\""` sans espace) attendait `example` : c'est faux sur tous les systèmes, car la méthode ne retire les guillemets que si l'argument contient une espace ; l'oracle est corrigé en `input4`.
4. **`ProcessUtils_execute_5_4_Test` : oracles faux (3 tests, 29 lignes).** Le test simule `ProcessBuilder`, `Process` **et** un `StreamGobbler`. Mais `execute` crée son propre `StreamGobbler` en interne : le mock injecté n'est jamais utilisé (Mockito le signale avec `UnnecessaryStubbing`). Les valeurs attendues (`100`, `"line1\nline2"`) ne peuvent donc jamais être produites. De plus, `assertEquals(1000, result.processTimeMillis)` suppose que le temps écoulé est égal au délai demandé, alors qu'avec des mocks il vaut environ 0 ms. Corrections : suppression du mock inutile et de ses stubs, `processTimeMillis >= 0` au lieu de `1000`, longueurs de flux `0` et `stderr` vide, et, dans le cas du délai dépassé, `exitValue == -1` (la surcharge avec redirection ne récupère jamais le code de sortie après le `destroyForcibly`) ; dans le cas de l'interruption, `processTimeMillis == -1`.

**Observation importante sur ChatUniTest.** Le plugin a affiché, pour ce même fichier `execute`, `3 tests failed` puis `compile and execute successfully`. Son critère de succès est que le test **compile et se lance**, pas que ses assertions passent. C'est pourquoi 11 tests sur 40 « réussis » selon l'outil échouent en réalité sous Maven. Un test généré qui échoue n'est pas rejeté : il faut le revérifier soi-même.

Nombre total de corrections manuelles : **1 script de mise en forme (9 fichiers), 1 fichier supprimé, 16 tests modifiés sur 40** (11 en échec réparés plus 5 rendus indépendants du système d'exploitation : `2_0` `WithQuotes_Windows` et `WithQuotes_NoWindows`, `2_0` `WithSpaces_NoWindows`, `2_1` `NoQuotes_NoWindows`, `3_1` `QuotesNonWindows`). **Plus 3 corrections révélées par le job Windows du workflow** (exécution #23, premier passage de ces fichiers sous Windows) : `2_3` `WithSpaces_NotWindows` et `2_4` `WithSpaces_NonWindows` manquaient de `@DisabledOnOs(WINDOWS)`, et `3_1` `QuotesWindows` attendait le dé-échappement de `"example"` (sans espace), ce que la méthode ne fait sur aucun OS – oracle corrigé. Total : 19 tests modifiés sur 40.


### 4.3 Campagne Windows (ProcessUtils + CharsetUtils, David) : obstacles et génération

Machine : PC Windows 11, Java 26 par défaut (JDK 17 Temurin installé pour le plugin), Ollama sur CPU/GPU grand public. Les mêmes obstacles qu'en §3 ont été rencontrés indépendamment ; ils sont rappelés ici avec les chiffres de cette campagne.

| # | Symptôme | Cause | Correctif (tous dans le dépôt) |
|---|---|---|---|
| 1 | `Invalid Java version format: 26` dès le lancement du plugin | ChatUniTest 2.1.1 analyse `java.version` avec un format `17.0.x` et ne reconnaît pas les JDK récents | le script `tache2/run-chatunitest.sh` détecte un JDK > 21 et bascule sur un JDK 17/21 installé (`JAVA_HOME`) |
| 2 | `No Model with name qwen2.5-coder:7b. Support models: gpt-3.5-turbo, …, code-llama, codeqwen:v1.5-chat` | le plugin n'accepte qu'une liste fermée de noms de modèles (pour choisir le *tokenizer* et la taille de contexte) | le modèle local `qwen2.5-coder:7b` est exposé par Ollama sous le nom `codeqwen:v1.5-chat` (`ollama cp qwen2.5-coder:7b codeqwen:v1.5-chat`) ; c'est bien qwen2.5-coder qui s'exécute |
| 3 | **Première campagne (12 oct., 1 h 05 de génération) : 0 test compilable sur 224 tentatives** (ProcessUtils 149, CharsetUtils 75) – journaux dans [`tache2/chatunitest-ProcessUtils.log`](tache2/chatunitest-ProcessUtils.log), [`tache2/chatunitest-CharsetUtils.log`](tache2/chatunitest-CharsetUtils.log) et les 224 fichiers `tika-core/target/chatunitest-info/…/error-message/*_CompilationError_*.txt` | analyse automatique des 224 rapports d'erreur : **100 % des erreurs** sont `package org.mockito does not exist`, `package org.mockito.junit.jupiter does not exist` ou des `cannot find symbol` sur des lignes `@Mock`, `@ExtendWith(MockitoExtension.class)`, `when(…)`. Le *prompt* de ChatUniTest impose Mockito au modèle, mais `tika-core` n'a pas Mockito dans ses dépendances de test ; les 5 rondes de « réparation » renvoient l'erreur au modèle, qui ne peut pas l'éliminer puisqu'elle ne dépend pas du code | ajout de `mockito-core` et `mockito-junit-jupiter` (versions gérées par `tika-parent`) en dépendances `test` de `tika-core/pom.xml` ; seconde campagne ci-dessous |

Autre point d'intégration : les tests générés sont copiés **tels quels** dans `src/test/java`, sans en-tête de licence Apache ni respect du style Tika (imports `*`). Plutôt que de les retoucher (ce qui serait une « intervention manuelle »), ils sont exclus de `checkstyle` et de `forbiddenapis` dans `tika-core/pom.xml` (motif `*_*_*_*_Test.java`). La « suite » `<Classe>_Suite.java` que ChatUniTest produit (`@RunWith(JUnitPlatform.class)`, format JUnit 4) n'est pas copiée : elle ne compile pas avec JUnit Jupiter 6.


#### Où sont les tests de la campagne Windows ?

- **Sortie brute de ChatUniTest** (non modifiée) : [`tika-core/chatunitest-windows/tika-parent/tika-core/org/apache/tika/utils/`](tika-core/chatunitest-windows/tika-parent/tika-core/org/apache/tika/utils) – 15 fichiers `<Classe>_<méthode>_<n°méthode>_<n°tentative>_Test.java` retenus par l'outil, plus deux fichiers `*_Suite.java` (format JUnit 4, non utilisables).
- **Traces complètes** : [`tika-core/target/chatunitest-info/tika-parent/tika-core/`](tika-core/target/chatunitest-info/tika-parent/tika-core) – pour chaque classe, chaque méthode et chaque tentative, le *prompt* envoyé au modèle, sa réponse, le code extrait et les erreurs de compilation/exécution (`history*/class*/method*/attempt*/records.json`, `error-message/`). Journaux Maven : [`tache2/chatunitest-ProcessUtils.log`](tache2/chatunitest-ProcessUtils.log) (40 min) et [`tache2/chatunitest-CharsetUtils.log`](tache2/chatunitest-CharsetUtils.log) (22 min).
- **Tests exécutés par le pipeline** : seuls les **2 fichiers `CharsetUtils_*`** de cette campagne sont copiés dans [`tika-core/src/test/java/org/apache/tika/utils/`](tika-core/src/test/java/org/apache/tika/utils) (avec les annotations `@Disabled` décrites ci-dessous). Les 13 fichiers `ProcessUtils_*` de cette campagne portent les mêmes noms que ceux de la campagne macOS, déjà intégrés et corrigés ; ils restent en sortie brute et servent à la comparaison des deux campagnes (tableau ci-dessous). Ils ont tout de même été exécutés dans le pipeline (exécutions #16 à #22) avant la fusion, ce qui a produit les chiffres du tableau suivant.

Paramètres : modèle `qwen2.5-coder:7b` (Ollama, local, exposé sous le nom `codeqwen:v1.5-chat`), `testNumber=5` (5 tentatives par méthode), `maxRounds=5` (jusqu'à 5 rondes de réparation par tentative), `temperature=0.2`, `stopWhenSuccess=false`, `prune=false`, schéma `CHATUNITEST` (prompt par défaut de l'outil). Les méthodes privées `register`/`release` et `initCommonCharsets` sont ignorées par l'outil ; les deux surcharges de `execute` et de `checkCommand` sont traitées séparément (d'où 10 tentatives pour chacune).


#### Campagne Windows : compilent-ils et s'exécutent-ils sans intervention manuelle ?

**Côté ChatUniTest (seconde campagne)** – 45 tentatives, 15 retenues :

| Classe | Méthode | Tentatives | Retenues par l'outil | Rondes de réparation avant succès | Échecs de compilation | Échecs d'exécution (hors assertions) |
|---|---|---|---|---|---|---|
| ProcessUtils | `escapeCommandLine` | 5 | 5 | 0, 0, 0, 0, 0 | 0 | 0 |
| ProcessUtils | `unescapeCommandLine` | 5 | 3 | 0, 0, 0 | 10 | 0 |
| ProcessUtils | `execute` (×2 surcharges) | 10 | 3 | 1, 1, 2 | 35 | 0 |
| ProcessUtils | `checkCommand` (×2 surcharges) | 10 | 2 | 0, 0 | 40 | 0 |
| CharsetUtils | `isSupported` | 5 | 1 | 0 | 5 | 15 |
| CharsetUtils | `clean` | 5 | 1 | 0 | 0 | 20 |
| CharsetUtils | `forName` | 5 | 0 | – | 10 | 15 |
| **Total** | | **45** | **15** (33 %) | | **100** | **50** |

Les 15 fichiers retenus contiennent **88 méthodes de test**. Mais « retenu par ChatUniTest » ne veut pas dire « passe » : en lisant le code de l'outil ([`AbstractRunner.runTest`](https://github.com/ZJU-ACES-ISE/chatunitest-core)), on constate qu'avec `prune=false` un test qui compile et s'exécute en n'échouant **que** par des `AssertionError` est considéré comme réussi et exporté (l'outil suppose que l'assertion révèle peut-être un bogue du code testé). Or les échecs de vérification Mockito (`WantedButNotInvoked`…) héritent aussi d'`AssertionError`.

**Côté pipeline (GitHub Actions, Linux et Windows), sans aucune modification** – exécution [#16](https://github.com/DavidStanescu13/tika/actions/runs/38392448544) :

| Fichier généré | Tests | Échecs Linux | Échecs Windows | Cause dominante |
|---|---|---|---|---|
| `CharsetUtils_clean_2_4_Test` | 19 | 7 | 7 | oracle faux (nom canonique, regex) |
| `CharsetUtils_isSupported_1_4_Test` | 2 | 0 | 0 | – |
| `ProcessUtils_escapeCommandLine_2_{0..4}_Test` | 35 | 7 | 6 | tests « Windows » et « non-Windows » écrits tous les deux sans condition : exactement l'un des deux échoue sur chaque OS ; plus un oracle faux (`"test arg" with spaces` supposé re-échappé) |
| `ProcessUtils_unescapeCommandLine_3_{0,2,3}_Test` | 5 | 5 | 5 | oracle faux (dé-échappement attendu hors Windows et sans espace) ; `when(systemUtils.IS_OS_WINDOWS)` sur un champ `static final` |
| `ProcessUtils_execute_{4_4,5_0,5_3}_Test` | 12 | 12 | 12 | `processTimeMillis` supposé égal au timeout ; mocks de `Process`/`ProcessBuilder`/`StreamGobbler` jamais utilisés par la méthode statique |
| `ProcessUtils_checkCommand_6_{1,4}_Test` | 15 | 15 | 15 | mock de `Process` jamais injecté (`Runtime.exec` réel), `verify` sur un mock jamais appelé, `IOException` *checked* sur `waitFor`, `UnnecessaryStubbingException` |
| **Total** | **88** | **46** | **45** | **51 tests distincts échouent sur au moins un OS, 37 passent partout** |

**Corrections nécessaires** pour que la suite s'exécute avec succès dans l'Action (critère « exécution ») – toutes visibles dans le dépôt, le code généré n'étant jamais réécrit :

- **11 tests** d'`escapeCommandLine` conditionnés à la plateforme : une ligne `assumeTrue(SystemUtils.IS_OS_WINDOWS)` ou `assumeFalse(...)` ajoutée en tête de méthode (même diagnostic que la campagne macOS, qui a utilisé `@EnabledOnOs`).
- **40 tests** marqués `@Disabled("IFT3913 : <raison>")`, la raison étant inscrite dans l'annotation (oracle faux, Mockito mal employé, exception impossible). Les 7 tests `CharsetUtils_clean_2_4_Test` ainsi désactivés sont ceux qui restent dans le dépôt final ; les autres sont dans la sortie brute.
- Au niveau du projet : ajout de Mockito (première campagne) et exclusion des fichiers générés de la campagne Windows de `checkstyle`/`forbiddenapis` (en-tête de licence absent, imports `*`).

Bilan de la campagne Windows : **0 test généré n'était utilisable sans intervention** à la première tentative ; à la seconde, **37 / 88 (42 %)** passent tels quels sur les deux OS, **48 / 88 (55 %)** après les 11 conditions de plateforme, et il a fallu 51 interventions (une ligne chacune) pour obtenir une suite verte.

#### Les deux campagnes ProcessUtils côte à côte

Même outil, même modèle, même classe, mêmes paramètres, deux machines : les résultats diffèrent sensiblement, ce qui illustre le non-déterminisme de la génération (et de la validation « compile et s'exécute » de l'outil).

| | Campagne macOS (Aness) | Campagne Windows (David) |
|---|---|---|
| Durée | 1 h 11 (30 tentatives) | 40 min pour `ProcessUtils` (30 tentatives) + 22 min `CharsetUtils` |
| Tentatives retenues par l'outil | 9 / 30 (30 %) | 13 / 30 (43 %) |
| Tests dans les fichiers retenus | 40 | 67 |
| Tests qui passent tels quels sous Maven (hors OS) | 29 / 40 | 23 / 67 sur les deux OS |
| Méthodes sans aucun test retenu | `execute(int,int)`, `checkCommand` ×2 | aucune (mais `execute`/`checkCommand` retenus avec des oracles tous faux) |
| Mutants supplémentaires tués par les tests générés (scénario B) | 6 (dont 3 sur `execute(Path)`) | 2 (`escapeCommandLine` l. 62-63) |
| Erreurs dominantes | symbole introuvable, `void` comme valeur, appel des méthodes privées `register`/`release`, mock de `Runtime` | mocks de `Process`/`ProcessBuilder` jamais utilisés, `processTimeMillis == 1000`, `when()` sur un champ `static final` |

La campagne macOS, plus « chanceuse » sur `execute(Path)`, a été retenue pour le dépôt final (tests corrigés, conformes au style Tika). La campagne Windows apporte `CharsetUtils` et la comparaison.



## 5. Critique des tests générés et comparaison des oracles

### 5.1 Campagne macOS : critique des oracles (ProcessUtils)

**Qualité des oracles générés.**

- *Pertinents pour `escapeCommandLine` / `unescapeCommandLine`.* Cas nul, argument sans espace, avec espaces, déjà entre guillemets : ce sont les bonnes classes d'équivalence. Les oracles (`assertEquals(entrée, résultat)` ou version entre guillemets) sont corrects, **à condition de tenir compte du système d'exploitation**, ce que le modèle ne fait pas.
- *Redondants.* Les 5 fichiers `escapeCommandLine` contiennent environ 30 tests, mais testent presque les mêmes 4 ou 5 cas (nul, sans espace, espace, guillemets). Seuls 2 mutants supplémentaires sont tués grâce à eux : `arg == null` (ligne 62) et le retour `""` de la ligne 63.
- *Faux ou triviaux pour `execute`.* Le seul fichier retenu vérifie des valeurs que la méthode ne peut pas produire (`1000`, `100`, `"line1\nline2"`), et ne vérifie ni le contenu du fichier de redirection ni le comportement réel d'un processus. Une fois corrigé, il se contente d'`assertTrue(temps >= 0)` et de longueurs nulles : ce sont des vérifications triviales qui ne décrivent pas le comportement attendu.
- *Faible spécificité.* Aucun test généré ne lance un vrai processus : tout est simulé, donc ni la lecture réelle de `stdout` / `stderr`, ni la destruction du processus, ni le délai ne sont réellement exercés.
- *Aucune réflexion sur le non-déterminisme.* Rien sur la concurrence (threads de lecture des flux), les temps d'attente, la fuite de processus.
- *Aucun test pour `checkCommand`* : le modèle ne sait pas contourner `Runtime.getRuntime()` (méthode statique), alors que `mockStatic` de Mockito le permet (nous l'utilisons en §8.2).

**Comparaison avec les oracles écrits à la main.** Le seul test original qui touche la classe, `FileCommandDetectorTest#testBasic`, vérifie un comportement de haut niveau (type MIME détecté par la commande `file`) : son oracle est précis mais très indirect, il ne dit rien sur les cas limites de `ProcessUtils`. Nos tests manuels (§8) vérifient au contraire chaque champ de `FileProcessResult` (`stdout`, `stderr`, `exitValue`, `isTimeout`, longueurs, troncatures, temps), avec de **vrais processus** (`sh -c`) dont la sortie est connue à l'avance, ce qui donne des oracles exacts et indépendants du code (par exemple `echo hello; exit 3` doit donner `stdout == "hello"` et `exitValue == 3`). Les oracles générés, eux, sont déduits du code source lu par le modèle : ils reflètent l'implémentation (y compris ses défauts) plutôt qu'une spécification indépendante, et deviennent faux dès que le modèle se trompe sur le comportement réel (système d'exploitation, mocks jamais utilisés).


### 5.2 Campagne Windows : critique des oracles (ProcessUtils et CharsetUtils)

**Ce que le modèle fait bien.**

- Sur une fonction pure et bien documentée (`escapeCommandLine`), les cas sont pertinents et proches de ce qu'un humain écrirait : `null`, sans espace, avec espace, déjà entre guillemets, guillemet d'un seul côté. `ProcessUtils_escapeCommandLine_2_1_Test.testEscapeCommandLine_WithQuotesAndSpaces_Windows` teste même l'entrée `"test arg" with spaces` (guillemet au début mais pas à la fin), un cas que nous n'avions pas pensé à écrire – avec toutefois un oracle faux (il attend un second échappement que le code ne fait jamais).
- Les oracles sont **spécifiques** (`assertEquals` sur la valeur exacte, `assertNull`) plutôt que de simples `assertNotNull` ; on ne trouve pas de test « ça ne plante pas ».
- `CharsetUtils_clean_2_4_Test` couvre systématiquement le nettoyage HTTP (espaces, virgules, casse) et les mots réservés `none`/`no`, avec 12 oracles corrects sur 19.
- La couverture visée est la couverture de branches : pour `checkCommand`, le modèle a bien identifié toutes les issues (succès, code d'erreur, `TimeoutException`, `IOException`, `SecurityException`, `Error`) – c'est la mise en œuvre qui échoue, pas l'intention.

**Ce qui ne va pas.**

1. **Oracles déduits de la lecture du code plutôt que de son exécution.** Le modèle *simule* mentalement le code et se trompe dès que la sémantique est subtile : il attend `CP1252` et `WINDOWS-1252` là où `Charset.name()` renvoie le nom canonique `windows-1252` ; il attend que `"iso 8859-1"` soit reconnu alors que la regex coupe au premier espace ; il attend qu'une virgule en tête soit ignorée alors que seule l'ancienne regex `[ "]*` l'autorise pour espaces et guillemets. Un humain qui doute exécute le code (c'est ce que nous avons fait pour `8859-99` ou `cp-9999`, cf. §7) ; le modèle écrit la valeur qu'il *croit* correcte, et `prune=false` laisse passer l'erreur.
2. **Oracles tautologiques ou absurdes sur le comportement temporel.** Les 12 tests d'`execute` affirment `assertEquals(1000, result.processTimeMillis)` – c'est-à-dire que la durée mesurée serait *exactement* égale au timeout, même quand le processus se termine immédiatement. Nos tests manuels n'encadrent la durée que par des bornes (`>= 400`, `< 10000`).
3. **Mocks hors de propos.** Le *prompt* par défaut de ChatUniTest pousse vers Mockito ; le modèle crée donc des `@Mock Process`, `@Mock StreamGobbler`, `@InjectMocks ProcessUtils` alors que **toutes les méthodes sont statiques** et instancient elles-mêmes leurs collaborateurs : les mocks ne sont jamais utilisés, `verify(mockProcess).waitFor(...)` échoue forcément, et `when(systemUtils.IS_OS_WINDOWS)` tente de stubber un champ `static final`. Le modèle ne tire aucune leçon des 5 rondes de réparation : il reformule le même mock. Nos tests manuels choisissent l'approche inverse – un vrai sous-processus contrôlé (`Helper`) – précisément parce que la classe ne s'y prête pas au *mocking*.
4. **Contradictions internes.** Dans chaque fichier `escapeCommandLine_*`, un test « Windows » et un test « non-Windows » coexistent sans condition : la suite ne peut passer sur aucune machine. Le modèle « connaît » la dépendance à l'OS (il nomme les tests ainsi) mais ne sait pas l'exprimer avec `assumeTrue`.
5. **Vérifications triviales.** `CharsetUtils_clean_2_4_Test.testCleanWithWinNamePatternAndWhitespace` est une méthode **vide** (passe toujours) ; `ProcessUtils_execute_4_4_Test` stubbe six méthodes de `StreamGobbler` dans `@BeforeEach` sans qu'aucune ne soit appelée ; plusieurs tests de `clean` sont des doublons à un espace près (`"UTF-8,"`, `"UTF-8,  "`, `"  UTF-8,  "`).
6. **Redondance sans valeur de mutation.** Les cinq fichiers `escapeCommandLine_2_{0..4}` sont quasi identiques (35 tests pour 4 branches) : ils ne tuent pas plus de mutants qu'un seul (voir §6).
7. **Pas de connaissance du contexte d'exécution.** `checkCommand_6_1` lance réellement `echo test` : cela fonctionne sous Linux mais pas sous Windows (pas d'exécutable `echo`), et `invalid_command` est attendu comme levant une `IOException` alors que la méthode l'attrape (`testCheckCommandWithInvalidCommand`).

**En résumé**, les tests générés sont *pertinents dans leurs intentions* (choix des entrées, branches visées) mais leurs *oracles ne sont fiables que pour les fonctions pures et simples* ; dès qu'il y a un effet de bord (processus, temps, OS) ou une bibliothèque mal adaptée (Mockito sur des méthodes statiques), ils affirment des valeurs fausses avec la même assurance. Le temps de génération (1 h) est comparable au temps d'écriture manuelle des tests de la §8, mais il faut y ajouter le temps de tri : 51 tests sur 88 ont dû être revus un par un.


## 6. PIT : installation et scores de mutation

[PIT](https://pitest.org) est ajouté dans `tika-core/pom.xml` (profil `pitest`) : `pitest-maven` 1.25.9 avec `pitest-junit5-plugin` 1.2.3 (nécessaire car Tika utilise JUnit Jupiter 6). Configuration : mutateurs `DEFAULTS`, cibles `org.apache.tika.utils.ProcessUtils` et `org.apache.tika.utils.CharsetUtils`, rapports HTML + XML + CSV, `timeoutConstant` porté à 20 s parce que `ProcessUtils` lance de vrais processus. Les tests à utiliser sont passés par `-DtargetTests=…`, ce qui permet de comparer plusieurs scénarios avec la même configuration (commandes en §13).

### Scores (exécution [#24](https://github.com/DavidStanescu13/tika/actions/runs/38435790822) du workflow, après fusion des deux contributions)

| Scénario | Tests utilisés | ProcessUtils (Linux) | ProcessUtils (Windows) | CharsetUtils (Linux = Windows) |
|---|---|---|---|---|
| **A – tests originaux** | `FileCommandDetectorTest`, `CharsetUtilsTest` | **4,9 %** (3/61) | 4,9 % (3/61) | **60,0 %** (15/25) |
| **B – originaux + générés (ChatUniTest)** | A + `ProcessUtils_*_Test` (40), `CharsetUtils_*_Test` (14 actifs) | **14,8 %** (9/61) | 29,5 % (18/61) | **60,0 %** (15/25) |
| **C₁ – originaux + manuels** (contrôle) | A + `ProcessUtils*ManualTest` (60), `CharsetUtilsManualTest` (10) | **77,0 %** (47/61) | 91,8 % (56/61) | **64,0 %** (16/25) |
| **C₂ – originaux + générés + manuels** | B + manuels | **77,0 %** (47/61) | 91,8 % (56/61) | **64,0 %** (16/25) |

Score PIT = (tués + timeouts) / total. Les quatre scénarios sont recalculés à chaque exécution du workflow, sur Linux et sur Windows. Le scénario C₁ sert de contrôle : il vérifie que chaque mutant compté est tué par un test manuel identifiable, et non par une interaction entre classes de test. Dans la suite fusionnée, **C₁ = C₂** sur les deux OS (les tests générés n'ajoutent rien aux tests manuels), et les résultats sont identiques d'une exécution à l'autre. Avant la fusion (exécutions #18-#20), 11 mutants équivalents ou de course apparaissaient « tués » uniquement lorsque les tests générés de la campagne Windows s'exécutaient dans la même JVM PIT que les tests portables : ces faux positifs ont disparu, et ces mutants sont maintenant tués de façon déterministe par les tests simulés d'Aness (§9.1) – sauf les mutants réellement équivalents, qui survivent sur les deux OS (§9.3).

Sous Windows, les 5 mutants restants (`release` l. 50, `checkCommand` l. 273, 276, 279, 286) sont exactement les mutants équivalents identifiés en §9.3 : **le maximum atteignable est atteint sur Windows**, et sous Linux il ne manque que les 9 mutants propres à Windows.

`ProcessUtils` contient du code spécifique à Windows (`escapeCommandLine` / `unescapeCommandLine` ne font quelque chose que si `SystemUtils.IS_OS_WINDOWS`) : le job `tache2-windows` du workflow exécute les mêmes tests et les mêmes scénarios PIT sous Windows, où les 9 mutants propres à ces deux méthodes sont tués en plus.

### Couverture de lignes de ProcessUtils

**Pourquoi pas 100 % de lignes couvertes (140 sur 146).** Les 6 lignes restantes (liste lue dans le rapport PIT du scénario C) ne peuvent pas être atteintes par un test sur macOS ou Linux :

| Lignes de `ProcessUtils.java` | Code | Raison |
|---|---|---|
| 39 et 40 (2 lignes) | corps du *shutdown hook* : `PROCESS_MAP.forEachValue(1, Process::destroyForcibly);` et `}));` | Il ne s'exécute qu'à l'arrêt de la JVM, donc après la fin des tests ; PIT ne le voit jamais. PIT ne génère d'ailleurs aucun mutant dans ce code. |
| 68 et 69 | `escapeCommandLine` : test `startsWith` / `endsWith`, puis ajout des guillemets | Atteint seulement si `SystemUtils.IS_OS_WINDOWS` est vrai. |
| 76 et 77 | `unescapeCommandLine` : test des guillemets, puis `substring` | Idem, uniquement sous Windows. |

Sous Windows, les 4 dernières lignes seraient couvertes par les tests générés marqués `@EnabledOnOs(WINDOWS)` (non vérifié, faute de machine Windows).


## 7. Mutants détectés par les tests générés

### 7.1 Mutants détectés par les tests générés de la campagne macOS (ProcessUtils, 6 mutants)

| Mutant | Test qui le tue | Pourquoi il est détecté |
|---|---|---|
| `escapeCommandLine` l. 62 : condition `arg == null` remplacée par `false` | `escapeCommandLine_2_0#NullInput` | Sans la garde, `arg.contains(" ")` lève une `NullPointerException` sur l'entrée `null`. |
| `escapeCommandLine` l. 63 : `return arg` remplacé par `""` | `escapeCommandLine_2_4#NullInput` | L'oracle `assertNull` exige que `null` soit rendu tel quel. |
| `unescapeCommandLine` l. 79 : retour remplacé par `""` | `unescapeCommandLine_3_1#QuotesNonWindows` | L'oracle exige que `"\"example\""` soit rendu inchangé (hors Windows). |
| `execute` l. 195 (surcharge avec redirection) : `if (complete)` remplacé par `false` | `execute_5_4#testExecute_Success` | Sans cette branche, le code de sortie n'est jamais lu : il reste `-1` au lieu de `0`. |
| `execute` l. 209 : condition de `isTimeout = !complete` remplacée par `false` | `execute_5_4#testExecute_Timeout` | L'oracle attend `isTimeout == true` quand `waitFor` rend `false`. |
| `execute` l. 215 : retour remplacé par `null` | `execute_5_4#testExecute_Timeout` | L'oracle déréférence le résultat. |

Ces 6 mutants, ajoutés aux 3 des tests originaux, font les 9 du scénario B. PIT ne nomme qu'un seul test par mutant (le premier à échouer) : plusieurs tests peuvent tuer le même mutant, et l'annexe peut donc en citer un autre.

La couverture de lignes passe de 48 % à 77 % parce que les tests générés **atteignent** `escapeCommandLine`, `unescapeCommandLine` et la surcharge avec redirection, mais **leurs assertions sont trop faibles** pour tuer la majorité des mutants : 33 survivent encore et 19 ne sont toujours pas couverts. Les mocks suppriment justement les comportements que les mutants modifient (threads, destruction du processus, délais).


### 7.2 Mutants détectés par les tests générés de la campagne Windows (ProcessUtils et CharsetUtils)

Mesuré avant la fusion (exécutions #18-#20, scénario B avec les 48 tests actifs de cette campagne) : PIT passait de 3 à **5 mutants tués sur 61** pour `ProcessUtils` et restait à **15 / 25** pour `CharsetUtils`. Détail :

| Mutant (ligne, PIT) | Statut en A | Statut en B | Tué par | Pourquoi |
|---|---|---|---|---|
| PU 62 : `if (arg == null)` supprimé | survivant | **tué** | `ProcessUtils_escapeCommandLine_2_0_Test.testEscapeCommandLine_NullInput` (et les 4 autres fichiers `escapeCommandLine_2_x`) | sans la garde, `arg.contains(" ")` lève `NullPointerException` ; le test appelle `escapeCommandLine(null)` et attend `null` |
| PU 63 : `return null` → `return ""` | sans couverture | **tué** | idem | `assertNull(result)` échoue avec `""` |
| PU 71 : `return arg` → `return ""` (escape) | tué (test original) | tué | `testEscapeCommandLine_WithSpaces_NonWindows_Quoted` passe avant le test original dans l'ordre de PIT | `assertEquals("\"example with spaces\"", result)` |
| PU 107, 149 | tués (test original) | tués | `FileCommandDetectorTest.testBasic` | inchangé |
| CU 110–195 (15 mutants) | tués (test original) | tués | `CharsetUtilsTest` | les tests générés de `clean`/`isSupported` recouvrent exactement les mêmes cas que le test original (chemin rapide `utf-8`, `none`/`no`, chaîne vide, `null`) : PIT les liste parfois comme premier test tueur, mais ils n'apportent aucun mutant |

Pourquoi si peu ?

1. **Tout ce qui touchait aux parties non couvertes a été désactivé.** Les 27 tests générés pour `execute` et `checkCommand` – les deux méthodes qui portent 42 des 61 mutants et toutes les branches non couvertes (timeout, redirection, codes d'erreur) – échouent tous (§4.2) : ils n'exercent donc aucun mutant. Les mutants de ces méthodes restent `NO_COVERAGE` ou `SURVIVED` exactement comme en A.
2. **Sur `escapeCommandLine`, le modèle teste les mêmes 4 branches 35 fois.** Les cinq fichiers sont des variantes du même test ; PIT n'a besoin que d'un seul. Les deux mutants gagnés (62, 63) le sont par le cas `null`, que le test original ne couvrait pas. Sous Windows, les tests `_Windows` conditionnés tuent bien les mutants 67/68 en C₂, mais PIT les rapporte survivants en B (ordre d'exécution : les 25 premiers tests de la file passent) – la détection dépend de l'ordre de PIT, ce qui confirme que ces tests sont redondants entre eux.
3. **Sur `CharsetUtils`, les oracles justes sont ceux du chemin déjà testé.** Les 12 tests de `clean` qui passent reprennent les entrées « faciles » (`UTF-8`, `utf-8`, espaces, virgule finale, `none`, `no`, `null`, `""`) : les mutants correspondants étaient déjà tués par `CharsetUtilsTest.testCleaningCharsetName`/`testFunkyNames`. Les tests qui visaient les regex (`cp-1252`, `win-1252`, `iso 8859-1`) avaient un oracle faux et sont désactivés ; `forName` n'a produit aucun test retenu (10 échecs de compilation + 15 d'exécution, le modèle s'obstinant à invoquer `forName` par réflexion comme si elle était privée).
4. **Aucun test généré n'observe l'état interne** (`PROCESS_MAP`) ni ne borne une durée : les mutants 46, 115, 154, 194, 220 ne peuvent pas être atteints par ce style d'oracle.

Comparaison avec les tests manuels de la §8 : ils tuent bien davantage, parce qu'ils (a) exercent les deux surcharges d'`execute` et `checkCommand` avec un vrai sous-processus contrôlé, (b) bornent `processTimeMillis`, (c) vérifient le registre interne par réflexion, et (d) choisissent des entrées là où le code *diffère* de l'intuition (`8859-99`, `cp-9999`, `"NO"` vs `"nonexistent"`), ce que le modèle ne fait que par accident.


## 8. Tests écrits à la main

Quatre classes, 70 tests, toutes dans `tika-core/src/test/java/org/apache/tika/utils/` :

| Classe | Auteur | Tests | Approche |
|---|---|---|---|
| `ProcessUtilsManualTest` | Aness | 16 | vrais processus `sh -c` (désactivée sous Windows) |
| `ProcessUtilsMockedManualTest` | Aness | 26 | `Runtime`, `ProcessBuilder`, `Process` simulés avec Mockito (`mockStatic`), flux retardés/bloqués déterministes |
| `ProcessUtilsPortableManualTest` | David | 18 | vrai sous-processus **JVM** contrôlé (`Helper`), portable Linux/Windows, sorties volumineuses (2 Mo), registre interne par réflexion |
| `CharsetUtilsManualTest` | David | 10 | fonction pure : exceptions de `forName`, regex, chemin JDK |

### Tests manuels ProcessUtils d'Aness (42 tests)

Deux classes dans `tika-core/src/test/java/org/apache/tika/utils/`.

- [`ProcessUtilsManualTest`](tika-core/src/test/java/org/apache/tika/utils/ProcessUtilsManualTest.java) (16 tests) : **vrais processus** `sh -c`, donc désactivée sous Windows (`@DisabledOnOs(WINDOWS)`). L'oracle est la sortie connue de la commande shell.
- [`ProcessUtilsMockedManualTest`](tika-core/src/test/java/org/apache/tika/utils/ProcessUtilsMockedManualTest.java) (26 tests) : `Runtime`, `ProcessBuilder` et `Process` **simulés** avec Mockito (`mockStatic(Runtime.class)` pour `checkCommand`). Indépendante du système d'exploitation, déterministe, sans attente de 60 s.

Constantes : délai court `300 ms`, délai long `20 s` (jamais atteint), `137` = code de sortie Java d'un processus tué par `SIGKILL` (128 + 9).

### 8.1 `ProcessUtilsManualTest` (Aness) : vrais processus `sh -c`

| # | Test | Intention | Données choisies | Oracle |
|---|---|---|---|---|
| 1 | `executeCapturesStdoutStderrAndExitValue` | Chemin nominal de `execute` : `stdout`, `stderr`, code de sortie, longueurs, temps. | `echo hello; echo oops 1>&2; exit 3` : sortie sur les deux flux et code de sortie non nul. | `stdout="hello"`, `stderr="oops"`, `exitValue=3`, longueurs 5 et 4, pas de timeout, `0 <= temps < 20 s` (la borne haute détecte `-` devenu `+` dans le calcul du temps). |
| 2 | `executeTruncatesStdoutAndStderrAtTheirOwnLimits` | Chaque tampon est tronqué à *sa* limite. | `printf abcdefghij` sur `stdout` (limite 5) et `printf klmnopqrst` sur `stderr` (limite 7), deux limites différentes pour détecter une inversion. | `"abcde"`, `"klmnopq"`, deux indicateurs de troncature à vrai. |
| 3 | `executeKillsProcessThatExceedsTimeout` | Un processus trop long est tué et son code récupéré. | `sleep 30` avec un délai de 300 ms. | `isTimeout`, `exitValue == 137` (preuve que `destroyForcibly` a été appelé), temps >= 300 ms. |
| 4 | `executeKeepsOutputProducedBeforeTimeout` | La sortie produite avant le délai est conservée. | `echo before; echo before-err 1>&2; sleep 30`, délai 300 ms. | `stdout="before"`, `stderr="before-err"`. |
| 5 | `executeReportsInterruption` | Le thread appelant interrompu reçoit `-1000`. | On pose le drapeau d'interruption avant l'appel : `waitFor` lève `InterruptedException` aussitôt. | `exitValue == -1000`, `isTimeout`, temps `-1` (jamais mesuré). Le drapeau est nettoyé dans un `finally`. |
| 6 | `executeRedirectWritesStdoutToFileAndCreatesMissingDirectories` | Surcharge avec redirection : le fichier est écrit et les répertoires manquants sont créés. | Fichier dans `.../does/not/exist/out.txt` (3 niveaux inexistants) ; `echo hello; echo oops 1>&2`. | Contenu du fichier `"hello\n"`, `stdoutLength == 6`, `stdout == ""`, `stderr == "oops"`, code 0. |
| 7 | `executeRedirectWithExistingDirectoryAndNonZeroExit` | Le répertoire existe déjà ; code de sortie non nul. | `printf abc; exit 2`. | Fichier `"abc"`, longueur 3, code `2`. |
| 8 | `executeRedirectTruncatesStderrOnly` | La limite ne s'applique qu'à `stderr` dans cette surcharge. | `printf klmnopqrst 1>&2`, limite 4. | `"klmn"`, `isStderrTruncated`, `stdout` non tronqué. |
| 9 | `executeRedirectTimeoutLeavesExitValueUnset` | Spécificité de cette surcharge : après le `destroyForcibly` elle ne lit pas le code de sortie. | `echo partial; sleep 30`, délai 300 ms. | `isTimeout`, `exitValue == -1`, contenu partiel `"partial\n"` du fichier. |
| 10 | `executeRedirectReportsInterruption` | Comme le test 5, pour la surcharge avec redirection. | Drapeau d'interruption + `sleep 30`. | `exitValue == -1000`, `isTimeout`, temps `-1`. |
| 11 | `checkCommandReturnsTrueWhenCommandSucceeds` | Une commande qui réussit rend `true` (les deux surcharges). | `true`, `sh -c "exit 0"`. | `true`. |
| 12 | `checkCommandOnlyTreats127AsErrorByDefault` | La valeur d'erreur par défaut est 127 seulement. | `exit 127` / `exit 1` / `false` (code 1). | 127 donne `false` ; 1 donne `true` (la commande existe, elle a seulement échoué). |
| 13 | `checkCommandUsesGivenErrorValuesInsteadOfTheDefault` | Les valeurs fournies remplacent la valeur par défaut, y compris plusieurs valeurs. | `exit 1` avec `(1)`, `(5,1,9)`, `(2,3)` ; `exit 127` avec `(1)`. | `false`, `false`, `true`, et `true` (127 n'est plus une erreur). |
| 14 | `checkCommandReturnsFalseWhenCommandCannotBeStarted` | Un exécutable introuvable rend `false` et non une exception. | `this-command-does-not-exist-ift3913` (avec et sans argument). | `false`. |
| 15 | `checkCommandDrainsLargeStdoutSoTheProcessCanFinish` | Le thread de `stdout` doit vider le tube sinon le processus bloque. | `head -c 300000 /dev/zero` (300 Ko, plus que le tampon de 64 Ko d'un tube). | `true` rapidement ; sans lecteur, le processus bloque et PIT détecte un timeout. |
| 16 | `checkCommandDrainsLargeStderrSoTheProcessCanFinish` | Même chose pour `stderr`. | `head -c 300000 /dev/zero 1>&2`. | `true`. |

### 8.2 `ProcessUtilsMockedManualTest` (Aness) : objets simulés

Outils : `DelayedStream` (flux muet pendant N ms, puis une donnée), `BlockedStream` (flux bloqué 30 s qui signale son interruption), accès par réflexion à `PROCESS_MAP`. Les tests de `checkCommand` appellent `ProcessUtils.escapeCommandLine(null)` dans un `@BeforeAll` : le bloc `static` de la classe enregistre un *shutdown hook* via `Runtime.getRuntime()` et doit s'exécuter **avant** que `Runtime` ne soit simulé.

| # | Test | Intention | Données choisies | Oracle |
|---|---|---|---|---|
| 17 | `checkCommandReturnsFalseAndKillsProcessWhenItNeverFinishes` | Le délai (60 s en dur) fait rendre `false` et détruit le processus, sans attendre 60 s. | `waitFor(60000 ms)` simulé à `false`. | `false` et `destroyForcibly()` appelé une fois. |
| 18 | `checkCommandReturnsFalseWhenInterrupted` | `InterruptedException` rend `false` et nettoie. | `waitFor` lève `InterruptedException`. | `false`, `destroyForcibly()` une fois. |
| 19 | `checkCommandUsesExitValueOfTheFinishedProcess` | Le code de sortie décide du résultat. | `exitValue` simulé à 127 puis à 0. | `false` puis `true`, `destroyForcibly()` deux fois (une par appel). |
| 20 | `checkCommandReturnsFalseOnPosixSpawnError` | Branche TIKA-1526 : une `Error` dont le message contient `posix_spawn` rend `false`. | `Error("error in posix_spawn")`. | `false`. |
| 21 | `checkCommandReturnsFalseOnUnixProcessError` | Même branche pour `UNIXProcess`. | `Error("java.lang.UNIXProcess")`. | `false`. |
| 22 | `checkCommandRethrowsUnrelatedErrors` | Une autre `Error` doit être relancée telle quelle. | `Error("something else")`. | La même instance est relancée (`assertSame`). |
| 23 | `checkCommandRethrowsErrorsWithoutMessage` | Une `Error` sans message ne doit pas provoquer de `NullPointerException`. | `new Error()` (message `null`). | La même instance est relancée. |
| 24 | `checkCommandRethrowsSecurityException` | `SecurityException` n'est pas avalée. | `SecurityException("denied")`. | La même instance est relancée. |
| 25 | `checkCommandWaitsForStdoutReaderToFinish` | `checkCommand` joint le thread de `stdout` avant de rendre la main. | Processus déjà terminé, mais `stdout` met 400 ms à se fermer. | Temps écoulé >= 300 ms (borne basse, donc sans risque de test instable). |
| 26 | `checkCommandWaitsForStderrReaderToFinish` | Idem pour `stderr`. | `stderr` met 400 ms à se fermer. | Temps >= 300 ms. |
| 27 | `executeWaitsForSlowStdoutReaderOfAFinishedProcess` | `execute` joint le thread de `stdout`. | `stdout` lent (500 ms), `stderr` immédiat ; un seul flux lent pour que chaque `join` soit vérifié séparément. | `stdout == "slow-out"`, `stderr == "fast-err"`, code 0, `destroyForcibly()` une seule fois (dans le `finally`). |
| 28 | `executeWaitsForSlowStderrReaderOfAFinishedProcess` | `execute` joint le thread de `stderr`. | `stderr` lent, `stdout` immédiat. | Le contraire du test 27. |
| 29 | `executeWaitsForSlowStdoutReaderAfterKillingProcessOnTimeout` | Après le délai et le kill, `execute` attend encore la fin de lecture. | `waitFor` rend `false` puis `true`, `exitValue` simulé à 137, `stdout` lent. | `isTimeout`, code 137, `stdout` complet, `destroyForcibly()` deux fois. |
| 30 | `executeWaitsForSlowStderrReaderAfterKillingProcessOnTimeout` | Idem côté `stderr`. | `stderr` lent. | Idem. |
| 31 | `executeKeepsMinusOneWhenKilledProcessStillReportsRunning` | Le processus dit être arrêté mais `exitValue` lève `IllegalThreadStateException`. | `waitFor(500)` à `true`, `exitValue` lève l'exception. | `exitValue == -1`, pas d'exception. |
| 32 | `executeKeepsMinusOneWhenKilledProcessDoesNotStopInTime` | Le processus ne s'arrête pas dans les 500 ms. | `waitFor(500)` à `false`. | `exitValue == -1` même si `exitValue()` rendrait 137. |
| 33 | `executeInterruptsStdoutReaderThatIsStillBlocked` | Un thread de lecture qui ne se termine pas sous 1 s est interrompu. | `stdout` bloqué (`BlockedStream`). | Le flux signale son interruption dans les 3 s. |
| 34 | `executeInterruptsStderrReaderThatIsStillBlocked` | Idem côté `stderr`. | `stderr` bloqué. | Idem. |
| 35 | `executeDestroysProcessAndForgetsItWhenInterrupted` | Interrompu : le processus est détruit et retiré de la table. | `waitFor` lève `InterruptedException`. | `exitValue == -1000`, `destroyForcibly()` une fois, `PROCESS_MAP` revient à sa taille d'avant. |
| 36 | `executeDoesNotLeakProcessesInTheInternalMap` | Après une exécution normale, la table est propre. | Exécution normale simulée. | Taille de `PROCESS_MAP` identique avant et après. |
| 37 | `executeDoesNotRegisterOrLeakAProcessThatCannotStart` | Un échec au démarrage ne laisse rien dans la table. | `start()` lève `IOException`. | `IOException` propagée, taille de la table inchangée. |
| 38 | `concurrentExecutionsAreRegisteredUnderDistinctIds` | Chaque processus en cours a son propre identifiant. | Deux threads, deux processus bloqués par un verrou. | Taille de la table = avant + 2 pendant l'exécution, puis retour à la taille d'avant. |
| 39 | `executeRedirectWaitsForStderrReaderOfAFinishedProcess` | Surcharge avec redirection : `join` du thread `stderr`. | `stderr` lent, fichier de redirection préexistant d'un octet. | `stderr == "late-err"`, `stdoutLength == 1`, `destroyForcibly()` une fois. |
| 40 | `executeRedirectWaitsForStderrReaderAfterKillingProcessOnTimeout` | Idem après délai dépassé. | `waitFor` à `false`, `stderr` lent. | `isTimeout`, `stderr == "late-err"`, `destroyForcibly()` deux fois. |
| 41 | `executeRedirectDestroysProcessAndForgetsItWhenInterrupted` | Interruption pour la surcharge avec redirection. | `InterruptedException`. | `-1000`, une destruction, table propre. |
| 42 | `executeRedirectDoesNotLeakProcessesInTheInternalMap` | Pas de fuite pour la surcharge avec redirection. | Exécution normale simulée. | Taille de la table inchangée. |

**Pourquoi le simulacre est ici préférable à un vrai processus.** Pour les tests 17 à 42, un vrai processus rendrait le test lent (60 s pour `checkCommand`), instable (courses entre threads) ou impossible (lever une `Error` de `posix_spawn`). Les flux simulés retardent la donnée de façon *déterministe*, et les assertions de temps n'utilisent que des **bornes basses** (`>=`) : un test lent ne peut donc pas devenir instable.


### 8.3 `ProcessUtilsPortableManualTest` (David) : sous-processus JVM contrôlé, portable Linux/Windows

| Test | Intention (comportement testé) | Motivation des données | Oracle (comportement attendu et pourquoi) |
|---|---|---|---|
| `testExecuteCapturesStdoutStderrAndExitValue` | `execute(pb, timeout, int, int)` capture stdout, stderr, le code de sortie et les longueurs de flux | processus qui écrit 2 lignes sur stdout, 1 sur stderr et sort avec **3** (ni 0 ni -1, pour distinguer « code lu » de « valeur par défaut ») | `exitValue == 3`, `stdout == "out-0\nout-1"` (lignes jointes par `\n` sans `\n` final, conformément à `StringUtils.joinWith`), `stderr == "err-0"`, `stdoutLength == 10` et `stderrLength == 5` (somme des longueurs de lignes **sans** retours à la ligne, comme le compte `StreamGobbler`), pas de timeout, pas de troncature, `0 <= processTimeMillis < 30000` (une durée, pas un horodatage) |
| `testExecuteTruncatesStdoutAtMaxBuffer` | le paramètre `maxStdoutBuffer` tronque la capture sans perdre la longueur réelle | 3 lignes de 5 caractères avec un tampon de **7** : la 1re tient, la 2e est coupée, la 3e est ignorée – trois cas différents de `StreamGobbler.run` en un seul flux | `stdout == "out-0\nou"`, `stdoutTruncated == true`, `stdoutLength == 15` (la longueur comptée continue d'avancer même pour les lignes rejetées), stderr intact |
| `testExecuteTimeoutKillsProcess` | branche timeout : le processus est tué, le résultat le signale, le temps est mesuré | `sleep 20000` avec un timeout de **500 ms** ; le helper écrit `sleeping` sur stderr avant de dormir pour vérifier que ce qui précède le timeout est conservé | `isTimeout == true`, durée murale < 10 s (le processus n'a pas vécu ses 20 s), `400 <= processTimeMillis < 10000`, `stderr == "sleeping"`, `exitValue != -1 && != 0` : après `destroyForcibly()` le code relance `waitFor(500 ms)` et relit le code de sortie du processus tué (137 sous Linux) |
| `testExecuteRedirectsStdoutToFileAndCreatesParentDirs` | `execute(pb, timeout, Path, int)` redirige stdout dans un fichier et crée les dossiers parents manquants | chemin `tmp/nouveau/sous-dossier/stdout.txt` dont le parent **n'existe pas** (précondition vérifiée) ; 2 lignes stdout, 1 ligne stderr | fichier créé contenant exactement `out-0`, `out-1` ; `stdout == ""` (non gardé en mémoire) ; `stdoutLength == Files.size(fichier)` (ici **avec** les `\n`, contrairement à l'autre surcharge) ; `stderr == "err-0"` ; `exitValue == 0` ; pas de timeout |
| `testExecuteRedirectTimeout` | branche timeout de la surcharge avec redirection | `sleep 20000`, timeout 500 ms, fichier de redirection dans un dossier existant | `isTimeout == true`, durée < 10 s, `stderr == "sleeping"`, `stdout == ""`, fichier créé ; **`exitValue == -1`** : cette surcharge ne refait pas de `waitFor` après `destroyForcibly()` (asymétrie avec l'autre surcharge, voir §10) – le test documente le comportement actuel |
| `testExecuteRedirectStderrTruncation` | la troncature de stderr fonctionne aussi dans la surcharge avec redirection | 3 lignes stderr de 5 caractères, tampon 7 | `stderr == "err-0\ner"`, `stderrTruncated == true`, `stderrLength == 15` |
| `testExecuteDrainsLargeOutputCompletely` | les flux sont lus **en parallèle** du processus et **jusqu'au bout** avant de construire le résultat | 20 000 lignes de 100 caractères sur stdout **et** stderr (~2 Mo chacun), soit bien plus que le tampon d'un pipe (~64 Ko) : sans lecteurs concurrents le processus se bloquerait, sans `join` le résultat serait incomplet | `exitValue == 0`, `stdoutLength == stderrLength == 2 000 000`, `stdout.length() == 101·n − 1`, pas de troncature |
| `testExecuteRedirectDrainsLargeStderrCompletely` | idem pour la surcharge avec redirection (stderr en mémoire, stdout dans le fichier) | même flux de 2 Mo | taille du fichier `== (100 + longueur du séparateur de ligne)·n` (`\n` sous Linux, `\r\n` sous Windows – oracle rendu portable après un échec du job Windows), `stdoutLength` égal, `stderrLength == 100·n`, `stderr.length() == 101·n − 1` (le `StreamGobbler` normalise `\r\n`) |
| `testProcessIsUnregisteredAfterExecute` | le registre interne `PROCESS_MAP` (utilisé par le *shutdown hook* pour tuer les processus orphelins) est nettoyé après chaque appel, y compris après un timeout | lecture de `PROCESS_MAP` par réflexion ; trois appels : surcharge int/int, surcharge Path, et un timeout | la taille du registre est identique avant et après chaque appel |
| `testCheckCommandMissingExecutableReturnsFalse` | `checkCommand` renvoie `false` quand l'exécutable n'existe pas (`IOException` de `Runtime.exec`) | nom aléatoire (`UUID`) pour être certain qu'il n'existe pas ; les deux surcharges (`String` et `String[]`) | `false`, sans exception |
| `testCheckCommandStringOverloadSuccess` | la surcharge `checkCommand(String, int...)` délègue correctement | le binaire `java` sans argument : affiche l'aide et sort avec **1** | `true` (1 n'est pas 127) ; `false` si l'on déclare 1 comme code d'erreur |
| `testCheckCommandDrainsLargeOutput` | `checkCommand` consomme stdout/stderr pour ne pas bloquer le processus | `flood 20000` (~2 Mo) | `true` ; si les flux n'étaient pas lus, le processus resterait bloqué sur le pipe plein et `checkCommand` finirait en timeout (`false`) |
| `testCheckCommandDefaultErrorValueIs127` | sans `errorValue`, seul 127 (« command not found ») est considéré comme un échec | codes de sortie 0, **127** et 3 | `true`, `false`, `true` |
| `testCheckCommandHonoursConfiguredErrorValues` | la liste `errorValue` remplace (et non complète) la valeur par défaut, et chaque élément est comparé | code 3 avec listes `{3}`, `{1,2,3}`, `{4}`, `{2,4}` ; code 127 avec `{1}` | `false`, `false`, `true`, `true`, `true` (127 n'est plus implicite dès qu'une liste est fournie) |
| `testEscapeCommandLineNullAndNoSpace` | cas limites communs à tous les OS | `null` ; chemin sans espace | `escapeCommandLine(null) == null` ; sans espace la **même instance** est renvoyée (`assertSame`) par `escape` et `unescape` |
| `testEscapeCommandLineOnWindows` | (exécuté seulement sous Windows) ajout/retrait des guillemets autour d'un argument contenant un espace | chemin avec espace, déjà cité, cité d'un seul côté | `"C:\Program Files\app.exe"` est entouré de guillemets une seule fois ; `unescape` les retire ; guillemet d'un seul côté : inchangé |
| `testEscapeCommandLineIsNoOpOutsideWindows` | (hors Windows) aucune transformation même avec espace | chemin avec espace, chemin cité | même instance renvoyée |
| `testUnescapeCommandLineNullIsNotHandled` | documente que `unescapeCommandLine(null)` lève `NullPointerException` alors que `escapeCommandLine(null)` renvoie `null` | `null` | `assertThrows(NullPointerException)` – test de non-régression du comportement actuel, voir §10 |


### 8.4 `CharsetUtilsManualTest` (David)

| Test | Intention | Motivation des données | Oracle |
|---|---|---|---|
| `testForNameNullThrowsIllegalArgument` | `forName(null)` est rejeté avant toute analyse | `null` | `IllegalArgumentException` qui **n'est pas** une `IllegalCharsetNameException` (branche `name == null` distincte de la branche regex) |
| `testForNameUnparseableNameThrowsIllegalCharsetName` | la regex `[ "]*([^ >,;"]+).*` exige au moins un caractère utile | `""`, `"   "`, `"\"\""` | `IllegalCharsetNameException` |
| `testForNameNoneAndNoAreRejected` | `none` et `no` (valeurs vues dans des en-têtes HTTP) sont des mots réservés, insensibles à la casse, mais pas leurs préfixes | `none`, `NO`, `nonexistent` | `IllegalCharsetNameException` avec `getCharsetName() == "none"` ; `NO` → même exception ; `nonexistent` → `UnsupportedCharsetException` (il va jusqu'au JDK) – différence invisible à travers `clean()`, qui renvoie `null` dans les deux cas |
| `testForNameUnknownButWellFormedThrowsUnsupported` | nom syntaxiquement valide mais inconnu | `bogus-charset-123` | `UnsupportedCharsetException` ; `clean` → `null` |
| `testForNameCommonCharsetIsCaseInsensitiveAndCached` | la table `COMMON_CHARSETS` est consultée en minuscules et contient les alias JDK et la faute de frappe `iso-8851-1` | `utf-8`, `UTF-8`, `Utf8`, `latin1`, `ISO-8851-1` | la **même instance** `StandardCharsets.UTF_8` / `ISO_8859_1` (`assertSame`) |
| `testForNameExtractsFirstTokenOfHttpStyleValue` | nettoyage des valeurs de type HTTP (guillemets, `;`, `,`, `>`) | `  "utf-8"; q=0.5`, `utf-8,latin1`, `"" utf-8`, `utf-16be>` | UTF-8 / UTF-16BE |
| `testForNameIsoPatternRequiresKnownPart` | regex `.*8859-(\d+)` : préfixe quelconque, mais le numéro doit exister dans la table | `x8859-7`, `8859-2`, `8859-99` | `ISO-8859-7`, `ISO-8859-2`, puis `null` / `UnsupportedCharsetException` pour `8859-99` |
| `testForNameCpAndWinPatterns` | regex `cp-(\d+)` et `win-?(\d+)` ; numéro inconnu ; `cp1252` sans tiret passe par l'alias JDK | `CP-1252`, `win1253`, `WIN-1255`, `cp-9999`, `win-9999`, `cp1252` | `windows-1252/1253/1255`, `null`, `null`, `windows-1252` |
| `testForNameFallsBackToJdkForNonCommonCharset` | un charset absent de la table et des regex est résolu par `Charset.forName` | `UTF-32` | `UTF-32` |
| `testIsSupportedDelegatesToJdkWithoutIcu` | sans ICU4J, `isSupported` délègue au JDK et absorbe `IllegalCharsetNameException` | `ISO-8859-1`, `utf-32`, `not-a-charset-xyz`, `utf 8` (espace), `!!` | `true`, `true`, `false`, `false`, `false` sans exception |


## 9. Mutants détectés par les tests manuels, mutants restants

### 9.1 ProcessUtils : mutants détectés par les tests manuels d'Aness (38 mutants)

38 mutants supplémentaires sont tués (voir l'annexe pour le test qui tue chacun). Par famille :

- **`join` des threads de lecture, 8 mutants** (`execute` l. 118, 119, 122, 123, 197, 200 et `checkCommand` l. 267, 268) : tués par des flux simulés qui n'écrivent ou ne se ferment qu'après 400 à 500 ms (`DelayedStream`). Si la méthode ne joint pas le thread, la sortie est vide ou la durée trop courte. Un seul flux est lent à la fois : sinon attendre l'un laisserait l'autre finir et la suppression d'un `join` passerait inaperçue.
- **`Thread::start`, 4 mutants** (`execute` l. 110 et 189, `checkCommand` l. 261 et 262) : sans lecteur, la sortie est vide ou, pour de grosses sorties, le processus bloque (les deux mutants de `checkCommand` sont comptés « tués par timeout » par PIT).
- **`Thread::interrupt`, 2 mutants** (`execute` l. 136, 137) : tués par un flux qui bloque 30 s et signale son interruption (`BlockedStream`).
- **`execute`, 10 autres mutants** (comparaisons `if (complete)`, `completed`, `p != null`, `id != null`, `!isDirectory`, calculs de temps `-` devenu `+`) : tués par les tests avec vrais processus (délai dépassé, code `137`, bornes sur le temps, création des répertoires) et par les simulacres (`destroyForcibly` appelé exactement une ou deux fois, `PROCESS_MAP` revenue à sa taille d'avant).
- **`register`, 1 mutant** (identifiant remplacé par `""`) : tué par les tests qui inspectent la table interne `PROCESS_MAP` par réflexion (aucune fuite après `execute`, y compris en cas d'interruption ou d'échec au démarrage, et deux exécutions simultanées ont deux entrées distinctes).
- **`checkCommand`, 13 autres mutants** : branche du délai de 60 s (simulée avec `mockStatic(Runtime)`), `Error` de `posix_spawn` / `UNIXProcess`, relance des autres `Error` et de `SecurityException`, valeurs d'erreur par défaut (127) ou fournies, commande introuvable.


### 9.2 ProcessUtils et CharsetUtils : mutants tués par les tests manuels de David (portables + CharsetUtils)

Avant la fusion (exécutions #4 à #20), `ProcessUtilsPortableManualTest` seul faisait passer `ProcessUtils` de 3 à 28 mutants détectés sous Linux (37 sous Windows) et `CharsetUtilsManualTest` faisait passer `CharsetUtils` de 15 à 16. Les lignes ci-dessous expliquent *pourquoi* chaque test détecte son mutant ; dans la suite fusionnée, PIT attribue souvent le même mutant à un test d'Aness (§9.1), le premier à échouer dans son ordre d'exécution. Détail (numéros de ligne de [`ProcessUtils.java`](tika-core/src/main/java/org/apache/tika/utils/ProcessUtils.java) et [`CharsetUtils.java`](tika-core/src/main/java/org/apache/tika/utils/CharsetUtils.java)) :

| Ligne | Mutant (PIT) | Tué par | Pourquoi le test le détecte |
|---|---|---|---|
| PU 46 | `register` renvoie `""` au lieu de l'UUID | `testProcessIsUnregisteredAfterExecute` | l'entrée est enregistrée sous le vrai UUID mais `release("")` ne retire rien : le registre `PROCESS_MAP` grossit à chaque appel, ce que le test détecte en comparant sa taille avant/après |
| PU 62, 63 | `if (arg == null)` supprimé ; `return null` → `return ""` | `testEscapeCommandLineNullAndNoSpace` | `escapeCommandLine(null)` doit renvoyer `null` (le mutant lève `NPE` ou renvoie `""`) |
| PU 71, 79 | `return arg` → `return ""` (escape / unescape) | `testEscapeCommandLineIsNoOpOutsideWindows` | `assertSame` exige l'instance d'origine |
| PU 107, 110 | suppression de `outThread.start()` / `errThread.start()` | `testExecuteCapturesStdoutStderrAndExitValue` | sans lecteur, `stdout`/`stderr` capturés sont vides au lieu de `out-0\nout-1` / `err-0` |
| PU 115, 194 | `elapsed = now − start` → `now + start` | `testExecuteCapturesStdoutStderrAndExitValue`, `testExecuteRedirectsStdoutToFileAndCreatesParentDirs` | l'oracle borne `processTimeMillis < 30 000` : un horodatage (≈1,7·10¹²) viole la borne |
| PU 125 | `if (completed)` forcé à faux après le timeout | `testExecuteTimeoutKillsProcess` | le code de sortie n'est plus relu : `exitValue == -1`, le test exige `≠ -1` |
| PU 143, 209 | `isTimeout = !complete` forcé | `testExecuteTimeoutKillsProcess`, `testExecuteRedirectTimeout` | `isTimeout()` doit être vrai après un timeout |
| PU 149, 215 | `execute` renvoie `null` | tous les tests `execute` | `NPE` sur le résultat |
| PU 154, 220 | `if (id != null) release(id)` forcé à faux | `testProcessIsUnregisteredAfterExecute` | le registre garde une entrée après l'appel |
| PU 174 | `if (!Files.isDirectory(parent))` forcé à faux | `testExecuteRedirectsStdoutToFileAndCreatesParentDirs` | le dossier parent n'est pas créé → `IOException` au `start()` |
| PU 189 | suppression de `errThread.start()` (surcharge `Path`) | `testExecuteRedirectStderrTruncation` | `stderr` vide |
| PU 195 | `if (complete)` forcé à faux (surcharge `Path`) | `testExecuteRedirectStderrTruncation` | dans **cette** surcharge la branche `else` ne relit pas le code de sortie : `exitValue == -1` au lieu de `0` |
| PU 237 | `checkCommand(String, …)` → `true` / `false` constant | `testCheckCommandStringOverloadSuccess`, `testCheckCommandMissingExecutableReturnsFalse` | les deux issues (`true` pour `java`, `false` pour une commande inexistante) sont vérifiées |
| PU 250 | `if (errorValue.length == 0)` forcé à faux | `testCheckCommandDefaultErrorValueIs127` | 127 n'est plus un code d'erreur par défaut → `checkCommand` renvoie `true` pour un exit 127 |
| PU 261, 262 | suppression de `outThread.start()` / `errThread.start()` dans `checkCommand` | `testCheckCommandDrainsLargeOutput` (**TIMED_OUT**) | personne ne vide le pipe : le processus (2 Mo de sortie) se bloque, `waitFor(60 s)` ne rend pas la main → PIT déclare le mutant détecté par timeout |
| PU 272, 273, 276, 279 | comparaison `result == err` forcée ; `return false`/`true` inversés | `testCheckCommandStringOverloadSuccess`, `testCheckCommandMissingExecutableReturnsFalse` | les tests attendent explicitement `false` pour un code listé et `true` sinon, et `false` quand `exec` échoue |
| CU 159 | `"no".equals(lower)` forcé à faux | `testForNameNoneAndNoAreRejected` | `forName("NO")` lève alors `UnsupportedCharsetException` (du JDK) au lieu de `IllegalCharsetNameException` ; le test original ne voyait pas la différence car `clean()` renvoie `null` dans les deux cas |


### 9.3 ProcessUtils : mutants non détectés sous Linux/macOS (14 mutants) et pourquoi

| Mutant(s) | Raison | Détectable ? |
|---|---|---|
| `checkCommand` l. 273, 279, 286 (retour remplacé par `false`) et l. 276 (retour remplacé par `true`) | **Mutants équivalents** : ces lignes retournent déjà exactement la valeur par laquelle PIT les remplace (`return false` devient `return false`). | Non, le comportement est identique. |
| `release` l. 50 : retour remplacé par `null` | Le résultat de `release` n'est jamais lu (`release(id);` en instruction seule). | Non, équivalent. |
| `escapeCommandLine` l. 67 (2 mutants, survivants) et l. 68 (2 mutants, sans couverture) ; `unescapeCommandLine` l. 75 (2 survivants), l. 76 (2 sans couverture) et l. 77 (1 sans couverture) | **Code exécutable seulement sous Windows** : la conjonction contient `SystemUtils.IS_OS_WINDOWS`, une constante `static final` fausse sur macOS et Linux ; hors Windows, supprimer l'une des autres conditions ne change rien (la conjonction reste fausse), et les lignes 68, 76 et 77 ne sont jamais atteintes. | **Oui, sous Windows seulement** – confirmé par le job Windows du workflow : les tests `@EnabledOnOs(WINDOWS)` générés et `testEscapeCommandLineOnWindows` (§8.3) les tuent tous les 9 (voir §6). |

Sous macOS et Linux, 47 est donc le maximum atteignable (47 sur les 56 mutants non équivalents, les 9 autres exigeant Windows) ; sous Windows, le job `tache2-windows` mesure le complément.


### 9.4 Mutants non détectés par les tests portables / CharsetUtils, et explication

**ProcessUtils – avec les seuls tests portables (avant fusion) : 33 mutants non détectés sous Linux, 24 sous Windows.** La plupart des lignes « de course » ci-dessous (`join`, `interrupt`) sont en fait tuées de façon déterministe par les tests *simulés* d'Aness (§8.2, flux `DelayedStream`/`BlockedStream`) : ce tableau documente la limite d'une approche « vrai processus », et pourquoi les deux approches sont complémentaires.

| Catégorie | Lignes | Explication |
|---|---|---|
| Propres à Windows | 67 (×2), 68 (×2), 75 (×2), 76 (×2), 77 | `arg.contains(" ") && SystemUtils.IS_OS_WINDOWS && …` : sous Linux `IS_OS_WINDOWS` est faux, les sous-conditions suivantes ne sont jamais évaluées (`NO_COVERAGE`) et forcer les deux premières à faux ne change rien (équivalents **sur Linux**). **Tous les 9 sont tués sous Windows** par `testEscapeCommandLineOnWindows` (job `tache2-windows`, rapports sur la branche [`tache2-reports-windows`](https://github.com/DavidStanescu13/tika/tree/tache2-reports-windows)). |
| Branche `catch (Error err)` de `checkCommand` | 283 (×2), 284, 286 (×2) | n'est atteinte que si `Runtime.exec` lève une `Error` dont le message contient `posix_spawn` ou `UNIXProcess` (bug JDK historique TIKA-1526) ; impossible avec un vrai JDK, mais `mockStatic(Runtime)` permet de la lever (§8.2, tests 20-23). **Tués (sauf 286 « → false », équivalent) dans la suite fusionnée.** |
| Mutants équivalents par construction | 273 (`return false` → `false`), 276 (`return true` → `true`), 279 (`return false` → `false`) | PIT remplace une valeur de retour par la même valeur : aucun test ne peut les distinguer. |
| `if (p != null) p.destroyForcibly()` dans les blocs `finally` | 151, 217, 290 | quand on arrive au `finally` le processus est soit terminé soit déjà détruit ; supprimer ce second `destroyForcibly` n'a pas d'effet observable (équivalent). |
| `if (complete)` de la 1re surcharge | 116 | avec un vrai processus terminé, la branche `else` fait `destroyForcibly` (sans effet), `waitFor(500)` puis relit `exitValue` : résultat identique → quasi équivalent, tué seulement de façon intermittente. Avec un `Process` simulé dont `waitFor(500)` n'est pas stubbé, la différence devient observable (§8.2, test 27). **Tué dans la suite fusionnée.** |
| Valeur de retour de `release` | 50 | `release(id)` est appelé sans utiliser la valeur renvoyée → équivalent. |
| Suppression des `join(1000)` sur les lecteurs de flux | 118, 119, 122, 123, 197, 200, 267, 268 | avec un **vrai** processus, ce sont des mutants « de course » : sans `join`, le résultat est construit avant que le lecteur ait fini, mais le lecteur ne met que quelques millisecondes à vider le pipe, donc un test ne l'attraperait que « parfois ». La solution déterministe est celle des tests simulés d'Aness : un flux qui ne se ferme qu'après 400-500 ms (`DelayedStream`) rend l'absence de `join` observable à coup sûr (§8.2, tests 25-30, 39-40). **Tués dans la suite fusionnée.** |
| Suppression des `interrupt()` dans le `finally` | 136, 137 | avec un vrai processus détruit, les lecteurs se terminent d'eux-mêmes (fin de flux) : inobservable. Avec un flux simulé qui **bloque** 30 s et signale son interruption (`BlockedStream`), l'absence d'`interrupt()` devient observable (§8.2, tests 33-34). **Tués dans la suite fusionnée.** |
| `if (!finished) throw new TimeoutException()` | 264 | avec un vrai processus il faudrait attendre les 60 s codées en dur ; avec `mockStatic(Runtime)` et un `Process` simulé dont `waitFor` rend `false` immédiatement, la branche est exercée sans attendre (§8.2, test 17). **Tué dans la suite fusionnée.** |

**CharsetUtils – 9 mutants (4 survivants + 5 sans couverture)**

| Catégorie | Lignes | Explication |
|---|---|---|
| Chemin ICU4J | 107 (2e condition), 108, 183, 186, 187 | `com.ibm.icu.charset.CharsetICU` n'est pas sur le classpath de `tika-core` : `isSupportedICU` et `getCharsetICU` valent `null`. Forcer `if (isSupportedICU != null)` / `if (getCharsetICU != null)` à faux est équivalent, et le corps de ces `if` est injoignable. Les couvrir exigerait d'ajouter ICU4J en dépendance de test, ce qui changerait le comportement de **tous** les tests de la classe. |
| `catch (Exception e) { return false; }` de `isSupported` | 118 | seul `isSupportedICU.invoke` peut lever une exception autre que `IllegalArgumentException` → même raison. |
| Vérification redondante | 157 | `if (charset != null) return charset;` puis, si aucune regex ne correspond, `charset` n'est pas modifié et la ligne 178 renvoie la même valeur : mutant **équivalent** (voir §10). |
| Second `if (charset != null)` | 178 | les noms obtenus par les regex (`iso-8859-N`, `cpNNNN`, `windows-NNNN`) sont tous aussi résolus par `Charset.forName` du JDK, donc sauter ce `return` donne le même résultat : équivalent dans l'environnement JDK. |


## 10. Exécution dans GitHub Actions

Le workflow [`tache2.yml`](.github/workflows/tache2.yml) se déclenche à chaque push sur `main` (et manuellement). Deux jobs :

**`tache2` (ubuntu-latest, JDK 17)**

1. `mvn -pl tika-core -am install -DskipTests` – compilation de `tika-parent` et `tika-core`.
2. **Tests originaux** : toute la suite de `tika-core` (nouveaux tests exclus) + rapport JaCoCo → `jacoco-original/`.
3. **Nouveaux tests** : `ProcessUtils*Test`, `CharsetUtils_*Test`, `CharsetUtilsManualTest` (11 fichiers générés + 4 manuels, **131 tests, 0 échec**, 15 ignorés sous Linux et 30 sous Windows pour cause d'OS ou de `@Disabled`) – l'étape échoue si l'un d'eux échoue (critère « exécution ») → `surefire-new/`, `jacoco-new/`.
4. **PIT scénario A** (tests originaux) → `pit-original/`.
5. **PIT scénario B** (originaux + générés) → `pit-generated/`.
6. **PIT scénario C₁** (originaux + manuels) → `pit-manual/`.
7. **PIT scénario C₂** (originaux + générés + manuels) → `pit-new/`.
8. Résumé Markdown dans l'onglet *Summary* de l'exécution, archivage en artefact, publication de tous les rapports sur la branche [`tache2-reports`](https://github.com/DavidStanescu13/tika/tree/tache2-reports).

**`tache2-windows` (windows-latest, JDK 17)** : mêmes nouveaux tests, puis PIT A, B, C₁ et C₂, rapports sur la branche [`tache2-reports-windows`](https://github.com/DavidStanescu13/tika/tree/tache2-reports-windows). Ce job existe parce que `ProcessUtils` a des branches propres à Windows.

Dernière exécution réussie (les deux jobs verts) : [#24](https://github.com/DavidStanescu13/tika/actions/runs/38435790822) ; historique : https://github.com/DavidStanescu13/tika/actions/workflows/tache2.yml.


## 11. Observations sur le code de Tika

Points relevés en écrivant les oracles (pas des bugs bloquants, mais des incohérences qu'un test met en évidence) :

1. **Asymétrie entre les deux surcharges de `execute` sur timeout.** `execute(pb, long, int, int)` appelle `destroyForcibly()` puis `waitFor(500 ms)` et relit `exitValue()` ; `execute(pb, long, Path, int)` appelle `destroyForcibly()` mais ne relit jamais le code de sortie, qui reste `-1`. Un appelant ne peut donc pas distinguer « tué par timeout » de « n'a pas pu être lu » dans la seconde surcharge, autrement que par `isTimeout()`.
2. **`stdoutLength` n'a pas la même sémantique dans les deux surcharges** : somme des longueurs de lignes *sans* retours à la ligne (via `StreamGobbler`) dans l'une, taille du fichier *avec* retours à la ligne (`Files.size`) dans l'autre.
3. **`unescapeCommandLine(null)` lève `NullPointerException`** alors que `escapeCommandLine(null)` renvoie `null`.
4. **Vérification redondante dans `CharsetUtils.forName`** : le `if (charset != null) return charset;` de la ligne 157 est entièrement rattrapé par celui de la ligne 178 (la variable n'est pas réassignée si aucune regex ne correspond). Conséquence : PIT ne peut pas tuer le mutant de la ligne 157 – c'est un mutant *équivalent* révélateur d'un code mort.
5. **`catch (Exception e)` de `isSupported` et chemin ICU4J** : injoignables sans `com.ibm.icu` sur le classpath de `tika-core` ; la couverture à 100 % de `CharsetUtils` n'est pas atteignable dans ce module seul.

## 12. Déclaration d'usage de l'intelligence artificielle

Conformément aux consignes du cours (usage de l'IA autorisé s'il est documenté), nous déclarons l'usage suivant :

- **Outil :** ChatUniTest (plugin Maven `chatunitest-maven-plugin` 2.1.1, Zhejiang University) pilotant le modèle de langage ouvert **`qwen2.5-coder:7b`** exécuté **localement** avec Ollama sur nos deux machines (aucune donnée envoyée à un service externe). Le modèle est appelé sous l'alias `codeqwen:v1.5-chat` (§3).
- **Usage :** génération automatique de tests unitaires pour `ProcessUtils` et `CharsetUtils` (§4) ; 9 fichiers (macOS) + 15 fichiers (Windows) retenus par le plugin, dont 11 intégrés au dépôt.
- **Ce que nous avons fait nous-mêmes :** choix des classes, installation et configuration (§3), diagnostic des échecs, correction ou désactivation des tests générés (§4), critique de leurs oracles (§5), analyse des mutants (§7, §9), écriture de tous les tests manuels (§8) et du pipeline (§10). Un assistant IA (Claude) a été utilisé par David comme outil d'aide à la rédaction et à l'automatisation du pipeline ; les analyses et choix de test ont été vérifiés par le binôme.

## 13. Reproduire les résultats

```bash
# compiler tika-parent + tika-core (Java 17+ ; mvn ou ./mvnw)
mvn -pl tika-core -am install -DskipTests -Drat.skip=true

# nouveaux tests (originaux + générés + manuels des deux classes)
mvn -pl tika-core -Drat.skip=true test -Dtest='ProcessUtils*Test,CharsetUtils*Test'

ORIG=org.apache.tika.detect.FileCommandDetectorTest,org.apache.tika.utils.CharsetUtilsTest
GEN=org.apache.tika.utils.ProcessUtils_*Test,org.apache.tika.utils.CharsetUtils_*Test
MAN=org.apache.tika.utils.ProcessUtils*ManualTest,org.apache.tika.utils.CharsetUtilsManualTest
PIT="mvn -pl tika-core -Drat.skip=true -Ppitest test-compile org.pitest:pitest-maven:mutationCoverage"
$PIT -DtargetTests="$ORIG"           -DreportsDirectory=target/pit-original    # A
$PIT -DtargetTests="$ORIG,$GEN"      -DreportsDirectory=target/pit-generated   # B
$PIT -DtargetTests="$ORIG,$MAN"      -DreportsDirectory=target/pit-manual      # C1
$PIT -DtargetTests="$ORIG,$GEN,$MAN" -DreportsDirectory=target/pit-new         # C2

# génération ChatUniTest (Ollama doit tourner ; voir §3)
bash tache2/run-chatunitest.sh
```

Les rapports sont dans `tika-core/target/pit-*/index.html`. La génération ChatUniTest n'est pas relancée par la GitHub Action (le modèle local n'est pas disponible sur un exécuteur GitHub) ; ses résultats sont versionnés dans `tika-core/chatunitest/` et `tika-core/chatunitest-windows/`.

---

## 14. Annexe : les 61 mutants de ProcessUtils et les 25 de CharsetUtils

État dans le scénario C₂ (originaux + générés + manuels) de l'exécution [#24](https://github.com/DavidStanescu13/tika/actions/runs/38435790822) sous Linux, et sous Windows lorsque l'état diffère. « Test qui le tue » : premier test qui détecte le mutant selon PIT (plusieurs tests peuvent tuer le même mutant). Numéros de ligne de PIT dans [`ProcessUtils.java`](tika-core/src/main/java/org/apache/tika/utils/ProcessUtils.java) et [`CharsetUtils.java`](tika-core/src/main/java/org/apache/tika/utils/CharsetUtils.java).


### ProcessUtils

| Ligne | Méthode | Mutation (PIT) | État Linux | Test qui le tue (Linux) | État Windows (si différent) |
|---|---|---|---|---|---|
| 46 | `register` | retour -> "" | tué | `ProcessUtilsMockedManualTest#executeDestroysProcessAndForgetsItWhenInterrupted` |  |
| 50 | `release` | retour -> null | survivant |  |  |
| 62 | `escapeCommandLine` | condition -> false | tué | `ProcessUtils_escapeCommandLine_2_0_Test#testEscapeCommandLine_NullInput` |  |
| 63 | `escapeCommandLine` | retour -> "" | tué | `ProcessUtilsPortableManualTest#testEscapeCommandLineNullAndNoSpace` |  |
| 67 | `escapeCommandLine` | condition -> false | survivant |  | tué – `ProcessUtils_escapeCommandLine_2_1_Test#testEscapeCommandLine_WithSpaces_Windows` |
| 67 | `escapeCommandLine` | condition -> false | survivant |  | tué – `ProcessUtils_escapeCommandLine_2_1_Test#testEscapeCommandLine_WithSpaces_Windows` |
| 68 | `escapeCommandLine` | condition -> false | sans couverture |  | tué – `ProcessUtils_escapeCommandLine_2_1_Test#testEscapeCommandLine_WithSpaces_Windows` |
| 68 | `escapeCommandLine` | condition -> false | sans couverture |  | tué – `ProcessUtils_escapeCommandLine_2_0_Test#testEscapeCommandLine_WithSpaces_Windows` |
| 71 | `escapeCommandLine` | retour -> "" | tué | `ProcessUtils_escapeCommandLine_2_3_Test#testEscapeCommandLine_WithQuotes_NoSpaces_Windows` |  |
| 75 | `unescapeCommandLine` | condition -> false | survivant |  | tué – `ProcessUtils_unescapeCommandLine_3_2_Test#testUnescapeCommandLine` |
| 75 | `unescapeCommandLine` | condition -> false | survivant |  | tué – `ProcessUtils_unescapeCommandLine_3_2_Test#testUnescapeCommandLine` |
| 76 | `unescapeCommandLine` | condition -> false | sans couverture |  | tué – `ProcessUtils_unescapeCommandLine_3_2_Test#testUnescapeCommandLine` |
| 76 | `unescapeCommandLine` | condition -> false | sans couverture |  | tué – `ProcessUtils_unescapeCommandLine_3_2_Test#testUnescapeCommandLine` |
| 77 | `unescapeCommandLine` | - remplace par + | sans couverture |  | tué – `ProcessUtils_unescapeCommandLine_3_2_Test#testUnescapeCommandLine` |
| 79 | `unescapeCommandLine` | retour -> "" | tué | `ProcessUtils_unescapeCommandLine_3_1_Test#testUnescapeCommandLine_QuotesNonWindows` |  |
| 107 | `execute` | appel supprime | tué | `ProcessUtilsManualTest#executeTruncatesStdoutAndStderrAtTheirOwnLimits` |  |
| 110 | `execute` | appel supprime | tué | `ProcessUtilsManualTest#executeTruncatesStdoutAndStderrAtTheirOwnLimits` |  |
| 115 | `execute` | - remplace par + | tué | `ProcessUtilsManualTest#executeCapturesStdoutStderrAndExitValue` |  |
| 116 | `execute` | condition -> false | tué | `ProcessUtilsMockedManualTest#executeWaitsForSlowStdoutReaderOfAFinishedProcess` |  |
| 118 | `execute` | appel supprime | tué | `ProcessUtilsMockedManualTest#executeWaitsForSlowStdoutReaderOfAFinishedProcess` |  |
| 119 | `execute` | appel supprime | tué | `ProcessUtilsManualTest#executeTruncatesStdoutAndStderrAtTheirOwnLimits` |  |
| 122 | `execute` | appel supprime | tué | `ProcessUtilsMockedManualTest#executeWaitsForSlowStdoutReaderAfterKillingProcessOnTimeout` |  |
| 123 | `execute` | appel supprime | tué | `ProcessUtilsMockedManualTest#executeWaitsForSlowStderrReaderAfterKillingProcessOnTimeout` |  |
| 125 | `execute` | condition -> false | tué | `ProcessUtilsMockedManualTest#executeWaitsForSlowStdoutReaderAfterKillingProcessOnTimeout` |  |
| 136 | `execute` | appel supprime | tué | `ProcessUtilsMockedManualTest#executeInterruptsStdoutReaderThatIsStillBlocked` |  |
| 137 | `execute` | appel supprime | tué | `ProcessUtilsMockedManualTest#executeInterruptsStderrReaderThatIsStillBlocked` |  |
| 143 | `execute` | condition -> false | tué | `ProcessUtilsManualTest#executeReportsInterruption` |  |
| 149 | `execute` | retour -> null | tué | `ProcessUtilsManualTest#executeReportsInterruption` |  |
| 151 | `execute` | condition -> false | tué | `ProcessUtilsMockedManualTest#executeDestroysProcessAndForgetsItWhenInterrupted` |  |
| 154 | `execute` | condition -> false | tué | `ProcessUtilsMockedManualTest#executeDestroysProcessAndForgetsItWhenInterrupted` |  |
| 174 | `execute` | condition -> false | tué | `ProcessUtilsManualTest#executeRedirectWritesStdoutToFileAndCreatesMissingDirectories` |  |
| 189 | `execute` | appel supprime | tué | `ProcessUtilsManualTest#executeRedirectTruncatesStderrOnly` |  |
| 194 | `execute` | - remplace par + | tué | `ProcessUtilsManualTest#executeRedirectWritesStdoutToFileAndCreatesMissingDirectories` |  |
| 195 | `execute` | condition -> false | tué | `ProcessUtilsManualTest#executeRedirectWithExistingDirectoryAndNonZeroExit` |  |
| 197 | `execute` | appel supprime | tué | `ProcessUtilsManualTest#executeRedirectWritesStdoutToFileAndCreatesMissingDirectories` |  |
| 200 | `execute` | appel supprime | tué | `ProcessUtilsMockedManualTest#executeRedirectWaitsForStderrReaderAfterKillingProcessOnTimeout` |  |
| 209 | `execute` | condition -> false | tué | `ProcessUtilsManualTest#executeRedirectReportsInterruption` |  |
| 215 | `execute` | retour -> null | tué | `ProcessUtilsManualTest#executeRedirectReportsInterruption` |  |
| 217 | `execute` | condition -> false | tué | `ProcessUtilsMockedManualTest#executeRedirectDestroysProcessAndForgetsItWhenInterrupted` |  |
| 220 | `execute` | condition -> false | tué | `ProcessUtilsMockedManualTest#executeRedirectDoesNotLeakProcessesInTheInternalMap` |  |
| 237 | `checkCommand` | retour -> false | tué | `ProcessUtilsManualTest#checkCommandReturnsTrueWhenCommandSucceeds` |  |
| 237 | `checkCommand` | retour -> true | tué | `ProcessUtilsManualTest#checkCommandReturnsFalseWhenCommandCannotBeStarted` |  |
| 250 | `checkCommand` | condition -> false | tué | `ProcessUtilsManualTest#checkCommandOnlyTreats127AsErrorByDefault` |  |
| 261 | `checkCommand` | appel supprime | tué (timeout) |  |  |
| 262 | `checkCommand` | appel supprime | tué (timeout) |  |  |
| 264 | `checkCommand` | condition -> false | tué | `ProcessUtilsMockedManualTest#checkCommandReturnsFalseAndKillsProcessWhenItNeverFinishes` |  |
| 267 | `checkCommand` | appel supprime | tué | `ProcessUtilsMockedManualTest#checkCommandWaitsForStdoutReaderToFinish` |  |
| 268 | `checkCommand` | appel supprime | tué | `ProcessUtilsMockedManualTest#checkCommandWaitsForStderrReaderToFinish` |  |
| 272 | `checkCommand` | condition -> false | tué | `ProcessUtilsManualTest#checkCommandOnlyTreats127AsErrorByDefault` |  |
| 273 | `checkCommand` | retour -> false | survivant |  |  |
| 273 | `checkCommand` | retour -> true | tué | `ProcessUtilsManualTest#checkCommandOnlyTreats127AsErrorByDefault` |  |
| 276 | `checkCommand` | retour -> false | tué | `ProcessUtilsManualTest#checkCommandReturnsTrueWhenCommandSucceeds` |  |
| 276 | `checkCommand` | retour -> true | survivant |  |  |
| 279 | `checkCommand` | retour -> false | survivant |  |  |
| 279 | `checkCommand` | retour -> true | tué | `ProcessUtilsManualTest#checkCommandReturnsFalseWhenCommandCannotBeStarted` |  |
| 283 | `checkCommand` | condition -> false | tué | `ProcessUtilsMockedManualTest#checkCommandReturnsFalseOnPosixSpawnError` |  |
| 283 | `checkCommand` | condition -> false | tué | `ProcessUtilsMockedManualTest#checkCommandRethrowsUnrelatedErrors` |  |
| 284 | `checkCommand` | condition -> false | tué | `ProcessUtilsMockedManualTest#checkCommandReturnsFalseOnUnixProcessError` |  |
| 286 | `checkCommand` | retour -> false | survivant |  |  |
| 286 | `checkCommand` | retour -> true | tué | `ProcessUtilsMockedManualTest#checkCommandReturnsFalseOnPosixSpawnError` |  |
| 290 | `checkCommand` | condition -> false | tué | `ProcessUtilsMockedManualTest#checkCommandReturnsFalseWhenInterrupted` |  |

### CharsetUtils

| Ligne | Méthode | Mutation (PIT) | État Linux | Test qui le tue (Linux) | État Windows (si différent) |
|---|---|---|---|---|---|
| 107 | `isSupported` | condition -> false | survivant |  |  |
| 107 | `isSupported` | condition -> false | sans couverture |  |  |
| 108 | `isSupported` | retour -> false | sans couverture |  |  |
| 110 | `isSupported` | retour -> false | tué | `CharsetUtilsTest#testValidCharset` |  |
| 110 | `isSupported` | retour -> true | tué | `CharsetUtilsTest#testValidCharset` |  |
| 112 | `isSupported` | retour -> true | tué | `CharsetUtilsTest#testInvalidCharset` |  |
| 115 | `isSupported` | retour -> true | tué | `CharsetUtilsTest#testInvalidCharset` |  |
| 118 | `isSupported` | retour -> true | sans couverture |  |  |
| 131 | `clean` | retour -> "" | tué | `CharsetUtilsTest#testCleaningCharsetName` |  |
| 133 | `clean` | retour -> "" | tué | `CharsetUtilsTest#testCleaningCharsetName` |  |
| 144 | `forName` | condition -> false | tué | `CharsetUtilsTest#testCleaningCharsetName` |  |
| 150 | `forName` | condition -> false | tué | `CharsetUtilsTest#testCleaningCharsetName` |  |
| 157 | `forName` | condition -> false | survivant |  |  |
| 158 | `forName` | retour -> null | tué | `CharsetUtilsTest#testCleaningCharsetName` |  |
| 159 | `forName` | condition -> false | tué | `CharsetUtilsTest#testCleaningCharsetName` |  |
| 159 | `forName` | condition -> false | tué | `CharsetUtilsManualTest#testForNameNoneAndNoAreRejected` |  |
| 165 | `forName` | condition -> false | tué | `CharsetUtilsTest#testFunkyNames` |  |
| 169 | `forName` | condition -> false | tué | `CharsetUtilsTest#testFunkyNames` |  |
| 173 | `forName` | condition -> false | tué | `CharsetUtilsTest#testFunkyNames` |  |
| 178 | `forName` | condition -> false | survivant |  |  |
| 179 | `forName` | retour -> null | tué | `CharsetUtilsTest#testFunkyNames` |  |
| 183 | `forName` | condition -> false | survivant |  |  |
| 186 | `forName` | condition -> false | sans couverture |  |  |
| 187 | `forName` | retour -> null | sans couverture |  |  |
| 195 | `forName` | retour -> null | tué | `CharsetUtilsTest#testCleaningCharsetName` |  |

