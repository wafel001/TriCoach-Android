#!/usr/bin/env python3
from pathlib import Path
import json, re, sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else 'ci-work/GRA-Unity-v0.4.0')

def read(rel):
    return (root / rel).read_text(encoding='utf-8')

def write(rel, text):
    p = root / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding='utf-8')

# Keep the legacy gameplay source untouched. This patch is restricted to visual/UI/editor build layers.
manifest = json.loads(read('Packages/manifest.json'))
deps = manifest.setdefault('dependencies', {})
# Unity 2022.3 LTS uses URP 14.x. Pin the package so Android and Editor use the same renderer.
deps['com.unity.render-pipelines.universal'] = deps.get('com.unity.render-pipelines.universal', '14.0.11')
write('Packages/manifest.json', json.dumps(manifest, indent=2, ensure_ascii=False) + '\n')

write('Assets/Resources/Shaders/VoxelColor.shader', r'''Shader "GRA/VoxelColor"
{
    Properties
    {
        [MainColor] _Color ("Color", Color) = (1,1,1,1)
        [HideInInspector] _ZWrite ("ZWrite", Float) = 1
    }
    SubShader
    {
        Tags { "RenderType"="Opaque" "Queue"="Geometry" "RenderPipeline"="UniversalPipeline" }
        LOD 100
        Pass
        {
            Name "Forward"
            Tags { "LightMode"="UniversalForward" }
            Cull Back
            ZWrite [_ZWrite]
            Blend SrcAlpha OneMinusSrcAlpha
            HLSLPROGRAM
            #pragma target 3.0
            #pragma vertex vert
            #pragma fragment frag
            #include "Packages/com.unity.render-pipelines.universal/ShaderLibrary/Core.hlsl"
            #include "Packages/com.unity.render-pipelines.universal/ShaderLibrary/Lighting.hlsl"

            struct Attributes
            {
                float4 positionOS : POSITION;
                float3 normalOS : NORMAL;
            };
            struct Varyings
            {
                float4 positionHCS : SV_POSITION;
                float3 normalWS : TEXCOORD0;
            };

            CBUFFER_START(UnityPerMaterial)
                half4 _Color;
                half _ZWrite;
            CBUFFER_END

            Varyings vert(Attributes input)
            {
                Varyings output;
                output.positionHCS = TransformObjectToHClip(input.positionOS.xyz);
                output.normalWS = TransformObjectToWorldNormal(input.normalOS);
                return output;
            }

            half4 frag(Varyings input) : SV_Target
            {
                Light mainLight = GetMainLight();
                half ndotl = saturate(dot(normalize(input.normalWS), mainLight.direction));
                half lighting = 0.48h + ndotl * 0.52h;
                half3 rgb = _Color.rgb * mainLight.color * lighting;
                return half4(rgb, _Color.a);
            }
            ENDHLSL
        }
    }
    Fallback Off
}
''')

write('Assets/Scripts/World/VisualMaterialLibrary.cs', r'''using System;
using System.Collections.Generic;
using UnityEngine;
namespace GRA.World
{
    public static class VisualMaterialLibrary
    {
        private const string ResourcePath = "Shaders/VoxelColor";
        private static readonly Dictionary<int, Material> Cache = new Dictionary<int, Material>();
        private static Shader shader;
        private static MaterialPropertyBlock block;
        public static Shader Shader
        {
            get
            {
                if (shader != null) return shader;
                shader = Resources.Load<Shader>(ResourcePath);
                if (shader == null) shader = UnityEngine.Shader.Find("GRA/VoxelColor");
                return shader;
            }
        }
        public static bool IsReady => Shader != null && Shader.isSupported;
        public static void ValidateOrThrow()
        {
            if (Shader == null) throw new InvalidOperationException("GRA/VoxelColor missing");
            if (!Shader.isSupported) throw new InvalidOperationException("GRA/VoxelColor unsupported on active graphics API");
        }
        public static Material Get(Color color, bool transparent = false)
        {
            ValidateOrThrow();
            Color32 c = color;
            int key = (c.r << 24) | (c.g << 16) | (c.b << 8) | c.a;
            if (transparent) key ^= unchecked((int)0x80000000);
            if (Cache.TryGetValue(key, out var cached) && cached != null) return cached;
            var m = new Material(Shader)
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

write('Assets/Scripts/UI/RuntimeFontProvider.cs', r'''using System;
using UnityEngine;
namespace GRA.UI
{
    public static class RuntimeFontProvider
    {
        private static Font cached;
        public static Font Get()
        {
            if (cached != null) return cached;
            try { cached = Resources.GetBuiltinResource<Font>("LegacyRuntime.ttf"); } catch (Exception) { cached = null; }
            if (cached != null) return cached;
            try
            {
                var names = Font.GetOSInstalledFontNames();
                string chosen = null;
                if (names != null)
                {
                    foreach (var n in names)
                        if (n.IndexOf("Roboto", StringComparison.OrdinalIgnoreCase) >= 0 || n.IndexOf("Noto Sans", StringComparison.OrdinalIgnoreCase) >= 0) { chosen = n; break; }
                    if (string.IsNullOrEmpty(chosen) && names.Length > 0) chosen = names[0];
                }
                if (!string.IsNullOrEmpty(chosen)) cached = Font.CreateDynamicFontFromOSFont(chosen, 32);
            }
            catch (Exception) { cached = null; }
            if (cached == null) Debug.LogError("GRA UI: no usable runtime font found");
            return cached;
        }
    }
}
''')

# Patch visual presenters only.
world = 'Assets/Scripts/World/VoxelWorldPresenter.cs'
s = read(world)
s = s.replace('using System.Collections.Generic;\n', '')
s = re.sub(r'\n\s*private readonly Dictionary<int, Material> materialCache = new Dictionary<int, Material>\(\);', '', s)
s = s.replace('r.sharedMaterial=GetMaterial(color,transparent);', 'VisualMaterialLibrary.Apply(r,color,transparent);')
start = s.find('        private Material GetMaterial(Color color,bool transparent)')
if start >= 0:
    s = s[:start] + '        private Material GetMaterial(Color color,bool transparent)\n        {\n            return VisualMaterialLibrary.Get(color, transparent);\n        }\n    }\n}\n'
write(world, s)

hero = 'Assets/Scripts/World/HeroArmyPresenter.cs'
s = read(hero).replace('go.GetComponent<Renderer>().material.color = color;', 'VisualMaterialLibrary.Apply(go.GetComponent<Renderer>(), color);')
write(hero, s)
fx = 'Assets/Scripts/World/CombatFxPresenter.cs'
s = read(fx).replace('r.material.color = new Color(1f, .69f, .05f);', 'VisualMaterialLibrary.Apply(r, new Color(1f, .69f, .05f));')
write(fx, s)

motion = 'Assets/Scripts/World/VoxelMotion.cs'
s = read(motion)
s = s.replace('private void Start(){r=GetComponent<Renderer>(); if(r!=null)baseColor=r.material.color;}\n        private void Update(){if(r==null)return; var c=baseColor; c.a=Mathf.Clamp01(baseColor.a+Mathf.Sin(Time.time*2.2f)*.08f); r.material.color=c;}',
'''private void Start(){r=GetComponent<Renderer>(); if(r!=null&&r.sharedMaterial!=null)baseColor=r.sharedMaterial.GetColor("_Color");}
        private void Update(){if(r==null)return; var c=baseColor; c.a=Mathf.Clamp01(baseColor.a+Mathf.Sin(Time.time*2.2f)*.08f); VisualMaterialLibrary.SetRendererColor(r,c);}''')
write(motion, s)

hud = 'Assets/Scripts/UI/LegacyHudController.cs'
s = read(hud)
s = re.sub(r't\.font=Resources\.GetBuiltinResource<Font>\("(?:Arial|LegacyRuntime)\.ttf"\);', 't.font=RuntimeFontProvider.Get();', s)
write(hud, s)

# Editor/build layer: preserve the URP asset already configured in the reconstructed project and fail loudly if it is absent.
write('Assets/Editor/ProjectBootstrap.cs', r'''#if UNITY_EDITOR
using System;
using System.IO;
using System.Linq;
using GRA.Core;
using UnityEditor;
using UnityEditor.Build.Reporting;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.Rendering;
using UnityEngine.Rendering.Universal;
namespace GRA.Editor
{
    public static class ProjectBootstrap
    {
        const string ScenePath = "Assets/Scenes/Main.unity";
        const string ShaderPath = "Assets/Resources/Shaders/VoxelColor.shader";
        const string BuildPath = "Builds/Android/Pixel-Empire-v0.5.1.apk";

        [MenuItem("GRA/Generate Main Scene")]
        public static void GenerateMainScene()
        {
            EnsureUrp();
            Directory.CreateDirectory("Assets/Scenes");
            var scene = EditorSceneManager.NewScene(NewSceneSetup.EmptyScene, NewSceneMode.Single);
            var root = new GameObject("GRA_GameRoot"); root.AddComponent<GameBootstrap>();
            var cameraGo = new GameObject("Main Camera", typeof(Camera), typeof(AudioListener));
            cameraGo.tag = "MainCamera";
            var cam = cameraGo.GetComponent<Camera>();
            cam.clearFlags = CameraClearFlags.SolidColor; cam.backgroundColor = new Color(.39f,.68f,.88f); cam.fieldOfView = 42f; cam.nearClipPlane = .1f; cam.farClipPlane = 150f;
            cameraGo.transform.position = new Vector3(13.5f,12.5f,-17f); cameraGo.transform.LookAt(new Vector3(0,1.1f,5f));
            var lightGo = new GameObject("Sun", typeof(Light)); var light = lightGo.GetComponent<Light>();
            light.type = LightType.Directional; light.intensity = 1.2f; light.color = new Color(1f,.91f,.74f); light.shadows = LightShadows.Soft; lightGo.transform.rotation = Quaternion.Euler(48,-32,0);
            var fillGo = new GameObject("Sky Fill", typeof(Light)); var fill = fillGo.GetComponent<Light>();
            fill.type = LightType.Directional; fill.intensity = .28f; fill.color = new Color(.46f,.68f,1f); fillGo.transform.rotation = Quaternion.Euler(35,150,0);
            new GameObject("EventSystem", typeof(EventSystem), typeof(StandaloneInputModule));
            RenderSettings.ambientMode = AmbientMode.Flat; RenderSettings.ambientLight = new Color(.43f,.52f,.46f);
            RenderSettings.fog = true; RenderSettings.fogColor = new Color(.58f,.75f,.78f); RenderSettings.fogMode = FogMode.Linear; RenderSettings.fogStartDistance = 24f; RenderSettings.fogEndDistance = 62f;
            EditorSceneManager.SaveScene(scene, ScenePath);
            EditorBuildSettings.scenes = new[] { new EditorBuildSettingsScene(ScenePath, true) };
            PlayerSettings.companyName = "GRA Studio"; PlayerSettings.productName = "Pixel Empire";
            PlayerSettings.SetApplicationIdentifier(BuildTargetGroup.Android, "com.grastudio.pixelempire");
            PlayerSettings.bundleVersion = "0.5.1"; PlayerSettings.Android.bundleVersionCode = 51;
            PlayerSettings.defaultInterfaceOrientation = UIOrientation.Portrait;
            PlayerSettings.Android.minSdkVersion = AndroidSdkVersions.AndroidApiLevel26;
            PlayerSettings.Android.targetSdkVersion = AndroidSdkVersions.AndroidApiLevelAuto;
            PlayerSettings.Android.targetArchitectures = AndroidArchitecture.ARM64;
            PlayerSettings.SetScriptingBackend(BuildTargetGroup.Android, ScriptingImplementation.IL2CPP);
            AssetDatabase.SaveAssets();
        }

        static void EnsureUrp()
        {
            var rp = GraphicsSettings.defaultRenderPipeline;
            if (rp == null) throw new Exception("URP gate: GraphicsSettings.defaultRenderPipeline is null");
            if (!(rp is UniversalRenderPipelineAsset)) throw new Exception("URP gate: active pipeline is not UniversalRenderPipelineAsset: " + rp.GetType().FullName);
            QualitySettings.renderPipeline = rp;
        }

        public static void ValidateProject()
        {
            string[] required = {
                "Assets/Scripts/Core/GameBootstrap.cs", "Assets/Scripts/Gameplay/CombatSystem.cs",
                "Assets/Scripts/UI/LegacyHudController.cs", "Assets/Scripts/UI/RuntimeFontProvider.cs",
                "Assets/Scripts/World/VoxelWorldPresenter.cs", "Assets/Scripts/World/VisualMaterialLibrary.cs", ShaderPath
            };
            foreach (var p in required) if (!File.Exists(p)) throw new Exception("Missing " + p);
            if (!File.ReadAllText("Packages/manifest.json").Contains("com.unity.render-pipelines.universal")) throw new Exception("URP package missing");
            AssetDatabase.ImportAsset(ShaderPath, ImportAssetOptions.ForceUpdate); AssetDatabase.Refresh(ImportAssetOptions.ForceUpdate);
            var shader = AssetDatabase.LoadAssetAtPath<Shader>(ShaderPath);
            if (shader == null) throw new Exception("Voxel shader import failed");
            if (ShaderUtil.ShaderHasError(shader)) throw new Exception("Voxel shader compile error: " + string.Join(" | ", ShaderUtil.GetShaderMessages(shader).Select(m => m.message)));
            EnsureUrp();
            string[] visuals = { "Assets/Scripts/World/VoxelWorldPresenter.cs", "Assets/Scripts/World/HeroArmyPresenter.cs", "Assets/Scripts/World/CombatFxPresenter.cs", "Assets/Scripts/World/VoxelMotion.cs" };
            foreach (var p in visuals) if (File.ReadAllText(p).Contains(".material.color")) throw new Exception("Unsafe material mutation: " + p);
            Debug.Log("GRA project + URP shader validation: PASS");
        }

        public static void BuildAndroid()
        {
            ValidateProject(); GenerateMainScene(); ValidateProject(); Directory.CreateDirectory("Builds/Android");
            var o = new BuildPlayerOptions { scenes = new[] { ScenePath }, locationPathName = BuildPath, target = BuildTarget.Android, options = BuildOptions.None };
            var r = BuildPipeline.BuildPlayer(o);
            if (r.summary.result != BuildResult.Succeeded) throw new Exception("Android build failed: " + r.summary.result);
            Debug.Log("GRA_BUILD_VERSION=0.5.1"); Debug.Log("GRA_RENDER_PIPELINE=URP"); Debug.Log("GRA_TARGET_SDK_SETTING=Auto");
        }
    }
}
#endif
''')

assert 'com.unity.render-pipelines.universal' in read('Packages/manifest.json')
assert '"RenderPipeline"="UniversalPipeline"' in read('Assets/Resources/Shaders/VoxelColor.shader')
assert 'Lighting.hlsl' in read('Assets/Resources/Shaders/VoxelColor.shader')
assert 'RuntimeFontProvider.Get()' in read(hud)
assert '.material.color' not in read(hero)
assert '.material.color' not in read(fx)
assert '.material.color' not in read(motion)
assert 'UniversalRenderPipelineAsset' in read('Assets/Editor/ProjectBootstrap.cs')
assert 'Pixel-Empire-v0.5.1.apk' in read('Assets/Editor/ProjectBootstrap.cs')
print('APPLY V0.5.1 URP VISUAL REBUILD: PASS')
