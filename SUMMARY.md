## IFT3913 Tache 2 - resume

### Couverture JaCoCo - tests originaux

| Classe | Instructions | Branches | Lignes | Methodes |
|---|---|---|---|---|
| ProcessUtils | 50.7% (266/525) | 22.2% (12/54) | 50.0% (71/142) | 63.6% (7/11) |

### Couverture JaCoCo - nouveaux tests seuls (ChatUniTest + manuels)

| Classe | Instructions | Branches | Lignes | Methodes |
|---|---|---|---|---|
| ProcessUtils | 94.9% (498/525) | 81.5% (44/54) | 97.2% (138/142) | 100.0% (11/11) |

### PIT - tests originaux

| Classe | Mutants | Tues | Survivants | Sans couverture | Timeout / autres | Score |
|---|---|---|---|---|---|---|
| ProcessUtils | 61 | 3 | 24 | 34 | 0 | 4.9% |

Score = (KILLED + TIMED_OUT + MEMORY_ERROR + RUN_ERROR) / total, comme PIT.

<details><summary>Mutants non detectes (SURVIVED / NO_COVERAGE)</summary>

| Classe | Ligne | Methode | Mutateur | Statut |
|---|---|---|---|---|
| ProcessUtils | 46 | register | EmptyObjectReturnValsMutator | SURVIVED |
| ProcessUtils | 50 | release | NullReturnValsMutator | SURVIVED |
| ProcessUtils | 62 | escapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 63 | escapeCommandLine | EmptyObjectReturnValsMutator | NO_COVERAGE |
| ProcessUtils | 67 | escapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 67 | escapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 68 | escapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 68 | escapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 75 | unescapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 75 | unescapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 76 | unescapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 76 | unescapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 77 | unescapeCommandLine | MathMutator | NO_COVERAGE |
| ProcessUtils | 79 | unescapeCommandLine | EmptyObjectReturnValsMutator | NO_COVERAGE |
| ProcessUtils | 110 | execute | VoidMethodCallMutator | SURVIVED |
| ProcessUtils | 115 | execute | MathMutator | SURVIVED |
| ProcessUtils | 116 | execute | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 118 | execute | VoidMethodCallMutator | SURVIVED |
| ProcessUtils | 119 | execute | VoidMethodCallMutator | SURVIVED |
| ProcessUtils | 122 | execute | VoidMethodCallMutator | NO_COVERAGE |
| ProcessUtils | 123 | execute | VoidMethodCallMutator | NO_COVERAGE |
| ProcessUtils | 125 | execute | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 136 | execute | VoidMethodCallMutator | SURVIVED |
| ProcessUtils | 137 | execute | VoidMethodCallMutator | SURVIVED |
| ProcessUtils | 143 | execute | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 151 | execute | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 154 | execute | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 174 | execute | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 189 | execute | VoidMethodCallMutator | NO_COVERAGE |
| ProcessUtils | 194 | execute | MathMutator | NO_COVERAGE |
| ProcessUtils | 195 | execute | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 197 | execute | VoidMethodCallMutator | NO_COVERAGE |
| ProcessUtils | 200 | execute | VoidMethodCallMutator | NO_COVERAGE |
| ProcessUtils | 209 | execute | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 215 | execute | NullReturnValsMutator | NO_COVERAGE |
| ProcessUtils | 217 | execute | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 220 | execute | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 237 | checkCommand | BooleanFalseReturnValsMutator | NO_COVERAGE |
| ProcessUtils | 237 | checkCommand | BooleanTrueReturnValsMutator | NO_COVERAGE |
| ProcessUtils | 250 | checkCommand | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 261 | checkCommand | VoidMethodCallMutator | SURVIVED |
| ProcessUtils | 262 | checkCommand | VoidMethodCallMutator | SURVIVED |
| ProcessUtils | 264 | checkCommand | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 267 | checkCommand | VoidMethodCallMutator | SURVIVED |
| ProcessUtils | 268 | checkCommand | VoidMethodCallMutator | SURVIVED |
| ProcessUtils | 272 | checkCommand | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 273 | checkCommand | BooleanFalseReturnValsMutator | NO_COVERAGE |
| ProcessUtils | 273 | checkCommand | BooleanTrueReturnValsMutator | NO_COVERAGE |
| ProcessUtils | 276 | checkCommand | BooleanFalseReturnValsMutator | SURVIVED |
| ProcessUtils | 276 | checkCommand | BooleanTrueReturnValsMutator | SURVIVED |
| ProcessUtils | 279 | checkCommand | BooleanFalseReturnValsMutator | NO_COVERAGE |
| ProcessUtils | 279 | checkCommand | BooleanTrueReturnValsMutator | NO_COVERAGE |
| ProcessUtils | 283 | checkCommand | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 283 | checkCommand | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 284 | checkCommand | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 286 | checkCommand | BooleanFalseReturnValsMutator | NO_COVERAGE |
| ProcessUtils | 286 | checkCommand | BooleanTrueReturnValsMutator | NO_COVERAGE |
| ProcessUtils | 290 | checkCommand | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |

</details>

### PIT - tests originaux + generes (ChatUniTest)

| Classe | Mutants | Tues | Survivants | Sans couverture | Timeout / autres | Score |
|---|---|---|---|---|---|---|
| ProcessUtils | 61 | 9 | 33 | 19 | 0 | 14.8% |

Score = (KILLED + TIMED_OUT + MEMORY_ERROR + RUN_ERROR) / total, comme PIT.

<details><summary>Mutants non detectes (SURVIVED / NO_COVERAGE)</summary>

| Classe | Ligne | Methode | Mutateur | Statut |
|---|---|---|---|---|
| ProcessUtils | 46 | register | EmptyObjectReturnValsMutator | SURVIVED |
| ProcessUtils | 50 | release | NullReturnValsMutator | SURVIVED |
| ProcessUtils | 67 | escapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 67 | escapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 68 | escapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 68 | escapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 75 | unescapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 75 | unescapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 76 | unescapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 76 | unescapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 77 | unescapeCommandLine | MathMutator | NO_COVERAGE |
| ProcessUtils | 110 | execute | VoidMethodCallMutator | SURVIVED |
| ProcessUtils | 115 | execute | MathMutator | SURVIVED |
| ProcessUtils | 116 | execute | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 118 | execute | VoidMethodCallMutator | SURVIVED |
| ProcessUtils | 119 | execute | VoidMethodCallMutator | SURVIVED |
| ProcessUtils | 122 | execute | VoidMethodCallMutator | NO_COVERAGE |
| ProcessUtils | 123 | execute | VoidMethodCallMutator | NO_COVERAGE |
| ProcessUtils | 125 | execute | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 136 | execute | VoidMethodCallMutator | SURVIVED |
| ProcessUtils | 137 | execute | VoidMethodCallMutator | SURVIVED |
| ProcessUtils | 143 | execute | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 151 | execute | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 154 | execute | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 174 | execute | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 189 | execute | VoidMethodCallMutator | SURVIVED |
| ProcessUtils | 194 | execute | MathMutator | SURVIVED |
| ProcessUtils | 197 | execute | VoidMethodCallMutator | SURVIVED |
| ProcessUtils | 200 | execute | VoidMethodCallMutator | SURVIVED |
| ProcessUtils | 217 | execute | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 220 | execute | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 237 | checkCommand | BooleanFalseReturnValsMutator | NO_COVERAGE |
| ProcessUtils | 237 | checkCommand | BooleanTrueReturnValsMutator | NO_COVERAGE |
| ProcessUtils | 250 | checkCommand | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 261 | checkCommand | VoidMethodCallMutator | SURVIVED |
| ProcessUtils | 262 | checkCommand | VoidMethodCallMutator | SURVIVED |
| ProcessUtils | 264 | checkCommand | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 267 | checkCommand | VoidMethodCallMutator | SURVIVED |
| ProcessUtils | 268 | checkCommand | VoidMethodCallMutator | SURVIVED |
| ProcessUtils | 272 | checkCommand | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 273 | checkCommand | BooleanFalseReturnValsMutator | NO_COVERAGE |
| ProcessUtils | 273 | checkCommand | BooleanTrueReturnValsMutator | NO_COVERAGE |
| ProcessUtils | 276 | checkCommand | BooleanFalseReturnValsMutator | SURVIVED |
| ProcessUtils | 276 | checkCommand | BooleanTrueReturnValsMutator | SURVIVED |
| ProcessUtils | 279 | checkCommand | BooleanFalseReturnValsMutator | NO_COVERAGE |
| ProcessUtils | 279 | checkCommand | BooleanTrueReturnValsMutator | NO_COVERAGE |
| ProcessUtils | 283 | checkCommand | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 283 | checkCommand | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 284 | checkCommand | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 286 | checkCommand | BooleanFalseReturnValsMutator | NO_COVERAGE |
| ProcessUtils | 286 | checkCommand | BooleanTrueReturnValsMutator | NO_COVERAGE |
| ProcessUtils | 290 | checkCommand | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |

</details>

### PIT - tests originaux + generes + manuels

| Classe | Mutants | Tues | Survivants | Sans couverture | Timeout / autres | Score |
|---|---|---|---|---|---|---|
| ProcessUtils | 61 | 45 | 9 | 5 | 2 | 77.0% |

Score = (KILLED + TIMED_OUT + MEMORY_ERROR + RUN_ERROR) / total, comme PIT.

<details><summary>Mutants non detectes (SURVIVED / NO_COVERAGE)</summary>

| Classe | Ligne | Methode | Mutateur | Statut |
|---|---|---|---|---|
| ProcessUtils | 50 | release | NullReturnValsMutator | SURVIVED |
| ProcessUtils | 67 | escapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 67 | escapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 68 | escapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 68 | escapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 75 | unescapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 75 | unescapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | SURVIVED |
| ProcessUtils | 76 | unescapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 76 | unescapeCommandLine | RemoveConditionalMutator_EQUAL_ELSE | NO_COVERAGE |
| ProcessUtils | 77 | unescapeCommandLine | MathMutator | NO_COVERAGE |
| ProcessUtils | 273 | checkCommand | BooleanFalseReturnValsMutator | SURVIVED |
| ProcessUtils | 276 | checkCommand | BooleanTrueReturnValsMutator | SURVIVED |
| ProcessUtils | 279 | checkCommand | BooleanFalseReturnValsMutator | SURVIVED |
| ProcessUtils | 286 | checkCommand | BooleanFalseReturnValsMutator | SURVIVED |

</details>

