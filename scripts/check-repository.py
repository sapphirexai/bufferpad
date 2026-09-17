"""Check tracked documentation links and public repository file boundaries.

Uses only the Python standard library. This checks the current Git index, not
all historical commits, external URLs, Markdown anchors, or image contents.
"""
import re
import subprocess
from pathlib import Path
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[1]


def main():
    files = subprocess.check_output(
        ["git", "ls-files", "-z"], cwd=ROOT
    ).decode("utf-8").split("\0")
    problems = []
    docs = 0
    for name in filter(None, files):
        path = ROOT / name
        parts = Path(name).parts
        lower = name.lower()
        generated = name.startswith((
            "bufferpad-installer/packages/", "bufferpad-installer/app/backend/",
            "bufferpad-installer/app/frontend/",
        )) and path.name not in ("README.md", ".gitkeep")
        forbidden = (
            generated or lower.endswith((".pdf", ".jar", ".exe", ".zip", ".tar", ".bundle"))
            or any(p in parts for p in ("node_modules", "logs", ".idea"))
            or name.startswith(("bufferpad/dist/", "wms-opc/target/"))
            or re.search(r"\.log(?:\.|$)", path.name, re.I)
            or path.name in ("ssh_key", "ssh_key.pub", "id_rsa", "id_ed25519")
        )
        if forbidden:
            problems.append(f"Excluded artifact is tracked: {name}")
        if not path.is_file():
            problems.append(f"Tracked file is missing: {name}")
            continue
        if path.stat().st_size > 10 * 1024 * 1024:
            problems.append(f"File exceeds repository's 10 MiB review limit: {name}")
        data = path.read_bytes()
        if b"\0" not in data:
            # Report only the path, never matched credentials.
            if re.search(rb"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----", data):
                problems.append(f"Private key marker: {name}")
            if re.search(rb"\b(?:gh[pousr]_[A-Za-z0-9]{30,}|glpat-[A-Za-z0-9_-]{20,})", data):
                problems.append(f"Credential-like token: {name}")
        if path.suffix.lower() != ".md":
            continue
        docs += 1
        for match in re.finditer(r"\]\(([^)]+)\)", data.decode("utf-8")):
            url = match.group(1).strip().strip("<>")
            if re.match(r"(?:[A-Za-z][A-Za-z0-9+.-]*:|#|//)", url):
                continue
            target = unquote(url.split("#")[0])
            if target and not (path.parent / target).exists():
                problems.append(f"Broken local link: {name} -> {url}")
    if problems:
        raise SystemExit("\n".join(problems))
    print(f"PASS: {len(list(filter(None, files)))} tracked files; {docs} Markdown documents. "
          "Local link targets, artifact boundaries, and selected credential markers checked.")


if __name__ == "__main__":
    main()
