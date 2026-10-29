"""Verify the installable jar in a dedicated dev server without FaceCam loose classes or Maven natives on the classpath."""
import pathlib, subprocess, shutil, os, json, ctypes, sys
from project import ROOT as root, JAR_NAME
role=sys.argv[1] if len(sys.argv)>1 else 'server'
if role not in ['server','sender','viewer']:raise ValueError('Role must be server, sender or viewer')
run_name='server' if role=='server' else 'verify'+role.title()
game=root/('run-packaged-'+role);(game/'mods').mkdir(parents=True,exist_ok=True)
for stale in list((game/'mods').glob('facecam-*.jar'))+list((game/'mods').glob('akis-3d-facecam-mod-*.jar')):
    if stale.name!=JAR_NAME:
        if stale.resolve().parent!=(game/'mods').resolve():raise RuntimeError('Unexpected test jar path')
        stale.unlink()
shutil.copy2(root/'build/libs'/JAR_NAME,game/'mods'/JAR_NAME)
if role=='server' and not (game/'server.properties').exists():
    (game/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25585\nonline-mode=false\ngamemode=creative\nforce-gamemode=true\nlevel-type=minecraft:flat\nview-distance=4\nsimulation-distance=4\nmax-players=4\n')
raw=json.loads((root/'build/verification/server-launch-command.json').read_text(encoding='utf-8-sig'))
argc=ctypes.c_int()
parse=ctypes.windll.shell32.CommandLineToArgvW;parse.restype=ctypes.POINTER(ctypes.c_wchar_p)
parse.argtypes=[ctypes.c_wchar_p,ctypes.POINTER(ctypes.c_int)]
argv=parse(raw,ctypes.byref(argc));arguments=[argv[i] for i in range(argc.value)]
original=arguments[arguments.index('-cp')+1]
paths=original.split(os.pathsep)
filtered=[p for p in paths if not any(t in p.replace('\\','/') for t in ['/build/classes/java/main','/build/resources/main','/org.bytedeco/','/com.microsoft.onnxruntime/'])]
for p in (root/'.gradle-user/caches/modules-2/files-2.1/net.neoforged/DevLaunch').rglob('*.jar'):
    if str(p) not in filtered:filtered.append(str(p))
legacy_original=(root/('build/moddev/'+run_name+'LegacyClasspath.txt')).read_text().strip().splitlines()
if len(legacy_original)==1:legacy_original=legacy_original[0].split(os.pathsep)
legacy_filtered=[p for p in legacy_original if not any(t in p.replace('\\','/') for t in ['/build/classes/java/main','/build/resources/main','/org.bytedeco/','/com.microsoft.onnxruntime/'])]
legacy=root/('build/verification/packaged-'+role+'-classpath.txt');legacy.write_text('\n'.join(legacy_filtered)+'\n')
vm=(root/('build/moddev/'+run_name+'RunVmArgs.txt')).read_text().splitlines()
vm=[line for line in vm if not line.startswith('-DlegacyClassPath.file=')]
vm.append('-DlegacyClassPath.file='+str(legacy).replace('\\','\\\\'))
args=root/('build/verification/packaged-'+role+'-vmargs.txt');args.write_text('\n'.join(vm)+'\n')
java=pathlib.Path(os.environ.get('JAVA_HOME','C:/Program Files/Java/jdk-21'))/'bin/java.exe'
command=[str(java),'@'+str(args),'-cp',os.pathsep.join(filtered),'net.neoforged.devlaunch.Main','@'+str(root/('build/moddev/'+run_name+'RunProgramArgs.txt'))]
print('Launching packaged jar role='+role+'; removed loose FaceCam classes and Maven native dependencies',flush=True)
raise SystemExit(subprocess.call(command,cwd=game))
