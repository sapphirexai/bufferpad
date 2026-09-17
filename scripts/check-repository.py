"""Check tracked documentation links and public repository file boundaries.

Uses only the Python standard library. This checks working files listed in the Git index, not
all historical commits, external URLs, Markdown anchors, or image contents.
"""
import json
import re
import subprocess
from pathlib import Path
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[1]


def check_documentation(root, files):
    """Check complete language coverage, reciprocal links, and English navigation."""
    problems = []
    manifest = json.loads((root / "docs/languages.json").read_text(encoding="utf-8"))
    pairs = manifest["pairs"]
    covered = [p[language] for p in pairs for language in ("zh", "en")]
    covered += manifest["bilingual"]
    markdown = {name for name in files if name.endswith(".md")}
    if len(covered) != len(set(covered)):
        problems.append("Duplicate document in language manifest")
    for name in sorted(markdown - set(covered)):
        problems.append(f"Markdown document has no language pair: {name}")
    for name in sorted(set(covered) - markdown):
        problems.append(f"Language manifest references untracked document: {name}")
    chinese = {p["zh"] for p in pairs}

    def targets(name, content):
        result = []
        for url in re.findall(r"\]\(([^)]+)\)", content):
            if re.match(r"(?:[A-Za-z][A-Za-z0-9+.-]*:|#|//)", url):
                continue
            target = unquote(url.split("#")[0]).strip("<>")
            resolved = ((root / name).parent / target).resolve()
            try:
                result.append(resolved.relative_to(root.resolve()).as_posix())
            except ValueError:
                problems.append(f"Documentation link escapes repository: {name}")
        return result

    for language in ("zh", "en"):
        index = "docs/README.md" if language == "zh" else "docs/README.en.md"
        index_path = root / index
        index_links = targets(index, index_path.read_text(encoding="utf-8")) if index_path.exists() else []
        for pair in pairs:
            name = pair[language]
            path = root / name
            if not path.is_file():
                continue  # Missing/untracked documents are reported above/by main.
            counterpart = pair["en" if language == "zh" else "zh"]
            content = path.read_text(encoding="utf-8")
            top = "\n".join(content.splitlines()[:5])
            if counterpart not in targets(name, top):
                problems.append(f"Missing reciprocal language link: {name}")
            if index not in targets(name, top):
                problems.append(f"Missing documentation index link: {name}")
            if name != index and name not in index_links:
                problems.append(f"Document absent from {index}: {name}")
            if language == "en":
                # Only the language switch may lead to Chinese Markdown.
                body = "\n".join(content.splitlines()[4:])
                for target in targets(name, body):
                    if target in chinese:
                        problems.append(f"English body links to Chinese document: {name} -> {target}")
    return problems


def main():
    files = subprocess.check_output(
        ["git", "ls-files", "-z"], cwd=ROOT
    ).decode("utf-8").split("\0")
    problems = check_documentation(ROOT, list(filter(None, files)))
    docs = 0
    for name in filter(None, files):
        path = ROOT / name
        parts = Path(name).parts
        lower = name.lower()
        generated = name.startswith((
            "bufferpad-installer/packages/", "bufferpad-installer/app/backend/",
            "bufferpad-installer/app/frontend/",
        )) and path.name not in ("README.md", "README.en.md", ".gitkeep")
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
          "Language pairs/navigation, local link targets, artifact boundaries, "
          "and selected credential markers checked.")


if __name__ == "__main__":
    main()
