"""Shared release metadata; no machine-specific paths or third-party modules."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PROPERTIES = dict(line.strip().split('=', 1) for line in
                  (ROOT / 'gradle.properties').read_text(encoding='utf-8').splitlines()
                  if '=' in line and not line.lstrip().startswith('#'))
VERSION = PROPERTIES['mod_version']
STEM = PROPERTIES['archives_base_name'] + '-' + VERSION
JAR_NAME = STEM + '.jar'
SOURCE_NAME = STEM + '-source-project.zip'
