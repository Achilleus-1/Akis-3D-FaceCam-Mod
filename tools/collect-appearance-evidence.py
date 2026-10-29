import pathlib,shutil
root=pathlib.Path(__file__).resolve().parents[1]
out=root/'docs/verification/appearance-1.1';out.mkdir(parents=True,exist_ok=True)
for name in ['appearance-settings','depth-175-hat-on','size-400','size-10','size-reset-100','appearance-opaque-cover','appearance-helmet-cover']:
    shutil.copy2(root/('run-packaged-viewer/screenshots/facecam-'+name+'.png'),out/(name+'.png'))
for name,source in [('junit-results.xml','build/test-results/test/TEST-dev.facecam.CoreTest.xml'),('server.log','run-packaged-server/logs/latest.log'),('viewer.log','run-packaged-viewer/logs/latest.log'),('receiver-metrics.txt','run-packaged-viewer/verification-metrics.txt')]:
    shutil.copy2(root/source,out/name)
print('Collected FaceCam 1.1.0 appearance evidence')
