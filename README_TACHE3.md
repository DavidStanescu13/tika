# IFT3913 - Tâche 3 : tests de sécurité avec Semgrep

Binôme : David Stanescu (DavidStanescu13) et Aness Lakhdar Azzouz (ans1008).
Dépôt : https://github.com/DavidStanescu13/tika, branche `tache3`.
Cas d'étude : tika-core, tika-serialization et les modules parser OCR, PDF et Microsoft.

## 1. Workflow Semgrep

Fichier : `.github/workflows/semgrep.yml`.

- Le build échoue seulement si un commit introduit une nouvelle détection : `semgrep scan --baseline-commit <réf> --error`. Tika en contient déjà 3, qui ne doivent pas bloquer le build.
- Commit de référence : la base de la PR, le commit précédent pour un push, ou `HEAD^`. Le checkout est complet (`fetch-depth: 0`) pour que ce commit existe.
- Seuls les cinq modules du cas d'étude sont analysés, avec les règles `p/java`, les mêmes que pour le rapport de base.
- Déclencheurs : PR vers `main` ou `tache3`, et push sur `tache3` et `tache3-**`.

Validation :

- Commit sans changement Java : vert (run 43754809935).
- PR propre (PR #1) : vert (run 43766511781).
- Push des vulnérabilités 5 à 7 : rouge, 3 nouvelles détections (run 44004471064).
- PR avec les 7 vulnérabilités (PR #2) : rouge, 7 nouvelles détections (run 44006559348).

## 2. Vulnérabilités introduites

Rapport de base : `semgrep/baseline.json`, 3 détections préexistantes (deux XXE et un socket non chiffré). Les 7 vulnérabilités sont sur la branche `tache3-vulns` (PR #2), fusionnée ensuite dans `tache3`. Elles sont dans du code nouveau ou un chemin de repli, pour que les tests existants passent toujours.

1. **Désérialisation non sûre (CWE-502)**, `JsonMetadata.java`. `fromJsonAsMap` active `enableDefaultTyping()`, donc le JSON choisit la classe instanciée. Règle : `jackson-unsafe-deserialization`.
2. **Injection SQL (CWE-89)**, `TikaInputStream.java`. `get(Connection, table, documentId, metadata)` concatène l'identifiant dans la requête. `x' OR '1'='1` en change le sens. Règle : `formatted-sql-string`.
3. **IV statique en CBC (CWE-329)**, `CryptoParser.java`. Sans paramètres du contexte, le déchiffrement utilise un IV constant codé en dur. Règle : `no-static-initialization-vector`.
4. **TLS faible (CWE-326)**, `NetworkParser.java`. Le schéma `telnets` ouvre un `SSLContext` en `TLSv1`, un protocole obsolète. Règle : `weak-ssl-context`.
5. **Hachage faible (CWE-328)**, `InputStreamDigester.java`. Si l'algorithme demandé est absent, le code retombe sur MD5, qui a des collisions. Règle : `use-of-md5`.
6. **XXE (CWE-611)**, `XMLReaderUtils.java`. `buildDOMWithEntities` crée une `DocumentBuilderFactory` qui développe les entités sans désactiver le DOCTYPE. Règle : `documentbuilderfactory-disallow-doctype-decl-missing`.
7. **Chiffrement ECB (CWE-327)**, `CryptoParser.java`. La transformation par défaut est `AES/ECB/PKCS5Padding`, qui laisse apparaître les motifs du clair. Règle : `use-of-aes-ecb`.

Choix des fichiers : classes de `tika-core` et `tika-serialization` liées à la sécurité (chiffrement, XML, réseau, hachage, JSON), où ces motifs sont plausibles.

## 3. Avant et après

Avant : 3 détections (rapport de base). Après, en mode diff contre ce rapport : 7 nouvelles détections, une par vulnérabilité, soit 10 au total. C'est ce que montre le run de la PR #2. Les 3 détections de base ne sont pas recomptées. Le build Semgrep de `tache3` est donc rouge depuis la fusion, comme prévu.

## 4. Humour

Quand un test échoue, `tache3-tests.yml` appelle l'action locale `.github/actions/test-failure-report`. Elle ajoute au résumé du run et en commentaire de PR une miniature cliquable qui mène à une vidéo YouTube. Elle ne s'exécute que si l'étape des tests échoue.

Le rickroll a été choisi minutieusement. Nous pensons que c'est le meilleur rickroll. Démonstration : PR #1, avec un test volontairement en échec (run 43765967492).

## 5. Usage de l'IA

Nous avons utilisé Claude (Anthropic) via Claude Code pour : rédiger les workflows `semgrep.yml` et `tache3-tests.yml`, écrire l'action `test-failure-report`, produire le premier squelette de ce README et introduire le premier test volontairement en échec (`TestFailureReportTest`). Nous avons vérifié le résultat par les runs GitHub Actions et par des scans Semgrep en local.
