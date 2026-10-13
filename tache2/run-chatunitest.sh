#!/usr/bin/env bash
# IFT3913 - Tache 2 : generation de tests avec ChatUniTest + Ollama (modele local)
#
# Prerequis :
#   - Java 17+ et Maven 3.9+
#   - Ollama installe et lance (https://ollama.com), avec un modele telecharge :
#         ollama pull qwen2.5-coder:7b        (ou qwen2.5-coder:1.5b sur une machine modeste)
#
# Usage (depuis la racine du depot) :
#   bash tache2/run-chatunitest.sh                       # ProcessUtils et CharsetUtils
#   bash tache2/run-chatunitest.sh ProcessUtils          # une seule classe
#   MODEL=qwen2.5-coder:1.5b bash tache2/run-chatunitest.sh
#
# Resultat : les tests generes sont dans tika-core/chatunitest/ (bruts) ; la suite fusionnee
# de chaque classe est copiee dans tika-core/src/test/java/org/apache/tika/utils/<Classe>ChatUniTest.java
# (c'est ce fichier que le workflow GitHub execute et que PIT utilise).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODEL="${MODEL:-qwen2.5-coder:7b}"
URL="${URL:-http://localhost:11434/v1/chat/completions}"
CLASSES=("$@")
if [ ${#CLASSES[@]} -eq 0 ]; then
  CLASSES=(ProcessUtils CharsetUtils)
fi

echo ">> Verification d'Ollama ($URL) avec le modele $MODEL"
if ! curl -sf -X POST "$URL" -H 'Content-Type: application/json' \
     -d "{\"model\":\"$MODEL\",\"messages\":[{\"role\":\"user\",\"content\":\"ping\"}],\"max_tokens\":1}" >/dev/null; then
  echo "!! Ollama ne repond pas sur $URL ou le modele $MODEL n'est pas disponible (ollama pull $MODEL)"
  exit 1
fi

echo ">> Installation locale de tika-parent et tika-core (sans tests) - ChatUniTest en a besoin"
mvn -B -ntp -q -f "$ROOT/pom.xml" -pl tika-core -am install -DskipTests -Drat.skip=true

for CLASS in "${CLASSES[@]}"; do
  echo ">> Generation des tests pour org.apache.tika.utils.$CLASS"
  mvn -B -ntp -f "$ROOT/tika-core/pom.xml" -Pchatunitest -Drat.skip=true \
      -Dchatunitest.model="$MODEL" -Dchatunitest.url="$URL" \
      io.github.zju-aces-ise:chatunitest-maven-plugin:2.1.1:class \
      -DselectClass="org.apache.tika.utils.$CLASS" \
      2>&1 | tee "$ROOT/tache2/chatunitest-$CLASS.log"

  # Suite fusionnee produite par le plugin (merge=true) -> fichier suivi par git
  OUT_DIR="$ROOT/tika-core/chatunitest"
  MERGED=$(find "$OUT_DIR" -name "${CLASS}_*Test.java" -o -name "${CLASS}Test.java" 2>/dev/null | head -1 || true)
  if [ -z "$MERGED" ]; then
    echo "!! Aucune suite fusionnee trouvee dans $OUT_DIR pour $CLASS ; voir tache2/chatunitest-$CLASS.log"
    continue
  fi
  TARGET="$ROOT/tika-core/src/test/java/org/apache/tika/utils/${CLASS}ChatUniTest.java"
  ORIG_NAME=$(basename "$MERGED" .java)
  sed "s/\b${ORIG_NAME}\b/${CLASS}ChatUniTest/g" "$MERGED" > "$TARGET"
  echo ">> Suite copiee : $TARGET (classe renommee ${ORIG_NAME} -> ${CLASS}ChatUniTest)"
done

echo ">> Termine. Etapes suivantes :"
echo "   1. mvn -B -f tika-core/pom.xml test -Dtest='*ChatUniTest' -Drat.skip=true   # verifier compilation/execution"
echo "   2. git add tika-core/src/test/java/org/apache/tika/utils/*ChatUniTest.java tika-core/chatunitest tache2/*.log"
echo "   3. git commit && git push   # le workflow GitHub relance JaCoCo + PIT"
