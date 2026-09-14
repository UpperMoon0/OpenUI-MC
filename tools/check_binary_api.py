#!/usr/bin/env python3
"""Fail CI when a released OpenUI public JVM class or subclass-visible member ABI changes."""

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
    parser.add_argument("--baseline-url", required=True, action="append", dest="baseline_urls")
    parser.add_argument("--candidate", required=True, type=pathlib.Path)
    parser.add_argument("--report", required=True, type=pathlib.Path)
    return parser.parse_args()


def download(url: str, destination: pathlib.Path) -> None:
    request = urllib.request.Request(url, headers={"User-Agent": "OpenUI-binary-compat-check"})
    with urllib.request.urlopen(request, timeout=60) as response, destination.open("wb") as output:
        shutil.copyfileobj(response, output)


def is_generated_class(class_name: str) -> bool:
    # Architectury injects implementation bridge classes whose names contain
    # build hashes and source-jar coordinates. They are not source-addressable
    # consumer APIs and legitimately change between otherwise ABI-compatible jars.
    return class_name.startswith("architectury_inject_")


def class_names(jar: pathlib.Path) -> list[str]:
    with zipfile.ZipFile(jar) as archive:
        result = []
        for name in archive.namelist():
            if not name.endswith(".class") or name.startswith("META-INF/versions/"):
                continue
            class_name = name[:-6].replace("/", ".")
            if class_name in {"module-info", "package-info"} or class_name.endswith(".package-info"):
                continue
            if is_generated_class(class_name):
                continue
            result.append(class_name)
        return sorted(set(result))


def parse_class_api(output: str) -> tuple[bool, str | None, set[tuple[str, str]]]:
    lines = output.splitlines()
    declaration = next(
        (
            line.strip()
            for line in lines
            if line.startswith("public ")
            and (" class " in line or " interface " in line or " enum " in line or " record " in line)
        ),
        None,
    )
    if declaration is None:
        return False, None, set()

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
    return True, declaration, members


def class_api(
    javap: str, jar: pathlib.Path, class_name: str
) -> tuple[bool, str | None, set[tuple[str, str]]]:
    process = subprocess.run(
        [javap, "-classpath", str(jar), "-protected", "-s", "-constants", class_name],
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if process.returncode != 0:
        raise RuntimeError(f"javap failed for {class_name}: {process.stderr.strip()}")
    return parse_class_api(process.stdout)


def compatible_class_declaration(baseline: str | None, candidate: str | None) -> bool:
    """Adding implemented interfaces preserves existing class linkage; removals do not."""
    if baseline == candidate:
        return True
    if baseline is None or candidate is None or " class " not in baseline:
        return False

    def parts(declaration: str) -> tuple[str, set[str]]:
        head, separator, tail = declaration.removesuffix(" {").partition(" implements ")
        interfaces: set[str] = set()
        depth, start = 0, 0
        if separator:
            for index, char in enumerate(tail):
                if char == "<": depth += 1
                elif char == ">": depth -= 1
                elif char == "," and depth == 0:
                    interfaces.add(tail[start:index].strip())
                    start = index + 1
            interfaces.add(tail[start:].strip())
        return head, interfaces

    old_head, old_interfaces = parts(baseline)
    new_head, new_interfaces = parts(candidate)
    return old_head == new_head and old_interfaces <= new_interfaces


def check_baseline(javap: str, baseline: pathlib.Path, candidate: pathlib.Path) -> tuple[int, int, list[str]]:
    candidate_classes = set(class_names(candidate))
    checked_classes = 0
    checked_members = 0
    failures: list[str] = []

    for class_name in class_names(baseline):
        baseline_public, baseline_declaration, baseline_members = class_api(javap, baseline, class_name)
        if not baseline_public:
            continue
        checked_classes += 1
        checked_members += len(baseline_members)
        if class_name not in candidate_classes:
            failures.append(f"REMOVED CLASS: {class_name}")
            continue
        candidate_public, candidate_declaration, candidate_members = class_api(javap, candidate, class_name)
        if not candidate_public:
            failures.append(f"NO LONGER PUBLIC: {class_name}")
            continue
        if not compatible_class_declaration(baseline_declaration, candidate_declaration):
            failures.append(
                f"CLASS SIGNATURE CHANGED: {class_name} :: {baseline_declaration} -> {candidate_declaration}"
            )
        for declaration, descriptor in sorted(baseline_members - candidate_members):
            failures.append(f"REMOVED/CHANGED: {class_name} :: {declaration} [{descriptor}]")

    return checked_classes, checked_members, failures


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
    baseline_results: list[tuple[str, int, int, list[str]]] = []
    all_failures: list[str] = []
    with tempfile.TemporaryDirectory(prefix="openui-api-") as temp_dir:
        temp_root = pathlib.Path(temp_dir)
        for index, baseline_url in enumerate(args.baseline_urls):
            baseline = temp_root / f"baseline-{index}.jar"
            download(baseline_url, baseline)
            checked_classes, checked_members, failures = check_baseline(javap, baseline, args.candidate)
            labelled_failures = [f"[{baseline_url}] {failure}" for failure in failures]
            all_failures.extend(labelled_failures)
            baseline_results.append((baseline_url, checked_classes, checked_members, labelled_failures))

    status = "PASS" if not all_failures else "FAIL"
    lines = [
        f"OpenUI binary API compatibility: {status}",
        f"Candidate: {args.candidate}",
        "Baselines:",
    ]
    for baseline_url, checked_classes, checked_members, failures in baseline_results:
        baseline_status = "PASS" if not failures else "FAIL"
        lines.append(
            f"- {baseline_status}: {baseline_url} :: {checked_classes} public class signatures / "
            f"{checked_members} public/protected members"
        )
    if all_failures:
        lines.extend(["", "Incompatible changes:", *[f"- {failure}" for failure in all_failures]])
    else:
        lines.extend(["", "No released public class-signature or member ABI regressions detected."])

    report = "\n".join(lines) + "\n"
    args.report.write_text(report, encoding="utf-8")
    print(report, end="")

    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as summary:
            summary.write("\n### OpenUI binary API compatibility\n\n")
            summary.write(f"**{status}** against {len(baseline_results)} released baseline(s).\n\n")
            for baseline_url, checked_classes, checked_members, failures in baseline_results:
                baseline_status = "PASS" if not failures else "FAIL"
                summary.write(
                    f"- **{baseline_status}** `{baseline_url}` ? {checked_classes} public class signatures / "
                    f"{checked_members} public/protected members.\n"
                )
            if all_failures:
                summary.write("\n```text\n")
                summary.write("\n".join(all_failures[:100]))
                summary.write("\n```\n")

    return 1 if all_failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
