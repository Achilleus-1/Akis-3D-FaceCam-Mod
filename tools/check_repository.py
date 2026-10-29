"""Check publishable files, metadata, documentation links, and source hygiene."""
import ast
import json
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

from project import ROOT, VERSION, JAR_NAME

PRIVATE_PATH = re.compile(r'(?:[A-Za-z]:[\\/]+Users[\\/]+|/Us' r'ers/|/ho' r'me/)[^\s"<>]+')
TOKEN = re.compile(r'(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{30,}|AKIA[0-9A-Z]{16}|sk-[A-Za-z0-9_-]{32,}|xox[baprs]-[A-Za-z0-9-]{20,})')
TEXT = {'.py','.java','.md','.txt','.xml','.json','.yml','.yaml','.toml','.properties','.gradle','.ps1'}


def main():
    result = subprocess.run(['git','ls-files','-z','--cached','--others','--exclude-standard'],
                            cwd=ROOT,capture_output=True,check=True)
    paths = sorted({Path(p.decode()) for p in result.stdout.split(b'\0') if p})
    failures=[]
    for relative in paths:
        path=ROOT/relative
        if not path.is_file():
            continue
        if any(part in {'.env','.vscode','.idea','.gradle','.gradle-user','dist','build','__pycache__'} or part.startswith('run-') for part in relative.parts):
            failures.append(str(relative)+': generated/private file must not be tracked')
        if path.stat().st_size > 90*1024*1024:
            failures.append(str(relative)+': use release assets for large files')
        if path.suffix not in TEXT:
            continue
        try:
            text=path.read_text(encoding='utf-8')
            if PRIVATE_PATH.search(text) or TOKEN.search(text):
                failures.append(str(relative)+': potential private path or credential; value withheld')
            if path.suffix=='.py': ast.parse(text,filename=str(relative))
            if path.suffix=='.json': json.loads(text)
            if path.suffix=='.xml': ET.fromstring(text)
            if path.suffix=='.md':
                links=re.findall(r'\]\(([^)]+)\)|src="([^"]+)"',text)
                for pair in links:
                    target=next(t for t in pair if t).split('#')[0]
                    if not target or '://' in target or target.startswith('mailto:'):continue
                    if not (path.parent/target).exists():
                        failures.append(str(relative)+': missing local link '+target)
        except (UnicodeError,SyntaxError,ValueError,ET.ParseError) as error:
            failures.append(str(relative)+': '+type(error).__name__)
    for name in ['README.md','docs/GUIDE.md','DEVELOPMENT.md','docs/RELEASE_NOTES.md']:
        if JAR_NAME not in (ROOT/name).read_text(encoding='utf-8'):
            failures.append(name+': release filename does not match gradle.properties')
    if not VERSION.endswith('-beta.1'):
        failures.append('Expected the prepared beta version')
    if failures:
        print('\n'.join(failures),file=sys.stderr)
        return 1
    print('Repository checks passed for '+str(len(paths))+' source/configuration files.')
    return 0


if __name__=='__main__':
    raise SystemExit(main())
