#!/usr/bin/env bash
# IFT3913 - Tache 2 : generation de tests avec ChatUniTest + Ollama (modele local)
#
# Prerequis :
#   - Java 17+ (Maven est optionnel : le wrapper ./mvnw de Tika est utilise sinon)
#   - Ollama installe et lance (https://ollama.com), avec un modele telecharge et renomme :
#         ollama pull qwen2.5-coder:7b        (ou qwen2.5-coder:1.5b sur une machine modeste)
#         ollama cp qwen2.5-coder:7b codeqwen:v1.5-chat   (nom exige par ChatUniTest, voir plus bas)
#
# Usage (depuis la racine du depot) :
#   bash tache2/run-chatunitest.sh                       # ProcessUtils et CharsetUtils
#   bash tache2/run-chatunitest.sh ProcessUtils          # une seule classe
#   MODEL=code-llama bash tache2/run-chatunitest.sh     (autre nom accepte par ChatUniTest)
#
# Resultat : les tests generes sont dans tika-core/chatunitest/ (bruts) ; les tests valides
# (<Classe>_<methode>_<i>_<j>_Test.java) sont copies tels quels dans
# tika-core/src/test/java/org/apache/tika/utils/ (c'est ce que le workflow GitHub execute et que PIT utilise).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# Maven : on utilise `mvn` s'il est installe, sinon le Maven Wrapper fourni par Tika (./mvnw),
# qui telecharge Maven tout seul (seul Java 17+ est requis).
if command -v mvn >/dev/null 2>&1; then
  MVN=mvn
else
  MVN="$ROOT/mvnw"
  echo ">> mvn introuvable : utilisation du Maven Wrapper $MVN"
fi
# ChatUniTest 2.1.1 ne sait pas analyser les numeros de version Java recents ("Invalid Java version
# format: 26") : il faut un JDK 17 ou 21. Si JAVA_HOME ne pointe pas dessus, on cherche un JDK 17/21
# aux emplacements habituels (Adoptium/Temurin, Oracle, Microsoft) et on l'utilise pour ce script.
JAVA_MAJOR=$("${JAVA_HOME:+$JAVA_HOME/bin/}java" -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1)
if [ -n "$JAVA_MAJOR" ] && [ "$JAVA_MAJOR" -gt 21 ]; then
  echo ">> Java $JAVA_MAJOR detecte : ChatUniTest exige un JDK 17 ou 21, recherche..."
  for d in "/c/Program Files/Eclipse Adoptium"/jdk-17* "/c/Program Files/Eclipse Adoptium"/jdk-21* \
           "/c/Program Files/Java"/jdk-17* "/c/Program Files/Java"/jdk-21* \
           "/c/Program Files/Microsoft"/jdk-17* "/c/Program Files/Microsoft"/jdk-21* \
           /usr/lib/jvm/java-17* /usr/lib/jvm/java-21* /usr/lib/jvm/temurin-17* /usr/lib/jvm/temurin-21* \
           /Library/Java/JavaVirtualMachines/temurin-17*/Contents/Home /Library/Java/JavaVirtualMachines/temurin-21*/Contents/Home; do
    if [ -x "$d/bin/java" ]; then
      export JAVA_HOME="$d"; export PATH="$d/bin:$PATH"
      echo ">> JAVA_HOME=$JAVA_HOME"; break
    fi
  done
  JAVA_MAJOR=$(java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1)
  if [ "$JAVA_MAJOR" -gt 21 ]; then
    echo "!! Aucun JDK 17/21 trouve. Installez-en un (ex. PowerShell : winget install EclipseAdoptium.Temurin.17.JDK)"
    echo "   ou lancez : JAVA_HOME=/chemin/vers/jdk-17 bash tache2/run-chatunitest.sh"
    exit 1
  fi
fi
java -version 2>&1 | head -1

# ChatUniTest 2.1.1 n'accepte qu'une liste fermee de noms de modeles (gpt-*, code-llama,
# codeqwen:v1.5-chat). On expose donc le modele local sous le nom "codeqwen:v1.5-chat" :
#     ollama pull qwen2.5-coder:7b && ollama cp qwen2.5-coder:7b codeqwen:v1.5-chat
MODEL="${MODEL:-codeqwen:v1.5-chat}"
URL="${URL:-http://localhost:11434/v1/chat/completions}"
CLASSES=("$@")
if [ ${#CLASSES[@]} -eq 0 ]; then
  CLASSES=(ProcessUtils CharsetUtils)
fi

echo ">> Verification d'Ollama ($URL) avec le modele $MODEL (le premier appel charge le modele : jusqu'a 1-2 min)"
CURL=curl
command -v curl.exe >/dev/null 2>&1 && CURL=curl.exe   # sous Git Bash, preferer le curl de Windows
set +e
RESP=$("$CURL" -sS --max-time 300 -X POST "$URL" -H 'Content-Type: application/json' \
     -d "{\"model\":\"$MODEL\",\"messages\":[{\"role\":\"user\",\"content\":\"ping\"}],\"max_tokens\":1}" 2>&1)
CURL_STATUS=$?
set -e
if [ $CURL_STATUS -ne 0 ] || ! echo "$RESP" | grep -q '"choices"'; then
  echo "!! Ollama ne repond pas correctement sur $URL (code curl $CURL_STATUS). Reponse :"
  echo "$RESP" | head -c 600; echo
  echo "   -> Ollama est-il lance ? Le modele est-il telecharge ? (ollama list / ollama pull $MODEL)"
  exit 1
fi
echo ">> Ollama OK"

echo ">> Installation locale de tika-parent et tika-core (sans tests) - ChatUniTest en a besoin"
"$MVN" -B -ntp -q -f "$ROOT/pom.xml" -pl tika-core -am install -DskipTests -Drat.skip=true

for CLASS in "${CLASSES[@]}"; do
  echo ">> Generation des tests pour org.apache.tika.utils.$CLASS"
  "$MVN" -B -ntp -f "$ROOT/tika-core/pom.xml" -Pchatunitest -Drat.skip=true \
      -Dchatunitest.model="$MODEL" -Dchatunitest.url="$URL" \
      io.github.zju-aces-ise:chatunitest-maven-plugin:2.1.1:class \
      -DselectClass="org.apache.tika.utils.$CLASS" \
      2>&1 | tee "$ROOT/tache2/chatunitest-$CLASS.log"

  # ChatUniTest ecrit chaque test valide dans tika-core/chatunitest/org/apache/tika/utils/
  # sous la forme <Classe>_<methode>_<i>_<j>_Test.java, plus une "suite" <Classe>_Suite.java
  # (format JUnit4 @RunWith, inutilisable ici). On copie les tests individuels, tels quels,
  # dans le dossier de test du module : c'est ce que Surefire et PIT executent.
  OUT_DIR="$ROOT/tika-core/chatunitest/org/apache/tika/utils"
  DEST="$ROOT/tika-core/src/test/java/org/apache/tika/utils"
  N=0
  for f in "$OUT_DIR"/${CLASS}_*_Test.java; do
    [ -e "$f" ] || continue
    cp "$f" "$DEST/"
    N=$((N+1))
  done
  if [ "$N" -eq 0 ]; then
    echo "!! Aucun test valide genere pour $CLASS (tous les essais ont echoue) ; voir tache2/chatunitest-$CLASS.log"
  else
    echo ">> $N fichier(s) de test generes copies dans $DEST"
  fi
done

echo ">> Termine. Etapes suivantes :"
echo "   1. $MVN -B -f tika-core/pom.xml test -Dtest='*_Test' -Drat.skip=true   # verifier compilation/execution"
echo "   2. git add -A && git add -f tika-core/target/chatunitest-info"
echo "   3. git commit && git push   # le workflow GitHub relance JaCoCo + PIT"
