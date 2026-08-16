#!/usr/bin/env python3
from pathlib import Path
import json, re, sys

root=Path(sys.argv[1] if len(sys.argv)>1 else 'ci-work/GRA-Unity-v0.4.0')

def read(rel): return (root/rel).read_text(encoding='utf-8')
def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding='utf-8')

def replace_once(rel, old, new):
    s=read(rel)
    if old not in s and new not in s: raise SystemExit(f'pattern not found in {rel}: {old[:80]!r}')
    if old in s: s=s.replace(old,new)
    write(rel,s)

manifest=json.loads(read('Packages/manifest.json'))
manifest.setdefault('dependencies',{}).pop('com.unity.render-pipelines.universal',None)
write('Packages/manifest.json',json.dumps(manifest,indent=2,ensure_ascii=False)+'\n')

write('Assets/Resources/Shaders/VoxelColor.shader',r'''Shader "GRA/VoxelColor"
{
    Properties { _Color ("Color", Color) = (1,1,1,1) [HideInInspector] _ZWrite ("ZWrite", Float) = 1 }
    SubShader
    {
        Tags { "RenderType"="Opaque" "Queue"="Geometry" }
        LOD 50
        Pass
        {
            Cull Back
            ZWrite [_ZWrite]
            Blend SrcAlpha OneMinusSrcAlpha
            CGPROGRAM
            #pragma target 2.0
            #pragma vertex vert
            #pragma fragment frag
            #include "UnityCG.cginc"
            struct appdata { float4 vertex : POSITION; };
            struct v2f { float4 vertex : SV_POSITION; };
            fixed4 _Color;
            v2f vert(appdata v) { v2f o; o.vertex=UnityObjectToClipPos(v.vertex); return o; }
            fixed4 frag(v2f i) : SV_Target { return _Color; }
            ENDCG
        }
    }
    Fallback Off
}
''')

write('Assets/Scripts/World/VisualMaterialLibrary.cs',r'''using System;
using System.Collections.Generic;
using UnityEngine;
namespace GRA.World
{
    public static class VisualMaterialLibrary
    {
        private const string ResourcePath="Shaders/VoxelColor";
        private static readonly Dictionary<int,Material> Cache=new Dictionary<int,Material>();
        private static Shader shader;
        private static MaterialPropertyBlock block;
        public static Shader Shader { get { if(shader!=null)return shader; shader=Resources.Load<Shader>(ResourcePath); if(shader==null)shader=UnityEngine.Shader.Find("GRA/VoxelColor"); return shader; } }
        public static bool IsReady=>Shader!=null&&Shader.isSupported;
        public static void ValidateOrThrow(){if(Shader==null)throw new InvalidOperationException("GRA/VoxelColor missing"); if(!Shader.isSupported)throw new InvalidOperationException("GRA/VoxelColor unsupported");}
        public static Material Get(Color color,bool transparent=false)
        {
            ValidateOrThrow(); Color32 c=color; int key=(c.r<<24)|(c.g<<16)|(c.b<<8)|c.a; if(transparent)key^=unchecked((int)0x80000000);
            if(Cache.TryGetValue(key,out var cached)&&cached!=null)return cached;
            var m=new Material(Shader){name=$"GRA_Voxel_{c.r}_{c.g}_{c.b}_{c.a}_{(transparent?1:0)}",hideFlags=HideFlags.DontSave};
            m.SetColor("_Color",color); if(m.HasProperty("_ZWrite"))m.SetFloat("_ZWrite",transparent?0f:1f); m.renderQueue=transparent?3000:2000; Cache[key]=m; return m;
        }
        public static void Apply(Renderer renderer,Color color,bool transparent=false){if(renderer!=null)renderer.sharedMaterial=Get(color,transparent);}
        public static void SetRendererColor(Renderer renderer,Color color){if(renderer==null)return; if(block==null)block=new MaterialPropertyBlock(); renderer.GetPropertyBlock(block); block.SetColor("_Color",color); renderer.SetPropertyBlock(block);}
    }
}
''')

write('Assets/Scripts/UI/RuntimeFontProvider.cs',r'''using System;
using UnityEngine;
namespace GRA.UI
{
    public static class RuntimeFontProvider
    {
        private static Font cached;
        public static Font Get()
        {
            if(cached!=null)return cached;
            try{cached=Resources.GetBuiltinResource<Font>("Arial.ttf");}catch(Exception){cached=null;} if(cached!=null)return cached;
            try{cached=Resources.GetBuiltinResource<Font>("LegacyRuntime.ttf");}catch(Exception){cached=null;} if(cached!=null)return cached;
            try{var names=Font.GetOSInstalledFontNames(); string chosen=null; if(names!=null){foreach(var n in names)if(n.IndexOf("Roboto",StringComparison.OrdinalIgnoreCase)>=0||n.IndexOf("Noto Sans",StringComparison.OrdinalIgnoreCase)>=0){chosen=n;break;} if(string.IsNullOrEmpty(chosen)&&names.Length>0)chosen=names[0];} if(!string.IsNullOrEmpty(chosen))cached=Font.CreateDynamicFontFromOSFont(chosen,32);}catch(Exception){cached=null;}
            if(cached==null)Debug.LogError("GRA UI: no usable runtime font found"); return cached;
        }
    }
}
''')

world='Assets/Scripts/World/VoxelWorldPresenter.cs'; s=read(world)
s=s.replace('using System.Collections.Generic;\n','')
s=re.sub(r'\n\s*private readonly Dictionary<int, Material> materialCache = new Dictionary<int, Material>\(\);','',s)
s=s.replace('r.sharedMaterial=GetMaterial(color,transparent);','VisualMaterialLibrary.Apply(r,color,transparent);')
start=s.find('        private Material GetMaterial(Color color,bool transparent)')
if start>=0:
    s=s[:start]+'        private Material GetMaterial(Color color,bool transparent)\n        {\n            return VisualMaterialLibrary.Get(color, transparent);\n        }\n    }\n}\n'
write(world,s)

replace_once('Assets/Scripts/World/HeroArmyPresenter.cs','go.GetComponent<Renderer>().material.color = color;','VisualMaterialLibrary.Apply(go.GetComponent<Renderer>(), color);')
replace_once('Assets/Scripts/World/CombatFxPresenter.cs','r.material.color = new Color(1f, .69f, .05f);','VisualMaterialLibrary.Apply(r, new Color(1f, .69f, .05f));')

motion='Assets/Scripts/World/VoxelMotion.cs'; s=read(motion)
old='private void Start(){r=GetComponent<Renderer>(); if(r!=null)baseColor=r.material.color;}\n        private void Update(){if(r==null)return; var c=baseColor; c.a=Mathf.Clamp01(baseColor.a+Mathf.Sin(Time.time*2.2f)*.08f); r.material.color=c;}'
new='private void Start(){r=GetComponent<Renderer>(); if(r!=null&&r.sharedMaterial!=null)baseColor=r.sharedMaterial.GetColor("_Color");}\n        private void Update(){if(r==null)return; var c=baseColor; c.a=Mathf.Clamp01(baseColor.a+Mathf.Sin(Time.time*2.2f)*.08f); VisualMaterialLibrary.SetRendererColor(r,c);}'
if old not in s and new not in s: raise SystemExit('WaterShimmer pattern missing')
s=s.replace(old,new); write(motion,s)

hud='Assets/Scripts/UI/LegacyHudController.cs'; s=read(hud)
s=re.sub(r't\.font=Resources\.GetBuiltinResource<Font>\("(?:Arial|LegacyRuntime)\.ttf"\);','t.font=RuntimeFontProvider.Get();',s)
write(hud,s)

write('Assets/Editor/ProjectBootstrap.cs',r'''#if UNITY_EDITOR
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
namespace GRA.Editor
{
    public static class ProjectBootstrap
    {
        const string ScenePath="Assets/Scenes/Main.unity"; const string ShaderPath="Assets/Resources/Shaders/VoxelColor.shader"; const string BuildPath="Builds/Android/Pixel-Empire-v0.5.0.apk";
        [MenuItem("GRA/Generate Main Scene")]
        public static void GenerateMainScene()
        {
            GraphicsSettings.defaultRenderPipeline=null; QualitySettings.renderPipeline=null;
            Directory.CreateDirectory("Assets/Scenes"); var scene=EditorSceneManager.NewScene(NewSceneSetup.EmptyScene,NewSceneMode.Single);
            var root=new GameObject("GRA_GameRoot"); root.AddComponent<GameBootstrap>();
            var cameraGo=new GameObject("Main Camera",typeof(Camera),typeof(AudioListener)); cameraGo.tag="MainCamera"; var cam=cameraGo.GetComponent<Camera>(); cam.clearFlags=CameraClearFlags.SolidColor; cam.backgroundColor=new Color(.39f,.68f,.88f); cam.fieldOfView=42f; cam.nearClipPlane=.1f; cam.farClipPlane=150f; cameraGo.transform.position=new Vector3(13.5f,12.5f,-17f); cameraGo.transform.LookAt(new Vector3(0,1.1f,5f));
            var lightGo=new GameObject("Sun",typeof(Light)); var light=lightGo.GetComponent<Light>(); light.type=LightType.Directional; light.intensity=1.2f; light.color=new Color(1f,.91f,.74f); light.shadows=LightShadows.Soft; lightGo.transform.rotation=Quaternion.Euler(48,-32,0);
            var fillGo=new GameObject("Sky Fill",typeof(Light)); var fill=fillGo.GetComponent<Light>(); fill.type=LightType.Directional; fill.intensity=.28f; fill.color=new Color(.46f,.68f,1f); fillGo.transform.rotation=Quaternion.Euler(35,150,0);
            new GameObject("EventSystem",typeof(EventSystem),typeof(StandaloneInputModule)); RenderSettings.ambientMode=AmbientMode.Flat; RenderSettings.ambientLight=new Color(.43f,.52f,.46f); RenderSettings.fog=true; RenderSettings.fogColor=new Color(.58f,.75f,.78f); RenderSettings.fogMode=FogMode.Linear; RenderSettings.fogStartDistance=24f; RenderSettings.fogEndDistance=62f;
            EditorSceneManager.SaveScene(scene,ScenePath); EditorBuildSettings.scenes=new[]{new EditorBuildSettingsScene(ScenePath,true)};
            PlayerSettings.companyName="GRA Studio"; PlayerSettings.productName="Pixel Empire"; PlayerSettings.SetApplicationIdentifier(BuildTargetGroup.Android,"com.grastudio.pixelempire"); PlayerSettings.bundleVersion="0.5.0"; PlayerSettings.Android.bundleVersionCode=50; PlayerSettings.defaultInterfaceOrientation=UIOrientation.Portrait; PlayerSettings.Android.minSdkVersion=AndroidSdkVersions.AndroidApiLevel26; PlayerSettings.Android.targetSdkVersion=AndroidSdkVersions.AndroidApiLevelAuto; PlayerSettings.Android.targetArchitectures=AndroidArchitecture.ARM64; PlayerSettings.SetScriptingBackend(BuildTargetGroup.Android,ScriptingImplementation.IL2CPP); AssetDatabase.SaveAssets();
        }
        public static void ValidateProject()
        {
            string[] required={"Assets/Scripts/Core/GameBootstrap.cs","Assets/Scripts/Gameplay/CombatSystem.cs","Assets/Scripts/UI/LegacyHudController.cs","Assets/Scripts/UI/RuntimeFontProvider.cs","Assets/Scripts/World/VoxelWorldPresenter.cs","Assets/Scripts/World/VisualMaterialLibrary.cs",ShaderPath}; foreach(var p in required)if(!File.Exists(p))throw new Exception("Missing "+p);
            AssetDatabase.ImportAsset(ShaderPath,ImportAssetOptions.ForceUpdate); AssetDatabase.Refresh(ImportAssetOptions.ForceUpdate); var shader=AssetDatabase.LoadAssetAtPath<Shader>(ShaderPath); if(shader==null)throw new Exception("Voxel shader import failed"); if(ShaderUtil.ShaderHasError(shader))throw new Exception("Voxel shader compile error: "+string.Join(" | ",ShaderUtil.GetShaderMessages(shader).Select(m=>m.message)));
            if(File.ReadAllText("Packages/manifest.json").Contains("com.unity.render-pipelines.universal"))throw new Exception("URP package still enabled");
            string[] visuals={"Assets/Scripts/World/VoxelWorldPresenter.cs","Assets/Scripts/World/HeroArmyPresenter.cs","Assets/Scripts/World/CombatFxPresenter.cs","Assets/Scripts/World/VoxelMotion.cs"}; foreach(var p in visuals){var src=File.ReadAllText(p); if(src.Contains(".material.color"))throw new Exception("Unsafe material mutation: "+p); if(src.Contains("Universal Render Pipeline/Lit"))throw new Exception("URP shader ref: "+p);} Debug.Log("GRA project + shader validation: PASS");
        }
        public static void BuildAndroid(){ValidateProject();GenerateMainScene();ValidateProject();Directory.CreateDirectory("Builds/Android");var o=new BuildPlayerOptions{scenes=new[]{ScenePath},locationPathName=BuildPath,target=BuildTarget.Android,options=BuildOptions.None};var r=BuildPipeline.BuildPlayer(o);if(r.summary.result!=BuildResult.Succeeded)throw new Exception("Android build failed: "+r.summary.result);Debug.Log("GRA_BUILD_VERSION=0.5.0");Debug.Log("GRA_RENDER_PIPELINE=Built-in");Debug.Log("GRA_TARGET_SDK_SETTING=Auto");}
    }
}
#endif
''')

assert 'com.unity.render-pipelines.universal' not in read('Packages/manifest.json')
assert 'Shader "GRA/VoxelColor"' in read('Assets/Resources/Shaders/VoxelColor.shader')
assert 'SRPDefaultUnlit' not in read('Assets/Resources/Shaders/VoxelColor.shader')
assert 'RuntimeFontProvider.Get()' in read(hud)
assert '.material.color' not in read('Assets/Scripts/World/HeroArmyPresenter.cs')
assert '.material.color' not in read('Assets/Scripts/World/CombatFxPresenter.cs')
assert '.material.color' not in read(motion)
assert 'AndroidApiLevelAuto' in read('Assets/Editor/ProjectBootstrap.cs')
print('APPLY V0.5.0: PASS')
