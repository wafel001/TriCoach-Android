#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else 'ci-work/GRA-Unity-v0.4.0')

def read(rel):
    return (root / rel).read_text(encoding='utf-8')

def write(rel, text):
    p = root / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding='utf-8')

# v0.5.2 is deliberately restricted to visual resource binding and Android compatibility.
# Gameplay source is checksum-protected by CI before/after all patches.

write('Assets/Scripts/World/VisualMaterialLibrary.cs', r'''using System;
using System.Collections.Generic;
using UnityEngine;
namespace GRA.World
{
    public static class VisualMaterialLibrary
    {
        private const string BaseMaterialResourcePath = "Materials/VoxelColor";
        private static readonly Dictionary<int, Material> Cache = new Dictionary<int, Material>();
        private static Material baseMaterial;
        private static MaterialPropertyBlock block;

        private static Material BaseMaterial
        {
            get
            {
                if (baseMaterial != null) return baseMaterial;
                baseMaterial = Resources.Load<Material>(BaseMaterialResourcePath);
                return baseMaterial;
            }
        }

        public static Shader Shader => BaseMaterial != null ? BaseMaterial.shader : null;
        public static bool IsReady => BaseMaterial != null && BaseMaterial.shader != null && BaseMaterial.shader.isSupported;

        public static void ValidateOrThrow()
        {
            if (BaseMaterial == null) throw new InvalidOperationException("GRA visual material missing from Resources/Materials/VoxelColor");
            if (BaseMaterial.shader == null) throw new InvalidOperationException("GRA visual material has no shader");
            if (!BaseMaterial.shader.isSupported) throw new InvalidOperationException("GRA/VoxelColor unsupported on active graphics API");
        }

        public static Material Get(Color color, bool transparent = false)
        {
            ValidateOrThrow();
            Color32 c = color;
            int key = (c.r << 24) | (c.g << 16) | (c.b << 8) | c.a;
            if (transparent) key ^= unchecked((int)0x80000000);
            if (Cache.TryGetValue(key, out var cached) && cached != null) return cached;

            // Clone the serialized Resources material so the APK contains a hard shader dependency.
            var m = new Material(BaseMaterial)
            {
                name = $"GRA_Voxel_{c.r}_{c.g}_{c.b}_{c.a}_{(transparent ? 1 : 0)}",
                hideFlags = HideFlags.DontSave
            };
            m.SetColor("_Color", color);
            if (m.HasProperty("_ZWrite")) m.SetFloat("_ZWrite", transparent ? 0f : 1f);
            m.renderQueue = transparent ? 3000 : 2000;
            Cache[key] = m;
            return m;
        }

        public static void Apply(Renderer renderer, Color color, bool transparent = false)
        {
            if (renderer != null) renderer.sharedMaterial = Get(color, transparent);
        }

        public static void SetRendererColor(Renderer renderer, Color color)
        {
            if (renderer == null) return;
            if (block == null) block = new MaterialPropertyBlock();
            renderer.GetPropertyBlock(block);
            block.SetColor("_Color", color);
            renderer.SetPropertyBlock(block);
        }
    }
}
''')

bootstrap_rel = 'Assets/Editor/ProjectBootstrap.cs'
s = read(bootstrap_rel)
s = s.replace('Pixel-Empire-v0.5.1.apk', 'Pixel-Empire-v0.5.2.apk')
s = s.replace('PlayerSettings.bundleVersion = "0.5.1"; PlayerSettings.Android.bundleVersionCode = 51;',
              'PlayerSettings.bundleVersion = "0.5.2"; PlayerSettings.Android.bundleVersionCode = 52;')
s = s.replace('PlayerSettings.Android.targetSdkVersion = AndroidSdkVersions.AndroidApiLevelAuto;',
              'PlayerSettings.Android.targetSdkVersion = (AndroidSdkVersions)35;')
s = s.replace('GRA_BUILD_VERSION=0.5.1', 'GRA_BUILD_VERSION=0.5.2')
s = s.replace('GRA_TARGET_SDK_SETTING=Auto', 'GRA_TARGET_SDK_SETTING=35')

needle = '''        public static void ValidateProject()\n        {\n'''
insert = r'''        const string MaterialPath = "Assets/Resources/Materials/VoxelColor.mat";

        static void EnsureVisualMaterialAsset()
        {
            Directory.CreateDirectory("Assets/Resources/Materials");
            AssetDatabase.ImportAsset(ShaderPath, ImportAssetOptions.ForceUpdate);
            var shader = AssetDatabase.LoadAssetAtPath<Shader>(ShaderPath);
            if (shader == null) throw new Exception("Voxel shader import failed before material creation");
            if (ShaderUtil.ShaderHasError(shader))
                throw new Exception("Voxel shader compile error before material creation: " + string.Join(" | ", ShaderUtil.GetShaderMessages(shader).Select(m => m.message)));

            var material = AssetDatabase.LoadAssetAtPath<Material>(MaterialPath);
            if (material == null)
            {
                material = new Material(shader) { name = "GRA_VoxelColor_Base" };
                material.SetColor("_Color", Color.white);
                AssetDatabase.CreateAsset(material, MaterialPath);
            }
            else if (material.shader != shader)
            {
                material.shader = shader;
                material.SetColor("_Color", Color.white);
                EditorUtility.SetDirty(material);
            }
            AssetDatabase.SaveAssets();
            AssetDatabase.Refresh(ImportAssetOptions.ForceUpdate);
        }

        public static void ValidateProject()
        {
            EnsureVisualMaterialAsset();
'''
if needle not in s:
    raise SystemExit('ValidateProject injection point not found')
s = s.replace(needle, insert, 1)

needle2 = '''            var shader = AssetDatabase.LoadAssetAtPath<Shader>(ShaderPath);\n            if (shader == null) throw new Exception("Voxel shader import failed");\n            if (ShaderUtil.ShaderHasError(shader)) throw new Exception("Voxel shader compile error: " + string.Join(" | ", ShaderUtil.GetShaderMessages(shader).Select(m => m.message)));\n'''
replacement2 = needle2 + '''            var material = AssetDatabase.LoadAssetAtPath<Material>(MaterialPath);\n            if (material == null) throw new Exception("Serialized voxel material missing");\n            if (material.shader != shader) throw new Exception("Serialized voxel material does not reference GRA/VoxelColor");\n'''
if needle2 not in s:
    raise SystemExit('Shader validation block not found')
s = s.replace(needle2, replacement2, 1)
write(bootstrap_rel, s)

assert 'Shader.Find(' not in read('Assets/Scripts/World/VisualMaterialLibrary.cs')
assert 'Resources.Load<Material>(BaseMaterialResourcePath)' in read('Assets/Scripts/World/VisualMaterialLibrary.cs')
assert 'Pixel-Empire-v0.5.2.apk' in read(bootstrap_rel)
assert 'bundleVersion = "0.5.2"' in read(bootstrap_rel)
assert 'targetSdkVersion = (AndroidSdkVersions)35' in read(bootstrap_rel)
assert 'EnsureVisualMaterialAsset();' in read(bootstrap_rel)
assert 'Serialized voxel material does not reference GRA/VoxelColor' in read(bootstrap_rel)
print('APPLY V0.5.2 VISUAL MATERIAL + ANDROID 35 FIX: PASS')
