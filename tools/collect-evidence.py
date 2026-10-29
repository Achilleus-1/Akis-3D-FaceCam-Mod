import pathlib, shutil
root=pathlib.Path(__file__).resolve().parents[1]
out=root/'docs/verification';out.mkdir(parents=True,exist_ok=True)
images={
    'front.png':'run-packaged-viewer/screenshots/facecam-packaged-front-final.png',
    'settings.png':'run-packaged-viewer/screenshots/facecam-configuration-final.png',
    'camera-error.png':'run-packaged-viewer/screenshots/facecam-camera-error-final.png',
    'world-occlusion.png':'run-packaged-viewer/screenshots/facecam-world-occlusion-final.png',
    'outer-opaque-transparent.png':'run-verify-viewer/screenshots/facecam-outer-mask-on.png',
    'outer-disabled-depth.png':'run-verify-viewer/screenshots/facecam-outer-disabled-depth.png',
    'helmet-oblique.png':'run-verify-viewer/screenshots/facecam-helmet-oblique.png',
    'standard-model.png':'run-verify-viewer/screenshots/facecam-standard-model.png',
    'sharing-off.png':'run-verify-viewer/screenshots/facecam-sharing-off.png',
    'timeout.png':'run-verify-viewer/screenshots/facecam-server-timeout.png',
}
for helmet in ['leather_helmet','chainmail_helmet','iron_helmet','golden_helmet','diamond_helmet','netherite_helmet','turtle_helmet']:
    images[helmet+'.png']='run-verify-viewer/screenshots/facecam-vanilla-'+helmet+'.png'
for name,source in images.items():shutil.copy2(root/source,out/name)
for name in ['heap-before.txt','heap-after.txt','server-native-instance-check.txt']:
    shutil.copy2(root/'build/verification'/name,out/name)
shutil.copy2(root/'build/test-results/test/TEST-dev.facecam.CoreTest.xml',out/'junit-results.xml')
shutil.copy2(root/'run-packaged-server/logs/latest.log',out/'packaged-server.log')
shutil.copy2(root/'run-packaged-viewer/logs/latest.log',out/'packaged-viewer.log')
shutil.copy2(root/'run-packaged-sender/logs/latest.log',out/'packaged-sender.log')
print('Collected',len(images),'unmodified framebuffer captures, test results and logs')
