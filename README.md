# IFT3913 – Tâche 2 : augmentation d'une suite de tests (Apache Tika)

| Nom complet | Identifiant GitHub |
|---|---|
| David STANESCU | DavidStanescu13 |
| Aness Lakhdar AZZOUZ | ans1008 |

- Dépôt de la tâche : https://github.com/DavidStanescu13/tika (fork de [umontreal-diro/tika](https://github.com/umontreal-diro/tika))
- Ce README : https://github.com/DavidStanescu13/tika/blob/main/README.md
- Workflow GitHub Actions : https://github.com/DavidStanescu13/tika/actions/workflows/tache2.yml
- Rapports complets (JaCoCo HTML, PIT HTML/XML/CSV, Surefire) : branche [`tache2-reports`](https://github.com/DavidStanescu13/tika/tree/tache2-reports), régénérée à chaque exécution du workflow

> Le README original de Tika a été déplacé dans [`README-tika.md`](README-tika.md).

## Sommaire

1. [Organisation du dépôt](#1-organisation-du-dépôt)
2. [Classes choisies et justification](#2-classes-choisies-et-justification)
3. [ChatUniTest dans le pipeline Maven](#3-chatunitest-dans-le-pipeline-maven)
4. [Tests générés : résultats et critique](#4-tests-générés--résultats-et-critique)
5. [PIT : installation et scores de mutation](#5-pit--installation-et-scores-de-mutation)
6. [Mutants détectés par les tests générés](#6-mutants-détectés-par-les-tests-générés)
7. [Tests supplémentaires écrits à la main](#7-tests-supplémentaires-écrits-à-la-main)
8. [Mutants restants et pourquoi](#8-mutants-restants-et-pourquoi)
9. [Exécution dans GitHub Actions](#9-exécution-dans-github-actions)
10. [Observations sur le code de Tika](#10-observations-sur-le-code-de-tika)

---

## 1. Organisation du dépôt

Tout le travail porte sur le module **`tika-core`** (un des modules autorisés). Fichiers ajoutés ou modifiés par rapport au dépôt amont :

| Fichier | Rôle |
|---|---|
| [`tika-core/pom.xml`](tika-core/pom.xml) | ajout de deux profils Maven : `pitest` (plugin `pitest-maven` + `pitest-junit5-plugin`) et `chatunitest` (plugin `chatunitest-maven-plugin` configuré pour Ollama) |
| [`tika-core/src/test/java/org/apache/tika/utils/ProcessUtilsChatUniTest.java`](tika-core/src/test/java/org/apache/tika/utils/) | tests **générés** par ChatUniTest pour `ProcessUtils` _(voir §4)_ |
| [`tika-core/src/test/java/org/apache/tika/utils/CharsetUtilsChatUniTest.java`](tika-core/src/test/java/org/apache/tika/utils/) | tests **générés** par ChatUniTest pour `CharsetUtils` _(voir §4)_ |
| [`tika-core/src/test/java/org/apache/tika/utils/ProcessUtilsManualTest.java`](tika-core/src/test/java/org/apache/tika/utils/ProcessUtilsManualTest.java) | tests **écrits à la main** pour `ProcessUtils` (§7) |
| [`tika-core/src/test/java/org/apache/tika/utils/CharsetUtilsManualTest.java`](tika-core/src/test/java/org/apache/tika/utils/CharsetUtilsManualTest.java) | tests **écrits à la main** pour `CharsetUtils` (§7) |
| [`tache2/run-chatunitest.sh`](tache2/run-chatunitest.sh) | script qui lance ChatUniTest + Ollama et copie la suite fusionnée dans le dossier de test |
| [`tache2/chatunitest-*.log`](tache2/) | journaux bruts de la génération |
| [`.github/workflows/tache2.yml`](.github/workflows/tache2.yml) | workflow GitHub Actions : tests originaux + JaCoCo, nouveaux tests, PIT avant/après, publication des rapports |
| [`.github/scripts/tache2_summary.py`](.github/scripts/tache2_summary.py) | produit le résumé Markdown affiché dans l'onglet *Summary* de chaque exécution |
| `.github/workflows/main-*.yml`, `docker-snapshot.yml`, `split-packages.yml` | workflows amont de Tika passés en déclenchement manuel (`workflow_dispatch`) pour ne pas recompiler tout Tika à chaque push |

Commandes locales (Java 17+, Maven 3.9+) :

```bash
# compiler tika-parent + tika-core
mvn -pl tika-core -am install -DskipTests -Drat.skip=true

# tous les tests de tika-core + rapport JaCoCo (tika-core/target/site/jacoco/index.html)
mvn -pl tika-core test -Drat.skip=true

# PIT avec les tests originaux seulement (tika-core/target/pit-reports/index.html)
mvn -pl tika-core -Ppitest test-compile org.pitest:pitest-maven:mutationCoverage -Drat.skip=true

# PIT avec tests originaux + générés + manuels
mvn -pl tika-core -Ppitest test-compile org.pitest:pitest-maven:mutationCoverage -Drat.skip=true \
  -DtargetTests='org.apache.tika.detect.FileCommandDetectorTest,org.apache.tika.utils.CharsetUtilsTest,org.apache.tika.utils.ProcessUtils*Test,org.apache.tika.utils.CharsetUtils*Test'

# génération ChatUniTest (Ollama doit tourner)
bash tache2/run-chatunitest.sh
```

## 2. Classes choisies et justification

Nous avons retenu deux classes du paquet `org.apache.tika.utils` de `tika-core` :

| Classe | Rôle | Tests existants | Pourquoi elle convient |
|---|---|---|---|
| **`ProcessUtils`** (296 lignes) | lance des processus externes avec timeout, capture/redirige stdout et stderr, vérifie qu'une commande existe (`checkCommand`), échappe les arguments pour Windows | aucun test dédié ; exercée **indirectement** par `FileCommandDetectorTest` (via `FileCommandDetector`, qui appelle `checkCommand` et `execute(pb, timeout, int, int)`) | couverture partielle, beaucoup de branches (timeouts, troncature, codes d'erreur) jamais exécutées, 58 mutants vivants sur 61 |
| **`CharsetUtils`** (197 lignes) | normalise des noms de jeux de caractères (`"utf-8>"`, `cp-1251`, `win1251`, `8859-15`…), les résout via une table, des regex, ICU4J si présent, puis le JDK | `CharsetUtilsTest` (4 tests, 29 assertions), qui ne passe que par `clean()` et `isSupported()` | couverture élevée mais incomplète (branches ICU, exceptions de `forName`), 10 mutants vivants sur 25 ; classe « pure », idéale pour comparer les oracles d'un LLM à ceux d'un humain |

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

[ChatUniTest](https://github.com/ZJU-ACES-ISE/ChatUniTest) est intégré comme **plugin Maven** dans `tika-core/pom.xml`, dans le profil `chatunitest` :

```xml
<profile>
  <id>chatunitest</id>
  <properties>
    <chatunitest.url>http://localhost:11434/v1/chat/completions</chatunitest.url>
    <chatunitest.model>qwen2.5-coder:7b</chatunitest.model>
  </properties>
  <build><plugins>
    <plugin>
      <groupId>io.github.zju-aces-ise</groupId>
      <artifactId>chatunitest-maven-plugin</artifactId>
      <version>2.1.1</version>
      <configuration>
        <apiKeys>ollama</apiKeys>          <!-- clé factice : Ollama n'en exige pas, le plugin oui -->
        <model>${chatunitest.model}</model>
        <url>${chatunitest.url}</url>      <!-- point de terminaison OpenAI-compatible d'Ollama -->
        <testNumber>5</testNumber>
        <maxRounds>5</maxRounds>
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

Le modèle de langage est **ouvert et exécuté localement** avec [Ollama](https://ollama.com) (`ollama pull qwen2.5-coder:7b`), qui expose une API compatible OpenAI sur `localhost:11434` ; ChatUniTest est pointé dessus via `url`. Commande de génération pour une classe :

```bash
mvn -f tika-core/pom.xml -Pchatunitest -Drat.skip=true \
    io.github.zju-aces-ise:chatunitest-maven-plugin:2.1.1:class \
    -DselectClass=org.apache.tika.utils.ProcessUtils
```

Le script [`tache2/run-chatunitest.sh`](tache2/run-chatunitest.sh) enchaîne : vérification qu'Ollama répond, `mvn install` de `tika-core` (requis par le plugin pour résoudre les dépendances), génération pour `ProcessUtils` puis `CharsetUtils`, et copie de la suite fusionnée produite par le plugin (`tika-core/chatunitest/…`) vers `tika-core/src/test/java/org/apache/tika/utils/<Classe>ChatUniTest.java` afin qu'elle soit exécutée par Surefire et par PIT comme n'importe quel test du projet.

## 4. Tests générés : résultats et critique

### 4.0 Journal des exécutions et obstacles rencontrés

Trois obstacles ont dû être levés avant d'obtenir des tests ; aucun ne concerne le contenu des tests eux-mêmes, mais ils font partie du « coût d'intégration » de l'outil et sont donc documentés.

| # | Symptôme | Cause | Correctif (tous dans le dépôt) |
|---|---|---|---|
| 1 | `Invalid Java version format: 26` dès le lancement du plugin | ChatUniTest 2.1.1 analyse `java.version` avec un format `17.0.x` et ne reconnaît pas les JDK récents | le script `tache2/run-chatunitest.sh` détecte un JDK > 21 et bascule sur un JDK 17/21 installé (`JAVA_HOME`) |
| 2 | `No Model with name qwen2.5-coder:7b. Support models: gpt-3.5-turbo, …, code-llama, codeqwen:v1.5-chat` | le plugin n'accepte qu'une liste fermée de noms de modèles (pour choisir le *tokenizer* et la taille de contexte) | le modèle local `qwen2.5-coder:7b` est exposé par Ollama sous le nom `codeqwen:v1.5-chat` (`ollama cp qwen2.5-coder:7b codeqwen:v1.5-chat`) ; c'est bien qwen2.5-coder qui s'exécute |
| 3 | **Première campagne (12 oct., 1 h 05 de génération) : 0 test compilable sur 224 tentatives** (ProcessUtils 149, CharsetUtils 75) – journaux dans [`tache2/chatunitest-ProcessUtils.log`](tache2/chatunitest-ProcessUtils.log), [`tache2/chatunitest-CharsetUtils.log`](tache2/chatunitest-CharsetUtils.log) et les 224 fichiers `tika-core/target/chatunitest-info/…/error-message/*_CompilationError_*.txt` | analyse automatique des 224 rapports d'erreur : **100 % des erreurs** sont `package org.mockito does not exist`, `package org.mockito.junit.jupiter does not exist` ou des `cannot find symbol` sur des lignes `@Mock`, `@ExtendWith(MockitoExtension.class)`, `when(…)`. Le *prompt* de ChatUniTest impose Mockito au modèle, mais `tika-core` n'a pas Mockito dans ses dépendances de test ; les 5 rondes de « réparation » renvoient l'erreur au modèle, qui ne peut pas l'éliminer puisqu'elle ne dépend pas du code | ajout de `mockito-core` et `mockito-junit-jupiter` (versions gérées par `tika-parent`) en dépendances `test` de `tika-core/pom.xml` ; seconde campagne ci-dessous |

Autre point d'intégration : les tests générés sont copiés **tels quels** dans `src/test/java`, sans en-tête de licence Apache ni respect du style Tika (imports `*`). Plutôt que de les retoucher (ce qui serait une « intervention manuelle »), ils sont exclus de `checkstyle` et de `forbiddenapis` dans `tika-core/pom.xml` (motif `*_*_*_*_Test.java`). La « suite » `<Classe>_Suite.java` que ChatUniTest produit (`@RunWith(JUnitPlatform.class)`, format JUnit 4) n'est pas copiée : elle ne compile pas avec JUnit Jupiter 6.

### 4.1 Où sont les tests générés ?

- **Sortie brute de ChatUniTest** (non modifiée) : [`tika-core/chatunitest/tika-parent/tika-core/org/apache/tika/utils/`](tika-core/chatunitest/tika-parent/tika-core/org/apache/tika/utils) – 15 fichiers `<Classe>_<méthode>_<n°méthode>_<n°tentative>_Test.java` retenus par l'outil, plus deux fichiers `*_Suite.java` (format JUnit 4, non utilisables).
- **Traces complètes** : [`tika-core/target/chatunitest-info/tika-parent/tika-core/`](tika-core/target/chatunitest-info/tika-parent/tika-core) – pour chaque classe, chaque méthode et chaque tentative, le *prompt* envoyé au modèle, sa réponse, le code extrait et les erreurs de compilation/exécution (`history*/class*/method*/attempt*/records.json`, `error-message/`). Journaux Maven : [`tache2/chatunitest-ProcessUtils.log`](tache2/chatunitest-ProcessUtils.log) (40 min) et [`tache2/chatunitest-CharsetUtils.log`](tache2/chatunitest-CharsetUtils.log) (22 min).
- **Tests exécutés par le pipeline** : copies des 15 fichiers dans [`tika-core/src/test/java/org/apache/tika/utils/`](tika-core/src/test/java/org/apache/tika/utils) (`*_*_*_*_Test.java`). Ce sont ces copies qui portent les annotations `@Disabled` / `assumeTrue` décrites en 4.2.

Paramètres : modèle `qwen2.5-coder:7b` (Ollama, local, exposé sous le nom `codeqwen:v1.5-chat`), `testNumber=5` (5 tentatives par méthode), `maxRounds=5` (jusqu'à 5 rondes de réparation par tentative), `temperature=0.2`, `stopWhenSuccess=false`, `prune=false`, schéma `CHATUNITEST` (prompt par défaut de l'outil). Les méthodes privées `register`/`release` et `initCommonCharsets` sont ignorées par l'outil ; les deux surcharges de `execute` et de `checkCommand` sont traitées séparément (d'où 10 tentatives pour chacune).

### 4.2 Compilent-ils et s'exécutent-ils sans intervention manuelle ?

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

- **11 tests** d'`escapeCommandLine` conditionnés à la plateforme : une ligne `assumeTrue(SystemUtils.IS_OS_WINDOWS)` ou `assumeFalse(...)` ajoutée en tête de méthode. Les tests deviennent corrects : le modèle avait bien compris les deux comportements, il n'avait simplement pas le moyen (ni l'idée) de les conditionner.
- **40 tests** marqués `@Disabled("IFT3913 : <raison>")`, la raison étant inscrite dans l'annotation (oracle faux, Mockito mal employé, exception impossible). Ils restent dans le dépôt pour l'analyse mais ne sont ni exécutés ni utilisés par PIT.
- Au niveau du projet : ajout de Mockito (campagne 1) et exclusion des fichiers générés de `checkstyle`/`forbiddenapis` (en-tête de licence absent, imports `*`).

Bilan : **0 test généré n'était utilisable sans intervention** à la première campagne ; à la seconde, **37 / 88 (42 %)** passent tels quels sur les deux OS, **48 / 88 (55 %)** après les 11 conditions de plateforme, et il a fallu 51 interventions (une ligne chacune) pour obtenir une suite verte.

### 4.3 Comparaison qualitative des oracles (IA vs écrits à la main)

**Ce que le modèle fait bien.**

- Sur une fonction pure et bien documentée (`escapeCommandLine`), les cas sont pertinents et proches de ce qu'un humain écrirait : `null`, sans espace, avec espace, déjà entre guillemets, guillemet d'un seul côté. `ProcessUtils_escapeCommandLine_2_1_Test.testEscapeCommandLine_WithQuotesAndSpaces_Windows` teste même l'entrée `"test arg" with spaces` (guillemet au début mais pas à la fin), un cas que nous n'avions pas pensé à écrire – avec toutefois un oracle faux (il attend un second échappement que le code ne fait jamais).
- Les oracles sont **spécifiques** (`assertEquals` sur la valeur exacte, `assertNull`) plutôt que de simples `assertNotNull` ; on ne trouve pas de test « ça ne plante pas ».
- `CharsetUtils_clean_2_4_Test` couvre systématiquement le nettoyage HTTP (espaces, virgules, casse) et les mots réservés `none`/`no`, avec 11 oracles corrects sur 19.
- La couverture visée est la couverture de branches : pour `checkCommand`, le modèle a bien identifié les quatre issues (succès, code d'erreur, `TimeoutException`, `IOException`, `SecurityException`, `Error`) – c'est la mise en œuvre qui échoue, pas l'intention.

**Ce qui ne va pas.**

1. **Oracles déduits de la lecture du code plutôt que de son exécution.** Le modèle *simule* mentalement le code et se trompe dès que la sémantique est subtile : il attend `CP1252` et `WINDOWS-1252` là où `Charset.name()` renvoie le nom canonique `windows-1252` ; il attend que `"iso 8859-1"` soit reconnu alors que la regex coupe au premier espace ; il attend qu'une virgule en tête soit ignorée alors que seule l'ancienne regex `[ "]*` l'autorise pour espaces et guillemets. Un humain qui doute exécute le code (c'est ce que nous avons fait pour `8859-99` ou `cp-9999`, cf. §7) ; le modèle écrit la valeur qu'il *croit* correcte, et `prune=false` laisse passer l'erreur.
2. **Oracles tautologiques ou absurdes sur le comportement temporel.** Les 12 tests d'`execute` affirment `assertEquals(1000, result.processTimeMillis)` – c'est-à-dire que la durée mesurée serait *exactement* égale au timeout, même quand le processus se termine immédiatement. Nos tests manuels n'encadrent la durée que par des bornes (`>= 400`, `< 10000`).
3. **Mocks hors de propos.** Le *prompt* par défaut de ChatUniTest pousse vers Mockito ; le modèle crée donc des `@Mock Process`, `@Mock StreamGobbler`, `@InjectMocks ProcessUtils` alors que **toutes les méthodes sont statiques** et instancient elles-mêmes leurs collaborateurs : les mocks ne sont jamais utilisés, `verify(mockProcess).waitFor(...)` échoue forcément, et `when(systemUtils.IS_OS_WINDOWS)` tente de stubber un champ `static final`. Le modèle ne tire aucune leçon des 5 rondes de réparation : il reformule le même mock. Nos tests manuels choisissent l'approche inverse – un vrai sous-processus contrôlé (`Helper`) – précisément parce que la classe ne s'y prête pas au *mocking*.
4. **Contradictions internes.** Dans chaque fichier `escapeCommandLine_*`, un test « Windows » et un test « non-Windows » coexistent sans condition : la suite ne peut passer sur aucune machine. Le modèle « connaît » la dépendance à l'OS (il nomme les tests ainsi) mais ne sait pas l'exprimer avec `assumeTrue`.
5. **Vérifications triviales.** `CharsetUtils_clean_2_4_Test.testCleanWithWinNamePatternAndWhitespace` est une méthode **vide** (passe toujours) ; `ProcessUtils_execute_4_4_Test` stubbe six méthodes de `StreamGobbler` dans `@BeforeEach` sans qu'aucune ne soit appelée ; plusieurs tests de `clean` sont des doublons à un espace près (`"UTF-8,"`, `"UTF-8,  "`, `"  UTF-8,  "`).
6. **Redondance sans valeur de mutation.** Les cinq fichiers `escapeCommandLine_2_{0..4}` sont quasi identiques (35 tests pour 4 branches) : ils ne tuent pas plus de mutants qu'un seul (voir §6).
7. **Pas de connaissance du contexte d'exécution.** `checkCommand_6_1` lance réellement `echo test` : cela fonctionne sous Linux mais pas sous Windows (pas d'exécutable `echo`), et `invalid_command` est attendu comme levant une `IOException` alors que la méthode l'attrape (`testCheckCommandWithInvalidCommand`).

**En résumé**, les tests générés sont *pertinents dans leurs intentions* (choix des entrées, branches visées) mais leurs *oracles ne sont fiables que pour les fonctions pures et simples* ; dès qu'il y a un effet de bord (processus, temps, OS) ou une bibliothèque mal adaptée (Mockito sur des méthodes statiques), ils affirment des valeurs fausses avec la même assurance. Le temps de génération (1 h) est comparable au temps d'écriture manuelle des 28 tests de la §7, mais il faut y ajouter le temps de tri : 51 tests sur 88 ont dû être revus un par un.

## 5. PIT : installation et scores de mutation

[PIT](https://pitest.org) est ajouté dans `tika-core/pom.xml` (profil `pitest`) : `pitest-maven` 1.25.9 avec `pitest-junit5-plugin` 1.2.3 (nécessaire car Tika utilise JUnit Jupiter 6). Configuration : mutateurs `DEFAULTS`, cibles `org.apache.tika.utils.ProcessUtils` et `org.apache.tika.utils.CharsetUtils`, rapports HTML + XML + CSV, `timeoutConstant` porté à 20 s parce que `ProcessUtils` lance de vrais processus. Les tests à utiliser sont passés par `-DtargetTests=…`, ce qui permet de comparer deux scénarios avec la même configuration.

### Scores

| Scénario | Tests utilisés | ProcessUtils (Linux) | ProcessUtils (Windows) | CharsetUtils |
|---|---|---|---|---|
| **A – tests originaux** | `FileCommandDetectorTest`, `CharsetUtilsTest` | **4,9 %** (3/61) | 4,9 % (3/61) | **60,0 %** (15/25) |
| **B – originaux + générés (ChatUniTest)** | A + `*_*_*_*_Test` (48 tests actifs) | **8,2 %** (5/61) | 8,2 % (5/61) | **60,0 %** (15/25) |
| **C₁ – originaux + manuels** (contrôle) | A + `*ManualTest` | **47,5 %** (29/61 : 27 tués + 2 timeouts) | 60,7 % (37/61, exécution #6) | **64,0 %** (16/25) |
| **C₂ – originaux + générés + manuels** | B + `*ManualTest` | 65,6 % (40/61) | 80,3 % (49/61) | **64,0 %** (16/25) |

Source : exécution [#20](https://github.com/DavidStanescu13/tika/actions/runs/38395274095) du workflow (branches `tache2-reports` et `tache2-reports-windows`). Les scénarios A, B, C₁ et C₂ sont recalculés à chaque exécution.

**Pourquoi deux lignes C.** Le score C₂ (toute la suite) est supérieur de 11 mutants à C₁ (sans les tests générés) alors que, d'après le scénario B, les tests générés ne tuent que 2 mutants de plus que les tests originaux. Les 11 mutants supplémentaires de C₂ sont tous attribués par PIT à des tests **manuels** (`testProcessIsUnregisteredAfterExecute`, `testExecuteRedirectStderrTruncation`, `testExecuteRedirectTimeout`) qui, seuls, ne les tuent pas (C₁, reproduit sur les exécutions #4, #6 et #20), et ce sont précisément les mutants que nous avions classés *équivalents* ou *de course* en §8 (suppression de `join`/`interrupt`, `if (p != null)` dans un `finally`, valeur de retour de `release` inutilisée). Un test ne peut pas distinguer un mutant équivalent : ces « kills » sont donc des artefacts d'interaction entre classes de test dans la JVM de PIT (très probablement un effet de charge/ordonnancement sur les conditions de course de `ProcessUtils`, les tests générés chargeant Mockito et son agent d'instrumentation), pas des détections. **Nous retenons C₁ comme score défendable** (47,5 % Linux / 60,7 % Windows pour `ProcessUtils`, 64 % pour `CharsetUtils`) et nous signalons C₂ par honnêteté, puisque c'est le chiffre que le rapport PIT complet affiche.

Couverture JaCoCo obtenue avec les **nouveaux tests seuls** (générés actifs + manuels, exécution #20) : ProcessUtils 87,0 % des instructions / 59,3 % des branches / 100 % des méthodes (contre 50,7 / 22,2 / 63,6 avec toute la suite originale de `tika-core`) ; CharsetUtils 83,4 % / 78,1 % / 100 %.

Les scénarios A et C sont calculés automatiquement à chaque exécution du workflow (étapes « PIT - tests originaux » et « PIT - tests originaux + generes + manuels »). Le scénario B a été mesuré en lançant PIT avec `-DtargetTests='…,org.apache.tika.utils.ProcessUtilsChatUniTest,org.apache.tika.utils.CharsetUtilsChatUniTest'`.

## 6. Mutants détectés par les tests générés

Scénario B (tests originaux + les 48 tests générés actifs, c'est-à-dire non `@Disabled`) : PIT passe de 3 à **5 mutants tués sur 61** pour `ProcessUtils` et reste à **15 / 25** pour `CharsetUtils`. Les tests générés **ne détectent donc pas tous les mutants** – loin de là. Détail :

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

Comparaison avec les tests manuels (C₁) : 28 à 37 mutants tués selon l'OS, parce qu'ils (a) exercent les deux surcharges d'`execute` et `checkCommand` avec un vrai sous-processus contrôlé, (b) bornent `processTimeMillis`, (c) vérifient le registre interne par réflexion, et (d) choisissent des entrées là où le code *diffère* de l'intuition (`8859-99`, `cp-9999`, `"NO"` vs `"nonexistent"`), ce que le modèle ne fait que par accident.

## 7. Tests supplémentaires écrits à la main

Les tests manuels sont dans `ProcessUtilsManualTest` (18 tests) et `CharsetUtilsManualTest` (10 tests). Principe retenu pour `ProcessUtils` : plutôt que de dépendre de `sh`, `echo` ou `sleep` (absents ou différents sous Windows), les tests lancent **une JVM** qui exécute la classe interne `ProcessUtilsManualTest.Helper` avec des arguments contrôlés (`emit <code> <nOut> <nErr>`, `sleep <ms>`, `flood <n>`). Le comportement du sous-processus est donc entièrement connu, ce qui permet des oracles exacts.

### 7.1 `ProcessUtilsManualTest`

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

### 7.2 `CharsetUtilsManualTest`

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

## 8. Mutants restants et pourquoi

### 8.1 Mutants tués par les tests manuels (scénario A → C₁)

`ProcessUtils` passe de 3 à 28–29 mutants détectés sous Linux (37 sous Windows), `CharsetUtils` de 15 à 16. Le 29ᵉ (ligne 116, `if (complete)`) n'est tué que dans certaines exécutions (#20 oui, #4 et #6 non) : c'est un mutant de course, voir 8.2. Détail (numéros de ligne de [`ProcessUtils.java`](tika-core/src/main/java/org/apache/tika/utils/ProcessUtils.java) et [`CharsetUtils.java`](tika-core/src/main/java/org/apache/tika/utils/CharsetUtils.java)) :

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

### 8.2 Mutants non détectés après les tests manuels (scénario C₁, Linux)

**ProcessUtils – 32 à 33 mutants sous Linux (22–23 survivants + 10 sans couverture), 24 sous Windows**

> Dans le scénario C₂ (avec les tests générés dans la même exécution PIT), 11 de ces mutants – 50, 118, 119, 122, 123, 136, 137, 151, 197, 200, 217 – apparaissent « tués » par des tests manuels qui ne les tuent pas lorsqu'ils s'exécutent sans les tests générés (C₁). Ce sont exactement les mutants équivalents ou de course ci-dessous ; nous ne les comptons pas comme détectés (voir §5).

| Catégorie | Lignes | Explication |
|---|---|---|
| Propres à Windows | 67 (×2), 68 (×2), 75 (×2), 76 (×2), 77 | `arg.contains(" ") && SystemUtils.IS_OS_WINDOWS && …` : sous Linux `IS_OS_WINDOWS` est faux, les sous-conditions suivantes ne sont jamais évaluées (`NO_COVERAGE`) et forcer les deux premières à faux ne change rien (équivalents **sur Linux**). **Tous les 9 sont tués sous Windows** par `testEscapeCommandLineOnWindows` (job `tache2-windows`, rapports sur la branche [`tache2-reports-windows`](https://github.com/DavidStanescu13/tika/tree/tache2-reports-windows)). |
| Branche `catch (Error err)` de `checkCommand` | 283 (×2), 284, 286 (×2) | n'est atteinte que si `Runtime.exec` lève une `Error` dont le message contient `posix_spawn` ou `UNIXProcess` (bug JDK historique TIKA-1526) ; impossible à provoquer avec un JDK moderne sans mocker `Runtime`. |
| Mutants équivalents par construction | 273 (`return false` → `false`), 276 (`return true` → `true`), 279 (`return false` → `false`) | PIT remplace une valeur de retour par la même valeur : aucun test ne peut les distinguer. |
| `if (p != null) p.destroyForcibly()` dans les blocs `finally` | 151, 217, 290 | quand on arrive au `finally` le processus est soit terminé soit déjà détruit ; supprimer ce second `destroyForcibly` n'a pas d'effet observable (équivalent). |
| `if (complete)` de la 1re surcharge | 116 | la branche `else` (prévue pour le timeout) fait `destroyForcibly` (sans effet sur un processus terminé), `waitFor(500)` puis relit `exitValue` : résultat identique *sauf* si le processus n'a pas encore été moissonné dans les 500 ms, auquel cas `exitValue` reste -1 → quasi équivalent, tué de façon intermittente (`FileCommandDetectorTest.testBasic` à l'exécution #20). Le même mutant dans la surcharge `Path` (ligne 195) **est** tué de façon stable, parce que là la branche `else` ne relit pas le code de sortie. |
| Valeur de retour de `release` | 50 | `release(id)` est appelé sans utiliser la valeur renvoyée → équivalent. |
| Suppression des `join(1000)` sur les lecteurs de flux | 118, 119, 122, 123, 197, 200, 267, 268 | mutants « de course » : sans `join`, le résultat est construit avant que le lecteur ait fini. Avec 2 Mo de sortie (`flood`), le processus se termine après avoir tout écrit et le lecteur ne met que quelques millisecondes à vider le reste du pipe, donc la course est presque toujours gagnée et un test qui échouerait seulement « parfois » serait un test instable. Pour `checkCommand` (267, 268) la sortie n'est de toute façon pas utilisée. Nous avons préféré ne pas écrire de test non déterministe. |
| Suppression des `interrupt()` dans le `finally` | 136, 137 | les lecteurs sont bloqués sur un flux dont l'autre extrémité est fermée (processus détruit) et se terminent d'eux-mêmes : l'interruption est une sécurité sans effet observable. |
| `if (!finished) throw new TimeoutException()` | 264 | n'est observable que si `waitFor(60 000 ms)` expire : il faudrait un test qui attend 60 s (le délai est codé en dur), ce qui dépasse le `timeoutConstant` de PIT et rendrait la suite inutilisable. |

**CharsetUtils – 9 mutants (4 survivants + 5 sans couverture)**

| Catégorie | Lignes | Explication |
|---|---|---|
| Chemin ICU4J | 107 (2e condition), 108, 183, 186, 187 | `com.ibm.icu.charset.CharsetICU` n'est pas sur le classpath de `tika-core` : `isSupportedICU` et `getCharsetICU` valent `null`. Forcer `if (isSupportedICU != null)` / `if (getCharsetICU != null)` à faux est équivalent, et le corps de ces `if` est injoignable. Les couvrir exigerait d'ajouter ICU4J en dépendance de test, ce qui changerait le comportement de **tous** les tests de la classe. |
| `catch (Exception e) { return false; }` de `isSupported` | 118 | seul `isSupportedICU.invoke` peut lever une exception autre que `IllegalArgumentException` → même raison. |
| Vérification redondante | 157 | `if (charset != null) return charset;` puis, si aucune regex ne correspond, `charset` n'est pas modifié et la ligne 178 renvoie la même valeur : mutant **équivalent** (voir §10). |
| Second `if (charset != null)` | 178 | les noms obtenus par les regex (`iso-8859-N`, `cpNNNN`, `windows-NNNN`) sont tous aussi résolus par `Charset.forName` du JDK, donc sauter ce `return` donne le même résultat : équivalent dans l'environnement JDK. |

## 9. Exécution dans GitHub Actions

Le workflow [`tache2.yml`](.github/workflows/tache2.yml) se déclenche à chaque push sur `main` (et manuellement). Étapes :

1. `mvn -pl tika-core -am install -DskipTests` – compilation de `tika-parent` et `tika-core`.
2. **Tests originaux** : toute la suite de `tika-core` (nouveaux tests exclus) + rapport JaCoCo → `jacoco-original/`.
3. **Nouveaux tests** : `ProcessUtils*Test`, `CharsetUtilsChatUniTest*`, `CharsetUtilsManualTest` – l'étape échoue si l'un d'eux échoue (critère « exécution ») → `surefire-new/`, `jacoco-new/`.
4. **PIT scénario A** (tests originaux) → `pit-original/`.
5. **PIT scénario C** (originaux + générés + manuels) → `pit-new/`.
6. Résumé Markdown dans l'onglet *Summary* de l'exécution, archivage en artefact, et publication de tous les rapports sur la branche `tache2-reports`.

Dernière exécution réussie : voir https://github.com/DavidStanescu13/tika/actions/workflows/tache2.yml.

## 10. Observations sur le code de Tika

Points relevés en écrivant les oracles (pas des bugs bloquants, mais des incohérences qu'un test met en évidence) :

1. **Asymétrie entre les deux surcharges de `execute` sur timeout.** `execute(pb, long, int, int)` appelle `destroyForcibly()` puis `waitFor(500 ms)` et relit `exitValue()` ; `execute(pb, long, Path, int)` appelle `destroyForcibly()` mais ne relit jamais le code de sortie, qui reste `-1`. Un appelant ne peut donc pas distinguer « tué par timeout » de « n'a pas pu être lu » dans la seconde surcharge, autrement que par `isTimeout()`.
2. **`stdoutLength` n'a pas la même sémantique dans les deux surcharges** : somme des longueurs de lignes *sans* retours à la ligne (via `StreamGobbler`) dans l'une, taille du fichier *avec* retours à la ligne (`Files.size`) dans l'autre.
3. **`unescapeCommandLine(null)` lève `NullPointerException`** alors que `escapeCommandLine(null)` renvoie `null`.
4. **Vérification redondante dans `CharsetUtils.forName`** : le `if (charset != null) return charset;` de la ligne 157 est entièrement rattrapé par celui de la ligne 178 (la variable n'est pas réassignée si aucune regex ne correspond). Conséquence : PIT ne peut pas tuer le mutant de la ligne 157 – c'est un mutant *équivalent* révélateur d'un code mort.
5. **`catch (Exception e)` de `isSupported` et chemin ICU4J** : injoignables sans `com.ibm.icu` sur le classpath de `tika-core` ; la couverture à 100 % de `CharsetUtils` n'est pas atteignable dans ce module seul.
