# IFT3913 - Tâche 2 : augmentation d'une suite de tests (ProcessUtils)

**Binôme :** AZZOUZ / STANESCU - cas d'étude : [Apache Tika](https://github.com/umontreal-diro/tika), module `tika-core`
**Classe testée :** `org.apache.tika.utils.ProcessUtils` ([source](tika-core/src/main/java/org/apache/tika/utils/ProcessUtils.java))

> Dépôt de la tâche : https://github.com/DavidStanescu13/tika
> Cette page est la documentation unique de la tâche (le README d'origine d'Apache Tika est conservé en bas de page).

## Résumé

| Étape | Tests | Mutants tués (PIT) | Score de mutation | Lignes couvertes (PIT) |
|---|---|---|---|---|
| Tests originaux de `tika-core` (suite complète) | 374 examinés | 3 / 61 | **5 %** | 70 / 146 (48 %) |
| + tests générés par ChatUniTest (après corrections) | + 40 | 9 / 61 | **15 %** | 113 / 146 (77 %) |
| + tests écrits à la main | + 42 | 47 / 61 | **77 %** | 140 / 146 (96 %) |

- ChatUniTest (modèle local `qwen2.5-coder:7b` via Ollama) a produit 9 fichiers de test retenus (40 tests) en 1 h 11. **11 de ces 40 tests échouent tels quels** sous Maven et 16 ont dû être modifiés (section 5).
- Les 14 mutants restants sont tous justifiés (section 7) : 4 mutants équivalents créés par PIT, 9 qui n'existent que sous Windows, 1 dont la valeur de retour n'est jamais lue.
- La suite complète de `tika-core` passe avec nos ajouts : **831 tests, 0 échec, 9 ignorés** (dont 7 des nôtres, réservés à un système d'exploitation précis).

## Table des matières

1. [Choix de la classe](#1-choix-de-la-classe)
2. [Installation de ChatUniTest dans le pipeline Maven](#2-installation-de-chatunitest-dans-le-pipeline-maven)
3. [Génération des tests](#3-génération-des-tests)
4. [Les tests générés : où sont-ils, compilent-ils, s'exécutent-ils ?](#4-les-tests-générés)
5. [Critique des tests générés et comparaison des oracles](#5-critique-des-tests-générés-et-comparaison-des-oracles)
6. [Analyse de mutation avec PIT](#6-analyse-de-mutation-avec-pit)
7. [Mutants détectés et non détectés](#7-mutants-détectés-et-non-détectés)
8. [Tests écrits à la main](#8-tests-écrits-à-la-main)
9. [Exécution dans GitHub Actions](#9-exécution-dans-github-actions)
10. [Déclaration d'usage de l'intelligence artificielle](#10-déclaration-dusage-de-lintelligence-artificielle)
11. [Reproduire les résultats](#11-reproduire-les-résultats)

---

## 1. Choix de la classe

`ProcessUtils` (module `tika-core`) lance des processus externes (`execute`, `checkCommand`) et adapte la ligne de commande selon le système d'exploitation (`escapeCommandLine`, `unescapeCommandLine`). Nous avons choisi **une seule classe** (la consigne en permet 1 à 3) pour pouvoir analyser en détail chaque mutant.

**Pourquoi elle n'est pas déjà bien couverte et a des mutants vivants.** Avec la suite originale complète de `tika-core` (374 tests examinés par PIT, dont 27 exécutés pour tuer des mutants) :

- 70 lignes sur 146 sont couvertes (48 %) ;
- sur **61 mutants**, seulement **3 sont tués** (5 %), **24 survivent** et **34 n'ont aucune couverture** ;
- aucun test original ne vise `ProcessUtils` : elle n'est exercée qu'indirectement, par `FileCommandDetectorTest#testBasic` (via `FileCommandDetector`), qui tue les 3 seuls mutants (`escapeCommandLine` ligne 71, `Thread::start` ligne 107 et valeur de retour de `execute` ligne 149) ;
- méthodes sans aucun test : `unescapeCommandLine`, `checkCommand` (les deux surcharges) et la surcharge `execute(ProcessBuilder, long, Path, int)` ; aucune branche d'erreur de `execute` n'est testée (délai dépassé, interruption, `IOException`).

Le tableau complet des 61 mutants avec leur état à chaque étape est en [annexe](#annexe-les-61-mutants-de-processutils).

## 2. Installation de ChatUniTest dans le pipeline Maven

ChatUniTest est branché dans [`tika-core/pom.xml`](tika-core/pom.xml) par le profil Maven **`chatunitest`** (plugin `io.github.zju-aces-ise:chatunitest-maven-plugin:2.1.1`). PIT est dans le profil **`pitest`** (`pitest-maven` 1.25.9 + `pitest-junit5-plugin` 1.2.3). Ces profils ne sont pas actifs dans un build normal.

Prérequis (machine : MacBook Pro M3 Pro, 36 Go de RAM) :

```bash
brew install ollama && brew services start ollama
ollama pull qwen2.5-coder:7b                          # 4,7 Go, tourne à 100 % sur le GPU (mémoire unifiée)
ollama cp qwen2.5-coder:7b codeqwen:v1.5-chat         # alias, voir ci-dessous
```

Trois obstacles rencontrés pendant l'installation, et leur solution :

| Problème | Cause | Solution |
|---|---|---|
| `Invalid Java version format: 25` | Le plugin ne reconnaît pas le format de version de Java 25. | Lancer Maven avec un JDK 21 (`JAVA_HOME` du JDK 21 pour la commande de génération seulement). |
| `No Model with name qwen2.5-coder:7b` | Le plugin n'accepte qu'une liste fermée de noms : `gpt-3.5-turbo`, `gpt-4o`, `gpt-4o-mini`, `code-llama`, `codeqwen:v1.5-chat`. | Alias Ollama `codeqwen:v1.5-chat` qui pointe vers `qwen2.5-coder:7b`. **Le modèle qui génère réellement les tests est donc `qwen2.5-coder:7b`**, pas CodeQwen. |
| Tous les tests générés qui importent Mockito échouent à la compilation (`package org.mockito does not exist`), et le plugin gaspille 5 rondes de réparation par tentative | Le plugin construit son classpath avec `MavenProject.getCompileClasspathElements()` (portées `compile`, `provided`, `system`) : Mockito, en portée `test`, lui est invisible. | Dans le profil `chatunitest` seulement, redéclarer `mockito-core` et `mockito-junit-jupiter` en portée `provided`. Le build normal garde Mockito en portée `test`. Effet mesuré : le premier essai (sans ce correctif) n'avait terminé qu'environ 10 tentatives sur 30 en 57 minutes, presque uniquement des erreurs de compilation, et a été interrompu ; avec le correctif, les 30 tentatives se terminent en 1 h 11. |

Configuration du profil (extrait) : `temperature` 0,2, `testNumber` 5 (5 tests candidats par méthode), `maxRounds` 5 (jusqu'à 5 rondes de réparation automatique par le LLM), `thread` désactivé, sortie dans `tika-core/chatunitest/`.

## 3. Génération des tests

Commande (depuis la racine du dépôt) :

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

## 4. Les tests générés

- **Sortie brute du plugin :** [`tika-core/chatunitest/tika-parent/tika-core/org/apache/tika/utils/`](tika-core/chatunitest/tika-parent/tika-core/org/apache/tika/utils/) (9 fichiers `ProcessUtils_<méthode>_<n°>_<tentative>_Test.java` et une `ProcessUtils_Suite.java` JUnit 4 que nous n'utilisons pas). Ces fichiers ne sont pas compilés par Maven. Le journal complet du plugin ([`chatunitest-run.log`](tika-core/chatunitest/chatunitest-run.log)) et les 105 messages d'erreur de compilation ou d'exécution ([`error-messages/`](tika-core/chatunitest/error-messages/)) qui ont servi aux statistiques de la section 3 y sont conservés.
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

Nombre total de corrections manuelles : **1 script de mise en forme (9 fichiers), 1 fichier supprimé, 16 tests modifiés sur 40** (11 en échec réparés plus 5 rendus indépendants du système d'exploitation : `2_0` `WithQuotes_Windows` et `WithQuotes_NoWindows`, `2_0` `WithSpaces_NoWindows`, `2_1` `NoQuotes_NoWindows`, `3_1` `QuotesNonWindows`).

## 5. Critique des tests générés et comparaison des oracles

**Qualité des oracles générés.**

- *Pertinents pour `escapeCommandLine` / `unescapeCommandLine`.* Cas nul, argument sans espace, avec espaces, déjà entre guillemets : ce sont les bonnes classes d'équivalence. Les oracles (`assertEquals(entrée, résultat)` ou version entre guillemets) sont corrects, **à condition de tenir compte du système d'exploitation**, ce que le modèle ne fait pas.
- *Redondants.* Les 5 fichiers `escapeCommandLine` contiennent environ 30 tests, mais testent presque les mêmes 4 ou 5 cas (nul, sans espace, espace, guillemets). Seuls 2 mutants supplémentaires sont tués grâce à eux : `arg == null` (ligne 62) et le retour `""` de la ligne 63.
- *Faux ou triviaux pour `execute`.* Le seul fichier retenu vérifie des valeurs que la méthode ne peut pas produire (`1000`, `100`, `"line1\nline2"`), et ne vérifie ni le contenu du fichier de redirection ni le comportement réel d'un processus. Une fois corrigé, il se contente d'`assertTrue(temps >= 0)` et de longueurs nulles : ce sont des vérifications triviales qui ne décrivent pas le comportement attendu.
- *Faible spécificité.* Aucun test généré ne lance un vrai processus : tout est simulé, donc ni la lecture réelle de `stdout` / `stderr`, ni la destruction du processus, ni le délai ne sont réellement exercés.
- *Aucune réflexion sur le non-déterminisme.* Rien sur la concurrence (threads de lecture des flux), les temps d'attente, la fuite de processus.
- *Aucun test pour `checkCommand`* : le modèle ne sait pas contourner `Runtime.getRuntime()` (méthode statique), alors que `mockStatic` de Mockito le permet (nous l'utilisons au chapitre 8).

**Comparaison avec les oracles écrits à la main.** Le seul test original qui touche la classe, `FileCommandDetectorTest#testBasic`, vérifie un comportement de haut niveau (type MIME détecté par la commande `file`) : son oracle est précis mais très indirect, il ne dit rien sur les cas limites de `ProcessUtils`. Nos tests manuels (chapitre 8) vérifient au contraire chaque champ de `FileProcessResult` (`stdout`, `stderr`, `exitValue`, `isTimeout`, longueurs, troncatures, temps), avec de **vrais processus** (`sh -c`) dont la sortie est connue à l'avance, ce qui donne des oracles exacts et indépendants du code (par exemple `echo hello; exit 3` doit donner `stdout == "hello"` et `exitValue == 3`). Les oracles générés, eux, sont déduits du code source lu par le modèle : ils reflètent l'implémentation (y compris ses défauts) plutôt qu'une spécification indépendante, et deviennent faux dès que le modèle se trompe sur le comportement réel (système d'exploitation, mocks jamais utilisés).

## 6. Analyse de mutation avec PIT

Les trois scénarios (mêmes mutateurs `DEFAULTS`, même classe ; commandes exactes à la section 11) :

| Scénario | Tests utilisés | Mutants | Tués | Survivants | Sans couverture | Score | Lignes couvertes |
|---|---|---|---|---|---|---|---|
| A. Originaux | `org.apache.tika.*` sauf `ProcessUtils*Test` | 61 | 3 | 24 | 34 | **5 %** | 70/146 (48 %) |
| B. + générés | A + `ProcessUtils_*_Test` (40 tests corrigés) | 61 | 9 | 33 | 19 | **15 %** | 113/146 (77 %) |
| C. + manuels | B + `ProcessUtilsManualTest` + `ProcessUtilsMockedManualTest` | 61 | 47 (dont 2 par timeout) | 9 | 5 | **77 %** | 140/146 (96 %) |

**Pourquoi pas 100 % de lignes couvertes (140 sur 146).** Les 6 lignes restantes (liste lue dans le rapport PIT du scénario C) ne peuvent pas être atteintes par un test sur macOS ou Linux :

| Lignes de `ProcessUtils.java` | Code | Raison |
|---|---|---|
| 39 et 40 (2 lignes) | corps du *shutdown hook* : `PROCESS_MAP.forEachValue(1, Process::destroyForcibly);` et `}));` | Il ne s'exécute qu'à l'arrêt de la JVM, donc après la fin des tests ; PIT ne le voit jamais. PIT ne génère d'ailleurs aucun mutant dans ce code. |
| 68 et 69 | `escapeCommandLine` : test `startsWith` / `endsWith`, puis ajout des guillemets | Atteint seulement si `SystemUtils.IS_OS_WINDOWS` est vrai. |
| 76 et 77 | `unescapeCommandLine` : test des guillemets, puis `substring` | Idem, uniquement sous Windows. |

Sous Windows, les 4 dernières lignes seraient couvertes par les tests générés marqués `@EnabledOnOs(WINDOWS)` (non vérifié, faute de machine Windows).

Score de PIT = (tués + timeouts) / total. Les rapports HTML complets sont produits par la GitHub Action (section 9) et en local dans `tika-core/target/pit-*/`.

## 7. Mutants détectés et non détectés

### 7.1 Détectés par les tests originaux (3 mutants)

Tous par `FileCommandDetectorTest#testBasic`, qui détecte des types de fichiers avec la commande `file` : `execute` doit rendre un résultat non nul (ligne 149), démarrer le thread qui lit `stdout` (ligne 107, sinon la sortie est vide et le type MIME introuvable), et `escapeCommandLine` ne doit pas rendre `""` (ligne 71).

### 7.2 Détectés en plus par les tests générés (6 mutants)

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

### 7.3 Détectés par les tests manuels (38 mutants)

38 mutants supplémentaires sont tués (voir l'annexe pour le test qui tue chacun). Par famille :

- **`join` des threads de lecture, 8 mutants** (`execute` l. 118, 119, 122, 123, 197, 200 et `checkCommand` l. 267, 268) : tués par des flux simulés qui n'écrivent ou ne se ferment qu'après 400 à 500 ms (`DelayedStream`). Si la méthode ne joint pas le thread, la sortie est vide ou la durée trop courte. Un seul flux est lent à la fois : sinon attendre l'un laisserait l'autre finir et la suppression d'un `join` passerait inaperçue.
- **`Thread::start`, 4 mutants** (`execute` l. 110 et 189, `checkCommand` l. 261 et 262) : sans lecteur, la sortie est vide ou, pour de grosses sorties, le processus bloque (les deux mutants de `checkCommand` sont comptés « tués par timeout » par PIT).
- **`Thread::interrupt`, 2 mutants** (`execute` l. 136, 137) : tués par un flux qui bloque 30 s et signale son interruption (`BlockedStream`).
- **`execute`, 10 autres mutants** (comparaisons `if (complete)`, `completed`, `p != null`, `id != null`, `!isDirectory`, calculs de temps `-` devenu `+`) : tués par les tests avec vrais processus (délai dépassé, code `137`, bornes sur le temps, création des répertoires) et par les simulacres (`destroyForcibly` appelé exactement une ou deux fois, `PROCESS_MAP` revenue à sa taille d'avant).
- **`register`, 1 mutant** (identifiant remplacé par `""`) : tué par les tests qui inspectent la table interne `PROCESS_MAP` par réflexion (aucune fuite après `execute`, y compris en cas d'interruption ou d'échec au démarrage, et deux exécutions simultanées ont deux entrées distinctes).
- **`checkCommand`, 13 autres mutants** : branche du délai de 60 s (simulée avec `mockStatic(Runtime)`), `Error` de `posix_spawn` / `UNIXProcess`, relance des autres `Error` et de `SecurityException`, valeurs d'erreur par défaut (127) ou fournies, commande introuvable.

### 7.4 Mutants non détectés (14 mutants) et pourquoi

| Mutant(s) | Raison | Détectable ? |
|---|---|---|
| `checkCommand` l. 273, 279, 286 (retour remplacé par `false`) et l. 276 (retour remplacé par `true`) | **Mutants équivalents** : ces lignes retournent déjà exactement la valeur par laquelle PIT les remplace (`return false` devient `return false`). | Non, le comportement est identique. |
| `release` l. 50 : retour remplacé par `null` | Le résultat de `release` n'est jamais lu (`release(id);` en instruction seule). | Non, équivalent. |
| `escapeCommandLine` l. 67 (2 mutants, survivants) et l. 68 (2 mutants, sans couverture) ; `unescapeCommandLine` l. 75 (2 survivants), l. 76 (2 sans couverture) et l. 77 (1 sans couverture) | **Code exécutable seulement sous Windows** : la conjonction contient `SystemUtils.IS_OS_WINDOWS`, une constante `static final` fausse sur macOS et Linux ; hors Windows, supprimer l'une des autres conditions ne change rien (la conjonction reste fausse), et les lignes 68, 76 et 77 ne sont jamais atteintes. | **Oui, sous Windows seulement.** Les tests générés marqués `@EnabledOnOs(WINDOWS)` les visent ; ils sont ignorés ici (7 tests ignorés). Nous n'avons pas de machine Windows pour le confirmer. |

Le score réel sur un système Windows serait donc plus élevé ; sous macOS et Linux, 47 est le maximum atteignable : 47 sur les 56 mutants non équivalents, les 9 autres exigeant Windows.

## 8. Tests écrits à la main

Deux classes, 42 tests, tous dans `tika-core/src/test/java/org/apache/tika/utils/`.

- [`ProcessUtilsManualTest`](tika-core/src/test/java/org/apache/tika/utils/ProcessUtilsManualTest.java) (16 tests) : **vrais processus** `sh -c`, donc désactivée sous Windows (`@DisabledOnOs(WINDOWS)`). L'oracle est la sortie connue de la commande shell.
- [`ProcessUtilsMockedManualTest`](tika-core/src/test/java/org/apache/tika/utils/ProcessUtilsMockedManualTest.java) (26 tests) : `Runtime`, `ProcessBuilder` et `Process` **simulés** avec Mockito (`mockStatic(Runtime.class)` pour `checkCommand`). Indépendante du système d'exploitation, déterministe, sans attente de 60 s.

Constantes : délai court `300 ms`, délai long `20 s` (jamais atteint), `137` = code de sortie Java d'un processus tué par `SIGKILL` (128 + 9).

### 8.1 `ProcessUtilsManualTest` (vrais processus)

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

### 8.2 `ProcessUtilsMockedManualTest` (objets simulés)

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

## 9. Exécution dans GitHub Actions

Le workflow [`.github/workflows/tache2.yml`](.github/workflows/tache2.yml) (JDK 17, `ubuntu-latest`) s'exécute à chaque *push* sur `main` ou à la demande, et enchaîne :

1. compilation de `tika-core` ;
2. tests originaux de `tika-core` avec couverture JaCoCo (nouveaux tests exclus) ;
3. **nouveaux tests** (`ProcessUtils*Test`) : doivent tous passer ;
4. PIT avec les tests originaux seulement ;
5. PIT avec originaux + générés ;
6. PIT avec originaux + générés + manuels ;
7. résumé dans l'onglet *Summary* ([`.github/scripts/tache2_summary.py`](.github/scripts/tache2_summary.py)), artefact `tache2-reports` et publication des rapports sur la branche `tache2-reports`.

Résultats locaux de ces mêmes étapes (macOS, JDK 25) : 82 tests `ProcessUtils*Test` (82 exécutés, 0 échec, 7 ignorés), suite complète de `tika-core` (831 tests, 0 échec), Checkstyle sans violation. **Le lien de l'exécution sur GitHub Actions est à ajouter ici après le premier *push* : ** `https://github.com/DavidStanescu13/tika/actions`.

## 10. Déclaration d'usage de l'intelligence artificielle

Conformément aux consignes du cours (usage de l'IA autorisé s'il est documenté), nous déclarons l'usage suivant :

- **Outil :** ChatUniTest (plugin Maven `chatunitest-maven-plugin` 2.1.1, Zhejiang University) pilotant le modèle de langage ouvert **`qwen2.5-coder:7b`** exécuté **localement** avec Ollama (aucune donnée envoyée à un service externe). Le modèle est appelé sous l'alias `codeqwen:v1.5-chat` (section 2).
- **Usage :** génération automatique de tests unitaires pour la classe `ProcessUtils` (section 3), dont 9 fichiers ont été retenus par le plugin.
- **Ce que nous avons fait nous-mêmes :** choix de la classe, installation et configuration, correction des tests générés (section 4), critique de leurs oracles (section 5), analyse des mutants (section 7) et écriture de tous les tests manuels (section 8).

## 11. Reproduire les résultats

```bash
# Tests de la classe (originaux + générés + manuels)
mvn -pl tika-core -Drat.skip=true test -Dtest='ProcessUtils*Test'

# A. PIT : tests originaux seulement
mvn -pl tika-core -Drat.skip=true -Ppitest test-compile org.pitest:pitest-maven:mutationCoverage \
    -DtargetClasses=org.apache.tika.utils.ProcessUtils -DtargetTests='org.apache.tika.*' \
    -DexcludedTestClasses='org.apache.tika.utils.ProcessUtils*Test' -DreportsDirectory=target/pit-original

# B. PIT : originaux + générés
mvn -pl tika-core -Drat.skip=true -Ppitest test-compile org.pitest:pitest-maven:mutationCoverage \
    -DtargetClasses=org.apache.tika.utils.ProcessUtils -DtargetTests='org.apache.tika.*' \
    -DexcludedTestClasses='org.apache.tika.utils.ProcessUtils*ManualTest' -DreportsDirectory=target/pit-generated

# C. PIT : originaux + générés + manuels
mvn -pl tika-core -Drat.skip=true -Ppitest test-compile org.pitest:pitest-maven:mutationCoverage \
    -DtargetClasses=org.apache.tika.utils.ProcessUtils -DtargetTests='org.apache.tika.*' \
    -DreportsDirectory=target/pit-final
```

Les rapports sont dans `tika-core/target/pit-*/index.html`. La génération ChatUniTest est décrite à la section 3 ; elle n'est pas relancée par la GitHub Action (le modèle local n'est pas disponible sur un exécuteur GitHub, et ses résultats sont versionnés dans `tika-core/chatunitest/`).

---

## Annexe, les 61 mutants de ProcessUtils

État dans le scénario C (originaux + générés + manuels). « Test qui le tue » : premier test qui détecte le mutant selon PIT. Les numéros de ligne sont ceux de PIT dans [`ProcessUtils.java`](tika-core/src/main/java/org/apache/tika/utils/ProcessUtils.java).

| Ligne | Méthode | Mutation (PIT) | État | Test qui le tue |
|---|---|---|---|---|
| 46 | `register` | return value -> "" for register | tue | `ProcessUtilsMockedManualTest#executeDoesNotLeakProcessesInTheInternalMap` |
| 50 | `release` | return value -> null for release | survivant |  |
| 62 | `escapeCommandLine` | condition -> false | tue | `ProcessUtils_escapeCommandLine_2_0_Test#testEscapeCommandLine_NullInput` |
| 63 | `escapeCommandLine` | return value -> "" for escapeCommandLine | tue | `ProcessUtils_escapeCommandLine_2_4_Test#testEscapeCommandLine_NullInput` |
| 67 | `escapeCommandLine` | condition -> false | survivant |  |
| 67 | `escapeCommandLine` | condition -> false | survivant |  |
| 68 | `escapeCommandLine` | condition -> false | sans couverture |  |
| 68 | `escapeCommandLine` | condition -> false | sans couverture |  |
| 71 | `escapeCommandLine` | return value -> "" for escapeCommandLine | tue | `ProcessUtils_escapeCommandLine_2_3_Test#testEscapeCommandLine_WithQuotes_NoSpaces_Windows` |
| 75 | `unescapeCommandLine` | condition -> false | survivant |  |
| 75 | `unescapeCommandLine` | condition -> false | survivant |  |
| 76 | `unescapeCommandLine` | condition -> false | sans couverture |  |
| 76 | `unescapeCommandLine` | condition -> false | sans couverture |  |
| 77 | `unescapeCommandLine` | - replaced by + | sans couverture |  |
| 79 | `unescapeCommandLine` | return value -> "" for unescapeCommandLine | tue | `ProcessUtils_unescapeCommandLine_3_1_Test#testUnescapeCommandLine_QuotesNonWindows` |
| 107 | `execute` | call removed: Thread::start | tue | `ProcessUtilsManualTest#executeTruncatesStdoutAndStderrAtTheirOwnLimits` |
| 110 | `execute` | call removed: Thread::start | tue | `ProcessUtilsManualTest#executeTruncatesStdoutAndStderrAtTheirOwnLimits` |
| 115 | `execute` | Replaced long subtraction with addition | tue | `ProcessUtilsManualTest#executeCapturesStdoutStderrAndExitValue` |
| 116 | `execute` | condition -> false | tue | `ProcessUtilsMockedManualTest#executeWaitsForSlowStdoutReaderOfAFinishedProcess` |
| 118 | `execute` | call removed: Thread::join | tue | `ProcessUtilsMockedManualTest#executeWaitsForSlowStdoutReaderOfAFinishedProcess` |
| 119 | `execute` | call removed: Thread::join | tue | `ProcessUtilsMockedManualTest#executeWaitsForSlowStderrReaderOfAFinishedProcess` |
| 122 | `execute` | call removed: Thread::join | tue | `ProcessUtilsMockedManualTest#executeWaitsForSlowStdoutReaderAfterKillingProcessOnTimeout` |
| 123 | `execute` | call removed: Thread::join | tue | `ProcessUtilsMockedManualTest#executeWaitsForSlowStderrReaderAfterKillingProcessOnTimeout` |
| 125 | `execute` | condition -> false | tue | `ProcessUtilsManualTest#executeKillsProcessThatExceedsTimeout` |
| 136 | `execute` | call removed: Thread::interrupt | tue | `ProcessUtilsMockedManualTest#executeInterruptsStdoutReaderThatIsStillBlocked` |
| 137 | `execute` | call removed: Thread::interrupt | tue | `ProcessUtilsMockedManualTest#executeInterruptsStderrReaderThatIsStillBlocked` |
| 143 | `execute` | condition -> false | tue | `ProcessUtilsMockedManualTest#executeKeepsMinusOneWhenKilledProcessStillReportsRunning` |
| 149 | `execute` | return value -> null for execute | tue | `ProcessUtilsMockedManualTest#executeDestroysProcessAndForgetsItWhenInterrupted` |
| 151 | `execute` | condition -> false | tue | `ProcessUtilsMockedManualTest#executeDestroysProcessAndForgetsItWhenInterrupted` |
| 154 | `execute` | condition -> false | tue | `ProcessUtilsMockedManualTest#executeDoesNotLeakProcessesInTheInternalMap` |
| 174 | `execute` | condition -> false | tue | `ProcessUtilsManualTest#executeRedirectWritesStdoutToFileAndCreatesMissingDirectories` |
| 189 | `execute` | call removed: Thread::start | tue | `ProcessUtilsManualTest#executeRedirectTruncatesStderrOnly` |
| 194 | `execute` | Replaced long subtraction with addition | tue | `ProcessUtilsManualTest#executeRedirectWritesStdoutToFileAndCreatesMissingDirectories` |
| 195 | `execute` | condition -> false | tue | `ProcessUtils_execute_5_4_Test#testExecute_Success` |
| 197 | `execute` | call removed: Thread::join | tue | `ProcessUtilsMockedManualTest#executeRedirectWaitsForStderrReaderOfAFinishedProcess` |
| 200 | `execute` | call removed: Thread::join | tue | `ProcessUtilsMockedManualTest#executeRedirectWaitsForStderrReaderAfterKillingProcessOnTimeout` |
| 209 | `execute` | condition -> false | tue | `ProcessUtils_execute_5_4_Test#testExecute_InterruptedException` |
| 215 | `execute` | return value -> null for execute | tue | `ProcessUtils_execute_5_4_Test#testExecute_InterruptedException` |
| 217 | `execute` | condition -> false | tue | `ProcessUtilsMockedManualTest#executeRedirectDestroysProcessAndForgetsItWhenInterrupted` |
| 220 | `execute` | condition -> false | tue | `ProcessUtilsMockedManualTest#executeRedirectDestroysProcessAndForgetsItWhenInterrupted` |
| 237 | `checkCommand` | boolean return -> false for checkCommand | tue | `ProcessUtilsManualTest#checkCommandReturnsTrueWhenCommandSucceeds` |
| 237 | `checkCommand` | boolean return -> true for checkCommand | tue | `ProcessUtilsManualTest#checkCommandReturnsFalseWhenCommandCannotBeStarted` |
| 250 | `checkCommand` | condition -> false | tue | `ProcessUtilsManualTest#checkCommandOnlyTreats127AsErrorByDefault` |
| 261 | `checkCommand` | call removed: Thread::start | tue (timeout) |  |
| 262 | `checkCommand` | call removed: Thread::start | tue (timeout) |  |
| 264 | `checkCommand` | condition -> false | tue | `ProcessUtilsMockedManualTest#checkCommandReturnsFalseAndKillsProcessWhenItNeverFinishes` |
| 267 | `checkCommand` | call removed: Thread::join | tue | `ProcessUtilsMockedManualTest#checkCommandWaitsForStdoutReaderToFinish` |
| 268 | `checkCommand` | call removed: Thread::join | tue | `ProcessUtilsMockedManualTest#checkCommandWaitsForStderrReaderToFinish` |
| 272 | `checkCommand` | condition -> false | tue | `ProcessUtilsManualTest#checkCommandOnlyTreats127AsErrorByDefault` |
| 273 | `checkCommand` | boolean return -> false for checkCommand | survivant |  |
| 273 | `checkCommand` | boolean return -> true for checkCommand | tue | `ProcessUtilsManualTest#checkCommandOnlyTreats127AsErrorByDefault` |
| 276 | `checkCommand` | boolean return -> false for checkCommand | tue | `ProcessUtilsManualTest#checkCommandDrainsLargeStderrSoTheProcessCanFinish` |
| 276 | `checkCommand` | boolean return -> true for checkCommand | survivant |  |
| 279 | `checkCommand` | boolean return -> false for checkCommand | survivant |  |
| 279 | `checkCommand` | boolean return -> true for checkCommand | tue | `ProcessUtilsMockedManualTest#checkCommandReturnsFalseWhenInterrupted` |
| 283 | `checkCommand` | condition -> false | tue | `ProcessUtilsMockedManualTest#checkCommandReturnsFalseOnPosixSpawnError` |
| 283 | `checkCommand` | condition -> false | tue | `ProcessUtilsMockedManualTest#checkCommandRethrowsUnrelatedErrors` |
| 284 | `checkCommand` | condition -> false | tue | `ProcessUtilsMockedManualTest#checkCommandReturnsFalseOnUnixProcessError` |
| 286 | `checkCommand` | boolean return -> false for checkCommand | survivant |  |
| 286 | `checkCommand` | boolean return -> true for checkCommand | tue | `ProcessUtilsMockedManualTest#checkCommandReturnsFalseOnPosixSpawnError` |
| 290 | `checkCommand` | condition -> false | tue | `ProcessUtilsMockedManualTest#checkCommandReturnsFalseWhenInterrupted` |

---

# README d'origine d'Apache Tika

Welcome to Apache Tika  <https://tika.apache.org/>
=================================================

[![license](https://img.shields.io/github/license/apache/tika.svg?maxAge=2592000)](http://www.apache.org/licenses/LICENSE-2.0)
[![Jenkins](https://img.shields.io/jenkins/s/https/ci-builds.apache.org/job/Tika/job/tika-main-jdk17.svg?maxAge=3600)](https://ci-builds.apache.org/job/Tika/job/tika-main-jdk17/)
[![Jenkins tests](https://img.shields.io/jenkins/t/https/ci-builds.apache.org/job/Tika/job/tika-main-jdk17.svg?maxAge=3600)](https://ci-builds.apache.org/job/Tika/job/tika-main-jdk17/lastBuild/testReport/)
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.tika/tika.svg?maxAge=86400)](http://search.maven.org/#search|ga|1|g%3A%22org.apache.tika%22)

Apache Tika(TM) is a toolkit for detecting and extracting metadata and structured text content from various documents using existing parser libraries.

Tika is a project of the [Apache Software Foundation](https://www.apache.org).

Apache Tika, Tika, Apache, the Apache feather logo, and the Apache Tika project logo are trademarks of The Apache Software Foundation.

Quick Start
===========

**Parse a file in Java:**

```java
import org.apache.tika.Tika;

Tika tika = new Tika();
String text = tika.parseToString(new File("document.pdf"));
System.out.println(text);
```

**From the command line:**

```bash
java -jar tika-app-*.jar --text document.pdf
```

**Maven dependency:**

```xml
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-parsers-standard-package</artifactId>
    <version>4.x.y</version>
    <type>pom</type>
</dependency>
```

Getting Started
===============
Pre-built binaries of Apache Tika standalone applications are available
from https://tika.apache.org/download.html . Pre-built binaries of all the
Tika jars can be fetched from Maven Central or your favourite Maven mirror.

**Tika 2.X and support for Java 8 reached End of Life (EOL) in April, 2025. 
See [Tika Roadmap 2.x, 3.x and beyond](https://cwiki.apache.org/confluence/display/TIKA/Tika+Roadmap+--+2.x%2C+3.x+and+Beyond).** 

Tika is based on **Java 17** and uses the [Maven 3](https://maven.apache.org) build system.
**N.B.** [Docker](https://www.docker.com/products/personal) is used for tests in tika-integration-tests. If Docker is not installed, those tests are skipped.

To build Tika from source, use the following command in the main directory:

    ./mvnw clean install

The Maven wrapper (`mvnw`) is included in the repository and will automatically download
the correct Maven version if needed. On Windows, use `mvnw.cmd` instead.

The build consists of a number of components, including a standalone runnable jar that you can use to try out Tika features. You can run it like this:

    java -jar tika-app/target/tika-app-*.jar --help


To build a specific project (for example, tika-server-standard):

    ./mvnw clean install -am -pl :tika-server-standard

If the ossindex-maven-plugin is causing the build to fail because a dependency
has now been discovered to have a vulnerability:

    ./mvnw clean install -Dossindex.skip


Faster Builds
=============

**Fast profile** - Use `-Pfast` to skip tests, checkstyle, and spotless:

    ./mvnw clean install -Pfast

**Parallel builds** - Add `-T1C` to build with 1 thread per CPU core:

    ./mvnw clean install -Pfast -T1C

**Maven Daemon (mvnd)** - Keeps a warm JVM running for 2-3x faster rebuilds:

```bash
# Install: https://github.com/apache/maven-mvnd
# macOS: brew install mvndaemon/tap/mvnd

# Use exactly like mvn
mvnd clean install -Pfast
mvnd test -pl :tika-core
```

**Combine both** for maximum speed during development:

    mvnd clean install -Pfast -T1C


Reproducible Builds
===================

Apache Tika supports [reproducible builds](https://reproducible-builds.org/). This means
that building the same source code with the same JDK version should produce
byte-for-byte identical artifacts, regardless of the build machine or time.

Key configuration:
- `project.build.outputTimestamp` is set in `tika-parent/pom.xml`
- All Maven plugins are configured to produce deterministic output

To verify the build plan supports reproducibility:

    ./mvnw artifact:check-buildplan

To verify two builds produce identical artifacts:

    ./mvnw clean install -DskipTests
    mv ~/.m2/repository/org/apache/tika tika-build-1
    ./mvnw clean install -DskipTests
    diff -r tika-build-1 ~/.m2/repository/org/apache/tika


Maven Dependencies
==================

Apache Tika provides *Bill of Material* (BOM) artifact to align Tika module versions and simplify version management. 
To avoid convergence errors in your own project, import this
bom or Tika's parent pom.xml in your dependency management section.

If you use Apache Maven:

```xml
<project>
  <dependencyManagement>
    <dependencies>
      <dependency>
       <groupId>org.apache.tika</groupId>
       <artifactId>tika-bom</artifactId>
       <version>4.x.y</version>
       <type>pom</type>
       <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <dependencies>
    <dependency>
      <groupId>org.apache.tika</groupId>
      <artifactId>tika-parsers-standard-package</artifactId>
      <type>pom</type>
      <!-- version not required since BOM included -->
    </dependency>
  </dependencies>
</project>
```

For Gradle:

```kotlin
dependencies {
  implementation(platform("org.apache.tika:tika-bom:4.x.y"))

  // version not required since bom (platform in Gradle terms)
  implementation("org.apache.tika:tika-parsers-standard-package@pom")
}
```

Migrating to 4.x
================
TBD

Contributing
============
See [CONTRIBUTING.md](CONTRIBUTING.md) and https://tika.apache.org/contribute.html

[![contributors](https://contributors-img.web.app/image?repo=apache/tika)](https://github.com/apache/tika/graphs/contributors)

Building from a Specific Tag
============================
Let's assume that you want to build the 3.0.1 tag:
```
0. Download and install hub.github.com
1. git clone https://github.com/apache/tika.git
2. cd tika
3. git checkout 3.0.1
4. ./mvnw clean install
```

If a new vulnerability has been discovered between the date of the
tag and the date you are building the tag, you may need to build with:

```
4. ./mvnw clean install -Dossindex.skip
```

If a local test is not working in your environment, please notify
 the project at dev@tika.apache.org. As an immediate workaround,
 you can turn off individual tests with e.g.:

```
4. ./mvnw clean install -Dossindex.skip -Dtest=\!UnpackerResourceTest#testPDFImages
```

License (see also LICENSE.txt)
==============================

Collective work: Copyright 2011 The Apache Software Foundation.

Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at

<https://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the specific language governing permissions and limitations under the License.

Apache Tika includes a number of subcomponents with separate copyright notices and license terms. Your use of these subcomponents is subject to the terms and conditions of the licenses listed in the LICENSE.txt file.

Export Control
==============

This distribution includes cryptographic software.  The country in which you currently reside may have restrictions on the import, possession, use, and/or re-export to another country, of encryption software.  BEFORE using any encryption software, please  check your country's laws, regulations and policies concerning the import, possession, or use, and re-export of encryption software, to  see if this is permitted.  See <http://www.wassenaar.org/> for more information.

The U.S. Government Department of Commerce, Bureau of Industry and Security (BIS), has classified this software as Export Commodity Control Number (ECCN) 5D002.C.1, which includes information security software using or performing cryptographic functions with asymmetric algorithms.  The form and manner of this Apache Software Foundation distribution makes it eligible for export under the License Exception ENC Technology Software Unrestricted (TSU) exception (see the BIS Export Administration Regulations, Section 740.13) for both object code and source code.

The following provides more details on the included cryptographic software:

Apache Tika uses the Bouncy Castle generic encryption libraries for extracting text content and metadata from encrypted PDF files.  See <http://www.bouncycastle.org/> for more details on Bouncy Castle.  

Mailing Lists
=============

* user@tika.apache.org - About using Tika
* dev@tika.apache.org - About developing Tika

Subscribe by sending a message to `{list}-subscribe@tika.apache.org`.

Issue Tracker
=============

https://issues.apache.org/jira/browse/TIKA

Security
========

See [SECURITY.md](SECURITY.md) and https://tika.apache.org/security.html
