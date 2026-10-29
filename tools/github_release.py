"""Publish the prepared beta using GitHub CLI and existing Git credentials.

No credentials are written to files or printed. Pass --inspect for read-only
account/repository inspection; publishing requires the explicit --publish flag.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
from project import VERSION, JAR_NAME

ROOT = Path(__file__).resolve().parents[1]
REPO = 'Achilleus-1/Akis-3D-FaceCam-Mod'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--gh', type=Path, help='Path to gh when it is not on PATH')
    parser.add_argument('--inspect', action='store_true')
    parser.add_argument('--publish', action='store_true')
    parser.add_argument('--finalize', action='store_true', help='Publish an uploaded draft after successful CI')
    args = parser.parse_args()
    if sum([args.inspect,args.publish,args.finalize])!=1:
        parser.error('Select exactly one of --inspect, --publish, or --finalize')
    gh = str(args.gh or shutil.which('gh') or 'gh')
    env = os.environ.copy()
    env.update(GIT_TERMINAL_PROMPT='0', GCM_INTERACTIVE='never')
    if not env.get('GH_TOKEN') and not env.get('GITHUB_TOKEN'):
        result = subprocess.run(['git', 'credential', 'fill'],
                                input='protocol=https\nhost=github.com\n\n',
                                text=True, capture_output=True, env=env, check=True)
        credentials = dict(line.split('=', 1) for line in result.stdout.splitlines() if '=' in line)
        if not credentials.get('password'):
            raise SystemExit('No existing GitHub authentication; run gh auth login first.')
        env['GH_TOKEN'] = credentials['password']
        del credentials, result

    def run(*command):
        result = subprocess.run([gh, *command], cwd=ROOT, env=env,
                                text=True, capture_output=True, check=True)
        return result.stdout.strip()

    login = run('api', 'user', '--jq', '.login')
    if login != 'Achilleus-1':
        raise SystemExit('Expected repository owner Achilleus-1; authenticated as ' + login)
    if args.inspect:
        print(run('repo', 'list', login, '--limit', '100', '--json', 'name,visibility,url'))
        return
    if args.finalize:
        tag='v'+VERSION
        release=json.loads(run('release','view',tag,'--repo',REPO,'--json','isDraft,isPrerelease,assets,url'))
        runs=json.loads(run('run','list','--repo',REPO,'--branch','main','--limit','5','--json','headSha,conclusion,status'))
        head=subprocess.run(['git','rev-parse','HEAD'],cwd=ROOT,text=True,capture_output=True,check=True).stdout.strip()
        if not any(r['headSha']==head and r['conclusion']=='success' for r in runs):
            raise SystemExit('The current main commit has no successful CI run.')
        expected={JAR_NAME}
        if {a['name'] for a in release['assets']}!=expected:
            raise SystemExit('Draft release asset set is incomplete or unexpected.')
        if not release['isPrerelease']:
            raise SystemExit('The beta must be marked prerelease.')
        print(run('release','edit',tag,'--repo',REPO,'--draft=false','--prerelease','--latest=false'))
        print(run('release','view',tag,'--repo',REPO,'--json','url,isDraft,isPrerelease,assets'))
        return
    publish(run, env)


def publish(run, env):
    def git(*args):
        result=subprocess.run(['git',*args],cwd=ROOT,env=env,text=True,capture_output=True,check=True)
        return result.stdout.strip()
    if git('status','--porcelain'):
        raise SystemExit('Commit the prepared source before publishing.')
    if git('branch','--show-current')!='main':
        raise SystemExit('Publish from main.')
    with (ROOT/'dist'/JAR_NAME).open('rb') as prepared, (ROOT/'build/libs'/JAR_NAME).open('rb') as built:
        if hashlib.file_digest(prepared,'sha256').digest()!=hashlib.file_digest(built,'sha256').digest():
            raise SystemExit('Prepared release jar does not match the build output.')
    existing=json.loads(run('repo','list','Achilleus-1','--limit','100','--json','name'))
    if REPO.split('/')[1] not in {r['name'] for r in existing}:
        print(run('repo','create',REPO,'--public','--description',
                  "Aki's 3D FaceCam Mod: opt-in local RGB webcam tracking and textured 3D player faces for Minecraft 1.21.1 / NeoForge. Windows x64 beta.",
                  '--homepage','https://linktr.ee/achilleus_'))
    origin='https://github.com/'+REPO+'.git'
    if 'origin' in git('remote').splitlines():
        if git('remote','get-url','origin')!=origin:
            raise SystemExit('Existing origin does not match the intended repository.')
    else:
        git('remote','add','origin',origin)
    print(git('push','--set-upstream','origin','main'))
    print(run('repo','edit',REPO,'--default-branch','main','--enable-issues=true','--enable-wiki=false',
              '--add-topic','minecraft,neoforge,minecraft-mod,java,webcam,face-tracking,3d,beta'))
    run('api','--method','PUT','repos/'+REPO+'/private-vulnerability-reporting')
    tag='v'+VERSION
    if not git('tag','--list',tag):
        git('tag','-a',tag,'-m',"Aki's 3D FaceCam Mod "+VERSION)
    if git('rev-list','-n','1',tag)!=git('rev-parse','HEAD'):
        raise SystemExit('Existing beta tag does not match the source commit.')
    print(git('push','origin',tag))
    body=(ROOT/'docs/RELEASE_NOTES.md').read_text(encoding='utf-8').replace(
        '](../VERIFICATION.md)','](https://github.com/'+REPO+'/blob/'+tag+'/VERIFICATION.md)')
    temp=ROOT/'build/publishing/release-notes.md'
    temp.parent.mkdir(parents=True,exist_ok=True);temp.write_text(body,encoding='utf-8')
    assets=[str(ROOT/'dist'/JAR_NAME)]
    releases=json.loads(run('release','list','--repo',REPO,'--json','tagName,isDraft'))
    if not any(r['tagName']==tag for r in releases):
        print(run('release','create',tag,*assets,'--repo',REPO,'--verify-tag','--draft','--prerelease',
                  '--title',"Aki's 3D FaceCam Mod "+VERSION,'--notes-file',str(temp)))
    else:
        release=json.loads(run('release','view',tag,'--repo',REPO,'--json','isDraft'))
        if not release['isDraft']:
            raise SystemExit('The release is already public; do not replace public assets.')
        print(run('release','upload',tag,*assets,'--repo',REPO,'--clobber'))
    print('Source pushed and beta draft uploaded. Verify CI, then run --finalize.')


if __name__ == '__main__':
    main()
