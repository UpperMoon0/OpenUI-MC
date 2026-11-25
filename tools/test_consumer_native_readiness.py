"""Bounded native client/server live-join readiness on general Linux CI."""
from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import run_consumer_live_join as join


class NativeJoinReadinessTests(unittest.TestCase):
    def test_default_budget_preserves_real_five_target_join(self):
        self.assertEqual(join.TARGETS, {
            "fabric-1.20.1", "forge-1.20.1", "fabric-1.21.1",
            "neoforge-1.21.1", "neoforge-26.1.2"})
        self.assertGreaterEqual(join.NATIVE_READY_TIMEOUT, 600)
        self.assertLessEqual(join.NATIVE_READY_TIMEOUT, join.MAX_READY_TIMEOUT)
        self.assertEqual(join.CONSUMER_REF,
                         "9a9681671e783bfd6e80594c3e23a76fb7132efb")

    def test_native_timeout_reaches_pinned_real_join_without_skipping(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder) / "fixture"
            argv = ["run_consumer_live_join.py", "--target", "fabric-1.20.1",
                    "--candidate-version", "0.0.16", "--work-dir", str(root)]
            with patch.object(sys, "argv", argv), \
                 patch.object(join, "clone_consumer") as clone, \
                 patch.object(join, "set_candidate_version") as version, \
                 patch.object(join, "isolate_consumer_target") as isolate, \
                 patch.object(join, "bound_fabric_server_heap"), \
                 patch.object(join, "install_cc_api_shim") as shim, \
                 patch.object(join, "consumer_environment", return_value={"CI": "true"}), \
                 patch.object(join, "run") as run:
                self.assertEqual(join.main(), 0)
            clone.assert_called_once_with(root / "simply-speakers")
            version.assert_called_once_with(root / "simply-speakers", "0.0.16")
            isolate.assert_called_once_with(root / "simply-speakers", "fabric-1.20.1")
            shim.assert_called_once_with(root / "simply-speakers", root, "fabric-1.20.1")
            args = run.call_args.args[0]
            self.assertEqual(args[-4:], ["--target", "fabric-1.20.1",
                                         "--timeout", str(join.NATIVE_READY_TIMEOUT)])
            self.assertEqual(run.call_args.kwargs["cwd"], root / "simply-speakers")

    def test_both_fabric_server_remappers_have_explicit_heap(self):
        marker = '            runDir("run/live-join/server")\n            programArg "nogui"\n'
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            for target in ("fabric-1.20.1", "fabric-1.21.1"):
                with self.subTest(target=target):
                    script = root / target / "build.gradle"
                    script.parent.mkdir()
                    script.write_text(marker, encoding="utf-8")
                    join.bound_fabric_server_heap(root, target)
                    self.assertEqual(script.read_text(), marker + '            vmArg "-Xmx1536m"\n')
                    script.write_text("unrecognized fixture", encoding="utf-8")
                    with self.assertRaises(RuntimeError):
                        join.bound_fabric_server_heap(root, target)

    def test_pinned_fixture_runtime_budget_and_serial_preflight(self):
        fixture = (
            'def command(root: Path, task: str) -> list[str]:\n'
            '        "--max-workers=4",\n        "-Dorg.gradle.jvmargs=-Xmx2048m",\n'
            '    subprocess.run(compile_cmd, cwd=root, check=True)\n'
            '        "spawn-protection=0\\n",\n'
            '        "skipMultiplayerWarning:true\\n",\n'
            'def run_target(root: Path, target: str, timeout: int) -> None:\n'
        )
        adapted = join.adapt_pinned_harness(fixture)
        self.assertIn('"1024m" if ":runLiveJoinTest" in task', adapted)
        self.assertIn('else "2048m"', adapted)
        self.assertIn("preparing=True", adapted)
        self.assertIn("task.enabled = false", adapted)
        self.assertIn("runLiveJoinTestServer', 'runLiveJoinTestClient", adapted)
        self.assertIn("view-distance=3", adapted)
        self.assertIn("NATIVE_PREPARE_TIMEOUT = 2400\n\ndef prepare_graph", adapted)
        self.assertIn("process.wait(timeout=NATIVE_PREPARE_TIMEOUT)", adapted)
        self.assertIn("stop_tree(process)", adapted)
        self.assertNotIn("check=True, timeout=timeout", adapted)
        with self.assertRaises(RuntimeError):
            join.adapt_pinned_harness(fixture.replace("--max-workers=4", "--max-workers=8"))

    def test_prepare_timeout_stops_only_its_fixture_process(self):
        import inspect
        import subprocess
        from unittest.mock import Mock
        process = Mock()
        process.wait.side_effect = subprocess.TimeoutExpired(["gradlew"], 2400)
        stop = Mock()
        ns = {"Path": Path, "subprocess": subprocess, "NATIVE_PREPARE_TIMEOUT": 2400,
              "popen": Mock(return_value=process), "OutputPump": Mock(), "stop_tree": stop}
        exec(inspect.getsource(join.prepare_graph), ns)
        with self.assertRaisesRegex(RuntimeError, "preparation downloadAssets exceeded 2400s"):
            ns["prepare_graph"](["gradlew"], Path("."), "downloadAssets")
        process.wait.assert_called_once_with(timeout=2400)
        stop.assert_called_once_with(process)

    def test_explicit_bounded_timeout_and_rejection(self):
        with tempfile.TemporaryDirectory() as folder:
            prefix = ["run_consumer_live_join.py", "--target", "fabric-1.20.1",
                      "--candidate-version", "0.0.16", "--work-dir", folder]
            for seconds in (119, 1201, 0, -1):
                with self.subTest(timeout=seconds),patch.object(
                        sys, "argv", prefix + ["--timeout", str(seconds)]):
                    with self.assertRaises(SystemExit) as raised:
                        join.main()
                    self.assertEqual(raised.exception.code, 2)
            with patch.object(sys, "argv", prefix + ["--timeout", "1200"]), \
                 patch.object(join, "clone_consumer"), \
                 patch.object(join, "set_candidate_version"), \
                 patch.object(join, "isolate_consumer_target"), \
                 patch.object(join, "bound_fabric_server_heap"), \
                 patch.object(join, "install_cc_api_shim"), \
                 patch.object(join, "consumer_environment", return_value={}), \
                 patch.object(join, "run") as launch:
                self.assertEqual(join.main(), 0)
            self.assertEqual(launch.call_args.args[0][-2:], ["--timeout", "1200"])


if __name__ == "__main__":
    unittest.main()
