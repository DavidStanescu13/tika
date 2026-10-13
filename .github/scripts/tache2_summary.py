#!/usr/bin/env python3
"""Resume des rapports de la tache 2 (JaCoCo + PIT) en Markdown.

Usage: tache2_summary.py <dossier reports>
Lit :  reports/jacoco-original/jacoco.csv, reports/jacoco-new/jacoco.csv,
       reports/pit-original/mutations.csv, reports/pit-new/mutations.csv
"""
import csv
import os
import sys
from collections import Counter, defaultdict

CLASSES = ["ProcessUtils", "CharsetUtils"]
PKG = "org.apache.tika.utils"


def jacoco_rows(path):
    if not os.path.exists(path):
        return {}
    out = {}
    with open(path, newline="") as f:
        for r in csv.DictReader(f):
            if r["PACKAGE"] == PKG and r["CLASS"] in CLASSES:
                out[r["CLASS"]] = r
    return out


def pct(cov, missed):
    tot = cov + missed
    return "n/a" if tot == 0 else f"{100.0 * cov / tot:.1f}% ({cov}/{tot})"


def jacoco_table(title, path):
    rows = jacoco_rows(path)
    lines = [f"### {title}", "", "| Classe | Instructions | Branches | Lignes | Methodes |", "|---|---|---|---|---|"]
    for c in CLASSES:
        r = rows.get(c)
        if not r:
            lines.append(f"| {c} | (absent) | | | |")
            continue
        g = lambda k: int(r[k])
        lines.append("| %s | %s | %s | %s | %s |" % (
            c,
            pct(g("INSTRUCTION_COVERED"), g("INSTRUCTION_MISSED")),
            pct(g("BRANCH_COVERED"), g("BRANCH_MISSED")),
            pct(g("LINE_COVERED"), g("LINE_MISSED")),
            pct(g("METHOD_COVERED"), g("METHOD_MISSED")),
        ))
    lines.append("")
    return lines


def pit_rows(path):
    if not os.path.exists(path):
        return []
    with open(path, newline="") as f:
        return [r for r in csv.reader(f) if len(r) >= 6]


def pit_table(title, path):
    rows = pit_rows(path)
    lines = [f"### {title}", ""]
    if not rows:
        lines.append("(rapport absent)")
        lines.append("")
        return lines
    by_class = defaultdict(Counter)
    for r in rows:
        cls = r[1].split(".")[-1]
        by_class[cls][r[5]] += 1
    lines += ["| Classe | Mutants | Tues | Survivants | Sans couverture | Timeout / autres | Score |", "|---|---|---|---|---|---|---|"]
    for cls in CLASSES:
        c = by_class.get(cls, Counter())
        tot = sum(c.values())
        killed = c["KILLED"] + c["TIMED_OUT"] + c["MEMORY_ERROR"] + c["RUN_ERROR"]
        surv = c["SURVIVED"]
        nocov = c["NO_COVERAGE"]
        other = tot - c["KILLED"] - surv - nocov
        score = "n/a" if tot == 0 else f"{100.0 * killed / tot:.1f}%"
        lines.append(f"| {cls} | {tot} | {c['KILLED']} | {surv} | {nocov} | {other} | {score} |")
    lines.append("")
    lines.append("Score = (KILLED + TIMED_OUT + MEMORY_ERROR + RUN_ERROR) / total, comme PIT.")
    lines.append("")
    # Detail des mutants non tues
    lines.append("<details><summary>Mutants non detectes (SURVIVED / NO_COVERAGE)</summary>")
    lines.append("")
    lines.append("| Classe | Ligne | Methode | Mutateur | Statut |")
    lines.append("|---|---|---|---|---|")
    for r in sorted(rows, key=lambda r: (r[1], int(r[4]) if r[4].isdigit() else 0)):
        if r[5] in ("SURVIVED", "NO_COVERAGE"):
            lines.append(f"| {r[1].split('.')[-1]} | {r[4]} | {r[3]} | {r[2].split('.')[-1]} | {r[5]} |")
    lines.append("")
    lines.append("</details>")
    lines.append("")
    return lines


def main():
    base = sys.argv[1] if len(sys.argv) > 1 else "reports"
    out = ["## IFT3913 Tache 2 - resume", ""]
    out += jacoco_table("Couverture JaCoCo - tests originaux", os.path.join(base, "jacoco-original", "jacoco.csv"))
    out += jacoco_table("Couverture JaCoCo - nouveaux tests seuls (ChatUniTest + manuels)", os.path.join(base, "jacoco-new", "jacoco.csv"))
    out += pit_table("PIT - tests originaux", os.path.join(base, "pit-original", "mutations.csv"))
    out += pit_table("PIT - tests originaux + generes + manuels", os.path.join(base, "pit-new", "mutations.csv"))
    print("\n".join(out))


if __name__ == "__main__":
    main()
