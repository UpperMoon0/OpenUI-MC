#!/usr/bin/env python3
"""Fail CI when a released OpenUI public JVM symbol disappears or changes descriptor."""

from __future__ import annotations

import argparse
import os
import pathlib
import shutil
import subprocess
import sys
import tempfile
import urllib.request
import zipfile


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--baseline-url", required=True)
    parser.add_argument("--candidate", required=True, type=pathlib.Path)
    parser.add_argument("--report", required=True, type=pathlib.Path)
    return parser.parse_args()


def download(url: str, destination: pathlib.Path) -> None:
    request = urllib.request.Request(url, headers={"User-Agent": "OpenUI-binary-compat-check"})
    with urllib.request.urlopen(request, timeout=60) as response, destination.open("wb") as output:
        shutil.copyfileobj(response, output)


def class_names(jar: pathlib.Path) -> list[str]:
    with zipfile.ZipFile(jar) as archive:
        result = []
        for name in archive.namelist():
            if not name.endswith(".class") or name.startswith("META-INF/versions/"):
                continue
            class_name = name[:-6].replace("/", ".")
            if class_name in {"module-info", "package-info"} or class_name.endswith(".package-info"):
                continue
            result.append(class_name)
        return sorted(set(result))


def public_members(javap: str, jar: pathlib.Path, class_name: str) -> tuple[bool, set[tuple[str, str]]]:
    process = subprocess.run(
        [javap, "-classpath", str(jar), "-public", "-s", "-constants", class_name],
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if process.returncode != 0:
        raise RuntimeError(f"javap failed for {class_name}: {process.stderr.strip()}")

    lines = process.stdout.splitlines()
    is_public = any(
        line.startswith("public ") and (" class " in line or " interface " in line or " enum " in line or " record " in line)
        for line in lines
    )
    if not is_public:
        return False, set()

    members: set[tuple[str, str]] = set()
    pending: str | None = None
    for line in lines:
        stripped = line.strip()
        if line.startswith("  ") and not line.startswith("    ") and (
            stripped.startswith("public ") or stripped.startswith("protected ")
        ):
            pending = stripped
            continue
        if pending is not None and stripped.startswith("descriptor:"):
            members.add((pending, stripped.removeprefix("descriptor:").strip()))
            pending = None
    return True, members


def main() -> int:
    args = parse_args()
    javap = shutil.which("javap")
    if javap is None:
        print("javap is required for binary API compatibility checking", file=sys.stderr)
        return 2
    if not args.candidate.is_file():
        print(f"candidate jar does not exist: {args.candidate}", file=sys.stderr)
        return 2

    args.report.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="openui-api-") as temp_dir:
        baseline = pathlib.Path(temp_dir) / "baseline.jar"
        download(args.baseline_url, baseline)

        candidate_classes = set(class_names(args.candidate))
        checked_classes = 0
        checked_members = 0
        failures: list[str] = []

        for class_name in class_names(baseline):
            baseline_public, baseline_members = public_members(javap, baseline, class_name)
            if not baseline_public:
                continue
            checked_classes += 1
            checked_members += len(baseline_members)
            if class_name not in candidate_classes:
                failures.append(f"REMOVED CLASS: {class_name}")
                continue
            candidate_public, candidate_members = public_members(javap, args.candidate, class_name)
            if not candidate_public:
                failures.append(f"NO LONGER PUBLIC: {class_name}")
                continue
            for declaration, descriptor in sorted(baseline_members - candidate_members):
                failures.append(f"REMOVED/CHANGED: {class_name} :: {declaration} [{descriptor}]")

    status = "PASS" if not failures else "FAIL"
    lines = [
        f"OpenUI binary API compatibility: {status}",
        f"Baseline: {args.baseline_url}",
        f"Candidate: {args.candidate}",
        f"Public classes checked: {checked_classes}",
        f"Public members checked: {checked_members}",
    ]
    if failures:
        lines.extend(["", "Incompatible changes:", *[f"- {failure}" for failure in failures]])
    else:
        lines.extend(["", "No released public class/member removals or descriptor changes detected."])

    report = "\n".join(lines) + "\n"
    args.report.write_text(report, encoding="utf-8")
    print(report, end="")

    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as summary:
            summary.write("\n### OpenUI binary API compatibility\n\n")
            summary.write(f"**{status}** — {checked_classes} released public classes / {checked_members} public members checked against v0.0.7.\n")
            if failures:
                summary.write("\n```text\n")
                summary.write("\n".join(failures[:100]))
                summary.write("\n```\n")

    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
